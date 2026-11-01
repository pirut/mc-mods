package net.minecraft.client.gui.screens.inventory;

import com.google.common.collect.Ordering;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;

@Environment(EnvType.CLIENT)
public class EffectsInInventory {
   private static final Identifier EFFECT_BACKGROUND_SPRITE = Identifier.withDefaultNamespace("container/inventory/effect_background");
   private static final Identifier EFFECT_BACKGROUND_AMBIENT_SPRITE = Identifier.withDefaultNamespace("container/inventory/effect_background_ambient");
   private static final int ICON_SIZE = 18;
   public static final int SPACING = 7;
   private static final int TEXT_X_OFFSET = 32;
   public static final int SPRITE_SQUARE_SIZE = 32;
   private final AbstractContainerScreen<?> screen;
   private final Minecraft minecraft;

   public EffectsInInventory(AbstractContainerScreen<?> screen) {
      this.screen = screen;
      this.minecraft = Minecraft.getInstance();
   }

   public boolean canSeeEffects() {
      int i = this.screen.leftPos + this.screen.imageWidth + 2;
      int j = this.screen.width - i;
      return j >= 32;
   }

   public void render(GuiGraphics guiGraphics, int mouseX, int mouseY) {
      int i = this.screen.leftPos + this.screen.imageWidth + 2;
      int j = this.screen.width - i;
      Collection<MobEffectInstance> collection = this.minecraft.player.getActiveEffects();
      if (!collection.isEmpty() && j >= 32) {
         int k = j >= 120 ? j - 7 : 32;
         int l = 33;
         if (collection.size() > 5) {
            l = 132 / (collection.size() - 1);
         }

         this.renderEffects(guiGraphics, collection, i, l, mouseX, mouseY, k);
      }
   }

   private void renderEffects(GuiGraphics guiGraphics, Collection<MobEffectInstance> effects, int x, int y, int mouseX, int mouseY, int minWidth) {
      Iterable<MobEffectInstance> iterable = Ordering.natural().sortedCopy(effects);
      int i = this.screen.topPos;
      Font font = this.screen.getFont();

      for (MobEffectInstance mobEffectInstance : iterable) {
         boolean bl = mobEffectInstance.isAmbient();
         Component component = this.getEffectName(mobEffectInstance);
         Component component2 = MobEffectUtil.formatDuration(mobEffectInstance, 1.0F, this.minecraft.level.tickRateManager().tickrate());
         int j = this.renderBackground(guiGraphics, font, component, component2, x, i, bl, minWidth);
         this.renderText(guiGraphics, component, component2, font, x, i, j, y, mouseX, mouseY);
         guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, Gui.getMobEffectSprite(mobEffectInstance.getEffect()), x + 7, i + 7, 18, 18);
         i += y;
      }
   }

   private int renderBackground(GuiGraphics guiGraphics, Font font, Component text, Component duration, int x, int y, boolean ambient, int minWidth) {
      int i = 32 + font.width(text) + 7;
      int j = 32 + font.width(duration) + 7;
      int k = Math.min(minWidth, Math.max(i, j));
      guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, ambient ? EFFECT_BACKGROUND_AMBIENT_SPRITE : EFFECT_BACKGROUND_SPRITE, x, y, k, 32);
      return k;
   }

   private void renderText(GuiGraphics guiGraphics, Component effect, Component name, Font font, int x, int y, int width, int height, int mouseX, int mouseY) {
      int i = x + 32;
      int j = y + 7;
      int k = width - 32 - 7;
      boolean bl2;
      if (k > 0) {
         boolean bl = font.width(effect) > k;
         FormattedCharSequence formattedCharSequence = bl ? StringWidget.clipText(effect, font, k) : effect.getVisualOrderText();
         guiGraphics.drawString(font, formattedCharSequence, i, j, -1);
         guiGraphics.drawString(font, name, i, j + 9, -8355712);
         bl2 = bl;
      } else {
         bl2 = true;
      }

      if (bl2 && mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height) {
         guiGraphics.setTooltipForNextFrame(this.screen.getFont(), List.of(effect, name), Optional.empty(), mouseX, mouseY);
      }
   }

   private Component getEffectName(MobEffectInstance effect) {
      MutableComponent mutableComponent = effect.getEffect().value().getDisplayName().copy();
      if (effect.getAmplifier() >= 1 && effect.getAmplifier() <= 9) {
         mutableComponent.append(CommonComponents.SPACE).append(Component.translatable("enchantment.level." + (effect.getAmplifier() + 1)));
      }

      return mutableComponent;
   }
}
