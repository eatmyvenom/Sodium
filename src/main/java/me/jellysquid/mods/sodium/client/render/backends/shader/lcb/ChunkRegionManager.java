package me.jellysquid.mods.sodium.client.render.backends.shader.lcb;

import it.unimi.dsi.fastutil.longs.Long2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import me.jellysquid.mods.sodium.client.util.MathUtil;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import org.apache.commons.lang3.Validate;

public class ChunkRegionManager {
    // Buffers span 4x2x4 chunks
    private static final int BUFFER_WIDTH = 4;
    private static final int BUFFER_HEIGHT = 4;
    private static final int BUFFER_LENGTH = 4;
    private static final int BUFFER_SIZE = BUFFER_WIDTH * BUFFER_HEIGHT * BUFFER_LENGTH;

    private static final int BUFFER_WIDTH_SH = Integer.bitCount(BUFFER_WIDTH - 1);
    private static final int BUFFER_HEIGHT_SH = Integer.bitCount(BUFFER_HEIGHT - 1);
    private static final int BUFFER_LENGTH_SH = Integer.bitCount(BUFFER_LENGTH - 1);

    private static final int BUFFER_WIDTH_M = -BUFFER_WIDTH;
    private static final int BUFFER_HEIGHT_M = -BUFFER_HEIGHT;
    private static final int BUFFER_LENGTH_M = -BUFFER_LENGTH;

    static {
        Validate.isTrue(MathUtil.isPowerOfTwo(BUFFER_WIDTH));
        Validate.isTrue(MathUtil.isPowerOfTwo(BUFFER_LENGTH));
        Validate.isTrue(MathUtil.isPowerOfTwo(BUFFER_HEIGHT));
    }

    private final Long2ReferenceOpenHashMap<ChunkRegion> regions = new Long2ReferenceOpenHashMap<>();

    public ChunkRegion createRegion(ChunkSectionPos pos) {
        ChunkRegion region = this.regions.get(getIndex(pos));

        if (region == null) {
            ChunkSectionPos origin = ChunkSectionPos.from(pos.getX() & BUFFER_WIDTH_M, pos.getY() & BUFFER_HEIGHT_M, pos.getZ() & BUFFER_LENGTH_M);
            region = new ChunkRegion(origin, BUFFER_SIZE);

            this.regions.put(getIndex(pos), region);
        }

        return region;
    }

    public static long getIndex(ChunkSectionPos pos) {
        return ChunkSectionPos.asLong(pos.getX() >> BUFFER_WIDTH_SH, pos.getY() >> BUFFER_HEIGHT_SH, pos.getZ() >> BUFFER_LENGTH_SH);
    }

    public void delete() {
        for (ChunkRegion region : this.regions.values()) {
            region.delete();
        }

        this.regions.clear();
    }

    public void cleanup() {
        for (ObjectIterator<ChunkRegion> iterator = this.regions.values().iterator(); iterator.hasNext(); ) {
            ChunkRegion block = iterator.next();

            if (block.isEmpty()) {
                block.delete();
                iterator.remove();
            }
        }
    }

    public BlockPos getRenderOffset(ChunkSectionPos pos) {
        return new BlockPos((pos.getX() & BUFFER_WIDTH_M) << 4, (pos.getY() & BUFFER_HEIGHT_M) << 4, (pos.getZ() & BUFFER_LENGTH_M) << 4);
    }

}
