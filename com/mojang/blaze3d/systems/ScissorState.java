package com.mojang.blaze3d.systems;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ScissorState {
   private boolean enabled;
   private int x;
   private int y;
   private int width;
   private int height;

   public void enable(int p_428240_, int p_428482_, int p_428424_, int p_428391_) {
      this.enabled = true;
      this.x = p_428240_;
      this.y = p_428482_;
      this.width = p_428424_;
      this.height = p_428391_;
   }

   public void disable() {
      this.enabled = false;
   }

   public boolean enabled() {
      return this.enabled;
   }

   public int x() {
      return this.x;
   }

   public int y() {
      return this.y;
   }

   public int width() {
      return this.width;
   }

   public int height() {
      return this.height;
   }
}
