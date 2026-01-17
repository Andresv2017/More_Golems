package net.darkblade.moregolems.sever.event;

import net.darkblade.moregolems.MoreGolems;
import net.darkblade.moregolems.sever.entity.custom.CactusGolemEntity;
import net.darkblade.moregolems.sever.entity.custom.GoldGolemEntity;
import net.darkblade.moregolems.sever.entity.custom.CastleGolemEntity; // Importante: Importar el Castle Golem
import net.darkblade.moregolems.sever.init.ModEntities;
import net.darkblade.moregolems.sever.item.custom.SolarisSwordItem;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = MoreGolems.MODID)
public class ModEvents {

    private static final List<SpawnRequest> PENDING_SPAWNS = new ArrayList<>();
    private record SpawnRequest(ServerLevel level, BlockPos pos, float yRot, float xRot, EntityType<?> entityType) {}

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide && event.getEntity().getClass() == IronGolem.class) {
            if (event.getLevel() instanceof ServerLevel serverLevel) {
                BlockPos pos = event.getEntity().blockPosition();

                if (serverLevel.getBiome(pos).is(BiomeTags.IS_JUNGLE)) {
                    PENDING_SPAWNS.add(new SpawnRequest(
                            serverLevel,
                            pos,
                            event.getEntity().getYRot(),
                            event.getEntity().getXRot(),
                            ModEntities.GOLD_GOLEM.get()
                    ));
                }
                else if (serverLevel.getBiome(pos).value().getBaseTemperature() >= 0.8f) {
                    PENDING_SPAWNS.add(new SpawnRequest(
                            serverLevel,
                            pos,
                            event.getEntity().getYRot(),
                            event.getEntity().getXRot(),
                            ModEntities.CACTUS_GOLEM.get()
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

                if (request.entityType == ModEntities.GOLD_GOLEM.get()) {
                    alreadyExists = !request.level.getEntitiesOfClass(GoldGolemEntity.class,
                            new net.minecraft.world.phys.AABB(request.pos).inflate(4.0D)).isEmpty();
                } else {
                    alreadyExists = !request.level.getEntitiesOfClass(CactusGolemEntity.class,
                            new net.minecraft.world.phys.AABB(request.pos).inflate(4.0D)).isEmpty();
                }

                if (!alreadyExists) {
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
        BlockPos eventPos = event.getPos();

        BlockPos pumpkinPos = null;

        if (placedBlock.is(Blocks.CARVED_PUMPKIN) || placedBlock.is(Blocks.JACK_O_LANTERN)) {
            pumpkinPos = eventPos;
        }
        else if (placedBlock.is(Blocks.BRICKS)) {
            BlockPos below = eventPos.below();
            BlockState stateBelow = event.getLevel().getBlockState(below);
            if (stateBelow.is(Blocks.CARVED_PUMPKIN) || stateBelow.is(Blocks.JACK_O_LANTERN)) {
                pumpkinPos = below;
            }
        }

        if (pumpkinPos == null) return;

        if (event.getLevel() instanceof ServerLevel serverLevel) {

            // --- GOLEM CASTILLO ---
            boolean topBrick = serverLevel.getBlockState(pumpkinPos.above()).is(Blocks.BRICKS);
            boolean bot1Clay = serverLevel.getBlockState(pumpkinPos.below(1)).is(Blocks.CLAY);
            boolean bot2Clay = serverLevel.getBlockState(pumpkinPos.below(2)).is(Blocks.CLAY);
            boolean bot3Diamond = serverLevel.getBlockState(pumpkinPos.below(3)).is(Blocks.DIAMOND_BLOCK);

            if (topBrick && bot1Clay && bot2Clay && bot3Diamond) {
                boolean patternFound = false;
                BlockPos tower1 = null, tower2 = null;
                BlockPos connect1 = null, connect2 = null;

                if (checkSideTower(serverLevel, pumpkinPos.north(2)) &&
                        checkConnector(serverLevel, pumpkinPos.north(1)) &&
                        checkSideTower(serverLevel, pumpkinPos.south(2)) &&
                        checkConnector(serverLevel, pumpkinPos.south(1))) {
                    tower1 = pumpkinPos.north(2); tower2 = pumpkinPos.south(2);
                    connect1 = pumpkinPos.north(1); connect2 = pumpkinPos.south(1);
                    patternFound = true;
                }
                else if (checkSideTower(serverLevel, pumpkinPos.east(2)) &&
                        checkConnector(serverLevel, pumpkinPos.east(1)) &&
                        checkSideTower(serverLevel, pumpkinPos.west(2)) &&
                        checkConnector(serverLevel, pumpkinPos.west(1))) {
                    tower1 = pumpkinPos.east(2); tower2 = pumpkinPos.west(2);
                    connect1 = pumpkinPos.east(1); connect2 = pumpkinPos.west(1);
                    patternFound = true;
                }

                if (patternFound) {

                    breakBlockWithParticles(serverLevel, pumpkinPos.above());
                    breakBlockWithParticles(serverLevel, pumpkinPos);
                    breakBlockWithParticles(serverLevel, pumpkinPos.below(1));
                    breakBlockWithParticles(serverLevel, pumpkinPos.below(2));
                    breakBlockWithParticles(serverLevel, pumpkinPos.below(3));

                    removeSideTower(serverLevel, tower1);
                    removeSideTower(serverLevel, tower2);

                    breakBlockWithParticles(serverLevel, connect1.below(3));
                    breakBlockWithParticles(serverLevel, connect2.below(3));

                    CastleGolemEntity golem = ModEntities.CASTLE_GOLEM.get().create(serverLevel);
                    if (golem != null) {
                        BlockPos spawnPos = pumpkinPos.below(3);
                        golem.moveTo(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D, 0.0F, 0.0F);

                        if (event.getEntity() != null) {
                            golem.setOwnerId(event.getEntity().getUUID());
                        }

                        golem.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(spawnPos), MobSpawnType.TRIGGERED, null, null);
                        serverLevel.addFreshEntity(golem);
                    }
                    return;
                }
            }


            if (placedBlock.is(Blocks.CARVED_PUMPKIN) || placedBlock.is(Blocks.JACK_O_LANTERN)) {

                BlockPos bodyPos = pumpkinPos.below();
                BlockPos legsPos = bodyPos.below();

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
                    } else if (serverLevel.getBlockState(bodyPos.east()).is(Blocks.GOLD_BLOCK) &&
                            serverLevel.getBlockState(bodyPos.west()).is(Blocks.GOLD_BLOCK)) {
                        arm1 = bodyPos.east();
                        arm2 = bodyPos.west();
                        patternFound = true;
                    }

                    if (patternFound) {

                        breakBlockWithParticles(serverLevel, pumpkinPos);
                        breakBlockWithParticles(serverLevel, bodyPos);
                        breakBlockWithParticles(serverLevel, legsPos);
                        breakBlockWithParticles(serverLevel, arm1);
                        breakBlockWithParticles(serverLevel, arm2);

                        GoldGolemEntity golem = ModEntities.GOLD_GOLEM.get().create(serverLevel);
                        if (golem != null) {
                            golem.moveTo(bodyPos.getX() + 0.5D, legsPos.getY(), bodyPos.getZ() + 0.5D, 0.0F, 0.0F);
                            golem.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(bodyPos), MobSpawnType.TRIGGERED, null, null);
                            serverLevel.addFreshEntity(golem);
                            serverLevel.playSound(null, bodyPos, SoundEvents.IRON_GOLEM_REPAIR, SoundSource.BLOCKS, 1.0F, 1.0F);
                        }
                        return;
                    }
                }

                // --- GOLEM DE CACTUS ---
                BlockPos cactusPos = pumpkinPos.below();
                if (serverLevel.getBlockState(cactusPos).is(Blocks.CACTUS)) {

                    breakBlockWithParticles(serverLevel, pumpkinPos);
                    breakBlockWithParticles(serverLevel, cactusPos);

                    CactusGolemEntity golem = ModEntities.CACTUS_GOLEM.get().create(serverLevel);
                    if (golem != null) {
                        golem.moveTo(cactusPos.getX() + 0.5D, cactusPos.getY(), cactusPos.getZ() + 0.5D, 0.0F, 0.0F);
                        golem.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(cactusPos), MobSpawnType.TRIGGERED, null, null);
                        serverLevel.addFreshEntity(golem);
                        serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, cactusPos.getX() + 0.5D, cactusPos.getY() + 0.5D, cactusPos.getZ() + 0.5D, 15, 0.5, 0.5, 0.5, 0.05);
                        serverLevel.playSound(null, cactusPos, SoundEvents.IRON_GOLEM_REPAIR, SoundSource.BLOCKS, 1.0F, 1.0F);
                    }
                }
            }
        }
    }

    private static boolean checkSideTower(ServerLevel level, BlockPos sidePos) {
        return level.getBlockState(sidePos.below(1)).is(Blocks.BRICKS) &&
                level.getBlockState(sidePos.below(2)).is(Blocks.CLAY) &&
                level.getBlockState(sidePos.below(3)).is(Blocks.CLAY);
    }

    private static boolean checkConnector(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos.below(3)).is(Blocks.CLAY);
    }

    private static void removeSideTower(ServerLevel level, BlockPos sidePos) {
        breakBlockWithParticles(level, sidePos.below(1));
        breakBlockWithParticles(level, sidePos.below(2));
        breakBlockWithParticles(level, sidePos.below(3));
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getSource().getEntity() instanceof Player player) {
            ItemStack heldItem = player.getMainHandItem();
            if (heldItem.getItem() instanceof SolarisSwordItem solaris) {
                solaris.addKill(heldItem);
            }
        }
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getSource().getEntity() instanceof Player player) {
            ItemStack heldItem = player.getMainHandItem();

            if (heldItem.getItem() instanceof SolarisSwordItem solaris) {
                int level = solaris.getLevel(heldItem);

                if (level > 0) {
                    float extraDamage = level * 2.0f;
                    event.setAmount(event.getAmount() + extraDamage);
                }

                if (level == 4) {
                    event.getEntity().setSecondsOnFire(4);
                }
            }
        }
    }

    private static void breakBlockWithParticles(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!state.isAir()) {
            level.levelEvent(2001, pos, Block.getId(state));
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        }
    }
}