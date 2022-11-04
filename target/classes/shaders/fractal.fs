#version 330

precision highp float;

uniform int width;
uniform int height;
uniform float zoom;
uniform float moveX;
uniform float moveY;
uniform float maxIter;
uniform float real;
uniform float imaginary;

float julia(vec2 z){

    z = vec2(1.5f * (z.x - width/2) / (0.5 * zoom * width) + moveX, (z.y - height/2) / (0.5 * zoom * height) + moveY);
    float i = maxIter;

    while(z.x * z.x + z.y * z.y < 4 && i > 0){
        //z = vec2(z.x * z.x - z.y * z.y - 0.7, 2.0 * z.x * z.y + 0.27015);
        z = vec2(z.x * z.x - z.y * z.y + real, 2.0 * z.x * z.y + imaginary);
        i--;
    }

    return i;
}

vec3 hsv2rgb(vec3 c){
    vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
    vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);
    return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);
}


void main() {
    float iter = julia(vec2(gl_FragCoord.x, gl_FragCoord.y));
    vec3 c = hsv2rgb(vec3(mod(maxIter/iter,1), 1.0f, iter/maxIter));
    gl_FragColor = vec4(c.x, c.y, c.z, 1.0f);
}