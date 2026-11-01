package net.minecraft.server.packs.metadata.pack;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.DataResult.Error;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.BiFunction;
import net.minecraft.server.packs.PackType;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.InclusiveRange;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public record PackFormat(int major, int minor) implements Comparable<PackFormat> {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final Codec<PackFormat> BOTTOM_CODEC = fullCodec(0);
   public static final Codec<PackFormat> TOP_CODEC = fullCodec(Integer.MAX_VALUE);

   private static Codec<PackFormat> fullCodec(int defaultMinorVersion) {
      return ExtraCodecs.compactListCodec(ExtraCodecs.NON_NEGATIVE_INT, ExtraCodecs.NON_NEGATIVE_INT.listOf(1, 256))
         .xmap(
            list -> list.size() > 1 ? of((Integer)list.getFirst(), (Integer)list.get(1)) : of((Integer)list.getFirst(), defaultMinorVersion),
            packFormat -> packFormat.minor != defaultMinorVersion ? List.of(packFormat.major(), packFormat.minor()) : List.of(packFormat.major())
         );
   }

   public static <ResultType, HolderType extends PackFormat.IntermediaryFormatHolder> DataResult<List<ResultType>> validateHolderList(
      List<HolderType> holderList, int versionThreshold, BiFunction<HolderType, InclusiveRange<PackFormat>, ResultType> mapper
   ) {
      int i = holderList.stream()
         .map(PackFormat.IntermediaryFormatHolder::format)
         .mapToInt(PackFormat.IntermediaryFormat::effectiveMinMajorVersion)
         .min()
         .orElse(Integer.MAX_VALUE);
      List<ResultType> list = new ArrayList<>(holderList.size());

      for (HolderType intermediaryFormatHolder : holderList) {
         PackFormat.IntermediaryFormat intermediaryFormat = intermediaryFormatHolder.format();
         if (intermediaryFormat.min().isEmpty() && intermediaryFormat.max().isEmpty() && intermediaryFormat.supported().isEmpty()) {
            LOGGER.warn("Unknown or broken overlay entry {}", intermediaryFormatHolder);
         } else {
            DataResult<InclusiveRange<PackFormat>> dataResult = intermediaryFormat.validate(
               versionThreshold, false, i <= versionThreshold, "Overlay \"" + intermediaryFormatHolder + "\"", "formats"
            );
            if (!dataResult.isSuccess()) {
               return DataResult.error(((Error)dataResult.error().get())::message);
            }

            list.add(mapper.apply(intermediaryFormatHolder, (InclusiveRange<PackFormat>)dataResult.getOrThrow()));
         }
      }

      return DataResult.success(List.copyOf(list));
   }

   @VisibleForTesting
   public static int lastPreMinorVersion(PackType type) {
      return switch (type) {
         case CLIENT_RESOURCES -> 64;
         case SERVER_DATA -> 81;
      };
   }

   public static MapCodec<InclusiveRange<PackFormat>> packCodec(PackType packType) {
      int i = lastPreMinorVersion(packType);
      return PackFormat.IntermediaryFormat.PACK_CODEC
         .flatXmap(
            intermediaryFormat -> intermediaryFormat.validate(i, true, false, "Pack", "supported_formats"),
            inclusiveRange -> DataResult.success(PackFormat.IntermediaryFormat.fromRange(inclusiveRange, i))
         );
   }

   public static PackFormat of(int major, int minor) {
      return new PackFormat(major, minor);
   }

   public static PackFormat of(int major) {
      return new PackFormat(major, 0);
   }

   public InclusiveRange<PackFormat> minorRange() {
      return new InclusiveRange<>(this, of(this.major, Integer.MAX_VALUE));
   }

   public int compareTo(PackFormat other) {
      int i = Integer.compare(this.major(), other.major());
      return i != 0 ? i : Integer.compare(this.minor(), other.minor());
   }

   @Override
   public String toString() {
      return this.minor == Integer.MAX_VALUE
         ? String.format(Locale.ROOT, "%d.*", this.major())
         : String.format(Locale.ROOT, "%d.%d", this.major(), this.minor());
   }

   public record IntermediaryFormat(Optional<PackFormat> min, Optional<PackFormat> max, Optional<Integer> format, Optional<InclusiveRange<Integer>> supported) {
      static final MapCodec<PackFormat.IntermediaryFormat> PACK_CODEC = RecordCodecBuilder.mapCodec(
         instance -> instance.group(
               PackFormat.BOTTOM_CODEC.optionalFieldOf("min_format").forGetter(PackFormat.IntermediaryFormat::min),
               PackFormat.TOP_CODEC.optionalFieldOf("max_format").forGetter(PackFormat.IntermediaryFormat::max),
               Codec.INT.optionalFieldOf("pack_format").forGetter(PackFormat.IntermediaryFormat::format),
               InclusiveRange.codec(Codec.INT).optionalFieldOf("supported_formats").forGetter(PackFormat.IntermediaryFormat::supported)
            )
            .apply(instance, PackFormat.IntermediaryFormat::new)
      );
      public static final MapCodec<PackFormat.IntermediaryFormat> OVERLAY_CODEC = RecordCodecBuilder.mapCodec(
         instance -> instance.group(
               PackFormat.BOTTOM_CODEC.optionalFieldOf("min_format").forGetter(PackFormat.IntermediaryFormat::min),
               PackFormat.TOP_CODEC.optionalFieldOf("max_format").forGetter(PackFormat.IntermediaryFormat::max),
               InclusiveRange.codec(Codec.INT).optionalFieldOf("formats").forGetter(PackFormat.IntermediaryFormat::supported)
            )
            .apply(
               instance, (optional, optional2, optional3) -> new PackFormat.IntermediaryFormat(optional, optional2, optional.map(PackFormat::major), optional3)
            )
      );

      public static PackFormat.IntermediaryFormat fromRange(InclusiveRange<PackFormat> range, int versionThreshold) {
         InclusiveRange<Integer> inclusiveRange = range.map(PackFormat::major);
         return new PackFormat.IntermediaryFormat(
            Optional.of(range.minInclusive()),
            Optional.of(range.maxInclusive()),
            inclusiveRange.isValueInRange(versionThreshold) ? Optional.of(inclusiveRange.minInclusive()) : Optional.empty(),
            inclusiveRange.isValueInRange(versionThreshold)
               ? Optional.of(new InclusiveRange<>(inclusiveRange.minInclusive(), inclusiveRange.maxInclusive()))
               : Optional.empty()
         );
      }

      public int effectiveMinMajorVersion() {
         if (this.min.isPresent()) {
            return this.supported.isPresent() ? Math.min(this.min.get().major(), this.supported.get().minInclusive()) : this.min.get().major();
         } else {
            return this.supported.isPresent() ? this.supported.get().minInclusive() : Integer.MAX_VALUE;
         }
      }

      public DataResult<InclusiveRange<PackFormat>> validate(
         int versionThreshold, boolean requireOldFormat, boolean requireNewFormat, String contextLabel, String oldFormatsKey
      ) {
         if (this.min.isPresent() != this.max.isPresent()) {
            return DataResult.error(() -> contextLabel + " missing field, must declare both min_format and max_format");
         } else if (requireNewFormat && this.supported.isEmpty()) {
            return DataResult.error(
               () -> contextLabel
                  + " missing required field "
                  + oldFormatsKey
                  + ", must be present in all overlays for any overlays to work across game versions"
            );
         } else if (this.min.isPresent()) {
            return this.validateNewFormat(versionThreshold, requireOldFormat, requireNewFormat, contextLabel, oldFormatsKey);
         } else if (this.supported.isPresent()) {
            return this.validateOldFormat(versionThreshold, requireOldFormat, contextLabel, oldFormatsKey);
         } else if (requireOldFormat && this.format.isPresent()) {
            int i = this.format.get();
            return i > versionThreshold
               ? DataResult.error(
                  () -> contextLabel
                     + " declares support for version newer than "
                     + versionThreshold
                     + ", but is missing mandatory fields min_format and max_format"
               )
               : DataResult.success(new InclusiveRange<>(PackFormat.of(i)));
         } else {
            return DataResult.error(() -> contextLabel + " could not be parsed, missing format version information");
         }
      }

      private DataResult<InclusiveRange<PackFormat>> validateNewFormat(
         int versionThreshold, boolean requireOldFormat, boolean requireNewFormat, String contextLabel, String oldFormatsKey
      ) {
         int i = this.min.get().major();
         int j = this.max.get().major();
         if (this.min.get().compareTo(this.max.get()) > 0) {
            return DataResult.error(() -> contextLabel + " min_format (" + this.min.get() + ") is greater than max_format (" + this.max.get() + ")");
         } else {
            if (i > versionThreshold && !requireNewFormat) {
               if (this.supported.isPresent()) {
                  return DataResult.error(
                     () -> contextLabel
                        + " key "
                        + oldFormatsKey
                        + " is deprecated starting from pack format "
                        + (versionThreshold + 1)
                        + ". Remove "
                        + oldFormatsKey
                        + " from your pack.mcmeta."
                  );
               }

               if (requireOldFormat && this.format.isPresent()) {
                  String string = this.validatePackFormatForRange(i, j);
                  if (string != null) {
                     return DataResult.error(() -> string);
                  }
               }
            } else {
               if (!this.supported.isPresent()) {
                  return DataResult.error(
                     () -> contextLabel
                        + " declares support for format "
                        + i
                        + ", but game versions supporting formats 17 to "
                        + versionThreshold
                        + " require a "
                        + oldFormatsKey
                        + " field. Add \""
                        + oldFormatsKey
                        + "\": ["
                        + i
                        + ", "
                        + versionThreshold
                        + "] or require a version greater or equal to "
                        + (versionThreshold + 1)
                        + ".0."
                  );
               }

               InclusiveRange<Integer> inclusiveRange = this.supported.get();
               if (inclusiveRange.minInclusive() != i) {
                  return DataResult.error(
                     () -> contextLabel
                        + " version declaration mismatch between "
                        + oldFormatsKey
                        + " (from "
                        + inclusiveRange.minInclusive()
                        + ") and min_format ("
                        + this.min.get()
                        + ")"
                  );
               }

               if (inclusiveRange.maxInclusive() != j && inclusiveRange.maxInclusive() != versionThreshold) {
                  return DataResult.error(
                     () -> contextLabel
                        + " version declaration mismatch between "
                        + oldFormatsKey
                        + " (up to "
                        + inclusiveRange.maxInclusive()
                        + ") and max_format ("
                        + this.max.get()
                        + ")"
                  );
               }

               if (requireOldFormat) {
                  if (!this.format.isPresent()) {
                     return DataResult.error(
                        () -> contextLabel
                           + " declares support for formats up to "
                           + versionThreshold
                           + ", but game versions supporting formats 17 to "
                           + versionThreshold
                           + " require a pack_format field. Add \"pack_format\": "
                           + i
                           + " or require a version greater or equal to "
                           + (versionThreshold + 1)
                           + ".0."
                     );
                  }

                  String string = this.validatePackFormatForRange(i, j);
                  if (string != null) {
                     return DataResult.error(() -> string);
                  }
               }
            }

            return DataResult.success(new InclusiveRange<>(this.min.get(), this.max.get()));
         }
      }

      private DataResult<InclusiveRange<PackFormat>> validateOldFormat(
         int versionThreshold, boolean requireOldFormat, String contextLabel, String oldFormatsKey
      ) {
         InclusiveRange<Integer> inclusiveRange = this.supported.get();
         int i = inclusiveRange.minInclusive();
         int j = inclusiveRange.maxInclusive();
         if (j > versionThreshold) {
            return DataResult.error(
               () -> contextLabel
                  + " declares support for version newer than "
                  + versionThreshold
                  + ", but is missing mandatory fields min_format and max_format"
            );
         } else {
            if (requireOldFormat) {
               if (!this.format.isPresent()) {
                  return DataResult.error(
                     () -> contextLabel
                        + " declares support for formats up to "
                        + versionThreshold
                        + ", but game versions supporting formats 17 to "
                        + versionThreshold
                        + " require a pack_format field. Add \"pack_format\": "
                        + i
                        + " or require a version greater or equal to "
                        + (versionThreshold + 1)
                        + ".0."
                  );
               }

               String string = this.validatePackFormatForRange(i, j);
               if (string != null) {
                  return DataResult.error(() -> string);
               }
            }

            return DataResult.success(new InclusiveRange<>(i, j).map(PackFormat::of));
         }
      }

      @Nullable
      private String validatePackFormatForRange(int min, int max) {
         int i = this.format.get();
         if (i < min || i > max) {
            return "Pack declared support for versions " + min + " to " + max + " but declared main format is " + i;
         } else {
            return i < 15
               ? "Multi-version packs cannot support minimum version of less than 15, since this will leave versions in range unable to load pack."
               : null;
         }
      }
   }

   public interface IntermediaryFormatHolder {
      PackFormat.IntermediaryFormat format();
   }
}
