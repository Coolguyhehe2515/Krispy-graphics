$input v_worldPos
precision highp float;
#include "bgfx_shader.sh"
#include "settings.h"

uniform vec4 FogColor;

float nl_dayFactor(vec3 fogColor) {
    float brightness = dot(fogColor, vec3(0.33, 0.33, 0.33));
    return clamp((brightness - 0.05) * 3.0, 0.0, 1.0);
}

float nl_rainFactor(vec3 fogColor) {
    // Rain skies go gray/desaturated — low color variance signals rain.
    float maxC = max(fogColor.r, max(fogColor.g, fogColor.b));
    float minC = min(fogColor.r, min(fogColor.g, fogColor.b));
    float saturation = maxC - minC;
    return clamp(1.0 - saturation * 6.0, 0.0, 1.0);
}

void nl_skyPaletteColors(vec3 fogColor, out vec3 zenithColor, out vec3 horizonColor, out vec3 edgeColor) {
    float dayFactor = nl_dayFactor(fogColor);

    float warmth = fogColor.r - fogColor.b;
    float twilightFactor = clamp(warmth * 2.5, 0.0, 1.0) * (1.0 - abs(dayFactor - 0.5) * 0.6);

    vec3 twilightZenith = NL_SKY_DAWN_ZENITH_COLOR;
    vec3 twilightHorizon = NL_SKY_DAWN_HORIZON_COLOR;
    vec3 twilightEdge = NL_SKY_DAWN_EDGE_COLOR;

    vec3 baseZenith = mix(NL_SKY_NIGHT_ZENITH_COLOR, NL_SKY_DAY_ZENITH_COLOR, dayFactor);
    vec3 baseHorizon = mix(NL_SKY_NIGHT_HORIZON_COLOR, NL_SKY_DAY_HORIZON_COLOR, dayFactor);
    vec3 baseEdge = mix(NL_SKY_NIGHT_EDGE_COLOR, NL_SKY_DAY_EDGE_COLOR, dayFactor);

    zenithColor = mix(baseZenith, twilightZenith, twilightFactor);
    horizonColor = mix(baseHorizon, twilightHorizon, twilightFactor);
    edgeColor = mix(baseEdge, twilightEdge, twilightFactor);
}

void main() {
    vec3 viewDir = normalize(v_worldPos);
    float horizonFactor = 1.0 - clamp(viewDir.y, 0.0, 1.0);

    float blend = smoothstep(0.0, 1.0, horizonFactor);
    blend = pow(blend, NL_SKY_HORIZON_SHARPNESS);

    vec3 zenithColor, horizonColor, edgeColor;
    nl_skyPaletteColors(FogColor.rgb, zenithColor, horizonColor, edgeColor);

    vec3 skyColor = mix(zenithColor, horizonColor, blend);

    float edgeFade = smoothstep(NL_SKY_EDGE_START, NL_SKY_EDGE_END, horizonFactor);
    skyColor = mix(skyColor, edgeColor, edgeFade * NL_SKY_EDGE_STRENGTH);

    float rain = nl_rainFactor(FogColor.rgb);
    skyColor *= mix(1.0, 1.0 - NL_RAIN_DARKEN_STRENGTH, rain);

    gl_FragColor = vec4(skyColor, 1.0);
}
