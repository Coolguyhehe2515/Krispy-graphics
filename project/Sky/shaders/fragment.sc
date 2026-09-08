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

float nl_blockNoise(vec2 p) {
    vec2 cell = floor(p);
    float h = nl_hash(cell.x * 127.1 + cell.y * 311.7);
    return h;
}

float nl_blockSmoothNoise(vec2 p) {
    vec2 cell = floor(p);
    vec2 f = fract(p);

    f = f * f * (3.0 - 2.0 * f);

    float a = nl_hash(cell.x * 127.1 + cell.y * 311.7);
    float b = nl_hash((cell.x + 1.0) * 127.1 + cell.y * 311.7);
    float c = nl_hash(cell.x * 127.1 + (cell.y + 1.0) * 311.7);
    float d = nl_hash((cell.x + 1.0) * 127.1 + (cell.y + 1.0) * 311.7);

    return mix(
        mix(a, b, f.x),
        mix(c, d, f.x),
        f.y
    );
}

vec3 nl_getAurora(vec3 vDir, float time, float dither) {
    float VdotU = clamp(vDir.y, 0.0, 1.0);

    float visibility =
        smoothstep(NL_AURORA_HEIGHT_MIN, 0.32, VdotU) *
        (1.0 - smoothstep(0.78, 1.0, VdotU));

    if (visibility <= 0.01) return vec3(0.0);

    vec3 aurora = vec3(0.0);

    vec3 wpos = vDir;
    wpos.xz /= max(wpos.y, 0.1);

    float auroraTime = time * NL_AURORA_SPEED;

    /*
     * Convert the view direction into a large sky-space coordinate.
     * The quantization below is what gives the Aurora its large,
     * broken rectangular/polygonal sections.
     */
    float angle = atan(wpos.x, wpos.z);

    float blockCount = max(NL_AURORA_SCALE * 4.0, 4.0);

    float angularCoord =
        (angle / 6.2831853 + 0.5) * blockCount;

    float blockIndex = floor(angularCoord);
    float blockFraction = fract(angularCoord);

    /*
     * Generate neighboring block heights.
     * Using discrete cells instead of continuous noise creates
     * the large stepped Aurora silhouette.
     */
    float h0 = nl_blockNoise(vec2(blockIndex, 17.0));
    float h1 = nl_blockNoise(vec2(blockIndex + 1.0, 17.0));
    float h2 = nl_blockNoise(vec2(blockIndex - 1.0, 17.0));

    /*
     * Slowly move the block pattern sideways.
     */
    float movement =
        auroraTime * 0.12 +
        sin(auroraTime * 0.7) * 0.35;

    float movingCoord = angularCoord + movement;
    float movingBlock = floor(movingCoord);
    float movingFraction = fract(movingCoord);

    float currentBlockHeight =
        nl_blockNoise(vec2(movingBlock, 17.0));

    float nextBlockHeight =
        nl_blockNoise(vec2(movingBlock + 1.0, 17.0));

    /*
     * Mostly stepped interpolation.
     * This prevents the Aurora from becoming a perfectly smooth wave.
     */
    float shapeBlend = smoothstep(
        0.15,
        0.85,
        movingFraction
    );

    float blockHeight =
        mix(currentBlockHeight, nextBlockHeight, shapeBlend);

    /*
     * Add a second large-scale layer so the silhouette isn't
     * just one straight strip.
     */
    float secondary =
        nl_blockSmoothNoise(
            vec2(
                wpos.x * 1.8 + auroraTime * 0.35,
                wpos.z * 1.8
            )
        );

    /*
     * Quantize the secondary shape.
     * Fewer steps = larger, more visible blocks.
     */
    secondary = floor(secondary * 7.0) / 7.0;

    /*
     * Shape of the Aurora curtain.
     */
    float curtainHeight =
        0.28 +
        blockHeight * 0.22 +
        secondary * 0.12;

    /*
     * Create several depth layers.
     */
    const int sampleCount = 18;

    float ditherM = dither;

    for (int i = 0; i < sampleCount; i++) {
        float current =
            pow2(
                (float(i) + ditherM) /
                float(sampleCount + 14)
            );

        /*
         * Slight perspective movement.
         */
        float layerOffset =
            current * 0.8 +
            auroraTime * 0.03;

        /*
         * Large rectangular cells.
         */
        vec2 blockPos =
            wpos.xz * (1.0 + current * 0.75);

        blockPos *= 2.8;

        blockPos.x += auroraTime * 0.22;
        blockPos.y -= auroraTime * 0.08;

        /*
         * Quantized coordinates.
         */
        vec2 cell = floor(blockPos * 1.35);

        float cellNoise =
            nl_hash(
                cell.x * 127.1 +
                cell.y * 311.7
            );

        /*
         * Quantize the vertical position.
         */
        float verticalSteps = 10.0;

        float quantizedCurrent =
            floor(
                (current + cellNoise * 0.035) *
                verticalSteps
            ) / verticalSteps;

        /*
         * Combine the angular curtain shape and block variation.
         */
        float localHeight =
            curtainHeight +
            (cellNoise - 0.5) * 0.055;

        /*
         * Main rectangular Aurora body.
         */
        float distanceToBand =
            abs(quantizedCurrent - localHeight);

        /*
         * Slightly softened edges.
         * Lower values produce harder edges.
         */
        float band =
            1.0 -
            smoothstep(
                0.018,
                0.075,
                distanceToBand
            );

        /*
         * Break the curtain into large vertical sections.
         */
        float blockMask =
            step(0.18, cellNoise);

        /*
         * Add a few deliberate dark gaps between sections.
         */
        float gapPattern =
            sin(
                floor(
                    (angle + auroraTime * 0.025) *
                    blockCount
                ) * 1.73
            );

        gapPattern =
            smoothstep(
                -0.25,
                0.25,
                gapPattern
            );

        blockMask *= mix(0.65, 1.0, gapPattern);

        band *= blockMask;

        /*
         * Vertical rays inside the Aurora.
         * These remain sharper than the old smooth ray pattern.
         */
        float rayCoord =
            angle * blockCount * 1.7 +
            cellNoise * 2.0 +
            auroraTime * 0.8;

        float rayPattern =
            abs(sin(rayCoord));

        rayPattern =
            pow(
                rayPattern,
                max(NL_AURORA_RAY_SHARPNESS, 1.0)
            );

        /*
         * Don't let the rays completely destroy the block shape.
         */
        band *= mix(
            0.35,
            1.0,
            rayPattern
        );

        /*
         * Layer intensity.
         */
        float currentM =
            1.0 - current;

        /*
         * Top = cyan/green.
         * Bottom = purple.
         */
        vec3 layerColor =
            mix(
                NL_AURORA_COLOR_BOTTOM,
                NL_AURORA_COLOR_TOP,
                currentM
            );

        /*
         * Add a brighter cyan rim toward the upper part.
         */
        float upperGlow =
            smoothstep(
                0.55,
                0.95,
                currentM
            );

        layerColor +=
            NL_AURORA_COLOR_TOP *
            upperGlow *
            0.35;

        /*
         * Depth falloff.
         */
        float depthFade =
            mix(
                1.0,
                0.15,
                current
            );

        aurora +=
            layerColor *
            band *
            depthFade;
    }

    /*
     * Overall Aurora intensity.
     */
    aurora *= 2.8;

    /*
     * Prevent the Aurora from becoming completely opaque.
     */
    aurora *= visibility;

    return aurora / float(sampleCount);
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
    float dawnFactor =
        clamp(
            1.0 - sunHeight * sunHeight,
            0.0,
            1.0
        );

    dawnFactor *=
        dawnFactor *
        dawnFactor;

    return dawnFactor;
}

