$input a_color0, a_position
$output v_worldPos

#include "bgfx_shader.sh"
#include "settings.h"

void main() {
    vec4 pos = vec4(a_position.xzy, 1.0);
    pos.xy = 2.0 * clamp(pos.xy, -0.5, 0.5);

    v_worldPos = mul(u_invViewProj, pos).xyz;

    gl_Position = pos;
}
