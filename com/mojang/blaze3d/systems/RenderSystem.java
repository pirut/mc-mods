package com.mojang.blaze3d.systems;

import com.mojang.blaze3d.DontObfuscate;
import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.TracyFrameCapture;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.GpuFence;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.GLX;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.shaders.ShaderSource;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.logging.LogUtils;
import java.nio.ByteBuffer;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.IntConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.DynamicUniforms;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.ArrayListDeque;
import net.minecraft.util.Mth;
import net.minecraft.util.TimeSource;
import net.minecraft.util.Util;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.ClientHooks;
import net.neoforged.neoforge.client.pipeline.PipelineModifier;
import net.neoforged.neoforge.client.pipeline.PipelineModifierStack;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.jspecify.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallbackI;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
@DontObfuscate
public class RenderSystem {
   static final Logger LOGGER = LogUtils.getLogger();
   public static final int MINIMUM_ATLAS_TEXTURE_SIZE = 1024;
   public static final int PROJECTION_MATRIX_UBO_SIZE = new Std140SizeCalculator().putMat4f().get();
   @Nullable
   private static Thread renderThread;
   @Nullable
   private static GpuDevice DEVICE;
   private static double lastDrawTime = Double.MIN_VALUE;
   private static final RenderSystem.AutoStorageIndexBuffer sharedSequential = new RenderSystem.AutoStorageIndexBuffer(1, 1, IntConsumer::accept);
   private static final RenderSystem.AutoStorageIndexBuffer sharedSequentialQuad = new RenderSystem.AutoStorageIndexBuffer(4, 6, (p_403829_, p_403830_) -> {
      p_403829_.accept(p_403830_);
      p_403829_.accept(p_403830_ + 1);
      p_403829_.accept(p_403830_ + 2);
      p_403829_.accept(p_403830_ + 2);
      p_403829_.accept(p_403830_ + 3);
      p_403829_.accept(p_403830_);
   });
   private static final RenderSystem.AutoStorageIndexBuffer sharedSequentialLines = new RenderSystem.AutoStorageIndexBuffer(4, 6, (p_403827_, p_403828_) -> {
      p_403827_.accept(p_403828_);
      p_403827_.accept(p_403828_ + 1);
      p_403827_.accept(p_403828_ + 2);
      p_403827_.accept(p_403828_ + 3);
      p_403827_.accept(p_403828_ + 2);
      p_403827_.accept(p_403828_ + 1);
   });
   private static ProjectionType projectionType = ProjectionType.PERSPECTIVE;
   private static ProjectionType savedProjectionType = ProjectionType.PERSPECTIVE;
   private static final Matrix4fStack modelViewStack = new Matrix4fStack(16);
   @Nullable
   private static GpuBufferSlice shaderFog = null;
   @Nullable
   private static GpuBufferSlice shaderLightDirections;
   @Nullable
   private static GpuBufferSlice projectionMatrixBuffer;
   @Nullable
   private static GpuBufferSlice savedProjectionMatrixBuffer;
   private static String apiDescription = "Unknown";
   private static final AtomicLong pollEventsWaitStart = new AtomicLong();
   private static final AtomicBoolean pollingEvents = new AtomicBoolean(false);
   private static final ArrayListDeque<RenderSystem.GpuAsyncTask> PENDING_FENCES = new ArrayListDeque<>();
   @Nullable
   public static GpuTextureView outputColorTextureOverride;
   @Nullable
   public static GpuTextureView outputDepthTextureOverride;
   @Nullable
   private static GpuBuffer globalSettingsUniform;
   @Nullable
   private static DynamicUniforms dynamicUniforms;
   private static final ScissorState scissorStateForRenderTypeDraws = new ScissorState();
   private static SamplerCache samplerCache = new SamplerCache();
   private static final PipelineModifierStack PIPELINE_MODIFIERS = new PipelineModifierStack();

   public static SamplerCache getSamplerCache() {
      return samplerCache;
   }

   public static void initRenderThread() {
      if (renderThread != null) {
         throw new IllegalStateException("Could not initialize render thread");
      } else {
         renderThread = Thread.currentThread();
      }
   }

   public static boolean isOnRenderThread() {
      return Thread.currentThread() == renderThread;
   }

   public static void assertOnRenderThread() {
      if (!isOnRenderThread()) {
         throw constructThreadException();
      }
   }

   private static IllegalStateException constructThreadException() {
      return new IllegalStateException("Rendersystem called from wrong thread");
   }

   private static void pollEvents() {
      pollEventsWaitStart.set(Util.getMillis());
      pollingEvents.set(true);
      GLFW.glfwPollEvents();
      pollingEvents.set(false);
   }

   public static boolean isFrozenAtPollEvents() {
      return pollingEvents.get() && Util.getMillis() - pollEventsWaitStart.get() > 200L;
   }

   public static void flipFrame(Window p_446361_, @Nullable TracyFrameCapture p_372841_) {
      pollEvents();
      Tesselator.getInstance().clear();
      GLFW.glfwSwapBuffers(p_446361_.handle());
      if (p_372841_ != null) {
         p_372841_.endFrame();
      }

      dynamicUniforms.reset();
      Minecraft.getInstance().levelRenderer.endFrame();
      pollEvents();
   }

