#version 410

precision highp float;

in vec3 vertices;

void main() {
    gl_Position = vec4(vertices,1);
}