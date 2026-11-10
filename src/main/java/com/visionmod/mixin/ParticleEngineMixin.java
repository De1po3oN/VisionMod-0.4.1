
package com.visionmod.mixin;

import com.visionmod.VisionModClient;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.TerrainParticle;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ParticleTypes;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ParticleEngine.class)
public abstract class ParticleEngineMixin {

    @Inject(
        method = "addParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)Lnet/minecraft/client/particle/Particle;",
        at = @At("HEAD"),
        cancellable = true,
        require = 0
    )
    private void visionmod$filterParticles(
            ParticleOptions options,
            double x,
            double y,
            double z,
            double velocityX,
            double velocityY,
            double velocityZ,
            CallbackInfoReturnable<Particle> cir
    ) {

        if (!VisionModClient.isParticlesEnabled()) {
            return;
        }

        if (VisionModClient.hideAllParticles) {
            cir.setReturnValue(null);
            return;
        }

        ParticleType<?> type = options.getType();

        // Частицы разрушения / взаимодействия с блоками
        if (VisionModClient.hideBlockParticles &&
                (type == ParticleTypes.BLOCK
                 || type == ParticleTypes.FALLING_DUST)) {

            cir.setReturnValue(null);
            return;
        }

        // Эндер-сундук использует общие частицы PORTAL
        if (VisionModClient.hideEnderChestParticles &&
                (type == ParticleTypes.PORTAL
                 || type == ParticleTypes.REVERSE_PORTAL)) {

            cir.setReturnValue(null);
        }
    }

    // Дополнительно перехватываем частицы,
    // напрямую добавляемые в менеджер.
    @Inject(
        method = "addParticle(Lnet/minecraft/client/particle/Particle;)V",
        at = @At("HEAD"),
        cancellable = true,
        require = 0
    )
    private void visionmod$filterDirectParticles(
            Particle particle,
            CallbackInfo ci
    ) {

        if (!VisionModClient.isParticlesEnabled()) return;
        if (VisionModClient.hideAllParticles ||
            (VisionModClient.hideBlockParticles && particle instanceof TerrainParticle)) {
            ci.cancel();
        }
    }
}