   public static void limitDisplayFPS(int p_69831_) {
      double d0 = lastDrawTime + 1.0 / p_69831_;

      double d1;
      for (d1 = GLFW.glfwGetTime(); d1 < d0; d1 = GLFW.glfwGetTime()) {
         GLFW.glfwWaitEventsTimeout(d0 - d1);
      }

      lastDrawTime = d1;
   }

   public static void setShaderFog(GpuBufferSlice p_418490_) {
      shaderFog = p_418490_;
   }

   @Nullable
   public static GpuBufferSlice getShaderFog() {
      return shaderFog;
   }

   public static void setShaderLights(GpuBufferSlice p_418388_) {
      shaderLightDirections = p_418388_;
   }

   @Nullable
   public static GpuBufferSlice getShaderLights() {
      return shaderLightDirections;
   }

   public static void enableScissorForRenderTypeDraws(int p_428564_, int p_428412_, int p_428568_, int p_428211_) {
      scissorStateForRenderTypeDraws.enable(p_428564_, p_428412_, p_428568_, p_428211_);
   }

   public static void disableScissorForRenderTypeDraws() {
      scissorStateForRenderTypeDraws.disable();
   }

   public static ScissorState getScissorStateForRenderTypeDraws() {
      return scissorStateForRenderTypeDraws;
   }

   public static String getBackendDescription() {
      return String.format(Locale.ROOT, "LWJGL version %s", GLX._getLWJGLVersion());
   }

   public static String getApiDescription() {
      return apiDescription;
   }

   public static TimeSource.NanoTimeSource initBackendSystem() {
      return GLX._initGlfw()::getAsLong;
   }

   public static void initRenderer(long p_409720_, int p_69581_, boolean p_69582_, ShaderSource p_460676_, boolean p_410401_) {
      DEVICE = ClientHooks.createGpuDevice(p_409720_, p_69581_, p_69582_, p_460676_, p_410401_);
      apiDescription = getDevice().getImplementationInformation();
      dynamicUniforms = new DynamicUniforms();
      samplerCache.initialize();
   }

   public static void setErrorCallback(GLFWErrorCallbackI p_69901_) {
      GLX._setGlfwErrorCallback(p_69901_);
   }

   public static void setupDefaultState() {
      modelViewStack.clear();
   }

   public static void setProjectionMatrix(GpuBufferSlice p_418326_, ProjectionType p_381637_) {
      assertOnRenderThread();
      projectionMatrixBuffer = p_418326_;
      projectionType = p_381637_;
   }

   public static void backupProjectionMatrix() {
      assertOnRenderThread();
      savedProjectionMatrixBuffer = projectionMatrixBuffer;
      savedProjectionType = projectionType;
   }

   public static void restoreProjectionMatrix() {
      assertOnRenderThread();
      projectionMatrixBuffer = savedProjectionMatrixBuffer;
      projectionType = savedProjectionType;
   }

   @Nullable
   public static GpuBufferSlice getProjectionMatrixBuffer() {
      assertOnRenderThread();
      return projectionMatrixBuffer;
   }

   public static Matrix4f getModelViewMatrix() {
      assertOnRenderThread();
      return modelViewStack;
   }

   public static Matrix4fStack getModelViewStack() {
      assertOnRenderThread();
      return modelViewStack;
   }

   public static RenderSystem.AutoStorageIndexBuffer getSequentialBuffer(VertexFormat.Mode p_221942_) {
      assertOnRenderThread();

      return switch (p_221942_) {
         case QUADS -> sharedSequentialQuad;
         case LINES -> sharedSequentialLines;
         default -> sharedSequential;
      };
   }

   public static void setGlobalSettingsUniform(GpuBuffer p_418439_) {
      globalSettingsUniform = p_418439_;
   }

   @Nullable
   public static GpuBuffer getGlobalSettingsUniform() {
      return globalSettingsUniform;
   }

   public static ProjectionType getProjectionType() {
      assertOnRenderThread();
      return projectionType;
   }

   public static void queueFencedTask(Runnable p_406359_) {
      PENDING_FENCES.addLast(new RenderSystem.GpuAsyncTask(p_406359_, getDevice().createCommandEncoder().createFence()));
   }

   public static void executePendingTasks() {
      for (RenderSystem.GpuAsyncTask rendersystem$gpuasynctask = PENDING_FENCES.peekFirst();
         rendersystem$gpuasynctask != null;
         rendersystem$gpuasynctask = PENDING_FENCES.peekFirst()
      ) {
         if (!rendersystem$gpuasynctask.fence.awaitCompletion(0L)) {
            return;
         }

         try {
            rendersystem$gpuasynctask.callback.run();
         } finally {
            rendersystem$gpuasynctask.fence.close();
         }

         PENDING_FENCES.removeFirst();
      }
   }

