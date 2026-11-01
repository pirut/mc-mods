package net.minecraft.client.renderer;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class Rect2i {
   private int xPos;
   private int yPos;
   private int width;
   private int height;

   public Rect2i(int p_110081_, int p_110082_, int p_110083_, int p_110084_) {
      this.xPos = p_110081_;
      this.yPos = p_110082_;
      this.width = p_110083_;
      this.height = p_110084_;
   }

   public Rect2i intersect(Rect2i p_173053_) {
      int $$1 = this.xPos;
      int $$2 = this.yPos;
      int $$3 = this.xPos + this.width;
      int $$4 = this.yPos + this.height;
      int $$5 = p_173053_.getX();
      int $$6 = p_173053_.getY();
      int $$7 = $$5 + p_173053_.getWidth();
      int $$8 = $$6 + p_173053_.getHeight();
      this.xPos = Math.max($$1, $$5);
      this.yPos = Math.max($$2, $$6);
      this.width = Math.max(0, Math.min($$3, $$7) - this.xPos);
      this.height = Math.max(0, Math.min($$4, $$8) - this.yPos);
      return this;
   }

   public int getX() {
      return this.xPos;
   }

   public int getY() {
      return this.yPos;
   }

   public void setX(int p_173048_) {
      this.xPos = p_173048_;
   }

   public void setY(int p_173055_) {
      this.yPos = p_173055_;
   }

   public int getWidth() {
      return this.width;
   }

   public int getHeight() {
      return this.height;
   }

   public void setWidth(int p_173057_) {
      this.width = p_173057_;
   }

   public void setHeight(int p_173059_) {
      this.height = p_173059_;
   }

   public void setPosition(int p_173050_, int p_173051_) {
      this.xPos = p_173050_;
      this.yPos = p_173051_;
   }

   public boolean contains(int p_110088_, int p_110089_) {
      return p_110088_ >= this.xPos && p_110088_ <= this.xPos + this.width && p_110089_ >= this.yPos && p_110089_ <= this.yPos + this.height;
   }
}
