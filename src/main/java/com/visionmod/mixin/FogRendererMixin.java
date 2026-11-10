
package com.visionmod.mixin;

import com.visionmod.VisionModClient;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.fog.FogRenderer;
import net.minecraft.world.level.material.FogType;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(FogRenderer.class)
public abstract class FogRendererMixin {

    @ModifyArgs(
        method = "setupFog",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/fog/FogRenderer;updateBuffer(Ljava/nio/ByteBuffer;ILorg/joml/Vector4f;FFFFFF)V"
        ),
        require = 0
    )
    private void visionmod$removeLiquidFog(Args args) {

        if (!VisionModClient.enabled) {
            return;
        }

        Minecraft client = Minecraft.getInstance();

        if (client.gameRenderer == null) {
            return;
        }

        Camera camera =
            client.gameRenderer.getMainCamera();

        FogType fluid = camera.getFluidInCamera();

        boolean removeWaterFog =
            fluid == FogType.WATER
                && VisionModClient.isWaterEnabled();

        boolean removeLavaFog =
            fluid == FogType.LAVA
                && VisionModClient.isLavaEnabled();

        if (!removeWaterFog && !removeLavaFog) {
            return;
        }

        // Отодвигаем начало и конец тумана
        // за пределы нормальной дальности обзора.
        float far = 1000000.0F;

        args.set(3, far);
        args.set(4, far + 1000.0F);

        args.set(5, far);
        args.set(6, far + 1000.0F);
    }
}
