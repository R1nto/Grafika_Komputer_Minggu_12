// ==========================================================
// FILE: src/main/resources/shaders/phong.vert (Vertex Shader)
// ==========================================================
#version 330 core
layout (location = 0) in vec3 aPos;
layout (location = 1) in vec3 aNormal;      // normal vertex hasil averaging (Smooth Shading)
layout (location = 2) in vec2 aTexCoords;
layout (location = 3) in vec3 aFlatNormal;  // normal per-face (Flat Shading)

out vec3 FragPos;
out vec3 Normal;
out vec2 TexCoords;
out vec4 FragPosLightSpace;

uniform mat4 model;
uniform mat4 view;
uniform mat4 projection;
uniform mat4 lightSpaceMatrix;   // P_light * V_light
uniform bool useFlatShading;     // Key 'F'

void main()
{
    vec4 worldPos = model * vec4(aPos, 1.0);
    FragPos = worldPos.xyz;

    vec3 n = useFlatShading ? aFlatNormal : aNormal;
    Normal = mat3(transpose(inverse(model))) * n;   // Normal Matrix

    TexCoords = aTexCoords;
    FragPosLightSpace = lightSpaceMatrix * worldPos;
    gl_Position = projection * view * worldPos;
}
