package com.mojang.blaze3d.opengl;

import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.TextureFormat;
import it.unimi.dsi.fastutil.ints.Int2IntArrayMap;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.IntIterator;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class GlTexture extends GpuTexture {
   private static final int EMPTY = -1;
   protected final int id;
   private int firstFboId = -1;
   private int firstFboDepthId = -1;
   @Nullable
   private Int2IntMap fboCache;
   protected boolean closed;
   private int views;
   protected final boolean external;

   protected GlTexture(
      @GpuTexture.Usage int p_409690_, String p_410238_, TextureFormat p_410416_, int p_409866_, int p_410468_, int p_409791_, int p_419794_, int p_423590_
   ) {
      this(p_409690_, p_410238_, p_410416_, p_409866_, p_410468_, p_409791_, p_419794_, p_423590_, false);
   }

   protected GlTexture(
      @GpuTexture.Usage int p_409690_,
      String p_410238_,
      TextureFormat p_410416_,
      int p_409866_,
      int p_410468_,
      int p_409791_,
      int p_419794_,
      int p_423590_,
      boolean external
   ) {
      super(p_409690_, p_410238_, p_410416_, p_409866_, p_410468_, p_409791_, p_419794_);
      this.id = p_423590_;
      this.external = external;
   }

   @Override
   public void close() {
      if (!this.closed && !this.external) {
         this.closed = true;
         if (this.views == 0) {
            this.destroyImmediately();
         }
      }
   }

   private void destroyImmediately() {
      GlStateManager._deleteTexture(this.id);
      if (this.firstFboId != -1) {
         GlStateManager._glDeleteFramebuffers(this.firstFboId);
      }

      if (this.fboCache != null) {
         IntIterator var1 = this.fboCache.values().iterator();

         while (var1.hasNext()) {
            int i = (Integer)var1.next();
            GlStateManager._glDeleteFramebuffers(i);
         }
      }
   }

   @Override
   public boolean isClosed() {
      return this.closed;
   }

   public int getFbo(DirectStateAccess p_412345_, @Nullable GpuTexture p_410364_) {
      int i = p_410364_ == null ? 0 : ((GlTexture)p_410364_).id;
      boolean useStencil = p_410364_ != null && p_410364_.getFormat().hasStencilAspect();
      if (this.firstFboDepthId == i) {
         return this.firstFboId;
      } else if (this.firstFboId == -1) {
         this.firstFboId = this.createFbo(p_412345_, i, useStencil);
         this.firstFboDepthId = i;
         return this.firstFboId;
      } else {
         if (this.fboCache == null) {
            this.fboCache = new Int2IntArrayMap();
         }

         return this.fboCache.computeIfAbsent(i, p_470461_ -> this.createFbo(p_412345_, p_470461_, useStencil));
      }
   }

   @Deprecated
   private int createFbo(DirectStateAccess p_470782_, int p_470657_) {
      return this.createFbo(p_470782_, p_470657_, false);
   }

   private int createFbo(DirectStateAccess p_470782_, int p_470657_, boolean useStencil) {
      int i = p_470782_.createFrameBufferObject();
      p_470782_.bindFrameBufferTextures(i, this.id, p_470657_, 0, 0, useStencil);
      return i;
   }

   public int glId() {
      return this.id;
   }

   public void addViews() {
      this.views++;
   }

   public void removeViews() {
      this.views--;
      if (this.closed && this.views == 0) {
         this.destroyImmediately();
      }
   }
}
