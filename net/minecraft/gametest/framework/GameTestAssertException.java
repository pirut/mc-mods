package net.minecraft.gametest.framework;

import net.minecraft.network.chat.Component;

public class GameTestAssertException extends GameTestException {
   protected final Component message;
   protected final int tick;

   public GameTestAssertException(Component p_397070_, int p_397550_) {
      super(p_397070_.getString());
      this.message = p_397070_;
      this.tick = p_397550_;
   }

   @Override
   public Component getDescription() {
      return Component.translatable("test.error.tick", this.message, this.tick);
   }

   @Override
   public String getMessage() {
      return this.getDescription().getString();
   }
}
