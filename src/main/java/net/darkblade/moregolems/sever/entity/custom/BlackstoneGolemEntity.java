package net.darkblade.moregolems.sever.entity.custom;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class BlackstoneGolemEntity extends Monster implements GeoEntity {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private static final EntityDataAccessor<Boolean> DATA_ATTACKING =
            SynchedEntityData.defineId(BlackstoneGolemEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Optional<UUID>> OWNER_UUID =
            SynchedEntityData.defineId(BlackstoneGolemEntity.class, EntityDataSerializers.OPTIONAL_UUID);

    private static final double ATTACK_HITBOX_INFLATE = 1.25D;

    private static final float MAX_SPIN_SPEED = 24.0F;
    private static final float SPIN_ACCEL = 2.0F;
    private static final float SPIN_BRAKE = 1.5F;
    private static final float MIN_BRAKE_SPEED = 4.0F;

    private float spinAngle;
    private float prevSpinAngle;
    private float spinSpeed;
    private float prevSpinSpeed;

    public BlackstoneGolemEntity(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    public void tick() {
        super.tick();
        this.prevSpinAngle = this.spinAngle;
        this.prevSpinSpeed = this.spinSpeed;

        if (this.isAttacking()) {
            this.spinSpeed = Math.min(MAX_SPIN_SPEED, this.spinSpeed + SPIN_ACCEL);
            this.spinAngle += this.spinSpeed;
        } else if (this.spinSpeed > 0.0F) {
            this.spinSpeed = Math.max(MIN_BRAKE_SPEED, this.spinSpeed - SPIN_BRAKE);
            float remaining = (360.0F - this.spinAngle % 360.0F) % 360.0F;

            if (remaining <= this.spinSpeed) {
                this.spinAngle += remaining;
                this.spinSpeed = 0.0F;
            } else {
                this.spinAngle += this.spinSpeed;
            }
        } else if (this.spinAngle != 0.0F) {
            this.spinAngle = 0.0F;
            this.prevSpinAngle = 0.0F;
        }
    }

    public float getSpinAngle(float partialTick) {
        return Mth.lerp(partialTick, this.prevSpinAngle, this.spinAngle);
    }

    public float getSpinLift(float partialTick) {
        return Mth.clamp(Mth.lerp(partialTick, this.prevSpinSpeed, this.spinSpeed) / MAX_SPIN_SPEED, 0.0F, 1.0F);
    }

    public static AttributeSupplier.Builder setAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 120.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.26D)
                .add(Attributes.ATTACK_DAMAGE, 14.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6D)
                .add(Attributes.ARMOR, 8.0D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_ATTACKING, false);
        this.entityData.define(OWNER_UUID, Optional.empty());
    }

    public void setOwnerId(UUID uuid) {
        this.entityData.set(OWNER_UUID, Optional.ofNullable(uuid));
    }

    public UUID getOwnerId() {
        return this.entityData.get(OWNER_UUID).orElse(null);
    }

    public void setAttacking(boolean attacking) {
        this.entityData.set(DATA_ATTACKING, attacking);
    }

    public boolean isAttacking() {
        return this.entityData.get(DATA_ATTACKING);
    }

    private boolean isValidAttackTarget(LivingEntity entity) {
        UUID owner = getOwnerId();
        return owner == null || !owner.equals(entity.getUUID());
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new SpinAttackGoal(this));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 0.8D));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, this::isValidAttackTarget));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Mob.class, 10, false, false,
                (entity) -> entity instanceof Enemy && !(entity instanceof Creeper)));
    }

    private void performSpinDamage() {
        LivingEntity target = this.getTarget();
        if (target == null) return;

        AABB attackBox = this.getBoundingBox().inflate(ATTACK_HITBOX_INFLATE);

        if (target instanceof Player) {
            List<Player> victims = this.level().getEntitiesOfClass(Player.class, attackBox,
                    p -> p.isAlive() && !p.isSpectator() && this.isValidAttackTarget(p));
            for (Player victim : victims) {
                strike(victim);
            }
        } else if (target.isAlive() && attackBox.intersects(target.getBoundingBox())) {
            strike(target);
        }
    }

    private void strike(LivingEntity victim) {
        if (victim.hurt(this.damageSources().mobAttack(this), (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE))) {
            double dx = victim.getX() - this.getX();
            double dz = victim.getZ() - this.getZ();
            victim.knockback(0.6D, -dx, -dz);
            this.playSound(SoundEvents.IRON_GOLEM_ATTACK, 1.0F, 0.9F);

            if (this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.CRIT,
                        victim.getX(), victim.getY() + victim.getBbHeight() * 0.5D, victim.getZ(),
                        6, 0.3D, 0.3D, 0.3D, 0.05D);
            }
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (getOwnerId() != null) tag.putUUID("Owner", getOwnerId());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("Owner")) this.setOwnerId(tag.getUUID("Owner"));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movementController", 5, event -> {
            if (event.getLimbSwingAmount() > 0.01F) {
                return event.setAndContinue(RawAnimation.begin().thenLoop("walk"));
            }
            return event.setAndContinue(RawAnimation.begin().thenLoop("idle"));
        }));

        controllers.add(new AnimationController<>(this, "maceController", 5, event -> {
            if (event.getLimbSwingAmount() > 0.01F) {
                return event.setAndContinue(RawAnimation.begin().thenLoop("mases_walk"));
            }
            return event.setAndContinue(RawAnimation.begin().thenLoop("mases_idle"));
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    static class SpinAttackGoal extends Goal {
        private final BlackstoneGolemEntity golem;

        SpinAttackGoal(BlackstoneGolemEntity golem) {
            this.golem = golem;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = golem.getTarget();
            return target != null && target.isAlive();
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public void start() {
            golem.setAttacking(true);
        }

        @Override
        public void stop() {
            golem.setAttacking(false);
            golem.getNavigation().stop();
        }

        @Override
        public void tick() {
            LivingEntity target = golem.getTarget();
            if (target == null) return;

            golem.getLookControl().setLookAt(target, 30.0F, 30.0F);

            double distSqr = golem.distanceToSqr(target);
            if (distSqr <= 4.0D) {
                golem.getNavigation().stop();
            } else if (target.onGround() || golem.getNavigation().isDone()) {
                golem.getNavigation().moveTo(target, 1.15D);
            }

            golem.performSpinDamage();
        }
    }
}
