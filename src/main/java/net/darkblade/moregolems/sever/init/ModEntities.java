package net.darkblade.moregolems.sever.init;

import net.darkblade.moregolems.MoreGolems;
import net.darkblade.moregolems.sever.entity.custom.CactusGolemEntity;
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

    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }
}