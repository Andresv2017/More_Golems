package net.darkblade.moregolems.sever.item.custom;

import net.darkblade.moregolems.sever.entity.custom.DartEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TippedArrowItem;
import net.minecraft.world.level.Level;

public class DartItem extends TippedArrowItem {

    public DartItem(Properties properties) {
        super(properties);
    }

    @Override
    public AbstractArrow createArrow(Level level, ItemStack stack, LivingEntity shooter) {
        DartEntity dart = new DartEntity(level, shooter);

        dart.setEffectsFromItem(stack);

        return dart;
    }
}