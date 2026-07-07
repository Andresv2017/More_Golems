package net.darkblade.moregolems.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.darkblade.moregolems.client.SlimeLeashRenderState;
import net.darkblade.moregolems.sever.entity.custom.SlimeGolemEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MobRenderer.class)
public abstract class MobRendererMixin {

    @Inject(
            method = "renderLeash(Lnet/minecraft/world/entity/Mob;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/world/entity/Entity;)V",
            at = @At("HEAD")
    )
    private void moregolems$startLeash(Mob mob, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, Entity leashHolder, CallbackInfo ci) {
        SlimeLeashRenderState.ACTIVE = leashHolder instanceof SlimeGolemEntity;
    }

    @Inject(
            method = "renderLeash(Lnet/minecraft/world/entity/Mob;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/world/entity/Entity;)V",
            at = @At("RETURN")
    )
    private void moregolems$endLeash(Mob mob, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, Entity leashHolder, CallbackInfo ci) {
        SlimeLeashRenderState.ACTIVE = false;
    }

    @Redirect(
            method = "addVertexPair(Lcom/mojang/blaze3d/vertex/VertexConsumer;Lorg/joml/Matrix4f;FFFIIIIFFFFIZ)V",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/VertexConsumer;color(FFFF)Lcom/mojang/blaze3d/vertex/VertexConsumer;")
    )
    private static VertexConsumer moregolems$tintLeash(VertexConsumer consumer, float red, float green, float blue, float alpha) {
        if (SlimeLeashRenderState.ACTIVE) {
            return consumer.color(0.42F, 0.68F, 0.35F, alpha);        }
        return consumer.color(red, green, blue, alpha);
    }
}
