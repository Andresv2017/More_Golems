package net.darkblade.moregolems.sever.entity.ai;

import net.darkblade.moregolems.sever.entity.debug.DebugAABB;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.AABB;

import java.util.EnumSet;
import java.util.List;
import java.util.function.Predicate;

public class SimpleAabbMeleeGoal<E extends PathfinderMob> extends Goal {

    public interface AttackAnimBridge { void setAttacking(boolean value); }

    public static final class AttackHitbox {
        public final double halfSize, height, forward, lateral, yOffset;
        public AttackHitbox(double halfSize, double height, double forward, double lateral, double yOffset) {
            this.halfSize = halfSize; this.height = height;
            this.forward = forward; this.lateral = lateral; this.yOffset = yOffset;
        }
        public static AttackHitbox of(double half, double h, double f, double lat, double y) {
            return new AttackHitbox(half, h, f, lat, y);
        }
    }

    private final E mob;
    private final double chaseSpeed;
    private final double attackRange;
    private final boolean requireLOS;
    private final int durationTicks;
    private final int[] damageFrames;
    private final int cooldownBaseTicks;
    private final AttackHitbox hitbox;
    private final AttackAnimBridge animBridge;

    private boolean active = false;
    private int tick = 0;
    private int ticksUntilNextAttack = 0;

    private static final boolean DEBUG_AABB = false;

    public SimpleAabbMeleeGoal(E mob, double attackRange, double chaseSpeed, boolean requireLOS,
                               int durationTicks, int[] damageFrames, int cooldownBase,
                               AttackHitbox hitbox, AttackAnimBridge animBridge) {
        this.mob = mob;
        this.attackRange = attackRange;
        this.chaseSpeed = chaseSpeed;
        this.requireLOS = requireLOS;
        this.durationTicks = durationTicks;
        this.damageFrames = damageFrames;
        this.cooldownBaseTicks = cooldownBase;
        this.hitbox = hitbox;
        this.animBridge = animBridge;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = mob.getTarget();
        return target != null && target.isAlive();
    }

    @Override
    public void tick() {
        LivingEntity target = mob.getTarget();
        if (target == null) { stopAttack(); return; }

        mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
        double distXZ = getDistXZ(target);
        boolean inRange = distXZ <= attackRange;

        this.ticksUntilNextAttack = Math.max(this.ticksUntilNextAttack - 1, 0);

        if (!active) {
            if (inRange && this.ticksUntilNextAttack <= 0 && isFacingTarget(target)) {
                startAttack();
            } else {
                mob.getNavigation().moveTo(target, chaseSpeed);
            }
        } else {
            mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
            updateAttack();
        }
    }

    private boolean isFacingTarget(LivingEntity target) {
        double dx = target.getX() - mob.getX();
        double dz = target.getZ() - mob.getZ();
        float targetYaw = (float)(Mth.atan2(dz, dx) * (180D / Math.PI)) - 90.0F;
        float diff = Mth.abs(Mth.wrapDegrees(targetYaw - mob.getYHeadRot()));

        return diff < 30.0F;
    }

    private void startAttack() {
        active = true;
        tick = 0;
        animBridge.setAttacking(true);
        mob.getNavigation().stop();
    }

    private void updateAttack() {
        tick++;
        for (int f : damageFrames) {
            if (tick == f) {
                AABB box = buildAABB(hitbox);
                applyDamage(box);
                if (DEBUG_AABB && mob.level() instanceof ServerLevel sl) {
                    DebugAABB.drawAabbEdges(sl, box);
                }
            }
        }
        if (tick >= durationTicks) stopAttack();
    }

    private void stopAttack() {
        active = false;
        animBridge.setAttacking(false);
        this.ticksUntilNextAttack = cooldownBaseTicks;
    }

    private double getDistXZ(LivingEntity target) {
        double dx = target.getX() - mob.getX();
        double dz = target.getZ() - mob.getZ();
        double distXZ = Math.sqrt(dx*dx + dz*dz);
        double sumR = 0.5 * (mob.getBbWidth() + target.getBbWidth());
        return Math.max(0.0, distXZ - sumR);
    }

    private AABB buildAABB(AttackHitbox hb) {
        float yawRad = mob.getYHeadRot() * Mth.DEG_TO_RAD;
        double dirX = -Mth.sin(yawRad), dirZ = Mth.cos(yawRad);
        double rightX = -dirZ, rightZ = dirX;
        double cx = mob.getX() + dirX * hb.forward + rightX * hb.lateral;
        double cz = mob.getZ() + dirZ * hb.forward + rightZ * hb.lateral;
        return new AABB(cx - hb.halfSize, mob.getY() + hb.yOffset, cz - hb.halfSize,
                cx + hb.halfSize, mob.getY() + hb.yOffset + hb.height, cz + hb.halfSize);
    }

    private void applyDamage(AABB box) {
        Predicate<LivingEntity> filter = e -> e.isAlive() && e != mob;
        List<LivingEntity> victims = mob.level().getEntitiesOfClass(LivingEntity.class, box, filter);
        for (LivingEntity v : victims) mob.doHurtTarget(v);
    }

    @Override
    public void stop() { stopAttack(); }
}