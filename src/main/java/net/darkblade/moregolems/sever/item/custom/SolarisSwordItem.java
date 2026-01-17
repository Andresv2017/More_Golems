package net.darkblade.moregolems.sever.item.custom;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class SolarisSwordItem extends SwordItem {

    private static final int[] KILL_THRESHOLDS = {1, 2, 3, 4};

    public SolarisSwordItem(Tier tier, int attackDamage, float attackSpeed, Properties properties) {
        super(tier, attackDamage, attackSpeed, properties);
    }

    public int getKills(ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTag();
        return tag.getInt("SolarisKills");
    }

    public int getLevel(ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTag();
        return tag.getInt("SolarisLevel");
    }

    public void addKill(ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTag();
        int currentKills = tag.getInt("SolarisKills");
        int currentLevel = tag.getInt("SolarisLevel");

        if (currentLevel == 4) {
            tag.putInt("SolarisKills", 0);
            tag.putInt("SolarisLevel", 0);
            return;
        }

        int newKills = currentKills + 1;
        tag.putInt("SolarisKills", newKills);

        if (currentLevel < 4) {
            if (newKills >= KILL_THRESHOLDS[currentLevel]) {
                tag.putInt("SolarisLevel", currentLevel + 1);
            }
        }
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return getLevel(stack) == 4 || super.isFoil(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> components, TooltipFlag flag) {
        int kills = getKills(stack);
        int internalLevel = getLevel(stack);
        int displayStage = internalLevel + 1;

        if (internalLevel == 4) {
            components.add(Component.literal("STAGE: MAXIMUM OVERCHARGE").withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
            components.add(Component.literal("Next kill will discharge energy!").withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC));
            components.add(Component.literal("Solar Damage: +8.0 & Fire").withStyle(ChatFormatting.GOLD));
        }
        else {
            int nextGoal = KILL_THRESHOLDS[internalLevel];
            components.add(Component.literal("Stage: " + displayStage + "/5").withStyle(ChatFormatting.YELLOW));
            components.add(Component.literal("Kills: " + kills + "/" + nextGoal).withStyle(ChatFormatting.GRAY));

            float bonus = internalLevel * 2.0f;
            if (bonus > 0) {
                components.add(Component.literal("Solar Bonus: +" + (int)bonus).withStyle(ChatFormatting.GOLD));
            }
        }

        super.appendHoverText(stack, level, components, flag);
    }
}