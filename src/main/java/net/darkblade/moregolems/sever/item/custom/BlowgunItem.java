package net.darkblade.moregolems.sever.item.custom;

import net.darkblade.moregolems.sever.entity.custom.DartEntity;
import net.darkblade.moregolems.sever.init.ModItems;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;

import java.util.function.Predicate;

public class BlowgunItem extends ProjectileWeaponItem {

    public BlowgunItem(Properties properties) {
        super(properties);
    }

    // Define qué munición acepta este arma
    @Override
    public Predicate<ItemStack> getAllSupportedProjectiles() {
        return (stack) -> stack.is(ModItems.DART.get());
    }

    // Rango base (velocidad) usado por encantamientos, aunque nosotros calculamos nuestra propia velocidad abajo
    @Override
    public int getDefaultProjectileRange() {
        return 15;
    }

    // Tiempo máximo de uso (como el arco)
    @Override
    public int getUseDuration(ItemStack stack) {
        return 72000;
    }

    // Animación al usarlo (BOW hace que la cámara se acerque ligeramente y el personaje levante el brazo)
    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    // Acción al hacer clic derecho: empieza a cargar si tienes munición
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack itemstack = player.getItemInHand(hand);
        boolean hasAmmo = !player.getProjectile(itemstack).isEmpty();

        if (!player.getAbilities().instabuild && !hasAmmo) {
            return InteractionResultHolder.fail(itemstack);
        } else {
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(itemstack);
        }
    }

    // Acción al soltar el clic derecho: dispara
    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entityLiving, int timeLeft) {
        if (entityLiving instanceof Player player) {
            boolean isCreative = player.getAbilities().instabuild;
            ItemStack ammoStack = player.getProjectile(stack);

            int i = this.getUseDuration(stack) - timeLeft;
            if (i < 0) return;

            if (!ammoStack.isEmpty() || isCreative) {
                if (ammoStack.isEmpty()) {
                    ammoStack = new ItemStack(ModItems.DART.get());
                }

                // Calcular fuerza del disparo (0.0 a 1.0)
                float f = getPowerForTime(i);
                if (!((double)f < 0.1D)) {
                    if (!level.isClientSide) {
                        // Crear la entidad del dardo
                        DartEntity dart = new DartEntity(level, player);

                        // Configurar velocidad y precisión
                        // shootFromRotation(entity, xRot, yRot, zRot, velocity, inaccuracy)
                        dart.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, f * 3.0F, 1.0F);

                        // Lógica de arcos: si la carga es máxima, es crítico
                        if (f == 1.0F) {
                            dart.setCritArrow(true);
                        }

                        // Consumir durabilidad del arma
                        stack.hurtAndBreak(1, player, (p) -> p.broadcastBreakEvent(player.getUsedItemHand()));

                        // Añadir la entidad al mundo
                        level.addFreshEntity(dart);
                    }

                    // Sonido de disparo (puedes cambiarlo por uno custom si tienes)
                    level.playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 1.0F, 1.0F / (level.getRandom().nextFloat() * 0.4F + 1.2F) + f * 0.5F);

                    // Consumir munición
                    if (!isCreative) {
                        ammoStack.shrink(1);
                        if (ammoStack.isEmpty()) {
                            player.getInventory().removeItem(ammoStack);
                        }
                    }

                    player.awardStat(Stats.ITEM_USED.get(this));
                }
            }
        }
    }

    // Calcula qué tan cargado está el disparo según el tiempo mantenido
    public static float getPowerForTime(int charge) {
        float f = (float)charge / 20.0F;
        f = (f * f + f * 2.0F) / 3.0F;
        if (f > 1.0F) {
            f = 1.0F;
        }
        return f;
    }
}