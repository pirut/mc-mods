package net.minecraft.client.gui.contextualbar;

import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface ContextualBarRenderer {
   int WIDTH = 182;
   int HEIGHT = 5;
   int MARGIN_BOTTOM = 24;
   ContextualBarRenderer EMPTY = new ContextualBarRenderer() {
      @Override
      public void renderBackground(GuiGraphics p_416316_, DeltaTracker p_416270_) {
      }

      @Override
      public void render(GuiGraphics p_416536_, DeltaTracker p_415672_) {
      }
   };

   default int left(Window p_415941_) {
      return (p_415941_.getGuiScaledWidth() - 182) / 2;
   }

   default int top(Window p_416466_) {
      return p_416466_.getGuiScaledHeight() - 24 - 5;
   }

   void renderBackground(GuiGraphics var1, DeltaTracker var2);

   void render(GuiGraphics var1, DeltaTracker var2);

   static void renderExperienceLevel(GuiGraphics p_416259_, Font p_415884_, int p_415715_) {
      Component component = Component.translatable("gui.experience.level", p_415715_);
      int i = (p_416259_.guiWidth() - p_415884_.width(component)) / 2;
      int j = p_416259_.guiHeight() - 24 - 9 - 2;
      p_416259_.drawString(p_415884_, component, i + 1, j, -16777216, false);
      p_416259_.drawString(p_415884_, component, i - 1, j, -16777216, false);
      p_416259_.drawString(p_415884_, component, i, j + 1, -16777216, false);
      p_416259_.drawString(p_415884_, component, i, j - 1, -16777216, false);
      p_416259_.drawString(p_415884_, component, i, j, -8323296, false);
   }
}
