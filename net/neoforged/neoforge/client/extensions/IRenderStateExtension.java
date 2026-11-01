package net.neoforged.neoforge.client.extensions;

import net.minecraft.util.context.ContextKey;
import org.jspecify.annotations.Nullable;

public interface IRenderStateExtension {
   @Nullable
   <T> T getRenderData(ContextKey<T> var1);

   <T> void setRenderData(ContextKey<T> var1, @Nullable T var2);

   default <T> T getRenderDataOrThrow(ContextKey<T> key) {
      T data = this.getRenderData(key);
      if (data == null) {
         throw new IllegalStateException("No value associated for key " + key);
      } else {
         return data;
      }
   }

   default <T> T getRenderDataOrDefault(ContextKey<T> key, T defaultVal) {
      T data = this.getRenderData(key);
      return data == null ? defaultVal : data;
   }
}
