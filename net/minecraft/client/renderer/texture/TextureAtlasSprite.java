package net.minecraft.client.renderer.texture;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.nio.ByteBuffer;
import net.minecraft.client.renderer.SpriteCoordinateExpander;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryUtil;

@OnlyIn(Dist.CLIENT)
public class TextureAtlasSprite implements AutoCloseable {
   private final Identifier atlasLocation;
   private final SpriteContents contents;
   private final int x;
   private final int y;
   private final float u0;
   private final float u1;
   private final float v0;
   private final float v1;
   private final int padding;

   protected TextureAtlasSprite(Identifier p_467342_, SpriteContents p_248526_, int p_248950_, int p_249741_, int p_248672_, int p_248637_, int p_461015_) {
      this.atlasLocation = p_467342_;
      this.contents = p_248526_;
      this.padding = p_461015_;
      this.x = p_248672_;
      this.y = p_248637_;
      this.u0 = (float)(p_248672_ + p_461015_) / p_248950_;
      this.u1 = (float)(p_248672_ + p_461015_ + p_248526_.width()) / p_248950_;
      this.v0 = (float)(p_248637_ + p_461015_) / p_249741_;
      this.v1 = (float)(p_248637_ + p_461015_ + p_248526_.height()) / p_249741_;
   }

   public int getX() {
      return this.x;
   }

   public int getY() {
      return this.y;
   }

   public float getU0() {
      return this.u0;
   }

   public float getU1() {
      return this.u1;
   }

   public SpriteContents contents() {
      return this.contents;
   }

   public SpriteContents.AnimationState createAnimationState(GpuBufferSlice p_461032_, int p_461021_) {
      return this.contents.createAnimationState(p_461032_, p_461021_);
   }

   public float getU(float p_294967_) {
      float f = this.u1 - this.u0;
      return this.u0 + f * p_294967_;
   }

   public float getV0() {
      return this.v0;
   }

   public float getV1() {
      return this.v1;
   }

   public float getV(float p_294167_) {
      float f = this.v1 - this.v0;
      return this.v0 + f * p_294167_;
   }

   public Identifier atlasLocation() {
      return this.atlasLocation;
   }

   @Override
   public String toString() {
      return "TextureAtlasSprite{contents='" + this.contents + "', u0=" + this.u0 + ", u1=" + this.u1 + ", v0=" + this.v0 + ", v1=" + this.v1 + "}";
   }

   public void uploadFirstFrame(GpuTexture p_404964_, int p_461239_) {
      this.contents.uploadFirstFrame(p_404964_, p_461239_);
   }

   public VertexConsumer wrap(VertexConsumer p_118382_) {
      return new SpriteCoordinateExpander(p_118382_, this);
   }

   boolean isAnimated() {
      return this.contents.isAnimated();
   }

   public void uploadSpriteUbo(ByteBuffer p_460745_, int p_461279_, int p_461147_, int p_460792_, int p_461042_, int p_460820_) {
      for (int i = 0; i <= p_461147_; i++) {
         Std140Builder.intoBuffer(MemoryUtil.memSlice(p_460745_, p_461279_ + i * p_460820_, p_460820_))
            .putMat4f(new Matrix4f().ortho2D(0.0F, p_460792_ >> i, 0.0F, p_461042_ >> i))
            .putMat4f(
               new Matrix4f()
                  .translate(this.x >> i, this.y >> i, 0.0F)
                  .scale(this.contents.width() + this.padding * 2 >> i, this.contents.height() + this.padding * 2 >> i, 1.0F)
            )
            .putFloat((float)this.padding / this.contents.width())
            .putFloat((float)this.padding / this.contents.height())
            .putInt(i);
      }
   }

   @Override
   public void close() {
      this.contents.close();
   }

   public int getPixelRGBA(int frameIndex, int x, int y) {
      if (this.contents.animatedTexture != null) {
         x += this.contents.animatedTexture.getFrameX(frameIndex) * this.contents.width;
         y += this.contents.animatedTexture.getFrameY(frameIndex) * this.contents.height;
      }

      return this.contents.getOriginalImage().getPixel(x, y);
   }
}
