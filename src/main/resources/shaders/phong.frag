// ==========================================================
// FILE: src/main/resources/shaders/phong.frag (Fragment Shader)
// Phong / Blinn-Phong per-pixel + Texture + Shadow Mapping (PCF 3x3)
// ==========================================================
#version 330 core
out vec4 FragColor;

in vec3 FragPos;
in vec3 Normal;
in vec2 TexCoords;
in vec4 FragPosLightSpace;

uniform vec3 lightPos;        // cahaya directional: arah dari titik asal (0,0,0) menuju lightPos
uniform vec3 viewPos;
uniform vec3 lightColor;
uniform vec3 objectColor;
uniform sampler2D textureSampler;
uniform sampler2D shadowMap;
uniform bool useTexture;      // Key 'T'
uniform bool useBlinnPhong;   // Key 'B'
uniform bool useShadows;      // Key 'S'

// Shadow factor: 1.0 = dalam bayangan, 0.0 = terkena cahaya (rata-rata PCF 3x3)
float calcShadow(vec4 fragPosLight, vec3 norm, vec3 lightDir)
{
    vec3 proj = fragPosLight.xyz / fragPosLight.w;   // perspective divide
    proj = proj * 0.5 + 0.5;                         // NDC [-1,1] -> [0,1]
    if (proj.z > 1.0) return 0.0;                    // di luar far plane cahaya

    // Slope-scaled bias. Nilai disesuaikan dengan rentang near-far frustum cahaya (1..40)
    float bias = max(0.0025 * (1.0 - dot(norm, lightDir)), 0.0005);

    vec2 texel = 1.0 / vec2(textureSize(shadowMap, 0));
    float shadow = 0.0;
    for (int x = -1; x <= 1; ++x)
    {
        for (int y = -1; y <= 1; ++y)
        {
            float closest = texture(shadowMap, proj.xy + vec2(x, y) * texel).r;
            shadow += (proj.z - bias > closest) ? 1.0 : 0.0;
        }
    }
    return shadow / 9.0;
}

void main()
{
    vec3 norm = normalize(Normal);
    if (!gl_FrontFacing) norm = -norm;               // sisi belakang (saat culling OFF) dilit dengan normal terbalik

    vec3 lightDir = normalize(lightPos);
    vec3 viewDir = normalize(viewPos - FragPos);

    // 1. Ambient
    float ambientStrength = 0.2;
    vec3 ambient = ambientStrength * lightColor;

    // 2. Diffuse (Lambertian)
    float diff = max(dot(norm, lightDir), 0.0);
    vec3 diffuse = diff * lightColor;

    // 3. Specular (Phong vs Blinn-Phong)
    float specularStrength = 0.8;
    float spec = 0.0;
    if (diff > 0.0)
    {
        if (useBlinnPhong)
        {
            vec3 halfwayDir = normalize(lightDir + viewDir);
            spec = pow(max(dot(norm, halfwayDir), 0.0), 32.0);   // Blinn-Phong
        }
        else
        {
            vec3 reflectDir = reflect(-lightDir, norm);
            spec = pow(max(dot(viewDir, reflectDir), 0.0), 16.0); // Phong standar
        }
    }
    vec3 specular = specularStrength * spec * lightColor;

    // 4. Shadow (ambient tidak ikut digelapkan; faktor bayangan = 0.2 saat sepenuhnya tertutup)
    float shadow = useShadows ? calcShadow(FragPosLightSpace, norm, lightDir) : 0.0;
    float lit = 1.0 - 0.8 * shadow;

    vec3 baseColor = useTexture ? texture(textureSampler, TexCoords).rgb : objectColor;
    vec3 result = (ambient + lit * (diffuse + specular)) * baseColor;
    FragColor = vec4(result, 1.0);
}
