package net.minecraft.core;

import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Mth;

public class Rotations {
   public static final StreamCodec<ByteBuf, Rotations> STREAM_CODEC = new StreamCodec<ByteBuf, Rotations>() {
      public Rotations decode(ByteBuf p_320504_) {
         return new Rotations(p_320504_.readFloat(), p_320504_.readFloat(), p_320504_.readFloat());
      }

      public void encode(ByteBuf p_320561_, Rotations p_320041_) {
         p_320561_.writeFloat(p_320041_.x);
         p_320561_.writeFloat(p_320041_.y);
         p_320561_.writeFloat(p_320041_.z);
      }
   };
   protected final float x;
   protected final float y;
   protected final float z;

   public Rotations(float p_123150_, float p_123151_, float p_123152_) {
      this.x = !Float.isInfinite(p_123150_) && !Float.isNaN(p_123150_) ? p_123150_ % 360.0F : 0.0F;
      this.y = !Float.isInfinite(p_123151_) && !Float.isNaN(p_123151_) ? p_123151_ % 360.0F : 0.0F;
      this.z = !Float.isInfinite(p_123152_) && !Float.isNaN(p_123152_) ? p_123152_ % 360.0F : 0.0F;
   }

   public Rotations(ListTag p_123154_) {
      this(p_123154_.getFloat(0), p_123154_.getFloat(1), p_123154_.getFloat(2));
   }

   public ListTag save() {
      ListTag $$0 = new ListTag();
      $$0.add(FloatTag.valueOf(this.x));
      $$0.add(FloatTag.valueOf(this.y));
      $$0.add(FloatTag.valueOf(this.z));
      return $$0;
   }

   @Override
   public boolean equals(Object p_123160_) {
      return !(p_123160_ instanceof Rotations $$1) ? false : this.x == $$1.x && this.y == $$1.y && this.z == $$1.z;
   }

   public float getX() {
      return this.x;
   }

   public float getY() {
      return this.y;
   }

   public float getZ() {
      return this.z;
   }

   public float getWrappedX() {
      return Mth.wrapDegrees(this.x);
   }

   public float getWrappedY() {
      return Mth.wrapDegrees(this.y);
   }

   public float getWrappedZ() {
      return Mth.wrapDegrees(this.z);
   }
}
