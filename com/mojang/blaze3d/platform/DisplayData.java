package com.mojang.blaze3d.platform;

import java.util.OptionalInt;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record DisplayData(int width, int height, OptionalInt fullscreenWidth, OptionalInt fullscreenHeight, boolean isFullscreen) {
   public DisplayData withSize(int p_393987_, int p_393854_) {
      return new DisplayData(p_393987_, p_393854_, this.fullscreenWidth, this.fullscreenHeight, this.isFullscreen);
   }

   public DisplayData withFullscreen(boolean p_394284_) {
      return new DisplayData(this.width, this.height, this.fullscreenWidth, this.fullscreenHeight, p_394284_);
   }
}