float nl_rainFactor(vec3 fogColor) {
    float maxC =
        max(
            fogColor.r,
            max(fogColor.g, fogColor.b)
        );

    float minC =
        min(
            fogColor.r,
            min(fogColor.g, fogColor.b)
        );

    float saturation =
        maxC - minC;

    return clamp(
        1.0 - saturation * 6.0,
        0.0,
        1.0
    );
}

float nl_noise1D(float x) {
    float i = floor(x);
    float f = fract(x);

    f =
        f * f *
        (3.0 - 2.0 * f);

    return mix(
        nl_hash(i),
        nl_hash(i + 1.0),
        f
    );
}

vec3 nl_godrays(
    vec3 viewDir,
    vec3 sunDir,
    float twilight,
    float t
) {
    float sunDot =
        dot(viewDir, sunDir);

    float raysMask =
        pow(
            clamp(sunDot, 0.0, 1.0),
            NL_GODRAY_SHARPNESS
        );

    if (raysMask <= 0.001)
        return vec3(0.0);

    vec3 tangent =
        normalize(
            cross(
                sunDir,
                vec3(0.0, 1.0, 0.0)
            ) +
            vec3(0.0001)
        );

    vec3 bitangent =
        cross(sunDir, tangent);

    float u =
        dot(viewDir, tangent);

    float v =
        dot(viewDir, bitangent);

    float angle =
        atan(v, u);

    float streaks =
        nl_noise1D(
            angle *
            NL_GODRAY_STREAK_SCALE +
            t * 0.02
        );

    streaks =
        pow(streaks, 2.0);

    float intensity =
        raysMask *
        streaks *
        twilight;

    return
        NL_GODRAY_COLOR *
        intensity *
        NL_GODRAY_BRIGHTNESS;
}

bool nl_shootingStarSpawn(
    float t,
    out float seed,
    out float cycle
) {
    cycle =
        floor(
            t *
            NL_SHOOTING_STAR_FREQUENCY /
            6.28318
        );

    seed =
        nl_hash(cycle);

    return
        seed <
        NL_SHOOTING_STAR_SPAWN_CHANCE;
}

