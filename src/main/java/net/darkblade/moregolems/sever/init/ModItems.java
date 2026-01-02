package net.darkblade.moregolems.sever.init;

import net.darkblade.moregolems.MoreGolems;
import net.darkblade.moregolems.sever.item.custom.BlowgunItem;
import net.darkblade.moregolems.sever.item.custom.DartItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, MoreGolems.MODID);

    public static final RegistryObject<Item> CACTUS_SPINE = ITEMS.register("cactus_spine",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> CACTUS_GOLEM_SPAWN_EGG = ITEMS.register("cactus_golem_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.CACTUS_GOLEM, 0X346124, 0X1D3413,
                    new Item.Properties()));

    public static final RegistryObject<Item> DART = ITEMS.register("dart",
            () -> new DartItem(new Item.Properties()));

    public static final RegistryObject<Item> BLOWGUN = ITEMS.register("blowgun",
            () -> new BlowgunItem(new Item.Properties().stacksTo(1).durability(384)));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}