$input a_color0, a_position, a_texcoord0
#ifdef INSTANCING__ON
$input i_data1, i_data2, i_data3
#endif

$output v_color0, v_texcoord0, v_worldPos

#include "bgfx_shader.sh"
#include "settings.h"

uniform vec4 CloudColor;
uniform vec4 DistanceControl;
uniform vec4 SubPixelOffset;

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

    vec3 worldPos = worldPos4.xyz;

    mat4 offsetProj = u_proj;
    #if BGFX_SHADER_LANGUAGE_GLSL
    offsetProj[2][0] += SubPixelOffset.x;
    offsetProj[2][1] -= SubPixelOffset.y;
    #else
    offsetProj[0][2] += SubPixelOffset.x;
    offsetProj[1][2] -= SubPixelOffset.y;
    #endif

    vec4 color = a_color0 * CloudColor;
    color.a *= clamp(1.0 - max((length(worldPos) / DistanceControl.x) - 0.9, 0.0), 0.0, 1.0);

    v_color0 = color;
    v_texcoord0 = a_texcoord0;
    v_worldPos = worldPos;

    gl_Position = mul(offsetProj, mul(u_view, vec4(worldPos, 1.0)));
}
