package net.darkblade.moregolems.sever.entity.custom;

import net.darkblade.moregolems.sever.entity.ai.SimpleAabbMeleeGoal;
import net.darkblade.moregolems.sever.init.ModParticles;
import net.darkblade.moregolems.sever.init.ModSounds; // <--- IMPORTANTE
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent; // <--- IMPORTANTE
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource; // <--- IMPORTANTE
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.DefendVillageTargetGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.ResetUniversalAngerTargetGoal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.EnumSet;
import java.util.List;

public class GoldGolemEntity extends BaseGolemEntity implements GeoEntity {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private static final EntityDataAccessor<Boolean> DATA_ATTACKING =
            SynchedEntityData.defineId(GoldGolemEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> USING_SOLAR =
            SynchedEntityData.defineId(GoldGolemEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_SOLAR_FLASH =
            SynchedEntityData.defineId(GoldGolemEntity.class, EntityDataSerializers.INT);

    private static final double ATTACK_RANGE = 1.50;
    private static final double CHASE_SPEED  = 1.00;
    private static final int ATTACK_DURATION = 25;
    private static final int[] DAMAGE_FRAMES = {10};
    private static final int CD_BASE = 10;

    private static final SimpleAabbMeleeGoal.AttackHitbox HITBOX =
            SimpleAabbMeleeGoal.AttackHitbox.of(1.00, 1.50, 1.50, 0.00, 0.00);

    private int effectActiveTime = 0;

    public GoldGolemEntity(EntityType<? extends IronGolem> entityType, Level level) {
        super(entityType, level);
    }

    public static AttributeSupplier.Builder setAttributes() {
        return IronGolem.createAttributes()
                .add(Attributes.MAX_HEALTH, 100.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.ATTACK_DAMAGE, 24.0D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_ATTACKING, false);
        this.entityData.define(USING_SOLAR, false);
        this.entityData.define(DATA_SOLAR_FLASH, 0);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.getSolarFlashTicks() > 0) {
            this.setSolarFlashTicks(this.getSolarFlashTicks() - 1);
        }
    }

    public void setSolarFlashTicks(int ticks) {
        this.entityData.set(DATA_SOLAR_FLASH, ticks);
    }

    public int getSolarFlashTicks() {
        return this.entityData.get(DATA_SOLAR_FLASH);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new RandomEmoteGoal(this, 66, 30));
        this.goalSelector.addGoal(2, new SolarFlareGoal(this));
        this.goalSelector.addGoal(3, new SimpleAabbMeleeGoal<>(
                this, ATTACK_RANGE, CHASE_SPEED, true,
                ATTACK_DURATION, DAMAGE_FRAMES, CD_BASE, HITBOX,
                this::setAttacking
        ));
        this.goalSelector.addGoal(4, new MoveBackToVillageGoal(this, 0.6D, false));
        this.goalSelector.addGoal(5, new GolemRandomStrollInVillageGoal(this, 0.6D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.goalSelector.addGoal(8, new WaterAvoidingRandomStrollGoal(this, 0.6D));

        this.targetSelector.addGoal(1, new DefendVillageTargetGoal(this));
        this.targetSelector.addGoal(2, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false,
                this::isAngryAt));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Mob.class, 5, false, false,
                (entity) -> entity instanceof Enemy && !(entity instanceof Creeper)));
        this.targetSelector.addGoal(4, new ResetUniversalAngerTargetGoal<>(this, false));
    }

    @Override
    public void aiStep() {
        super.aiStep();

        if (!this.level().isClientSide) {
            if (this.effectActiveTime > 0) {
                this.effectActiveTime--;
                ServerLevel serverLevel = (ServerLevel) this.level();

                if (this.random.nextInt(3) == 0) {
                    double x = this.getX() + (this.random.nextDouble() - 0.5) * 1.5;
                    double y = this.getY() + 0.5 + this.random.nextDouble() * 2.0;
                    double z = this.getZ() + (this.random.nextDouble() - 0.5) * 1.5;

                    serverLevel.sendParticles(ParticleTypes.WAX_ON, x, y, z, 1, 0, 0, 0, 0);
                }

                List<LivingEntity> enemies = this.level().getEntitiesOfClass(LivingEntity.class,
                        this.getBoundingBox().inflate(20.0D),
                        e -> e instanceof Enemy && e.isAlive());

                for (LivingEntity enemy : enemies) {
                    if (enemy.hasEffect(MobEffects.MOVEMENT_SLOWDOWN)) {
                        serverLevel.sendParticles(ParticleTypes.WAX_ON,
                                enemy.getX(), enemy.getEyeY(), enemy.getZ(),
                                2, 0.2, 0.2, 0.2, 0.05);
                    }
                }
            }
        }
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack itemstack = player.getItemInHand(hand);

        if (itemstack.is(Items.GOLD_INGOT)) {
            float health = this.getHealth();
            this.heal(25.0F);
            if (this.getHealth() == health) {
                return InteractionResult.PASS;
            } else {
                float pitch = 1.0F + (this.random.nextFloat() - this.random.nextFloat()) * 0.2F;
                this.playSound(SoundEvents.IRON_GOLEM_REPAIR, 1.0F, pitch);
                if (!player.getAbilities().instabuild) {
                    itemstack.shrink(1);
                }
                return InteractionResult.sidedSuccess(this.level().isClientSide);
            }
        }
        return super.mobInteract(player, hand);
    }

    public void activateEffectTimer(int ticks) {
        this.effectActiveTime = ticks;
    }

    public void setAttacking(boolean attacking) {
        this.entityData.set(DATA_ATTACKING, attacking);
    }

    public boolean isAttacking() {
        return this.entityData.get(DATA_ATTACKING);
    }

    public void setUsingSolar(boolean using) {
        this.entityData.set(USING_SOLAR, using);
    }

    public boolean isUsingSolar() {
        return this.entityData.get(USING_SOLAR);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        super.registerControllers(controllers);

        controllers.add(new AnimationController<>(this, "movementController", 5, event -> {
            if (this.isEmoting() || this.isAttacking() || this.isUsingSolar()) {
                return PlayState.STOP;
            }

            if (event.getLimbSwingAmount() > 0.01F) {
                return event.setAndContinue(RawAnimation.begin().thenLoop("walk"));
            }
            return event.setAndContinue(RawAnimation.begin().thenLoop("idle"));
        }));

        controllers.add(new AnimationController<>(this, "attackController", 5, event -> {
            if (this.isAttacking()) {
                return event.setAndContinue(RawAnimation.begin().thenPlay("attack"));
            }
            event.getController().forceAnimationReset();
            return PlayState.STOP;
        }));

        controllers.add(new AnimationController<>(this, "solarController", 5, event -> {
            if (this.isUsingSolar()) {
                return event.setAndContinue(RawAnimation.begin().thenPlay("reflex_attack"));
            }
            event.getController().forceAnimationReset();
            return PlayState.STOP;
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    static class SolarFlareGoal extends Goal {
        private final GoldGolemEntity golem;

        private static final int COOLDOWN_TICKS = 600;
        private static final int ANIMATION_LENGTH = 40;
        private static final int EFFECT_TRIGGER_TICK = 20;
        private static final double RADIUS = 20.0D;
        private static final int SLOWNESS_DURATION = 100;

        private int cooldownTimer = 0;
        private int animationTick = 0;

        public SolarFlareGoal(GoldGolemEntity golem) {
            this.golem = golem;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (cooldownTimer > 0) {
                cooldownTimer--;
                return false;
            }
            if (!golem.level().isDay()) return false;

            List<LivingEntity> enemies = golem.level().getEntitiesOfClass(LivingEntity.class,
                    golem.getBoundingBox().inflate(RADIUS),
                    e -> e instanceof Enemy && e.isAlive());

            return !enemies.isEmpty();
        }

        @Override
        public void start() {
            this.animationTick = 0;
            golem.setUsingSolar(true);
            golem.getNavigation().stop();
            golem.playSound(SoundEvents.BEACON_ACTIVATE, 1.0F, 1.0F);
        }

        @Override
        public void stop() {
            golem.setUsingSolar(false);
            this.cooldownTimer = COOLDOWN_TICKS;
        }

        @Override
        public boolean canContinueToUse() {
            return animationTick < ANIMATION_LENGTH;
        }

        @Override
        public void tick() {
            animationTick++;
            golem.getNavigation().stop();

            if (animationTick == EFFECT_TRIGGER_TICK) {
                applyEffect();
            }
        }

        private void applyEffect() {
            if (golem.level().isClientSide) return;
            ServerLevel serverLevel = (ServerLevel) golem.level();

            golem.activateEffectTimer(SLOWNESS_DURATION);

            golem.playSound(ModSounds.GOLD_GOLEM_REFLECT.get(), 1.0F, 1.0F);

            golem.setSolarFlashTicks(20);

            serverLevel.sendParticles(ModParticles.SHINE.get(),
                    golem.getX(), golem.getY() + 1.5, golem.getZ(),
                    20, 1.0, 1.0, 1.0, 0.0);
            serverLevel.sendParticles(ParticleTypes.FLASH,
                    golem.getX(), golem.getY() + 1.5, golem.getZ(),
                    1, 0, 0, 0, 0);

            List<LivingEntity> enemies = golem.level().getEntitiesOfClass(LivingEntity.class,
                    golem.getBoundingBox().inflate(RADIUS),
                    e -> e instanceof Enemy && e.isAlive());

            for (LivingEntity enemy : enemies) {
                enemy.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, SLOWNESS_DURATION, 2));
            }
        }
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHitIn) {
        super.dropCustomDeathLoot(source, looting, recentlyHitIn);
        int goldCount = 3 + this.random.nextInt(3);
        this.spawnAtLocation(new ItemStack(Items.GOLD_INGOT, goldCount));
    }
}