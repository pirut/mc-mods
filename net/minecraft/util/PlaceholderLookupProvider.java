package net.minecraft.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JavaOps;
import com.mojang.serialization.Lifecycle;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderOwner;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;

public class PlaceholderLookupProvider implements HolderGetter.Provider {
   final HolderLookup.Provider context;
   final PlaceholderLookupProvider.UniversalLookup lookup = new PlaceholderLookupProvider.UniversalLookup();
   final Map<ResourceKey<Object>, Holder.Reference<Object>> holders = new HashMap<>();
   final Map<TagKey<Object>, HolderSet.Named<Object>> holderSets = new HashMap<>();

   public PlaceholderLookupProvider(HolderLookup.Provider p_399859_) {
      this.context = p_399859_;
   }

   @Override
   public <T> Optional<? extends HolderGetter<T>> lookup(ResourceKey<? extends Registry<? extends T>> p_399488_) {
      return Optional.of(this.lookup.castAsLookup());
   }

   public <V> RegistryOps<V> createSerializationContext(DynamicOps<V> p_399673_) {
      return RegistryOps.create(
         p_399673_,
         new RegistryOps.RegistryInfoLookup() {
            @Override
            public <T> Optional<RegistryOps.RegistryInfo<T>> lookup(ResourceKey<? extends Registry<? extends T>> p_399695_) {
               return PlaceholderLookupProvider.this.context
                  .lookup(p_399695_)
                  .map(RegistryOps.RegistryInfo::fromRegistryLookup)
                  .or(
                     () -> Optional.of(
                        new RegistryOps.RegistryInfo<>(
                           PlaceholderLookupProvider.this.lookup.castAsOwner(), PlaceholderLookupProvider.this.lookup.castAsLookup(), Lifecycle.experimental()
                        )
                     )
                  );
            }
         }
      );
   }

   public RegistryContextSwapper createSwapper() {
      return new RegistryContextSwapper() {
         @Override
         public <T> DataResult<T> swapTo(Codec<T> p_399724_, T p_400105_, HolderLookup.Provider p_399595_) {
            return p_399724_.encodeStart(PlaceholderLookupProvider.this.createSerializationContext(JavaOps.INSTANCE), p_400105_)
               .flatMap(p_400241_ -> p_399724_.parse(p_399595_.createSerializationContext(JavaOps.INSTANCE), p_400241_));
         }
      };
   }

   public boolean hasRegisteredPlaceholders() {
      return !this.holders.isEmpty() || !this.holderSets.isEmpty();
   }

   class UniversalLookup implements HolderGetter<Object>, HolderOwner<Object> {
      @Override
      public Optional<Holder.Reference<Object>> get(ResourceKey<Object> p_400058_) {
         return Optional.of(this.getOrCreate(p_400058_));
      }

      @Override
      public Holder.Reference<Object> getOrThrow(ResourceKey<Object> p_399477_) {
         return this.getOrCreate(p_399477_);
      }

      private Holder.Reference<Object> getOrCreate(ResourceKey<Object> p_400029_) {
         return PlaceholderLookupProvider.this.holders
            .computeIfAbsent(p_400029_, p_399866_ -> Holder.Reference.createStandAlone(this, (ResourceKey<Object>)p_399866_));
      }

      @Override
      public Optional<HolderSet.Named<Object>> get(TagKey<Object> p_399601_) {
         return Optional.of(this.getOrCreate(p_399601_));
      }

      @Override
      public HolderSet.Named<Object> getOrThrow(TagKey<Object> p_399857_) {
         return this.getOrCreate(p_399857_);
      }

      private HolderSet.Named<Object> getOrCreate(TagKey<Object> p_400076_) {
         return PlaceholderLookupProvider.this.holderSets.computeIfAbsent(p_400076_, p_399772_ -> HolderSet.emptyNamed(this, (TagKey<Object>)p_399772_));
      }

      public <T> HolderGetter<T> castAsLookup() {
         return this;
      }

      public <T> HolderOwner<T> castAsOwner() {
         return this;
      }
   }
}
