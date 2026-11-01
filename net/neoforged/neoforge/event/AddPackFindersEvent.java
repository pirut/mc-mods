package net.neoforged.neoforge.event;

import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.BuiltInPackSource;
import net.minecraft.server.packs.repository.KnownPack;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.repository.RepositorySource;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.event.IModBusEvent;
import net.neoforged.fml.jarcontents.JarContents;
import net.neoforged.neoforge.resource.JarContentsPackResources;
import net.neoforged.neoforgespi.language.IModInfo;
import org.apache.maven.artifact.versioning.ArtifactVersion;

public class AddPackFindersEvent extends Event implements IModBusEvent {
   private final PackType packType;
   private final Consumer<RepositorySource> sources;
   private final boolean trusted;

   public AddPackFindersEvent(PackType packType, Consumer<RepositorySource> sources, boolean trusted) {
      this.packType = packType;
      this.sources = sources;
      this.trusted = trusted;
   }

   public void addRepositorySource(RepositorySource source) {
      this.sources.accept(source);
   }

   public PackType getPackType() {
      return this.packType;
   }

   public void addPackFinders(
      Identifier packLocation, PackType packType, Component packNameDisplay, PackSource packSource, boolean alwaysActive, Pack.Position packPosition
   ) {
      if (this.getPackType() == packType) {
         IModInfo modInfo = ((ModContainer)ModList.get()
               .getModContainerById(packLocation.getNamespace())
               .orElseThrow(() -> new IllegalArgumentException("Mod not found: " + packLocation.getNamespace())))
            .getModInfo();
         ArtifactVersion version = modInfo.getVersion();
         String prefix = packLocation.getPath();
         Pack pack = Pack.readMetaAndCreate(
            new PackLocationInfo(
               "mod/" + packLocation, packNameDisplay, packSource, Optional.of(new KnownPack("neoforge", "mod/" + packLocation, version.toString()))
            ),
            BuiltInPackSource.fromName(locationInfo -> {
               JarContents contents = modInfo.getOwningFile().getFile().getContents();
               return new JarContentsPackResources(locationInfo, contents, prefix);
            }),
            packType,
            new PackSelectionConfig(alwaysActive, packPosition, false)
         );
         this.addRepositorySource(packConsumer -> packConsumer.accept(pack));
      }
   }

   public boolean isTrusted() {
      return this.trusted;
   }
}
