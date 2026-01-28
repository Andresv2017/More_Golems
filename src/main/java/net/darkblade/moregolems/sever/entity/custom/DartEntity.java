package net.darkblade.moregolems.sever.entity.custom;

import net.darkblade.moregolems.sever.init.ModEntities;
import net.darkblade.moregolems.sever.init.ModItems;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.Collection;

public class DartEntity extends Arrow implements GeoEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public DartEntity(EntityType<? extends Arrow> entityType, Level level) {
        super(entityType, level);
    }

    public DartEntity(Level level, LivingEntity shooter) {
        super(ModEntities.DART_PROJECTILE.get(), level);

        this.setOwner(shooter);
        this.setPos(shooter.getX(), shooter.getEyeY() - 0.1D, shooter.getZ());
        this.setBaseDamage(1.5);

        if (shooter instanceof Player) {
            this.pickup = AbstractArrow.Pickup.ALLOWED;
        }
    }

    @Override
    protected ItemStack getPickupItem() {
        return new ItemStack(ModItems.DART.get());
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    protected SoundEvent getDefaultHitGroundSoundEvent() {
        return SoundEvents.EMPTY;
    }


    @Override
    public void setEffectsFromItem(ItemStack stack) {
        Potion potion = PotionUtils.getPotion(stack);


        for (MobEffectInstance effect : potion.getEffects()) {
            this.addEffect(new MobEffectInstance(
                    effect.getEffect(),
                    Math.max(effect.getDuration() / 8, 1),
                    effect.getAmplifier(),
                    effect.isAmbient(),
                    effect.isVisible()
            ));
        }

        Collection<MobEffectInstance> customEffects = PotionUtils.getCustomEffects(stack);
        if (!customEffects.isEmpty()) {
            for (MobEffectInstance effect : customEffects) {
                this.addEffect(new MobEffectInstance(effect));
            }
        }

    }
}