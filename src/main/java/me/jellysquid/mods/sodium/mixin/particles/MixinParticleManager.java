package me.jellysquid.mods.sodium.mixin.particles;

import me.jellysquid.mods.sodium.client.SodiumClientMod;
import me.jellysquid.mods.sodium.client.render.SodiumWorldRenderer;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.client.particle.ParticleTextureSheet;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayDeque;
import java.util.Map;
import java.util.Queue;

@Mixin(ParticleManager.class)
public class MixinParticleManager {
    @Shadow
    @Final
    private Map<ParticleTextureSheet, Queue<Particle>> particles;

    private final Queue<Particle> cachedQueue = new ArrayDeque<>();

    private Frustum cullingFrustum;

    @Inject(method = "renderParticles", at = @At("HEAD"))
    private void preRenderParticles(MatrixStack matrixStack, VertexConsumerProvider.Immediate immediate, LightmapTextureManager lightmapTextureManager, Camera camera, float f, CallbackInfo ci) {
        Frustum frustum = SodiumWorldRenderer.getInstance().getFrustum();
        boolean useCulling = SodiumClientMod.options().performance.useParticleCulling;

        // Setup the frustum state before rendering particles
        if (useCulling && frustum != null) {
            this.cullingFrustum = frustum;
        } else {
            this.cullingFrustum = null;
        }
    }

    @SuppressWarnings({ "SuspiciousMethodCalls", "unchecked" })
    @Redirect(method = "renderParticles", at = @At(value = "INVOKE", target = "Ljava/util/Map;get(Ljava/lang/Object;)Ljava/lang/Object;"))
    private <V> V filterParticleList(Map<ParticleTextureSheet, Queue<Particle>> map, Object key, MatrixStack matrixStack, VertexConsumerProvider.Immediate immediate, LightmapTextureManager lightmapTextureManager, Camera camera, float f) {
        Queue<Particle> queue = this.particles.get(key);

        if (queue == null || queue.isEmpty()) {
            return null;
        }

        // If the frustum isn't available (whether disabled or some other issue arose), simply return the queue as-is
        if (this.cullingFrustum == null) {
            return (V) queue;
        }

        // Filter particles which are not visible
        Queue<Particle> filtered = this.cachedQueue;
        filtered.clear();

        for (Particle particle : queue) {
            if (this.cullingFrustum.isVisible(particle.getBoundingBox())) {
                filtered.add(particle);
            }
        }

        return (V) filtered;
    }

    @Inject(method = "renderParticles", at = @At("RETURN"))
    private void postRenderParticles(MatrixStack matrixStack, VertexConsumerProvider.Immediate immediate, LightmapTextureManager lightmapTextureManager, Camera camera, float f, CallbackInfo ci) {
        // Ensure particles don't linger in the temporary collection
        this.cachedQueue.clear();
    }
}
