package net.minecraft.server.packs;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.regex.Pattern;
import net.minecraft.server.packs.metadata.MetadataSectionType;
import net.minecraft.server.packs.metadata.pack.PackFormat;
import net.minecraft.util.InclusiveRange;
import net.neoforged.neoforge.common.conditions.ConditionalOps;

public record OverlayMetadataSection(List<OverlayMetadataSection.OverlayEntry> overlays) {
   private static final Pattern DIR_VALIDATOR = Pattern.compile("[-_a-zA-Z0-9.]+");
   public static final MetadataSectionType<OverlayMetadataSection> CLIENT_TYPE = new MetadataSectionType<>(
      "overlays", codecForPackType(PackType.CLIENT_RESOURCES)
   );
   public static final MetadataSectionType<OverlayMetadataSection> SERVER_TYPE = new MetadataSectionType<>("overlays", codecForPackType(PackType.SERVER_DATA));
   public static final MetadataSectionType<OverlayMetadataSection> NEOFORGE_CLIENT_TYPE = new MetadataSectionType<>("neoforge:overlays", CLIENT_TYPE.codec());
   public static final MetadataSectionType<OverlayMetadataSection> NEOFORGE_SERVER_TYPE = new MetadataSectionType<>("neoforge:overlays", SERVER_TYPE.codec());

   private static DataResult<String> validateOverlayDir(String p_296447_) {
      return !DIR_VALIDATOR.matcher(p_296447_).matches()
         ? DataResult.error(() -> p_296447_ + " is not accepted directory name")
         : DataResult.success(p_296447_);
   }

   @VisibleForTesting
   public static Codec<OverlayMetadataSection> codecForPackType(PackType p_433080_) {
      return RecordCodecBuilder.create(
         p_432471_ -> p_432471_.group(
               OverlayMetadataSection.OverlayEntry.listCodecForPackType(p_433080_).fieldOf("entries").forGetter(OverlayMetadataSection::overlays)
            )
            .apply(p_432471_, OverlayMetadataSection::new)
      );
   }

   public static MetadataSectionType<OverlayMetadataSection> forPackType(PackType p_434837_) {
      return switch (p_434837_) {
         case CLIENT_RESOURCES -> CLIENT_TYPE;
         case SERVER_DATA -> SERVER_TYPE;
      };
   }

   public static MetadataSectionType<OverlayMetadataSection> forPackTypeNeoForge(PackType p_434837_) {
      return switch (p_434837_) {
         case CLIENT_RESOURCES -> NEOFORGE_CLIENT_TYPE;
         case SERVER_DATA -> NEOFORGE_SERVER_TYPE;
      };
   }

   public List<String> overlaysForVersion(PackFormat p_435088_) {
      return this.overlays.stream().filter(p_432473_ -> p_432473_.isApplicable(p_435088_)).map(OverlayMetadataSection.OverlayEntry::overlay).toList();
   }

   public record OverlayEntry(InclusiveRange<PackFormat> format, String overlay) {
      static Codec<List<OverlayMetadataSection.OverlayEntry>> listCodecForPackType(PackType p_434025_) {
         int i = PackFormat.lastPreMinorVersion(p_434025_);
         return ConditionalOps.decodeListWithElementConditions(OverlayMetadataSection.OverlayEntry.IntermediateEntry.CODEC)
            .flatXmap(
               p_432477_ -> PackFormat.validateHolderList(
                  p_432477_, i, (p_432478_, p_432479_) -> new OverlayMetadataSection.OverlayEntry(p_432479_, p_432478_.overlay())
               ),
               p_432475_ -> DataResult.success(
                  p_432475_.stream()
                     .map(
                        p_432481_ -> new OverlayMetadataSection.OverlayEntry.IntermediateEntry(
                           PackFormat.IntermediaryFormat.fromRange(p_432481_.format(), i), p_432481_.overlay()
                        )
                     )
                     .toList()
               )
            );
      }

      public boolean isApplicable(PackFormat p_433137_) {
         return this.format.isValueInRange(p_433137_);
      }

      public record IntermediateEntry(PackFormat.IntermediaryFormat format, String overlay) implements PackFormat.IntermediaryFormatHolder {
         public static final Codec<OverlayMetadataSection.OverlayEntry.IntermediateEntry> CODEC = RecordCodecBuilder.create(
            p_434215_ -> p_434215_.group(
                  PackFormat.IntermediaryFormat.OVERLAY_CODEC.forGetter(OverlayMetadataSection.OverlayEntry.IntermediateEntry::format),
                  Codec.STRING
                     .validate(OverlayMetadataSection::validateOverlayDir)
                     .fieldOf("directory")
                     .forGetter(OverlayMetadataSection.OverlayEntry.IntermediateEntry::overlay)
               )
               .apply(p_434215_, OverlayMetadataSection.OverlayEntry.IntermediateEntry::new)
         );

         @Override
         public String toString() {
            return this.overlay;
         }
      }
   }
}
