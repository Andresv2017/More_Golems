package net.darkblade.moregolems.sever.entity.custom;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.EnumSet;

public class BaseGolemEntity extends IronGolem implements GeoEntity {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private static final EntityDataAccessor<Boolean> IS_EMOTING =
            SynchedEntityData.defineId(BaseGolemEntity.class, EntityDataSerializers.BOOLEAN);

    public BaseGolemEntity(EntityType<? extends IronGolem> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(IS_EMOTING, false);
    }

    public void setEmoting(boolean emoting) {
        this.entityData.set(IS_EMOTING, emoting);
    }

    public boolean isEmoting() {
        return this.entityData.get(IS_EMOTING);
    }

    public float cachedHeadYaw = 0F;
    public float cachedHeadPitch = 0F;

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "emoteController", 15, event -> {
            if (this.isEmoting()) {
                return event.setAndContinue(RawAnimation.begin().thenPlay("emote"));
            }
            return PlayState.STOP;
        }).setAnimationSpeed(0.35));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    public static class RandomEmoteGoal extends Goal {
        private final BaseGolemEntity golem;
        private int animationTick = 0;

        private final int animationLength;
        private final int chance;


        public RandomEmoteGoal(BaseGolemEntity golem, int durationTicks, int chance) {
            this.golem = golem;
            this.animationLength = durationTicks;
            this.chance = chance;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = golem.getTarget();
            return (golem.getRandom().nextInt(chance) == 0) &&
                    (target == null || !target.isAlive()) &&
                    golem.onGround();
        }

        @Override
        public boolean canContinueToUse() {
            return animationTick < animationLength;
        }

        @Override
        public void start() {
            this.animationTick = 0;
            golem.setEmoting(true);
            golem.getNavigation().stop();
        }

        @Override
        public void stop() {
            golem.setEmoting(false);
            this.animationTick = 0;
        }

        @Override
        public void tick() {
            golem.getNavigation().stop();
            animationTick++;
        }
    }
}