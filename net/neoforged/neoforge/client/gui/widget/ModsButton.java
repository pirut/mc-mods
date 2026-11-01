package net.neoforged.neoforge.client.gui.widget;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.neoforged.fml.VersionChecker.Status;
import net.neoforged.fml.loading.FMLConfig;
import net.neoforged.fml.loading.FMLConfig.ConfigValue;
import net.neoforged.neoforge.client.loading.ClientModLoader;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jspecify.annotations.Nullable;

@Internal
public class ModsButton extends Button.Plain {
   private static final Identifier VERSION_CHECK_ICONS = Identifier.fromNamespaceAndPath("neoforge", "textures/gui/version_check_icons.png");
   @Nullable
   private Status showNotification;
   private boolean hasCheckedForUpdates = false;

   public ModsButton(Button.Builder builder) {
      super(builder);
   }

   @Override
   protected void renderContents(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
      super.renderContents(guiGraphics, mouseX, mouseY, partialTick);
      if (!this.hasCheckedForUpdates) {
         this.showNotification = ClientModLoader.checkForUpdates();
         this.hasCheckedForUpdates = true;
      }

      if (this.showNotification != null && this.showNotification.shouldDraw() && FMLConfig.getBoolConfigValue(ConfigValue.VERSION_CHECK)) {
         int x = this.getX();
         int y = this.getY();
         int w = this.getWidth();
         int h = this.getHeight();
         guiGraphics.blit(
            RenderPipelines.GUI_TEXTURED,
            VERSION_CHECK_ICONS,
            x + w - (h / 2 + 4),
            y + (h / 2 - 4),
            this.showNotification.getSheetOffset() * 8,
            this.showNotification.isAnimated() && (System.currentTimeMillis() / 800L & 1L) == 1L ? 8.0F : 0.0F,
            8,
            8,
            64,
            16
         );
      }
   }
}
