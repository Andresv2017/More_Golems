package net.darkblade.moregolems.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.darkblade.moregolems.sever.entity.custom.SlimeGolemEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

public class SlimeGolemShellLayer extends GeoRenderLayer<SlimeGolemEntity> {

    private static final ResourceLocation SHELL_TEXTURE =
            new ResourceLocation("moregolems", "textures/entity/slime_golem_shell.png");

    public SlimeGolemShellLayer(GeoRenderer<SlimeGolemEntity> entityRenderer) {
        super(entityRenderer);
    }

    @Override
    public void render(PoseStack poseStack,
                        SlimeGolemEntity animatable,
                        BakedGeoModel bakedModel,
                        RenderType renderType,
                        MultiBufferSource bufferSource,
                        VertexConsumer buffer,
                        float partialTick,
                        int packedLight,
                        int packedOverlay) {

        RenderType shellRenderType = RenderType.entityTranslucentCull(SHELL_TEXTURE);

        this.getRenderer().reRender(
                bakedModel,
                poseStack,
                bufferSource,
                animatable,
                shellRenderType,
                bufferSource.getBuffer(shellRenderType),
                partialTick,
                packedLight,
                packedOverlay,
                1.0F, 1.0F, 1.0F, 1.0F
        );
    }
}
