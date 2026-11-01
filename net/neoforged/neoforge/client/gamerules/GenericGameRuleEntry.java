package net.neoforged.neoforge.client.gamerules;

import com.mojang.serialization.DataResult;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.worldselection.EditGameRulesScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.gamerules.GameRule;

public class GenericGameRuleEntry<T> extends GameRuleEntry {
   private final EditBox input;

   public GenericGameRuleEntry(EditGameRulesScreen screen, Component label, List<FormattedCharSequence> tooltip, String str, GameRule<T> gameRule) {
      super(screen.getFont(), tooltip, label);
      this.input = new EditBox(this.font, 10, 5, 44, 20, label.copy().append("\n").append(str).append("\n"));
      this.input.setValue(screen.gameRules.getAsString(gameRule));
      this.input.setResponder(value -> {
         DataResult<T> dataresult = gameRule.deserialize(value);
         if (dataresult.isSuccess()) {
            this.input.setTextColor(-2039584);
            screen.clearInvalid(this);
            screen.gameRules.set(gameRule, (T)dataresult.getOrThrow(), null);
         } else {
            this.input.setTextColor(-65536);
            screen.markInvalid(this);
         }
      });
      this.children.add(this.input);
   }

   @Override
   public void renderContent(GuiGraphics graphics, int mouseX, int mouseY, boolean hovered, float particlTick) {
      this.renderLabel(graphics, this.getContentY(), this.getContentX());
      this.input.setX(this.getContentRight() - 45);
      this.input.setY(this.getContentY());
      this.input.render(graphics, mouseX, mouseY, particlTick);
   }
}
