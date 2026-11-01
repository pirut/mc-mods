package com.mojang.blaze3d.opengl;

import com.mojang.blaze3d.DontObfuscate;
import com.mojang.blaze3d.platform.MacosUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.jtracy.Plot;
import com.mojang.jtracy.TracyClient;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.stream.IntStream;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;
import org.lwjgl.PointerBuffer;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL20C;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL32;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

@OnlyIn(Dist.CLIENT)
@DontObfuscate
public class GlStateManager {
   private static final Plot PLOT_TEXTURES = TracyClient.createPlot("GPU Textures");
   private static int numTextures = 0;
   private static final Plot PLOT_BUFFERS = TracyClient.createPlot("GPU Buffers");
   private static int numBuffers = 0;
   private static final GlStateManager.BlendState BLEND = new GlStateManager.BlendState();
   private static final GlStateManager.DepthState DEPTH = new GlStateManager.DepthState();
   private static final GlStateManager.CullState CULL = new GlStateManager.CullState();
   private static final GlStateManager.PolygonOffsetState POLY_OFFSET = new GlStateManager.PolygonOffsetState();
   private static final GlStateManager.ColorLogicState COLOR_LOGIC = new GlStateManager.ColorLogicState();
   private static final GlStateManager.StencilState STENCIL = new GlStateManager.StencilState();
   private static final GlStateManager.ScissorState SCISSOR = new GlStateManager.ScissorState();
   private static int activeTexture;
   private static final int TEXTURE_COUNT = 12;
   private static final GlStateManager.TextureState[] TEXTURES = IntStream.range(0, 12)
      .mapToObj(p_412448_ -> new GlStateManager.TextureState())
      .toArray(GlStateManager.TextureState[]::new);
   private static final GlStateManager.ColorMask COLOR_MASK = new GlStateManager.ColorMask();
   private static int readFbo;
   private static int writeFbo;

   public static void _disableScissorTest() {
      RenderSystem.assertOnRenderThread();
      SCISSOR.mode.disable();
   }

   public static void _enableScissorTest() {
      RenderSystem.assertOnRenderThread();
      SCISSOR.mode.enable();
   }

   public static void _scissorBox(int p_412544_, int p_412671_, int p_412434_, int p_412223_) {
      RenderSystem.assertOnRenderThread();
      GL20.glScissor(p_412544_, p_412671_, p_412434_, p_412223_);
   }

   public static void _disableDepthTest() {
      RenderSystem.assertOnRenderThread();
      DEPTH.mode.disable();
   }

   public static void _enableDepthTest() {
      RenderSystem.assertOnRenderThread();
      DEPTH.mode.enable();
   }

   public static void _depthFunc(int p_412390_) {
      RenderSystem.assertOnRenderThread();
      if (p_412390_ != DEPTH.func) {
         DEPTH.func = p_412390_;
         GL11.glDepthFunc(p_412390_);
      }
   }

   public static void _depthMask(boolean p_412682_) {
      RenderSystem.assertOnRenderThread();
      if (p_412682_ != DEPTH.mask) {
         DEPTH.mask = p_412682_;
         GL11.glDepthMask(p_412682_);
      }
   }

   public static void _disableBlend() {
      RenderSystem.assertOnRenderThread();
      BLEND.mode.disable();
   }

   public static void _enableBlend() {
      RenderSystem.assertOnRenderThread();
      BLEND.mode.enable();
   }

   public static void _blendFuncSeparate(int p_412261_, int p_412664_, int p_412076_, int p_412367_) {
      RenderSystem.assertOnRenderThread();
      if (p_412261_ != BLEND.srcRgb || p_412664_ != BLEND.dstRgb || p_412076_ != BLEND.srcAlpha || p_412367_ != BLEND.dstAlpha) {
         BLEND.srcRgb = p_412261_;
         BLEND.dstRgb = p_412664_;
         BLEND.srcAlpha = p_412076_;
         BLEND.dstAlpha = p_412367_;
         glBlendFuncSeparate(p_412261_, p_412664_, p_412076_, p_412367_);
      }
   }

