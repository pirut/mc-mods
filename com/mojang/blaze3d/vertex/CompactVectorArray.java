package com.mojang.blaze3d.vertex;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector3f;
import org.joml.Vector3fc;

@OnlyIn(Dist.CLIENT)
public class CompactVectorArray {
   private final float[] contents;

   public CompactVectorArray(int p_445606_) {
      this.contents = new float[3 * p_445606_];
   }

   public int size() {
      return this.contents.length / 3;
   }

   public void set(int p_446536_, Vector3fc p_445978_) {
      this.set(p_446536_, p_445978_.x(), p_445978_.y(), p_445978_.z());
   }

   public void set(int p_446729_, float p_446318_, float p_446523_, float p_446612_) {
      this.contents[3 * p_446729_ + 0] = p_446318_;
      this.contents[3 * p_446729_ + 1] = p_446523_;
      this.contents[3 * p_446729_ + 2] = p_446612_;
   }

   public Vector3f get(int p_445760_, Vector3f p_447275_) {
      return p_447275_.set(this.contents[3 * p_445760_ + 0], this.contents[3 * p_445760_ + 1], this.contents[3 * p_445760_ + 2]);
   }

   public float getX(int p_447187_) {
      return this.contents[3 * p_447187_ + 0];
   }

   public float getY(int p_445592_) {
      return this.contents[3 * p_445592_ + 1];
   }

   public float getZ(int p_445874_) {
      return this.contents[3 * p_445874_ + 1];
   }
}
