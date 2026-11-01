package com.mojang.blaze3d.opengl;

import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import it.unimi.dsi.fastutil.ints.Int2IntArrayMap;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.IntIterator;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class GlTextureView extends GpuTextureView {
   private static final int EMPTY = -1;
   private boolean closed;
   private int firstFboId = -1;
   private int firstFboDepthId = -1;
   @Nullable
   private Int2IntMap fboCache;

   protected GlTextureView(GlTexture p_423648_, int p_423671_, int p_423545_) {
      super(p_423648_, p_423671_, p_423545_);
      p_423648_.addViews();
   }

   @Override
   public boolean isClosed() {
      return this.closed;
   }

   @Override
   public void close() {
      if (!this.closed) {
         this.closed = true;
         this.texture().removeViews();
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
   }

   public int getFbo(DirectStateAccess p_461283_, @Nullable GpuTexture p_460970_) {
      int i = p_460970_ == null ? 0 : ((GlTexture)p_460970_).id;
      boolean useStencil = p_460970_ != null && p_460970_.getFormat().hasStencilAspect();
      if (this.firstFboDepthId == i) {
         return this.firstFboId;
      } else if (this.firstFboId == -1) {
         this.firstFboId = this.createFbo(p_461283_, i, useStencil);
         this.firstFboDepthId = i;
         return this.firstFboId;
      } else {
         if (this.fboCache == null) {
            this.fboCache = new Int2IntArrayMap();
         }

         return this.fboCache.computeIfAbsent(i, p_470463_ -> this.createFbo(p_461283_, p_470463_, useStencil));
      }
   }

   @Deprecated
   private int createFbo(DirectStateAccess p_470787_, int p_470630_) {
      return this.createFbo(p_470787_, p_470630_, false);
   }

   private int createFbo(DirectStateAccess p_470787_, int p_470630_, boolean useStencil) {
      int i = p_470787_.createFrameBufferObject();
      p_470787_.bindFrameBufferTextures(i, this.texture().id, p_470630_, this.baseMipLevel(), 0, useStencil);
      return i;
   }

   public GlTexture texture() {
      return (GlTexture)super.texture();
   }
}
