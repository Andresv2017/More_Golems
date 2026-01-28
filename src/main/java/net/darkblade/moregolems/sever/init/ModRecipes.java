package net.darkblade.moregolems.sever.init;

import net.darkblade.moregolems.MoreGolems;
import net.darkblade.moregolems.sever.recipe.TippedDartRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModRecipes {
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, MoreGolems.MODID);

    public static final RegistryObject<RecipeSerializer<TippedDartRecipe>> TIPPED_DART_SERIALIZER =
            SERIALIZERS.register("tipped_dart", () -> new SimpleCraftingRecipeSerializer<>(TippedDartRecipe::new));

    public static void register(IEventBus eventBus) {
        SERIALIZERS.register(eventBus);
    }
}