   public static int glGetProgrami(int p_412785_, int p_412182_) {
      RenderSystem.assertOnRenderThread();
      return GL20.glGetProgrami(p_412785_, p_412182_);
   }

   public static void glAttachShader(int p_412381_, int p_412166_) {
      RenderSystem.assertOnRenderThread();
      GL20.glAttachShader(p_412381_, p_412166_);
   }

   public static void glDeleteShader(int p_412373_) {
      RenderSystem.assertOnRenderThread();
      GL20.glDeleteShader(p_412373_);
   }

   public static int glCreateShader(int p_412529_) {
      RenderSystem.assertOnRenderThread();
      return GL20.glCreateShader(p_412529_);
   }

   public static void glShaderSource(int p_412070_, String p_412719_) {
      RenderSystem.assertOnRenderThread();
      byte[] abyte = p_412719_.getBytes(StandardCharsets.UTF_8);
      ByteBuffer bytebuffer = MemoryUtil.memAlloc(abyte.length + 1);
      bytebuffer.put(abyte);
      bytebuffer.put((byte)0);
      bytebuffer.flip();

      try {
         MemoryStack memorystack = MemoryStack.stackPush();

         try {
            PointerBuffer pointerbuffer = memorystack.mallocPointer(1);
            pointerbuffer.put(bytebuffer);
            GL20C.nglShaderSource(p_412070_, 1, pointerbuffer.address0(), 0L);
         } catch (Throwable var12) {
            if (memorystack != null) {
               try {
                  memorystack.close();
               } catch (Throwable var11) {
                  var12.addSuppressed(var11);
               }
            }

            throw var12;
         }

         if (memorystack != null) {
            memorystack.close();
         }
      } finally {
         MemoryUtil.memFree(bytebuffer);
      }
   }

   public static void glCompileShader(int p_412483_) {
      RenderSystem.assertOnRenderThread();
      GL20.glCompileShader(p_412483_);
   }

   public static int glGetShaderi(int p_412274_, int p_412320_) {
      RenderSystem.assertOnRenderThread();
      return GL20.glGetShaderi(p_412274_, p_412320_);
   }

   public static void _glUseProgram(int p_412686_) {
      RenderSystem.assertOnRenderThread();
      GL20.glUseProgram(p_412686_);
   }

   public static int glCreateProgram() {
      RenderSystem.assertOnRenderThread();
      return GL20.glCreateProgram();
   }

   public static void glDeleteProgram(int p_412461_) {
      RenderSystem.assertOnRenderThread();
      GL20.glDeleteProgram(p_412461_);
   }

   public static void glLinkProgram(int p_412342_) {
      RenderSystem.assertOnRenderThread();
      GL20.glLinkProgram(p_412342_);
   }

   public static int _glGetUniformLocation(int p_412693_, CharSequence p_412767_) {
      RenderSystem.assertOnRenderThread();
      return GL20.glGetUniformLocation(p_412693_, p_412767_);
   }

   public static void _glUniform1i(int p_412109_, int p_412740_) {
      RenderSystem.assertOnRenderThread();
      GL20.glUniform1i(p_412109_, p_412740_);
   }

   public static void _glBindAttribLocation(int p_412646_, int p_412124_, CharSequence p_412306_) {
      RenderSystem.assertOnRenderThread();
      GL20.glBindAttribLocation(p_412646_, p_412124_, p_412306_);
   }

   public static void incrementTrackedBuffers() {
      numBuffers++;
      PLOT_BUFFERS.setValue(numBuffers);
   }

   public static int _glGenBuffers() {
      RenderSystem.assertOnRenderThread();
      incrementTrackedBuffers();
      return GL15.glGenBuffers();
   }

   public static int _glGenVertexArrays() {
      RenderSystem.assertOnRenderThread();
      return GL30.glGenVertexArrays();
   }

