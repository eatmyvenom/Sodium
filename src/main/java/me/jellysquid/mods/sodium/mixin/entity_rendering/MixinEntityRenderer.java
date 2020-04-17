package me.jellysquid.mods.sodium.mixin.entity_rendering;

import me.jellysquid.mods.sodium.client.SodiumClientMod;
import me.jellysquid.mods.sodium.client.gui.SodiumGameOptions;
import me.jellysquid.mods.sodium.client.render.SodiumWorldRenderer;
import me.jellysquid.mods.sodium.client.render.light.EntityLighter;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.LightType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderer.class)
public abstract class MixinEntityRenderer<T extends Entity> {
    @Shadow
    protected abstract int getBlockLight(T entity, float tickDelta);

    private static final int POISON_VALUE = 0xDEADBEEF;

    private boolean isAdvancedLightEnabled = false;

    /**
     * @reason Use a return value poison to detect the default light path being used
     * @author JellySquid
     */
    @Inject(method = "getBlockLight", at = @At("HEAD"), cancellable = true)
    public void getBlockLight(T entity, float tickDelta, CallbackInfoReturnable<Integer> cir) {
        if (this.isAdvancedLightEnabled) {
            cir.setReturnValue(POISON_VALUE);
        }
    }

    /**
     * @reason Check that the entity renderer doesn't have special lighting, and if so, use our new lighting system
     * @author JellySquid
     */
    @Overwrite
    public final int getLight(T entity, float tickDelta) {
        int blockLight = this.getBlockLightWrapper(entity, tickDelta);

        if (blockLight == 0xDEADBEEF) {
            return EntityLighter.getAdvancedLight(entity, tickDelta);
        }

        return this.getSimpleLight(entity, tickDelta, blockLight);
    }

    private int getBlockLightWrapper(T entity, float tickDelta) {
        this.isAdvancedLightEnabled = SodiumClientMod.options().quality.smoothLighting == SodiumGameOptions.LightingQuality.HIGH;

        int blockLight;

        try {
            blockLight = this.getBlockLight(entity, tickDelta);
        } finally {
            // We're just being paranoid with a finally block...
            this.isAdvancedLightEnabled = false;
        }

        return blockLight;
    }

    // [VanillaCopy] EntityRenderer#getLight(Entity, float)
    private int getSimpleLight(T entity, float tickDelta, int blockLight) {
        return LightmapTextureManager.pack(blockLight, entity.world.getLightLevel(LightType.SKY, new BlockPos(entity.getCameraPosVec(tickDelta))));
    }

    @Inject(method = "shouldRender", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Frustum;isVisible(Lnet/minecraft/util/math/Box;)Z", shift = At.Shift.AFTER), cancellable = true)
    private void preShouldRender(T entity, Frustum frustum, double x, double y, double z, CallbackInfoReturnable<Boolean> cir) {
        // If the entity isn't culled already by other means, try to perform a second pass
        if (cir.getReturnValue() && !SodiumWorldRenderer.getInstance().isEntityVisible(entity)) {
            cir.setReturnValue(false);
        }
    }
}
