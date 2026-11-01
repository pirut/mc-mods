package net.minecraft.gametest.framework;

import net.minecraft.network.chat.Component;

public class GameTestTimeoutException extends GameTestException {
   protected final Component message;

   public GameTestTimeoutException(Component p_401095_) {
      super(p_401095_.getString());
      this.message = p_401095_;
   }

   @Override
   public Component getDescription() {
      return this.message;
   }
}
