package net.minecraft.client.renderer.texture;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.metadata.animation.AnimationFrame;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.client.resources.metadata.texture.TextureMetadataSection;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.metadata.MetadataSectionType;
import net.minecraft.util.ARGB;
import org.slf4j.Logger;

@Environment(EnvType.CLIENT)
public class SpriteContents implements Stitcher.Entry, AutoCloseable {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final int UBO_SIZE = new Std140SizeCalculator().putMat4f().putMat4f().putFloat().putFloat().putInt().get();
   final Identifier name;
   final int width;
   final int height;
   private final NativeImage originalImage;
   NativeImage[] byMipLevel;
   private final SpriteContents.AnimatedTexture animatedTexture;
   private final List<MetadataSectionType.WithValue<?>> additionalMetadata;
   private final MipmapStrategy mipmapStrategy;
   private final float alphaCutoffBias;

   public SpriteContents(Identifier name, FrameSize size, NativeImage originalImage) {
      this(name, size, originalImage, Optional.empty(), List.of(), Optional.empty());
   }

   public SpriteContents(
      Identifier name,
      FrameSize size,
      NativeImage originalImage,
      Optional<AnimationMetadataSection> animationMetadata,
      List<MetadataSectionType.WithValue<?>> additionalMetadata,
      Optional<TextureMetadataSection> textureMetadata
   ) {
      this.name = name;
      this.width = size.width();
      this.height = size.height();
      this.additionalMetadata = additionalMetadata;
      this.animatedTexture = animationMetadata.<SpriteContents.AnimatedTexture>map(
            animationMetadataSection -> this.createAnimatedTexture(size, originalImage.getWidth(), originalImage.getHeight(), animationMetadataSection)
         )
         .orElse(null);
      this.originalImage = originalImage;
      this.byMipLevel = new NativeImage[]{this.originalImage};
      this.mipmapStrategy = textureMetadata.map(TextureMetadataSection::mipmapStrategy).orElse(MipmapStrategy.AUTO);
      this.alphaCutoffBias = textureMetadata.map(TextureMetadataSection::alphaCutoffBias).orElse(0.0F);
   }

   public void increaseMipLevel(int mipLevel) {
      try {
         this.byMipLevel = MipmapGenerator.generateMipLevels(this.name, this.byMipLevel, mipLevel, this.mipmapStrategy, this.alphaCutoffBias);
      } catch (Throwable var5) {
         CrashReport crashReport = CrashReport.forThrowable(var5, "Generating mipmaps for frame");
         CrashReportCategory crashReportCategory = crashReport.addCategory("Frame being iterated");
         crashReportCategory.setDetail("Sprite name", this.name);
         crashReportCategory.setDetail("Sprite size", () -> this.width + " x " + this.height);
         crashReportCategory.setDetail("Sprite frames", () -> this.getFrameCount() + " frames");
         crashReportCategory.setDetail("Mipmap levels", mipLevel);
         crashReportCategory.setDetail("Original image size", () -> this.originalImage.getWidth() + "x" + this.originalImage.getHeight());
         throw new ReportedException(crashReport);
      }
   }

   private int getFrameCount() {
      return this.animatedTexture != null ? this.animatedTexture.frames.size() : 1;
   }

   public boolean isAnimated() {
      return this.getFrameCount() > 1;
   }

