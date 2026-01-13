package net.darkblade.moregolems.sever.init;

import net.darkblade.moregolems.MoreGolems;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MoreGolems.MODID);

    public static final RegistryObject<CreativeModeTab> MORE_GOLEMS_TAB = CREATIVE_MODE_TABS.register("more_golems_tab",
            () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(ModItems.TAB_ICON.get()))
                    .title(Component.translatable("creativetab.more_golems_tab"))
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.CACTUS_SPINE.get());
                        output.accept(ModItems.CACTUS_GOLEM_SPAWN_EGG.get());
                        output.accept(ModItems.BLOWGUN.get());
                        output.accept(ModItems.DART.get());
                        output.accept(ModItems.GOLD_GOLEM_SPAWN_EGG.get());
                        output.accept(ModItems.CASTLE_GOLEM_SPAWN_EGG.get());
                    })
                    .build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}