package me.jellysquid.mods.sodium.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import me.jellysquid.mods.sodium.client.SodiumClientMod;
import me.jellysquid.mods.sodium.client.gl.SodiumVertexFormats;
import me.jellysquid.mods.sodium.client.gl.array.GlVertexArray;
import me.jellysquid.mods.sodium.client.gui.SodiumGameOptions;
import me.jellysquid.mods.sodium.client.render.backends.ChunkRenderBackend;
import me.jellysquid.mods.sodium.client.render.backends.shader.lcb.ShaderLCBChunkRenderBackend;
import me.jellysquid.mods.sodium.client.render.backends.shader.vao.ShaderVAOChunkRenderBackend;
import me.jellysquid.mods.sodium.client.render.backends.shader.vbo.ShaderVBOChunkRenderBackend;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkRenderData;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkRenderManager;
import me.jellysquid.mods.sodium.client.render.layer.BlockRenderPass;
import me.jellysquid.mods.sodium.client.render.layer.BlockRenderPassManager;
import me.jellysquid.mods.sodium.client.util.RenderList;
import me.jellysquid.mods.sodium.client.world.ChunkManagerWithStatusListener;
import me.jellysquid.mods.sodium.client.world.ChunkStatusListener;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.*;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.client.render.model.ModelLoader;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.*;

import java.util.Collection;
import java.util.Set;
import java.util.SortedSet;

public class SodiumWorldRenderer implements ChunkStatusListener {
    private static SodiumWorldRenderer instance;

    private final MinecraftClient client;

    private ClientWorld world;
    private int renderDistance;

    private double lastTranslucentSortX;
    private double lastTranslucentSortY;
    private double lastTranslucentSortZ;

    private double lastCameraX;
    private double lastCameraY;
    private double lastCameraZ;

    private double lastCameraPitch;
    private double lastCameraYaw;

    private boolean useEntityCulling;

    private final LongSet loadedChunkPositions = new LongOpenHashSet();
    private final Set<BlockEntity> globalBlockEntities = new ObjectOpenHashSet<>();

    private Frustum frustum;
    private ChunkRenderManager<?> chunkRenderManager;
    private BlockRenderPassManager renderPassManager;
    private ChunkRenderBackend<?> chunkRenderBackend;

    public static SodiumWorldRenderer create() {
        if (instance == null) {
            instance = new SodiumWorldRenderer(MinecraftClient.getInstance());
        }

        return instance;
    }

    private SodiumWorldRenderer(MinecraftClient client) {
        this.client = client;
    }

    public void setWorld(ClientWorld world) {
        this.world = world;
        this.loadedChunkPositions.clear();

        if (world == null) {
            if (this.chunkRenderManager != null) {
                this.chunkRenderManager.destroy();
                this.chunkRenderManager = null;
            }

            if (this.chunkRenderBackend != null) {
                this.chunkRenderBackend.delete();
                this.chunkRenderBackend = null;
            }

            this.loadedChunkPositions.clear();
        } else {
            this.initRenderer();

            ((ChunkManagerWithStatusListener) world.getChunkManager()).setListener(this);
        }
    }

    public int getCompletedChunkCount() {
        int count = 0;

        for (BlockRenderPass pass : BlockRenderPass.VALUES) {
            RenderList<?> list = this.chunkRenderManager.getRenderList(pass);

            if (list != null) {
                count += list.size();
            }
        }

        return count;
    }

    public void scheduleTerrainUpdate() {
        // seems to be called before init
        if (this.chunkRenderManager != null) {
            this.chunkRenderManager.markDirty();
        }
    }

    public boolean isTerrainRenderComplete() {
        return this.chunkRenderManager.isBuildComplete();
    }

