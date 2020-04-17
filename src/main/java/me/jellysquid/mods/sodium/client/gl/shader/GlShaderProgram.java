package me.jellysquid.mods.sodium.client.gl.shader;

import me.jellysquid.mods.sodium.client.gl.GlHandle;
import net.minecraft.util.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

public abstract class GlShaderProgram extends GlHandle {
    private static final Logger LOGGER = LogManager.getLogger(GlShaderProgram.class);

    private final Identifier name;

    protected GlShaderProgram(Identifier name, int program) {
        this.name = name;
        this.setHandle(program);
    }

    public static Builder builder(Identifier identifier) {
        return new Builder(identifier);
    }

    public void bind() {
        GL20.glUseProgram(this.handle());
    }

    public void unbind() {
        GL20.glUseProgram(0);
    }

    public Identifier getName() {
        return this.name;
    }

    public int getUniformLocation(String name) {
        int index = GL20.glGetUniformLocation(this.handle(), name);

        if (index < 0) {
            throw new NullPointerException("No uniform exists with name: " + name);
        }

        return index;
    }

    public int getAttributeLocation(String name) {
        int index = GL20.glGetAttribLocation(this.handle(), name);

        if (index < 0) {
            throw new NullPointerException("No attribute exists with name: " + name);
        }

        return index;
    }

    public void delete() {
        GL20.glDeleteProgram(this.handle());

        this.invalidateHandle();
    }

    public static class Builder {
        private final Identifier name;
        private final int program;

        public Builder(Identifier name) {
            this.name = name;
            this.program = GL20.glCreateProgram();
        }

        public Builder attach(GlShader shader) {
            GL20.glAttachShader(this.program, shader.handle());

            return this;
        }

        public <P extends GlShaderProgram> P link(ShaderTypeFactory<P> factory) {
            GL20.glLinkProgram(this.program);

            String log = GL20.glGetProgramInfoLog(this.program);

            if (!log.isEmpty()) {
                LOGGER.warn("Program link log for " + this.name + ": " + log);
            }

            int result = GL20.glGetProgrami(this.program, GL20.GL_LINK_STATUS);

            if (result != GL11.GL_TRUE) {
                throw new RuntimeException("Shader program linking failed, see log for details");
            }

            return factory.create(this.name, this.program);
        }

        public Builder attribute(int index, String name) {
            GL20.glBindAttribLocation(this.program, index, name);

            return this;
        }
    }

    public interface ShaderTypeFactory<P extends GlShaderProgram> {
        P create(Identifier name, int handle);
    }
}
