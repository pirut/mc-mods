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
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

@OnlyIn(Dist.CLIENT)
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
      WorldBorderRenderState p_451068_, double p_401863_, double p_401925_, double p_401782_, float p_401880_, float p_401871_, float p_401854_
   ) {
      try (ByteBufferBuilder bytebufferbuilder = ByteBufferBuilder.exactlySized(DefaultVertexFormat.POSITION_TEX.getVertexSize() * 4 * 4)) {
         double d0 = p_451068_.minX;
         double d1 = p_451068_.maxX;
         double d2 = p_451068_.minZ;
         double d3 = p_451068_.maxZ;
         double d4 = Math.max((double)Mth.floor(p_401925_ - p_401863_), d2);
         double d5 = Math.min((double)Mth.ceil(p_401925_ + p_401863_), d3);
         float f = (Mth.floor(d4) & 1) * 0.5F;
         float f1 = (float)(d5 - d4) / 2.0F;
         double d6 = Math.max((double)Mth.floor(p_401782_ - p_401863_), d0);
         double d7 = Math.min((double)Mth.ceil(p_401782_ + p_401863_), d1);
         float f2 = (Mth.floor(d6) & 1) * 0.5F;
         float f3 = (float)(d7 - d6) / 2.0F;
         BufferBuilder bufferbuilder = new BufferBuilder(bytebufferbuilder, VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
         bufferbuilder.addVertex(0.0F, -p_401880_, (float)(d3 - d4)).setUv(f2, p_401871_);
         bufferbuilder.addVertex((float)(d7 - d6), -p_401880_, (float)(d3 - d4)).setUv(f3 + f2, p_401871_);
         bufferbuilder.addVertex((float)(d7 - d6), p_401880_, (float)(d3 - d4)).setUv(f3 + f2, p_401854_);
         bufferbuilder.addVertex(0.0F, p_401880_, (float)(d3 - d4)).setUv(f2, p_401854_);
         bufferbuilder.addVertex(0.0F, -p_401880_, 0.0F).setUv(f, p_401871_);
         bufferbuilder.addVertex(0.0F, -p_401880_, (float)(d5 - d4)).setUv(f1 + f, p_401871_);
         bufferbuilder.addVertex(0.0F, p_401880_, (float)(d5 - d4)).setUv(f1 + f, p_401854_);
         bufferbuilder.addVertex(0.0F, p_401880_, 0.0F).setUv(f, p_401854_);
         bufferbuilder.addVertex((float)(d7 - d6), -p_401880_, 0.0F).setUv(f2, p_401871_);
         bufferbuilder.addVertex(0.0F, -p_401880_, 0.0F).setUv(f3 + f2, p_401871_);
         bufferbuilder.addVertex(0.0F, p_401880_, 0.0F).setUv(f3 + f2, p_401854_);
         bufferbuilder.addVertex((float)(d7 - d6), p_401880_, 0.0F).setUv(f2, p_401854_);
         bufferbuilder.addVertex((float)(d1 - d6), -p_401880_, (float)(d5 - d4)).setUv(f, p_401871_);
         bufferbuilder.addVertex((float)(d1 - d6), -p_401880_, 0.0F).setUv(f1 + f, p_401871_);
         bufferbuilder.addVertex((float)(d1 - d6), p_401880_, 0.0F).setUv(f1 + f, p_401854_);
         bufferbuilder.addVertex((float)(d1 - d6), p_401880_, (float)(d5 - d4)).setUv(f, p_401854_);

         try (MeshData meshdata = bufferbuilder.buildOrThrow()) {
            RenderSystem.getDevice().createCommandEncoder().writeToBuffer(this.worldBorderBuffer.slice(), meshdata.vertexBuffer());
         }

         this.lastBorderMinX = d0;
         this.lastBorderMaxX = d1;
         this.lastBorderMinZ = d2;
         this.lastBorderMaxZ = d3;
         this.lastMinX = d6;
         this.lastMinZ = d4;
         this.needsRebuild = false;
      }
   }

   public void extract(WorldBorder p_451506_, float p_459151_, Vec3 p_451107_, double p_451423_, WorldBorderRenderState p_451461_) {
      p_451461_.minX = p_451506_.getMinX(p_459151_);
      p_451461_.maxX = p_451506_.getMaxX(p_459151_);
      p_451461_.minZ = p_451506_.getMinZ(p_459151_);
      p_451461_.maxZ = p_451506_.getMaxZ(p_459151_);
      if ((
            !(p_451107_.x < p_451461_.maxX - p_451423_)
               || !(p_451107_.x > p_451461_.minX + p_451423_)
               || !(p_451107_.z < p_451461_.maxZ - p_451423_)
               || !(p_451107_.z > p_451461_.minZ + p_451423_)
         )
         && !(p_451107_.x < p_451461_.minX - p_451423_)
         && !(p_451107_.x > p_451461_.maxX + p_451423_)
         && !(p_451107_.z < p_451461_.minZ - p_451423_)
         && !(p_451107_.z > p_451461_.maxZ + p_451423_)) {
         p_451461_.alpha = 1.0 - p_451506_.getDistanceToBorder(p_451107_.x, p_451107_.z) / p_451423_;
         p_451461_.alpha = Math.pow(p_451461_.alpha, 4.0);
         p_451461_.alpha = Mth.clamp(p_451461_.alpha, 0.0, 1.0);
         p_451461_.tint = p_451506_.getStatus().getColor();
      } else {
         p_451461_.alpha = 0.0;
      }
   }

   public void render(WorldBorderRenderState p_451527_, Vec3 p_360961_, double p_364707_, double p_361253_) {
      if (!(p_451527_.alpha <= 0.0)) {
         double d0 = p_360961_.x;
         double d1 = p_360961_.z;
         float f = (float)p_361253_;
         float f1 = ARGB.red(p_451527_.tint) / 255.0F;
         float f2 = ARGB.green(p_451527_.tint) / 255.0F;
         float f3 = ARGB.blue(p_451527_.tint) / 255.0F;
         float f4 = (float)(Util.getMillis() % 3000L) / 3000.0F;
         float f5 = (float)(-Mth.frac(p_360961_.y * 0.5));
         float f6 = f5 + f;
         if (this.shouldRebuildWorldBorderBuffer(p_451527_)) {
            this.rebuildWorldBorderBuffer(p_451527_, p_364707_, d1, d0, f, f6, f5);
         }

         TextureManager texturemanager = Minecraft.getInstance().getTextureManager();
         AbstractTexture abstracttexture = texturemanager.getTexture(FORCEFIELD_LOCATION);
         RenderPipeline renderpipeline = RenderPipelines.WORLD_BORDER;
         RenderTarget rendertarget = Minecraft.getInstance().getMainRenderTarget();
         RenderTarget rendertarget1 = Minecraft.getInstance().levelRenderer.getWeatherTarget();
         GpuTextureView gputextureview;
         GpuTextureView gputextureview1;
         if (rendertarget1 != null) {
            gputextureview = rendertarget1.getColorTextureView();
            gputextureview1 = rendertarget1.getDepthTextureView();
         } else {
            gputextureview = rendertarget.getColorTextureView();
            gputextureview1 = rendertarget.getDepthTextureView();
         }

         GpuBuffer gpubuffer = this.indices.getBuffer(6);
         GpuBufferSlice gpubufferslice = RenderSystem.getDynamicUniforms()
            .writeTransform(
               RenderSystem.getModelViewMatrix(),
               new Vector4f(f1, f2, f3, (float)p_451527_.alpha),
               new Vector3f((float)(this.lastMinX - d0), (float)(-p_360961_.y), (float)(this.lastMinZ - d1)),
               new Matrix4f().translation(f4, f4, 0.0F)
            );

         try (RenderPass renderpass = RenderSystem.getDevice()
               .createCommandEncoder()
               .createRenderPass(() -> "World border", gputextureview, OptionalInt.empty(), gputextureview1, OptionalDouble.empty())) {
            renderpass.setPipeline(renderpipeline);
            RenderSystem.bindDefaultUniforms(renderpass);
            renderpass.setUniform("DynamicTransforms", gpubufferslice);
            renderpass.setIndexBuffer(gpubuffer, this.indices.type());
            renderpass.bindTexture("Sampler0", abstracttexture.getTextureView(), abstracttexture.getSampler());
            renderpass.setVertexBuffer(0, this.worldBorderBuffer);
            ArrayList<RenderPass.Draw<WorldBorderRenderer>> arraylist = new ArrayList<>();

            for (WorldBorderRenderState.DistancePerDirection worldborderrenderstate$distanceperdirection : p_451527_.closestBorder(d0, d1)) {
               if (worldborderrenderstate$distanceperdirection.distance() < p_364707_) {
                  int i = worldborderrenderstate$distanceperdirection.direction().get2DDataValue();
                  arraylist.add(new RenderPass.Draw<>(0, this.worldBorderBuffer, gpubuffer, this.indices.type(), 6 * i, 6));
               }
            }

            renderpass.drawMultipleIndexed(arraylist, null, null, Collections.emptyList(), this);
         }
      }
   }

   public void invalidate() {
      this.needsRebuild = true;
   }

   private boolean shouldRebuildWorldBorderBuffer(WorldBorderRenderState p_451029_) {
      return this.needsRebuild
         || p_451029_.minX != this.lastBorderMinX
         || p_451029_.minZ != this.lastBorderMinZ
         || p_451029_.maxX != this.lastBorderMaxX
         || p_451029_.maxZ != this.lastBorderMaxZ;
   }
}
