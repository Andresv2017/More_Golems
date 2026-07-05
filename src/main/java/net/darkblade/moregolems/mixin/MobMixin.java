package net.darkblade.moregolems.mixin;

import net.darkblade.moregolems.sever.entity.custom.SlimeGolemEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.ItemLike;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mob.class)
public abstract class MobMixin {

    @Unique
    private boolean moregolems$suppressLeashDrop;

    @Inject(method = "dropLeash(ZZ)V", at = @At("HEAD"))
    private void moregolems$captureLeashHolder(boolean sendPacket, boolean dropItem, CallbackInfo ci) {
        Mob self = (Mob) (Object) this;
        this.moregolems$suppressLeashDrop = self.getLeashHolder() instanceof SlimeGolemEntity;
    }

    @Redirect(
            method = "dropLeash(ZZ)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Mob;spawnAtLocation(Lnet/minecraft/world/level/ItemLike;)Lnet/minecraft/world/entity/item/ItemEntity;")
    )
    private ItemEntity moregolems$suppressLeadDrop(Mob self, ItemLike item) {
        if (this.moregolems$suppressLeashDrop) {
            return null;
        }
        return self.spawnAtLocation(item);
    }
}
