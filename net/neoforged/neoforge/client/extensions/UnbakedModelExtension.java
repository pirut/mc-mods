package net.neoforged.neoforge.client.extensions;

import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.util.context.ContextMap;
import org.jetbrains.annotations.ApiStatus.OverrideOnly;

public interface UnbakedModelExtension extends ResolvableModel {
   @OverrideOnly
   default void fillAdditionalProperties(ContextMap.Builder propertiesBuilder) {
   }

   @Override
   default void resolveDependencies(ResolvableModel.Resolver resolver) {
   }
}
