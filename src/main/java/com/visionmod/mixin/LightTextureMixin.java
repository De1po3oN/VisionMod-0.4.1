
package com.visionmod.mixin;

import com.visionmod.VisionModClient;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.renderer.LightTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LightTexture.class)
public abstract class LightTextureMixin {

    @ModifyExpressionValue(
        method = "updateLightTexture",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/OptionInstance;get()Ljava/lang/Object;",
            ordinal = 2
        ),
        require = 0
    )
    private Object visionmod$fullbright(Object original) {

        if (VisionModClient.isFullBrightEnabled()) {
            return 16.0D;
        }

        return original;
    }
}
