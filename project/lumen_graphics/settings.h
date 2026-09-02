/*
 * Lumen Graphics - settings.h
 * Central config for tunable values across the shader.
 * if you are trying to make a variant include this in vertex.sc / fragment.sc files that need these values.
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
#define NL_TORCH_INTENSITY 1.5
#define NL_MIN_AMBIENT 0.03

// ---------------------------------------------------
// Cloud rendering (Clouds material, Transparent pass)
// ---------------------------------------------------
#define NL_CLOUD_SCROLL_SPEED 0.02   // How fast the pattern drifts with worldPos
#define NL_CLOUD_SCALE 25.0          // Overall pattern density
#define NL_CLOUD_ITERATIONS 5        // Layer count — higher = thicker clouds, more cost
#define NL_CLOUD_CLUSTER_SIZE 4.93   // Cluster grouping size
#define NL_CLOUD_THRESHOLD 0.250     // Coverage threshold — higher = fewer clouds
#define NL_CLOUD_CELL_SIZE 0.6       // Individual puff size
#define NL_CLOUD_RIM_OFFSET 0.2      // Rim/edge detection thickness
#define NL_CLOUD_RIM_BRIGHTNESS 2.0  // Rim highlight brightness multiplier
#define NL_CLOUD_RIM_STRENGTH 0.6    // How strongly rim highlight blends in
#define NL_CLOUD_SHADE_STRENGTH 0.2  // Self-shading darkness on cloud undersides
