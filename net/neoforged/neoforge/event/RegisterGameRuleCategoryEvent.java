package net.neoforged.neoforge.event;

import java.util.function.Consumer;
import net.minecraft.world.level.gamerules.GameRuleCategory;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;
import org.jetbrains.annotations.ApiStatus.Internal;

public final class RegisterGameRuleCategoryEvent extends Event implements IModBusEvent {
   private final Consumer<GameRuleCategory> registrar;

   @Internal
   public RegisterGameRuleCategoryEvent(Consumer<GameRuleCategory> registrar) {
      this.registrar = registrar;
   }

   public void register(GameRuleCategory category) {
      this.registrar.accept(category);
   }
}