    public void update(Camera camera, Frustum frustum, boolean hasForcedFrustum, int frame, boolean spectator) {
        this.frustum = frustum;

        this.applySettings();
        this.chunkRenderManager.onFrameChanged();

        Vec3d cameraPos = camera.getPos();

        if (this.client.options.viewDistance != this.renderDistance) {
            this.reload();
        }

        this.world.getProfiler().push("camera");

        ClientPlayerEntity player = this.client.player;

        if (player == null) {
            throw new IllegalStateException("Client instance has no active player entity");
        }

        this.chunkRenderManager.setCameraPosition(cameraPos.x, cameraPos.y, cameraPos.z);

        this.world.getProfiler().swap("cull");
        this.client.getProfiler().swap("culling");

        float pitch = camera.getPitch();
        float yaw = camera.getYaw();

        boolean dirty = this.chunkRenderManager.isDirty() ||
                cameraPos.x != this.lastCameraX || cameraPos.y != this.lastCameraY || cameraPos.z != this.lastCameraZ ||
                pitch != this.lastCameraPitch || yaw != this.lastCameraYaw;

        if (dirty) {
            this.chunkRenderManager.markDirty();
        }

        this.lastCameraX = cameraPos.x;
        this.lastCameraY = cameraPos.y;
        this.lastCameraZ = cameraPos.z;
        this.lastCameraPitch = pitch;
        this.lastCameraYaw = yaw;

        this.client.getProfiler().swap("update");

        BlockPos blockPos = camera.getBlockPos();

        this.chunkRenderManager.updateChunks();

        if (!hasForcedFrustum && this.chunkRenderManager.isDirty()) {
            this.client.getProfiler().push("iteration");

            this.chunkRenderManager.updateGraph(camera, cameraPos, blockPos, frame, (FrustumExtended) frustum, spectator);

            this.client.getProfiler().pop();
        }

        Entity.setRenderDistanceMultiplier(MathHelper.clamp((double) this.client.options.viewDistance / 8.0D, 1.0D, 2.5D));

        this.client.getProfiler().pop();
    }

    private void applySettings() {
        this.useEntityCulling = SodiumClientMod.options().performance.useAdvancedEntityCulling;
    }

    public void renderLayer(RenderLayer renderLayer, MatrixStack matrixStack, double x, double y, double z) {
        BlockRenderPass pass = this.renderPassManager.getRenderPassForLayer(renderLayer);
        pass.startDrawing();

        this.chunkRenderManager.renderLayer(matrixStack, pass, x, y, z);

        pass.endDrawing();

        RenderSystem.clearCurrentColor();
    }

    public void renderChunkDebugInfo(Camera camera) {
        // TODO: re-implement
    }

    public void reload() {
        if (this.world == null) {
            return;
        }

        this.initRenderer();
    }

    private void initRenderer() {
        if (this.chunkRenderManager != null) {
            this.chunkRenderManager.destroy();
            this.chunkRenderManager = null;
        }

        if (this.chunkRenderBackend != null) {
            this.chunkRenderBackend.delete();
            this.chunkRenderBackend = null;
        }

        this.renderDistance = this.client.options.viewDistance;

        SodiumGameOptions opts = SodiumClientMod.options();

        if (opts.performance.useRenderLayerConsolidation) {
            this.renderPassManager = BlockRenderPassManager.consolidated();
        } else {
            this.renderPassManager = BlockRenderPassManager.vanilla();
        }

        if (GlVertexArray.isSupported() && opts.performance.useLargeBuffers) {
            this.chunkRenderBackend = new ShaderLCBChunkRenderBackend(SodiumVertexFormats.CHUNK_MESH_VANILLA);
        } else if (GlVertexArray.isSupported() && opts.performance.useVertexArrays) {
            this.chunkRenderBackend = new ShaderVAOChunkRenderBackend(SodiumVertexFormats.CHUNK_MESH_VANILLA);
        } else {
            this.chunkRenderBackend = new ShaderVBOChunkRenderBackend(SodiumVertexFormats.CHUNK_MESH_VANILLA);
        }

        this.chunkRenderManager = new ChunkRenderManager<>(this, this.chunkRenderBackend, this.renderPassManager, this.world, this.renderDistance);
        this.chunkRenderManager.restoreChunks(this.loadedChunkPositions);
    }

