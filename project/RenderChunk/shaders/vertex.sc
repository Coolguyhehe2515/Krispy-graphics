$input a_color0, a_position, a_texcoord0, a_texcoord1
#ifdef INSTANCING__ON
$input i_data1, i_data2, i_data3
#endif

$output v_color0, v_fog, v_lightmapUV, v_texcoord0, v_worldPos

#include "bgfx_shader.sh"
#include "settings.h"

uniform vec4 FogAndDistanceControl;
uniform vec4 FogColor;
uniform vec4 GlobalRoughness;
uniform vec4 LightDiffuseColorAndIlluminance;
uniform vec4 LightWorldSpaceDirection;
uniform vec4 MaterialID;
uniform vec4 RenderChunkFogAlpha;
uniform vec4 SubPixelOffset;
uniform vec4 ViewPositionAndTime;
uniform vec4 TimeOfDay;
vec4 ViewRect;
mat4 Proj;
mat4 View;
vec4 ViewTexel;
mat4 InvView;
mat4 InvProj;
mat4 ViewProj;
mat4 InvViewProj;
mat4 PrevViewProj;
mat4 WorldArray[4];
mat4 World;
mat4 WorldView;
mat4 WorldViewProj;
vec4 PrevWorldPosOffset;
vec4 AlphaRef4;
float AlphaRef;

struct VertexInput {
    vec4 color0;
    vec2 lightmapUV;
    vec3 position;
    vec2 texcoord0;
    #ifdef INSTANCING__ON
    vec4 instanceData0;
    vec4 instanceData1;
    vec4 instanceData2;
    #endif
};

struct VertexOutput {
    vec4 position;
    vec4 color0;
    vec4 fog;
    vec2 lightmapUV;
    vec2 texcoord0;
    vec3 worldPos;
};

struct StandardVertexInput {
    VertexInput vertInput;
    vec3 worldPos;
};

vec4 jitterVertexPosition(vec3 worldPosition) {
    mat4 offsetProj = Proj;
    #if BGFX_SHADER_LANGUAGE_GLSL
    offsetProj[2][0] += SubPixelOffset.x;
    offsetProj[2][1] -= SubPixelOffset.y;
    #else
    offsetProj[0][2] += SubPixelOffset.x;
    offsetProj[1][2] -= SubPixelOffset.y;
    #endif
    return mul(offsetProj, mul(View, vec4(worldPosition, 1.0f)));
}

float calculateFogIntensityFadedVanilla(float cameraDepth, float maxDistance, float fogStart, float fogEnd, float fogAlpha) {
    float distance = cameraDepth / maxDistance;
    distance += fogAlpha;
    return clamp((distance - fogStart) / (fogEnd - fogStart), 0.0, 1.0);
}

void transformAsBillboardVertex(inout StandardVertexInput stdInput, inout VertexOutput vertOutput) {
    stdInput.worldPos += vec3(0.5, 0.5, 0.5);
    vec3 forward = normalize(stdInput.worldPos - ViewPositionAndTime.xyz);
    vec3 right = normalize(cross(vec3(0.0, 1.0, 0.0), forward));
    vec3 up = cross(forward, right);
    vec3 offsets = stdInput.vertInput.color0.xyz;
    stdInput.worldPos -= up * (offsets.z - 0.5) + right * (offsets.x - 0.5);
    vertOutput.position = mul(ViewProj, vec4(stdInput.worldPos, 1.0));
}