   public static void _glBindBuffer(int p_412674_, int p_412428_) {
      RenderSystem.assertOnRenderThread();
      GL15.glBindBuffer(p_412674_, p_412428_);
   }

   public static void _glBindVertexArray(int p_412659_) {
      RenderSystem.assertOnRenderThread();
      GL30.glBindVertexArray(p_412659_);
   }

   public static void _glBufferData(int p_412508_, ByteBuffer p_412262_, int p_412282_) {
      RenderSystem.assertOnRenderThread();
      GL15.glBufferData(p_412508_, p_412262_, p_412282_);
   }

   public static void _glBufferSubData(int p_412190_, long p_482304_, ByteBuffer p_412299_) {
      RenderSystem.assertOnRenderThread();
      GL15.glBufferSubData(p_412190_, p_482304_, p_412299_);
   }

   public static void _glBufferData(int p_412471_, long p_412328_, int p_412602_) {
      RenderSystem.assertOnRenderThread();
      GL15.glBufferData(p_412471_, p_412328_, p_412602_);
   }

   @Nullable
   public static ByteBuffer _glMapBufferRange(int p_412389_, long p_482311_, long p_482290_, int p_412504_) {
      RenderSystem.assertOnRenderThread();
      return GL30.glMapBufferRange(p_412389_, p_482311_, p_482290_, p_412504_);
   }

   public static void _glUnmapBuffer(int p_412042_) {
      RenderSystem.assertOnRenderThread();
      GL15.glUnmapBuffer(p_412042_);
   }

   public static void _glDeleteBuffers(int p_412311_) {
      RenderSystem.assertOnRenderThread();
      numBuffers--;
      PLOT_BUFFERS.setValue(numBuffers);
      GL15.glDeleteBuffers(p_412311_);
   }

   public static void _glBindFramebuffer(int p_412624_, int p_412635_) {
      if ((p_412624_ == 36008 || p_412624_ == 36160) && readFbo != p_412635_) {
         GL30.glBindFramebuffer(36008, p_412635_);
         readFbo = p_412635_;
      }

      if ((p_412624_ == 36009 || p_412624_ == 36160) && writeFbo != p_412635_) {
         GL30.glBindFramebuffer(36009, p_412635_);
         writeFbo = p_412635_;
      }
   }

   public static int getFrameBuffer(int p_412720_) {
      if (p_412720_ == 36008) {
         return readFbo;
      } else {
         return p_412720_ == 36009 ? writeFbo : 0;
      }
   }

   public static void _glBlitFrameBuffer(
      int p_412319_, int p_412052_, int p_412080_, int p_412580_, int p_412560_, int p_412565_, int p_412724_, int p_412575_, int p_412505_, int p_412309_
   ) {
      RenderSystem.assertOnRenderThread();
      GL30.glBlitFramebuffer(p_412319_, p_412052_, p_412080_, p_412580_, p_412560_, p_412565_, p_412724_, p_412575_, p_412505_, p_412309_);
   }

   public static void _glDeleteFramebuffers(int p_412563_) {
      RenderSystem.assertOnRenderThread();
      GL30.glDeleteFramebuffers(p_412563_);
      if (readFbo == p_412563_) {
         readFbo = 0;
      }

      if (writeFbo == p_412563_) {
         writeFbo = 0;
      }
   }

   public static int glGenFramebuffers() {
      RenderSystem.assertOnRenderThread();
      return GL30.glGenFramebuffers();
   }

   public static void _glFramebufferTexture2D(int p_412121_, int p_412761_, int p_412688_, int p_412161_, int p_412731_) {
      RenderSystem.assertOnRenderThread();
      GL30.glFramebufferTexture2D(p_412121_, p_412761_, p_412688_, p_412161_, p_412731_);
   }

   public static void glBlendFuncSeparate(int p_412333_, int p_412425_, int p_412352_, int p_412353_) {
      RenderSystem.assertOnRenderThread();
      GL14.glBlendFuncSeparate(p_412333_, p_412425_, p_412352_, p_412353_);
   }

