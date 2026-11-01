package net.neoforged.neoforge.resource;

import com.google.common.collect.Sets;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
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
import net.minecraft.server.packs.FilePackResources;
import net.minecraft.server.packs.OverlayMetadataSection;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.metadata.MetadataSectionType;
import net.minecraft.server.packs.metadata.pack.PackFormat;
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
import net.neoforged.fml.jarcontents.FolderJarContents;
import net.neoforged.fml.jarcontents.JarContents;
import net.neoforged.fml.jarcontents.JarFileContents;
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
   private static final InclusiveRange<PackFormat> UNLIMITED_SUPPORT = new InclusiveRange<>(
      new PackFormat(0, 0), new PackFormat(Integer.MAX_VALUE, Integer.MAX_VALUE)
   );
   private static final MetadataSectionType<PackMetadataSection> OPTIONAL_CLIENT_FORMAT = new MetadataSectionType<>(
      "pack", metadataCodecForPackType(PackType.CLIENT_RESOURCES)
   );
   private static final MetadataSectionType<PackMetadataSection> OPTIONAL_SERVER_FORMAT = new MetadataSectionType<>(
      "pack", metadataCodecForPackType(PackType.SERVER_DATA)
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
            .filter(mfi -> hasResourcePack(mfi.getFile()))
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
         IModFile modFile = e.getKey();
         if (hasResourcePack(e.getKey())) {
            IModFileInfo modFileInfo = modFile.getModFileInfo();
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
               boolean isRequired = packType == PackType.CLIENT_RESOURCES && modFileInfo.showAsResourcePack()
                  || packType == PackType.SERVER_DATA && modFileInfo.showAsDataPack();
               Pack modPack;
               if (isRequired) {
                  modPack = Pack.readMetaAndCreate(locationInfo, e.getValue(), packType, MOD_PACK_SELECTION_CONFIG);
                  if (modPack == null) {
                     ModLoader.addLoadingIssue(ModLoadingIssue.warning("fml.modloading.brokenresources", new Object[]{e.getKey()}).withAffectedModFile(modFile));
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
            } catch (IOException var14) {
               LOGGER.error("Failed to read pack.mcmeta file of {}", modFile, var14);
               ModLoader.addLoadingIssue(
                  ModLoadingIssue.warning("fml.modloading.brokenresources", new Object[]{e.getKey()}).withAffectedModFile(modFile).withCause(var14)
               );
            }
         }
      }

      packAcceptor.accept(makePack(packType, hiddenPacks));
   }

   private static Codec<PackMetadataSection> metadataCodecForPackType(PackType type) {
      int lastPreMinor = PackFormat.lastPreMinorVersion(type);
      MapCodec<InclusiveRange<PackFormat>> formatCodec = PackFormat.IntermediaryFormat.PACK_CODEC
         .flatXmap(
            intermediary -> intermediary.min().isEmpty()
                  && intermediary.max().isEmpty()
                  && intermediary.format().isEmpty()
                  && intermediary.supported().isEmpty()
               ? DataResult.success(UNLIMITED_SUPPORT)
               : intermediary.validate(lastPreMinor, true, false, "Pack", "supported_formats"),
            range -> range.equals(UNLIMITED_SUPPORT)
               ? DataResult.success(new PackFormat.IntermediaryFormat(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()))
               : DataResult.success(PackFormat.IntermediaryFormat.fromRange(range, lastPreMinor))
         );
      return RecordCodecBuilder.create(
         in -> in.group(
               ComponentSerialization.CODEC.optionalFieldOf("description", Component.empty()).forGetter(PackMetadataSection::description),
               formatCodec.forGetter(PackMetadataSection::supportedFormats)
            )
            .apply(in, PackMetadataSection::new)
      );
   }

   private static MetadataSectionType<PackMetadataSection> metadataTypeForPackType(PackType type) {
      return switch (type) {
         case CLIENT_RESOURCES -> OPTIONAL_CLIENT_FORMAT;
         case SERVER_DATA -> OPTIONAL_SERVER_FORMAT;
      };
   }

   public static Pack readWithOptionalMeta(PackLocationInfo location, Pack.ResourcesSupplier resources, PackType type, PackSelectionConfig selectionConfig) throws IOException {
      Pack.Metadata packInfo = readMeta(type, location, resources);
      return new Pack(location, resources, packInfo, selectionConfig);
   }

   private static Pack.Metadata readMeta(PackType type, PackLocationInfo location, Pack.ResourcesSupplier resources) throws IOException {
      PackFormat currentVersion = SharedConstants.getCurrentVersion().packVersion(type);

      Pack.Metadata var13;
      try (PackResources primaryResources = resources.openPrimary(location)) {
         PackMetadataSection metadata = primaryResources.getMetadataSection(metadataTypeForPackType(type));
         FeatureFlagSet flags = Optional.ofNullable(primaryResources.getMetadataSection(FeatureFlagsMetadataSection.TYPE))
            .map(FeatureFlagsMetadataSection::flags)
            .orElse(FeatureFlagSet.of());
         MetadataSectionType<OverlayMetadataSection> vanillaOverlayType = OverlayMetadataSection.forPackType(type);
         List<String> vanillaOverlays = Optional.ofNullable(primaryResources.getMetadataSection(vanillaOverlayType))
            .map(section -> section.overlaysForVersion(currentVersion))
            .orElse(List.of());
         MetadataSectionType<OverlayMetadataSection> neoOverlayType = OverlayMetadataSection.forPackTypeNeoForge(type);
         List<String> neoOverlays = Optional.ofNullable(primaryResources.getMetadataSection(neoOverlayType))
            .map(section -> section.overlaysForVersion(currentVersion))
            .orElse(List.of());
         List<String> overlays = new ArrayList<>(vanillaOverlays);
         overlays.addAll(neoOverlays);
         overlays = List.copyOf(overlays);
         if (metadata == null) {
            return new Pack.Metadata(location.title(), PackCompatibility.COMPATIBLE, flags, overlays, primaryResources.isHidden());
         }

         PackCompatibility compatibility;
         if (metadata.supportedFormats().equals(UNLIMITED_SUPPORT)) {
            compatibility = PackCompatibility.COMPATIBLE;
         } else {
            compatibility = PackCompatibility.forVersion(metadata.supportedFormats(), currentVersion);
         }

         var13 = new Pack.Metadata(metadata.description(), compatibility, flags, overlays, primaryResources.isHidden());
      }

      return var13;
   }

   private static Pack makePack(PackType packType, ArrayList<Pack> hiddenPacks) {
      String id = packType == PackType.CLIENT_RESOURCES ? "mod_resources" : "mod_data";
      String name = packType == PackType.CLIENT_RESOURCES ? "Mod Resources" : "Mod Data";
      String descriptionKey = packType == PackType.CLIENT_RESOURCES ? "fml.resources.modresources" : "fml.resources.moddata";
      return Pack.readMetaAndCreate(
            new PackLocationInfo(id, Component.literal(name), PackSource.DEFAULT, Optional.empty()),
            new EmptyPackResources.EmptyResourcesSupplier(
               new PackMetadataSection(
                  Component.translatable(descriptionKey, hiddenPacks.size()), new InclusiveRange<>(SharedConstants.getCurrentVersion().packVersion(packType))
               )
            ),
            packType,
            new PackSelectionConfig(true, Pack.Position.TOP, false)
         )
         .withChildren(hiddenPacks);
   }

   public static Pack.ResourcesSupplier createPackForMod(IModFileInfo mf) {
      return createPackForJarContents(mf.getFile().getContents());
   }

   public static Pack.ResourcesSupplier createPackForJarContents(JarContents contents) {
      return (Pack.ResourcesSupplier)(switch (contents) {
         case FolderJarContents folderJarContents -> new PathPackResources.PathResourcesSupplier(folderJarContents.getPrimaryPath());
         case JarFileContents jarFileContents -> new FilePackResources.FileResourcesSupplier(jarFileContents.getPrimaryPath());
         default -> new JarContentsPackResources.JarContentsResourcesSupplier(contents);
      });
   }

   public static List<String> getPackNames(PackType packType) {
      List<String> ids = new ArrayList<>();
      ids.addAll(
         ModList.get()
            .getModFiles()
            .stream()
            .filter(packType == PackType.CLIENT_RESOURCES ? IModFileInfo::showAsResourcePack : IModFileInfo::showAsDataPack)
            .<IModFile>map(IModFileInfo::getFile)
            .filter(ResourcePackLoader::hasResourcePack)
            .map(mf -> "mod/" + mf.getModInfos().stream().<CharSequence>map(IModInfo::getModId).collect(Collectors.joining()))
            .toList()
      );
      ids.add(packType == PackType.CLIENT_RESOURCES ? "mod_resources" : "mod_data");
      return ids;
   }

   private static boolean hasResourcePack(IModFile mf) {
      return mf.getModInfos().stream().noneMatch(m -> "minecraft".equals(m.getModId()));
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
