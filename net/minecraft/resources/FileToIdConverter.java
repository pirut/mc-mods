package net.minecraft.resources;

import java.util.List;
import java.util.Map;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

public class FileToIdConverter {
   private final String prefix;
   private final String extension;

   public FileToIdConverter(String p_248876_, String p_251478_) {
      this.prefix = p_248876_;
      this.extension = p_251478_;
   }

   public static FileToIdConverter json(String p_248754_) {
      return new FileToIdConverter(p_248754_, ".json");
   }

   public static FileToIdConverter registry(ResourceKey<? extends Registry<?>> p_387510_) {
      return json(Registries.elementsDirPath(p_387510_));
   }

   public Identifier idToFile(Identifier p_466905_) {
      return p_466905_.withPath(this.prefix + "/" + p_466905_.getPath() + this.extension);
   }

   public Identifier fileToId(Identifier p_468192_) {
      String s = p_468192_.getPath();
      return p_468192_.withPath(s.substring(this.prefix.length() + 1, s.length() - this.extension.length()));
   }

   public Map<Identifier, Resource> listMatchingResources(ResourceManager p_252045_) {
      return p_252045_.listResources(this.prefix, p_466119_ -> p_466119_.getPath().endsWith(this.extension));
   }

   public Map<Identifier, List<Resource>> listMatchingResourceStacks(ResourceManager p_249881_) {
      return p_249881_.listResourceStacks(this.prefix, p_466118_ -> p_466118_.getPath().endsWith(this.extension));
   }

   public Map<Identifier, Resource> listMatchingResourcesFromNamespace(ResourceManager manager, String namespace) {
      return manager.listResources(this.prefix, path -> path.getNamespace().equals(namespace) && path.getPath().endsWith(this.extension));
   }

   public Map<Identifier, List<Resource>> listMatchingResourceStacksFromNamespace(ResourceManager manager, String namespace) {
      return manager.listResourceStacks(this.prefix, path -> path.getNamespace().equals(namespace) && path.getPath().endsWith(this.extension));
   }
}
