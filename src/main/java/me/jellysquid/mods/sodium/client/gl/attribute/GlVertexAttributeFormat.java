package me.jellysquid.mods.sodium.client.gl.attribute;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

public enum GlVertexAttributeFormat {
    FLOAT(GL11.GL_FLOAT, 4),
    HALF_FLOAT(GL30.GL_HALF_FLOAT, 2),
    SHORT(GL11.GL_SHORT, 2),
    UNSIGNED_SHORT(GL11.GL_UNSIGNED_SHORT, 2),
    BYTE(GL11.GL_BYTE, 1),
    UNSIGNED_BYTE(GL11.GL_UNSIGNED_BYTE, 1);

    private final int glId;
    private final int size;

    GlVertexAttributeFormat(int glId, int size) {
        this.glId = glId;
        this.size = size;
    }

    public int getSize() {
        return this.size;
    }

    public int getGlFormat() {
        return this.glId;
    }
}
