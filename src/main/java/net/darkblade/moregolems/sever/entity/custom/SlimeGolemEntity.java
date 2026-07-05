package net.darkblade.moregolems.sever.entity.custom;

import net.darkblade.moregolems.sever.init.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class SlimeGolemEntity extends BaseGolemEntity implements GeoEntity {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public static final int SMALL = 1;
    public static final int MEDIUM = 2;
    public static final int BIG = 3;

    private static final EntityDataAccessor<Integer> DATA_SIZE =
            SynchedEntityData.defineId(SlimeGolemEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> IS_LANDING =
            SynchedEntityData.defineId(SlimeGolemEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> IS_ROTATING =
            SynchedEntityData.defineId(SlimeGolemEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> IS_INTERACTING =
            SynchedEntityData.defineId(SlimeGolemEntity.class, EntityDataSerializers.BOOLEAN);

    private static final int LAND_ANIM_TICKS = 13;
    private static final int ROTATE_ANIM_TICKS = 20;
    private static final int INTERACT_ANIM_TICKS = 8;

    private int hopCooldown = 10;
    private boolean wasOnGround = true;
    private int landingTimer = 0;
    private int rotateTimer = 0;
    private int interactTimer = 0;
    private float hopFacingYaw;
    private boolean hopHitDelivered = true;

    public SlimeGolemEntity(EntityType<? extends IronGolem> entityType, Level level) {
        super(entityType, level);
    }

    public static AttributeSupplier.Builder setAttributes() {
        return IronGolem.createAttributes()
                .add(Attributes.MAX_HEALTH, 100.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.23D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.5D)
                .add(Attributes.ATTACK_DAMAGE, 10.0D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_SIZE, BIG);
        this.entityData.define(IS_LANDING, false);
        this.entityData.define(IS_ROTATING, false);
        this.entityData.define(IS_INTERACTING, false);
    }

    @Override
    public SpawnGroupData finalizeSpawn(net.minecraft.world.level.ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason, @Nullable SpawnGroupData spawnData, @Nullable CompoundTag dataTag) {
        this.wasOnGround = true;
        return super.finalizeSpawn(level, difficulty, reason, spawnData, dataTag);
    }

    public void setSlimeSize(int size, boolean resetHealth) {
        this.entityData.set(DATA_SIZE, size);
        this.reapplyPosition();
        this.refreshDimensions();

        double health;
        double damage;
        double speed;

        switch (size) {
            case SMALL:
                health = 20.0D;
                damage = 3.0D;
                speed = 0.32D;
                break;
            case MEDIUM:
                health = 45.0D;
                damage = 6.0D;
                speed = 0.28D;
                break;
            default:
                health = 100.0D;
                damage = 10.0D;
                speed = 0.23D;
                break;
        }

        AttributeInstance maxHealthAttr = this.getAttribute(Attributes.MAX_HEALTH);
        AttributeInstance damageAttr = this.getAttribute(Attributes.ATTACK_DAMAGE);
        AttributeInstance speedAttr = this.getAttribute(Attributes.MOVEMENT_SPEED);

        if (maxHealthAttr != null) maxHealthAttr.setBaseValue(health);
        if (damageAttr != null) damageAttr.setBaseValue(damage);
        if (speedAttr != null) speedAttr.setBaseValue(speed);

        if (resetHealth) this.setHealth((float) health);

        this.xpReward = size;
    }

    public int getSlimeSize() {
        return this.entityData.get(DATA_SIZE);
    }

    public float getSizeScale() {
        return switch (this.getSlimeSize()) {
            case SMALL -> 0.4F;
            case MEDIUM -> 0.65F;
            default -> 1.0F;
        };
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return super.getDimensions(pose).scale(this.getSizeScale());
    }

    public boolean isLanding() {
        return this.entityData.get(IS_LANDING);
    }

    public boolean isRotating() {
        return this.entityData.get(IS_ROTATING);
    }

    public boolean isInteracting() {
        return this.entityData.get(IS_INTERACTING);
    }

    private void triggerInteractAnim() {
        this.entityData.set(IS_INTERACTING, true);
        this.interactTimer = INTERACT_ANIM_TICKS;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 6.0F));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, this::isAngryAt));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Mob.class, 5, false, false,
                (entity) -> entity instanceof Enemy && !(entity instanceof Creeper)));
        this.targetSelector.addGoal(4, new ResetUniversalAngerTargetGoal<>(this, false));
    }

    @Override
    public void aiStep() {
        boolean onGroundBefore = this.onGround();

        if (!this.level().isClientSide) {
            tickTimers();
            if (onGroundBefore) {
                maybeStartHop();
            }
        }

        super.aiStep();

        if (!this.level().isClientSide) {
            if (onGroundBefore && this.getDeltaMovement().y > 0.1D) {
                launchRidingPlayers();
            }

            handleTrampolineBounce();
            checkHopContactDamage();

            boolean groundedNow = this.onGround();
            if (!this.wasOnGround && groundedNow) {
                onLanded();
            }
            this.wasOnGround = groundedNow;
        }
    }

    private void checkHopContactDamage() {
        if (this.hopHitDelivered) return;

        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive() || !(target instanceof Enemy || target instanceof Player)) {
            return;
        }

        if (this.getBoundingBox().inflate(0.2D).intersects(target.getBoundingBox())) {
            this.hopHitDelivered = true;
            if (target.hurt(this.damageSources().mobAttack(this), (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE))) {
                double dx = target.getX() - this.getX();
                double dz = target.getZ() - this.getZ();
                target.knockback(0.4D, -dx, -dz);
            }
        }
    }

    private void handleTrampolineBounce() {
        // Mobs don't provide solid vertical collision like blocks do, so a falling player
        // would otherwise clip straight through instead of ever resting on a thin surface slab.
        // Scan a generous column above the golem and bounce anyone whose feet are near/at the top.
        AABB box = this.getBoundingBox();
        AABB scanArea = new AABB(box.minX, box.minY, box.minZ, box.maxX, box.maxY + 1.5D, box.maxZ);

        for (Player player : this.level().getEntitiesOfClass(Player.class, scanArea)) {
            if (player.isShiftKeyDown()) continue;

            Vec3 motion = player.getDeltaMovement();
            if (motion.y < 0.0D && player.getBoundingBox().minY <= box.maxY + 0.25D) {
                player.setDeltaMovement(motion.x, Math.max(0.9D, -motion.y * 1.4D), motion.z);
                player.hasImpulse = true;
                player.fallDistance = 0.0F;
                syncPlayerMotion(player);
            }
        }
    }

    private void tickTimers() {
        if (this.landingTimer > 0 && --this.landingTimer == 0) this.entityData.set(IS_LANDING, false);
        if (this.rotateTimer > 0 && --this.rotateTimer == 0) this.entityData.set(IS_ROTATING, false);
        if (this.interactTimer > 0 && --this.interactTimer == 0) this.entityData.set(IS_INTERACTING, false);
    }

    private void maybeStartHop() {
        this.hopCooldown--;
        if (this.hopCooldown <= 0) {
            LivingEntity target = this.getTarget();
            boolean chasing = target != null && target.isAlive();

            double distToTarget = 0.0D;
            if (chasing) {
                double dx = target.getX() - this.getX();
                double dz = target.getZ() - this.getZ();
                distToTarget = Math.sqrt(dx * dx + dz * dz);
                this.hopFacingYaw = (float) (Mth.atan2(dz, dx) * (180D / Math.PI)) - 90.0F;
            } else if (this.random.nextInt(3) == 0) {
                this.hopFacingYaw = this.random.nextFloat() * 360.0F;
            }

            this.setYRot(this.hopFacingYaw);
            this.yBodyRot = this.hopFacingYaw;
            this.yHeadRot = this.hopFacingYaw;

            float yawRad = this.hopFacingYaw * Mth.DEG_TO_RAD;
            double dirX = -Mth.sin(yawRad);
            double dirZ = Mth.cos(yawRad);
            double hopSpeed = (chasing ? 0.32D : 0.22D) * (0.85D + 0.15D * this.getSizeScale());

            if (chasing) {
                // Don't overshoot past a target that's already in melee range, or the landing hit will miss.
                hopSpeed = Math.min(hopSpeed, Math.max(0.08D, distToTarget * 0.7D));
            }

            Vec3 current = this.getDeltaMovement();
            this.setDeltaMovement(dirX * hopSpeed, current.y, dirZ * hopSpeed);
            this.jumpFromGround();
            this.hopHitDelivered = false;

            // Chasing cooldown is kept above the ~1s hurt-invulnerability window so consecutive
            // landings don't get silently absorbed by the target's residual damage immunity.
            this.hopCooldown = chasing ? 22 + this.random.nextInt(10) : 25 + this.random.nextInt(30);
            return;
        }

        if (this.getTarget() == null && !this.isRotating() && !this.isLanding() && this.random.nextInt(300) == 0) {
            this.entityData.set(IS_ROTATING, true);
            this.rotateTimer = ROTATE_ANIM_TICKS;
        }
    }

    private void onLanded() {
        this.entityData.set(IS_LANDING, true);
        this.landingTimer = LAND_ANIM_TICKS;

        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.ITEM_SLIME,
                    this.getX(), this.getY(), this.getZ(),
                    3, this.getBbWidth() * 0.3D, 0.05D, this.getBbWidth() * 0.3D, 0.0D);
        }

        this.playSound(this.getSlimeSize() == SMALL ? SoundEvents.SLIME_SQUISH_SMALL : SoundEvents.SLIME_SQUISH,
                1.0F, (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F);
    }

    private void launchRidingPlayers() {
        AABB box = this.getBoundingBox();
        AABB above = new AABB(box.minX, box.maxY - 0.2D, box.minZ, box.maxX, box.maxY + 0.6D, box.maxZ);
        double jumpY = this.getDeltaMovement().y;

        for (Player player : this.level().getEntitiesOfClass(Player.class, above)) {
            if (!player.onGround()) continue;
            player.setDeltaMovement(player.getDeltaMovement().x, jumpY, player.getDeltaMovement().z);
            player.hasImpulse = true;
            player.fallDistance = 0.0F;
            syncPlayerMotion(player);
        }
    }

    private static void syncPlayerMotion(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.connection.send(new ClientboundSetEntityMotionPacket(serverPlayer));
        }
    }

    @Override
    public void die(DamageSource damageSource) {
        int size = this.getSlimeSize();
        Level level = this.level();
        double x = this.getX();
        double y = this.getY();
        double z = this.getZ();

        super.die(damageSource);

        if (!level.isClientSide && size > SMALL && level instanceof ServerLevel serverLevel) {
            int childSize = size - 1;
            int count = size == BIG ? 3 : 4;

            for (int i = 0; i < count; i++) {
                SlimeGolemEntity child = ModEntities.SLIME_GOLEM.get().create(serverLevel);
                if (child == null) continue;

                child.setSlimeSize(childSize, true);

                double offsetX = ((i % 2) - 0.5D) * 0.5D;
                double offsetZ = ((i / 2) - 0.5D) * 0.5D;

                child.moveTo(x + offsetX, y + 0.5D, z + offsetZ, this.random.nextFloat() * 360.0F, 0.0F);
                child.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(child.blockPosition()), MobSpawnType.MOB_SUMMONED, null, null);
                serverLevel.addFreshEntity(child);
            }
        }
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack itemstack = player.getItemInHand(hand);

        if (itemstack.is(Items.SLIME_BALL)) {
            float health = this.getHealth();
            this.heal(this.getMaxHealth() * 0.2F);
            if (this.getHealth() == health) {
                return InteractionResult.PASS;
            } else {
                float pitch = 1.0F + (this.random.nextFloat() - this.random.nextFloat()) * 0.2F;
                this.playSound(SoundEvents.SLIME_SQUISH, 1.0F, pitch);
                if (!player.getAbilities().instabuild) {
                    itemstack.shrink(1);
                }
                return InteractionResult.sidedSuccess(this.level().isClientSide);
            }
        }

        if (hand != InteractionHand.MAIN_HAND) {
            return super.mobInteract(player, hand);
        }

        List<Mob> leashedToPlayer = this.level().getEntitiesOfClass(Mob.class,
                this.getBoundingBox().inflate(7.0D), m -> m.getLeashHolder() == player);

        if (!leashedToPlayer.isEmpty()) {
            if (!this.level().isClientSide) {
                for (Mob m : leashedToPlayer) {
                    m.setLeashedTo(this, true);
                    this.spawnLeashGoo(m);
                }
                this.playSound(SoundEvents.SLIME_SQUISH, 1.0F, 1.0F);
                this.triggerInteractAnim();
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }

        if (player.isShiftKeyDown()) {
            if (!this.level().isClientSide) {
                PENDING_LEASH.put(player.getUUID(), new PendingLeash(this.getUUID(), this.level().getGameTime() + PENDING_LEASH_TICKS));
                this.playSound(SoundEvents.SLIME_SQUISH, 0.6F, 1.4F);
                this.triggerInteractAnim();
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }

        return super.mobInteract(player, hand);
    }

    private static final int PENDING_LEASH_TICKS = 200;
    private static final Map<UUID, PendingLeash> PENDING_LEASH = new HashMap<>();

    private record PendingLeash(UUID golemId, long expiresAtTick) {
    }

    public static boolean tryCompleteLeash(Player player, Entity target) {
        PendingLeash pending = PENDING_LEASH.get(player.getUUID());
        if (pending == null) {
            return false;
        }

        if (player.level().getGameTime() > pending.expiresAtTick()) {
            PENDING_LEASH.remove(player.getUUID());
            return false;
        }

        if (!(target instanceof Mob mob) || mob.isLeashed() || mob instanceof SlimeGolemEntity) {
            return false;
        }

        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return false;
        }

        Entity golemEntity = serverLevel.getEntity(pending.golemId());
        if (!(golemEntity instanceof SlimeGolemEntity golem) || !golem.isAlive()) {
            PENDING_LEASH.remove(player.getUUID());
            return false;
        }

        if (mob.distanceToSqr(golem) > 36.0D) {
            return false;
        }

        mob.setLeashedTo(golem, true);
        golem.spawnLeashGoo(mob);
        golem.playSound(SoundEvents.SLIME_SQUISH, 1.0F, 1.4F);
        golem.triggerInteractAnim();
        PENDING_LEASH.remove(player.getUUID());
        return true;
    }

    public void spawnLeashGoo(Mob target) {
        if (this.level() instanceof ServerLevel serverLevel) {
            Vec3 from = this.position().add(0.0D, this.getBbHeight() * 0.5D, 0.0D);
            Vec3 to = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);

            int steps = 6;
            for (int i = 0; i <= steps; i++) {
                double t = (double) i / steps;
                serverLevel.sendParticles(ParticleTypes.ITEM_SLIME,
                        Mth.lerp(t, from.x, to.x), Mth.lerp(t, from.y, to.y), Mth.lerp(t, from.z, to.z),
                        2, 0.05D, 0.05D, 0.05D, 0.0D);
            }
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return this.getSlimeSize() == SMALL ? SoundEvents.SLIME_SQUISH_SMALL : SoundEvents.SLIME_SQUISH;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return this.getSlimeSize() == SMALL ? SoundEvents.SLIME_HURT_SMALL : SoundEvents.SLIME_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return this.getSlimeSize() == SMALL ? SoundEvents.SLIME_DEATH_SMALL : SoundEvents.SLIME_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState blockIn) {
        this.playSound(SoundEvents.SLIME_JUMP, 0.4F, (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F);
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHitIn) {
        super.dropCustomDeathLoot(source, looting, recentlyHitIn);
        if (this.getSlimeSize() == SMALL) {
            int count = 1 + this.random.nextInt(2) + looting;
            this.spawnAtLocation(new ItemStack(Items.SLIME_BALL, count));
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("SlimeSize", this.getSlimeSize());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("SlimeSize")) {
            this.setSlimeSize(tag.getInt("SlimeSize"), false);
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        super.registerControllers(controllers);

        controllers.add(new AnimationController<>(this, "jumpController", 5, event -> {
            if (this.isLanding()) {
                return event.setAndContinue(RawAnimation.begin().thenPlay("jump_land"));
            }
            if (this.isRotating()) {
                return event.setAndContinue(RawAnimation.begin().thenPlay("rotate"));
            }
            if (!this.onGround()) {
                if (this.getDeltaMovement().y > 0.0D) {
                    return event.setAndContinue(RawAnimation.begin().thenLoop("jump_up"));
                }
                return event.setAndContinue(RawAnimation.begin().thenLoop("jump_fall"));
            }
            return event.setAndContinue(RawAnimation.begin().thenLoop("idle"));
        }));

        controllers.add(new AnimationController<>(this, "interactController", 5, event -> {
            if (this.isInteracting()) {
                return event.setAndContinue(RawAnimation.begin().thenPlay("interact"));
            }
            event.getController().forceAnimationReset();
            return PlayState.STOP;
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
