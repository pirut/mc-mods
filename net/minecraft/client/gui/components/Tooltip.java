package net.minecraft.client.gui.components;

import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.narration.NarrationSupplier;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class Tooltip implements NarrationSupplier {
   private static final int MAX_WIDTH = 170;
   private final Component message;
   @Nullable
   private List<FormattedCharSequence> cachedTooltip;
   @Nullable
   private Language splitWithLanguage;
   @Nullable
   private final Component narration;

   private Tooltip(Component message, @Nullable Component narration) {
      this.message = message;
      this.narration = narration;
   }

   public static Tooltip create(Component message, @Nullable Component narration) {
      return new Tooltip(message, narration);
   }

   public static Tooltip create(Component message) {
      return new Tooltip(message, message);
   }

   @Override
   public void updateNarration(NarrationElementOutput narrationElementOutput) {
      if (this.narration != null) {
         narrationElementOutput.add(NarratedElementType.HINT, this.narration);
      }
   }

   public List<FormattedCharSequence> toCharSequence(Minecraft minecraft) {
      Language language = Language.getInstance();
      if (this.cachedTooltip == null || language != this.splitWithLanguage) {
         this.cachedTooltip = splitTooltip(minecraft, this.message);
         this.splitWithLanguage = language;
      }

      return this.cachedTooltip;
   }

   public static List<FormattedCharSequence> splitTooltip(Minecraft minecraft, Component message) {
      return minecraft.font.split(message, 170);
   }
}
