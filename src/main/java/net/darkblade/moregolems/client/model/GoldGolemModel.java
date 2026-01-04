package net.darkblade.moregolems.client.model;

import net.darkblade.moregolems.MoreGolems;
import net.darkblade.moregolems.sever.entity.custom.GoldGolemEntity;
import net.minecraft.resources.ResourceLocation;

public class GoldGolemModel extends GolemModel<GoldGolemEntity> {
    @Override
    public ResourceLocation getModelResource(GoldGolemEntity animatable) {
        return new ResourceLocation(MoreGolems.MODID, "geo/entity/gold_golem.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(GoldGolemEntity animatable) {
        return new ResourceLocation(MoreGolems.MODID, "textures/entity/gold_golem.png");
    }

    @Override
    public ResourceLocation getAnimationResource(GoldGolemEntity animatable) {
        return new ResourceLocation(MoreGolems.MODID, "animations/entity/gold_golem.animation.json");
    }
}