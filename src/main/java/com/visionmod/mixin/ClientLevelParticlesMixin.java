package com.visionmod.mixin;

import com.visionmod.VisionModClient;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Breaking/cracking block particles bypass normal particle type creation.
 * Block destruction, hit effects and the mining-progress particles must
 * all be filtered at their shared client-level entry points.
 */
@Mixin(ClientLevel.class)
public abstract class ClientLevelParticlesMixin {
    private static boolean hide() {
        return VisionModClient.isParticlesEnabled() &&
            (VisionModClient.hideAllParticles || VisionModClient.hideBlockParticles);
    }

    @Inject(method = "addBreakingBlockEffect(Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;)V", at = @At("HEAD"), cancellable = true, require = 0)
    private void visionmod$hideMiningProgress(BlockPos pos, Direction direction, CallbackInfo ci) {
        if (hide()) ci.cancel();
    }

    @Inject(method = "addDestroyBlockEffect(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)V", at = @At("HEAD"), cancellable = true, require = 0)
    private void visionmod$hideBlockDestruction(BlockPos pos, BlockState state, CallbackInfo ci) {
        if (hide()) ci.cancel();
    }
}