   public static GpuDevice getDevice() {
      if (DEVICE == null) {
         throw new IllegalStateException("Can't getDevice() before it was initialized");
      } else {
         return DEVICE;
      }
   }

   @Nullable
   public static GpuDevice tryGetDevice() {
      return DEVICE;
   }

   public static DynamicUniforms getDynamicUniforms() {
      if (dynamicUniforms == null) {
         throw new IllegalStateException("Can't getDynamicUniforms() before device was initialized");
      } else {
         return dynamicUniforms;
      }
   }

   public static void bindDefaultUniforms(RenderPass p_418212_) {
      GpuBufferSlice gpubufferslice = getProjectionMatrixBuffer();
      if (gpubufferslice != null) {
         p_418212_.setUniform("Projection", gpubufferslice);
      }

      GpuBufferSlice gpubufferslice1 = getShaderFog();
      if (gpubufferslice1 != null) {
         p_418212_.setUniform("Fog", gpubufferslice1);
      }

      GpuBuffer gpubuffer = getGlobalSettingsUniform();
      if (gpubuffer != null) {
         p_418212_.setUniform("Globals", gpubuffer);
      }

      GpuBufferSlice gpubufferslice2 = getShaderLights();
      if (gpubufferslice2 != null) {
         p_418212_.setUniform("Lighting", gpubufferslice2);
      }
   }

   public static void pushPipelineModifier(ResourceKey<PipelineModifier> modifier) {
      PIPELINE_MODIFIERS.push(modifier);
   }

   public static void popPipelineModifier() {
      PIPELINE_MODIFIERS.pop();
   }

   public static void renderWithPipelineModifier(ResourceKey<PipelineModifier> modifier, Runnable renderTask) {
      PIPELINE_MODIFIERS.renderWithModifier(modifier, renderTask);
   }

   @Internal
   public static RenderPipeline applyPipelineModifiers(RenderPipeline pipeline) {
      return PIPELINE_MODIFIERS.apply(pipeline);
   }

   @Internal
   public static void ensurePipelineModifiersEmpty() {
      PIPELINE_MODIFIERS.ensureEmpty();
   }

   @OnlyIn(Dist.CLIENT)
   public static final class AutoStorageIndexBuffer {
      private final int vertexStride;
      private final int indexStride;
      private final RenderSystem.AutoStorageIndexBuffer.IndexGenerator generator;
      @Nullable
      private GpuBuffer buffer;
      private VertexFormat.IndexType type = VertexFormat.IndexType.SHORT;
      private int indexCount;

      AutoStorageIndexBuffer(int p_157472_, int p_157473_, RenderSystem.AutoStorageIndexBuffer.IndexGenerator p_157474_) {
         this.vertexStride = p_157472_;
         this.indexStride = p_157473_;
         this.generator = p_157474_;
      }

      public boolean hasStorage(int p_221945_) {
         return p_221945_ <= this.indexCount;
      }

      public GpuBuffer getBuffer(int p_410107_) {
         this.ensureStorage(p_410107_);
         return this.buffer;
      }

      private void ensureStorage(int p_157477_) {
         if (!this.hasStorage(p_157477_)) {
            p_157477_ = Mth.roundToward(p_157477_ * 2, this.indexStride);
            RenderSystem.LOGGER.debug("Growing IndexBuffer: Old limit {}, new limit {}.", this.indexCount, p_157477_);
            int i = p_157477_ / this.indexStride;
            int j = i * this.vertexStride;
            VertexFormat.IndexType vertexformat$indextype = VertexFormat.IndexType.least(j);
            int k = Mth.roundToward(p_157477_ * vertexformat$indextype.bytes, 4);
            ByteBuffer bytebuffer = MemoryUtil.memAlloc(k);

            try {
               this.type = vertexformat$indextype;
               it.unimi.dsi.fastutil.ints.IntConsumer intconsumer = this.intConsumer(bytebuffer);

               for (int l = 0; l < p_157477_; l += this.indexStride) {
                  this.generator.accept(intconsumer, l * this.vertexStride / this.indexStride);
               }

               bytebuffer.flip();
               if (this.buffer != null) {
                  this.buffer.close();
               }

               this.buffer = RenderSystem.getDevice().createBuffer(() -> "Auto Storage index buffer", 64, bytebuffer);
            } finally {
               MemoryUtil.memFree(bytebuffer);
            }

            this.indexCount = p_157477_;
         }
      }

      private it.unimi.dsi.fastutil.ints.IntConsumer intConsumer(ByteBuffer p_157479_) {
         switch (this.type) {
            case SHORT:
               return p_157482_ -> p_157479_.putShort((short)p_157482_);
            case INT:
            default:
               return p_157479_::putInt;
         }
      }

      public VertexFormat.IndexType type() {
         return this.type;
      }

      @OnlyIn(Dist.CLIENT)
      interface IndexGenerator {
         void accept(it.unimi.dsi.fastutil.ints.IntConsumer var1, int var2);
      }
   }

   @OnlyIn(Dist.CLIENT)
   record GpuAsyncTask(Runnable callback, GpuFence fence) {
   }
}
