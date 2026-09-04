$input v_color0, v_texcoord0, v_worldPos
precision highp float;
#include "bgfx_shader.sh"
#include "settings.h"

SAMPLER2D_AUTOREG(s_SunMoonTexture);
uniform vec4 SunMoonColor;

void main() {
    vec4 texColor = texture(s_SunMoonTexture, v_texcoord0);
    float luma = dot(SunMoonColor.rgb, vec3(0.299, 0.587, 0.114));
    vec3 tamedTint = mix(SunMoonColor.rgb, vec3_splat(luma), NL_SUNMOON_TINT_DESATURATE);

    vec4 result = vec4(tamedTint, SunMoonColor.a) * texColor;

    // Cap the glow's blend strength — keeps the core disc bright, softens the wide halo.
    result.a = min(result.a, NL_SUNMOON_GLOW_ALPHA_CAP + texColor.a * (1.0 - NL_SUNMOON_GLOW_ALPHA_CAP));

    gl_FragColor = result;
}
