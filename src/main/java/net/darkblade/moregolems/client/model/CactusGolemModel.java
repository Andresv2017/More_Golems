package net.darkblade.moregolems.client.model;

import net.darkblade.moregolems.MoreGolems;
import net.darkblade.moregolems.sever.entity.custom.CactusGolemEntity;
import net.minecraft.resources.ResourceLocation;

public class CactusGolemModel extends GolemModel<CactusGolemEntity> {

    private static final ResourceLocation TEXTURE_BASE = new ResourceLocation(MoreGolems.MODID, "textures/entity/cactus_golem.png");
    private static final ResourceLocation TEXTURE_LOW = new ResourceLocation(MoreGolems.MODID, "textures/entity/cactus_golem_low.png");
    private static final ResourceLocation TEXTURE_MEDIUM = new ResourceLocation(MoreGolems.MODID, "textures/entity/cactus_golem_medium.png");
    private static final ResourceLocation TEXTURE_HIGH = new ResourceLocation(MoreGolems.MODID, "textures/entity/cactus_golem_high.png");

    @Override
    public ResourceLocation getModelResource(CactusGolemEntity animatable) {
        return new ResourceLocation(MoreGolems.MODID, "geo/entity/cactus_golem.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(CactusGolemEntity animatable) {
        float health = animatable.getHealth();
        float maxHealth = animatable.getMaxHealth();
        float percentage = health / maxHealth;

        if (percentage <= 0.25f) {
            return TEXTURE_HIGH;
        } else if (percentage <= 0.5f) {
            return TEXTURE_MEDIUM;
        } else if (percentage <= 0.75f) {
            return TEXTURE_LOW;
        } else {
            return TEXTURE_BASE;
        }
    }

    @Override
    public ResourceLocation getAnimationResource(CactusGolemEntity animatable) {
        return new ResourceLocation(MoreGolems.MODID, "animations/entity/cactus_golem.animation.json");
    }
}