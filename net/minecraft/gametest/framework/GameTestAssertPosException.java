package net.minecraft.gametest.framework;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

public class GameTestAssertPosException extends GameTestAssertException {
   private final BlockPos absolutePos;
   private final BlockPos relativePos;

   public GameTestAssertPosException(Component p_397040_, BlockPos p_177052_, BlockPos p_177053_, int p_397616_) {
      super(p_397040_, p_397616_);
      this.absolutePos = p_177052_;
      this.relativePos = p_177053_;
   }

   @Override
   public Component getDescription() {
      return Component.translatable(
         "test.error.position",
         this.message,
         this.absolutePos.getX(),
         this.absolutePos.getY(),
         this.absolutePos.getZ(),
         this.relativePos.getX(),
         this.relativePos.getY(),
         this.relativePos.getZ(),
         this.tick
      );
   }

   public Component getMessageToShowAtBlock() {
      return this.message;
   }

   @Nullable
   public BlockPos getRelativePos() {
      return this.relativePos;
   }

   @Nullable
   public BlockPos getAbsolutePos() {
      return this.absolutePos;
   }
}