float RenderChunkVert(StandardVertexInput stdInput, inout VertexOutput vertOutput) {
    #ifdef RENDER_AS_BILLBOARDS__ON
    vertOutput.color0 = vec4(1.0, 1.0, 1.0, 1.0);
    transformAsBillboardVertex(stdInput, vertOutput);
    #endif
    float cameraDepth = length(ViewPositionAndTime.xyz - stdInput.worldPos);
    float fogIntensity = calculateFogIntensityFadedVanilla(cameraDepth, FogAndDistanceControl.z, FogAndDistanceControl.x, FogAndDistanceControl.y, RenderChunkFogAlpha.x);
    vertOutput.fog = vec4(FogColor.rgb, fogIntensity);

    // Decode packed UV coordinates (1.26.x vanilla vertex format).
    // Uses fract/mod/floor only — avoids uint bitwise ops (unreliable on some Mali GPUs).
    float rawUx = round(stdInput.vertInput.texcoord0.x * 65535.0);
    float rawUy = round(stdInput.vertInput.texcoord0.y * 65535.0);

    float low15X = mod(rawUx, 32768.0);
    float low15Y = mod(rawUy, 32768.0);
    float signX = floor(rawUx / 32768.0);
    float signY = floor(rawUy / 32768.0);

    vec2 decodedUV;
    decodedUV.x = (low15X * 2.0) * 1.525902189314365386962890625e-05;
    decodedUV.y = (low15Y * 2.0) * 1.525902189314365386962890625e-05;
    decodedUV.x += 3.0517578125e-05 * (2.0 * signX - 1.0);
    decodedUV.y += 3.0517578125e-05 * (2.0 * signY - 1.0);
    vertOutput.texcoord0 = decodedUV;

    #ifdef ALPHA_TEST_PASS
    if (cameraDepth < NL_WAVE_MAX_DISTANCE) {
        float waveTime = ViewPositionAndTime.w;
        float heightFrac = fract(stdInput.worldPos.y + 0.001);
        float waveStrength = heightFrac * NL_WAVE_AMPLITUDE;
        float wave = sin(waveTime * NL_WAVE_SPEED + stdInput.worldPos.x * NL_WAVE_FREQ + stdInput.worldPos.z * NL_WAVE_FREQ);
        stdInput.worldPos.x += wave * waveStrength;
        stdInput.worldPos.z += cos(waveTime * NL_WAVE_SPEED + stdInput.worldPos.x) * waveStrength * 0.5;
    }
    #endif

    vertOutput.position = jitterVertexPosition(stdInput.worldPos);
    return cameraDepth;
}

void RenderChunkVertTransparent(StandardVertexInput stdInput, inout VertexOutput vertOutput) {
    float cameraDepth = RenderChunkVert(stdInput, vertOutput);
    bool shouldBecomeOpaqueInTheDistance = stdInput.vertInput.color0.a < 0.95;
    if (shouldBecomeOpaqueInTheDistance) {
        float cameraDistance = cameraDepth / FogAndDistanceControl.w;
        float alphaFadeOut = clamp(cameraDistance, 0.0, 1.0);
        vertOutput.color0.a = mix(stdInput.vertInput.color0.a, 1.0, alphaFadeOut);
    }
}

void StandardTemplate_VertSharedTransform(inout StandardVertexInput stdInput, inout VertexOutput vertOutput) {
    VertexInput vertInput = stdInput.vertInput;
    #ifdef INSTANCING__ON
    mat4 model;
    model[0] = vec4(vertInput.instanceData0.x, vertInput.instanceData1.x, vertInput.instanceData2.x, 0);
    model[1] = vec4(vertInput.instanceData0.y, vertInput.instanceData1.y, vertInput.instanceData2.y, 0);
    model[2] = vec4(vertInput.instanceData0.z, vertInput.instanceData1.z, vertInput.instanceData2.z, 0);
    model[3] = vec4(vertInput.instanceData0.w, vertInput.instanceData1.w, vertInput.instanceData2.w, 1);
    vec3 wpos = instMul(model, vec4(vertInput.position, 1.0)).xyz;
    #else
    vec3 wpos = mul(World, vec4(vertInput.position, 1.0)).xyz;
    #endif
    vertOutput.position = mul(ViewProj, vec4(wpos, 1.0));
    stdInput.worldPos = wpos;
    vertOutput.worldPos = wpos;
}

void StandardTemplate_VertexPreprocessIdentity(VertexInput vertInput, inout VertexOutput vertOutput) { }

void StandardTemplate_InvokeVertexPreprocessFunction(inout VertexInput vertInput, inout VertexOutput vertOutput);
void StandardTemplate_InvokeVertexOverrideFunction(StandardVertexInput vertInput, inout VertexOutput vertOutput);
void StandardTemplate_InvokeLightingVertexFunction(VertexInput vertInput, inout VertexOutput vertOutput, vec3 worldPosition);

