$input v_color0, v_texcoord0, v_worldPos
precision highp float;
#include "bgfx_shader.sh"
#include "settings.h"

uniform vec4 FogColor;
uniform vec4 SkyColor;
uniform vec4 LightWorldSpaceDirection;

vec3 nl_skyPaletteColor() {
    vec3 sunDir = normalize(LightWorldSpaceDirection.xyz);
    float elevation = sunDir.y;

    float dayFactor = smoothstep(-0.05, 0.2, elevation);

    float twilightFactor = 1.0 - smoothstep(0.0, NL_SKY_TWILIGHT_RANGE, abs(elevation));

    // Heuristic split between dawn and dusk using the sun's horizontal direction.
    float dawnDuskSide = step(0.0, sunDir.x);
    vec3 twilightColor = mix(NL_SKY_DUSK_COLOR, NL_SKY_DAWN_COLOR, dawnDuskSide);

    vec3 baseColor = mix(NL_SKY_NIGHT_COLOR, NL_SKY_DAY_COLOR, dayFactor);
    return mix(baseColor, twilightColor, twilightFactor);
}

void main() {
    vec3 dir = normalize(v_worldPos);
    float horizonFactor = 1.0 - clamp(dir.y, 0.0, 1.0);

    float blend = smoothstep(0.0, 1.0, horizonFactor);
    blend = pow(blend, NL_SKY_HORIZON_SHARPNESS);

    vec3 zenithColor = nl_skyPaletteColor();
    vec3 skyColor = mix(zenithColor, FogColor.rgb, blend);

    float edgeFade = smoothstep(NL_SKY_EDGE_START, NL_SKY_EDGE_END, horizonFactor);
    skyColor = mix(skyColor, FogColor.rgb, edgeFade * NL_SKY_EDGE_STRENGTH);

    gl_FragColor = vec4(skyColor, 1.0);
}
