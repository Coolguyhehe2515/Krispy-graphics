$input v_worldPos
precision highp float;
#include "bgfx_shader.sh"
#include "settings.h"

uniform vec4 FogColor;
uniform vec4 ViewPositionAndTime;
uniform vec4 TimeOfDay;

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

// Simple pseudo-random generator — turns any float into a "random-looking" 0-1 value.
// Used everywhere below to fake randomness without a real noise texture.
float nl_hash(float n) {
    return fract(sin(n) * 43758.5453123);
}

float nl_noise1D(float x) {
    float i = floor(x);
    float f = fract(x);
    f = f * f * (3.0 - 2.0 * f);
    return mix(nl_hash(i), nl_hash(i + 1.0), f);
}

vec3 nl_aurora(vec3 viewDir, float t) {
    if (viewDir.y < NL_AURORA_HEIGHT_MIN) {
        return vec3(0.0);
    }

    // Angle around the horizon — used as the noise coordinate so bands wrap
    // fully around the sky like real vertical aurora curtains.
    float angle = atan(viewDir.x, viewDir.z);

    // Two overlapping noise layers, animated at slightly different speeds,
    // creates the wavy/shifting look instead of a static pattern.
    float n = nl_noise1D(angle * NL_AURORA_SCALE + t * NL_AURORA_SPEED);
    n += 0.5 * nl_noise1D(angle * NL_AURORA_SCALE * 2.3 - t * NL_AURORA_SPEED * 1.7);
    n /= 1.5;

    // Sharpen into thin bright rays instead of a soft blob.
    float rays = pow(n, NL_AURORA_RAY_SHARPNESS);

    // Fade in gradually starting near the horizon, stay visible across most of the
    // sky, and only fade out again in the last sliver near true zenith.
    float heightFactor = smoothstep(NL_AURORA_HEIGHT_MIN, NL_AURORA_HEIGHT_MIN + 0.5, viewDir.y);
    heightFactor *= 1.0 - smoothstep(0.97, 1.0, viewDir.y);

    vec3 color = mix(NL_AURORA_COLOR_BOTTOM, NL_AURORA_COLOR_TOP, clamp((viewDir.y - NL_AURORA_HEIGHT_MIN) * 2.0, 0.0, 1.0));

    return color * rays * heightFactor * NL_AURORA_BRIGHTNESS;
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
        skyColor += nl_aurora(viewDir, ViewPositionAndTime.w);
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
