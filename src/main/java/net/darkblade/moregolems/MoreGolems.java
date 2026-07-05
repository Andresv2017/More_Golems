package net.darkblade.moregolems;
import com.mojang.logging.LogUtils;
import net.darkblade.moregolems.client.model.BlowgunModel;
import net.darkblade.moregolems.client.particle.ShieldRingParticle;
import net.darkblade.moregolems.client.particle.ShineParticle;
import net.darkblade.moregolems.client.renderer.BlackstoneGolemRenderer;
import net.darkblade.moregolems.client.renderer.CactusGolemRenderer;
import net.darkblade.moregolems.client.renderer.CastleGolemRenderer;
import net.darkblade.moregolems.client.renderer.DartRenderer;
import net.darkblade.moregolems.client.renderer.GoldGolemRenderer;
import net.darkblade.moregolems.client.renderer.SlimeGolemRenderer;
import net.darkblade.moregolems.constans.MGConstans;
import net.darkblade.moregolems.sever.entity.custom.BlackstoneGolemEntity;
import net.darkblade.moregolems.sever.entity.custom.CactusGolemEntity;
import net.darkblade.moregolems.sever.entity.custom.CastleGolemEntity;
import net.darkblade.moregolems.sever.entity.custom.GoldGolemEntity;
import net.darkblade.moregolems.sever.entity.custom.SlimeGolemEntity;
import net.darkblade.moregolems.sever.init.*;
import net.darkblade.moregolems.sever.item.custom.SolarisSwordItem;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
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
        ModSounds.register(modEventBus);
        ModRecipes.register(modEventBus);
        ModCreativeTabs.register(modEventBus);
        ModParticles.register(modEventBus);

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
            event.put(ModEntities.GOLD_GOLEM.get(), GoldGolemEntity.setAttributes().build());
            event.put(ModEntities.CASTLE_GOLEM.get(), CastleGolemEntity.setAttributes().build());
            event.put(ModEntities.SLIME_GOLEM.get(), SlimeGolemEntity.setAttributes().build());
            event.put(ModEntities.BLACKSTONE_GOLEM.get(), BlackstoneGolemEntity.setAttributes().build());
        }
    }

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            EntityRenderers.register(ModEntities.CACTUS_GOLEM.get(), CactusGolemRenderer::new);
            EntityRenderers.register(ModEntities.GOLD_GOLEM.get(), GoldGolemRenderer::new);
            EntityRenderers.register(ModEntities.DART_PROJECTILE.get(), DartRenderer::new);
            EntityRenderers.register(ModEntities.CASTLE_GOLEM.get(), CastleGolemRenderer::new);
            EntityRenderers.register(ModEntities.SLIME_GOLEM.get(), SlimeGolemRenderer::new);
            EntityRenderers.register(ModEntities.BLACKSTONE_GOLEM.get(), BlackstoneGolemRenderer::new);

            event.enqueueWork(() -> {
                ItemProperties.register(ModItems.BLOWGUN.get(), new ResourceLocation("pulling"),
                        (stack, level, entity, seed) ->
                                entity != null && entity.isUsingItem() && entity.getUseItem() == stack ? 1.0F : 0.0F);

                ItemProperties.register(ModItems.BLOWGUN.get(), new ResourceLocation("held"),
                        (stack, level, entity, seed) -> {
                            if (entity == null) return 0.0F;
                            boolean mainHand = entity.getMainHandItem() == stack;
                            boolean offHand = entity.getOffhandItem() == stack;
                            return (mainHand || offHand) ? 1.0F : 0.0F;
                        });

                ItemProperties.register(ModItems.SOLARIS_SWORD.get(), new ResourceLocation(MoreGolems.MODID, "stage"),
                        (stack, level, entity, seed) -> {
                            if (stack.getItem() instanceof SolarisSwordItem item) {
                                return (float) item.getLevel(stack);
                            }
                            return 0.0f;
                        });

                ItemProperties.register(ModItems.CONCRETE_SHIELD.get(), new ResourceLocation("blocking"),
                        (stack, level, entity, seed) ->
                                entity != null && entity.isUsingItem() && entity.getUseItem() == stack ? 1.0F : 0.0F);
            });
        }

        @SubscribeEvent
        public static void registerParticleProviders(RegisterParticleProvidersEvent event) {
            event.registerSpriteSet(ModParticles.SHINE.get(), ShineParticle.Provider::new);
            event.registerSpriteSet(ModParticles.SHIELD_RING.get(), ShieldRingParticle.Provider::new);
        }

        @SubscribeEvent
        public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
            event.registerLayerDefinition(BlowgunModel.LAYER_LOCATION, BlowgunModel::createLayer);
        }
    }
}
