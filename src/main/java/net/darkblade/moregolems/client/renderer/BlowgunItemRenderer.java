package net.darkblade.moregolems.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.darkblade.moregolems.client.model.BlowgunModel;
import net.darkblade.moregolems.sever.init.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class BlowgunItemRenderer extends BlockEntityWithoutLevelRenderer {

    private final BlowgunModel model;

    public BlowgunItemRenderer(BlockEntityRenderDispatcher berd, EntityModelSet models) {
        super(berd, models);
        this.model = new BlowgunModel(models.bakeLayer(BlowgunModel.LAYER_LOCATION));
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext transformType, PoseStack poseStack,
                             MultiBufferSource buffer, int combinedLightIn, int combinedOverlayIn) {

        ClientLevel level = Minecraft.getInstance().level;
        Player player = Minecraft.getInstance().player;

        boolean heldIn3d =
                transformType == ItemDisplayContext.THIRD_PERSON_LEFT_HAND
                        || transformType == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND
                        || transformType == ItemDisplayContext.FIRST_PERSON_LEFT_HAND
                        || transformType == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND;

        if (heldIn3d) {
            poseStack.pushPose();

            poseStack.translate(0.5F, 0.5F, 0.5F);

            poseStack.scale(0.75F, 0.75F, 0.75F);

            poseStack.mulPose(Axis.XP.rotationDegrees(-180f));
            poseStack.translate(0, -0.5F, 0);

            switch (transformType) {
                case FIRST_PERSON_RIGHT_HAND -> {
                    boolean isUsing = player != null && player.isUsingItem() && player.getUseItem() == stack;

                    if (isUsing) {
                        poseStack.translate(-0.75F, 0.1, 1.1F);
                        poseStack.mulPose(Axis.XP.rotationDegrees(-90));
                        poseStack.mulPose(Axis.YP.rotationDegrees(0));
                    } else {
                        poseStack.mulPose(Axis.YP.rotationDegrees(-85));
                        poseStack.translate(0, -0.6, 0);
                    }
                }
                case FIRST_PERSON_LEFT_HAND -> {
                    poseStack.mulPose(Axis.YP.rotationDegrees(45));
                    poseStack.translate(0, 0, 0);
                }
                case THIRD_PERSON_RIGHT_HAND, THIRD_PERSON_LEFT_HAND -> {
                    poseStack.translate(0, -0.6, 0);
                }
                default -> {}
            }

            VertexConsumer vc = buffer.getBuffer(RenderType.entitySolid(BlowgunModel.TEXTURE));
            model.renderToBuffer(
                    poseStack,
                    vc,
                    combinedLightIn,
                    OverlayTexture.NO_OVERLAY,
                    1F, 1F, 1F, 1F
            );

            poseStack.popPose();
            return;
        }

        poseStack.translate(0.55F, 0.55F, 0.0F);

        ItemStack spriteItem = new ItemStack(ModItems.BLOWGUN_SPRITE.get());
        spriteItem.setTag(stack.getTag());
        renderStaticItemSprite(spriteItem, transformType, combinedLightIn, combinedOverlayIn, poseStack, buffer, level);
    }

    private void renderStaticItemSprite(ItemStack spriteItem, ItemDisplayContext transformType, int combinedLightIn,
                                        int combinedOverlayIn, PoseStack poseStack, MultiBufferSource buffer, ClientLevel level) {
        Minecraft.getInstance().getItemRenderer().renderStatic(
                spriteItem,
                transformType,
                transformType == ItemDisplayContext.GROUND ? combinedLightIn : 240,
                combinedOverlayIn,
                poseStack,
                buffer,
                level,
                0
        );
    }
}