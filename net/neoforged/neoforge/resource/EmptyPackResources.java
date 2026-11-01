package net.neoforged.neoforge.resource;

import java.io.InputStream;
import java.util.Collections;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.AbstractPackResources;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.resources.IoSupplier;
import org.jetbrains.annotations.Nullable;

public class EmptyPackResources extends AbstractPackResources {
   private final PackMetadataSection packMeta;

   public EmptyPackResources(PackLocationInfo packId, PackMetadataSection packMeta) {
      super(packId);
      this.packMeta = packMeta;
   }

   @Nullable
   @Override
   public <T> T getMetadataSection(MetadataSectionSerializer<T> deserializer) {
      return (T)(deserializer.getMetadataSectionName().equals("pack") ? this.packMeta : null);
   }

   @Override
   public void close() {
   }

   @Override
   public void listResources(PackType type, String resourceNamespace, String paths, PackResources.ResourceOutput resourceOutput) {
   }

   @Override
   public Set<String> getNamespaces(PackType type) {
      return Collections.emptySet();
   }

   @Nullable
   @Override
   public IoSupplier<InputStream> getRootResource(String... paths) {
      return null;
   }

   @Nullable
   @Override
   public IoSupplier<InputStream> getResource(PackType type, ResourceLocation location) {
      return null;
   }

   public static class EmptyResourcesSupplier implements Pack.ResourcesSupplier {
      private final PackMetadataSection packMeta;

      public EmptyResourcesSupplier(PackMetadataSection packMeta) {
         this.packMeta = packMeta;
      }

      @Override
      public PackResources openPrimary(PackLocationInfo id) {
         return new EmptyPackResources(id, this.packMeta);
      }

      @Override
      public PackResources openFull(PackLocationInfo id, Pack.Metadata info) {
         return this.openPrimary(id);
      }
   }
}
