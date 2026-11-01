package net.minecraft.gametest.framework;

import net.minecraft.network.chat.Component;

public abstract class GameTestException extends RuntimeException {
   public GameTestException(String p_401300_) {
      super(p_401300_);
   }

   public abstract Component getDescription();
}
