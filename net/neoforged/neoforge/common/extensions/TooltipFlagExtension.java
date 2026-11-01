package net.neoforged.neoforge.common.extensions;

public interface TooltipFlagExtension {
   default boolean hasControlDown() {
      return false;
   }

   default boolean hasShiftDown() {
      return false;
   }

   default boolean hasAltDown() {
      return false;
   }

   default boolean shouldDisplayAllInformation() {
      return false;
   }
}
