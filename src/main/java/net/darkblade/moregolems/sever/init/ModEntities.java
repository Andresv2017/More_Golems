package net.darkblade.moregolems.sever.init;

import net.darkblade.moregolems.MoreGolems;
import net.darkblade.moregolems.sever.entity.custom.CactusGolemEntity;
import net.darkblade.moregolems.sever.entity.custom.DartEntity;
import net.darkblade.moregolems.sever.entity.custom.GoldGolemEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MoreGolems.MODID);

    public static final RegistryObject<EntityType<CactusGolemEntity>> CACTUS_GOLEM =
            ENTITY_TYPES.register("cactus_golem",
                    () -> EntityType.Builder.of(CactusGolemEntity::new, MobCategory.MISC)
                            .sized(0.6f, 1.6f)
                            .build("cactus_golem"));

    public static final RegistryObject<EntityType<GoldGolemEntity>> GOLD_GOLEM =
            ENTITY_TYPES.register("gold_golem",
                    () -> EntityType.Builder.of(GoldGolemEntity::new, MobCategory.MISC)
                            .sized(1.4f, 2.7f)
                            .build("gold_golem"));

    public static final RegistryObject<EntityType<DartEntity>> DART_PROJECTILE =
            ENTITY_TYPES.register("dart_projectile",
                    () -> EntityType.Builder.<DartEntity>of(DartEntity::new, MobCategory.MISC)
                            .sized(0.5F, 0.5F)
                            .clientTrackingRange(4)
                            .updateInterval(20)
                            .build("dart_projectile"));

    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }
}