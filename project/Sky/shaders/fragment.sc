$input v_worldPos
precision highp float;
#include "bgfx_shader.sh"
#include "settings.h"

uniform vec4 FogColor;
uniform vec4 ViewPositionAndTime;
uniform vec4 TimeOfDay;

SAMPLER2D_AUTOREG(s_NoiseVoxel);

float pow2(float x) { return x * x; }
float clamp01(float x) { return clamp(x, 0.0, 1.0); }
float sqrt1(float x) { return sqrt(max(x, 0.0)); }

float nl_hash(float n) {
    return fract(sin(n) * 43758.5453123);
}

float nl_noise1D(float x) {
    float i = floor(x);
    float f = fract(x);
    f = f * f * (3.0 - 2.0 * f);
    return mix(nl_hash(i), nl_hash(i + 1.0), f);
}

vec3 nl_getAurora(vec3 vDir, float time) {
    float VdotU = clamp(vDir.y, 0.0, 1.0);
    float visibility = smoothstep(0.05, 0.35, VdotU) * (1.0 - smoothstep(0.75, 1.0, VdotU));

    if (visibility <= 0.01) return vec3(0.0);

    vec3 wpos = vDir;
    wpos.xz /= max(wpos.y, 0.1);
    float angle = atan(wpos.x, wpos.z);

    // Normalized height within the visible band: 0 = bottom, 1 = top.
    float heightT = clamp((VdotU - 0.05) / (0.70 - 0.05), 0.0, 1.0);

    // Keep time-based values bounded — sin()-based hashing loses precision
    // badly on mobile GPUs once the input grows into the thousands, which
    // silently breaks randomness (everything evaluates the same way).
    float boundedTime = mod(time, 1000.0);

    // --- Blocky green segments (hard-edged grid, no smoothing) ---
    float col = floor(angle * NL_AURORA_BLOCK_COLUMNS / 6.28318 + boundedTime * NL_AURORA_SCROLL_SPEED);
    float row = floor(heightT * NL_AURORA_BLOCK_ROWS);
    float flicker = floor(boundedTime * NL_AURORA_FLICKER_SPEED);

    float blockNoise = nl_hash(col * 12.9898 + row * 78.233 + flicker * 37.7);
    float blockActive = step(NL_AURORA_BLOCK_THRESHOLD, blockNoise);

    // Only appears in the lower portion of the band, fully hard-edged.
    float greenZone = 1.0 - smoothstep(0.0, 0.5, heightT);
    float green = blockActive * greenZone;

    // --- Smooth purple fade above the blocky section ---
    float purple = smoothstep(0.3, 0.7, heightT) * (1.0 - smoothstep(0.85, 1.0, heightT));

    vec3 color = NL_AURORA_GREEN_COLOR * green + NL_AURORA_PURPLE_COLOR * purple;

    return color * visibility;
}

float nl_sunHeight(float timeOfDay) {
    float t = 2.0 * 3.14159265 * timeOfDay;
    return cos(t);
}

vec3 nl_sunDirection(float timeOfDay) {
    float t = 2.0 * 3.14159265 * timeOfDay;
    return normalize(vec3(-sin(t), cos(t), 0.0));
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

vec3 nl_godrays(vec3 viewDir, vec3 sunDir, float twilight, float t) {
    float sunDot = dot(viewDir, sunDir);
    float raysMask = pow(clamp(sunDot, 0.0, 1.0), NL_GODRAY_SHARPNESS);

    if (raysMask <= 0.001) return vec3(0.0);

    vec3 tangent = normalize(cross(sunDir, vec3(0.0, 1.0, 0.0)) + vec3(0.0001));
    vec3 bitangent = cross(sunDir, tangent);
    float u = dot(viewDir, tangent);
    float v = dot(viewDir, bitangent);
    float angle = atan(v, u);

    float streaks = nl_noise1D(angle * NL_GODRAY_STREAK_SCALE + t * 0.02);
    streaks = pow(streaks, 2.0);

    float intensity = raysMask * streaks * twilight;
    return NL_GODRAY_COLOR * intensity * NL_GODRAY_BRIGHTNESS;
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
    vec3 sunDir = nl_sunDirection(TimeOfDay.x);
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

    #if NL_GODRAY_ENABLED
    if (twilight > 0.05 && rain < 0.3) {
        skyColor += nl_godrays(viewDir, sunDir, twilight * (1.0 - rain), ViewPositionAndTime.w);
    }
    #endif

    #if NL_AURORA_ENABLED
    float auroraMask = (1.0 - rain) * max(1.0 - 3.0 * max(FogColor.g, FogColor.b), 0.0);
    vec3 aurora = nl_getAurora(viewDir, ViewPositionAndTime.w) * auroraMask;
    skyColor += aurora * NL_AURORA_BRIGHTNESS;
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
