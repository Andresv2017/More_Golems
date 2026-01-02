package net.darkblade.moregolems;
import com.mojang.logging.LogUtils;
import net.darkblade.moregolems.client.renderer.CactusGolemRenderer;
import net.darkblade.moregolems.client.renderer.DartRenderer;
import net.darkblade.moregolems.constans.MGConstans;
import net.darkblade.moregolems.sever.entity.custom.CactusGolemEntity;
import net.darkblade.moregolems.sever.init.ModCreativeTabs;
import net.darkblade.moregolems.sever.init.ModEntities;
import net.darkblade.moregolems.sever.init.ModItems;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import software.bernie.geckolib.core.molang.LazyVariable;
import software.bernie.geckolib.core.molang.MolangParser;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(MoreGolems.MODID)
public class MoreGolems
{

    public static final String MODID = "moregolems";

    private static final Logger LOGGER = LogUtils.getLogger();

    public MoreGolems()
    {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        modEventBus.addListener(this::commonSetup);

        MinecraftForge.EVENT_BUS.register(this);
        ModEntities.register(modEventBus);
        ModItems.register(modEventBus);
        ModCreativeTabs.register(modEventBus);

        MolangParser.INSTANCE.register(new LazyVariable(MGConstans.HEAD_X_QUERY, 0));
        MolangParser.INSTANCE.register(new LazyVariable(MGConstans.HEAD_Y_QUERY, 0));

        modEventBus.addListener(this::addCreative);

    }

    private void commonSetup(final FMLCommonSetupEvent event)
    {

    }

    // Add the example block item to the building blocks tab
    private void addCreative(BuildCreativeModeTabContentsEvent event)
    {

    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event)
    {

    }

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ModEventBusEvents {
        @SubscribeEvent
        public static void registerAttributes(EntityAttributeCreationEvent event) {
            event.put(ModEntities.CACTUS_GOLEM.get(), CactusGolemEntity.setAttributes().build());
        }
    }

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            EntityRenderers.register(ModEntities.CACTUS_GOLEM.get(), CactusGolemRenderer::new);
            EntityRenderers.register(ModEntities.DART_PROJECTILE.get(), DartRenderer::new);

            event.enqueueWork(() -> {
                ItemProperties.register(ModItems.BLOWGUN.get(), new ResourceLocation("pulling"),
                        (stack, level, entity, seed) -> entity != null && entity.isUsingItem() && entity.getUseItem() == stack ? 1.0F : 0.0F);

                ItemProperties.register(ModItems.BLOWGUN.get(), new ResourceLocation("held"),
                        (stack, level, entity, seed) -> entity != null ? 1.0F : 0.0F);
            });
        }
    }
}