   private SpriteContents.AnimatedTexture createAnimatedTexture(FrameSize frameSize, int width, int height, AnimationMetadataSection metadata) {
      int i = width / frameSize.width();
      int j = height / frameSize.height();
      int k = i * j;
      int l = metadata.defaultFrameTime();
      List<SpriteContents.FrameInfo> list;
      if (metadata.frames().isEmpty()) {
         list = new ArrayList<>(k);

         for (int m = 0; m < k; m++) {
            list.add(new SpriteContents.FrameInfo(m, l));
         }
      } else {
         List<AnimationFrame> list2 = metadata.frames().get();
         list = new ArrayList<>(list2.size());

         for (AnimationFrame animationFrame : list2) {
            list.add(new SpriteContents.FrameInfo(animationFrame.index(), animationFrame.timeOr(l)));
         }

         int n = 0;
         IntSet intSet = new IntOpenHashSet();

         for (Iterator<SpriteContents.FrameInfo> iterator = list.iterator(); iterator.hasNext(); n++) {
            SpriteContents.FrameInfo frameInfo = iterator.next();
            boolean bl = true;
            if (frameInfo.time <= 0) {
               LOGGER.warn("Invalid frame duration on sprite {} frame {}: {}", new Object[]{this.name, n, frameInfo.time});
               bl = false;
            }

            if (frameInfo.index < 0 || frameInfo.index >= k) {
               LOGGER.warn("Invalid frame index on sprite {} frame {}: {}", new Object[]{this.name, n, frameInfo.index});
               bl = false;
            }

            if (bl) {
               intSet.add(frameInfo.index);
            } else {
               iterator.remove();
            }
         }

         int[] is = IntStream.range(0, k).filter(ix -> !intSet.contains(ix)).toArray();
         if (is.length > 0) {
            LOGGER.warn("Unused frames in sprite {}: {}", this.name, Arrays.toString(is));
         }
      }

      return list.size() <= 1 ? null : new SpriteContents.AnimatedTexture(List.copyOf(list), i, metadata.interpolatedFrames());
   }

   @Override
   public int width() {
      return this.width;
   }

   @Override
   public int height() {
      return this.height;
   }

   @Override
   public Identifier name() {
      return this.name;
   }

   public IntStream getUniqueFrames() {
      return this.animatedTexture != null ? this.animatedTexture.getUniqueFrames() : IntStream.of(1);
   }

   public SpriteContents.AnimationState createAnimationState(GpuBufferSlice buffer, int size) {
      return this.animatedTexture != null ? this.animatedTexture.createAnimationState(buffer, size) : null;
   }

   public <T> Optional<T> getAdditionalMetadata(MetadataSectionType<T> sectionType) {
      for (MetadataSectionType.WithValue<?> withValue : this.additionalMetadata) {
         Optional<T> optional = withValue.unwrapToType(sectionType);
         if (optional.isPresent()) {
            return optional;
         }
      }

      return Optional.empty();
   }

   @Override
   public void close() {
      for (NativeImage nativeImage : this.byMipLevel) {
         nativeImage.close();
      }
   }

   @Override
   public String toString() {
      return "SpriteContents{name=" + this.name + ", frameCount=" + this.getFrameCount() + ", height=" + this.height + ", width=" + this.width + "}";
   }

   public boolean isTransparent(int frame, int x, int y) {
      int i = x;
      int j = y;
      if (this.animatedTexture != null) {
         i = x + this.animatedTexture.getFrameX(frame) * this.width;
         j = y + this.animatedTexture.getFrameY(frame) * this.height;
      }

      return ARGB.alpha(this.originalImage.getPixel(i, j)) == 0;
   }

   public void uploadFirstFrame(GpuTexture texture, int mipLevel) {
      RenderSystem.getDevice()
         .createCommandEncoder()
         .writeToTexture(texture, this.byMipLevel[mipLevel], mipLevel, 0, 0, 0, this.width >> mipLevel, this.height >> mipLevel, 0, 0);
   }

   @Environment(EnvType.CLIENT)
   class AnimatedTexture {
      final List<SpriteContents.FrameInfo> frames;
      private final int frameRowSize;
      final boolean interpolateFrames;

      AnimatedTexture(final List<SpriteContents.FrameInfo> frames, final int frameRowSize, final boolean interpolateFrames) {
         this.frames = frames;
         this.frameRowSize = frameRowSize;
         this.interpolateFrames = interpolateFrames;
      }

      int getFrameX(int frameIndex) {
         return frameIndex % this.frameRowSize;
      }

      int getFrameY(int frameIndex) {
         return frameIndex / this.frameRowSize;
      }

