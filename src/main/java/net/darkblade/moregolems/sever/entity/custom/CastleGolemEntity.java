package net.darkblade.moregolems.sever.entity.custom;

import net.darkblade.moregolems.sever.entity.ai.SimpleAabbMeleeGoal;
import net.darkblade.moregolems.sever.init.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class CastleGolemEntity extends BaseGolemEntity implements GeoEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private static final EntityDataAccessor<Boolean> DATA_ATTACKING =
            SynchedEntityData.defineId(CastleGolemEntity.class, EntityDataSerializers.BOOLEAN);

    private static final EntityDataAccessor<Integer> CASTLE_STATE =
            SynchedEntityData.defineId(CastleGolemEntity.class, EntityDataSerializers.INT);

    private static final EntityDataAccessor<Optional<UUID>> OWNER_UUID =
            SynchedEntityData.defineId(CastleGolemEntity.class, EntityDataSerializers.OPTIONAL_UUID);

    private static final double ATTACK_RANGE = 2.5D;
    private static final double CHASE_SPEED  = 1.0D;
    private static final int ATTACK_DURATION = 44;
    private static final int[] DAMAGE_FRAMES = {16};
    private static final int CD_BASE = 20;

    private static final int TICKS_ANIM_SET = 83;
    private static final int TICKS_ANIM_OFF = 109;

    private static final double EFFECT_RADIUS = 50.0D;
    private static final int EFFECT_INTERVAL = 40;
    private static final int EFFECT_DURATION = 100;
    private static final int EFFECT_AMPLIFIER = 2;

    private int castleTimer = 0;

    private static final SimpleAabbMeleeGoal.AttackHitbox HITBOX =
            SimpleAabbMeleeGoal.AttackHitbox.of(3.5D, 3.0D, 1.5D, 0.0D, 0.0D);

    public CastleGolemEntity(EntityType<? extends IronGolem> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason, @Nullable SpawnGroupData spawnData, @Nullable CompoundTag dataTag) {
        this.setCastleState(2);
        return super.finalizeSpawn(level, difficulty, reason, spawnData, dataTag);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_ATTACKING, false);
        this.entityData.define(CASTLE_STATE, 0);
        this.entityData.define(OWNER_UUID, Optional.empty());
    }

    public static AttributeSupplier.Builder setAttributes() {
        return IronGolem.createAttributes()
                .add(Attributes.MAX_HEALTH, 120.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.20D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.ATTACK_DAMAGE, 14.0D);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.CASTLE_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.CASTLE_DEATH.get();
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState blockIn) {
        this.playSound(ModSounds.CASTLE_WALK.get(), 1.0F, 1.0F);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new CastleModeGoal(this));
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new SimpleAabbMeleeGoal<>(
                this, ATTACK_RANGE, CHASE_SPEED, true,
                ATTACK_DURATION, DAMAGE_FRAMES, CD_BASE, HITBOX,
                this::setAttacking
        ).setOnDamageAction(this::spawnSmashParticles));

        this.goalSelector.addGoal(3, new MoveBackToVillageGoal(this, 0.6D, false));
        this.goalSelector.addGoal(4, new GolemRandomStrollInVillageGoal(this, 0.6D));
        this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 1.0D));

        this.targetSelector.addGoal(1, new DefendVillageTargetGoal(this));
        this.targetSelector.addGoal(2, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, this::isAngryAt));
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, Mob.class, 5, false, false, (entity) -> entity instanceof Enemy && !(entity instanceof Creeper)));
        this.targetSelector.addGoal(5, new ResetUniversalAngerTargetGoal<>(this, false));
    }

    public void setOwnerId(UUID uuid) {
        this.entityData.set(OWNER_UUID, Optional.ofNullable(uuid));
    }

    public UUID getOwnerId() {
        return this.entityData.get(OWNER_UUID).orElse(null);
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand == InteractionHand.MAIN_HAND) {
            if (this.level().isClientSide) {
                UUID owner = getOwnerId();
                boolean isOwner = (owner != null && owner.equals(player.getUUID()));
                return (isOwner || player.isCreative()) ? InteractionResult.SUCCESS : InteractionResult.PASS;
            }

            UUID owner = getOwnerId();
            boolean isOwner = (owner != null && owner.equals(player.getUUID()));

            if (isOwner || player.isCreative()) {
                int currentState = this.getCastleState();

                if (currentState == 0) {
                    this.setCastleState(1);
                    this.castleTimer = TICKS_ANIM_SET;
                    this.playSound(SoundEvents.IRON_DOOR_CLOSE, 1.0f, 0.5f);
                    return InteractionResult.SUCCESS;
                }
                else if (currentState == 2) {
                    this.setCastleState(3);
                    this.castleTimer = TICKS_ANIM_OFF;
                    this.playSound(SoundEvents.IRON_DOOR_OPEN, 1.0f, 0.5f);
                    return InteractionResult.SUCCESS;
                }
            } else {
                player.displayClientMessage(Component.literal("¡Not your Golem!"), true);
                return InteractionResult.CONSUME;
            }
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public void aiStep() {
        super.aiStep();

        if (!this.level().isClientSide) {
            int state = getCastleState();

            if (state == 1) {
                this.castleTimer--;
                if (this.castleTimer <= 0) this.setCastleState(2);
            } else if (state == 3) {
                this.castleTimer--;
                if (this.castleTimer <= 0) this.setCastleState(0);
            }

            if (state > 0) {
                this.setDeltaMovement(Vec3.ZERO);
                this.getNavigation().stop();
            }

            if (state == 2) {
                if (this.tickCount % EFFECT_INTERVAL == 0) {
                    applyCastleEffect();
                }

                if (this.tickCount % 20 == 0) {
                    this.playSound(ModSounds.CASTLE_HEARTBEAT.get(), 1.0F, 1.0F);
                }
            }
        }
    }

    private void applyCastleEffect() {
        AABB area = this.getBoundingBox().inflate(EFFECT_RADIUS);
        List<Player> players = this.level().getEntitiesOfClass(Player.class, area);

        for (Player p : players) {
            if (p.isCreative() || p.isSpectator()) continue;
            p.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, EFFECT_DURATION, EFFECT_AMPLIFIER, true, true));
        }
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public void doPush(net.minecraft.world.entity.Entity entity) {

        if (!this.isPassengerOfSameVehicle(entity) && !entity.noPhysics && !this.noPhysics) {
            double dx = entity.getX() - this.getX();
            double dz = entity.getZ() - this.getZ();
            double distAbs = Math.max(Math.abs(dx), Math.abs(dz));

            if (distAbs >= 0.01F) {
                distAbs = Math.sqrt(distAbs);
                dx /= distAbs;
                dz /= distAbs;

                double multiplier = 1.0D / distAbs;
                if (multiplier > 1.0D) multiplier = 1.0D;

                dx *= multiplier;
                dz *= multiplier;
                dx *= 0.05F;
                dz *= 0.05F;

                if (!entity.isVehicle()) {
                    entity.push(dx, 0.0D, dz);
                }
            }
        }
    }

    @Override
    public void knockback(double strength, double x, double z) {
        if (this.getCastleState() == 0) super.knockback(strength, x, z);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("CastleState", this.getCastleState());
        tag.putInt("CastleTimer", this.castleTimer);
        if (getOwnerId() != null) tag.putUUID("Owner", getOwnerId());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.setCastleState(tag.getInt("CastleState"));
        this.castleTimer = tag.getInt("CastleTimer");
        if (tag.hasUUID("Owner")) this.setOwnerId(tag.getUUID("Owner"));
    }

    public void setAttacking(boolean attacking) {
        this.entityData.set(DATA_ATTACKING, attacking);
    }

    public boolean isAttacking() {
        return this.entityData.get(DATA_ATTACKING);
    }

    public void setCastleState(int state) {
        this.entityData.set(CASTLE_STATE, state);
    }

    public int getCastleState() {
        return this.entityData.get(CASTLE_STATE);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movementController", 5, event -> {
            int state = this.getCastleState();
            if (state > 0 || this.isAttacking()) return PlayState.STOP;
            if (event.getLimbSwingAmount() > 0.01F) return event.setAndContinue(RawAnimation.begin().thenLoop("walk"));
            return event.setAndContinue(RawAnimation.begin().thenLoop("idle"));
        }));

        controllers.add(new AnimationController<>(this, "attackController", 2, event -> {
            if (this.isAttacking() && this.getCastleState() == 0) {
                return event.setAndContinue(RawAnimation.begin().thenPlay("attack"));
            }
            event.getController().forceAnimationReset();
            return PlayState.STOP;
        }));

        controllers.add(new AnimationController<>(this, "castleController", 0, event -> {
            int state = this.getCastleState();
            if (state == 1) return event.setAndContinue(RawAnimation.begin().thenPlay("castle_set"));
            else if (state == 2) return event.setAndContinue(RawAnimation.begin().thenLoop("castle_idle"));
            else if (state == 3) return event.setAndContinue(RawAnimation.begin().thenPlay("castle_off"));

            event.getController().forceAnimationReset();
            return PlayState.STOP;
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    private void spawnSmashParticles() {
        if (this.level() instanceof ServerLevel serverLevel) {
            double x = this.getX();
            double y = this.getY();
            double z = this.getZ();

            double lookX = this.getLookAngle().x;
            double lookZ = this.getLookAngle().z;
            double impactX = x + (lookX * 2.5);
            double impactZ = z + (lookZ * 2.5);

            for (int i = 0; i < 360; i += 20) {
                double angle = Math.toRadians(i);
                double offsetX = Math.cos(angle) * 1.5;
                double offsetZ = Math.sin(angle) * 1.5;

                serverLevel.sendParticles(
                        new BlockParticleOption(ParticleTypes.BLOCK, Blocks.DIRT.defaultBlockState()),
                        impactX + offsetX, y + 0.5, impactZ + offsetZ,
                        3, 0.2, 0.2, 0.2, 0.15
                );
            }

            serverLevel.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, impactX, y + 0.5, impactZ, 15, 0.5, 0.2, 0.5, 0.05);
            serverLevel.sendParticles(ParticleTypes.EXPLOSION, impactX, y + 0.5, impactZ, 1, 0, 0, 0, 0);

            this.playSound(ModSounds.CASTLE_SMASH.get(), 1.0f, 0.8f);
        }
    }

    static class CastleModeGoal extends Goal {
        private final CastleGolemEntity golem;
        public CastleModeGoal(CastleGolemEntity golem) {
            this.golem = golem;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return golem.getCastleState() > 0;
        }

        @Override
        public void start() {
            golem.getNavigation().stop();
            golem.setDeltaMovement(Vec3.ZERO);
        }

        @Override
        public void tick() {
            golem.getNavigation().stop();
        }
    }
}