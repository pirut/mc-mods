package net.neoforged.neoforge.registries;

import java.util.Map;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.neoforged.neoforge.registries.callback.RegistryCallback;
import net.neoforged.neoforge.registries.datamaps.DataMapType;
import org.jspecify.annotations.Nullable;

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

   void addAlias(Identifier var1, Identifier var2);

   Identifier resolve(Identifier var1);

   ResourceKey<T> resolve(ResourceKey<T> var1);

   int getId(ResourceKey<T> var1);

   int getId(Identifier var1);

   boolean containsValue(T var1);

   <A> Map<ResourceKey<T>, A> getDataMap(DataMapType<T, A> var1);

   @Nullable
   default Identifier getKeyOrNull(T element) {
      return this.self().getKey(element);
   }
}
