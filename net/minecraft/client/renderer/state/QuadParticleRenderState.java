package net.minecraft.client.renderer.state;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.feature.ParticleFeatureRenderer;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class QuadParticleRenderState implements SubmitNodeCollector.ParticleGroupRenderer, ParticleGroupRenderState {
   private static final int INITIAL_PARTICLE_CAPACITY = 1024;
   private static final int FLOATS_PER_PARTICLE = 12;
   private static final int INTS_PER_PARTICLE = 2;
   private final Map<SingleQuadParticle.Layer, QuadParticleRenderState.Storage> particles = new HashMap<>();
   private int particleCount;

   public void add(
      SingleQuadParticle.Layer p_451483_,
      float p_450934_,
      float p_451032_,
      float p_451083_,
      float p_450951_,
      float p_451534_,
      float p_451303_,
      float p_451113_,
      float p_451108_,
      float p_451084_,
      float p_451110_,
      float p_451263_,
      float p_450958_,
      int p_451513_,
      int p_451575_
   ) {
      this.particles
         .computeIfAbsent(p_451483_, p_451410_ -> new QuadParticleRenderState.Storage())
         .add(
            p_450934_,
            p_451032_,
            p_451083_,
            p_450951_,
            p_451534_,
            p_451303_,
            p_451113_,
            p_451108_,
            p_451084_,
            p_451110_,
            p_451263_,
            p_450958_,
            p_451513_,
            p_451575_
         );
      this.particleCount++;
   }

   @Override
   public void clear() {
      this.particles.values().forEach(QuadParticleRenderState.Storage::clear);
      this.particleCount = 0;
   }

   @Nullable
   @Override
   public QuadParticleRenderState.PreparedBuffers prepare(ParticleFeatureRenderer.ParticleBufferCache p_451288_) {
      int i = this.particleCount * 4;

      Object object;
      try (ByteBufferBuilder bytebufferbuilder = ByteBufferBuilder.exactlySized(i * DefaultVertexFormat.PARTICLE.getVertexSize())) {
         BufferBuilder bufferbuilder = new BufferBuilder(bytebufferbuilder, VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
         Map<SingleQuadParticle.Layer, QuadParticleRenderState.PreparedLayer> map = new HashMap<>();
         int j = 0;

         for (Entry<SingleQuadParticle.Layer, QuadParticleRenderState.Storage> entry : this.particles.entrySet()) {
            entry.getValue()
               .forEachParticle(
                  (p_450929_, p_451248_, p_451307_, p_451411_, p_451517_, p_451115_, p_451484_, p_451330_, p_451081_, p_451392_, p_451178_, p_451023_, p_451176_, p_451252_) -> this.renderRotatedQuad(
                     bufferbuilder,
                     p_450929_,
                     p_451248_,
                     p_451307_,
                     p_451411_,
                     p_451517_,
                     p_451115_,
                     p_451484_,
                     p_451330_,
                     p_451081_,
                     p_451392_,
                     p_451178_,
                     p_451023_,
                     p_451176_,
                     p_451252_
                  )
               );
            if (entry.getValue().count() > 0) {
               map.put(entry.getKey(), new QuadParticleRenderState.PreparedLayer(j, entry.getValue().count() * 6));
            }

            j += entry.getValue().count() * 4;
         }

         MeshData meshdata = bufferbuilder.build();
         if (meshdata != null) {
            p_451288_.write(meshdata.vertexBuffer());
            RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS).getBuffer(meshdata.drawState().indexCount());
            GpuBufferSlice gpubufferslice = RenderSystem.getDynamicUniforms()
               .writeTransform(RenderSystem.getModelViewMatrix(), new Vector4f(1.0F, 1.0F, 1.0F, 1.0F), new Vector3f(), new Matrix4f());
            return new QuadParticleRenderState.PreparedBuffers(meshdata.drawState().indexCount(), gpubufferslice, map);
         }

         object = null;
      }

      return (QuadParticleRenderState.PreparedBuffers)object;
   }

   @Override
   public void render(
      QuadParticleRenderState.PreparedBuffers p_451476_,
      ParticleFeatureRenderer.ParticleBufferCache p_451364_,
      RenderPass p_451026_,
      TextureManager p_450917_,
      boolean p_450993_
   ) {
      RenderSystem.AutoStorageIndexBuffer rendersystem$autostorageindexbuffer = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);
      p_451026_.setVertexBuffer(0, p_451364_.get());
      p_451026_.setIndexBuffer(rendersystem$autostorageindexbuffer.getBuffer(p_451476_.indexCount), rendersystem$autostorageindexbuffer.type());
      p_451026_.setUniform("DynamicTransforms", p_451476_.dynamicTransforms);

      for (Entry<SingleQuadParticle.Layer, QuadParticleRenderState.PreparedLayer> entry : p_451476_.layers.entrySet()) {
         if (p_450993_ == entry.getKey().translucent()) {
            p_451026_.setPipeline(entry.getKey().pipeline());
            AbstractTexture abstracttexture = p_450917_.getTexture(entry.getKey().textureAtlasLocation());
            p_451026_.bindTexture("Sampler0", abstracttexture.getTextureView(), abstracttexture.getSampler());
            p_451026_.drawIndexed(entry.getValue().vertexOffset, 0, entry.getValue().indexCount, 1);
         }
      }
   }

   protected void renderRotatedQuad(
      VertexConsumer p_451037_,
      float p_451323_,
      float p_451350_,
      float p_451241_,
      float p_451326_,
      float p_451182_,
      float p_451048_,
      float p_450939_,
      float p_451515_,
      float p_451558_,
      float p_451343_,
      float p_451453_,
      float p_451552_,
      int p_451266_,
      int p_451033_
   ) {
      Quaternionf quaternionf = new Quaternionf(p_451326_, p_451182_, p_451048_, p_450939_);
      this.renderVertex(p_451037_, quaternionf, p_451323_, p_451350_, p_451241_, 1.0F, -1.0F, p_451515_, p_451343_, p_451552_, p_451266_, p_451033_);
      this.renderVertex(p_451037_, quaternionf, p_451323_, p_451350_, p_451241_, 1.0F, 1.0F, p_451515_, p_451343_, p_451453_, p_451266_, p_451033_);
      this.renderVertex(p_451037_, quaternionf, p_451323_, p_451350_, p_451241_, -1.0F, 1.0F, p_451515_, p_451558_, p_451453_, p_451266_, p_451033_);
      this.renderVertex(p_451037_, quaternionf, p_451323_, p_451350_, p_451241_, -1.0F, -1.0F, p_451515_, p_451558_, p_451552_, p_451266_, p_451033_);
   }

   private void renderVertex(
      VertexConsumer p_451429_,
      Quaternionf p_451152_,
      float p_451154_,
      float p_451510_,
      float p_451099_,
      float p_451333_,
      float p_450981_,
      float p_451389_,
      float p_450933_,
      float p_451000_,
      int p_451498_,
      int p_451079_
   ) {
      Vector3f vector3f = new Vector3f(p_451333_, p_450981_, 0.0F).rotate(p_451152_).mul(p_451389_).add(p_451154_, p_451510_, p_451099_);
      p_451429_.addVertex(vector3f.x(), vector3f.y(), vector3f.z()).setUv(p_450933_, p_451000_).setColor(p_451498_).setLight(p_451079_);
   }

   @Override
   public void submit(SubmitNodeCollector p_451442_, CameraRenderState p_451044_) {
      if (this.particleCount > 0) {
         p_451442_.submitParticleGroup(this);
      }
   }

   @FunctionalInterface
   @OnlyIn(Dist.CLIENT)
   public interface ParticleConsumer {
      void consume(
         float var1,
         float var2,
         float var3,
         float var4,
         float var5,
         float var6,
         float var7,
         float var8,
         float var9,
         float var10,
         float var11,
         float var12,
         int var13,
         int var14
      );
   }

   @OnlyIn(Dist.CLIENT)
   public record PreparedBuffers(int indexCount, GpuBufferSlice dynamicTransforms, Map<SingleQuadParticle.Layer, QuadParticleRenderState.PreparedLayer> layers) {
   }

   @OnlyIn(Dist.CLIENT)
   public record PreparedLayer(int vertexOffset, int indexCount) {
   }

   @OnlyIn(Dist.CLIENT)
   static class Storage {
      private int capacity = 1024;
      private float[] floatValues = new float[12288];
      private int[] intValues = new int[2048];
      private int currentParticleIndex;

      public void add(
         float p_450943_,
         float p_451507_,
         float p_451576_,
         float p_451070_,
         float p_451062_,
         float p_450982_,
         float p_451425_,
         float p_450930_,
         float p_451128_,
         float p_451432_,
         float p_451162_,
         float p_451492_,
         int p_451573_,
         int p_451279_
      ) {
         if (this.currentParticleIndex >= this.capacity) {
            this.grow();
         }

         int i = this.currentParticleIndex * 12;
         this.floatValues[i++] = p_450943_;
         this.floatValues[i++] = p_451507_;
         this.floatValues[i++] = p_451576_;
         this.floatValues[i++] = p_451070_;
         this.floatValues[i++] = p_451062_;
         this.floatValues[i++] = p_450982_;
         this.floatValues[i++] = p_451425_;
         this.floatValues[i++] = p_450930_;
         this.floatValues[i++] = p_451128_;
         this.floatValues[i++] = p_451432_;
         this.floatValues[i++] = p_451162_;
         this.floatValues[i] = p_451492_;
         i = this.currentParticleIndex * 2;
         this.intValues[i++] = p_451573_;
         this.intValues[i] = p_451279_;
         this.currentParticleIndex++;
      }

      public void forEachParticle(QuadParticleRenderState.ParticleConsumer p_451049_) {
         for (int i = 0; i < this.currentParticleIndex; i++) {
            int j = i * 12;
            int k = i * 2;
            p_451049_.consume(
               this.floatValues[j++],
               this.floatValues[j++],
               this.floatValues[j++],
               this.floatValues[j++],
               this.floatValues[j++],
               this.floatValues[j++],
               this.floatValues[j++],
               this.floatValues[j++],
               this.floatValues[j++],
               this.floatValues[j++],
               this.floatValues[j++],
               this.floatValues[j],
               this.intValues[k++],
               this.intValues[k]
            );
         }
      }

      public void clear() {
         this.currentParticleIndex = 0;
      }

      private void grow() {
         this.capacity *= 2;
         this.floatValues = Arrays.copyOf(this.floatValues, this.capacity * 12);
         this.intValues = Arrays.copyOf(this.intValues, this.capacity * 2);
      }

      public int count() {
         return this.currentParticleIndex;
      }
   }
}
