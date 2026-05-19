#version 330 core
in vec2 texCoord;
out vec4 outputColor;
uniform sampler2D uFontTexture;
uniform vec3 uTextColor;
void main() {
    float alpha = texture(uFontTexture, texCoord).r;
    outputColor = vec4(uTextColor, alpha);
}