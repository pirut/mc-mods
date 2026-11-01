package net.minecraft.client.gui.font;

import com.mojang.blaze3d.font.GlyphBitmap;
import com.mojang.blaze3d.font.GlyphInfo;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.TextureFormat;
import java.nio.file.Path;
import java.util.function.Supplier;
import net.minecraft.client.gui.font.glyphs.BakedSheetGlyph;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.Dumpable;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class FontTexture extends AbstractTexture implements Dumpable {
   private static final int SIZE = 256;
   private final GlyphRenderTypes renderTypes;
   private final boolean colored;
   private final FontTexture.Node root;

   public FontTexture(Supplier<String> p_405523_, GlyphRenderTypes p_285000_, boolean p_285085_) {
      this.colored = p_285085_;
      this.root = new FontTexture.Node(0, 0, 256, 256);
      GpuDevice gpudevice = RenderSystem.getDevice();
      this.texture = gpudevice.createTexture(p_405523_, 7, p_285085_ ? TextureFormat.RGBA8 : TextureFormat.RED8, 256, 256, 1, 1);
      this.sampler = RenderSystem.getSamplerCache().getRepeat(FilterMode.NEAREST);
      this.textureView = gpudevice.createTextureView(this.texture);
      this.renderTypes = p_285000_;
   }

   @Nullable
   public BakedSheetGlyph add(GlyphInfo p_440102_, GlyphBitmap p_439576_) {
      if (p_439576_.isColored() != this.colored) {
         return null;
      } else {
         FontTexture.Node fonttexture$node = this.root.insert(p_439576_);
         if (fonttexture$node != null) {
            p_439576_.upload(fonttexture$node.x, fonttexture$node.y, this.getTexture());
            float f = 256.0F;
            float f1 = 256.0F;
            float f2 = 0.01F;
            return new BakedSheetGlyph(
               p_440102_,
               this.renderTypes,
               this.getTextureView(),
               (fonttexture$node.x + 0.01F) / 256.0F,
               (fonttexture$node.x - 0.01F + p_439576_.getPixelWidth()) / 256.0F,
               (fonttexture$node.y + 0.01F) / 256.0F,
               (fonttexture$node.y - 0.01F + p_439576_.getPixelHeight()) / 256.0F,
               p_439576_.getLeft(),
               p_439576_.getRight(),
               p_439576_.getTop(),
               p_439576_.getBottom()
            );
         } else {
            return null;
         }
      }
   }

   @Override
   public void dumpContents(Identifier p_466856_, Path p_285511_) {
      if (this.texture != null) {
         String s = p_466856_.toDebugFileName();
         TextureUtil.writeAsPNG(p_285511_, s, this.texture, 0, p_285145_ -> (p_285145_ & 0xFF000000) == 0 ? -16777216 : p_285145_);
      }
   }

   @OnlyIn(Dist.CLIENT)
   static class Node {
      final int x;
      final int y;
      private final int width;
      private final int height;
      @Nullable
      private FontTexture.Node left;
      @Nullable
      private FontTexture.Node right;
      private boolean occupied;

      Node(int p_95113_, int p_95114_, int p_95115_, int p_95116_) {
         this.x = p_95113_;
         this.y = p_95114_;
         this.width = p_95115_;
         this.height = p_95116_;
      }

      @Nullable
      FontTexture.Node insert(GlyphBitmap p_439823_) {
         if (this.left != null && this.right != null) {
            FontTexture.Node fonttexture$node = this.left.insert(p_439823_);
            if (fonttexture$node == null) {
               fonttexture$node = this.right.insert(p_439823_);
            }

            return fonttexture$node;
         } else if (this.occupied) {
            return null;
         } else {
            int i = p_439823_.getPixelWidth();
            int j = p_439823_.getPixelHeight();
            if (i > this.width || j > this.height) {
               return null;
            } else if (i == this.width && j == this.height) {
               this.occupied = true;
               return this;
            } else {
               int k = this.width - i;
               int l = this.height - j;
               if (k > l) {
                  this.left = new FontTexture.Node(this.x, this.y, i, this.height);
                  this.right = new FontTexture.Node(this.x + i + 1, this.y, this.width - i - 1, this.height);
               } else {
                  this.left = new FontTexture.Node(this.x, this.y, this.width, j);
                  this.right = new FontTexture.Node(this.x, this.y + j + 1, this.width, this.height - j - 1);
               }

               return this.left.insert(p_439823_);
            }
         }
      }
   }
}
