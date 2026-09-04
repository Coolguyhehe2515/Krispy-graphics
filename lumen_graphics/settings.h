/*
 * Lumen Graphics - settings.h
 * Central config for tunable values across the shader.
 * Synced into every material's shaders folder at build time.
 */

// ---------------------------------------------------
// Plant / foliage wave (RenderChunk vertex, ALPHA_TEST_PASS)
// ---------------------------------------------------
#define NL_WAVE_SPEED 2.0            // Overall sway speed multiplier
#define NL_WAVE_AMPLITUDE 0.08       // Max horizontal sway distance (in blocks)
#define NL_WAVE_FREQ 1.5             // Spatial frequency of the sway pattern
#define NL_WAVE_MAX_DISTANCE 24.0    // Beyond this distance (in blocks), skip wave animation entirely

// ---------------------------------------------------
// Water / lava surface wave (RenderChunk vertex)
// ---------------------------------------------------
#define NL_WATER_WAVE_SPEED 1.2
#define NL_WATER_WAVE_HEIGHT 0.10
#define NL_LAVA_WAVE_SPEED 1.0
#define NL_LAVA_WAVE_HEIGHT 0.10

// ---------------------------------------------------
// Terrain lighting (RenderChunk fragment)
// ---------------------------------------------------
#define NL_SKY_BRIGHTNESS 1.8               // How strongly FogColor drives outdoor brightness
#define NL_TORCH_COLOR vec3(1.00, 0.60, 0.30) // Torch/block-light tint
#define NL_TORCH_INTENSITY 0.7              // Torch light strength
#define NL_MIN_LIGHTING_BOOST 0.0   // lighting boost
#define NL_LIGHT_WARMTH vec3(1.08, 1.0, 0.90) //lighting color this is crucial if you want to make your own variant
#define NL_CREVICE_SHADE_THRESHOLD 0.45
#define NL_CREVICE_SHADE_STRENGTH 2.0

// ---------------------------------------------------
// Color grading (RenderChunk fragment, final output)
// ---------------------------------------------------
#define NL_SATURATION 1.25   // 1.0 = no change, higher = more vivid colors
#define NL_CONTRAST 1.20     // 1.0 = no change, higher = punchier light/dark separation

// ---------------------------------------------------
// Cloud rendering (Clouds material, Transparent pass)
// ---------------------------------------------------
#define NL_CLOUD_SCROLL_SPEED 0.5   // How fast the pattern drifts with worldPos
#define NL_CLOUD_SCALE 4.5          // Overall pattern density
#define NL_CLOUD_ITERATIONS 5       // Layer count — higher = thicker clouds, more cost
#define NL_CLOUD_CLUSTER_SIZE 6.0   // Cluster grouping size
#define NL_CLOUD_THRESHOLD 0.500     // Coverage threshold — higher = fewer clouds
#define NL_CLOUD_CELL_SIZE 3.0       // Individual puff size
#define NL_CLOUD_RIM_OFFSET 0.2      // Rim/edge detection thickness
#define NL_CLOUD_RIM_BRIGHTNESS 2.0  // Rim highlight brightness multiplier
#define NL_CLOUD_RIM_STRENGTH 0.6    // How strongly rim highlight blends in
#define NL_CLOUD_SHADE_STRENGTH 0.2  // Self-shading darkness on cloud undersides

// ---------------------------------------------------
// Sky color palette (Sky material)
// ---------------------------------------------------

// Day
#define NL_SKY_DAY_ZENITH_COLOR   vec3(0.20, 0.50, 0.90)
#define NL_SKY_DAY_HORIZON_COLOR  vec3(0.60, 0.75, 0.95)
#define NL_SKY_DAY_EDGE_COLOR     vec3(0.90, 0.95, 1.00)

// Night
#define NL_SKY_NIGHT_ZENITH_COLOR   vec3(0.02, 0.03, 0.08)
#define NL_SKY_NIGHT_HORIZON_COLOR  vec3(0.06, 0.10, 0.22)
#define NL_SKY_NIGHT_EDGE_COLOR     vec3(0.03, 0.04, 0.10)

// Twilight (shared for dawn and dusk)
#define NL_SKY_TWILIGHT_ZENITH_COLOR   vec3(0.70, 0.50, 0.40)
#define NL_SKY_TWILIGHT_HORIZON_COLOR  vec3(0.98, 0.75, 0.50)
#define NL_SKY_TWILIGHT_EDGE_COLOR     vec3(0.80, 0.35, 0.25)

#define NL_SKY_TWILIGHT_RANGE 0.35

#define NL_SKY_HORIZON_SHARPNESS 1.5  // Higher = zenith color holds longer before transitioning
#define NL_SKY_EDGE_START 0.85        // Where the horizon edge-fade begins
#define NL_SKY_EDGE_END 1.0           // Where it fully reaches horizon color
#define NL_SKY_EDGE_STRENGTH 0.3      // How strong the extra horizon fade is

#define NL_NIGHT_BRIGHTNESS_BOOST 0.7   // Extra brightness added at full night, fades to 0 by day

// ---------------------------------------------------
// Terrain light tint (RenderChunk) — fixed palette, not raw FogColor
// ---------------------------------------------------
#define NL_LIGHT_DAY_COLOR       vec3(1.00, 1.00, 1.00)
#define NL_LIGHT_NIGHT_COLOR     vec3(0.35, 0.45, 0.65)
#define NL_LIGHT_TWILIGHT_COLOR  vec3(1.00, 0.75, 0.55)
#define NL_LIGHT_TWILIGHT_STRENGTH 0.35   // Capped — twilight can only ever blend in this much, can't overpower

// ---------------------------------------------------
// Rain darkening (Sky + RenderChunk)
// ---------------------------------------------------
#define NL_RAIN_DARKEN_STRENGTH 0.4   // How much dimmer things get at full rain intensity

// ---------------------------------------------------
// Shooting stars (Sky material, night only)
// ---------------------------------------------------
#define NL_SHOOTING_STAR_ENABLED 4
#define NL_SHOOTING_STAR_SPEED 0.6         // How fast a star travels across its streak once spawned
#define NL_SHOOTING_STAR_FREQUENCY 20.0     // How often a new "roll" happens — higher = more chances per second
#define NL_SHOOTING_STAR_SPAWN_CHANCE 1.0 // Fraction of rolls that actually produce a star (0 = never, 1 = every roll)
#define NL_SHOOTING_STAR_LENGTH 0.15       // Length of the visible streak/tail
#define NL_SHOOTING_STAR_WIDTH 0.0025      // Thickness of the streak line
#define NL_SHOOTING_STAR_BRIGHTNESS 2.5   // Peak brightness of the streak

// ---------------------------------------------------
// Sun/Moon glow (SunMoon material)
// ---------------------------------------------------
#define NL_SUNMOON_TINT_DESATURATE 0.6   // 0 = vanilla color, 1 = fully neutral/white
#define NL_SUNMOON_GLOW_ALPHA_CAP 0.5    // Caps how strong the soft glow can blend, core disc unaffected
