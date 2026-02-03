package net.darkblade.moregolems.sever.recipe;

import net.darkblade.moregolems.sever.init.ModItems;
import net.darkblade.moregolems.sever.init.ModRecipes;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public class TippedDartRecipe extends CustomRecipe {

    public TippedDartRecipe(ResourceLocation id, CraftingBookCategory category) {
        super(id, category);
    }

    @Override
    public boolean matches(CraftingContainer pContainer, Level pLevel) {
        if (pContainer.getWidth() == 3 && pContainer.getHeight() == 3) {
            for(int i = 0; i < pContainer.getWidth(); ++i) {
                for(int j = 0; j < pContainer.getHeight(); ++j) {
                    ItemStack itemstack = pContainer.getItem(i + j * pContainer.getWidth());

                    if (itemstack.isEmpty()) {
                        return false;
                    }

                    if (i == 1 && j == 1) {
                        if (!itemstack.is(Items.POTION)) {
                            return false;
                        }
                    } else {
                        if (!itemstack.is(ModItems.DART.get())) {
                            return false;
                        }
                    }
                }
            }
            return true;
        } else {
            return false;
        }
    }

    @Override
    public ItemStack assemble(CraftingContainer pContainer, RegistryAccess pRegistryAccess) {
        ItemStack potionStack = pContainer.getItem(1 + pContainer.getWidth());

        if (!potionStack.is(Items.POTION)) {
            return ItemStack.EMPTY;
        } else {
            ItemStack resultStack = new ItemStack(ModItems.DART.get(), 8);

            PotionUtils.setPotion(resultStack, PotionUtils.getPotion(potionStack));
            PotionUtils.setCustomEffects(resultStack, PotionUtils.getCustomEffects(potionStack));

            return resultStack;
        }
    }

    @Override
    public boolean canCraftInDimensions(int pWidth, int pHeight) {
        return pWidth >= 2 && pHeight >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.TIPPED_DART_SERIALIZER.get();
    }
}