package net.darkblade.moregolems.client.model;

import net.darkblade.moregolems.MoreGolems;
import net.darkblade.moregolems.sever.entity.custom.BlackstoneGolemEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;

public class BlackstoneGolemModel extends GeoModel<BlackstoneGolemEntity> {

    private static final ResourceLocation MODEL = new ResourceLocation(MoreGolems.MODID, "geo/entity/blackstone_golem.geo.json");
    private static final ResourceLocation TEXTURE = new ResourceLocation(MoreGolems.MODID, "textures/entity/blackstone_golem.png");
    private static final ResourceLocation ANIMATIONS = new ResourceLocation(MoreGolems.MODID, "animations/entity/blackstone_golem.animation.json");

    @Override
    public ResourceLocation getModelResource(BlackstoneGolemEntity animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(BlackstoneGolemEntity animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(BlackstoneGolemEntity animatable) {
        return ANIMATIONS;
    }

    @Override
    public void setCustomAnimations(BlackstoneGolemEntity animatable, long instanceId, AnimationState<BlackstoneGolemEntity> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);

        float partialTick = animationState.getPartialTick();

        CoreGeoBone both = getAnimationProcessor().getBone("both");
        if (both != null) {
            both.setRotY(-animatable.getSpinAngle(partialTick) * Mth.DEG_TO_RAD);
        }

        float lift = animatable.getSpinLift(partialTick);
        if (lift > 0.0F) {
            levelArm(getAnimationProcessor().getBone("bone"), lift);
            levelArm(getAnimationProcessor().getBone("bone2"), lift);
        }
    }

    private static void levelArm(CoreGeoBone arm, float lift) {
        if (arm == null) return;
        float keep = 1.0F - lift;
        arm.setRotX(arm.getRotX() * keep);
        arm.setRotY(arm.getRotY() * keep);
        arm.setRotZ(arm.getRotZ() * keep);
    }
}
