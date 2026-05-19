#version 330 core
in vec3 fragmentNormal;
in float electrodeValue;
out vec4 outputColor;

uniform vec3 uLightDirection;

vec3 heatmapColor(float value) {
    value = clamp(value, 0.0, 1.0);
    float red   = smoothstep(0.5, 1.0, value);
    float green = smoothstep(0.0, 0.5, value) - smoothstep(0.75, 1.0, value);
    float blue  = 1.0 - smoothstep(0.0, 0.5, value);
    return vec3(red, green, blue);
}

void main() {
    vec3 surfaceColor = heatmapColor(electrodeValue);
    float lightIntensity = max(dot(normalize(fragmentNormal), normalize(uLightDirection)), 0.15);
    outputColor = vec4(surfaceColor * lightIntensity, 1.0);
}