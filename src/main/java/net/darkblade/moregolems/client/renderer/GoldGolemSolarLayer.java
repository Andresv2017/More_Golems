package net.darkblade.moregolems.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.darkblade.moregolems.sever.entity.custom.GoldGolemEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

public class GoldGolemSolarLayer extends GeoRenderLayer<GoldGolemEntity> {

    private static final ResourceLocation SOLAR_TEXTURE =
            new ResourceLocation("moregolems", "textures/entity/gold_golem_layer.png");

    public GoldGolemSolarLayer(GeoRenderer<GoldGolemEntity> entityRenderer) {
        super(entityRenderer);
    }

    @Override
    public void render(PoseStack poseStack,
                       GoldGolemEntity animatable,
                       BakedGeoModel bakedModel,
                       RenderType renderType,
                       MultiBufferSource bufferSource,
                       VertexConsumer buffer,
                       float partialTick,
                       int packedLight,
                       int packedOverlay) {

        int flashTicks = animatable.getSolarFlashTicks();

        if (flashTicks <= 0) {
            return;
        }

        float r = 1.0F;
        float g = 1.0F;
        float b = 1.0F;

        float maxAlpha = 1.0F;

        float maxDuration = 15.0F;

        float interpolatedTicks = (float) flashTicks - partialTick;
        float fade = Math.max(0, interpolatedTicks / maxDuration);

        float currentAlpha = fade * maxAlpha;

        if (currentAlpha < 0.05F) return;

        RenderType solarRenderType = RenderType.entityTranslucentEmissive(SOLAR_TEXTURE);

        this.getRenderer().reRender(
                bakedModel,
                poseStack,
                bufferSource,
                animatable,
                solarRenderType,
                bufferSource.getBuffer(solarRenderType),
                partialTick,
                packedLight,
                packedOverlay,
                r, g, b,
                currentAlpha
        );
    }
}