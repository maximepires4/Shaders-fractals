#version 410

uniform int width;
uniform int height;
uniform double zoom;
uniform double moveX;
uniform double moveY;
uniform int maxIter;
uniform float real;
uniform float imaginary;
uniform int nb_colors;
uniform float colors[20];

int julia(dvec2 z){

    z = dvec2(1.5f * (z.x - width/2) / (0.5 * zoom * width) + moveX, (z.y - height/2) / (0.5 * zoom * height) + moveY);
    int i = maxIter;

    while(z.x * z.x + z.y * z.y < 4 && i > 0){
        z = dvec2(z.x * z.x - z.y * z.y + real, 2.0 * z.x * z.y + imaginary);
        i--;
    }

    return i;
}

vec3 get_color(float iterations){
    if(iterations == 0) return vec3(0,0,0);

	float value = 1 - float(iterations) / float(maxIter);
	vec3 color = vec3(1,1,1);

	float min_value;
	float max_value;

	for (int i = 0; i < nb_colors; i++){
		min_value = float(i) / float(nb_colors);
		max_value = float((i + 1)) / float(nb_colors);

		if (value >= min_value && value <= max_value){
			color = mix(vec3(colors[i*3], colors[i*3+1], colors[i*3+2]), vec3(colors[i*3+3], colors[i*3+4], colors[i*3+5]), (value-min_value) * nb_colors);
			break;
		}
	}

	return color;
}

void main() {
    int iter = julia(dvec2(gl_FragCoord.x, gl_FragCoord.y));

    vec3 c = get_color(iter);

    gl_FragColor = vec4(c.x, c.y, c.z, 1.0f);
}