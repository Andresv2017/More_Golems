package net.darkblade.moregolems.client.renderer;

import net.darkblade.moregolems.client.model.GoldGolemModel;
import net.darkblade.moregolems.sever.entity.custom.GoldGolemEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class GoldGolemRenderer extends GeoEntityRenderer<GoldGolemEntity> {
    public GoldGolemRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new GoldGolemModel());
        this.addRenderLayer(new GoldGolemSolarLayer(this));
        this.shadowRadius = 0.9f;
    }
}