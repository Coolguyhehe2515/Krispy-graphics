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
// Overall lighting
// ---------------------------------------------------
#define NL_TORCH_INTENSITY 1.5
#define NL_MIN_AMBIENT 0.03
#define NL_SKY_BRIGHTNESS 1.8
#define NL_TORCH_COLOR vec3(0.969,0.737,0.302)

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

// ---------------------------------------------------
// Sky rendering (Sky material)
// ---------------------------------------------------
#define NL_SKY_HORIZON_SHARPNESS 1.5  // Higher = zenith color holds longer before transitioning
#define NL_SKY_EDGE_START 0.85        // Where the horizon edge-fade begins
#define NL_SKY_EDGE_END 1.0           // Where it fully reaches horizon color
#define NL_SKY_EDGE_STRENGTH 0.3      // How strong the extra horizon fade 

// ---------------------------------------------------
// Sky color palette (Sky material)
// ---------------------------------------------------

// Day
#define NL_SKY_DAY_ZENITH_COLOR   vec3(0.10, 0.45, 0.85)
#define NL_SKY_DAY_HORIZON_COLOR  vec3(0.55, 0.75, 0.95)
#define NL_SKY_DAY_EDGE_COLOR     vec3(0.75, 0.85, 0.92)

// Night
#define NL_SKY_NIGHT_ZENITH_COLOR   vec3(0.01, 0.02, 0.06)
#define NL_SKY_NIGHT_HORIZON_COLOR  vec3(0.04, 0.05, 0.10)
#define NL_SKY_NIGHT_EDGE_COLOR     vec3(0.06, 0.07, 0.12)

// Dawn
#define NL_SKY_DAWN_ZENITH_COLOR   vec3(0.35, 0.30, 0.55)
#define NL_SKY_DAWN_HORIZON_COLOR  vec3(1.00, 0.55, 0.35)
#define NL_SKY_DAWN_EDGE_COLOR     vec3(1.00, 0.70, 0.45)

// Dusk
#define NL_SKY_DUSK_ZENITH_COLOR   vec3(0.15, 0.35, 0.68)
#define NL_SKY_DUSK_HORIZON_COLOR  vec3(1.0, 0.78, 0.55)
#define NL_SKY_DUSK_EDGE_COLOR     vec3(1.0, 0.98, 0.85)

#define NL_SKY_TWILIGHT_RANGE 0.35
