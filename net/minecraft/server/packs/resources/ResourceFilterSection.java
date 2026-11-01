package net.minecraft.server.packs.resources;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.server.packs.metadata.MetadataSectionType;
import net.minecraft.util.IdentifierPattern;

public class ResourceFilterSection {
   private static final Codec<ResourceFilterSection> CODEC = RecordCodecBuilder.create(
      p_466389_ -> p_466389_.group(Codec.list(IdentifierPattern.CODEC).fieldOf("block").forGetter(p_215520_ -> p_215520_.blockList))
         .apply(p_466389_, ResourceFilterSection::new)
   );
   public static final MetadataSectionType<ResourceFilterSection> TYPE = new MetadataSectionType<>("filter", CODEC);
   private final List<IdentifierPattern> blockList;

   public ResourceFilterSection(List<IdentifierPattern> p_215518_) {
      this.blockList = List.copyOf(p_215518_);
   }

   public boolean isNamespaceFiltered(String p_215524_) {
      return this.blockList.stream().anyMatch(p_466388_ -> p_466388_.namespacePredicate().test(p_215524_));
   }

   public boolean isPathFiltered(String p_215529_) {
      return this.blockList.stream().anyMatch(p_466386_ -> p_466386_.pathPredicate().test(p_215529_));
   }
}