   public static String glGetShaderInfoLog(int p_412120_, int p_412078_) {
      RenderSystem.assertOnRenderThread();
      return GL20.glGetShaderInfoLog(p_412120_, p_412078_);
   }

   public static String glGetProgramInfoLog(int p_412598_, int p_412472_) {
      RenderSystem.assertOnRenderThread();
      return GL20.glGetProgramInfoLog(p_412598_, p_412472_);
   }

   public static void _enableCull() {
      RenderSystem.assertOnRenderThread();
      CULL.enable.enable();
   }

   public static void _disableCull() {
      RenderSystem.assertOnRenderThread();
      CULL.enable.disable();
   }

   public static void _polygonMode(int p_412155_, int p_412317_) {
      RenderSystem.assertOnRenderThread();
      GL11.glPolygonMode(p_412155_, p_412317_);
   }

   public static void _enablePolygonOffset() {
      RenderSystem.assertOnRenderThread();
      POLY_OFFSET.fill.enable();
   }

   public static void _disablePolygonOffset() {
      RenderSystem.assertOnRenderThread();
      POLY_OFFSET.fill.disable();
   }

   public static void _polygonOffset(float p_412343_, float p_412604_) {
      RenderSystem.assertOnRenderThread();
      if (p_412343_ != POLY_OFFSET.factor || p_412604_ != POLY_OFFSET.units) {
         POLY_OFFSET.factor = p_412343_;
         POLY_OFFSET.units = p_412604_;
         GL11.glPolygonOffset(p_412343_, p_412604_);
      }
   }

   public static void _enableColorLogicOp() {
      RenderSystem.assertOnRenderThread();
      COLOR_LOGIC.enable.enable();
   }

   public static void _disableColorLogicOp() {
      RenderSystem.assertOnRenderThread();
      COLOR_LOGIC.enable.disable();
   }

   public static void _logicOp(int p_412555_) {
      RenderSystem.assertOnRenderThread();
      if (p_412555_ != COLOR_LOGIC.op) {
         COLOR_LOGIC.op = p_412555_;
         GL11.glLogicOp(p_412555_);
      }
   }

   public static void _activeTexture(int p_412206_) {
      RenderSystem.assertOnRenderThread();
      if (activeTexture != p_412206_ - 33984) {
         activeTexture = p_412206_ - 33984;
         GL13.glActiveTexture(p_412206_);
      }
   }

   public static void _texParameter(int p_412413_, int p_412226_, int p_412243_) {
      RenderSystem.assertOnRenderThread();
      GL11.glTexParameteri(p_412413_, p_412226_, p_412243_);
   }

   public static int _getTexLevelParameter(int p_412349_, int p_412379_, int p_412747_) {
      return GL11.glGetTexLevelParameteri(p_412349_, p_412379_, p_412747_);
   }

   public static int _genTexture() {
      RenderSystem.assertOnRenderThread();
      numTextures++;
      PLOT_TEXTURES.setValue(numTextures);
      return GL11.glGenTextures();
   }

   public static void _deleteTexture(int p_412766_) {
      RenderSystem.assertOnRenderThread();
      GL11.glDeleteTextures(p_412766_);

      for (GlStateManager.TextureState glstatemanager$texturestate : TEXTURES) {
         if (glstatemanager$texturestate.binding == p_412766_) {
            glstatemanager$texturestate.binding = -1;
         }
      }

      numTextures--;
      PLOT_TEXTURES.setValue(numTextures);
   }

   public static void _bindTexture(int p_412384_) {
      RenderSystem.assertOnRenderThread();
      if (p_412384_ != TEXTURES[activeTexture].binding) {
         TEXTURES[activeTexture].binding = p_412384_;
         GL11.glBindTexture(3553, p_412384_);
      }
   }

