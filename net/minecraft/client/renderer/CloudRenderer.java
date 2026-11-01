package net.minecraft.client.renderer;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class CloudRenderer extends SimplePreparableReloadListener<Optional<CloudRenderer.TextureData>> implements AutoCloseable {
   private static final int FLAG_INSIDE_FACE = 16;
   private static final int FLAG_USE_TOP_COLOR = 32;
   private static final float CELL_SIZE_IN_BLOCKS = 12.0F;
   private static final int TICKS_PER_CELL = 400;
   private static final float BLOCKS_PER_SECOND = 0.6F;
   private static final int UBO_SIZE = new Std140SizeCalculator().putVec4().putVec3().putVec3().get();
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final Identifier TEXTURE_LOCATION = Identifier.withDefaultNamespace("textures/environment/clouds.png");
   private static final long EMPTY_CELL = 0L;
   private static final int COLOR_OFFSET = 4;
   private static final int NORTH_OFFSET = 3;
   private static final int EAST_OFFSET = 2;
   private static final int SOUTH_OFFSET = 1;
   private static final int WEST_OFFSET = 0;
   private boolean needsRebuild = true;
   private int prevCellX = Integer.MIN_VALUE;
   private int prevCellZ = Integer.MIN_VALUE;
   private CloudRenderer.RelativeCameraPos prevRelativeCameraPos = CloudRenderer.RelativeCameraPos.INSIDE_CLOUDS;
   @Nullable
   private CloudStatus prevType;
   @Nullable
   private CloudRenderer.TextureData texture;
   private int quadCount = 0;
   private final MappableRingBuffer ubo = new MappableRingBuffer(() -> "Cloud UBO", 130, UBO_SIZE);
   @Nullable
   private MappableRingBuffer utb;

   protected Optional<CloudRenderer.TextureData> prepare(ResourceManager p_363181_, ProfilerFiller p_361418_) {
      try {
         Optional optional;
         try (
            InputStream inputstream = p_363181_.open(TEXTURE_LOCATION);
            NativeImage nativeimage = NativeImage.read(inputstream);
         ) {
            int i = nativeimage.getWidth();
            int j = nativeimage.getHeight();
            long[] along = new long[i * j];

            for (int k = 0; k < j; k++) {
               for (int l = 0; l < i; l++) {
                  int i1 = nativeimage.getPixel(l, k);
                  if (isCellEmpty(i1)) {
                     along[l + k * i] = 0L;
                  } else {
                     boolean flag = isCellEmpty(nativeimage.getPixel(l, Math.floorMod(k - 1, j)));
                     boolean flag1 = isCellEmpty(nativeimage.getPixel(Math.floorMod(l + 1, j), k));
                     boolean flag2 = isCellEmpty(nativeimage.getPixel(l, Math.floorMod(k + 1, j)));
                     boolean flag3 = isCellEmpty(nativeimage.getPixel(Math.floorMod(l - 1, j), k));
                     along[l + k * i] = packCellData(i1, flag, flag1, flag2, flag3);
                  }
               }
            }

            optional = Optional.of(new CloudRenderer.TextureData(along, i, j));
         }

         return optional;
      } catch (IOException var20) {
         LOGGER.error("Failed to load cloud texture", var20);
         return Optional.empty();
      }
   }

   private static int getSizeForCloudDistance(int p_419493_) {
      int i = 4;
      int j = (p_419493_ + 1) * 2 * (p_419493_ + 1) * 2 / 2;
      int k = j * 4 + 54;
      return k * 3;
   }

   protected void apply(Optional<CloudRenderer.TextureData> p_362811_, ResourceManager p_364101_, ProfilerFiller p_360749_) {
      this.texture = p_362811_.orElse(null);
      this.needsRebuild = true;
   }

   private static boolean isCellEmpty(int p_363144_) {
      return ARGB.alpha(p_363144_) < 10;
   }

   private static long packCellData(int p_363244_, boolean p_365018_, boolean p_363077_, boolean p_360343_, boolean p_360813_) {
      return (long)p_363244_ << 4 | (p_365018_ ? 1 : 0) << 3 | (p_363077_ ? 1 : 0) << 2 | (p_360343_ ? 1 : 0) << 1 | (p_360813_ ? 1 : 0) << 0;
   }

   private static boolean isNorthEmpty(long p_361438_) {
      return (p_361438_ >> 3 & 1L) != 0L;
   }

   private static boolean isEastEmpty(long p_361625_) {
      return (p_361625_ >> 2 & 1L) != 0L;
   }

   private static boolean isSouthEmpty(long p_361797_) {
      return (p_361797_ >> 1 & 1L) != 0L;
   }

   private static boolean isWestEmpty(long p_363963_) {
      return (p_363963_ >> 0 & 1L) != 0L;
   }

   public void render(int p_363907_, CloudStatus p_364293_, float p_363260_, Vec3 p_363573_, long p_467391_, float p_360711_) {
      if (this.texture != null) {
         int i = Minecraft.getInstance().options.cloudRange().get() * 16;
         int j = Mth.ceil(i / 12.0F);
         int k = getSizeForCloudDistance(j);
         if (this.utb == null || this.utb.currentBuffer().size() != k) {
            if (this.utb != null) {
               this.utb.close();
            }

            this.utb = new MappableRingBuffer(() -> "Cloud UTB", 258, k);
         }

         float f = (float)(p_363260_ - p_363573_.y);
         float f1 = f + 4.0F;
         CloudRenderer.RelativeCameraPos cloudrenderer$relativecamerapos;
         if (f1 < 0.0F) {
            cloudrenderer$relativecamerapos = CloudRenderer.RelativeCameraPos.ABOVE_CLOUDS;
         } else if (f > 0.0F) {
            cloudrenderer$relativecamerapos = CloudRenderer.RelativeCameraPos.BELOW_CLOUDS;
         } else {
            cloudrenderer$relativecamerapos = CloudRenderer.RelativeCameraPos.INSIDE_CLOUDS;
         }

         float f2 = (float)(p_467391_ % (this.texture.width * 400L)) + p_360711_;
         double d0 = p_363573_.x + f2 * 0.030000001F;
         double d1 = p_363573_.z + 3.96F;
         double d2 = this.texture.width * 12.0;
         double d3 = this.texture.height * 12.0;
         d0 -= Mth.floor(d0 / d2) * d2;
         d1 -= Mth.floor(d1 / d3) * d3;
         int l = Mth.floor(d0 / 12.0);
         int i1 = Mth.floor(d1 / 12.0);
         float f3 = (float)(d0 - l * 12.0F);
         float f4 = (float)(d1 - i1 * 12.0F);
         boolean flag = p_364293_ == CloudStatus.FANCY;
         RenderPipeline renderpipeline = flag ? RenderPipelines.CLOUDS : RenderPipelines.FLAT_CLOUDS;
         if (this.needsRebuild
            || l != this.prevCellX
            || i1 != this.prevCellZ
            || cloudrenderer$relativecamerapos != this.prevRelativeCameraPos
            || p_364293_ != this.prevType) {
            this.needsRebuild = false;
            this.prevCellX = l;
            this.prevCellZ = i1;
            this.prevRelativeCameraPos = cloudrenderer$relativecamerapos;
            this.prevType = p_364293_;
            this.utb.rotate();

            try (GpuBuffer.MappedView gpubuffer$mappedview = RenderSystem.getDevice().createCommandEncoder().mapBuffer(this.utb.currentBuffer(), false, true)) {
               this.buildMesh(cloudrenderer$relativecamerapos, gpubuffer$mappedview.data(), l, i1, flag, j);
               this.quadCount = gpubuffer$mappedview.data().position() / 3;
            }
         }

         if (this.quadCount != 0) {
            try (GpuBuffer.MappedView gpubuffer$mappedview1 = RenderSystem.getDevice().createCommandEncoder().mapBuffer(this.ubo.currentBuffer(), false, true)) {
               Std140Builder.intoBuffer(gpubuffer$mappedview1.data())
                  .putVec4(ARGB.vector4fFromARGB32(p_363907_))
                  .putVec3(-f3, f, -f4)
                  .putVec3(12.0F, 4.0F, 12.0F);
            }

            GpuBufferSlice gpubufferslice = RenderSystem.getDynamicUniforms()
               .writeTransform(RenderSystem.getModelViewMatrix(), new Vector4f(1.0F, 1.0F, 1.0F, 1.0F), new Vector3f(), new Matrix4f());
            RenderTarget rendertarget = Minecraft.getInstance().getMainRenderTarget();
            RenderTarget rendertarget1 = Minecraft.getInstance().levelRenderer.getCloudsTarget();
            RenderSystem.AutoStorageIndexBuffer rendersystem$autostorageindexbuffer = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);
            GpuBuffer gpubuffer = rendersystem$autostorageindexbuffer.getBuffer(6 * this.quadCount);
            GpuTextureView gputextureview;
            GpuTextureView gputextureview1;
            if (rendertarget1 != null) {
               gputextureview = rendertarget1.getColorTextureView();
               gputextureview1 = rendertarget1.getDepthTextureView();
            } else {
               gputextureview = rendertarget.getColorTextureView();
               gputextureview1 = rendertarget.getDepthTextureView();
            }

            try (RenderPass renderpass = RenderSystem.getDevice()
                  .createCommandEncoder()
                  .createRenderPass(() -> "Clouds", gputextureview, OptionalInt.empty(), gputextureview1, OptionalDouble.empty())) {
               renderpass.setPipeline(renderpipeline);
               RenderSystem.bindDefaultUniforms(renderpass);
               renderpass.setUniform("DynamicTransforms", gpubufferslice);
               renderpass.setIndexBuffer(gpubuffer, rendersystem$autostorageindexbuffer.type());
               renderpass.setUniform("CloudInfo", this.ubo.currentBuffer());
               renderpass.setUniform("CloudFaces", this.utb.currentBuffer());
               renderpass.drawIndexed(0, 0, 6 * this.quadCount, 1);
            }
         }
      }
   }

   private void buildMesh(CloudRenderer.RelativeCameraPos p_364842_, ByteBuffer p_418203_, int p_362701_, int p_361589_, boolean p_418059_, int p_419903_) {
      if (this.texture != null) {
         long[] along = this.texture.cells;
         int i = this.texture.width;
         int j = this.texture.height;

         for (int k = 0; k <= 2 * p_419903_; k++) {
            for (int l = -k; l <= k; l++) {
               int i1 = k - Math.abs(l);
               if (i1 >= 0 && i1 <= p_419903_ && l * l + i1 * i1 <= p_419903_ * p_419903_) {
                  if (i1 != 0) {
                     this.tryBuildCell(p_364842_, p_418203_, p_362701_, p_361589_, p_418059_, l, i, -i1, j, along);
                  }

                  this.tryBuildCell(p_364842_, p_418203_, p_362701_, p_361589_, p_418059_, l, i, i1, j, along);
               }
            }
         }
      }
   }

   private void tryBuildCell(
      CloudRenderer.RelativeCameraPos p_428373_,
      ByteBuffer p_428298_,
      int p_428405_,
      int p_428485_,
      boolean p_428426_,
      int p_428326_,
      int p_428303_,
      int p_428570_,
      int p_428476_,
      long[] p_428541_
   ) {
      int i = Math.floorMod(p_428405_ + p_428326_, p_428303_);
      int j = Math.floorMod(p_428485_ + p_428570_, p_428476_);
      long k = p_428541_[i + j * p_428303_];
      if (k != 0L) {
         if (p_428426_) {
            this.buildExtrudedCell(p_428373_, p_428298_, p_428326_, p_428570_, k);
         } else {
            this.buildFlatCell(p_428298_, p_428326_, p_428570_);
         }
      }
   }

   private void buildFlatCell(ByteBuffer p_418189_, int p_364027_, int p_361818_) {
      this.encodeFace(p_418189_, p_364027_, p_361818_, Direction.DOWN, 32);
   }

   private void encodeFace(ByteBuffer p_418193_, int p_418174_, int p_418379_, Direction p_418493_, int p_418242_) {
      int i = p_418493_.get3DDataValue() | p_418242_;
      i |= (p_418174_ & 1) << 7;
      i |= (p_418379_ & 1) << 6;
      p_418193_.put((byte)(p_418174_ >> 1)).put((byte)(p_418379_ >> 1)).put((byte)i);
   }

   private void buildExtrudedCell(CloudRenderer.RelativeCameraPos p_360766_, ByteBuffer p_418364_, int p_362180_, int p_364234_, long p_364423_) {
      if (p_360766_ != CloudRenderer.RelativeCameraPos.BELOW_CLOUDS) {
         this.encodeFace(p_418364_, p_362180_, p_364234_, Direction.UP, 0);
      }

      if (p_360766_ != CloudRenderer.RelativeCameraPos.ABOVE_CLOUDS) {
         this.encodeFace(p_418364_, p_362180_, p_364234_, Direction.DOWN, 0);
      }

      if (isNorthEmpty(p_364423_) && p_364234_ > 0) {
         this.encodeFace(p_418364_, p_362180_, p_364234_, Direction.NORTH, 0);
      }

      if (isSouthEmpty(p_364423_) && p_364234_ < 0) {
         this.encodeFace(p_418364_, p_362180_, p_364234_, Direction.SOUTH, 0);
      }

      if (isWestEmpty(p_364423_) && p_362180_ > 0) {
         this.encodeFace(p_418364_, p_362180_, p_364234_, Direction.WEST, 0);
      }

      if (isEastEmpty(p_364423_) && p_362180_ < 0) {
         this.encodeFace(p_418364_, p_362180_, p_364234_, Direction.EAST, 0);
      }

      boolean flag = Math.abs(p_362180_) <= 1 && Math.abs(p_364234_) <= 1;
      if (flag) {
         for (Direction direction : Direction.values()) {
            this.encodeFace(p_418364_, p_362180_, p_364234_, direction, 16);
         }
      }
   }

   public void markForRebuild() {
      this.needsRebuild = true;
   }

   public void endFrame() {
      this.ubo.rotate();
   }

   @Override
   public void close() {
      this.ubo.close();
      if (this.utb != null) {
         this.utb.close();
      }
   }

   @OnlyIn(Dist.CLIENT)
   static enum RelativeCameraPos {
      ABOVE_CLOUDS,
      INSIDE_CLOUDS,
      BELOW_CLOUDS;
   }

   @OnlyIn(Dist.CLIENT)
   public record TextureData(long[] cells, int width, int height) {
   }
}
