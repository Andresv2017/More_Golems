package net.darkblade.moregolems.sever.event;

import net.darkblade.moregolems.MoreGolems;
import net.darkblade.moregolems.sever.entity.custom.CactusGolemEntity;
import net.darkblade.moregolems.sever.entity.custom.GoldGolemEntity;
import net.darkblade.moregolems.sever.init.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = MoreGolems.MODID)
public class ModEvents {

    private static final List<SpawnRequest> PENDING_SPAWNS = new ArrayList<>();

    // AHORA INCLUYE EL TIPO DE ENTIDAD PARA SABER CUÁL SPAWNEAR
    private record SpawnRequest(ServerLevel level, BlockPos pos, float yRot, float xRot, EntityType<?> entityType) {}

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide && event.getEntity().getClass() == IronGolem.class) {
            if (event.getLevel() instanceof ServerLevel serverLevel) {
                BlockPos pos = event.getEntity().blockPosition();

                // 1. PRIORIDAD: JUNGLE -> GOLEM DE ORO
                if (serverLevel.getBiome(pos).is(BiomeTags.IS_JUNGLE)) {
                    PENDING_SPAWNS.add(new SpawnRequest(
                            serverLevel,
                            pos,
                            event.getEntity().getYRot(),
                            event.getEntity().getXRot(),
                            ModEntities.GOLD_GOLEM.get() // Pedimos Golem de Oro
                    ));
                }
                // 2. DESIERTO / CALOR -> GOLEM DE CACTUS
                // Usamos 'else if' para que no salgan los dos si el bioma cumple ambas condiciones
                else if (serverLevel.getBiome(pos).value().getBaseTemperature() >= 0.8f) {
                    PENDING_SPAWNS.add(new SpawnRequest(
                            serverLevel,
                            pos,
                            event.getEntity().getYRot(),
                            event.getEntity().getXRot(),
                            ModEntities.CACTUS_GOLEM.get() // Pedimos Golem de Cactus
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
                boolean alreadyExists = false;

                // Verificar si ya existe el golem específico cerca (Oro o Cactus)
                if (request.entityType == ModEntities.GOLD_GOLEM.get()) {
                    alreadyExists = !request.level.getEntitiesOfClass(GoldGolemEntity.class,
                            new net.minecraft.world.phys.AABB(request.pos).inflate(4.0D)).isEmpty();
                } else {
                    alreadyExists = !request.level.getEntitiesOfClass(CactusGolemEntity.class,
                            new net.minecraft.world.phys.AABB(request.pos).inflate(4.0D)).isEmpty();
                }

                if (!alreadyExists) {
                    // Crear la entidad genérica basada en el tipo solicitado
                    Mob golem = (Mob) request.entityType.create(request.level);

                    if (golem != null) {
                        golem.moveTo(request.pos.getX() + 1.5, request.pos.getY(), request.pos.getZ() + 1.5,
                                request.yRot, request.xRot);

                        golem.finalizeSpawn(request.level, request.level.getCurrentDifficultyAt(request.pos),
                                MobSpawnType.EVENT, null, null);

                        request.level.addFreshEntity(golem);
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getLevel().isClientSide()) return;

        BlockState placedBlock = event.getPlacedBlock();

        if (placedBlock.is(Blocks.CARVED_PUMPKIN) || placedBlock.is(Blocks.JACK_O_LANTERN)) {
            BlockPos pumpkinPos = event.getPos();
            BlockPos bodyPos = pumpkinPos.below();
            BlockPos legsPos = bodyPos.below();

            if (event.getLevel() instanceof ServerLevel serverLevel) {

                // --- GOLEM DE ORO ---
                if (serverLevel.getBlockState(bodyPos).is(Blocks.GOLD_BLOCK) &&
                        serverLevel.getBlockState(legsPos).is(Blocks.GOLD_BLOCK)) {

                    BlockPos arm1 = null;
                    BlockPos arm2 = null;
                    boolean patternFound = false;

                    if (serverLevel.getBlockState(bodyPos.north()).is(Blocks.GOLD_BLOCK) &&
                            serverLevel.getBlockState(bodyPos.south()).is(Blocks.GOLD_BLOCK)) {
                        arm1 = bodyPos.north();
                        arm2 = bodyPos.south();
                        patternFound = true;
                    }
                    else if (serverLevel.getBlockState(bodyPos.east()).is(Blocks.GOLD_BLOCK) &&
                            serverLevel.getBlockState(bodyPos.west()).is(Blocks.GOLD_BLOCK)) {
                        arm1 = bodyPos.east();
                        arm2 = bodyPos.west();
                        patternFound = true;
                    }

                    if (patternFound) {
                        serverLevel.setBlock(pumpkinPos, Blocks.AIR.defaultBlockState(), 3);
                        serverLevel.setBlock(bodyPos, Blocks.AIR.defaultBlockState(), 3);
                        serverLevel.setBlock(legsPos, Blocks.AIR.defaultBlockState(), 3);
                        serverLevel.setBlock(arm1, Blocks.AIR.defaultBlockState(), 3);
                        serverLevel.setBlock(arm2, Blocks.AIR.defaultBlockState(), 3);

                        GoldGolemEntity golem = ModEntities.GOLD_GOLEM.get().create(serverLevel);
                        if (golem != null) {
                            golem.moveTo(bodyPos.getX() + 0.5D, legsPos.getY(), bodyPos.getZ() + 0.5D, 0.0F, 0.0F);

                            golem.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(bodyPos),
                                    MobSpawnType.TRIGGERED, null, null);

                            serverLevel.addFreshEntity(golem);

                            serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                                    bodyPos.getX() + 0.5D, bodyPos.getY() + 0.5D, bodyPos.getZ() + 0.5D,
                                    1, 0.0, 0.0, 0.0, 0.0);

                            serverLevel.playSound(null, bodyPos, SoundEvents.IRON_GOLEM_REPAIR,
                                    SoundSource.BLOCKS, 1.0F, 1.0F);
                        }
                        return;
                    }
                }

                // --- GOLEM DE CACTUS ---
                BlockPos cactusPos = pumpkinPos.below();
                if (serverLevel.getBlockState(cactusPos).is(Blocks.CACTUS)) {

                    serverLevel.setBlock(pumpkinPos, Blocks.AIR.defaultBlockState(), 3);
                    serverLevel.setBlock(cactusPos, Blocks.AIR.defaultBlockState(), 3);

                    CactusGolemEntity golem = ModEntities.CACTUS_GOLEM.get().create(serverLevel);
                    if (golem != null) {
                        golem.moveTo(cactusPos.getX() + 0.5D, cactusPos.getY(), cactusPos.getZ() + 0.5D, 0.0F, 0.0F);

                        golem.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(cactusPos),
                                MobSpawnType.TRIGGERED, null, null);

                        serverLevel.addFreshEntity(golem);

                        serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                                cactusPos.getX() + 0.5D, cactusPos.getY() + 0.5D, cactusPos.getZ() + 0.5D,
                                15, 0.5, 0.5, 0.5, 0.05);

                        serverLevel.playSound(null, cactusPos, SoundEvents.IRON_GOLEM_REPAIR,
                                SoundSource.BLOCKS, 1.0F, 1.0F);
                    }
                }
            }
        }
    }
}