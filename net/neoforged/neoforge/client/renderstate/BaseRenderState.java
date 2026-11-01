package net.neoforged.neoforge.client.renderstate;

import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import java.util.Map;
import net.minecraft.util.context.ContextKey;
import net.neoforged.neoforge.client.extensions.IRenderStateExtension;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jspecify.annotations.Nullable;

public abstract class BaseRenderState implements IRenderStateExtension {
   protected final Map<ContextKey<?>, Object> extensions = new Reference2ObjectOpenHashMap();

   @Nullable
   @Override
   public <T> T getRenderData(ContextKey<T> key) {
      return (T)this.extensions.get(key);
   }

   @Override
   public <T> void setRenderData(ContextKey<T> key, @Nullable T data) {
      if (data != null) {
         this.extensions.put(key, data);
      } else {
         this.extensions.remove(key);
      }
   }

   @Internal
   public void resetRenderData() {
      this.extensions.clear();
   }
}
