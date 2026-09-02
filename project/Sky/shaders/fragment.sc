$input v_color0, v_texcoord0, v_worldPos
precision highp float;
#include "bgfx_shader.sh"
#include "settings.h"

uniform vec4 FogColor;
uniform vec4 SkyColor;
uniform vec4 LightWorldSpaceDirection;

void nl_skyPaletteColors(out vec3 zenithColor, out vec3 horizonColor, out vec3 edgeColor) {
    vec3 sunDir = normalize(LightWorldSpaceDirection.xyz);
    float elevation = sunDir.y;

    float dayFactor = smoothstep(-0.05, 0.2, elevation);
    float twilightFactor = 1.0 - smoothstep(0.0, NL_SKY_TWILIGHT_RANGE, abs(elevation));

    // Heuristic split between dawn and dusk — flip sign if reversed in-game.
    float dawnDuskSide = step(0.0, sunDir.x);
    vec3 twilightZenith = mix(NL_SKY_DUSK_ZENITH_COLOR, NL_SKY_DAWN_ZENITH_COLOR, dawnDuskSide);
    vec3 twilightHorizon = mix(NL_SKY_DUSK_HORIZON_COLOR, NL_SKY_DAWN_HORIZON_COLOR, dawnDuskSide);
    vec3 twilightEdge = mix(NL_SKY_DUSK_EDGE_COLOR, NL_SKY_DAWN_EDGE_COLOR, dawnDuskSide);

    vec3 baseZenith = mix(NL_SKY_NIGHT_ZENITH_COLOR, NL_SKY_DAY_ZENITH_COLOR, dayFactor);
    vec3 baseHorizon = mix(NL_SKY_NIGHT_HORIZON_COLOR, NL_SKY_DAY_HORIZON_COLOR, dayFactor);
    vec3 baseEdge = mix(NL_SKY_NIGHT_EDGE_COLOR, NL_SKY_DAY_EDGE_COLOR, dayFactor);

    zenithColor = mix(baseZenith, twilightZenith, twilightFactor);
    horizonColor = mix(baseHorizon, twilightHorizon, twilightFactor);
    edgeColor = mix(baseEdge, twilightEdge, twilightFactor);
}

void main() {
    vec3 dir = normalize(v_worldPos);
    float horizonFactor = 1.0 - clamp(dir.y, 0.0, 1.0);

    float blend = smoothstep(0.0, 1.0, horizonFactor);
    blend = pow(blend, NL_SKY_HORIZON_SHARPNESS);

    vec3 zenithColor;
    vec3 horizonColor;
    vec3 edgeColor;
    nl_skyPaletteColors(zenithColor, horizonColor, edgeColor);

    vec3 skyColor = mix(zenithColor, horizonColor, blend);

    float edgeFade = smoothstep(NL_SKY_EDGE_START, NL_SKY_EDGE_END, horizonFactor);
    skyColor = mix(skyColor, edgeColor, edgeFade * NL_SKY_EDGE_STRENGTH);

    gl_FragColor = vec4(skyColor, 1.0);
}
