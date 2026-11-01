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
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
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
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@Environment(EnvType.CLIENT)
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

   protected Optional<CloudRenderer.TextureData> prepare(ResourceManager resourceManager, ProfilerFiller profilerFiller) {
      try {
         Optional var20;
         try (
            InputStream inputStream = resourceManager.open(TEXTURE_LOCATION);
            NativeImage nativeImage = NativeImage.read(inputStream);
         ) {
            int i = nativeImage.getWidth();
            int j = nativeImage.getHeight();
            long[] ls = new long[i * j];

            for (int k = 0; k < j; k++) {
               for (int l = 0; l < i; l++) {
                  int m = nativeImage.getPixel(l, k);
                  if (isCellEmpty(m)) {
                     ls[l + k * i] = 0L;
                  } else {
                     boolean bl = isCellEmpty(nativeImage.getPixel(l, Math.floorMod(k - 1, j)));
                     boolean bl2 = isCellEmpty(nativeImage.getPixel(Math.floorMod(l + 1, j), k));
                     boolean bl3 = isCellEmpty(nativeImage.getPixel(l, Math.floorMod(k + 1, j)));
                     boolean bl4 = isCellEmpty(nativeImage.getPixel(Math.floorMod(l - 1, j), k));
                     ls[l + k * i] = packCellData(m, bl, bl2, bl3, bl4);
                  }
               }
            }

            var20 = Optional.of(new CloudRenderer.TextureData(ls, i, j));
         }

         return var20;
      } catch (IOException var19) {
         LOGGER.error("Failed to load cloud texture", var19);
         return Optional.empty();
      }
   }

   private static int getSizeForCloudDistance(int cloudDistance) {
      int i = 4;
      int j = (cloudDistance + 1) * 2 * (cloudDistance + 1) * 2 / 2;
      int k = j * 4 + 54;
      return k * 3;
   }

   protected void apply(Optional<CloudRenderer.TextureData> optional, ResourceManager resourceManager, ProfilerFiller profilerFiller) {
      this.texture = optional.orElse(null);
      this.needsRebuild = true;
   }

   private static boolean isCellEmpty(int color) {
      return ARGB.alpha(color) < 10;
   }

   private static long packCellData(int color, boolean northEmpty, boolean eastEmpty, boolean southEmpty, boolean westEmpty) {
      return (long)color << 4 | (northEmpty ? 1 : 0) << 3 | (eastEmpty ? 1 : 0) << 2 | (southEmpty ? 1 : 0) << 1 | (westEmpty ? 1 : 0) << 0;
   }

   private static boolean isNorthEmpty(long cellData) {
      return (cellData >> 3 & 1L) != 0L;
   }

   private static boolean isEastEmpty(long cellData) {
      return (cellData >> 2 & 1L) != 0L;
   }

   private static boolean isSouthEmpty(long cellData) {
      return (cellData >> 1 & 1L) != 0L;
   }

   private static boolean isWestEmpty(long cellData) {
      return (cellData >> 0 & 1L) != 0L;
   }

   public void render(int color, CloudStatus status, float height, Vec3 cameraPosition, long ticks, float partialTick) {
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

         float f = (float)(height - cameraPosition.y);
         float g = f + 4.0F;
         CloudRenderer.RelativeCameraPos relativeCameraPos;
         if (g < 0.0F) {
            relativeCameraPos = CloudRenderer.RelativeCameraPos.ABOVE_CLOUDS;
         } else if (f > 0.0F) {
            relativeCameraPos = CloudRenderer.RelativeCameraPos.BELOW_CLOUDS;
         } else {
            relativeCameraPos = CloudRenderer.RelativeCameraPos.INSIDE_CLOUDS;
         }

         float h = (float)(ticks % (this.texture.width * 400L)) + partialTick;
         double d = cameraPosition.x + h * 0.030000001F;
         double e = cameraPosition.z + 3.96F;
         double l = this.texture.width * 12.0;
         double m = this.texture.height * 12.0;
         d -= Mth.floor(d / l) * l;
         e -= Mth.floor(e / m) * m;
         int n = Mth.floor(d / 12.0);
         int o = Mth.floor(e / 12.0);
         float p = (float)(d - n * 12.0F);
         float q = (float)(e - o * 12.0F);
         boolean bl = status == CloudStatus.FANCY;
         RenderPipeline renderPipeline = bl ? RenderPipelines.CLOUDS : RenderPipelines.FLAT_CLOUDS;
         if (this.needsRebuild || n != this.prevCellX || o != this.prevCellZ || relativeCameraPos != this.prevRelativeCameraPos || status != this.prevType) {
            this.needsRebuild = false;
            this.prevCellX = n;
            this.prevCellZ = o;
            this.prevRelativeCameraPos = relativeCameraPos;
            this.prevType = status;
            this.utb.rotate();

            try (GpuBuffer.MappedView mappedView = RenderSystem.getDevice().createCommandEncoder().mapBuffer(this.utb.currentBuffer(), false, true)) {
               this.buildMesh(relativeCameraPos, mappedView.data(), n, o, bl, j);
               this.quadCount = mappedView.data().position() / 3;
            }
         }

         if (this.quadCount != 0) {
            try (GpuBuffer.MappedView mappedView = RenderSystem.getDevice().createCommandEncoder().mapBuffer(this.ubo.currentBuffer(), false, true)) {
               Std140Builder.intoBuffer(mappedView.data()).putVec4(ARGB.vector4fFromARGB32(color)).putVec3(-p, f, -q).putVec3(12.0F, 4.0F, 12.0F);
            }

            GpuBufferSlice gpuBufferSlice = RenderSystem.getDynamicUniforms()
               .writeTransform(RenderSystem.getModelViewMatrix(), new Vector4f(1.0F, 1.0F, 1.0F, 1.0F), new Vector3f(), new Matrix4f());
            RenderTarget renderTarget = Minecraft.getInstance().getMainRenderTarget();
            RenderTarget renderTarget2 = Minecraft.getInstance().levelRenderer.getCloudsTarget();
            RenderSystem.AutoStorageIndexBuffer autoStorageIndexBuffer = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);
            GpuBuffer gpuBuffer = autoStorageIndexBuffer.getBuffer(6 * this.quadCount);
            GpuTextureView gpuTextureView;
            GpuTextureView gpuTextureView2;
            if (renderTarget2 != null) {
               gpuTextureView = renderTarget2.getColorTextureView();
               gpuTextureView2 = renderTarget2.getDepthTextureView();
            } else {
               gpuTextureView = renderTarget.getColorTextureView();
               gpuTextureView2 = renderTarget.getDepthTextureView();
            }

            try (RenderPass renderPass = RenderSystem.getDevice()
                  .createCommandEncoder()
                  .createRenderPass(() -> "Clouds", gpuTextureView, OptionalInt.empty(), gpuTextureView2, OptionalDouble.empty())) {
               renderPass.setPipeline(renderPipeline);
               RenderSystem.bindDefaultUniforms(renderPass);
               renderPass.setUniform("DynamicTransforms", gpuBufferSlice);
               renderPass.setIndexBuffer(gpuBuffer, autoStorageIndexBuffer.type());
               renderPass.setUniform("CloudInfo", this.ubo.currentBuffer());
               renderPass.setUniform("CloudFaces", this.utb.currentBuffer());
               renderPass.drawIndexed(0, 0, 6 * this.quadCount, 1);
            }
         }
      }
   }

   private void buildMesh(CloudRenderer.RelativeCameraPos relativeCameraPos, ByteBuffer buffer, int cellX, int cellZ, boolean fancyClouds, int size) {
      if (this.texture != null) {
         long[] ls = this.texture.cells;
         int i = this.texture.width;
         int j = this.texture.height;

         for (int k = 0; k <= 2 * size; k++) {
            for (int l = -k; l <= k; l++) {
               int m = k - Math.abs(l);
               if (m >= 0 && m <= size && l * l + m * m <= size * size) {
                  if (m != 0) {
                     this.tryBuildCell(relativeCameraPos, buffer, cellX, cellZ, fancyClouds, l, i, -m, j, ls);
                  }

                  this.tryBuildCell(relativeCameraPos, buffer, cellX, cellZ, fancyClouds, l, i, m, j, ls);
               }
            }
         }
      }
   }

   private void tryBuildCell(
      CloudRenderer.RelativeCameraPos relativeCameraPos,
      ByteBuffer buffer,
      int cellX,
      int cellZ,
      boolean fancyClouds,
      int x,
      int width,
      int z,
      int height,
      long[] cells
   ) {
      int i = Math.floorMod(cellX + x, width);
      int j = Math.floorMod(cellZ + z, height);
      long l = cells[i + j * width];
      if (l != 0L) {
         if (fancyClouds) {
            this.buildExtrudedCell(relativeCameraPos, buffer, x, z, l);
         } else {
            this.buildFlatCell(buffer, x, z);
         }
      }
   }

   private void buildFlatCell(ByteBuffer buffer, int cellX, int cellZ) {
      this.encodeFace(buffer, cellX, cellZ, Direction.DOWN, 32);
   }

   private void encodeFace(ByteBuffer buffer, int cellX, int cellZ, Direction face, int offset) {
      int i = face.get3DDataValue() | offset;
      i |= (cellX & 1) << 7;
      i |= (cellZ & 1) << 6;
      buffer.put((byte)(cellX >> 1)).put((byte)(cellZ >> 1)).put((byte)i);
   }

   private void buildExtrudedCell(CloudRenderer.RelativeCameraPos relativeCameraPos, ByteBuffer buffer, int cellX, int cellZ, long cellData) {
      if (relativeCameraPos != CloudRenderer.RelativeCameraPos.BELOW_CLOUDS) {
         this.encodeFace(buffer, cellX, cellZ, Direction.UP, 0);
      }

      if (relativeCameraPos != CloudRenderer.RelativeCameraPos.ABOVE_CLOUDS) {
         this.encodeFace(buffer, cellX, cellZ, Direction.DOWN, 0);
      }

      if (isNorthEmpty(cellData) && cellZ > 0) {
         this.encodeFace(buffer, cellX, cellZ, Direction.NORTH, 0);
      }

      if (isSouthEmpty(cellData) && cellZ < 0) {
         this.encodeFace(buffer, cellX, cellZ, Direction.SOUTH, 0);
      }

      if (isWestEmpty(cellData) && cellX > 0) {
         this.encodeFace(buffer, cellX, cellZ, Direction.WEST, 0);
      }

      if (isEastEmpty(cellData) && cellX < 0) {
         this.encodeFace(buffer, cellX, cellZ, Direction.EAST, 0);
      }

      boolean bl = Math.abs(cellX) <= 1 && Math.abs(cellZ) <= 1;
      if (bl) {
         for (Direction direction : Direction.values()) {
            this.encodeFace(buffer, cellX, cellZ, direction, 16);
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

   @Environment(EnvType.CLIENT)
   static enum RelativeCameraPos {
      ABOVE_CLOUDS,
      INSIDE_CLOUDS,
      BELOW_CLOUDS;
   }

   @Environment(EnvType.CLIENT)
   public record TextureData(long[] cells, int width, int height) {
   }
}
