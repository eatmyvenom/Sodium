package me.jellysquid.mods.sodium.mixin.pipeline;

import com.google.common.collect.ImmutableList;
import me.jellysquid.mods.sodium.client.render.model.quad.ModelQuadConsumer;
import me.jellysquid.mods.sodium.client.render.model.quad.ModelQuadViewMutable;
import me.jellysquid.mods.sodium.client.util.BufferUtil;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.FixedColorVertexConsumer;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormatElement;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.nio.ByteBuffer;

@Mixin(BufferBuilder.class)
public abstract class MixinBufferBuilder extends FixedColorVertexConsumer implements ModelQuadConsumer {
    @Shadow
    private VertexFormat format;

    @Shadow
    private int currentElementId;

    @Shadow
    private int elementOffset;

    @Shadow
    private VertexFormatElement currentElement;

    @Shadow
    private ByteBuffer buffer;

    @Shadow
    protected abstract void grow(int size);

    @Shadow
    public abstract void vertex(float x, float y, float z, float red, float green, float blue, float alpha, float u, float v, int overlay, int light, float normalX, float normalY, float normalZ);

    @Shadow
    private int vertexCount;

    /**
     * @author JellySquid
     */
    @Overwrite
    public void nextElement() {
        ImmutableList<VertexFormatElement> elements = this.format.getElements();

        // avoid the modulo!
        if (++this.currentElementId >= elements.size()) {
            this.currentElementId -= elements.size();
        }

        this.elementOffset += this.currentElement.getSize();
        this.currentElement = elements.get(this.currentElementId);

        if (this.currentElement.getType() == VertexFormatElement.Type.PADDING) {
            this.nextElement();
        }

        if (this.colorFixed && this.currentElement.getType() == VertexFormatElement.Type.COLOR) {
            this.color(this.fixedRed, this.fixedGreen, this.fixedBlue, this.fixedAlpha);
        }
    }

    @Override
    public void write(ModelQuadViewMutable quad) {
        int[] data = quad.getVertexData();

        int bytes = data.length * 4;

        this.grow(bytes);

        BufferUtil.copyIntArray(data, data.length, this.elementOffset, this.buffer);

        this.vertexCount += 4;
        this.elementOffset += bytes;
    }
}
