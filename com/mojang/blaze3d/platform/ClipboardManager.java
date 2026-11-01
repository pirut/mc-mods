package com.mojang.blaze3d.platform;

import com.google.common.base.Charsets;
import java.nio.ByteBuffer;
import net.minecraft.util.StringDecomposer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWErrorCallbackI;
import org.lwjgl.system.MemoryUtil;

@OnlyIn(Dist.CLIENT)
public class ClipboardManager {
   public static final int FORMAT_UNAVAILABLE = 65545;
   private final ByteBuffer clipboardScratchBuffer = BufferUtils.createByteBuffer(8192);

   public String getClipboard(long p_83996_, GLFWErrorCallbackI p_83997_) {
      GLFWErrorCallback $$2 = GLFW.glfwSetErrorCallback(p_83997_);
      String $$3 = GLFW.glfwGetClipboardString(p_83996_);
      $$3 = $$3 != null ? StringDecomposer.filterBrokenSurrogates($$3) : "";
      GLFWErrorCallback $$4 = GLFW.glfwSetErrorCallback($$2);
      if ($$4 != null) {
         $$4.free();
      }

      return $$3;
   }

   private static void pushClipboard(long p_83992_, ByteBuffer p_83993_, byte[] p_83994_) {
      p_83993_.clear();
      p_83993_.put(p_83994_);
      p_83993_.put((byte)0);
      p_83993_.flip();
      GLFW.glfwSetClipboardString(p_83992_, p_83993_);
   }

   public void setClipboard(long p_83989_, String p_83990_) {
      byte[] $$2 = p_83990_.getBytes(Charsets.UTF_8);
      int $$3 = $$2.length + 1;
      if ($$3 < this.clipboardScratchBuffer.capacity()) {
         pushClipboard(p_83989_, this.clipboardScratchBuffer, $$2);
      } else {
         ByteBuffer $$4 = MemoryUtil.memAlloc($$3);

         try {
            pushClipboard(p_83989_, $$4, $$2);
         } finally {
            MemoryUtil.memFree($$4);
         }
      }
   }
}
