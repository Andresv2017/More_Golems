package net.darkblade.moregolems.sever.entity.custom;

import net.darkblade.moregolems.sever.entity.ai.SimpleAabbMeleeGoal;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.*;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

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
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new CastleModeGoal(this));
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new SimpleAabbMeleeGoal<>(
                this, ATTACK_RANGE, CHASE_SPEED, true,
                ATTACK_DURATION, DAMAGE_FRAMES, CD_BASE, HITBOX,
                this::setAttacking
        ));
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
        if (hand == InteractionHand.MAIN_HAND && !this.level().isClientSide) {
            UUID owner = getOwnerId();
            if (owner != null && owner.equals(player.getUUID()) || player.isCreative()) {
                int currentState = this.getCastleState();
                if (currentState == 0) {
                    this.setCastleState(1);
                    this.castleTimer = TICKS_ANIM_SET;
                    this.playSound(net.minecraft.sounds.SoundEvents.IRON_DOOR_CLOSE, 1.0f, 0.5f);
                    return InteractionResult.SUCCESS;
                }
                else if (currentState == 2) {
                    this.setCastleState(3);
                    this.castleTimer = TICKS_ANIM_OFF;
                    this.playSound(net.minecraft.sounds.SoundEvents.IRON_DOOR_OPEN, 1.0f, 0.5f);
                    return InteractionResult.SUCCESS;
                }
            } else {
                if (!this.level().isClientSide) {
                    player.displayClientMessage(Component.literal("Not your Golem!"), true);
                }
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
            }
        }
    }

    private void applyCastleEffect() {
        AABB area = this.getBoundingBox().inflate(EFFECT_RADIUS);

        List<Player> players = this.level().getEntitiesOfClass(Player.class, area);

        UUID ownerId = getOwnerId();

        for (Player p : players) {
            if (p.isCreative() || p.isSpectator()) continue;

            //if (ownerId != null && ownerId.equals(p.getUUID())) continue;

            p.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, EFFECT_DURATION, EFFECT_AMPLIFIER, true, true));
        }
    }

    @Override
    public boolean isPushable() {
        return this.getCastleState() == 0 && super.isPushable();
    }

    @Override
    public void doPush(net.minecraft.world.entity.Entity entity) {
        if (this.getCastleState() == 0) super.doPush(entity);
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