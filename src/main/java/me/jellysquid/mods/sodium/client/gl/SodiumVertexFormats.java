package me.jellysquid.mods.sodium.client.gl;

import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexAttribute;
import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexAttributeFormat;
import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexFormat;
import me.jellysquid.mods.sodium.client.render.model.quad.ModelQuadEncoder;
import me.jellysquid.mods.sodium.client.util.fp.HFloat;

public class SodiumVertexFormats {
    /**
     * Standard vertex format which is bit compatible with vanilla's own format.
     */
    public static final GlVertexFormat<ChunkMeshAttribute> CHUNK_MESH_VANILLA = GlVertexAttribute.builder(ChunkMeshAttribute.class)
            .add(ChunkMeshAttribute.POSITION, new GlVertexAttribute(GlVertexAttributeFormat.FLOAT, 3, false, 0))
            .add(ChunkMeshAttribute.COLOR, new GlVertexAttribute(GlVertexAttributeFormat.UNSIGNED_BYTE, 4, true, 12))
            .add(ChunkMeshAttribute.TEXTURE, new GlVertexAttribute(GlVertexAttributeFormat.FLOAT, 2, false, 16))
            .add(ChunkMeshAttribute.LIGHT, new GlVertexAttribute(GlVertexAttributeFormat.SHORT, 2, false, 24))
            .build(32);

    /**
     * Compact vertex format which makes use of half-floats for encoding position and texture vectors.
     */
    public static final GlVertexFormat<ChunkMeshAttribute> CHUNK_MESH_HFP = GlVertexAttribute.builder(ChunkMeshAttribute.class)
            .add(ChunkMeshAttribute.POSITION, new GlVertexAttribute(GlVertexAttributeFormat.HALF_FLOAT, 3, false, 0))
            .add(ChunkMeshAttribute.COLOR, new GlVertexAttribute(GlVertexAttributeFormat.UNSIGNED_BYTE, 4, true, 8))
            .add(ChunkMeshAttribute.TEXTURE, new GlVertexAttribute(GlVertexAttributeFormat.HALF_FLOAT, 2, false, 12))
            .add(ChunkMeshAttribute.LIGHT, new GlVertexAttribute(GlVertexAttributeFormat.SHORT, 2, false, 16))
            .build(20);

    private static final Reference2ObjectMap<GlVertexFormat<?>, ModelQuadEncoder> encoders = new Reference2ObjectOpenHashMap<>();

    static {
        registerEncoder(CHUNK_MESH_VANILLA, (format, buffer, position, quad, x, y, z) -> {
            for (int i = 0; i < 4; i++) {
                buffer.putFloat(position, quad.getX(i) + x);
                buffer.putFloat(position + 4, quad.getY(i) + y);
                buffer.putFloat(position + 8, quad.getZ(i) + z);
                buffer.putInt(position + 12, quad.getColor(i));
                buffer.putFloat(position + 16, quad.getTexU(i));
                buffer.putFloat(position + 20, quad.getTexV(i));
                buffer.putInt(position + 24, quad.getLight(i));

                position += format.getStride();
            }
        });

        registerEncoder(CHUNK_MESH_HFP, (format, buffer, position, quad, x, y, z) -> {
            for (int i = 0; i < 4; i++) {
                buffer.putShort(position, HFloat.encodeHalfS(quad.getX(i) + x));
                buffer.putShort(position + 2, HFloat.encodeHalfS(quad.getY(i) + y));
                buffer.putShort(position + 4, HFloat.encodeHalfS(quad.getZ(i) + z));
                buffer.putInt(position + 8, quad.getColor(i));
                buffer.putShort(position + 12, HFloat.encodeHalfS(quad.getTexU(i)));
                buffer.putShort(position + 14, HFloat.encodeHalfS(quad.getTexV(i)));
                buffer.putInt(position + 16, quad.getLight(i));

                position += format.getStride();
            }
        });
    }

    public static void registerEncoder(GlVertexFormat<?> format, ModelQuadEncoder encoder) {
        if (encoders.containsKey(format)) {
            throw new IllegalStateException("Encoder already registered for format: " + format);
        }

        encoders.put(format, encoder);
    }

    public static ModelQuadEncoder getEncoder(GlVertexFormat<?> format) {
        ModelQuadEncoder encoder = encoders.get(format);

        if (encoder == null) {
            throw new NullPointerException("No encoder exists for format: " + format);
        }

        return encoder;
    }

    public enum ChunkMeshAttribute {
        POSITION,
        COLOR,
        TEXTURE,
        LIGHT
    }
}