   public static void _texImage2D(
      int p_412310_, int p_412419_, int p_412630_, int p_412350_, int p_412242_, int p_412290_, int p_412363_, int p_412035_, @Nullable ByteBuffer p_436805_
   ) {
      RenderSystem.assertOnRenderThread();
      GL11.glTexImage2D(p_412310_, p_412419_, p_412630_, p_412350_, p_412242_, p_412290_, p_412363_, p_412035_, p_436805_);
   }

   public static void _texSubImage2D(
      int p_412334_, int p_412330_, int p_412661_, int p_412280_, int p_412301_, int p_412313_, int p_412612_, int p_412158_, long p_412094_
   ) {
      RenderSystem.assertOnRenderThread();
      GL11.glTexSubImage2D(p_412334_, p_412330_, p_412661_, p_412280_, p_412301_, p_412313_, p_412612_, p_412158_, p_412094_);
   }

   public static void _texSubImage2D(
      int p_412117_, int p_412548_, int p_412368_, int p_412679_, int p_412432_, int p_412239_, int p_412198_, int p_412304_, ByteBuffer p_436763_
   ) {
      RenderSystem.assertOnRenderThread();
      GL11.glTexSubImage2D(p_412117_, p_412548_, p_412368_, p_412679_, p_412432_, p_412239_, p_412198_, p_412304_, p_436763_);
   }

   public static void _viewport(int p_412768_, int p_412268_, int p_412374_, int p_412660_) {
      GL11.glViewport(p_412768_, p_412268_, p_412374_, p_412660_);
   }

   public static void _colorMask(boolean p_412534_, boolean p_412706_, boolean p_412187_, boolean p_412294_) {
      RenderSystem.assertOnRenderThread();
      if (p_412534_ != COLOR_MASK.red || p_412706_ != COLOR_MASK.green || p_412187_ != COLOR_MASK.blue || p_412294_ != COLOR_MASK.alpha) {
         COLOR_MASK.red = p_412534_;
         COLOR_MASK.green = p_412706_;
         COLOR_MASK.blue = p_412187_;
         COLOR_MASK.alpha = p_412294_;
         GL11.glColorMask(p_412534_, p_412706_, p_412187_, p_412294_);
      }
   }

   public static void _clear(int p_412293_) {
      RenderSystem.assertOnRenderThread();
      GL11.glClear(p_412293_);
      if (MacosUtil.IS_MACOS) {
         _getError();
      }
   }

   public static void _vertexAttribPointer(int p_412619_, int p_412089_, int p_412208_, boolean p_412687_, int p_412716_, long p_412347_) {
      RenderSystem.assertOnRenderThread();
      GL20.glVertexAttribPointer(p_412619_, p_412089_, p_412208_, p_412687_, p_412716_, p_412347_);
   }

   public static void _vertexAttribIPointer(int p_412449_, int p_412145_, int p_412344_, int p_412169_, long p_412325_) {
      RenderSystem.assertOnRenderThread();
      GL30.glVertexAttribIPointer(p_412449_, p_412145_, p_412344_, p_412169_, p_412325_);
   }

   public static void _enableVertexAttribArray(int p_412609_) {
      RenderSystem.assertOnRenderThread();
      GL20.glEnableVertexAttribArray(p_412609_);
   }

   public static void _drawElements(int p_412772_, int p_412151_, int p_412245_, long p_412484_) {
      RenderSystem.assertOnRenderThread();
      GL11.glDrawElements(p_412772_, p_412151_, p_412245_, p_412484_);
   }

   public static void _drawArrays(int p_412383_, int p_412165_, int p_412633_) {
      RenderSystem.assertOnRenderThread();
      GL11.glDrawArrays(p_412383_, p_412165_, p_412633_);
   }

   public static void _pixelStore(int p_412183_, int p_412338_) {
      RenderSystem.assertOnRenderThread();
      GL11.glPixelStorei(p_412183_, p_412338_);
   }

   public static void _readPixels(int p_412385_, int p_412288_, int p_412300_, int p_412743_, int p_412722_, int p_412758_, long p_412203_) {
      RenderSystem.assertOnRenderThread();
      GL11.glReadPixels(p_412385_, p_412288_, p_412300_, p_412743_, p_412722_, p_412758_, p_412203_);
   }

