package com.mojang.blaze3d.buffers;

import com.mojang.blaze3d.DontObfuscate;
import java.nio.ByteBuffer;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4fc;
import org.joml.Vector2fc;
import org.joml.Vector2ic;
import org.joml.Vector3fc;
import org.joml.Vector3ic;
import org.joml.Vector4fc;
import org.joml.Vector4ic;
import org.lwjgl.system.MemoryStack;

@OnlyIn(Dist.CLIENT)
@DontObfuscate
public class Std140Builder {
   private final ByteBuffer buffer;
   private final int start;

   private Std140Builder(ByteBuffer p_418360_) {
      this.buffer = p_418360_;
      this.start = p_418360_.position();
   }

   public static Std140Builder intoBuffer(ByteBuffer p_418040_) {
      return new Std140Builder(p_418040_);
   }

   public static Std140Builder onStack(MemoryStack p_418464_, int p_418367_) {
      return new Std140Builder(p_418464_.malloc(p_418367_));
   }

   public ByteBuffer get() {
      return this.buffer.flip();
   }

   public Std140Builder align(int p_418208_) {
      int i = this.buffer.position();
      this.buffer.position(this.start + Mth.roundToward(i - this.start, p_418208_));
      return this;
   }

   public Std140Builder putFloat(float p_418453_) {
      this.align(4);
      this.buffer.putFloat(p_418453_);
      return this;
   }

   public Std140Builder putInt(int p_418434_) {
      this.align(4);
      this.buffer.putInt(p_418434_);
      return this;
   }

   public Std140Builder putVec2(float p_418529_, float p_418317_) {
      this.align(8);
      this.buffer.putFloat(p_418529_);
      this.buffer.putFloat(p_418317_);
      return this;
   }

   public Std140Builder putVec2(Vector2fc p_418253_) {
      this.align(8);
      p_418253_.get(this.buffer);
      this.buffer.position(this.buffer.position() + 8);
      return this;
   }

   public Std140Builder putIVec2(int p_418296_, int p_418329_) {
      this.align(8);
      this.buffer.putInt(p_418296_);
      this.buffer.putInt(p_418329_);
      return this;
   }

   public Std140Builder putIVec2(Vector2ic p_418365_) {
      this.align(8);
      p_418365_.get(this.buffer);
      this.buffer.position(this.buffer.position() + 8);
      return this;
   }

   public Std140Builder putVec3(float p_418082_, float p_418358_, float p_418315_) {
      this.align(16);
      this.buffer.putFloat(p_418082_);
      this.buffer.putFloat(p_418358_);
      this.buffer.putFloat(p_418315_);
      this.buffer.position(this.buffer.position() + 4);
      return this;
   }

   public Std140Builder putVec3(Vector3fc p_418523_) {
      this.align(16);
      p_418523_.get(this.buffer);
      this.buffer.position(this.buffer.position() + 16);
      return this;
   }

   public Std140Builder putIVec3(int p_418318_, int p_418086_, int p_418418_) {
      this.align(16);
      this.buffer.putInt(p_418318_);
      this.buffer.putInt(p_418086_);
      this.buffer.putInt(p_418418_);
      this.buffer.position(this.buffer.position() + 4);
      return this;
   }

   public Std140Builder putIVec3(Vector3ic p_418255_) {
      this.align(16);
      p_418255_.get(this.buffer);
      this.buffer.position(this.buffer.position() + 16);
      return this;
   }

   public Std140Builder putVec4(float p_418196_, float p_418090_, float p_418034_, float p_418469_) {
      this.align(16);
      this.buffer.putFloat(p_418196_);
      this.buffer.putFloat(p_418090_);
      this.buffer.putFloat(p_418034_);
      this.buffer.putFloat(p_418469_);
      return this;
   }

   public Std140Builder putVec4(Vector4fc p_418025_) {
      this.align(16);
      p_418025_.get(this.buffer);
      this.buffer.position(this.buffer.position() + 16);
      return this;
   }

   public Std140Builder putIVec4(int p_418311_, int p_418098_, int p_418369_, int p_418463_) {
      this.align(16);
      this.buffer.putInt(p_418311_);
      this.buffer.putInt(p_418098_);
      this.buffer.putInt(p_418369_);
      this.buffer.putInt(p_418463_);
      return this;
   }

   public Std140Builder putIVec4(Vector4ic p_418107_) {
      this.align(16);
      p_418107_.get(this.buffer);
      this.buffer.position(this.buffer.position() + 16);
      return this;
   }

   public Std140Builder putMat4f(Matrix4fc p_418252_) {
      this.align(16);
      p_418252_.get(this.buffer);
      this.buffer.position(this.buffer.position() + 64);
      return this;
   }
}
