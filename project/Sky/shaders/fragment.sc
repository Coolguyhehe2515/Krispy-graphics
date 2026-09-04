$input v_worldPos
precision highp float;
#include "bgfx_shader.sh"
#include "settings.h"

uniform vec4 FogColor;
uniform vec4 ViewPositionAndTime;

vec3 nl_tameFogTint(vec3 fogColor) {
    float luma = dot(fogColor, vec3(0.299, 0.587, 0.114));
    return mix(fogColor, vec3_splat(luma), NL_FOG_TINT_DESATURATE);
}

float nl_dayFactor(vec3 fogColor) {
    float brightness = dot(fogColor, vec3(0.33, 0.33, 0.33));
    return clamp((brightness - 0.05) * 3.0, 0.0, 1.0);
}

float nl_twilightFactor(vec3 fogColor, float dayFactor) {
    float warmth = fogColor.r - fogColor.b;
    return clamp(warmth * 1.5, 0.0, 1.0) * (1.0 - abs(dayFactor - 0.5) * 0.6);
}

float nl_rainFactor(vec3 fogColor) {
    float maxC = max(fogColor.r, max(fogColor.g, fogColor.b));
    float minC = min(fogColor.r, min(fogColor.g, fogColor.b));
    float saturation = maxC - minC;
    return clamp(1.0 - saturation * 6.0, 0.0, 1.0);
}

void nl_skyPaletteColors(vec3 fogColor, float dayFactor, out vec3 zenithColor, out vec3 horizonColor, out vec3 edgeColor) {
    float twilight = nl_twilightFactor(fogColor, dayFactor);

    vec3 baseZenith = mix(NL_SKY_NIGHT_ZENITH_COLOR, NL_SKY_DAY_ZENITH_COLOR, dayFactor);
    vec3 baseHorizon = mix(NL_SKY_NIGHT_HORIZON_COLOR, NL_SKY_DAY_HORIZON_COLOR, dayFactor);
    vec3 baseEdge = mix(NL_SKY_NIGHT_EDGE_COLOR, NL_SKY_DAY_EDGE_COLOR, dayFactor);

    zenithColor = mix(baseZenith, NL_SKY_TWILIGHT_ZENITH_COLOR, twilight);
    horizonColor = mix(baseHorizon, NL_SKY_TWILIGHT_HORIZON_COLOR, twilight);
    edgeColor = mix(baseEdge, NL_SKY_TWILIGHT_EDGE_COLOR, twilight);
}

// Simple pseudo-random generator — turns any float into a "random-looking" 0-1 value.
// Used everywhere below to fake randomness without a real noise texture.
float nl_hash(float n) {
    return fract(sin(n) * 43758.5453123);
}

// Decides IF a shooting star exists during this time window ("cycle"), and returns
// a seed value unique to that cycle if one does. Keeping spawn logic separate from
// the actual streak drawing means we can tune "how often" independently of "what it looks like".
//
// How it works: time is divided into repeating cycles (controlled by NL_SHOOTING_STAR_FREQUENCY).
// Each cycle gets a fixed random number from nl_hash(). If that number falls under
// NL_SHOOTING_STAR_SPAWN_CHANCE, this cycle "wins" and produces a star; otherwise, no star
// this cycle at all — the sky just stays empty for that window.
bool nl_shootingStarSpawn(float t, out float seed, out float cycle) {
    cycle = floor(t * NL_SHOOTING_STAR_FREQUENCY / 6.28318);
    seed = nl_hash(cycle);
    return seed < NL_SHOOTING_STAR_SPAWN_CHANCE;
}

// Draws the actual streak for a star that has already been confirmed to spawn this cycle.
// viewDir = the direction the camera is currently looking (per-pixel).
// t = current time, used to animate the star moving across the sky during its cycle.
// seed / cycle = unique values for this cycle, used to randomize the star's direction and path
// so every shooting star doesn't look identical.
float nl_shootingStarStreak(vec3 viewDir, float t, float seed, float cycle) {
    // How far through this star's short lifetime we are, looping 0 to 1.
    float localT = fract(t * NL_SHOOTING_STAR_SPEED + seed * 10.0);

    // Pick a random starting direction for this cycle's star, biased upward
    // (y component starts at 0.3+ so stars don't spawn low near the horizon).
    vec3 starDir = normalize(vec3(
        nl_hash(cycle * 2.0) * 2.0 - 1.0,
        0.3 + nl_hash(cycle * 3.0) * 0.5,
        nl_hash(cycle * 4.0) * 2.0 - 1.0
    ));

    // Pick a random travel direction — this is the path the star streaks along,
    // generally angled downward so it looks like it's falling.
    vec3 starTrailDir = normalize(vec3(
        nl_hash(cycle * 5.0) * 2.0 - 1.0,
        -0.2,
        nl_hash(cycle * 6.0) * 2.0 - 1.0
    ));

    // Current head position of the star, moving along its trail over the cycle's lifetime.
    vec3 headPos = normalize(starDir + starTrailDir * localT * 2.0);

    // Measure how close the current pixel's view direction is to the star's line,
    // and how far along that line we are — used to draw a thin, fading streak shape.
    float distToStreak = length(cross(viewDir, headPos));
    float alongStreak = dot(viewDir, headPos);

    float streak = smoothstep(NL_SHOOTING_STAR_WIDTH, 0.0, distToStreak);
    streak *= smoothstep(0.0, NL_SHOOTING_STAR_LENGTH, alongStreak);
    streak *= smoothstep(1.0, 0.99, localT); // Quick fade-out right at the end of its life.

    return streak * NL_SHOOTING_STAR_BRIGHTNESS;
}

void main() {
    vec3 viewDir = normalize(v_worldPos);
    float horizonFactor = 1.0 - clamp(viewDir.y, 0.0, 1.0);

    float blend = smoothstep(0.0, 1.0, horizonFactor);
    blend = pow(blend, NL_SKY_HORIZON_SHARPNESS);

    float dayFactor = nl_dayFactor(FogColor.rgb);
    vec3 zenithColor, horizonColor, edgeColor;
    nl_skyPaletteColors(FogColor.rgb, dayFactor, zenithColor, horizonColor, edgeColor);

    vec3 skyColor = mix(zenithColor, horizonColor, blend);

    float edgeFade = smoothstep(NL_SKY_EDGE_START, NL_SKY_EDGE_END, horizonFactor);
    skyColor = mix(skyColor, edgeColor, edgeFade * NL_SKY_EDGE_STRENGTH);

    float rain = nl_rainFactor(FogColor.rgb);
    skyColor *= mix(1.0, 1.0 - NL_RAIN_DARKEN_STRENGTH, rain);

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