   public static int _getError() {
      RenderSystem.assertOnRenderThread();
      return GL11.glGetError();
   }

   public static void clearGlErrors() {
      RenderSystem.assertOnRenderThread();

      while (GL11.glGetError() != 0) {
      }
   }

   public static String _getString(int p_412524_) {
      RenderSystem.assertOnRenderThread();
      return GL11.glGetString(p_412524_);
   }

   public static int _getInteger(int p_412627_) {
      RenderSystem.assertOnRenderThread();
      return GL11.glGetInteger(p_412627_);
   }

   public static long _glFenceSync(int p_412755_, int p_412391_) {
      RenderSystem.assertOnRenderThread();
      return GL32.glFenceSync(p_412755_, p_412391_);
   }

   public static int _glClientWaitSync(long p_412773_, int p_412625_, long p_412397_) {
      RenderSystem.assertOnRenderThread();
      return GL32.glClientWaitSync(p_412773_, p_412625_, p_412397_);
   }

   public static void _glDeleteSync(long p_412289_) {
      RenderSystem.assertOnRenderThread();
      GL32.glDeleteSync(p_412289_);
   }

   public static void _disableStencilTest() {
      RenderSystem.assertOnRenderThread();
      STENCIL.mode.disable();
   }

   public static void _enableStencilTest() {
      RenderSystem.assertOnRenderThread();
      STENCIL.mode.enable();
   }

   public static void _stencilFunc(int func, int ref, int readMask) {
      RenderSystem.assertOnRenderThread();
      if (func != STENCIL.frontFunc
         || ref != STENCIL.frontRef
         || readMask != STENCIL.frontReadMask
         || func != STENCIL.backFunc
         || ref != STENCIL.backRef
         || readMask != STENCIL.backReadMask) {
         STENCIL.frontFunc = func;
         STENCIL.frontRef = ref;
         STENCIL.frontReadMask = readMask;
         STENCIL.backFunc = func;
         STENCIL.backRef = ref;
         STENCIL.backReadMask = readMask;
         GL32.glStencilFuncSeparate(1032, func, ref, readMask);
      }
   }

   public static void _stencilFuncFront(int func, int ref, int readMask) {
      RenderSystem.assertOnRenderThread();
      if (func != STENCIL.frontFunc || ref != STENCIL.frontRef || readMask != STENCIL.frontReadMask) {
         STENCIL.frontFunc = func;
         STENCIL.frontRef = ref;
         STENCIL.frontReadMask = readMask;
         GL32.glStencilFuncSeparate(1028, func, ref, readMask);
      }
   }

   public static void _stencilFuncBack(int func, int ref, int readMask) {
      RenderSystem.assertOnRenderThread();
      if (func != STENCIL.backFunc || ref != STENCIL.backRef || readMask != STENCIL.backReadMask) {
         STENCIL.backFunc = func;
         STENCIL.backRef = ref;
         STENCIL.backReadMask = readMask;
         GL32.glStencilFuncSeparate(1029, func, ref, readMask);
      }
   }

   public static void _stencilMask(int mask) {
      RenderSystem.assertOnRenderThread();
      if (mask != STENCIL.writeMask) {
         STENCIL.writeMask = mask;
         GL11.glStencilMask(mask);
      }
   }

   public static void _stencilOp(int stencilFail, int depthFail, int pass) {
      RenderSystem.assertOnRenderThread();
      if (stencilFail != STENCIL.frontStencilFail
         || depthFail != STENCIL.frontDepthFail
         || pass != STENCIL.frontPass
         || stencilFail != STENCIL.backStencilFail
         || depthFail != STENCIL.backDepthFail
         || pass != STENCIL.backPass) {
         STENCIL.frontStencilFail = stencilFail;
         STENCIL.frontDepthFail = depthFail;
         STENCIL.frontPass = pass;
         STENCIL.backStencilFail = stencilFail;
         STENCIL.backDepthFail = depthFail;
         STENCIL.backPass = pass;
         GL32.glStencilOpSeparate(1032, stencilFail, depthFail, pass);
      }
   }

