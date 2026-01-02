package net.darkblade.moregolems.sever.entity.custom;

import net.darkblade.moregolems.sever.init.ModEntities;
import net.darkblade.moregolems.sever.init.ModItems;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

public class DartEntity extends AbstractArrow implements GeoEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public DartEntity(EntityType<? extends AbstractArrow> entityType, Level level) {
        super(entityType, level);
    }

    public DartEntity(Level level, LivingEntity shooter) {
        super(ModEntities.DART_PROJECTILE.get(), shooter, level);
        this.setBaseDamage(1.5); // Daño base (el arco es ~2.0)
    }

    @Override
    protected ItemStack getPickupItem() {
        return new ItemStack(ModItems.DART.get());
    }

    // --- GECKOLIB BOILERPLATE ---

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // Aquí podrías añadir animaciones (ej. rotación al volar) si las tuvieras.
        // Por ahora lo dejamos vacío para que solo muestre el modelo estático.
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    // --- NETWORK ---
    // Necesario para que Forge spawnee la entidad correctamente en el cliente
    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}