#version 330 core
layout(location = 0) in vec3 aPosition;
layout(location = 1) in vec3 aNormal;
layout(location = 2) in float aElectrodeValue;

out vec3 fragmentNormal;
out float electrodeValue;

uniform mat4 uMVPMatrix;
uniform mat4 uModelMatrix;

void main() {
    gl_Position = uMVPMatrix * vec4(aPosition, 1.0);
    fragmentNormal = normalize(mat3(uModelMatrix) * aNormal);
    electrodeValue = aElectrodeValue;
}