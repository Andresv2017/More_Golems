package net.darkblade.moregolems.sever.init;

import net.darkblade.moregolems.MoreGolems;
import net.darkblade.moregolems.sever.item.custom.BlowgunItem;
import net.darkblade.moregolems.sever.item.custom.ConcreteShieldItem;
import net.darkblade.moregolems.sever.item.custom.DartItem;
import net.darkblade.moregolems.sever.item.custom.SolarisSwordItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Tiers;
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

    public static final RegistryObject<Item> GOLD_GOLEM_SPAWN_EGG = ITEMS.register("gold_golem_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.GOLD_GOLEM, 0xFDF55F, 0xEA9E32,
                    new Item.Properties()));

    public static final RegistryObject<Item> CASTLE_GOLEM_SPAWN_EGG = ITEMS.register("castle_golem_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.CASTLE_GOLEM, 0x9b5643, 0xe6ddb4,
                    new Item.Properties()));

    public static final RegistryObject<Item> BLOWGUN_SPRITE = ITEMS.register("blowgun_sprite",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> DART= ITEMS.register("dart",
            () -> new DartItem(new Item.Properties()));

    public static final RegistryObject<Item> BLOWGUN = ITEMS.register("blowgun",
            () -> new BlowgunItem(new Item.Properties().stacksTo(1).durability(384)));

    public static final RegistryObject<Item> SOLARIS_SWORD = ITEMS.register("solaris_sword",
            () -> new SolarisSwordItem(Tiers.DIAMOND, 3, -2.4f, new Item.Properties()));

    public static final RegistryObject<Item> CONCRETE_SHIELD = ITEMS.register("concrete_shield",
            () -> new ConcreteShieldItem(new Item.Properties().durability(600)));

    public static final RegistryObject<Item> TAB_ICON = ITEMS.register("tab_icon",
            () -> new Item(new Item.Properties()));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}