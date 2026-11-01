package net.neoforged.neoforge.common.util;

import com.mojang.serialization.Codec;
import net.minecraft.Util;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.Nullable;

public class DataComponentUtil {
   public static <T extends DataComponentHolder> Tag wrapEncodingExceptions(T componentHolder, Codec<T> codec, HolderLookup.Provider provider, Tag tag) {
      try {
         return (Tag)codec.encode(componentHolder, provider.createSerializationContext(NbtOps.INSTANCE), tag).getOrThrow();
      } catch (Exception var5) {
         logDataComponentSaveError(componentHolder, var5, tag);
         throw var5;
      }
   }

   public static <T extends DataComponentHolder> Tag wrapEncodingExceptions(T componentHolder, Codec<T> codec, HolderLookup.Provider provider) {
      try {
         return (Tag)codec.encodeStart(provider.createSerializationContext(NbtOps.INSTANCE), componentHolder).getOrThrow();
      } catch (Exception var4) {
         logDataComponentSaveError(componentHolder, var4, null);
         throw var4;
      }
   }

   public static void logDataComponentSaveError(DataComponentHolder componentHolder, Exception original, @Nullable Tag tag) {
      StringBuilder cause = new StringBuilder("Error saving [" + componentHolder + "]. Original cause: " + original);
      cause.append("\nWith components:\n{");
      componentHolder.getComponents().forEach(component -> cause.append("\n\t").append(component));
      cause.append("\n}");
      if (tag != null) {
         cause.append("\nWith tag: ").append(tag);
      }

      Util.logAndPauseIfInIde(cause.toString());
   }
}
