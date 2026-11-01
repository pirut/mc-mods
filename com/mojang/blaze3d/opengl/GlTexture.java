package com.mojang.blaze3d.opengl;

import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.TextureFormat;
import it.unimi.dsi.fastutil.ints.Int2IntArrayMap;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.IntIterator;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class GlTexture extends GpuTexture {
   private static final int EMPTY = -1;
   protected final int id;
   private int firstFboId = -1;
   private int firstFboDepthId = -1;
   @Nullable
   private Int2IntMap fboCache;
   protected boolean closed;
   private int views;

   protected GlTexture(@GpuTexture.Usage int usage, String label, TextureFormat format, int width, int height, int depthOrLayers, int mipLevels, int id) {
      super(usage, label, format, width, height, depthOrLayers, mipLevels);
      this.id = id;
   }

   @Override
   public void close() {
      if (!this.closed) {
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

   public int getFbo(DirectStateAccess directStateAccess, @Nullable GpuTexture texture) {
      int i = texture == null ? 0 : ((GlTexture)texture).id;
      if (this.firstFboDepthId == i) {
         return this.firstFboId;
      } else if (this.firstFboId == -1) {
         this.firstFboId = this.createFbo(directStateAccess, i);
         this.firstFboDepthId = i;
         return this.firstFboId;
      } else {
         if (this.fboCache == null) {
            this.fboCache = new Int2IntArrayMap();
         }

         return this.fboCache.computeIfAbsent(i, ix -> this.createFbo(directStateAccess, ix));
      }
   }

   private int createFbo(DirectStateAccess directStateAccess, int id) {
      int i = directStateAccess.createFrameBufferObject();
      directStateAccess.bindFrameBufferTextures(i, this.id, id, 0, 0);
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
