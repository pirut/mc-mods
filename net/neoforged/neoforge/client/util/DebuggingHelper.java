package net.neoforged.neoforge.client.util;

import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
public final class DebuggingHelper {
   private DebuggingHelper() {
      throw new IllegalStateException("Tried to create utility class!");
   }

   public static boolean releaseMouse() {
      Minecraft.getInstance().mouseHandler.releaseMouse();
      return true;
   }
}
