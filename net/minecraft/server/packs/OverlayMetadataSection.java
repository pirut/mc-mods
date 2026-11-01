package net.minecraft.server.packs;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.regex.Pattern;
import net.minecraft.server.packs.metadata.MetadataSectionType;
import net.minecraft.util.InclusiveRange;
import net.neoforged.neoforge.common.conditions.ConditionalOps;

public record OverlayMetadataSection(List<OverlayMetadataSection.OverlayEntry> overlays) {
   private static final Pattern DIR_VALIDATOR = Pattern.compile("[-_a-zA-Z0-9.]+");
   private static final Codec<OverlayMetadataSection> CODEC = RecordCodecBuilder.create(
      p_294898_ -> p_294898_.group(
            ConditionalOps.decodeListWithElementConditions(OverlayMetadataSection.OverlayEntry.CODEC)
               .fieldOf("entries")
               .forGetter(OverlayMetadataSection::overlays)
         )
         .apply(p_294898_, OverlayMetadataSection::new)
   );
   public static final MetadataSectionType<OverlayMetadataSection> TYPE = MetadataSectionType.fromCodec("overlays", CODEC);
   public static final MetadataSectionType<OverlayMetadataSection> NEOFORGE_TYPE = MetadataSectionType.fromCodec("neoforge:overlays", CODEC);

   private static DataResult<String> validateOverlayDir(String p_296447_) {
      return !DIR_VALIDATOR.matcher(p_296447_).matches()
         ? DataResult.error(() -> p_296447_ + " is not accepted directory name")
         : DataResult.success(p_296447_);
   }

   public List<String> overlaysForVersion(int p_296262_) {
      return this.overlays.stream().filter(p_296207_ -> p_296207_.isApplicable(p_296262_)).map(OverlayMetadataSection.OverlayEntry::overlay).toList();
   }

   public record OverlayEntry(InclusiveRange<Integer> format, String overlay) {
      public static final Codec<OverlayMetadataSection.OverlayEntry> CODEC = RecordCodecBuilder.create(
         p_337556_ -> p_337556_.group(
               InclusiveRange.codec(Codec.INT).fieldOf("formats").forGetter(OverlayMetadataSection.OverlayEntry::format),
               Codec.STRING.validate(OverlayMetadataSection::validateOverlayDir).fieldOf("directory").forGetter(OverlayMetadataSection.OverlayEntry::overlay)
            )
            .apply(p_337556_, OverlayMetadataSection.OverlayEntry::new)
      );

      public boolean isApplicable(int p_295083_) {
         return this.format.isValueInRange(p_295083_);
      }
   }
}
