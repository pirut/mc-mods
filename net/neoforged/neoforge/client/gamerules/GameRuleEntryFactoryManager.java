package net.neoforged.neoforge.client.gamerules;

import com.google.common.collect.Maps;
import java.util.Map;
import java.util.function.BiConsumer;
import net.minecraft.client.gui.screens.worldselection.EditGameRulesScreen;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRuleType;
import net.neoforged.fml.ModLoader;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
public final class GameRuleEntryFactoryManager {
   private static final Map<GameRuleType, GameRuleEntryFactory<?>> FACTORIES = Maps.newEnumMap(GameRuleType.class);
   private static final GameRuleEntryFactory<?> GENERIC_FACTORY = GenericGameRuleEntry::new;

   public static void register() {
      ModLoader.postEvent(new RegisterGameRuleEntryFactoryEvent(FACTORIES));
   }

   public static <T> void appendGameRuleEntry(
      EditGameRulesScreen screen, GameRule<T> gameRule, BiConsumer<GameRule<T>, EditGameRulesScreen.EntryFactory<T>> addEntry
   ) {
      GameRuleType ruleType = gameRule.gameRuleType();
      if (ruleType != GameRuleType.BOOL && ruleType != GameRuleType.INT) {
         GameRuleEntryFactory<?> factory = FACTORIES.get(gameRule.gameRuleType());
         EditGameRulesScreen.EntryFactory<T> vanillaFactory = (EditGameRulesScreen.EntryFactory<T>)(factory == null ? GENERIC_FACTORY : factory)
            .toVanilla(screen);
         addEntry.accept(gameRule, vanillaFactory);
      }
   }
}
