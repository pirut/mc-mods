package net.neoforged.neoforge.common;

import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
public final class NeoForgeVersion {
   private static final NeoForgeBuildType BUILD_TYPE = NeoForgeBuildType.valueOf("STABLE");

   private NeoForgeVersion() {
   }

   public static String getVersion() {
      return "21.11.48";
   }

   public static NeoForgeBuildType getBuildType() {
      return BUILD_TYPE;
   }
}