float nl_shootingStarStreak(
    vec3 viewDir,
    float t,
    float seed,
    float cycle
) {
    float localT =
        fract(
            t *
            NL_SHOOTING_STAR_SPEED +
            seed * 10.0
        );

    vec3 starDir =
        normalize(
            vec3(
                nl_hash(cycle * 2.0) * 2.0 - 1.0,
                0.3 +
                    nl_hash(cycle * 3.0) * 0.5,
                nl_hash(cycle * 4.0) * 2.0 - 1.0
            )
        );

    vec3 starTrailDir =
        normalize(
            vec3(
                nl_hash(cycle * 5.0) * 2.0 - 1.0,
                -0.2,
                nl_hash(cycle * 6.0) * 2.0 - 1.0
            )
        );

    vec3 headPos =
        normalize(
            starDir +
            starTrailDir *
            localT *
            2.0
        );

    float distToStreak =
        length(
            cross(viewDir, headPos)
        );

    float alongStreak =
        dot(viewDir, headPos);

    float streak =
        smoothstep(
            NL_SHOOTING_STAR_WIDTH,
            0.0,
            distToStreak
        );

    streak *=
        smoothstep(
            0.0,
            NL_SHOOTING_STAR_LENGTH,
            alongStreak
        );

    streak *=
        smoothstep(
            1.0,
            0.99,
            localT
        );

    return
        streak *
        NL_SHOOTING_STAR_BRIGHTNESS;
}

void main() {
    vec3 viewDir =
        normalize(v_worldPos);

    float horizonFactor =
        1.0 -
        clamp(
            viewDir.y,
            0.0,
            1.0
        );

    float blend =
        smoothstep(
            0.0,
            1.0,
            horizonFactor
        );

    blend =
        pow(
            blend,
            NL_SKY_HORIZON_SHARPNESS
        );

    float sunHeight =
        nl_sunHeight(
            TimeOfDay.x
        );

    vec3 sunDir =
        nl_sunDirection(
            TimeOfDay.x
        );

    float dayFactor =
        nl_dayFactorFromSun(
            sunHeight
        );

    float twilight =
        nl_twilightFactorFromSun(
            sunHeight
        );

    vec3 baseZenith =
        mix(
            NL_SKY_NIGHT_ZENITH_COLOR,
            NL_SKY_DAY_ZENITH_COLOR,
            dayFactor
        );

    vec3 baseHorizon =
        mix(
            NL_SKY_NIGHT_HORIZON_COLOR,
            NL_SKY_DAY_HORIZON_COLOR,
            dayFactor
        );

    vec3 baseEdge =
        mix(
            NL_SKY_NIGHT_EDGE_COLOR,
            NL_SKY_DAY_EDGE_COLOR,
            dayFactor
        );

    vec3 zenithColor =
        mix(
            baseZenith,
            NL_SKY_TWILIGHT_ZENITH_COLOR,
            twilight
        );

    vec3 horizonColor =
        mix(
            baseHorizon,
            NL_SKY_TWILIGHT_HORIZON_COLOR,
            twilight
        );

    vec3 edgeColor =
        mix(
            baseEdge,
            NL_SKY_TWILIGHT_EDGE_COLOR,
            twilight
        );

    vec3 skyColor =
        mix(
            zenithColor,
            horizonColor,
            blend
        );

    float edgeFade =
        smoothstep(
            NL_SKY_EDGE_START,
            NL_SKY_EDGE_END,
            horizonFactor
        );

    skyColor =
        mix(
            skyColor,
            edgeColor,
            edgeFade *
            NL_SKY_EDGE_STRENGTH
        );

    float rain =
        nl_rainFactor(
            FogColor.rgb
        );

    skyColor *=
        mix(
            1.0,
            1.0 -
                NL_RAIN_DARKEN_STRENGTH,
            rain
        );

    #if NL_GODRAY_ENABLED
    if (twilight > 0.05 && rain < 0.3) {
        skyColor +=
            nl_godrays(
                viewDir,
                sunDir,
                twilight *
                    (1.0 - rain),
                ViewPositionAndTime.w
            );
    }
    #endif

    #if NL_AURORA_ENABLED
    float dither =
        texture(
            s_NoiseVoxel,
            mod(
                gl_FragCoord.xy,
                256.0
            ) / 256.0
        ).r;

    float auroraMask =
        (1.0 - rain) *
        max(
            1.0 -
                3.0 *
                max(
                    FogColor.g,
                    FogColor.b
                ),
            0.0
        );

    vec3 aurora =
        nl_getAurora(
            viewDir,
            ViewPositionAndTime.w,
            dither
        ) *
        auroraMask;

    skyColor +=
        aurora *
        NL_AURORA_BRIGHTNESS;
    #endif

    #if NL_SHOOTING_STAR_ENABLED
    if (dayFactor < 0.15 && rain < 0.3) {
        float seed;
        float cycle;

        if (
            nl_shootingStarSpawn(
                ViewPositionAndTime.w,
                seed,
                cycle
            )
        ) {
            float star =
                nl_shootingStarStreak(
                    viewDir,
                    ViewPositionAndTime.w,
                    seed,
                    cycle
                );

            skyColor +=
                vec3_splat(star);
        }
    }
    #endif

    gl_FragColor =
        vec4(
            skyColor,
            1.0
        );
}
