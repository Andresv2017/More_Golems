package net.darkblade.moregolems.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.darkblade.moregolems.client.model.SlimeGolemModel;
import net.darkblade.moregolems.sever.entity.custom.SlimeGolemEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class SlimeGolemRenderer extends GeoEntityRenderer<SlimeGolemEntity> {
    public SlimeGolemRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new SlimeGolemModel());
        this.shadowRadius = 1.0f;
    }

    @Override
    public RenderType getRenderType(SlimeGolemEntity animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(texture);
    }

    @Override
    public void render(SlimeGolemEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        float scale = entity.getSizeScale();
        this.shadowRadius = 1.0f * scale;

        poseStack.pushPose();
        poseStack.scale(scale, scale, scale);
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        poseStack.popPose();
    }
}
