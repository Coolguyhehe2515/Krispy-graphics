$input v_worldPos
precision highp float;
#include "bgfx_shader.sh"
#include "settings.h"

uniform vec4 FogColor;
uniform vec4 ViewPositionAndTime;
uniform vec4 TimeOfDay;

SAMPLER2D_AUTOREG(s_NoiseVoxel);

float nl_pow2(float x) { return x * x; }
float nl_clamp01(float x) { return clamp(x, 0.0, 1.0); }
float nl_sqrt1(float x) { return sqrt(max(x, 0.0)); }

vec3 nl_getAurora(vec3 vDir, float time, float dither) {
    float VdotU = clamp(vDir.y, 0.0, 1.0);
    float visibility = nl_sqrt1(nl_clamp01(VdotU * 4.5 - 0.225));
    visibility *= 4.0 - VdotU * 0.9;
    if (visibility <= 1.0) return vec3(0.0);

    vec3 aurora = vec3(0.0);
    vec3 wpos = vDir;
    wpos.xz /= max(wpos.y, 0.1);
    vec2 cameraPosM = vec2(0.0);
    cameraPosM.x += time * 10.0;

    const int sampleCount = 7;
    const int sampleCountP = sampleCount + 10;

    float ditherM = dither + 10.0;
    float auroraAnimate = time * 0.0;

    for (int i = 0; i < sampleCount; i++) {
        float current = nl_pow2((float(i) + ditherM) / float(sampleCountP));
        vec2 planePos = wpos.xz * (0.8 + current) * 10.0 + cameraPosM;
        planePos *= 0.0007;
        float noise = texture(s_NoiseVoxel, planePos).r;
        noise = nl_pow2(nl_pow2(nl_pow2(nl_pow2(1.0 - 0.8 * abs(noise - 0.5)))));
        noise *= texture(s_NoiseVoxel, planePos * 8.0 + auroraAnimate).b;
        noise *= texture(s_NoiseVoxel, planePos * 1.0 - auroraAnimate).g;
        float currentM = 1.0 - current;
        aurora += noise * currentM * mix(vec3(0.65, 0.48, 1.05), vec3(0.0, 4.5, 3.0), nl_pow2(nl_pow2(currentM)));
    }

    aurora *= 3.8;
    return aurora * visibility / float(sampleCount);
}

float nl_sunHeight(float timeOfDay) {
    float t = 2.0 * 3.14159265 * timeOfDay;
    return cos(t);
}

float nl_dayFactorFromSun(float sunHeight) {
    return clamp(sunHeight * 0.5 + 0.5, 0.0, 1.0);
}

float nl_twilightFactorFromSun(float sunHeight) {
    float dawnFactor = clamp(1.0 - sunHeight * sunHeight, 0.0, 1.0);
    dawnFactor *= dawnFactor * dawnFactor;
    return dawnFactor;
}

float nl_rainFactor(vec3 fogColor) {
    float maxC = max(fogColor.r, max(fogColor.g, fogColor.b));
    float minC = min(fogColor.r, min(fogColor.g, fogColor.b));
    float saturation = maxC - minC;
    return clamp(1.0 - saturation * 6.0, 0.0, 1.0);
}

float nl_hash(float n) {
    return fract(sin(n) * 43758.5453123);
}

bool nl_shootingStarSpawn(float t, out float seed, out float cycle) {
    cycle = floor(t * NL_SHOOTING_STAR_FREQUENCY / 6.28318);
    seed = nl_hash(cycle);
    return seed < NL_SHOOTING_STAR_SPAWN_CHANCE;
}

float nl_shootingStarStreak(vec3 viewDir, float t, float seed, float cycle) {
    float localT = fract(t * NL_SHOOTING_STAR_SPEED + seed * 10.0);

    vec3 starDir = normalize(vec3(
        nl_hash(cycle * 2.0) * 2.0 - 1.0,
        0.3 + nl_hash(cycle * 3.0) * 0.5,
        nl_hash(cycle * 4.0) * 2.0 - 1.0
    ));

    vec3 starTrailDir = normalize(vec3(
        nl_hash(cycle * 5.0) * 2.0 - 1.0,
        -0.2,
        nl_hash(cycle * 6.0) * 2.0 - 1.0
    ));

    vec3 headPos = normalize(starDir + starTrailDir * localT * 2.0);

    float distToStreak = length(cross(viewDir, headPos));
    float alongStreak = dot(viewDir, headPos);

    float streak = smoothstep(NL_SHOOTING_STAR_WIDTH, 0.0, distToStreak);
    streak *= smoothstep(0.0, NL_SHOOTING_STAR_LENGTH, alongStreak);
    streak *= smoothstep(1.0, 0.99, localT);

    return streak * NL_SHOOTING_STAR_BRIGHTNESS;
}

void main() {
    vec3 viewDir = normalize(v_worldPos);
    float horizonFactor = 1.0 - clamp(viewDir.y, 0.0, 1.0);

    float blend = smoothstep(0.0, 1.0, horizonFactor);
    blend = pow(blend, NL_SKY_HORIZON_SHARPNESS);

    float sunHeight = nl_sunHeight(TimeOfDay.x);
    float dayFactor = nl_dayFactorFromSun(sunHeight);
    float twilight = nl_twilightFactorFromSun(sunHeight);

    vec3 baseZenith = mix(NL_SKY_NIGHT_ZENITH_COLOR, NL_SKY_DAY_ZENITH_COLOR, dayFactor);
    vec3 baseHorizon = mix(NL_SKY_NIGHT_HORIZON_COLOR, NL_SKY_DAY_HORIZON_COLOR, dayFactor);
    vec3 baseEdge = mix(NL_SKY_NIGHT_EDGE_COLOR, NL_SKY_DAY_EDGE_COLOR, dayFactor);

    vec3 zenithColor = mix(baseZenith, NL_SKY_TWILIGHT_ZENITH_COLOR, twilight);
    vec3 horizonColor = mix(baseHorizon, NL_SKY_TWILIGHT_HORIZON_COLOR, twilight);
    vec3 edgeColor = mix(baseEdge, NL_SKY_TWILIGHT_EDGE_COLOR, twilight);

    vec3 skyColor = mix(zenithColor, horizonColor, blend);

    float edgeFade = smoothstep(NL_SKY_EDGE_START, NL_SKY_EDGE_END, horizonFactor);
    skyColor = mix(skyColor, edgeColor, edgeFade * NL_SKY_EDGE_STRENGTH);

    float rain = nl_rainFactor(FogColor.rgb);
    skyColor *= mix(1.0, 1.0 - NL_RAIN_DARKEN_STRENGTH, rain);

    #if NL_AURORA_ENABLED
    if (dayFactor < 0.1 && rain < 0.3) {
        float dither = fract(sin(dot(gl_FragCoord.xy, vec2(12.9898, 78.233))) * 43758.5453);
        vec3 aurora = nl_getAurora(viewDir, ViewPositionAndTime.w, dither);
        skyColor += aurora * NL_AURORA_BRIGHTNESS;
    }
    #endif

    #if NL_SHOOTING_STAR_ENABLED
    if (dayFactor < 0.15 && rain < 0.3) {
        float seed, cycle;
        if (nl_shootingStarSpawn(ViewPositionAndTime.w, seed, cycle)) {
            float star = nl_shootingStarStreak(viewDir, ViewPositionAndTime.w, seed, cycle);
            skyColor += vec3_splat(star);
        }
    }
    #endif

    gl_FragColor = vec4(skyColor, 1.0);
}
