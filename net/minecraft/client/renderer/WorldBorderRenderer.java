package net.minecraft.client.renderer;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.state.WorldBorderRenderState;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

@Environment(EnvType.CLIENT)
public class WorldBorderRenderer {
   public static final Identifier FORCEFIELD_LOCATION = Identifier.withDefaultNamespace("textures/misc/forcefield.png");
   private boolean needsRebuild = true;
   private double lastMinX;
   private double lastMinZ;
   private double lastBorderMinX;
   private double lastBorderMaxX;
   private double lastBorderMinZ;
   private double lastBorderMaxZ;
   private final GpuBuffer worldBorderBuffer = RenderSystem.getDevice()
      .createBuffer(() -> "World border vertex buffer", 40, 16L * DefaultVertexFormat.POSITION_TEX.getVertexSize());
   private final RenderSystem.AutoStorageIndexBuffer indices = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);

   private void rebuildWorldBorderBuffer(
      WorldBorderRenderState renderState, double renderDistance, double camZ, double camX, float depthFar, float vBottom, float vTop
   ) {
      try (ByteBufferBuilder byteBufferBuilder = ByteBufferBuilder.exactlySized(DefaultVertexFormat.POSITION_TEX.getVertexSize() * 4 * 4)) {
         double d = renderState.minX;
         double e = renderState.maxX;
         double f = renderState.minZ;
         double g = renderState.maxZ;
         double h = Math.max((double)Mth.floor(camZ - renderDistance), f);
         double i = Math.min((double)Mth.ceil(camZ + renderDistance), g);
         float j = (Mth.floor(h) & 1) * 0.5F;
         float k = (float)(i - h) / 2.0F;
         double l = Math.max((double)Mth.floor(camX - renderDistance), d);
         double m = Math.min((double)Mth.ceil(camX + renderDistance), e);
         float n = (Mth.floor(l) & 1) * 0.5F;
         float o = (float)(m - l) / 2.0F;
         BufferBuilder bufferBuilder = new BufferBuilder(byteBufferBuilder, VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
         bufferBuilder.addVertex(0.0F, -depthFar, (float)(g - h)).setUv(n, vBottom);
         bufferBuilder.addVertex((float)(m - l), -depthFar, (float)(g - h)).setUv(o + n, vBottom);
         bufferBuilder.addVertex((float)(m - l), depthFar, (float)(g - h)).setUv(o + n, vTop);
         bufferBuilder.addVertex(0.0F, depthFar, (float)(g - h)).setUv(n, vTop);
         bufferBuilder.addVertex(0.0F, -depthFar, 0.0F).setUv(j, vBottom);
         bufferBuilder.addVertex(0.0F, -depthFar, (float)(i - h)).setUv(k + j, vBottom);
         bufferBuilder.addVertex(0.0F, depthFar, (float)(i - h)).setUv(k + j, vTop);
         bufferBuilder.addVertex(0.0F, depthFar, 0.0F).setUv(j, vTop);
         bufferBuilder.addVertex((float)(m - l), -depthFar, 0.0F).setUv(n, vBottom);
         bufferBuilder.addVertex(0.0F, -depthFar, 0.0F).setUv(o + n, vBottom);
         bufferBuilder.addVertex(0.0F, depthFar, 0.0F).setUv(o + n, vTop);
         bufferBuilder.addVertex((float)(m - l), depthFar, 0.0F).setUv(n, vTop);
         bufferBuilder.addVertex((float)(e - l), -depthFar, (float)(i - h)).setUv(j, vBottom);
         bufferBuilder.addVertex((float)(e - l), -depthFar, 0.0F).setUv(k + j, vBottom);
         bufferBuilder.addVertex((float)(e - l), depthFar, 0.0F).setUv(k + j, vTop);
         bufferBuilder.addVertex((float)(e - l), depthFar, (float)(i - h)).setUv(j, vTop);

         try (MeshData meshData = bufferBuilder.buildOrThrow()) {
            RenderSystem.getDevice().createCommandEncoder().writeToBuffer(this.worldBorderBuffer.slice(), meshData.vertexBuffer());
         }

         this.lastBorderMinX = d;
         this.lastBorderMaxX = e;
         this.lastBorderMinZ = f;
         this.lastBorderMaxZ = g;
         this.lastMinX = l;
         this.lastMinZ = h;
         this.needsRebuild = false;
      }
   }

   public void extract(WorldBorder worldBorder, float partialTick, Vec3 cameraPosition, double renderDistance, WorldBorderRenderState reusedState) {
      reusedState.minX = worldBorder.getMinX(partialTick);
      reusedState.maxX = worldBorder.getMaxX(partialTick);
      reusedState.minZ = worldBorder.getMinZ(partialTick);
      reusedState.maxZ = worldBorder.getMaxZ(partialTick);
      if ((
            !(cameraPosition.x < reusedState.maxX - renderDistance)
               || !(cameraPosition.x > reusedState.minX + renderDistance)
               || !(cameraPosition.z < reusedState.maxZ - renderDistance)
               || !(cameraPosition.z > reusedState.minZ + renderDistance)
         )
         && !(cameraPosition.x < reusedState.minX - renderDistance)
         && !(cameraPosition.x > reusedState.maxX + renderDistance)
         && !(cameraPosition.z < reusedState.minZ - renderDistance)
         && !(cameraPosition.z > reusedState.maxZ + renderDistance)) {
         reusedState.alpha = 1.0 - worldBorder.getDistanceToBorder(cameraPosition.x, cameraPosition.z) / renderDistance;
         reusedState.alpha = Math.pow(reusedState.alpha, 4.0);
         reusedState.alpha = Mth.clamp(reusedState.alpha, 0.0, 1.0);
         reusedState.tint = worldBorder.getStatus().getColor();
      } else {
         reusedState.alpha = 0.0;
      }
   }

   public void render(WorldBorderRenderState renderState, Vec3 cameraPostion, double renderDistance, double depthFar) {
      if (!(renderState.alpha <= 0.0)) {
         double d = cameraPostion.x;
         double e = cameraPostion.z;
         float f = (float)depthFar;
         float g = ARGB.red(renderState.tint) / 255.0F;
         float h = ARGB.green(renderState.tint) / 255.0F;
         float i = ARGB.blue(renderState.tint) / 255.0F;
         float j = (float)(Util.getMillis() % 3000L) / 3000.0F;
         float k = (float)(-Mth.frac(cameraPostion.y * 0.5));
         float l = k + f;
         if (this.shouldRebuildWorldBorderBuffer(renderState)) {
            this.rebuildWorldBorderBuffer(renderState, renderDistance, e, d, f, l, k);
         }

         TextureManager textureManager = Minecraft.getInstance().getTextureManager();
         AbstractTexture abstractTexture = textureManager.getTexture(FORCEFIELD_LOCATION);
         RenderPipeline renderPipeline = RenderPipelines.WORLD_BORDER;
         RenderTarget renderTarget = Minecraft.getInstance().getMainRenderTarget();
         RenderTarget renderTarget2 = Minecraft.getInstance().levelRenderer.getWeatherTarget();
         GpuTextureView gpuTextureView;
         GpuTextureView gpuTextureView2;
         if (renderTarget2 != null) {
            gpuTextureView = renderTarget2.getColorTextureView();
            gpuTextureView2 = renderTarget2.getDepthTextureView();
         } else {
            gpuTextureView = renderTarget.getColorTextureView();
            gpuTextureView2 = renderTarget.getDepthTextureView();
         }

         GpuBuffer gpuBuffer = this.indices.getBuffer(6);
         GpuBufferSlice gpuBufferSlice = RenderSystem.getDynamicUniforms()
            .writeTransform(
               RenderSystem.getModelViewMatrix(),
               new Vector4f(g, h, i, (float)renderState.alpha),
               new Vector3f((float)(this.lastMinX - d), (float)(-cameraPostion.y), (float)(this.lastMinZ - e)),
               new Matrix4f().translation(j, j, 0.0F)
            );

         try (RenderPass renderPass = RenderSystem.getDevice()
               .createCommandEncoder()
               .createRenderPass(() -> "World border", gpuTextureView, OptionalInt.empty(), gpuTextureView2, OptionalDouble.empty())) {
            renderPass.setPipeline(renderPipeline);
            RenderSystem.bindDefaultUniforms(renderPass);
            renderPass.setUniform("DynamicTransforms", gpuBufferSlice);
            renderPass.setIndexBuffer(gpuBuffer, this.indices.type());
            renderPass.bindTexture("Sampler0", abstractTexture.getTextureView(), abstractTexture.getSampler());
            renderPass.setVertexBuffer(0, this.worldBorderBuffer);
            ArrayList<RenderPass.Draw<WorldBorderRenderer>> arrayList = new ArrayList<>();

            for (WorldBorderRenderState.DistancePerDirection distancePerDirection : renderState.closestBorder(d, e)) {
               if (distancePerDirection.distance() < renderDistance) {
                  int m = distancePerDirection.direction().get2DDataValue();
                  arrayList.add(new RenderPass.Draw<>(0, this.worldBorderBuffer, gpuBuffer, this.indices.type(), 6 * m, 6));
               }
            }

            renderPass.drawMultipleIndexed(arrayList, null, null, Collections.emptyList(), this);
         }
      }
   }

   public void invalidate() {
      this.needsRebuild = true;
   }

   private boolean shouldRebuildWorldBorderBuffer(WorldBorderRenderState renderState) {
      return this.needsRebuild
         || renderState.minX != this.lastBorderMinX
         || renderState.minZ != this.lastBorderMinZ
         || renderState.maxX != this.lastBorderMaxX
         || renderState.maxZ != this.lastBorderMaxZ;
   }
}
