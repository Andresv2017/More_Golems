package net.darkblade.moregolems.client.model;

import net.darkblade.moregolems.MoreGolems;
import net.darkblade.moregolems.sever.entity.custom.CastleGolemEntity;
import net.minecraft.resources.ResourceLocation;

public class CastleGolemModel extends GolemModel<CastleGolemEntity> {

    @Override
    public ResourceLocation getModelResource(CastleGolemEntity animatable) {
        return new ResourceLocation(MoreGolems.MODID, "geo/entity/castle_golem.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(CastleGolemEntity animatable) {
        if (animatable.getCastleState() == 2) {
            return new ResourceLocation(MoreGolems.MODID, "textures/entity/castle_golem_castle.png");
        }

        return new ResourceLocation(MoreGolems.MODID, "textures/entity/castle_golem.png");
    }

    @Override
    public ResourceLocation getAnimationResource(CastleGolemEntity animatable) {
        return new ResourceLocation(MoreGolems.MODID, "animations/entity/castle_golem.animation.json");
    }
}