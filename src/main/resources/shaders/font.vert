#version 330 core
layout(location = 0) in vec2 aScreenPosition;
layout(location = 1) in vec2 aTexCoord;
out vec2 texCoord;
uniform vec2 uScreenSize;
void main() {
    vec2 ndcPosition = (aScreenPosition / uScreenSize) * 2.0 - 1.0;
    ndcPosition.y = -ndcPosition.y;
    gl_Position = vec4(ndcPosition, 0.0, 1.0);
    texCoord = aTexCoord;
}