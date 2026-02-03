package net.darkblade.moregolems.sever.item.custom;

import net.darkblade.moregolems.sever.init.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class SolarisSwordItem extends SwordItem {

    // Thresholds adaptados a golpes.
    // Con {1, 2, 3, 4}, subirá de nivel prácticamente con cada golpe consecutivo.
    // Si quieres que cueste más golpes, aumenta estos números (ej: {3, 6, 9, 12}).
    private static final int[] HIT_THRESHOLDS = {1, 2, 3, 4};

    public SolarisSwordItem(Tier tier, int attackDamage, float attackSpeed, Properties properties) {
        super(tier, attackDamage, attackSpeed, properties);
    }

    public int getHits(ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTag();
        return tag.getInt("SolarisHits");
    }

    public int getLevel(ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTag();
        return tag.getInt("SolarisLevel");
    }

    // --- CAMBIO PRINCIPAL: Detectar golpe en lugar de muerte ---
    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        // Llamamos a super para que haga el daño normal y reduzca durabilidad
        boolean result = super.hurtEnemy(stack, target, attacker);

        // Si estamos en el lado del servidor, procesamos el golpe para la evolución
        if (!attacker.level().isClientSide) {
            addHit(stack, attacker);
        }

        return result;
    }

    public void addHit(ItemStack stack, LivingEntity owner) {
        CompoundTag tag = stack.getOrCreateTag();
        int currentHits = tag.getInt("SolarisHits");
        int currentLevel = tag.getInt("SolarisLevel");

        // Caso: Descarga de energía (Reset) al golpear estando al máximo
        if (currentLevel == 4) {
            tag.putInt("SolarisHits", 0);
            tag.putInt("SolarisLevel", 0);

            // SONIDO DE TRANSFORMACIÓN (Descarga)
            playSound(owner);
            return;
        }

        int newHits = currentHits + 1;
        tag.putInt("SolarisHits", newHits);

        // Caso: Subida de Nivel
        if (currentLevel < 4) {
            // Verificamos si los golpes actuales alcanzan el requisito del nivel actual
            if (newHits >= HIT_THRESHOLDS[currentLevel]) {
                tag.putInt("SolarisLevel", currentLevel + 1);

                // SONIDO DE TRANSFORMACIÓN (Level Up)
                playSound(owner);
            }
        }
    }

    private void playSound(LivingEntity owner) {
        if (!owner.level().isClientSide) {
            owner.level().playSound(null, owner.getX(), owner.getY(), owner.getZ(),
                    ModSounds.GOLD_SWORD_TRANSFORM.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> components, TooltipFlag flag) {
        int hits = getHits(stack);
        int internalLevel = getLevel(stack);
        int displayStage = internalLevel + 1;

        if (internalLevel == 4) {
            components.add(Component.literal("STAGE: MAXIMUM OVERCHARGE").withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
            components.add(Component.literal("Next strike will discharge energy!").withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC));
            components.add(Component.literal("Solar Damage: +8.0 & Fire").withStyle(ChatFormatting.GOLD));
        }
        else {
            int nextGoal = HIT_THRESHOLDS[internalLevel];
            components.add(Component.literal("Stage: " + displayStage + "/5").withStyle(ChatFormatting.YELLOW));
            // Cambiado "Kills" por "Hits"
            components.add(Component.literal("Hits: " + hits + "/" + nextGoal).withStyle(ChatFormatting.GRAY));

            float bonus = internalLevel * 2.0f;
            if (bonus > 0) {
                components.add(Component.literal("Solar Bonus: +" + (int)bonus).withStyle(ChatFormatting.GOLD));
            }
        }

        super.appendHoverText(stack, level, components, flag);
    }
}