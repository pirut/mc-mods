package net.neoforged.neoforge.resource;

import com.google.common.collect.Sets;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.SequencedSet;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.server.packs.FeatureFlagsMetadataSection;
import net.minecraft.server.packs.OverlayMetadataSection;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.metadata.MetadataSectionType;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;
import net.minecraft.server.packs.repository.KnownPack;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackCompatibility;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.repository.RepositorySource;
import net.minecraft.util.InclusiveRange;
import net.minecraft.world.flag.FeatureFlagSet;
import net.neoforged.fml.ModList;
import net.neoforged.fml.ModLoader;
import net.neoforged.fml.ModLoadingIssue;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import net.neoforged.neoforgespi.language.IModFileInfo;
import net.neoforged.neoforgespi.language.IModInfo;
import net.neoforged.neoforgespi.locating.IModFile;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.jetbrains.annotations.ApiStatus.Internal;

public class ResourcePackLoader {
   public static final String MOD_DATA_ID = "mod_data";
   public static final String MOD_RESOURCES_ID = "mod_resources";
   private static Map<IModFile, Pack.ResourcesSupplier> modResourcePacks;
   private static final Logger LOGGER = LogManager.getLogger();
   private static final PackSelectionConfig MOD_PACK_SELECTION_CONFIG = new PackSelectionConfig(false, Pack.Position.TOP, false);
   public static final MetadataSectionType<PackMetadataSection> OPTIONAL_FORMAT = MetadataSectionType.fromCodec(
      "pack",
      RecordCodecBuilder.create(
         in -> in.group(
               ComponentSerialization.CODEC.optionalFieldOf("description", Component.empty()).forGetter(PackMetadataSection::description),
               Codec.INT.optionalFieldOf("pack_format", -1).forGetter(PackMetadataSection::packFormat),
               InclusiveRange.codec(Codec.INT).optionalFieldOf("supported_formats").forGetter(PackMetadataSection::supportedFormats)
            )
            .apply(in, PackMetadataSection::new)
      )
   );

   public static Optional<Pack.ResourcesSupplier> getPackFor(String modId) {
      return Optional.ofNullable(ModList.get().getModFileById(modId)).<IModFile>map(IModFileInfo::getFile).map(mf -> modResourcePacks.get(mf));
   }

   public static void populatePackRepository(PackRepository resourcePacks, PackType packType, boolean trusted) {
      findResourcePacks();
      resourcePacks.addPackFinder(buildPackFinder(modResourcePacks, packType));
      ModLoader.postEvent(new AddPackFindersEvent(packType, resourcePacks::addPackFinder, trusted));
   }

   private static synchronized void findResourcePacks() {
      if (modResourcePacks == null) {
         modResourcePacks = ModList.get()
            .getModFiles()
            .stream()
            .filter(mf -> mf.requiredLanguageLoaders().stream().noneMatch(ls -> ls.languageName().equals("minecraft")))
            .map(mf -> Pair.of(mf, createPackForMod(mf)))
            .collect(Collectors.toMap(p -> ((IModFileInfo)p.getFirst()).getFile(), Pair::getSecond, (u, v) -> {
               throw new IllegalStateException(String.format(Locale.ENGLISH, "Duplicate key %s", u));
            }, LinkedHashMap::new));
      }
   }

   public static RepositorySource buildPackFinder(Map<IModFile, Pack.ResourcesSupplier> modResourcePacks, PackType packType) {
      return packAcceptor -> packFinder(modResourcePacks, packAcceptor, packType);
   }

