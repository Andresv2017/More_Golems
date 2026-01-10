package net.darkblade.moregolems.client;

import net.darkblade.moregolems.MoreGolems;
import net.darkblade.moregolems.sever.init.ModItems;
import net.darkblade.moregolems.sever.item.custom.BlowgunItem;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

public class BlowgunHudOverlay {

    private static final ResourceLocation BAR_TEXTURE =
            new ResourceLocation(MoreGolems.MODID, "textures/gui/blowbun_bar.png");

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

            // --- CONFIGURACIÓN DE LA BARRA (¡AJUSTA ESTO A TU IMAGEN!) ---
            // Asumiendo que tu imagen tiene la barra vacía a la izquierda y la llena a la derecha
            int barWidth = 18;   // Ancho de UNA barra en píxeles (la parte negra)
            int barHeight = 5;   // Alto de la barra en píxeles

            // Tamaño total de tu archivo .png (Importante para que no se deforme)
            // Si tu imagen mide por ejemplo 36x5 píxeles, pon 36 y 5 aquí.
            int texWidth = 36;
            int texHeight = 5;

            // Posición en pantalla (x-9 para centrar una barra de ancho 18)
            int renderX = x - (barWidth / 2);
            int renderY = y + 10; // Un poco debajo de la mira

            // 2. Renderizar FONDO (Barra vacía)
            // blit(textura, x, y, u, v, ancho, alto, anchoTotalTex, altoTotalTex)
            // U=0, V=0 -> Empieza al inicio de la imagen
            guiGraphics.blit(BAR_TEXTURE, renderX, renderY, 0, 0, barWidth, barHeight, texWidth, texHeight);

            // 3. Renderizar PROGRESO (Barra llena)
            int filledWidth = (int) (barWidth * progress);

            if (filledWidth > 0) {
                // U = barWidth -> Empieza donde termina la barra vacía (asumiendo que están lado a lado)
                guiGraphics.blit(BAR_TEXTURE, renderX, renderY, barWidth, 0, filledWidth, barHeight, texWidth, texHeight);
            }
        }
    });
}