package me.jellysquid.mods.sodium.client.render.chunk.compile;

import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexFormat;
import me.jellysquid.mods.sodium.client.gl.buffer.BufferUploadData;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkMesh;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkMeshBuilder;
import me.jellysquid.mods.sodium.client.render.layer.BlockRenderPass;
import me.jellysquid.mods.sodium.client.render.layer.BlockRenderPassManager;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.util.math.Vector3d;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

public class ChunkBuildBuffers {
    private final ChunkMeshBuilder[] builders = new ChunkMeshBuilder[BlockRenderPass.count()];
    private final GlVertexFormat<?> format;

    private final BlockRenderPassManager renderPassManager;

    public ChunkBuildBuffers(GlVertexFormat<?> format, BlockRenderPassManager renderPassManager) {
        this.format = format;
        this.renderPassManager = renderPassManager;

        for (RenderLayer layer : RenderLayer.getBlockLayers()) {
            this.builders[renderPassManager.getRenderPassId(layer)] = new ChunkMeshBuilder(format, layer.getExpectedBufferSize());
        }
    }

    public ChunkMeshBuilder get(RenderLayer layer) {
        return this.builders[this.renderPassManager.getRenderPassId(layer)];
    }

    public List<ChunkMesh> createMeshes(Vector3d camera, BlockPos pos) {
        List<ChunkMesh> layers = new ArrayList<>();

        for (int i = 0; i < this.builders.length; i++) {
            ChunkMeshBuilder builder = this.builders[i];

            if (builder == null || builder.isEmpty()) {
                continue;
            }

            BlockRenderPass pass = this.renderPassManager.getRenderPass(i);

            if (pass.isTranslucent()) {
                builder.sortQuads((float) camera.x - (float) pos.getX(),
                        (float) camera.y - (float) pos.getY(),
                        (float) camera.z - (float) pos.getZ());
            }

            BufferUploadData upload = new BufferUploadData(builder.end(), this.format);
            layers.add(new ChunkMesh(pass, upload));
        }

        return layers;
    }
}
