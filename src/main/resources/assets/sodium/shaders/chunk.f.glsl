#version 110

varying vec4 v_Color;
varying vec2 v_TexCoord;
varying vec2 v_LightCoord;

uniform sampler2D u_BlockTex;
uniform sampler2D u_LightTex;

const float LIGHT_COORD_SCALE = 1.0 / 256.0;
const float LIGHT_COORD_OFFSET = 1.0 / 32.0;

void main() {
    vec4 sampleBlockTex = texture2D(u_BlockTex, v_TexCoord);
    vec4 sampleLightTex = texture2D(u_LightTex, (v_LightCoord * LIGHT_COORD_SCALE) + LIGHT_COORD_OFFSET);

    gl_FragColor = v_Color * sampleBlockTex * sampleLightTex;
}