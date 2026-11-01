package net.neoforged.neoforge.registries;

import java.util.Map;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.registries.callback.RegistryCallback;
import net.neoforged.neoforge.registries.datamaps.DataMapType;
import org.jetbrains.annotations.Nullable;

public interface IRegistryExtension<T> {
   private Registry<T> self() {
      return (Registry<T>)this;
   }

   boolean doesSync();

   int getMaxId();

   void addCallback(RegistryCallback<T> var1);

   default <C extends RegistryCallback<T>> void addCallback(Class<C> type, C callback) {
      this.addCallback(callback);
   }

   void addAlias(ResourceLocation var1, ResourceLocation var2);

   ResourceLocation resolve(ResourceLocation var1);

   ResourceKey<T> resolve(ResourceKey<T> var1);

   int getId(ResourceKey<T> var1);

   int getId(ResourceLocation var1);

   boolean containsValue(T var1);

   @Nullable
   <A> A getData(DataMapType<T, A> var1, ResourceKey<T> var2);

   <A> Map<ResourceKey<T>, A> getDataMap(DataMapType<T, A> var1);

   @Nullable
   default ResourceLocation getKeyOrNull(T element) {
      return this.self().getKey(element);
   }
}
