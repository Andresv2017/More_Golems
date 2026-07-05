package net.darkblade.moregolems.sever.event;

import net.darkblade.moregolems.MoreGolems;
import net.darkblade.moregolems.sever.entity.custom.CactusGolemEntity;
import net.darkblade.moregolems.sever.entity.custom.GoldGolemEntity;
import net.darkblade.moregolems.sever.entity.custom.CastleGolemEntity;
import net.darkblade.moregolems.sever.entity.custom.SlimeGolemEntity;
import net.darkblade.moregolems.sever.init.ModEntities;
import net.darkblade.moregolems.sever.init.ModItems;
import net.darkblade.moregolems.sever.init.ModParticles;
import net.darkblade.moregolems.sever.init.ModSounds;
import net.darkblade.moregolems.sever.item.custom.SolarisSwordItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.Biomes; // <--- IMPORTANTE: Importar Biomes
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
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

            if (event.getEntity().getPersistentData().getBoolean("moregolems:checked")) {
                return;
            }

            event.getEntity().getPersistentData().putBoolean("moregolems:checked", true);
            // ---------------------------------------------------

            if (event.getLevel() instanceof ServerLevel serverLevel) {
                BlockPos pos = event.getEntity().blockPosition();
                var biome = serverLevel.getBiome(pos);

                if (biome.is(BiomeTags.IS_JUNGLE)) {
                    PENDING_SPAWNS.add(new SpawnRequest(serverLevel, pos, event.getEntity().getYRot(), event.getEntity().getXRot(), ModEntities.GOLD_GOLEM.get()));
                }
                else if (biome.is(Biomes.DESERT)) {
                    PENDING_SPAWNS.add(new SpawnRequest(serverLevel, pos, event.getEntity().getYRot(), event.getEntity().getXRot(), ModEntities.CACTUS_GOLEM.get()));
                }
                else if (biome.is(Biomes.PLAINS) || biome.is(Biomes.SUNFLOWER_PLAINS)) {
                    if (serverLevel.getRandom().nextInt(3) == 0) {
                        PENDING_SPAWNS.add(new SpawnRequest(serverLevel, pos, event.getEntity().getYRot(), event.getEntity().getXRot(), ModEntities.CASTLE_GOLEM.get()));
                    }
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

                BlockPos spawnPos = request.pos;

                if (request.entityType == ModEntities.CASTLE_GOLEM.get()) {
                    BlockPos bestPos = null;
                    double bestDistance = Double.MAX_VALUE;

                    for (int x = -10; x <= 10; x++) {
                        for (int z = -10; z <= 10; z++) {
                            int targetX = request.pos.getX() + x;
                            int targetZ = request.pos.getZ() + z;

                            int targetY = request.level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, targetX, targetZ);

                            BlockPos candidatePos = new BlockPos(targetX, targetY, targetZ);

                            if (validateAndClearSpace(request.level, candidatePos, true)) {
                                double dist = request.pos.distSqr(candidatePos);
                                if (dist < bestDistance) {
                                    bestDistance = dist;
                                    bestPos = candidatePos;
                                }
                            }
                        }
                    }

                    if (bestPos != null) {
                        spawnPos = bestPos;
                        validateAndClearSpace(request.level, spawnPos, false);
                    } else {
                        continue;
                    }
                }

                boolean alreadyExists = false;
                double checkRadius = 4.0D;
                if (request.entityType == ModEntities.GOLD_GOLEM.get()) {
                    alreadyExists = !request.level.getEntitiesOfClass(GoldGolemEntity.class, new net.minecraft.world.phys.AABB(spawnPos).inflate(checkRadius)).isEmpty();
                } else if (request.entityType == ModEntities.CACTUS_GOLEM.get()) {
                    alreadyExists = !request.level.getEntitiesOfClass(CactusGolemEntity.class, new net.minecraft.world.phys.AABB(spawnPos).inflate(checkRadius)).isEmpty();
                } else if (request.entityType == ModEntities.CASTLE_GOLEM.get()) {
                    alreadyExists = !request.level.getEntitiesOfClass(CastleGolemEntity.class, new net.minecraft.world.phys.AABB(spawnPos).inflate(checkRadius)).isEmpty();
                }

                if (!alreadyExists) {
                    List<IronGolem> originalGolems = request.level.getEntitiesOfClass(IronGolem.class, new net.minecraft.world.phys.AABB(request.pos).inflate(2.0D));
                    for (IronGolem ironGolem : originalGolems) {
                        if (!ironGolem.isRemoved()) {
                            ironGolem.discard();
                        }
                    }

                    Mob golem = (Mob) request.entityType.create(request.level);
                    if (golem != null) {
                        golem.moveTo(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5, request.yRot, request.xRot);
                        golem.finalizeSpawn(request.level, request.level.getCurrentDifficultyAt(spawnPos), MobSpawnType.EVENT, null, null);
                        request.level.addFreshEntity(golem);
                    }
                }
            }
        }
    }

    private static boolean validateAndClearSpace(ServerLevel level, BlockPos centerPos, boolean simulate) {
        List<BlockPos> toDestroy = new ArrayList<>();

        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                BlockPos floorPos = centerPos.offset(x, -1, z);

                BlockState floorState = level.getBlockState(floorPos);
                if (!floorState.isFaceSturdy(level, floorPos, Direction.UP) &&
                        !floorState.is(Blocks.FARMLAND) &&
                        !floorState.is(Blocks.DIRT_PATH)) {
                    return false;
                }

                for (int y = 0; y <= 2; y++) {
                    BlockPos bodyPos = centerPos.offset(x, y, z);
                    BlockState bodyState = level.getBlockState(bodyPos);

                    if (!bodyState.getCollisionShape(level, bodyPos).isEmpty() || !bodyState.isAir()) {
                        if (isBreakableObstacle(bodyState)) {
                            toDestroy.add(bodyPos);
                        } else {
                            return false;
                        }
                    }
                }
            }
        }

        if (simulate) return true;

        for (BlockPos pos : toDestroy) {
            level.destroyBlock(pos, false);
        }

        return true;
    }

    private static boolean isBreakableObstacle(BlockState state) {
        return state.is(Blocks.GRASS) ||
                state.is(Blocks.TALL_GRASS) ||
                state.is(Blocks.FERN) ||
                state.is(Blocks.LARGE_FERN) ||
                state.is(Blocks.DEAD_BUSH) ||
                state.is(Blocks.SNOW) ||
                state.is(Blocks.VINE) ||
                state.is(Blocks.SUNFLOWER) ||
                state.is(Blocks.LILAC) ||
                state.is(Blocks.ROSE_BUSH) ||
                state.is(Blocks.PEONY) ||
                state.is(BlockTags.LEAVES) ||
                state.is(BlockTags.FLOWERS) ||
                state.is(BlockTags.REPLACEABLE) ||
                state.is(Blocks.GRASS_BLOCK) ||
                state.is(Blocks.DIRT);
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

                // --- SLIME GOLEM: 3 slime blocks in a row, pumpkin on top of the middle one ---
                BlockPos slimeMiddlePos = pumpkinPos.below();
                if (serverLevel.getBlockState(slimeMiddlePos).is(Blocks.SLIME_BLOCK)) {

                    BlockPos slimeSide1 = null;
                    BlockPos slimeSide2 = null;
                    boolean slimePatternFound = false;

                    if (serverLevel.getBlockState(slimeMiddlePos.north()).is(Blocks.SLIME_BLOCK) &&
                            serverLevel.getBlockState(slimeMiddlePos.south()).is(Blocks.SLIME_BLOCK)) {
                        slimeSide1 = slimeMiddlePos.north();
                        slimeSide2 = slimeMiddlePos.south();
                        slimePatternFound = true;
                    } else if (serverLevel.getBlockState(slimeMiddlePos.east()).is(Blocks.SLIME_BLOCK) &&
                            serverLevel.getBlockState(slimeMiddlePos.west()).is(Blocks.SLIME_BLOCK)) {
                        slimeSide1 = slimeMiddlePos.east();
                        slimeSide2 = slimeMiddlePos.west();
                        slimePatternFound = true;
                    }

                    if (slimePatternFound) {
                        breakBlockWithParticles(serverLevel, pumpkinPos);
                        breakBlockWithParticles(serverLevel, slimeMiddlePos);
                        breakBlockWithParticles(serverLevel, slimeSide1);
                        breakBlockWithParticles(serverLevel, slimeSide2);

                        SlimeGolemEntity golem = ModEntities.SLIME_GOLEM.get().create(serverLevel);
                        if (golem != null) {
                            golem.moveTo(slimeMiddlePos.getX() + 0.5D, slimeMiddlePos.getY(), slimeMiddlePos.getZ() + 0.5D, 0.0F, 0.0F);
                            golem.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(slimeMiddlePos), MobSpawnType.TRIGGERED, null, null);
                            serverLevel.addFreshEntity(golem);
                            serverLevel.playSound(null, slimeMiddlePos, SoundEvents.SLIME_SQUISH, SoundSource.BLOCKS, 1.0F, 1.0F);
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

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide) return;

        if (SlimeGolemEntity.tryCompleteLeash(event.getEntity(), event.getTarget())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
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

    @SubscribeEvent
    public static void onPlayerFall(LivingFallEvent event) {
        if (event.getEntity() instanceof Player player) {

            if (player.level().isClientSide) {
                return;
            }

            if (event.getDistance() >= 4.0f &&
                    player.isUsingItem() &&
                    player.getUseItem().getItem() == ModItems.CONCRETE_SHIELD.get()) {

                ServerLevel serverLevel = (ServerLevel) player.level();
                Vec3 pos = player.position();
                double radius = 4.0D;
                float damageAmount = 10.0F;

                serverLevel.sendParticles(
                        ModParticles.SHIELD_RING.get(),
                        pos.x,
                        pos.y + 0.15,
                        pos.z,
                        1,
                        0.0, 0.0, 0.0,
                        0.0
                );

                serverLevel.playSound(null, pos.x, pos.y, pos.z,
                        ModSounds.CONCRETE_SHIELD.get(), SoundSource.PLAYERS, 1.0f, 1.0f);

                List<LivingEntity> enemies = serverLevel.getEntitiesOfClass(
                        LivingEntity.class,
                        player.getBoundingBox().inflate(radius),
                        e -> e != player && e.isAlive() && !player.isAlliedTo(e)
                );

                for (LivingEntity enemy : enemies) {
                    if (enemy.distanceToSqr(player) <= radius * radius) {
                        enemy.hurt(player.damageSources().playerAttack(player), damageAmount);

                        double dx = enemy.getX() - player.getX();
                        double dz = enemy.getZ() - player.getZ();
                        enemy.knockback(1.5F, -dx, -dz);
                    }
                }

                event.setDamageMultiplier(0.0f);
            }
        }
    }
}