package net.darkblade.moregolems.client.renderer;

import net.darkblade.moregolems.client.model.BlackstoneGolemModel;
import net.darkblade.moregolems.sever.entity.custom.BlackstoneGolemEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class BlackstoneGolemRenderer extends GeoEntityRenderer<BlackstoneGolemEntity> {
    public BlackstoneGolemRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new BlackstoneGolemModel());
        this.shadowRadius = 1.2f;
    }
}
