package net.darkblade.moregolems.client.renderer;

import net.darkblade.moregolems.client.model.CastleGolemModel;
import net.darkblade.moregolems.sever.entity.custom.CastleGolemEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class CastleGolemRenderer extends GeoEntityRenderer<CastleGolemEntity> {
    public CastleGolemRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new CastleGolemModel());
        this.shadowRadius = 2.0f;
    }
}