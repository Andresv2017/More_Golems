package net.darkblade.moregolems.sever.event;

import net.darkblade.moregolems.MoreGolems;
import net.darkblade.moregolems.sever.entity.custom.CactusGolemEntity;
import net.darkblade.moregolems.sever.init.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = MoreGolems.MODID)
public class ModEvents {

    private static final List<SpawnRequest> PENDING_SPAWNS = new ArrayList<>();

    private record SpawnRequest(ServerLevel level, BlockPos pos, float yRot, float xRot) {}

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide && event.getEntity().getClass() == IronGolem.class) {
            if (event.getLevel() instanceof ServerLevel serverLevel) {
                BlockPos pos = event.getEntity().blockPosition();

                if (serverLevel.getBiome(pos).value().getBaseTemperature() >= 0.8f) {
                    PENDING_SPAWNS.add(new SpawnRequest(
                            serverLevel,
                            pos,
                            event.getEntity().getYRot(),
                            event.getEntity().getXRot()
                    ));
                }
            }
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && !PENDING_SPAWNS.isEmpty()) {
            List<SpawnRequest> toProcess = new ArrayList<>(PENDING_SPAWNS);
            PENDING_SPAWNS.clear();

            for (SpawnRequest request : toProcess) {
                if (request.level.getEntitiesOfClass(CactusGolemEntity.class,
                        new net.minecraft.world.phys.AABB(request.pos).inflate(4.0D)).isEmpty()) {

                    CactusGolemEntity cactus = ModEntities.CACTUS_GOLEM.get().create(request.level);
                    if (cactus != null) {
                        cactus.moveTo(request.pos.getX() + 1.5, request.pos.getY(), request.pos.getZ() + 1.5,
                                request.yRot, request.xRot);

                        cactus.finalizeSpawn(request.level, request.level.getCurrentDifficultyAt(request.pos),
                                MobSpawnType.EVENT, null, null);

                        request.level.addFreshEntity(cactus);
                        //System.out.println("[DEBUG] Cactus Golem generado como acompañante en: " + request.pos);
                    }
                }
            }
        }
    }
}