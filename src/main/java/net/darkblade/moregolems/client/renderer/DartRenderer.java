package net.darkblade.moregolems.client.renderer;

import net.darkblade.moregolems.client.model.DartModel;
import net.darkblade.moregolems.sever.entity.custom.DartEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class DartRenderer extends GeoEntityRenderer<DartEntity> {
    public DartRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new DartModel());
    }
}