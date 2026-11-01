package com.mojang.blaze3d.platform;

import ca.weblite.objc.Client;
import ca.weblite.objc.NSObject;
import com.sun.jna.Pointer;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import java.util.Optional;
import net.minecraft.server.packs.resources.IoSupplier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.lwjgl.glfw.GLFWNativeCocoa;

@OnlyIn(Dist.CLIENT)
public class MacosUtil {
   private static final int NS_RESIZABLE_WINDOW_MASK = 8;
   private static final int NS_FULL_SCREEN_WINDOW_MASK = 16384;

   public static void exitNativeFullscreen(long p_182518_) {
      getNsWindow(p_182518_).filter(MacosUtil::isInNativeFullscreen).ifPresent(MacosUtil::toggleNativeFullscreen);
   }

   public static void clearResizableBit(long p_304542_) {
      getNsWindow(p_304542_).ifPresent(p_304987_ -> {
         long $$1 = getStyleMask(p_304987_);
         p_304987_.send("setStyleMask:", new Object[]{$$1 & -9L});
      });
   }

   private static Optional<NSObject> getNsWindow(long p_182522_) {
      long $$1 = GLFWNativeCocoa.glfwGetCocoaWindow(p_182522_);
      return $$1 != 0L ? Optional.of(new NSObject(new Pointer($$1))) : Optional.empty();
   }

   private static boolean isInNativeFullscreen(NSObject p_304820_) {
      return (getStyleMask(p_304820_) & 16384L) != 0L;
   }

   private static long getStyleMask(NSObject p_304421_) {
      return (Long)p_304421_.sendRaw("styleMask", new Object[0]);
   }

   private static void toggleNativeFullscreen(NSObject p_182524_) {
      p_182524_.send("toggleFullScreen:", new Object[]{Pointer.NULL});
   }

   public static void loadIcon(IoSupplier<InputStream> p_250929_) throws IOException {
      try (InputStream $$1 = p_250929_.get()) {
         String $$2 = Base64.getEncoder().encodeToString($$1.readAllBytes());
         Client $$3 = Client.getInstance();
         Object $$4 = $$3.sendProxy("NSData", "alloc", new Object[0]).send("initWithBase64Encoding:", new Object[]{$$2});
         Object $$5 = $$3.sendProxy("NSImage", "alloc", new Object[0]).send("initWithData:", new Object[]{$$4});
         $$3.sendProxy("NSApplication", "sharedApplication", new Object[0]).send("setApplicationIconImage:", new Object[]{$$5});
      }
   }
}
