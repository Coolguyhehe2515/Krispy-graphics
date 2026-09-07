$input v_color0, v_texcoord0, v_worldPos
precision highp float;
#include "bgfx_shader.sh"
#include "settings.h"

float nl_hash(vec2 p) {
    p = fract(p * vec2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
}

float nl_valueNoise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);

    float a = nl_hash(i);
    float b = nl_hash(i + vec2(1.0, 0.0));
    float c = nl_hash(i + vec2(0.0, 1.0));
    float d = nl_hash(i + vec2(1.0, 1.0));

    return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
}

// Fractal Brownian Motion — layers multiple octaves of noise at increasing
// frequency and decreasing amplitude, building organic, rounded cloud shapes.
float nl_fbm(vec2 p) {
    float value = 0.0;
    float amplitude = 0.5;
    for (int i = 0; i < NL_CLOUD_OCTAVES; i++) {
        value += amplitude * nl_valueNoise(p);
        p *= 2.0;
        amplitude *= 0.5;
    }
    return value;
}

// Domain warping: offset the sample position using another noise field before
// sampling the main FBM. This is what gives clouds their soft, rounded, puffy
// look instead of a flat/grainy pattern — the coordinate itself gets distorted.
float nl_cloudDensity(vec2 p) {
    vec2 warp = vec2(
        nl_fbm(p + vec2(1.7, 9.2)),
        nl_fbm(p + vec2(8.3, 2.8))
    );
    p += warp * NL_CLOUD_WARP_STRENGTH;

    float shape = nl_fbm(p);

    // Small-scale detail layer adds texture/puffiness on top of the main rounded shape.
    float detail = nl_fbm(p * 4.0) * NL_CLOUD_DETAIL_STRENGTH;

    return shape + detail;
}

void main() {
    vec2 t = v_worldPos.xz * NL_CLOUD_SCROLL_SPEED;
    vec2 uv = v_worldPos.xz * 0.01 * NL_CLOUD_SCALE * 0.1 + t;

    float density = nl_cloudDensity(uv);

    // Soft threshold — smoothstep gives rounded, fluffy edges instead of a hard cutoff.
    float coverage = smoothstep(
        NL_CLOUD_COVERAGE - NL_CLOUD_SOFTNESS,
        NL_CLOUD_COVERAGE + NL_CLOUD_SOFTNESS,
        density
    );

    if (coverage < 0.01) {
        discard;
    }

    // Cheap pseudo-lighting: sample density slightly offset to approximate a
    // gradient (like a fake normal), used to brighten "upper" edges of puffs.
    float e = 0.03;
    float densityUp = nl_cloudDensity(uv + vec2(0.0, e));
    float rim = clamp((densityUp - density) * 8.0, 0.0, 1.0);

    vec3 rimColor = v_color0.rgb * NL_CLOUD_RIM_BRIGHTNESS;
    vec3 cloudColor = mix(v_color0.rgb, rimColor, rim * NL_CLOUD_RIM_STRENGTH);

    // Self-shading — undersides of thicker cloud areas read slightly darker.
    float shade = clamp(density - NL_CLOUD_COVERAGE, 0.0, 1.0);
    cloudColor *= 1.0 - shade * NL_CLOUD_SHADE_STRENGTH;

    float cloudAlpha = coverage * v_color0.a;

    gl_FragColor = vec4(cloudColor, cloudAlpha);
}
