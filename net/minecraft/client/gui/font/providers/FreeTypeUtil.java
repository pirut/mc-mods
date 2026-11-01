package net.minecraft.client.gui.font.providers;

import com.mojang.logging.LogUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.freetype.FT_Vector;
import org.lwjgl.util.freetype.FreeType;
import org.slf4j.Logger;

@Environment(EnvType.CLIENT)
public class FreeTypeUtil {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final Object LIBRARY_LOCK = new Object();
   private static long library = 0L;

   public static long getLibrary() {
      synchronized (LIBRARY_LOCK) {
         if (library == 0L) {
            MemoryStack memoryStack = MemoryStack.stackPush();

            try {
               PointerBuffer pointerBuffer = memoryStack.mallocPointer(1);
               assertError(FreeType.FT_Init_FreeType(pointerBuffer), "Initializing FreeType library");
               library = pointerBuffer.get();
            } catch (Throwable var6) {
               if (memoryStack != null) {
                  try {
                     memoryStack.close();
                  } catch (Throwable var5) {
                     var6.addSuppressed(var5);
                  }
               }

               throw var6;
            }

            if (memoryStack != null) {
               memoryStack.close();
            }
         }

         return library;
      }
   }

   public static void assertError(int errorId, String action) {
      if (errorId != 0) {
         throw new IllegalStateException("FreeType error: " + describeError(errorId) + " (" + action + ")");
      }
   }

   public static boolean checkError(int errorId, String action) {
      if (errorId != 0) {
         LOGGER.error("FreeType error: {} ({})", describeError(errorId), action);
         return true;
      } else {
         return false;
      }
   }

   private static String describeError(int errorId) {
      String string = FreeType.FT_Error_String(errorId);
      return string != null ? string : "Unrecognized error: 0x" + Integer.toHexString(errorId);
   }

   public static FT_Vector setVector(FT_Vector vector, float x, float y) {
      long l = Math.round(x * 64.0F);
      long m = Math.round(y * 64.0F);
      return vector.set(l, m);
   }

   public static float x(FT_Vector vector) {
      return (float)vector.x() / 64.0F;
   }

   public static void destroy() {
      synchronized (LIBRARY_LOCK) {
         if (library != 0L) {
            FreeType.FT_Done_Library(library);
            library = 0L;
         }
      }
   }
}
