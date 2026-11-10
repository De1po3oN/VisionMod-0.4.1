
package com.visionmod.mixin;

import com.visionmod.VisionModClient;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Avatar;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.WeakHashMap;

@Mixin(AvatarRenderer.class)
public abstract class PlayerHeartsMixin {

    @Unique
    private static final Map<AvatarRenderState, float[]>
        visionmod$health = new WeakHashMap<>();

    @Inject(
        method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V",
        at = @At("TAIL"),
        require = 0
    )
    private void visionmod$captureHealth(
            Avatar player,
            AvatarRenderState state,
            float partialTick,
            CallbackInfo ci
    ) {
        visionmod$health.put(
            state,
            new float[] {
                player.getHealth(),
                player.getMaxHealth()
            }
        );
    }

    @Inject(
        method = "submitNameTag(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V",
        at = @At("TAIL"),
        require = 0
    )
    private void visionmod$drawHearts(
            AvatarRenderState state,
            PoseStack matrices,
            SubmitNodeCollector queue,
            CameraRenderState camera,
            CallbackInfo ci
    ) {
        if (!VisionModClient.isHeartsEnabled()) {
            return;
        }

        if (state.nameTagAttachment == null
                || state.isInvisible) {
            return;
        }

        float[] data = visionmod$health.get(state);

        if (data == null) {
            return;
        }

        float hp = Math.max(0.0F, data[0]);
        float maxHp = Math.max(1.0F, data[1]);

        int slots = Math.min(
            20,
            (int) Math.ceil(maxHp / 2.0F)
        );

        int filled = Math.min(
            slots,
            (int) Math.ceil(hp / 2.0F)
        );

        int empty = slots - filled;

        Component label = Component.literal(
            "♥".repeat(filled)
        ).withStyle(ChatFormatting.RED).append(
            Component.literal(
                "♡".repeat(empty)
            ).withStyle(ChatFormatting.GRAY)
        ).append(
            Component.literal(
                "  " +
                Math.round(hp * 10.0F) / 10.0F +
                "/" +
                Math.round(maxHp * 10.0F) / 10.0F +
                " HP"
            ).withStyle(ChatFormatting.WHITE)
        );

        queue.submitNameTag(
            matrices,
            state.nameTagAttachment.add(
                0.0, 0.30, 0.0
            ),
            0,
            label,
            !state.isDiscrete,
            state.lightCoords,
            state.distanceToCameraSq,
            camera
        );
    }
}
