package net.darkblade.moregolems.client.model;

import net.darkblade.moregolems.MoreGolems;
import net.darkblade.moregolems.sever.entity.custom.CastleGolemEntity;
import net.minecraft.resources.ResourceLocation;

public class CastleGolemModel extends GolemModel<CastleGolemEntity> {

    private static final ResourceLocation CASTLE_MODE_TEXTURE = new ResourceLocation(MoreGolems.MODID, "textures/entity/castle_golem_castle.png");

    private static final ResourceLocation TEXTURE_BASE = new ResourceLocation(MoreGolems.MODID, "textures/entity/castle_golem.png");
    private static final ResourceLocation TEXTURE_LOW = new ResourceLocation(MoreGolems.MODID, "textures/entity/castle_golem_low.png");
    private static final ResourceLocation TEXTURE_MEDIUM = new ResourceLocation(MoreGolems.MODID, "textures/entity/castle_golem_medium.png");
    private static final ResourceLocation TEXTURE_HIGH = new ResourceLocation(MoreGolems.MODID, "textures/entity/castle_golem_high.png");

    @Override
    public ResourceLocation getModelResource(CastleGolemEntity animatable) {
        return new ResourceLocation(MoreGolems.MODID, "geo/entity/castle_golem.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(CastleGolemEntity animatable) {
        if (animatable.getCastleState() == 2) {
            return CASTLE_MODE_TEXTURE;
        }

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
    public ResourceLocation getAnimationResource(CastleGolemEntity animatable) {
        return new ResourceLocation(MoreGolems.MODID, "animations/entity/castle_golem.animation.json");
    }
}