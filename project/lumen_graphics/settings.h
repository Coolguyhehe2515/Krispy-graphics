/*
 * Lumen Graphics - settings.h
 * Central config for tunable values across the shader.
 * Include this in vertex.sc / fragment.sc files that need these values.
 */

// ---------------------------------------------------
// Plant / foliage wave (RenderChunk vertex, ALPHA_TEST_PASS)
// ---------------------------------------------------
#define NL_WAVE_SPEED 2.0        // Overall sway speed multiplier
#define NL_WAVE_AMPLITUDE 0.08   // Max horizontal sway distance (in blocks)
#define NL_WAVE_FREQ 1.5         // Spatial frequency of the sway pattern

// ---------------------------------------------------
// Directional lighting boost (RenderChunk fragment)
// ---------------------------------------------------
#define NL_SUNLIGHT_BOOST 0.15   // Strength of direct sunlight added on top of lightmap

// ---------------------------------------------------
// Cloud depth layers (Clouds material) - NOT YET IMPLEMENTED
// Placeholder only, values unused until Clouds.sc work begins.
// ---------------------------------------------------
// #define NL_CLOUD_LAYER_COUNT 2
// #define NL_CLOUD_PARALLAX_SPEED_1 1.0
// #define NL_CLOUD_PARALLAX_SPEED_2 0.6
