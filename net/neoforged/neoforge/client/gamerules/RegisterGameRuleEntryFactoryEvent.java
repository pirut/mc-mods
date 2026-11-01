package net.neoforged.neoforge.client.gamerules;

import java.util.Map;
import net.minecraft.world.level.gamerules.GameRuleType;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;
import org.jetbrains.annotations.ApiStatus.Internal;

public final class RegisterGameRuleEntryFactoryEvent extends Event implements IModBusEvent {
   private final Map<GameRuleType, GameRuleEntryFactory<?>> factories;

   @Internal
   RegisterGameRuleEntryFactoryEvent(Map<GameRuleType, GameRuleEntryFactory<?>> factories) {
      this.factories = factories;
   }

   public <T> void register(GameRuleType gameRuleType, GameRuleEntryFactory<T> factory) {
      if (gameRuleType == GameRuleType.INT || gameRuleType == GameRuleType.BOOL) {
         throw new IllegalStateException("Registering custom entry factory for vanilla GameRuleTypes is disallowed");
      } else if (this.factories.putIfAbsent(gameRuleType, factory) != null) {
         throw new IllegalStateException("Duplicate GameRuleTypeEntryFactory registration!");
      }
   }
}
