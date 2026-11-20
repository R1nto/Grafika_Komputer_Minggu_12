#version 330 core
layout (location = 0) in vec3 aPos;
layout (location = 1) in vec3 aSmoothNormal;
layout (location = 2) in vec2 aTexCoords;
layout (location = 3) in vec3 aFlatNormal;

out vec3 FragPos;
out vec3 Normal;

uniform mat4 model;
uniform mat4 view;
uniform mat4 projection;
uniform bool useFlatShading;

void main() {
    FragPos = vec3(model * vec4(aPos, 1.0));
    vec3 chosenNormal = useFlatShading ? aFlatNormal : aSmoothNormal;
    Normal = mat3(transpose(inverse(model))) * chosenNormal;
    gl_Position = projection * view * vec4(FragPos, 1.0);
}
