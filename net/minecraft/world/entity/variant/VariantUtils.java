package net.minecraft.world.entity.variant;

import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class VariantUtils {
   public static final String TAG_VARIANT = "variant";

   public static <T> Holder<T> getDefaultOrAny(RegistryAccess p_400148_, ResourceKey<T> p_399728_) {
      Registry<T> registry = p_400148_.lookupOrThrow(p_399728_.registryKey());
      return registry.get(p_399728_).or(registry::getAny).orElseThrow();
   }

   public static <T> Holder<T> getAny(RegistryAccess p_399785_, ResourceKey<? extends Registry<T>> p_400271_) {
      return p_399785_.lookupOrThrow(p_400271_).getAny().orElseThrow();
   }

   public static <T> void writeVariant(ValueOutput p_421671_, Holder<T> p_399604_) {
      p_399604_.unwrapKey().ifPresent(p_466622_ -> p_421671_.store("variant", Identifier.CODEC, p_466622_.identifier()));
   }

   public static <T> Optional<Holder<T>> readVariant(ValueInput p_422377_, ResourceKey<? extends Registry<T>> p_399850_) {
      return p_422377_.<Identifier>read("variant", Identifier.CODEC)
         .map(p_466624_ -> ResourceKey.create(p_399850_, p_466624_))
         .flatMap(p_422377_.lookup()::get);
   }

   public static <T extends PriorityProvider<SpawnContext, ?>> Optional<Holder.Reference<T>> selectVariantToSpawn(
      SpawnContext p_427352_, ResourceKey<Registry<T>> p_427473_
   ) {
      ServerLevelAccessor serverlevelaccessor = p_427352_.level();
      Stream<Holder.Reference<T>> stream = serverlevelaccessor.registryAccess().lookupOrThrow(p_427473_).listElements();
      return PriorityProvider.pick(stream, Holder::value, serverlevelaccessor.getRandom(), p_427352_);
   }
}
