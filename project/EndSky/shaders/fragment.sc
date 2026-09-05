$input v_color0, v_texcoord0, v_worldPos
precision highp float;
#include "bgfx_shader.sh"
#include "settings.h"

SAMPLER2D_AUTOREG(s_SkyTexture);
uniform vec4 SkyColor;

void main() {
    vec4 texColor = texture(s_SkyTexture, v_texcoord0);
    vec4 result = SkyColor * texColor;
    gl_FragColor = vec4(result.rgb, result.a);
}
