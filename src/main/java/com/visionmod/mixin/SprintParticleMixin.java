
package com.visionmod.mixin;

import com.visionmod.VisionModClient;
import net.minecraft.world.entity.Entity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class SprintParticleMixin {

    @Inject(
        method = "spawnSprintParticle",
        at = @At("HEAD"),
        cancellable = true,
        require = 0
    )
    private void visionmod$hideSprintParticle(
            CallbackInfo ci
    ) {

        if (!VisionModClient.isParticlesEnabled()) {
            return;
        }

        if (VisionModClient.hideAllParticles
                || VisionModClient.hideSprintParticles) {
            ci.cancel();
        }
    }
}
