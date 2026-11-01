package net.neoforged.neoforge.client.extensions;

import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.util.context.ContextKeySet;
import net.minecraft.util.context.ContextMap;
import org.jspecify.annotations.Nullable;

public interface ResolvedModelExtension {
   private ResolvedModel self() {
      return (ResolvedModel)this;
   }

   static ContextMap findTopAdditionalProperties(ResolvedModel topModel) {
      ContextMap.Builder builder = new ContextMap.Builder();
      fillAdditionalProperties(topModel, builder);
      return builder.create(ContextKeySet.EMPTY);
   }

   private static void fillAdditionalProperties(@Nullable ResolvedModel model, ContextMap.Builder propertiesBuilder) {
      if (model != null) {
         fillAdditionalProperties(model.parent(), propertiesBuilder);
         model.wrapped().fillAdditionalProperties(propertiesBuilder);
      }
   }

   default ContextMap getTopAdditionalProperties() {
      return findTopAdditionalProperties(this.self());
   }
}
