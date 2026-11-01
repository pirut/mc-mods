package net.neoforged.neoforge.client.gamerules;

import java.util.List;
import net.minecraft.client.gui.screens.worldselection.EditGameRulesScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.gamerules.GameRule;

@FunctionalInterface
public interface GameRuleEntryFactory<T> {
   EditGameRulesScreen.RuleEntry create(EditGameRulesScreen var1, Component var2, List<FormattedCharSequence> var3, String var4, GameRule<T> var5);

   default EditGameRulesScreen.EntryFactory<T> toVanilla(EditGameRulesScreen screen) {
      return (label, tooltip, str, gameRule) -> this.create(screen, label, tooltip, str, gameRule);
   }
}
