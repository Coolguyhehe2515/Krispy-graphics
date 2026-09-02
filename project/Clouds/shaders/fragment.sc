$input v_color0, v_texcoord0, v_worldPos
precision highp float;
#include "bgfx_shader.sh"
#include "settings.h"

float nl_cloudHash(vec2 p) {
    return fract(cos(p.x + p.y * 332.0) * 335.552);
}

float nl_cloudSmoothNoise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    float a = nl_cloudHash(i);
    float b = nl_cloudHash(i + vec2(1.0, 0.0));
    float c = nl_cloudHash(i + vec2(0.0, 1.0));
    float d = nl_cloudHash(i + vec2(1.0, 1.0));
    return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
}

float nl_cloudCell(vec2 uv, float size) {
    vec2 f = fract(uv) - 0.5;
    float d = max(abs(f.x), abs(f.y)) - size;
    return smoothstep(0.03, -0.03, d);
}

vec3 nl_cloudPattern(vec2 uv, vec2 t) {
    float a = 0.0;
    float b = 0.0;

    uv *= NL_CLOUD_SCALE;

    for (int i = 0; i < NL_CLOUD_ITERATIONS; i++) {
        uv /= 1.007;
        float density = nl_cloudSmoothNoise(floor(uv + t) / NL_CLOUD_CLUSTER_SIZE);
        float c = step(NL_CLOUD_THRESHOLD, density);
        float r = nl_cloudCell(uv + t, NL_CLOUD_CELL_SIZE);
        a = max(a, r * c);
    }

    float shadeDensity = nl_cloudSmoothNoise(floor(uv + t) / NL_CLOUD_CLUSTER_SIZE);
    float shadeCell = step(NL_CLOUD_THRESHOLD, shadeDensity);
    float shadeShape = nl_cloudCell(uv + t, NL_CLOUD_CELL_SIZE);
    b = shadeCell * shadeShape;

    vec2 e = vec2(NL_CLOUD_RIM_OFFSET, 0.0);
    float n1 = step(NL_CLOUD_THRESHOLD, nl_cloudSmoothNoise(floor(uv + t + vec2(e.x, 0.0)) / NL_CLOUD_CLUSTER_SIZE)) * nl_cloudCell(uv + t + vec2(e.x, 0.0), NL_CLOUD_CELL_SIZE);
    float n2 = step(NL_CLOUD_THRESHOLD, nl_cloudSmoothNoise(floor(uv + t - vec2(e.x, 0.0)) / NL_CLOUD_CLUSTER_SIZE)) * nl_cloudCell(uv + t - vec2(e.x, 0.0), NL_CLOUD_CELL_SIZE);
    float n3 = step(NL_CLOUD_THRESHOLD, nl_cloudSmoothNoise(floor(uv + t + vec2(0.0, e.x)) / NL_CLOUD_CLUSTER_SIZE)) * nl_cloudCell(uv + t + vec2(0.0, e.x), NL_CLOUD_CELL_SIZE);
    float n4 = step(NL_CLOUD_THRESHOLD, nl_cloudSmoothNoise(floor(uv + t - vec2(0.0, e.x)) / NL_CLOUD_CLUSTER_SIZE)) * nl_cloudCell(uv + t - vec2(0.0, e.x), NL_CLOUD_CELL_SIZE);

    float rim = b * (1.0 - n1) + b * (1.0 - n2) + b * (1.0 - n3) + b * (1.0 - n4);
    rim = clamp(rim, 0.0, 1.0);

    a -= b * a * NL_CLOUD_SHADE_STRENGTH;

    return vec3(clamp(a, 0.0, 1.0), b, rim);
}

void main() {
    vec2 t = v_worldPos.xz * NL_CLOUD_SCROLL_SPEED;
    vec2 uv = v_worldPos.xz * 0.005;

    vec3 cloud = nl_cloudPattern(uv, t);

    float cloudAlpha = clamp(cloud.r, 0.0, 1.0);
    vec3 rimColor = v_color0.rgb * NL_CLOUD_RIM_BRIGHTNESS;
    vec3 cloudColor = mix(v_color0.rgb, rimColor, cloud.b * NL_CLOUD_RIM_STRENGTH);

    gl_FragColor = vec4(cloudColor, cloudAlpha);
}
