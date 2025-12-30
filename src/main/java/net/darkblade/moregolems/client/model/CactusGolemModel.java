package net.darkblade.moregolems.client.model;

import net.darkblade.moregolems.MoreGolems;
import net.darkblade.moregolems.sever.entity.custom.CactusGolemEntity;
import net.minecraft.resources.ResourceLocation;

public class CactusGolemModel extends GolemModel<CactusGolemEntity> {
    @Override
    public ResourceLocation getModelResource(CactusGolemEntity animatable) {
        return new ResourceLocation(MoreGolems.MODID, "geo/entity/cactus_golem.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(CactusGolemEntity animatable) {
        return new ResourceLocation(MoreGolems.MODID, "textures/entity/cactus_golem.png");
    }

    @Override
    public ResourceLocation getAnimationResource(CactusGolemEntity animatable) {
        return new ResourceLocation(MoreGolems.MODID, "animations/entity/cactus_golem.animation.json");
    }
}