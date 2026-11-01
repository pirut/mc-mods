package com.mojang.blaze3d.platform.cursor;

import com.mojang.blaze3d.platform.Window;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.lwjgl.glfw.GLFW;

@OnlyIn(Dist.CLIENT)
public class CursorType {
   public static final CursorType DEFAULT = new CursorType("default", 0L);
   private final String name;
   private final long handle;

   private CursorType(String p_443424_, long p_443217_) {
      this.name = p_443424_;
      this.handle = p_443217_;
   }

   public void select(Window p_442541_) {
      GLFW.glfwSetCursor(p_442541_.handle(), this.handle);
   }

   @Override
   public String toString() {
      return this.name;
   }

   public static CursorType createStandardCursor(int p_442811_, String p_443595_, CursorType p_442863_) {
      long i = GLFW.glfwCreateStandardCursor(p_442811_);
      return i == 0L ? p_442863_ : new CursorType(p_443595_, i);
   }
}
