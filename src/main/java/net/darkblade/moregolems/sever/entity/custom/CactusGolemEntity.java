package net.darkblade.moregolems.sever.entity.custom;

import net.darkblade.moregolems.sever.entity.ai.SimpleAabbMeleeGoal;
import net.darkblade.moregolems.sever.init.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
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
import net.minecraft.world.level.biome.Biomes;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;


public class CactusGolemEntity extends IronGolem implements GeoEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private static final EntityDataAccessor<Boolean> DATA_ATTACKING =
            SynchedEntityData.defineId(CactusGolemEntity.class, EntityDataSerializers.BOOLEAN);

    private static final EntityDataAccessor<Boolean> HAS_SPINES =
            SynchedEntityData.defineId(CactusGolemEntity.class, EntityDataSerializers.BOOLEAN);

    private static final double ATTACK_RANGE = 1.50;
    private static final double CHASE_SPEED  = 1.25;
    private static final int ATTACK_DURATION = 21;
    private static final int[] DAMAGE_FRAMES = {9};
    private static final int CD_BASE = 10;

    private static final SimpleAabbMeleeGoal.AttackHitbox HITBOX =
            SimpleAabbMeleeGoal.AttackHitbox.of(
                    0.90,
                    1.00,
                    1.30,
                    0.00,
                    0.00
            );

    public float cachedHeadYaw = 0F;
    public float cachedHeadPitch = 0F;

    private static final int REGEN_TIME = 6000;
    private int growthTimer = 0;

    public CactusGolemEntity(EntityType<? extends IronGolem> entityType, Level level) {
        super(entityType, level);
    }

    public static AttributeSupplier.Builder setAttributes() {
        return IronGolem.createAttributes()
                .add(Attributes.MAX_HEALTH, 60.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.ATTACK_DAMAGE, 0.6D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_ATTACKING, false);
        this.entityData.define(HAS_SPINES, true);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new SimpleAabbMeleeGoal<>(
                this, ATTACK_RANGE, CHASE_SPEED, true,
                ATTACK_DURATION, DAMAGE_FRAMES, CD_BASE, HITBOX,
                this::setAttacking
        ));

        this.goalSelector.addGoal(2, new MoveBackToVillageGoal(this, 0.6D, false));
        this.goalSelector.addGoal(4, new GolemRandomStrollInVillageGoal(this, 0.6D));

        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.goalSelector.addGoal(9, new WaterAvoidingRandomStrollGoal(this, 1.0D));

        this.targetSelector.addGoal(1, new DefendVillageTargetGoal(this));
        this.targetSelector.addGoal(2, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false,
                this::isAngryAt));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Mob.class, 5, false, false,
                (entity) -> entity instanceof Enemy && !(entity instanceof Creeper)));
        this.targetSelector.addGoal(4, new ResetUniversalAngerTargetGoal<>(this, false));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movementController", 5, event -> {
            if (event.getLimbSwingAmount() > 0.01F) {
                return event.setAndContinue(RawAnimation.begin().thenLoop("walk"));
            }

            return event.setAndContinue(RawAnimation.begin().thenLoop("idle"));
        }));

        controllers.add(new AnimationController<>(this, "attackController", 2, event -> {
            if (this.isAttacking()) {
                return event.setAndContinue(RawAnimation.begin().thenPlay("attack"));
            }
            event.getController().forceAnimationReset();
            return PlayState.STOP;
        }));
    }

    public void setAttacking(boolean attacking) {
        this.entityData.set(DATA_ATTACKING, attacking);
    }

    public boolean isAttacking() {
        return this.entityData.get(DATA_ATTACKING);
    }

    @Override
    public void aiStep() {
        super.aiStep();

        if (!this.level().isClientSide && this.isAlive()) {
            if (!this.hasSpines()) {

                boolean isHot = this.level().getBiome(this.blockPosition()).value().getBaseTemperature() >= 1.0f;

                if (isHot) {
                    this.growthTimer += 2;
                } else {
                    this.growthTimer += 1;
                }

                if (this.growthTimer >= REGEN_TIME) {
                    this.setHasSpines(true);
                    this.growthTimer = 0;
                    this.playSound(SoundEvents.CHORUS_FLOWER_GROW, 1.0F, 1.0F);
                }
            }
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putInt("GrowthTimer", this.growthTimer);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        this.growthTimer = compound.getInt("GrowthTimer");
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!this.level().isClientSide && source.getDirectEntity() instanceof LivingEntity attacker) {
            attacker.hurt(this.damageSources().thorns(this), 3.0F);
        }
        return super.hurt(source, amount);
    }

    @Override
    public void playerTouch(Player player) {
        if (!this.level().isClientSide && this.hasSpines()) {
            if (this.distanceToSqr(player) < (this.getBbWidth() * this.getBbWidth() + 0.1)) {
                player.hurt(this.damageSources().cactus(), 1.0F);
            }
        }
        super.playerTouch(player);
    }

    @Override
    protected void doPush(Entity entity) {
        if (!this.level().isClientSide && this.hasSpines() && entity instanceof LivingEntity living) {
            double distance = this.distanceToSqr(living);
            double range = 0.5D;

            if (distance < range) {
                living.hurt(this.damageSources().cactus(), 1.0F);
            }
        }
        super.doPush(entity);
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack itemstack = player.getItemInHand(hand);

        if (itemstack.is(Items.SHEARS) && this.hasSpines()) {
            if (!this.level().isClientSide) {
                this.setHasSpines(false);
                this.growthTimer = 0;

                int count = 2 + this.random.nextInt(3);
                for(int i = 0; i < count; ++i) {
                    this.spawnAtLocation(ModItems.CACTUS_SPINE.get());
                }

                itemstack.hurtAndBreak(1, player, (p) -> p.broadcastBreakEvent(hand));
                this.playSound(SoundEvents.SHEEP_SHEAR, 1.0F, 1.0F);
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }
        return super.mobInteract(player, hand);
    }

    public boolean hasSpines() { return this.entityData.get(HAS_SPINES); }
    public void setHasSpines(boolean value) { this.entityData.set(HAS_SPINES, value); }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}