      public SpriteContents.AnimationState createAnimationState(GpuBufferSlice buffer, int size) {
         GpuDevice gpuDevice = RenderSystem.getDevice();
         Int2ObjectMap<GpuTextureView> int2ObjectMap = new Int2ObjectOpenHashMap();
         GpuBufferSlice[] gpuBufferSlices = new GpuBufferSlice[SpriteContents.this.byMipLevel.length];

         for (int i : this.getUniqueFrames().toArray()) {
            GpuTexture gpuTexture = gpuDevice.createTexture(
               () -> SpriteContents.this.name + " animation frame " + i,
               5,
               TextureFormat.RGBA8,
               SpriteContents.this.width,
               SpriteContents.this.height,
               1,
               SpriteContents.this.byMipLevel.length + 1
            );
            int j = this.getFrameX(i) * SpriteContents.this.width;
            int k = this.getFrameY(i) * SpriteContents.this.height;

            for (int l = 0; l < SpriteContents.this.byMipLevel.length; l++) {
               RenderSystem.getDevice()
                  .createCommandEncoder()
                  .writeToTexture(
                     gpuTexture, SpriteContents.this.byMipLevel[l], l, 0, 0, 0, SpriteContents.this.width >> l, SpriteContents.this.height >> l, j >> l, k >> l
                  );
            }

            int2ObjectMap.put(i, RenderSystem.getDevice().createTextureView(gpuTexture));
         }

         for (int m = 0; m < SpriteContents.this.byMipLevel.length; m++) {
            gpuBufferSlices[m] = buffer.slice(m * size, size);
         }

         return SpriteContents.this.new AnimationState(this, int2ObjectMap, gpuBufferSlices);
      }

      public IntStream getUniqueFrames() {
         return this.frames.stream().mapToInt(frameInfo -> frameInfo.index).distinct();
      }
   }

   @Environment(EnvType.CLIENT)
   public class AnimationState implements AutoCloseable {
      private int frame;
      private int subFrame;
      private final SpriteContents.AnimatedTexture animationInfo;
      private final Int2ObjectMap<GpuTextureView> frameTexturesByIndex;
      private final GpuBufferSlice[] spriteUbosByMip;
      private boolean isDirty = true;

      AnimationState(
         final SpriteContents.AnimatedTexture animationInfo, final Int2ObjectMap<GpuTextureView> frameTexturesByIndex, final GpuBufferSlice[] spriteUbosByMip
      ) {
         this.animationInfo = animationInfo;
         this.frameTexturesByIndex = frameTexturesByIndex;
         this.spriteUbosByMip = spriteUbosByMip;
      }

      public void tick() {
         this.subFrame++;
         this.isDirty = false;
         SpriteContents.FrameInfo frameInfo = this.animationInfo.frames.get(this.frame);
         if (this.subFrame >= frameInfo.time) {
            int i = frameInfo.index;
            this.frame = (this.frame + 1) % this.animationInfo.frames.size();
            this.subFrame = 0;
            int j = this.animationInfo.frames.get(this.frame).index;
            if (i != j) {
               this.isDirty = true;
            }
         }
      }

      public GpuBufferSlice getDrawUbo(int mipLevel) {
         return this.spriteUbosByMip[mipLevel];
      }

      public boolean needsToDraw() {
         return this.animationInfo.interpolateFrames || this.isDirty;
      }

      public void drawToAtlas(RenderPass renderPass, GpuBufferSlice ubo) {
         GpuSampler gpuSampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST, true);
         List<SpriteContents.FrameInfo> list = this.animationInfo.frames;
         int i = list.get(this.frame).index;
         float f = (float)this.subFrame / this.animationInfo.frames.get(this.frame).time;
         int j = (int)(f * 1000.0F);
         if (this.animationInfo.interpolateFrames) {
            int k = list.get((this.frame + 1) % list.size()).index;
            renderPass.setPipeline(RenderPipelines.ANIMATE_SPRITE_INTERPOLATE);
            renderPass.bindTexture("CurrentSprite", (GpuTextureView)this.frameTexturesByIndex.get(i), gpuSampler);
            renderPass.bindTexture("NextSprite", (GpuTextureView)this.frameTexturesByIndex.get(k), gpuSampler);
         } else if (this.isDirty) {
            renderPass.setPipeline(RenderPipelines.ANIMATE_SPRITE_BLIT);
            renderPass.bindTexture("Sprite", (GpuTextureView)this.frameTexturesByIndex.get(i), gpuSampler);
         }

         renderPass.setUniform("SpriteAnimationInfo", ubo);
         renderPass.draw(j << 3, 6);
      }

      @Override
      public void close() {
         ObjectIterator var1 = this.frameTexturesByIndex.values().iterator();

         while (var1.hasNext()) {
            GpuTextureView gpuTextureView = (GpuTextureView)var1.next();
            gpuTextureView.texture().close();
            gpuTextureView.close();
         }
      }
   }

   @Environment(EnvType.CLIENT)
   record FrameInfo(int index, int time) {
   }
}
