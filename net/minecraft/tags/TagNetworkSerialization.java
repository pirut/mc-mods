package net.minecraft.tags;

import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistrySynchronization;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.RegistryLayer;

public class TagNetworkSerialization {
   public static Map<ResourceKey<? extends Registry<?>>, TagNetworkSerialization.NetworkPayload> serializeTagsToNetwork(
      LayeredRegistryAccess<RegistryLayer> p_251774_
   ) {
      return RegistrySynchronization.networkSafeRegistries(p_251774_)
         .map(p_203949_ -> Pair.of(p_203949_.key(), serializeToNetwork(p_203949_.value())))
         .filter(p_321439_ -> ((TagNetworkSerialization.NetworkPayload)p_321439_.getSecond()).size() > 0)
         .collect(Collectors.toMap(Pair::getFirst, Pair::getSecond));
   }

   private static <T> TagNetworkSerialization.NetworkPayload serializeToNetwork(Registry<T> p_203943_) {
      Map<ResourceLocation, IntList> $$1 = new HashMap<>();
      p_203943_.getTags().forEach(p_339471_ -> {
         HolderSet<T> $$3 = (HolderSet<T>)p_339471_.getSecond();
         IntList $$4 = new IntArrayList($$3.size());

         for (Holder<T> $$5 : $$3) {
            if ($$5.kind() != Holder.Kind.REFERENCE) {
               throw new IllegalStateException("Can't serialize unregistered value " + $$5);
            }

            $$4.add(p_203943_.getId($$5.value()));
         }

         $$1.put(((TagKey)p_339471_.getFirst()).location(), $$4);
      });
      return new TagNetworkSerialization.NetworkPayload($$1);
   }

   static <T> void deserializeTagsFromNetwork(
      ResourceKey<? extends Registry<T>> p_203953_,
      Registry<T> p_203954_,
      TagNetworkSerialization.NetworkPayload p_203955_,
      TagNetworkSerialization.TagOutput<T> p_203956_
   ) {
      p_203955_.tags.forEach((p_248278_, p_248279_) -> {
         TagKey<T> $$5 = TagKey.create(p_203953_, p_248278_);
         List<Holder<T>> $$6 = p_248279_.intStream().mapToObj(p_203954_::getHolder).flatMap(Optional::stream).collect(Collectors.toUnmodifiableList());
         p_203956_.accept($$5, $$6);
      });
   }

   public static final class NetworkPayload {
      final Map<ResourceLocation, IntList> tags;

      NetworkPayload(Map<ResourceLocation, IntList> p_203965_) {
         this.tags = p_203965_;
      }

      public void write(FriendlyByteBuf p_203968_) {
         p_203968_.writeMap(this.tags, FriendlyByteBuf::writeResourceLocation, FriendlyByteBuf::writeIntIdList);
      }

      public static TagNetworkSerialization.NetworkPayload read(FriendlyByteBuf p_203970_) {
         return new TagNetworkSerialization.NetworkPayload(p_203970_.readMap(FriendlyByteBuf::readResourceLocation, FriendlyByteBuf::readIntIdList));
      }

      public int size() {
         return this.tags.size();
      }

      public <T> void applyToRegistry(Registry<T> p_321755_) {
         if (this.size() != 0) {
            Map<TagKey<T>, List<Holder<T>>> $$1 = new HashMap<>(this.size());
            TagNetworkSerialization.deserializeTagsFromNetwork(p_321755_.key(), p_321755_, this, $$1::put);
            p_321755_.bindTags($$1);
         }
      }
   }

   @FunctionalInterface
   public interface TagOutput<T> {
      void accept(TagKey<T> var1, List<Holder<T>> var2);
   }
}