    public void renderTileEntities(MatrixStack matrices, BufferBuilderStorage bufferBuilders, Long2ObjectMap<SortedSet<BlockBreakingInfo>> blockBreakingProgressions,
                                   Camera camera, float tickDelta) {
        VertexConsumerProvider.Immediate immediate = bufferBuilders.getEntityVertexConsumers();

        Vec3d cameraPos = camera.getPos();
        double x = cameraPos.getX();
        double y = cameraPos.getY();
        double z = cameraPos.getZ();

        for (BlockEntity blockEntity : this.chunkRenderManager.getVisibleBlockEntities()) {
            BlockPos pos = blockEntity.getPos();

            matrices.push();
            matrices.translate((double) pos.getX() - x, (double) pos.getY() - y, (double) pos.getZ() - z);

            VertexConsumerProvider consumer = immediate;
            SortedSet<BlockBreakingInfo> breakingInfos = blockBreakingProgressions.get(pos.asLong());

            if (breakingInfos != null && !breakingInfos.isEmpty()) {
                int stage = breakingInfos.last().getStage();

                if (stage >= 0) {
                    VertexConsumer transformer = new TransformingVertexConsumer(bufferBuilders.getEffectVertexConsumers().getBuffer(ModelLoader.BLOCK_DESTRUCTION_RENDER_LAYERS.get(stage)), matrices.peek());
                    consumer = (layer) -> layer.method_23037() ? VertexConsumers.dual(transformer, immediate.getBuffer(layer)) : immediate.getBuffer(layer);
                }
            }

            BlockEntityRenderDispatcher.INSTANCE.render(blockEntity, tickDelta, matrices, consumer);

            matrices.pop();
        }

        for (BlockEntity blockEntity : this.globalBlockEntities) {
            BlockPos pos = blockEntity.getPos();

            matrices.push();
            matrices.translate((double) pos.getX() - x, (double) pos.getY() - y, (double) pos.getZ() - z);

            BlockEntityRenderDispatcher.INSTANCE.render(blockEntity, tickDelta, matrices, immediate);

            matrices.pop();
        }
    }

    @Override
    public void onChunkAdded(int x, int z) {
        this.loadedChunkPositions.add(ChunkPos.toLong(x, z));
        this.chunkRenderManager.onChunkAdded(x, z);
    }

    @Override
    public void onChunkRemoved(int x, int z) {
        this.loadedChunkPositions.remove(ChunkPos.toLong(x, z));
        this.chunkRenderManager.onChunkRemoved(x, z);
    }

    public void onChunkRenderUpdated(ChunkRenderData meshBefore, ChunkRenderData meshAfter) {
        Collection<BlockEntity> entitiesBefore = meshBefore.getGlobalBlockEntities();

        if (!entitiesBefore.isEmpty()) {
            this.globalBlockEntities.removeAll(entitiesBefore);
        }

        Collection<BlockEntity> entitiesAfter = meshAfter.getGlobalBlockEntities();

        if (!entitiesAfter.isEmpty()) {
            this.globalBlockEntities.addAll(entitiesAfter);
        }
    }

    /**
     * Returns whether or not the entity intersects with any visible chunks in the graph.
     * @return True if the entity is visible, otherwise false
     */
    public boolean isEntityVisible(Entity entity) {
        if (!this.useEntityCulling) {
            return true;
        }

        Box box = entity.getVisibilityBoundingBox();

        int minX = MathHelper.floor(box.x1 - 0.5D) >> 4;
        int minY = MathHelper.floor(box.y1 - 0.5D) >> 4;
        int minZ = MathHelper.floor(box.z1 - 0.5D) >> 4;

        int maxX = MathHelper.floor(box.x2 + 0.5D) >> 4;
        int maxY = MathHelper.floor(box.y2 + 0.5D) >> 4;
        int maxZ = MathHelper.floor(box.z2 + 0.5D) >> 4;

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int y = minY; y <= maxY; y++) {
                    if (this.chunkRenderManager.isChunkVisible(x, y, z)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public static SodiumWorldRenderer getInstance() {
        if (instance == null) {
            throw new IllegalStateException("Renderer not initialized");
        }

        return instance;
    }

    public Frustum getFrustum() {
        return this.frustum;
    }

    public String getChunksDebugString() {
        // C: visible/total
        return String.format("C: %s/%s", this.chunkRenderManager.getVisibleSectionCount(), this.chunkRenderManager.getTotalSections());
    }

    public void scheduleRebuildForArea(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, boolean important) {
        int minChunkX = minX >> 4;
        int minChunkY = minY >> 4;
        int minChunkZ = minZ >> 4;

        int maxChunkX = maxX >> 4;
        int maxChunkY = maxY >> 4;
        int maxChunkZ = maxZ >> 4;

        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkY = minChunkY; chunkY <= maxChunkY; chunkY++) {
                for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                    this.scheduleRebuildForChunk(chunkX, chunkY, chunkZ, important);
                }
            }
        }
    }

    public void scheduleRebuildForChunk(int x, int y, int z, boolean important) {
        this.chunkRenderManager.scheduleRebuild(x, y, z, important);
    }
}
