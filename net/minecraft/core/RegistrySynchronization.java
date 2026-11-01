package net.minecraft.core;

import com.mojang.serialization.DynamicOps;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.nbt.Tag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.RegistryLayer;
import net.minecraft.server.packs.repository.KnownPack;

public class RegistrySynchronization {
   private static final Set<ResourceKey<? extends Registry<?>>> NETWORKABLE_REGISTRIES = RegistryDataLoader.SYNCHRONIZED_REGISTRIES
      .stream()
      .map(RegistryDataLoader.RegistryData::key)
      .collect(Collectors.toUnmodifiableSet());

   public static void packRegistries(
      DynamicOps<Tag> p_321796_,
      RegistryAccess p_321733_,
      Set<KnownPack> p_326143_,
      BiConsumer<ResourceKey<? extends Registry<?>>, List<RegistrySynchronization.PackedRegistryEntry>> p_321539_
   ) {
      RegistryDataLoader.SYNCHRONIZED_REGISTRIES
         .forEach(p_325532_ -> packRegistry(p_321796_, (RegistryDataLoader.RegistryData<?>)p_325532_, p_321733_, p_326143_, p_321539_));
   }

   private static <T> void packRegistry(
      DynamicOps<Tag> p_321608_,
      RegistryDataLoader.RegistryData<T> p_321701_,
      RegistryAccess p_321717_,
      Set<KnownPack> p_326432_,
      BiConsumer<ResourceKey<? extends Registry<?>>, List<RegistrySynchronization.PackedRegistryEntry>> p_321724_
   ) {
      p_321717_.lookup(p_321701_.key())
         .ifPresent(
            p_367812_ -> {
               List<RegistrySynchronization.PackedRegistryEntry> list = new ArrayList<>(p_367812_.size());
               p_367812_.listElements()
                  .forEach(
                     p_465914_ -> {
                        boolean flag = p_367812_.registrationInfo(p_465914_.key())
                           .flatMap(RegistrationInfo::knownPackInfo)
                           .filter(p_326432_::contains)
                           .isPresent();
                        Optional<Tag> optional;
                        if (flag) {
                           optional = Optional.empty();
                        } else {
                           Tag tag = (Tag)p_321701_.elementCodec()
                              .encodeStart(p_321608_, p_465914_.value())
                              .getOrThrow(p_339341_ -> new IllegalArgumentException("Failed to serialize " + p_465914_.key() + ": " + p_339341_));
                           optional = Optional.of(tag);
                        }

                        list.add(new RegistrySynchronization.PackedRegistryEntry(p_465914_.key().identifier(), optional));
                     }
                  );
               p_321724_.accept(p_367812_.key(), list);
            }
         );
   }

   private static Stream<RegistryAccess.RegistryEntry<?>> ownedNetworkableRegistries(RegistryAccess p_251842_) {
      return p_251842_.registries().filter(p_359340_ -> isNetworkable(p_359340_.key()));
   }

   public static Stream<RegistryAccess.RegistryEntry<?>> networkedRegistries(LayeredRegistryAccess<RegistryLayer> p_259290_) {
      return ownedNetworkableRegistries(p_259290_.getAccessFrom(RegistryLayer.WORLDGEN));
   }

   public static Stream<RegistryAccess.RegistryEntry<?>> networkSafeRegistries(LayeredRegistryAccess<RegistryLayer> p_249066_) {
      Stream<RegistryAccess.RegistryEntry<?>> stream = p_249066_.getLayer(RegistryLayer.STATIC).registries();
      Stream<RegistryAccess.RegistryEntry<?>> stream1 = networkedRegistries(p_249066_);
      return Stream.concat(stream1, stream);
   }

   public static boolean isNetworkable(ResourceKey<? extends Registry<?>> p_364744_) {
      return NETWORKABLE_REGISTRIES.contains(p_364744_);
   }

   public record PackedRegistryEntry(Identifier id, Optional<Tag> data) {
      public static final StreamCodec<ByteBuf, RegistrySynchronization.PackedRegistryEntry> STREAM_CODEC = StreamCodec.composite(
         Identifier.STREAM_CODEC,
         RegistrySynchronization.PackedRegistryEntry::id,
         ByteBufCodecs.TAG.apply(ByteBufCodecs::optional),
         RegistrySynchronization.PackedRegistryEntry::data,
         RegistrySynchronization.PackedRegistryEntry::new
      );
   }
}
