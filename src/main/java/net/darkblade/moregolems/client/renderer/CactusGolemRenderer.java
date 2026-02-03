package net.darkblade.moregolems.client.renderer;

import net.darkblade.moregolems.client.model.CactusGolemModel;
import net.darkblade.moregolems.sever.entity.custom.CactusGolemEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class CactusGolemRenderer extends GeoEntityRenderer<CactusGolemEntity> {
    public CactusGolemRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new CactusGolemModel());
        this.shadowRadius = 0.5f;
    }
}