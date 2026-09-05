$input v_color0, v_texcoord0, v_worldPos
precision highp float;
#include "bgfx_shader.sh"
#include "settings.h"

SAMPLER2D_AUTOREG(s_SkyTexture);
uniform vec4 SkyColor;

void main() {
    vec3 dir = normalize(v_worldPos);

    // Convert view direction into spherical (equirectangular) UV coordinates.
    float longitude = atan(dir.x, dir.z);
    float latitude = asin(clamp(dir.y, -1.0, 1.0));

    vec2 equirectUV;
    equirectUV.x = (longitude / 3.14159265) * 0.5 + 0.5;
    equirectUV.y = 0.5 - (latitude / 3.14159265);

    vec4 texColor = texture(s_SkyTexture, equirectUV);
    vec4 result = SkyColor * texColor;
    gl_FragColor = vec4(result.rgb, result.a);
}
