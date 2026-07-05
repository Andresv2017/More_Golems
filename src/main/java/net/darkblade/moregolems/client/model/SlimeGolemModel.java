package net.darkblade.moregolems.client.model;

import net.darkblade.moregolems.MoreGolems;
import net.darkblade.moregolems.sever.entity.custom.SlimeGolemEntity;
import net.minecraft.resources.ResourceLocation;

public class SlimeGolemModel extends GolemModel<SlimeGolemEntity> {

    private static final ResourceLocation MODEL = new ResourceLocation(MoreGolems.MODID, "geo/entity/slime_golem.geo.json");
    private static final ResourceLocation TEXTURE = new ResourceLocation(MoreGolems.MODID, "textures/entity/slime_golem.png");
    private static final ResourceLocation ANIMATIONS = new ResourceLocation(MoreGolems.MODID, "animations/entity/slime_golem.animation.json");

    @Override
    public ResourceLocation getModelResource(SlimeGolemEntity animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(SlimeGolemEntity animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(SlimeGolemEntity animatable) {
        return ANIMATIONS;
    }
}