   private static void packFinder(Map<IModFile, Pack.ResourcesSupplier> modResourcePacks, Consumer<Pack> packAcceptor, PackType packType) {
      ArrayList<Pack> hiddenPacks = new ArrayList<>();

      for (Entry<IModFile, Pack.ResourcesSupplier> e : modResourcePacks.entrySet()) {
         IModInfo mod = (IModInfo)e.getKey().getModInfos().get(0);
         if (!"minecraft".equals(mod.getModId())) {
            String name = "mod/" + e.getKey().getModInfos().stream().<CharSequence>map(IModInfo::getModId).collect(Collectors.joining(","));
            String version = e.getKey()
               .getModInfos()
               .stream()
               .map(IModInfo::getVersion)
               .<CharSequence>map(ArtifactVersion::toString)
               .collect(Collectors.joining(","));
            String packName = e.getKey().getFileName();

            try {
               PackLocationInfo locationInfo = new PackLocationInfo(
                  name,
                  Component.literal(packName.isEmpty() ? "[unnamed]" : packName),
                  PackSource.DEFAULT,
                  Optional.of(new KnownPack("neoforge", name, version))
               );
               boolean isRequired = packType == PackType.CLIENT_RESOURCES && mod.getOwningFile().showAsResourcePack()
                  || packType == PackType.SERVER_DATA && mod.getOwningFile().showAsDataPack();
               Pack modPack;
               if (isRequired) {
                  modPack = Pack.readMetaAndCreate(locationInfo, e.getValue(), packType, MOD_PACK_SELECTION_CONFIG);
                  if (modPack == null) {
                     ModLoader.addLoadingIssue(ModLoadingIssue.warning("fml.modloading.brokenresources", new Object[]{e.getKey()}).withAffectedMod(mod));
                     continue;
                  }
               } else {
                  modPack = readWithOptionalMeta(locationInfo, e.getValue(), packType, MOD_PACK_SELECTION_CONFIG);
               }

               if (isRequired) {
                  packAcceptor.accept(modPack);
               } else {
                  hiddenPacks.add(modPack.hidden());
               }
            } catch (IOException var13) {
               LOGGER.error("Failed to read pack.mcmeta file of mod {}", mod.getModId(), var13);
               ModLoader.addLoadingIssue(
                  ModLoadingIssue.warning("fml.modloading.brokenresources", new Object[]{e.getKey()}).withAffectedMod(mod).withCause(var13)
               );
            }
         }
      }

      packAcceptor.accept(makePack(packType, hiddenPacks));
   }

   public static Pack readWithOptionalMeta(PackLocationInfo location, Pack.ResourcesSupplier resources, PackType type, PackSelectionConfig selectionConfig) throws IOException {
      Pack.Metadata packInfo = readMeta(type, location, resources);
      return new Pack(location, resources, packInfo, selectionConfig);
   }

   private static Pack.Metadata readMeta(PackType type, PackLocationInfo location, Pack.ResourcesSupplier resources) throws IOException {
      int currentVersion = SharedConstants.getCurrentVersion().getPackVersion(type);

      Pack.Metadata var11;
      try (PackResources primaryResources = resources.openPrimary(location)) {
         PackMetadataSection metadata = primaryResources.getMetadataSection(OPTIONAL_FORMAT);
         FeatureFlagSet flags = Optional.ofNullable(primaryResources.getMetadataSection(FeatureFlagsMetadataSection.TYPE))
            .map(FeatureFlagsMetadataSection::flags)
            .orElse(FeatureFlagSet.of());
         List<String> vanillaOverlays = Optional.ofNullable(primaryResources.getMetadataSection(OverlayMetadataSection.TYPE))
            .map(section -> section.overlaysForVersion(currentVersion))
            .orElse(List.of());
         List<String> neoOverlays = Optional.ofNullable(primaryResources.getMetadataSection(OverlayMetadataSection.NEOFORGE_TYPE))
            .map(section -> section.overlaysForVersion(currentVersion))
            .orElse(List.of());
         List<String> overlays = new ArrayList<>(vanillaOverlays);
         overlays.addAll(neoOverlays);
         overlays = List.copyOf(overlays);
         if (metadata == null) {
            return new Pack.Metadata(location.title(), PackCompatibility.COMPATIBLE, flags, overlays, primaryResources.isHidden());
         }

         PackCompatibility compatibility;
         if (metadata.packFormat() == -1 && metadata.supportedFormats().isEmpty()) {
            compatibility = PackCompatibility.COMPATIBLE;
         } else {
            compatibility = PackCompatibility.forVersion(Pack.getDeclaredPackVersions(location.id(), metadata), currentVersion);
         }

         var11 = new Pack.Metadata(metadata.description(), compatibility, flags, overlays, primaryResources.isHidden());
      }

      return var11;
   }

