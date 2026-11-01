package net.minecraft.client.renderer.texture;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import javax.annotation.Nullable;
import net.minecraft.client.resources.metadata.texture.TextureMetadataSection;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class SimpleTexture extends AbstractTexture {
   static final Logger LOGGER = LogUtils.getLogger();
   protected final ResourceLocation location;

   public SimpleTexture(ResourceLocation p_118133_) {
      this.location = p_118133_;
   }

   @Override
   public void load(ResourceManager p_118135_) throws IOException {
      SimpleTexture.TextureImage $$1 = this.getTextureImage(p_118135_);
      $$1.throwIfError();
      TextureMetadataSection $$2 = $$1.getTextureMetadata();
      boolean $$3;
      boolean $$4;
      if ($$2 != null) {
         $$3 = $$2.isBlur();
         $$4 = $$2.isClamp();
      } else {
         $$3 = false;
         $$4 = false;
      }

      NativeImage $$7 = $$1.getImage();
      if (!RenderSystem.isOnRenderThreadOrInit()) {
         RenderSystem.recordRenderCall(() -> this.doLoad($$7, $$3, $$4));
      } else {
         this.doLoad($$7, $$3, $$4);
      }
   }

   private void doLoad(NativeImage p_118137_, boolean p_118138_, boolean p_118139_) {
      TextureUtil.prepareImage(this.getId(), 0, p_118137_.getWidth(), p_118137_.getHeight());
      p_118137_.upload(0, 0, 0, 0, 0, p_118137_.getWidth(), p_118137_.getHeight(), p_118138_, p_118139_, false, true);
   }

   protected SimpleTexture.TextureImage getTextureImage(ResourceManager p_118140_) {
      return SimpleTexture.TextureImage.load(p_118140_, this.location);
   }

   @OnlyIn(Dist.CLIENT)
   protected static class TextureImage implements Closeable {
      @Nullable
      private final TextureMetadataSection metadata;
      @Nullable
      private final NativeImage image;
      @Nullable
      private final IOException exception;

      public TextureImage(IOException p_118153_) {
         this.exception = p_118153_;
         this.metadata = null;
         this.image = null;
      }

      public TextureImage(@Nullable TextureMetadataSection p_118150_, NativeImage p_118151_) {
         this.exception = null;
         this.metadata = p_118150_;
         this.image = p_118151_;
      }

      public static SimpleTexture.TextureImage load(ResourceManager p_118156_, ResourceLocation p_118157_) {
         try {
            Resource $$2 = p_118156_.getResourceOrThrow(p_118157_);

            NativeImage $$4;
            try (InputStream $$3 = $$2.open()) {
               $$4 = NativeImage.read($$3);
            }

            TextureMetadataSection $$6 = null;

            try {
               $$6 = $$2.metadata().getSection(TextureMetadataSection.SERIALIZER).orElse(null);
            } catch (RuntimeException var8) {
               SimpleTexture.LOGGER.warn("Failed reading metadata of: {}", p_118157_, var8);
            }

            return new SimpleTexture.TextureImage($$6, $$4);
         } catch (IOException var10) {
            return new SimpleTexture.TextureImage(var10);
         }
      }

      @Nullable
      public TextureMetadataSection getTextureMetadata() {
         return this.metadata;
      }

      public NativeImage getImage() throws IOException {
         if (this.exception != null) {
            throw this.exception;
         } else {
            return this.image;
         }
      }

      @Override
      public void close() {
         if (this.image != null) {
            this.image.close();
         }
      }

      public void throwIfError() throws IOException {
         if (this.exception != null) {
            throw this.exception;
         }
      }
   }
}
