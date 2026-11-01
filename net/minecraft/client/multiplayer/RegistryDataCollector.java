package net.minecraft.client.multiplayer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.RegistrySynchronization;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.resources.ResourceProvider;
import net.minecraft.tags.TagLoader;
import net.minecraft.tags.TagNetworkSerialization;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class RegistryDataCollector {
   @Nullable
   private RegistryDataCollector.ContentsCollector contentsCollector;
   @Nullable
   private RegistryDataCollector.TagCollector tagCollector;

   public void appendContents(ResourceKey<? extends Registry<?>> p_321794_, List<RegistrySynchronization.PackedRegistryEntry> p_321772_) {
      if (this.contentsCollector == null) {
         this.contentsCollector = new RegistryDataCollector.ContentsCollector();
      }

      this.contentsCollector.append(p_321794_, p_321772_);
   }

   public void appendTags(Map<ResourceKey<? extends Registry<?>>, TagNetworkSerialization.NetworkPayload> p_321771_) {
      if (this.tagCollector == null) {
         this.tagCollector = new RegistryDataCollector.TagCollector();
      }

      p_321771_.forEach(this.tagCollector::append);
   }

   private static <T> Registry.PendingTags<T> resolveRegistryTags(
      RegistryAccess.Frozen p_361269_, ResourceKey<? extends Registry<? extends T>> p_363497_, TagNetworkSerialization.NetworkPayload p_364570_
   ) {
      Registry<T> registry = p_361269_.lookupOrThrow(p_363497_);
      return registry.prepareTagReload(p_364570_.resolve(registry));
   }

   private RegistryAccess loadNewElementsAndTags(ResourceProvider p_363026_, RegistryDataCollector.ContentsCollector p_360790_, boolean p_360761_) {
      LayeredRegistryAccess<ClientRegistryLayer> layeredregistryaccess = ClientRegistryLayer.createRegistryAccess();
      RegistryAccess.Frozen registryaccess$frozen = layeredregistryaccess.getAccessForLoading(ClientRegistryLayer.REMOTE);
      Map<ResourceKey<? extends Registry<?>>, RegistryDataLoader.NetworkedRegistryData> map = new HashMap<>();
      p_360790_.elements
         .forEach(
            (p_360955_, p_361011_) -> map.put(
               (ResourceKey<? extends Registry<?>>)p_360955_,
               new RegistryDataLoader.NetworkedRegistryData(
                  (List<RegistrySynchronization.PackedRegistryEntry>)p_361011_, TagNetworkSerialization.NetworkPayload.EMPTY
               )
            )
         );
      List<Registry.PendingTags<?>> list = new ArrayList<>();
      if (this.tagCollector != null) {
         this.tagCollector.forEach((p_364406_, p_365188_) -> {
            if (!p_365188_.isEmpty()) {
               if (RegistrySynchronization.isNetworkable((ResourceKey<? extends Registry<?>>)p_364406_)) {
                  map.compute((ResourceKey<? extends Registry<?>>)p_364406_, (p_363401_, p_364225_) -> {
                     List<RegistrySynchronization.PackedRegistryEntry> list2 = p_364225_ != null ? p_364225_.elements() : List.of();
                     return new RegistryDataLoader.NetworkedRegistryData(list2, p_365188_);
                  });
               } else if (!p_360761_) {
                  list.add(resolveRegistryTags(registryaccess$frozen, (ResourceKey<? extends Registry<?>>)p_364406_, p_365188_));
               }
            }
         });
      }

      List<HolderLookup.RegistryLookup<?>> list1 = TagLoader.buildUpdatedLookups(registryaccess$frozen, list);

      RegistryAccess.Frozen registryaccess$frozen1;
      try {
         registryaccess$frozen1 = RegistryDataLoader.load(map, p_363026_, list1, RegistryDataLoader.SYNCHRONIZED_REGISTRIES).freeze();
      } catch (Exception var12) {
         CrashReport crashreport = CrashReport.forThrowable(var12, "Network Registry Load");
         addCrashDetails(crashreport, map, list);
         throw new ReportedException(crashreport);
      }

      RegistryAccess registryaccess = layeredregistryaccess.replaceFrom(ClientRegistryLayer.REMOTE, registryaccess$frozen1).compositeAccess();
      list.forEach(Registry.PendingTags::apply);
      return registryaccess;
   }

   private static void addCrashDetails(
      CrashReport p_380152_,
      Map<ResourceKey<? extends Registry<?>>, RegistryDataLoader.NetworkedRegistryData> p_380213_,
      List<Registry.PendingTags<?>> p_379926_
   ) {
      CrashReportCategory crashreportcategory = p_380152_.addCategory("Received Elements and Tags");
      crashreportcategory.setDetail(
         "Dynamic Registries",
         () -> p_380213_.entrySet()
            .stream()
            .sorted(Comparator.comparing(p_465555_ -> p_465555_.getKey().identifier()))
            .map(
               p_465553_ -> String.format(
                  Locale.ROOT,
                  "\n\t\t%s: elements=%d tags=%d",
                  p_465553_.getKey().identifier(),
                  p_465553_.getValue().elements().size(),
                  p_465553_.getValue().tags().size()
               )
            )
            .collect(Collectors.joining())
      );
      crashreportcategory.setDetail(
         "Static Registries",
         () -> p_379926_.stream()
            .sorted(Comparator.comparing(p_465554_ -> p_465554_.key().identifier()))
            .map(p_465556_ -> String.format(Locale.ROOT, "\n\t\t%s: tags=%d", p_465556_.key().identifier(), p_465556_.size()))
            .collect(Collectors.joining())
      );
   }

   private void loadOnlyTags(RegistryDataCollector.TagCollector p_361700_, RegistryAccess.Frozen p_363342_, boolean p_360419_) {
      p_361700_.forEach((p_360314_, p_361795_) -> {
         if (p_360419_ || RegistrySynchronization.isNetworkable((ResourceKey<? extends Registry<?>>)p_360314_)) {
            resolveRegistryTags(p_363342_, (ResourceKey<? extends Registry<?>>)p_360314_, p_361795_).apply();
         }
      });
   }

   public RegistryAccess.Frozen collectGameRegistries(ResourceProvider p_326319_, RegistryAccess.Frozen p_363224_, boolean p_321683_) {
      RegistryAccess registryaccess;
      if (this.contentsCollector != null) {
         registryaccess = this.loadNewElementsAndTags(p_326319_, this.contentsCollector, p_321683_);
      } else {
         if (this.tagCollector != null) {
            this.loadOnlyTags(this.tagCollector, p_363224_, !p_321683_);
         }

         registryaccess = p_363224_;
      }

      return registryaccess.freeze();
   }

   @OnlyIn(Dist.CLIENT)
   static class ContentsCollector {
      final Map<ResourceKey<? extends Registry<?>>, List<RegistrySynchronization.PackedRegistryEntry>> elements = new HashMap<>();

      public void append(ResourceKey<? extends Registry<?>> p_321577_, List<RegistrySynchronization.PackedRegistryEntry> p_321551_) {
         this.elements.computeIfAbsent(p_321577_, p_321745_ -> new ArrayList<>()).addAll(p_321551_);
      }
   }

   @OnlyIn(Dist.CLIENT)
   static class TagCollector {
      private final Map<ResourceKey<? extends Registry<?>>, TagNetworkSerialization.NetworkPayload> tags = new HashMap<>();

      public void append(ResourceKey<? extends Registry<?>> p_360353_, TagNetworkSerialization.NetworkPayload p_361162_) {
         this.tags.put(p_360353_, p_361162_);
      }

      public void forEach(BiConsumer<? super ResourceKey<? extends Registry<?>>, ? super TagNetworkSerialization.NetworkPayload> p_361476_) {
         this.tags.forEach(p_361476_);
      }
   }
}
