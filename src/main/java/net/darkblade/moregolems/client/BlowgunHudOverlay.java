package net.darkblade.moregolems.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.darkblade.moregolems.sever.init.ModItems;
import net.darkblade.moregolems.sever.item.custom.BlowgunItem;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

public class BlowgunHudOverlay {

    public static final IGuiOverlay HUD_BLOWGUN = ((gui, guiGraphics, partialTick, screenWidth, screenHeight) -> {
        int x = screenWidth / 2;
        int y = screenHeight / 2;

        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;

        if (player != null && player.isUsingItem() && player.getUseItem().is(ModItems.BLOWGUN.get())) {

            // 1. Calcular progreso (0.0 a 1.0)
            int useTicks = player.getTicksUsingItem();
            float progress = (float) useTicks / (float) BlowgunItem.MAX_DRAW_DURATION;
            if (progress > 1.0F) progress = 1.0F;

            // Coordenadas donde va el icono (x-8, y+9 es la posición estándar bajo la mira)
            int iconX = x - 8;
            int iconY = y + 7;
            int iconSize = 16;

            // Calculamos la altura de la parte "llena" (de 0 a 16 píxeles)
            int filledHeight = (int) (iconSize * progress);

            if (filledHeight > 0) {
                // 2. ACTIVAR TIJERAS (SCISSOR)
                // Definimos un rectángulo que empieza abajo y crece hacia arriba.
                // Todo lo que se dibuje fuera de este rectángulo será invisible.
                // Coordenadas: (minX, minY, maxX, maxY)

                // Nota: minY se calcula como "PosiciónY + (TamañoTotal - AlturaLlenada)"
                // Esto hace que el recorte baje desde arriba, revelando el item de abajo hacia arriba.
                guiGraphics.enableScissor(iconX, iconY + (iconSize - filledHeight), iconX + iconSize, iconY + iconSize);

                // 3. RENDERIZAR EL ÍTEM
                // El juego intenta dibujar el ítem completo, pero las tijeras
                // cortarán la parte superior que aún no se ha cargado.
                ItemStack spriteStack = new ItemStack(ModItems.BLOWGUN_SPRITE.get());
                guiGraphics.renderItem(spriteStack, iconX, iconY);

                // 4. DESACTIVAR TIJERAS
                guiGraphics.disableScissor();
            }
        }
    });
}