   private static Pack makePack(PackType packType, ArrayList<Pack> hiddenPacks) {
      String id = packType == PackType.CLIENT_RESOURCES ? "mod_resources" : "mod_data";
      String name = packType == PackType.CLIENT_RESOURCES ? "Mod Resources" : "Mod Data";
      String descriptionKey = packType == PackType.CLIENT_RESOURCES ? "fml.resources.modresources" : "fml.resources.moddata";
      return Pack.readMetaAndCreate(
            new PackLocationInfo(id, Component.literal(name), PackSource.DEFAULT, Optional.empty()),
            new EmptyPackResources.EmptyResourcesSupplier(
               new PackMetadataSection(Component.translatable(descriptionKey, hiddenPacks.size()), SharedConstants.getCurrentVersion().getPackVersion(packType))
            ),
            packType,
            new PackSelectionConfig(true, Pack.Position.TOP, false)
         )
         .withChildren(hiddenPacks);
   }

   public static Pack.ResourcesSupplier createPackForMod(IModFileInfo mf) {
      return new PathPackResources.PathResourcesSupplier(mf.getFile().getSecureJar().getRootPath());
   }

   public static List<String> getPackNames(PackType packType) {
      List<String> ids = new ArrayList<>();
      ids.addAll(
         ModList.get()
            .getModFiles()
            .stream()
            .filter(packType == PackType.CLIENT_RESOURCES ? IModFileInfo::showAsResourcePack : IModFileInfo::showAsDataPack)
            .filter(mf -> mf.requiredLanguageLoaders().stream().noneMatch(ls -> ls.languageName().equals("minecraft")))
            .<IModFile>map(IModFileInfo::getFile)
            .map(mf -> "mod/" + mf.getModInfos().stream().<CharSequence>map(IModInfo::getModId).collect(Collectors.joining()))
            .toList()
      );
      ids.add(packType == PackType.CLIENT_RESOURCES ? "mod_resources" : "mod_data");
      return ids;
   }

   @Internal
   public static List<Pack> expandAndRemoveRootChildren(Stream<Pack> packs, Collection<Pack> availablePacks) {
      Set<Pack> hiddenSubPacks = Sets.newHashSet();

      for (Pack pack : availablePacks) {
         if (pack.isRequired()) {
            hiddenSubPacks.addAll(pack.getChildren());
         }
      }

      SequencedSet<Pack> orderedPackSet = new LinkedHashSet<>();
      Iterator<Pack> iterator = packs.iterator();

      while (iterator.hasNext()) {
         Pack rootPack = iterator.next();
         if (!rootPack.isHidden() || !orderedPackSet.contains(rootPack) && !hiddenSubPacks.contains(rootPack)) {
            orderedPackSet.addLast(rootPack);

            for (Pack packx : rootPack.getChildren()) {
               orderedPackSet.addLast(packx);
            }
         }
      }

      return new ArrayList<>(orderedPackSet);
   }

   @Internal
   public static void reorderNewlyDiscoveredPacks(Collection<String> set, Collection<String> old, PackRepository packRepository) {
      Set<String> added = Sets.newLinkedHashSet(set);
      Set<String> oldSet = Sets.newLinkedHashSet(old);
      set.clear();
      List<String> newOrder = new ArrayList<>();

      for (String s : added) {
         Pack pack = packRepository.getPack(s);
         if (!oldSet.contains(s) && pack != null && pack.getDefaultPosition() == Pack.Position.BOTTOM) {
            newOrder.add(0, s);
         } else {
            newOrder.add(s);
         }
      }

      set.addAll(newOrder);
   }
}