void computeLighting_RenderChunk_Vertex(VertexInput vInput, inout VertexOutput vOutput, vec3 worldPosition) {
    // Decode packed lightmap UV using Newb's proven fract-based approach
    // (avoids bitwise ops entirely, matches vanilla 1.26.x packing).
    vOutput.lightmapUV = fract(vInput.lightmapUV.y * vec2(256.0, 4096.0));
}

void StandardTemplate_VertShared(VertexInput vertInput, inout VertexOutput vertOutput) {
    StandardTemplate_InvokeVertexPreprocessFunction(vertInput, vertOutput);
    StandardVertexInput stdInput;
    stdInput.vertInput = vertInput;
    StandardTemplate_VertSharedTransform(stdInput, vertOutput);
    vertOutput.color0 = vertInput.color0;
    StandardTemplate_InvokeVertexOverrideFunction(stdInput, vertOutput);
    StandardTemplate_InvokeLightingVertexFunction(vertInput, vertOutput, stdInput.worldPos);
}

void StandardTemplate_InvokeVertexPreprocessFunction(inout VertexInput vertInput, inout VertexOutput vertOutput) {
    StandardTemplate_VertexPreprocessIdentity(vertInput, vertOutput);
}

void StandardTemplate_InvokeVertexOverrideFunction(StandardVertexInput vertInput, inout VertexOutput vertOutput) {
    #ifndef TRANSPARENT_PASS
    RenderChunkVert(vertInput, vertOutput);
    #endif
    #ifdef TRANSPARENT_PASS
    RenderChunkVertTransparent(vertInput, vertOutput);
    #endif
}

void StandardTemplate_InvokeLightingVertexFunction(VertexInput vertInput, inout VertexOutput vertOutput, vec3 worldPosition) {
    computeLighting_RenderChunk_Vertex(vertInput, vertOutput, worldPosition);
}

void StandardTemplate_Opaque_Vert(VertexInput vertInput, inout VertexOutput vertOutput) {
    StandardTemplate_VertShared(vertInput, vertOutput);
}

void main() {
    VertexInput vertexInput;
    VertexOutput vertexOutput;
    vertexInput.color0 = (a_color0);
    vertexInput.lightmapUV = (a_texcoord1);
    vertexInput.position = (a_position);
    vertexInput.texcoord0 = (a_texcoord0);
    #ifdef INSTANCING__ON
    vertexInput.instanceData0 = i_data1;
    vertexInput.instanceData1 = i_data2;
    vertexInput.instanceData2 = i_data3;
    #endif
    vertexOutput.color0 = vec4(0, 0, 0, 0);
    vertexOutput.fog = vec4(0, 0, 0, 0);
    vertexOutput.lightmapUV = vec2(0, 0);
    vertexOutput.texcoord0 = vec2(0, 0);
    vertexOutput.worldPos = vec3(0, 0, 0);
    vertexOutput.position = vec4(0, 0, 0, 0);
    ViewRect = u_viewRect;
    Proj = u_proj;
    View = u_view;
    ViewTexel = u_viewTexel;
    InvView = u_invView;
    InvProj = u_invProj;
    ViewProj = u_viewProj;
    InvViewProj = u_invViewProj;
    PrevViewProj = u_prevViewProj;
    {
        WorldArray[0] = u_model[0];
        WorldArray[1] = u_model[1];
        WorldArray[2] = u_model[2];
        WorldArray[3] = u_model[3];
    }
    World = u_model[0];
    WorldView = u_modelView;
    WorldViewProj = u_modelViewProj;
    PrevWorldPosOffset = u_prevWorldPosOffset;
    AlphaRef4 = u_alphaRef4;
    AlphaRef = u_alphaRef4.x;
    StandardTemplate_Opaque_Vert(vertexInput, vertexOutput);
    v_color0 = vertexOutput.color0;
    v_fog = vertexOutput.fog;
    v_lightmapUV = vertexOutput.lightmapUV;
    v_texcoord0 = vertexOutput.texcoord0;
    v_worldPos = vertexOutput.worldPos;
    gl_Position = vertexOutput.position;
}
