package net.darkblade.moregolems.client.model;

import net.darkblade.moregolems.MoreGolems;
import net.darkblade.moregolems.sever.entity.custom.DartEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class DartModel extends GeoModel<DartEntity> {
    @Override
    public ResourceLocation getModelResource(DartEntity object) {
        return new ResourceLocation(MoreGolems.MODID, "geo/entity/dart.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(DartEntity object) {
        return new ResourceLocation(MoreGolems.MODID, "textures/entity/dart.png");
    }

    @Override
    public ResourceLocation getAnimationResource(DartEntity object) {
        return new ResourceLocation(MoreGolems.MODID, "animations/entity/dart.animation.json");
    }
}