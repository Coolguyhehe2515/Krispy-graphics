$input a_color0, a_position, a_texcoord0
#ifdef INSTANCING__ON
$input i_data1, i_data2, i_data3
#endif

$output v_color0, v_texcoord0, v_worldPos

#include "bgfx_shader.sh"
#include "settings.h"

uniform vec4 FogColor;
uniform vec4 SkyColor;

void main() {
    #ifdef INSTANCING__OFF
    vec4 worldPos4 = mul(u_model[0], vec4(a_position, 1.0));
    #endif
    #ifdef INSTANCING__ON
    mat4 model;
    model[0] = vec4(i_data1.x, i_data2.x, i_data3.x, 0.0);
    model[1] = vec4(i_data1.y, i_data2.y, i_data3.y, 0.0);
    model[2] = vec4(i_data1.z, i_data2.z, i_data3.z, 0.0);
    model[3] = vec4(i_data1.w, i_data2.w, i_data3.w, 1.0);
    vec4 worldPos4 = mul(model, vec4(a_position, 1.0));
    #endif

    // Pass raw vertex color through — the real blend happens per-pixel in fragment.sc now.
    v_color0 = a_color0;
    v_texcoord0 = a_texcoord0;
    v_worldPos = worldPos4.xyz;

    gl_Position = mul(u_viewProj, vec4(worldPos4.xyz, 1.0));
}
