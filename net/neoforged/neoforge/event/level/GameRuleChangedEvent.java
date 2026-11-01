package net.neoforged.neoforge.event.level;

import java.util.function.Consumer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRules;
import net.neoforged.bus.api.Event;
import org.jetbrains.annotations.ApiStatus.Internal;

public final class GameRuleChangedEvent extends Event {
   private final MinecraftServer server;
   private final GameRule<?> gameRule;
   private final Object newValue;

   @Internal
   public <T> GameRuleChangedEvent(MinecraftServer server, GameRule<T> gameRule, T newValue) {
      this.server = server;
      this.gameRule = gameRule;
      this.newValue = newValue;
   }

   public MinecraftServer getServer() {
      return this.server;
   }

   public GameRules getGameRules() {
      return this.server.getWorldData().getGameRules();
   }

   public GameRule<?> getGameRule() {
      return this.gameRule;
   }

   public Object getNewValue() {
      return this.newValue;
   }

   public <T> void runIfMatching(GameRule<T> gameRule, Consumer<T> action) {
      if (this.gameRule == gameRule) {
         action.accept((T)this.newValue);
      }
   }
}
