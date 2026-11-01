package net.minecraft.gametest.framework;

import net.minecraft.network.chat.Component;

public class UnknownGameTestException extends GameTestException {
   private final Throwable reason;

   public UnknownGameTestException(Throwable p_401384_) {
      super(p_401384_.getMessage());
      this.reason = p_401384_;
   }

   @Override
   public Component getDescription() {
      return Component.translatable("test.error.unknown", this.reason.getMessage());
   }
}
