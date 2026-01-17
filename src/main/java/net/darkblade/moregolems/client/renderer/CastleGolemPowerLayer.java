package net.darkblade.moregolems.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.darkblade.moregolems.MoreGolems;
import net.darkblade.moregolems.sever.entity.custom.CastleGolemEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

public class CastleGolemPowerLayer extends GeoRenderLayer<CastleGolemEntity> {

    private static final ResourceLocation POWER_TEXTURE = new ResourceLocation(MoreGolems.MODID, "textures/entity/castle_golem_power.png");

    private static final float ANIMATION_CYCLE_TICKS = 36.0F;

    private static final float PULSE_SPEED = (float) (Math.PI * 2.0 / ANIMATION_CYCLE_TICKS);

    public CastleGolemPowerLayer(GeoRenderer<CastleGolemEntity> entityRendererIn) {
        super(entityRendererIn);
    }

    @Override
    public void render(PoseStack poseStack,
                       CastleGolemEntity animatable,
                       BakedGeoModel bakedModel,
                       RenderType renderType,
                       MultiBufferSource bufferSource,
                       VertexConsumer buffer,
                       float partialTick,
                       int packedLight,
                       int packedOverlay) {

        if (animatable.getCastleState() == 2) {

            float gameTime = animatable.tickCount + partialTick;

            float sineWave = (Mth.sin(gameTime * PULSE_SPEED) + 1.0F) / 2.0F;

            float alpha = sineWave * 0.20F + 0.03F;

            RenderType type = RenderType.entityTranslucentEmissive(POWER_TEXTURE);

            this.getRenderer().reRender(
                    bakedModel,
                    poseStack,
                    bufferSource,
                    animatable,
                    type,
                    bufferSource.getBuffer(type),
                    partialTick,
                    packedLight,
                    packedOverlay,
                    0.18f, 0.78f, 0.82f,
                    alpha
            );
        }
    }
}