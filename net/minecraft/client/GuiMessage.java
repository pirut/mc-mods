package net.minecraft.client;

import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.ComponentRenderUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public record GuiMessage(int addedTime, Component content, @Nullable MessageSignature signature, @Nullable GuiMessageTag tag) {
   private static final int MESSAGE_TAG_MARGIN_LEFT = 4;

   public List<FormattedCharSequence> splitLines(Font p_457955_, int p_457923_) {
      if (this.tag != null && this.tag.icon() != null) {
         p_457923_ -= this.tag.icon().width + 4 + 2;
      }

      return ComponentRenderUtils.wrapComponents(this.content, p_457923_, p_457955_);
   }

   @OnlyIn(Dist.CLIENT)
   public record Line(int addedTime, FormattedCharSequence content, @Nullable GuiMessageTag tag, boolean endOfEntry) {
      public int getTagIconLeft(Font p_458212_) {
         return p_458212_.width(this.content) + 4;
      }
   }
}
