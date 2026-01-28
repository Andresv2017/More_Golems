package net.darkblade.moregolems.sever.item.custom;

import net.darkblade.moregolems.client.renderer.BlowgunItemRenderer;
import net.darkblade.moregolems.sever.entity.custom.DartEntity;
import net.darkblade.moregolems.sever.init.ModItems;
import net.darkblade.moregolems.sever.init.ModSounds;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ArrowItem; // Necesario
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import java.util.function.Consumer;
import java.util.function.Predicate;

public class BlowgunItem extends ProjectileWeaponItem {

    public static final int MAX_DRAW_DURATION = 10;

    public BlowgunItem(Properties properties) {
        super(properties);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private BlockEntityWithoutLevelRenderer bewlr;
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (bewlr == null) {
                    var mc = net.minecraft.client.Minecraft.getInstance();
                    bewlr = new BlowgunItemRenderer(mc.getBlockEntityRenderDispatcher(), mc.getEntityModels());
                }
                return bewlr;
            }
        });
    }

    @Override
    public void onUseTick(Level level, LivingEntity livingEntity, ItemStack stack, int remainingUseDuration) {
        if (livingEntity instanceof Player player && !level.isClientSide) {
            int duration = this.getUseDuration(stack) - remainingUseDuration;

            if (duration == MAX_DRAW_DURATION) {
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.PLAYERS, 0.5F, 1.0F);
            }
        }
    }

    @Override
    public Predicate<ItemStack> getAllSupportedProjectiles() {
        return (stack) -> stack.is(ModItems.DART.get());
    }

    @Override
    public int getDefaultProjectileRange() {
        return 15;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 72000;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE;
    }

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

                float f = getPowerForTime(i);
                if (!((double)f < 0.1D)) {
                    if (!level.isClientSide) {

                        DartItem dartItem = (DartItem) (ammoStack.getItem() instanceof DartItem ? ammoStack.getItem() : ModItems.DART.get());
                        AbstractArrow abstractArrow = dartItem.createArrow(level, ammoStack, player);

                        if (abstractArrow instanceof DartEntity dart) {
                            dart.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, f * 3.0F, 1.0F);

                            if (f == 1.0F) {
                                dart.setCritArrow(true);
                            }

                            stack.hurtAndBreak(1, player, (p) -> p.broadcastBreakEvent(player.getUsedItemHand()));
                            level.addFreshEntity(dart);
                        }
                    }

                    level.playSound(null, player.getX(), player.getY(), player.getZ(),
                            ModSounds.BLOWGUN_SHOOT.get(), SoundSource.PLAYERS, 1.0F, 1.0F / (level.getRandom().nextFloat() * 0.4F + 1.2F) + f * 0.5F);

                    if (!isCreative) {
                        ammoStack.shrink(1);
                        if (ammoStack.isEmpty()) player.getInventory().removeItem(ammoStack);
                    }
                    player.awardStat(Stats.ITEM_USED.get(this));
                }
            }
        }
    }

    public static float getPowerForTime(int charge) {
        float f = (float)charge / (float)MAX_DRAW_DURATION;
        f = (f * f + f * 2.0F) / 3.0F;
        if (f > 1.0F) f = 1.0F;
        return f;
    }
}