package net.minecraft.server.packs.metadata.pack;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.MetadataSectionType;
import net.minecraft.util.InclusiveRange;

public record PackMetadataSection(Component description, InclusiveRange<PackFormat> supportedFormats) {
   private static final Codec<PackMetadataSection> FALLBACK_CODEC = RecordCodecBuilder.create(
      p_432482_ -> p_432482_.group(ComponentSerialization.CODEC.fieldOf("description").forGetter(PackMetadataSection::description))
         .apply(p_432482_, p_432483_ -> new PackMetadataSection(p_432483_, new InclusiveRange<>(PackFormat.of(Integer.MAX_VALUE))))
   );
   public static final MetadataSectionType<PackMetadataSection> CLIENT_TYPE = new MetadataSectionType<>("pack", codecForPackType(PackType.CLIENT_RESOURCES));
   public static final MetadataSectionType<PackMetadataSection> SERVER_TYPE = new MetadataSectionType<>("pack", codecForPackType(PackType.SERVER_DATA));
   public static final MetadataSectionType<PackMetadataSection> FALLBACK_TYPE = new MetadataSectionType<>("pack", FALLBACK_CODEC);

   private static Codec<PackMetadataSection> codecForPackType(PackType p_434308_) {
      return RecordCodecBuilder.create(
         p_432485_ -> p_432485_.group(
               ComponentSerialization.CODEC.fieldOf("description").forGetter(PackMetadataSection::description),
               PackFormat.packCodec(p_434308_).forGetter(PackMetadataSection::supportedFormats)
            )
            .apply(p_432485_, PackMetadataSection::new)
      );
   }

   public static MetadataSectionType<PackMetadataSection> forPackType(PackType p_434545_) {
      return switch (p_434545_) {
         case CLIENT_RESOURCES -> CLIENT_TYPE;
         case SERVER_DATA -> SERVER_TYPE;
      };
   }
}