   public static void _stencilOpFront(int stencilFail, int depthFail, int pass) {
      RenderSystem.assertOnRenderThread();
      if (stencilFail != STENCIL.frontStencilFail || depthFail != STENCIL.frontDepthFail || pass != STENCIL.frontPass) {
         STENCIL.frontStencilFail = stencilFail;
         STENCIL.frontDepthFail = depthFail;
         STENCIL.frontPass = pass;
         GL32.glStencilOpSeparate(1028, stencilFail, depthFail, pass);
      }
   }

   public static void _stencilOpBack(int stencilFail, int depthFail, int pass) {
      RenderSystem.assertOnRenderThread();
      if (stencilFail != STENCIL.backStencilFail || depthFail != STENCIL.backDepthFail || pass != STENCIL.backPass) {
         STENCIL.backStencilFail = stencilFail;
         STENCIL.backDepthFail = depthFail;
         STENCIL.backPass = pass;
         GL32.glStencilOpSeparate(1029, stencilFail, depthFail, pass);
      }
   }

   @OnlyIn(Dist.CLIENT)
   static class BlendState {
      public final GlStateManager.BooleanState mode = new GlStateManager.BooleanState(3042);
      public int srcRgb = 1;
      public int dstRgb = 0;
      public int srcAlpha = 1;
      public int dstAlpha = 0;
   }

   @OnlyIn(Dist.CLIENT)
   static class BooleanState {
      private final int state;
      private boolean enabled;

      public BooleanState(int p_412087_) {
         this.state = p_412087_;
      }

      public void disable() {
         this.setEnabled(false);
      }

      public void enable() {
         this.setEnabled(true);
      }

      public void setEnabled(boolean p_412132_) {
         RenderSystem.assertOnRenderThread();
         if (p_412132_ != this.enabled) {
            this.enabled = p_412132_;
            if (p_412132_) {
               GL11.glEnable(this.state);
            } else {
               GL11.glDisable(this.state);
            }
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   static class ColorLogicState {
      public final GlStateManager.BooleanState enable = new GlStateManager.BooleanState(3058);
      public int op = 5379;
   }

   @OnlyIn(Dist.CLIENT)
   static class ColorMask {
      public boolean red = true;
      public boolean green = true;
      public boolean blue = true;
      public boolean alpha = true;
   }

   @OnlyIn(Dist.CLIENT)
   static class CullState {
      public final GlStateManager.BooleanState enable = new GlStateManager.BooleanState(2884);
   }

   @OnlyIn(Dist.CLIENT)
   static class DepthState {
      public final GlStateManager.BooleanState mode = new GlStateManager.BooleanState(2929);
      public boolean mask = true;
      public int func = 513;
   }

   @OnlyIn(Dist.CLIENT)
   static class PolygonOffsetState {
      public final GlStateManager.BooleanState fill = new GlStateManager.BooleanState(32823);
      public float factor;
      public float units;
   }

   @OnlyIn(Dist.CLIENT)
   static class ScissorState {
      public final GlStateManager.BooleanState mode = new GlStateManager.BooleanState(3089);
   }

   @OnlyIn(Dist.CLIENT)
   static class StencilState {
      public final GlStateManager.BooleanState mode = new GlStateManager.BooleanState(2960);
      public int frontFunc = 519;
      public int frontRef;
      public int frontReadMask = -1;
      public int backFunc = 519;
      public int backRef;
      public int backReadMask = -1;
      public int writeMask = -1;
      public int frontStencilFail = 7680;
      public int frontDepthFail = 7680;
      public int frontPass = 7680;
      public int backStencilFail = 7680;
      public int backDepthFail = 7680;
      public int backPass = 7680;
   }

   @OnlyIn(Dist.CLIENT)
   static class TextureState {
      public int binding;
   }
}
