package net.minecraft.network;

import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.TypedDataComponent;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record HashedPatchMap(Map<DataComponentType<?>, Integer> addedComponents, Set<DataComponentType<?>> removedComponents) {
   public static final StreamCodec<RegistryFriendlyByteBuf, HashedPatchMap> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.map(HashMap::new, ByteBufCodecs.registry(Registries.DATA_COMPONENT_TYPE), ByteBufCodecs.INT, 256),
      HashedPatchMap::addedComponents,
      ByteBufCodecs.collection(HashSet::new, ByteBufCodecs.registry(Registries.DATA_COMPONENT_TYPE), 256),
      HashedPatchMap::removedComponents,
      HashedPatchMap::new
   );

   public static HashedPatchMap create(DataComponentPatch p_412122_, HashedPatchMap.HashGenerator p_412570_) {
      DataComponentPatch.SplitResult datacomponentpatch$splitresult = p_412122_.split();
      Map<DataComponentType<?>, Integer> map = new IdentityHashMap<>(datacomponentpatch$splitresult.added().size());
      datacomponentpatch$splitresult.added().forEach(p_412727_ -> map.put(p_412727_.type(), p_412570_.apply((TypedDataComponent<?>)p_412727_)));
      return new HashedPatchMap(map, datacomponentpatch$splitresult.removed());
   }

   public boolean matches(DataComponentPatch p_412240_, HashedPatchMap.HashGenerator p_412463_) {
      DataComponentPatch.SplitResult datacomponentpatch$splitresult = p_412240_.split();
      if (!datacomponentpatch$splitresult.removed().equals(this.removedComponents)) {
         return false;
      } else if (this.addedComponents.size() != datacomponentpatch$splitresult.added().size()) {
         return false;
      } else {
         for (TypedDataComponent<?> typeddatacomponent : datacomponentpatch$splitresult.added()) {
            Integer integer = this.addedComponents.get(typeddatacomponent.type());
            if (integer == null) {
               return false;
            }

            Integer integer1 = p_412463_.apply(typeddatacomponent);
            if (!integer1.equals(integer)) {
               return false;
            }
         }

         return true;
      }
   }

   @FunctionalInterface
   public interface HashGenerator extends Function<TypedDataComponent<?>, Integer> {
   }
}
