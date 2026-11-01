package net.neoforged.neoforge.client.gamerules;

import com.google.common.collect.Lists;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.worldselection.EditGameRulesScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.jspecify.annotations.Nullable;

public abstract class GameRuleEntry extends EditGameRulesScreen.RuleEntry {
   private final List<FormattedCharSequence> label;
   protected final List<AbstractWidget> children = Lists.newArrayList();
   protected final Font font;

   public GameRuleEntry(Font font, @Nullable List<FormattedCharSequence> tooltip, Component label) {
      super(tooltip);
      this.font = font;
      this.label = font.split(label, 175);
   }

   @Override
   public List<? extends GuiEventListener> children() {
      return this.children;
   }

   @Override
   public List<? extends NarratableEntry> narratables() {
      return this.children;
   }

   protected void renderLabel(GuiGraphics graphics, int y, int x) {
      if (this.label.size() == 1) {
         graphics.drawString(this.font, this.label.getFirst(), x, y + 5, -1);
      } else if (this.label.size() >= 2) {
         graphics.drawString(this.font, this.label.getFirst(), x, y, -1);
         graphics.drawString(this.font, this.label.get(1), x, y + 10, -1);
      }
   }
}
