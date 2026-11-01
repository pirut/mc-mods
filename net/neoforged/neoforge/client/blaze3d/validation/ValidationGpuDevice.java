package net.neoforged.neoforge.client.blaze3d.validation;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.CompiledRenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.shaders.ShaderSource;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.OptionalDouble;
import java.util.function.Supplier;
import net.neoforged.neoforge.client.blaze3d.GpuDeviceFeatures;
import net.neoforged.neoforge.client.blaze3d.GpuDeviceProperties;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jspecify.annotations.Nullable;

public class ValidationGpuDevice implements GpuDevice {
   protected final GpuDevice realDevice;
   protected final GpuDeviceUsageValidator validator;
   private final ValidationCommandEncoder validationCommandEncoder;

   public ValidationGpuDevice(GpuDevice realDevice, boolean checkReservedUsageBits) {
      this.realDevice = realDevice;
      this.validator = new GpuDeviceUsageValidator(this, checkReservedUsageBits);
      this.validationCommandEncoder = this.wrapCommandEncoder(realDevice.createCommandEncoder(), this.validator);
   }

   @Internal
   public GpuDevice getRealDevice() {
      return this.realDevice;
   }

   protected ValidationCommandEncoder wrapCommandEncoder(CommandEncoder commandEncoder, GpuDeviceUsageValidator validator) {
      return new ValidationCommandEncoder(commandEncoder, validator);
   }

   @Override
   public CommandEncoder createCommandEncoder() {
      return this.validationCommandEncoder;
   }

   @Override
   public GpuSampler createSampler(
      AddressMode addressModeU, AddressMode addressModeV, FilterMode minFilter, FilterMode magFilter, int maxAnisotropy, OptionalDouble maxLod
   ) {
      return this.realDevice.createSampler(addressModeU, addressModeV, minFilter, magFilter, maxAnisotropy, maxLod);
   }

   protected ValidationGpuTexture wrapGpuTexture(GpuTexture texture, GpuDeviceUsageValidator validator) {
      return new ValidationGpuTexture(texture, validator);
   }

   @Override
   public GpuTexture createTexture(@Nullable Supplier<String> label, int usage, TextureFormat format, int width, int height, int depthOrLayers, int mipLevels) {
      this.validator.validateTextureUsage(usage);
      return this.wrapGpuTexture(this.realDevice.createTexture(label, usage, format, width, height, depthOrLayers, mipLevels), this.validator);
   }

   @Override
   public GpuTexture createTexture(@Nullable String label, int usage, TextureFormat format, int width, int height, int depthOrLayers, int mipLevels) {
      this.validator.validateTextureUsage(usage);
      return this.wrapGpuTexture(this.realDevice.createTexture(label, usage, format, width, height, depthOrLayers, mipLevels), this.validator);
   }

   protected ValidationGpuTextureView wrapGpuTextureView(
      ValidationGpuTexture validationGpuTexture, GpuTextureView gpuTextureView, GpuDeviceUsageValidator validator
   ) {
      return new ValidationGpuTextureView(validationGpuTexture, gpuTextureView, validator);
   }

   @Override
   public GpuTextureView createTextureView(GpuTexture texture) {
      if (texture instanceof ValidationGpuTexture validationTexture) {
         return this.wrapGpuTextureView(validationTexture, this.realDevice.createTextureView(validationTexture.getRealTexture()), this.validator);
      } else {
         throw new IllegalArgumentException();
      }
   }

   @Override
   public GpuTextureView createTextureView(GpuTexture texture, int baseMipLevel, int mipLevels) {
      if (texture instanceof ValidationGpuTexture validationTexture) {
         return this.wrapGpuTextureView(
            validationTexture, this.realDevice.createTextureView(validationTexture.getRealTexture(), baseMipLevel, mipLevels), this.validator
         );
      } else {
         throw new IllegalArgumentException();
      }
   }

   @Override
   public GpuBuffer createBuffer(@Nullable Supplier<String> label, int usage, long size) {
      this.validator.validateBufferUsage(usage);
      return this.realDevice.createBuffer(label, usage, size);
   }

   @Override
   public GpuBuffer createBuffer(@Nullable Supplier<String> label, int usage, ByteBuffer data) {
      this.validator.validateBufferUsage(usage);
      return this.realDevice.createBuffer(label, usage, data);
   }

   @Override
   public String getImplementationInformation() {
      return this.realDevice.getImplementationInformation();
   }

   @Override
   public List<String> getLastDebugMessages() {
      return this.realDevice.getLastDebugMessages();
   }

   @Override
   public boolean isDebuggingEnabled() {
      return this.realDevice.isDebuggingEnabled();
   }

   @Override
   public String getVendor() {
      return this.realDevice.getVendor();
   }

   @Override
   public String getBackendName() {
      return this.realDevice.getBackendName();
   }

   @Override
   public String getVersion() {
      return this.realDevice.getVersion();
   }

   @Override
   public String getRenderer() {
      return this.realDevice.getRenderer();
   }

   @Override
   public int getMaxTextureSize() {
      return this.realDevice.getMaxTextureSize();
   }

   @Override
   public int getUniformOffsetAlignment() {
      return this.realDevice.getUniformOffsetAlignment();
   }

   @Override
   public int getMaxSupportedAnisotropy() {
      return this.realDevice.getMaxSupportedAnisotropy();
   }

   @Override
   public CompiledRenderPipeline precompilePipeline(RenderPipeline pipeline, @Nullable ShaderSource shaderSourceProvider) {
      this.validator.validatePipeline(pipeline);
      return this.realDevice.precompilePipeline(pipeline, shaderSourceProvider);
   }

   @Override
   public void clearPipelineCache() {
      this.realDevice.clearPipelineCache();
   }

   @Override
   public List<String> getEnabledExtensions() {
      return this.realDevice.getEnabledExtensions();
   }

   @Override
   public void close() {
      this.realDevice.close();
   }

   @Override
   public GpuDeviceProperties deviceProperties() {
      return this.realDevice.deviceProperties();
   }

   @Override
   public GpuDeviceFeatures enabledFeatures() {
      return this.realDevice.enabledFeatures();
   }
}
