package me.jellysquid.mods.sodium.client.render.backends.shader;

import com.google.common.collect.ImmutableList;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.system.MemoryStack;

import java.nio.FloatBuffer;
import java.util.List;
import java.util.function.Function;

public abstract class FogShaderComponent {
    public abstract void setup();

    public static class None extends FogShaderComponent {
        public None(ChunkShader program) {

        }

        @Override
        public void setup() {

        }
    }

    public static class Exp2 extends FogShaderComponent {
        private final int uFogColor;
        private final int uFogDensity;

        public Exp2(ChunkShader program) {
            this.uFogColor = program.getUniformLocation("u_FogColor");
            this.uFogDensity = program.getUniformLocation("u_FogDensity");
        }

        @Override
        public void setup() {
            FogShaderComponent.setupColorUniform(this.uFogColor);

            GL20.glUniform1f(this.uFogDensity, GL11.glGetFloat(GL11.GL_FOG_DENSITY));
        }
    }

    public static class Linear extends FogShaderComponent {
        private final int uFogColor;
        private final int uFogLength;
        private final int uFogEnd;

        public Linear(ChunkShader program) {
            this.uFogColor = program.getUniformLocation("u_FogColor");
            this.uFogLength = program.getUniformLocation("u_FogLength");
            this.uFogEnd = program.getUniformLocation("u_FogEnd");
        }

        @Override
        public void setup() {
            FogShaderComponent.setupColorUniform(this.uFogColor);

            float end = GL11.glGetFloat(GL11.GL_FOG_END);
            float start = GL11.glGetFloat(GL11.GL_FOG_START);

            GL20.glUniform1f(this.uFogLength, end - start);
            GL20.glUniform1f(this.uFogEnd, end);
        }
    }

    private static void setupColorUniform(int index) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer bufFogColor = stack.mallocFloat(4);
            GL11.glGetFloatv(GL11.GL_FOG_COLOR, bufFogColor);
            GL20.glUniform4fv(index, bufFogColor);
        }
    }

    enum FogMode {
        NONE(None::new, ImmutableList.of()),
        LINEAR(Linear::new, ImmutableList.of("USE_FOG", "USE_FOG_LINEAR")),
        EXP2(Exp2::new, ImmutableList.of("USE_FOG", "USE_FOG_EXP2"));

        private final Function<ChunkShader, FogShaderComponent> factory;
        private final List<String> defines;

        FogMode(Function<ChunkShader, FogShaderComponent> factory, List<String> defines) {
            this.factory = factory;
            this.defines = defines;
        }

        public Function<ChunkShader, FogShaderComponent> getFactory() {
            return this.factory;
        }

        public List<String> getDefines() {
            return this.defines;
        }

        public static FogMode chooseFogMode() {
            if (!GL11.glGetBoolean(GL11.GL_FOG)) {
                return FogMode.NONE;
            }

            int mode = GL11.glGetInteger(GL11.GL_FOG_MODE);

            switch (mode) {
                case GL11.GL_EXP2:
                case GL11.GL_EXP:
                    return FogMode.EXP2;
                case GL11.GL_LINEAR:
                    return FogMode.LINEAR;
                default:
                    throw new UnsupportedOperationException("Unknown fog mode: " + mode);
            }
        }
    }
}
