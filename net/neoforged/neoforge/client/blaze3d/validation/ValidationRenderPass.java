package net.neoforged.neoforge.client.blaze3d.validation;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.Collection;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;

public class ValidationRenderPass implements RenderPass {
   private final RenderPass realRenderPass;
   private final GpuDeviceUsageValidator validator;

   protected ValidationRenderPass(RenderPass realRenderPass, GpuDeviceUsageValidator validator) {
      this.realRenderPass = realRenderPass;
      this.validator = validator;
   }

   @Override
   public void pushDebugGroup(Supplier<String> label) {
      this.realRenderPass.pushDebugGroup(label);
   }

   @Override
   public void popDebugGroup() {
      this.realRenderPass.popDebugGroup();
   }

   @Override
   public void setPipeline(RenderPipeline pipeline) {
      this.validator.validatePipeline(pipeline);
      this.realRenderPass.setPipeline(pipeline);
   }

   @Override
   public void bindTexture(String name, @Nullable GpuTextureView textureView, @Nullable GpuSampler sampler) {
      if (textureView instanceof ValidationGpuTextureView validationTextureView) {
         textureView = validationTextureView.getRealTextureView();
      }

      this.realRenderPass.bindTexture(name, textureView, sampler);
   }

   @Override
   public void setUniform(String name, GpuBuffer buffer) {
      this.realRenderPass.setUniform(name, buffer);
   }

   @Override
   public void setUniform(String name, GpuBufferSlice bufferSlice) {
      this.realRenderPass.setUniform(name, bufferSlice);
   }

   @Override
   public void setViewport(int x, int y, int width, int height) {
      this.realRenderPass.setViewport(x, y, width, height);
   }

   @Override
   public void enableScissor(int x, int y, int width, int height) {
      this.realRenderPass.enableScissor(x, y, width, height);
   }

   @Override
   public void disableScissor() {
      this.realRenderPass.disableScissor();
   }

   @Override
   public void setVertexBuffer(int index, GpuBuffer vertexBuffer) {
      this.realRenderPass.setVertexBuffer(index, vertexBuffer);
   }

   @Override
   public void setIndexBuffer(GpuBuffer indexBuffer, VertexFormat.IndexType indexType) {
      this.realRenderPass.setIndexBuffer(indexBuffer, indexType);
   }

   @Override
   public void drawIndexed(int vertexOffset, int firstIndex, int indexCount, int instanceCount) {
      this.realRenderPass.drawIndexed(vertexOffset, firstIndex, indexCount, instanceCount);
   }

   @Override
   public <T> void drawMultipleIndexed(
      Collection<RenderPass.Draw<T>> draws,
      @Nullable GpuBuffer indexBuffer,
      @Nullable VertexFormat.IndexType indexType,
      Collection<String> dynamicUniforms,
      T userData
   ) {
      this.realRenderPass.drawMultipleIndexed(draws, indexBuffer, indexType, dynamicUniforms, userData);
   }

   @Override
   public void draw(int firstVertex, int vertexCount) {
      this.realRenderPass.draw(firstVertex, vertexCount);
   }

   @Override
   public void close() {
      this.realRenderPass.close();
   }
}
