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

   private static Codec<PackFormat> fullCodec(int p_433611_) {
      return ExtraCodecs.compactListCodec(ExtraCodecs.NON_NEGATIVE_INT, ExtraCodecs.NON_NEGATIVE_INT.listOf(1, 256))
         .xmap(
            p_435293_ -> p_435293_.size() > 1 ? of((Integer)p_435293_.getFirst(), (Integer)p_435293_.get(1)) : of((Integer)p_435293_.getFirst(), p_433611_),
            p_434947_ -> p_434947_.minor != p_433611_ ? List.of(p_434947_.major(), p_434947_.minor()) : List.of(p_434947_.major())
         );
   }

   public static <ResultType, HolderType extends PackFormat.IntermediaryFormatHolder> DataResult<List<ResultType>> validateHolderList(
      List<HolderType> p_435698_, int p_434854_, BiFunction<HolderType, InclusiveRange<PackFormat>, ResultType> p_433703_
   ) {
      int i = p_435698_.stream()
         .map(PackFormat.IntermediaryFormatHolder::format)
         .mapToInt(PackFormat.IntermediaryFormat::effectiveMinMajorVersion)
         .min()
         .orElse(Integer.MAX_VALUE);
      List<ResultType> list = new ArrayList<>(p_435698_.size());

      for (HolderType holdertype : p_435698_) {
         PackFormat.IntermediaryFormat packformat$intermediaryformat = holdertype.format();
         if (packformat$intermediaryformat.min().isEmpty()
            && packformat$intermediaryformat.max().isEmpty()
            && packformat$intermediaryformat.supported().isEmpty()) {
            LOGGER.warn("Unknown or broken overlay entry {}", holdertype);
         } else {
            DataResult<InclusiveRange<PackFormat>> dataresult = packformat$intermediaryformat.validate(
               p_434854_, false, i <= p_434854_, "Overlay \"" + holdertype + "\"", "formats"
            );
            if (!dataresult.isSuccess()) {
               return DataResult.error(((Error)dataresult.error().get())::message);
            }

            list.add(p_433703_.apply(holdertype, (InclusiveRange<PackFormat>)dataresult.getOrThrow()));
         }
      }

      return DataResult.success(List.copyOf(list));
   }

   @VisibleForTesting
   public static int lastPreMinorVersion(PackType p_435107_) {
      return switch (p_435107_) {
         case CLIENT_RESOURCES -> 64;
         case SERVER_DATA -> 81;
      };
   }

   public static MapCodec<InclusiveRange<PackFormat>> packCodec(PackType p_433328_) {
      int i = lastPreMinorVersion(p_433328_);
      return PackFormat.IntermediaryFormat.PACK_CODEC
         .flatXmap(
            p_435214_ -> p_435214_.validate(i, true, false, "Pack", "supported_formats"),
            p_434902_ -> DataResult.success(PackFormat.IntermediaryFormat.fromRange(p_434902_, i))
         );
   }

   public static PackFormat of(int p_434834_, int p_433219_) {
      return new PackFormat(p_434834_, p_433219_);
   }

   public static PackFormat of(int p_436044_) {
      return new PackFormat(p_436044_, 0);
   }

   public InclusiveRange<PackFormat> minorRange() {
      return new InclusiveRange<>(this, of(this.major, Integer.MAX_VALUE));
   }

   public int compareTo(PackFormat p_432991_) {
      int i = Integer.compare(this.major(), p_432991_.major());
      return i != 0 ? i : Integer.compare(this.minor(), p_432991_.minor());
   }

   @Override
   public String toString() {
      return this.minor == Integer.MAX_VALUE
         ? String.format(Locale.ROOT, "%d.*", this.major())
         : String.format(Locale.ROOT, "%d.%d", this.major(), this.minor());
   }

   public record IntermediaryFormat(Optional<PackFormat> min, Optional<PackFormat> max, Optional<Integer> format, Optional<InclusiveRange<Integer>> supported) {
      public static final MapCodec<PackFormat.IntermediaryFormat> PACK_CODEC = RecordCodecBuilder.mapCodec(
         p_436535_ -> p_436535_.group(
               PackFormat.BOTTOM_CODEC.optionalFieldOf("min_format").forGetter(PackFormat.IntermediaryFormat::min),
               PackFormat.TOP_CODEC.optionalFieldOf("max_format").forGetter(PackFormat.IntermediaryFormat::max),
               Codec.INT.optionalFieldOf("pack_format").forGetter(PackFormat.IntermediaryFormat::format),
               InclusiveRange.codec(Codec.INT).optionalFieldOf("supported_formats").forGetter(PackFormat.IntermediaryFormat::supported)
            )
            .apply(p_436535_, PackFormat.IntermediaryFormat::new)
      );
      public static final MapCodec<PackFormat.IntermediaryFormat> OVERLAY_CODEC = RecordCodecBuilder.mapCodec(
         p_436531_ -> p_436531_.group(
               PackFormat.BOTTOM_CODEC.optionalFieldOf("min_format").forGetter(PackFormat.IntermediaryFormat::min),
               PackFormat.TOP_CODEC.optionalFieldOf("max_format").forGetter(PackFormat.IntermediaryFormat::max),
               InclusiveRange.codec(Codec.INT).optionalFieldOf("formats").forGetter(PackFormat.IntermediaryFormat::supported)
            )
            .apply(
               p_436531_,
               (p_432882_, p_435411_, p_435168_) -> new PackFormat.IntermediaryFormat(p_432882_, p_435411_, p_432882_.map(PackFormat::major), p_435168_)
            )
      );

      public static PackFormat.IntermediaryFormat fromRange(InclusiveRange<PackFormat> p_436062_, int p_434772_) {
         InclusiveRange<Integer> inclusiverange = p_436062_.map(PackFormat::major);
         return new PackFormat.IntermediaryFormat(
            Optional.of(p_436062_.minInclusive()),
            Optional.of(p_436062_.maxInclusive()),
            inclusiverange.isValueInRange(p_434772_) ? Optional.of(inclusiverange.minInclusive()) : Optional.empty(),
            inclusiverange.isValueInRange(p_434772_)
               ? Optional.of(new InclusiveRange<>(inclusiverange.minInclusive(), inclusiverange.maxInclusive()))
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

      public DataResult<InclusiveRange<PackFormat>> validate(int p_433361_, boolean p_433851_, boolean p_436057_, String p_433029_, String p_435289_) {
         if (this.min.isPresent() != this.max.isPresent()) {
            return DataResult.error(() -> p_433029_ + " missing field, must declare both min_format and max_format");
         } else if (p_436057_ && this.supported.isEmpty()) {
            return DataResult.error(
               () -> p_433029_ + " missing required field " + p_435289_ + ", must be present in all overlays for any overlays to work across game versions"
            );
         } else if (this.min.isPresent()) {
            return this.validateNewFormat(p_433361_, p_433851_, p_436057_, p_433029_, p_435289_);
         } else if (this.supported.isPresent()) {
            return this.validateOldFormat(p_433361_, p_433851_, p_433029_, p_435289_);
         } else if (p_433851_ && this.format.isPresent()) {
            int i = this.format.get();
            return i > p_433361_
               ? DataResult.error(
                  () -> p_433029_ + " declares support for version newer than " + p_433361_ + ", but is missing mandatory fields min_format and max_format"
               )
               : DataResult.success(new InclusiveRange<>(PackFormat.of(i)));
         } else {
            return DataResult.error(() -> p_433029_ + " could not be parsed, missing format version information");
         }
      }

      private DataResult<InclusiveRange<PackFormat>> validateNewFormat(int p_432896_, boolean p_435209_, boolean p_434008_, String p_435685_, String p_435300_) {
         int i = this.min.get().major();
         int j = this.max.get().major();
         if (this.min.get().compareTo(this.max.get()) > 0) {
            return DataResult.error(() -> p_435685_ + " min_format (" + this.min.get() + ") is greater than max_format (" + this.max.get() + ")");
         } else {
            if (i > p_432896_ && !p_434008_) {
               if (this.supported.isPresent()) {
                  return DataResult.error(
                     () -> p_435685_
                        + " key "
                        + p_435300_
                        + " is deprecated starting from pack format "
                        + (p_432896_ + 1)
                        + ". Remove "
                        + p_435300_
                        + " from your pack.mcmeta."
                  );
               }

               if (p_435209_ && this.format.isPresent()) {
                  String s1 = this.validatePackFormatForRange(i, j);
                  if (s1 != null) {
                     return DataResult.error(() -> s1);
                  }
               }
            } else {
               if (!this.supported.isPresent()) {
                  return DataResult.error(
                     () -> p_435685_
                        + " declares support for format "
                        + i
                        + ", but game versions supporting formats 17 to "
                        + p_432896_
                        + " require a "
                        + p_435300_
                        + " field. Add \""
                        + p_435300_
                        + "\": ["
                        + i
                        + ", "
                        + p_432896_
                        + "] or require a version greater or equal to "
                        + (p_432896_ + 1)
                        + ".0."
                  );
               }

               InclusiveRange<Integer> inclusiverange = this.supported.get();
               if (inclusiverange.minInclusive() != i) {
                  return DataResult.error(
                     () -> p_435685_
                        + " version declaration mismatch between "
                        + p_435300_
                        + " (from "
                        + inclusiverange.minInclusive()
                        + ") and min_format ("
                        + this.min.get()
                        + ")"
                  );
               }

               if (inclusiverange.maxInclusive() != j && inclusiverange.maxInclusive() != p_432896_) {
                  return DataResult.error(
                     () -> p_435685_
                        + " version declaration mismatch between "
                        + p_435300_
                        + " (up to "
                        + inclusiverange.maxInclusive()
                        + ") and max_format ("
                        + this.max.get()
                        + ")"
                  );
               }

               if (p_435209_) {
                  if (!this.format.isPresent()) {
                     return DataResult.error(
                        () -> p_435685_
                           + " declares support for formats up to "
                           + p_432896_
                           + ", but game versions supporting formats 17 to "
                           + p_432896_
                           + " require a pack_format field. Add \"pack_format\": "
                           + i
                           + " or require a version greater or equal to "
                           + (p_432896_ + 1)
                           + ".0."
                     );
                  }

                  String s = this.validatePackFormatForRange(i, j);
                  if (s != null) {
                     return DataResult.error(() -> s);
                  }
               }
            }

            return DataResult.success(new InclusiveRange<>(this.min.get(), this.max.get()));
         }
      }

      private DataResult<InclusiveRange<PackFormat>> validateOldFormat(int p_434790_, boolean p_434529_, String p_434893_, String p_433339_) {
         InclusiveRange<Integer> inclusiverange = this.supported.get();
         int i = inclusiverange.minInclusive();
         int j = inclusiverange.maxInclusive();
         if (j > p_434790_) {
            return DataResult.error(
               () -> p_434893_ + " declares support for version newer than " + p_434790_ + ", but is missing mandatory fields min_format and max_format"
            );
         } else {
            if (p_434529_) {
               if (!this.format.isPresent()) {
                  return DataResult.error(
                     () -> p_434893_
                        + " declares support for formats up to "
                        + p_434790_
                        + ", but game versions supporting formats 17 to "
                        + p_434790_
                        + " require a pack_format field. Add \"pack_format\": "
                        + i
                        + " or require a version greater or equal to "
                        + (p_434790_ + 1)
                        + ".0."
                  );
               }

               String s = this.validatePackFormatForRange(i, j);
               if (s != null) {
                  return DataResult.error(() -> s);
               }
            }

            return DataResult.success(new InclusiveRange<>(i, j).map(PackFormat::of));
         }
      }

      @Nullable
      private String validatePackFormatForRange(int p_435052_, int p_433150_) {
         int i = this.format.get();
         if (i >= p_435052_ && i <= p_433150_) {
            return i < 15
               ? "Multi-version packs cannot support minimum version of less than 15, since this will leave versions in range unable to load pack."
               : null;
         } else {
            return "Pack declared support for versions " + p_435052_ + " to " + p_433150_ + " but declared main format is " + i;
         }
      }
   }

   public interface IntermediaryFormatHolder {
      PackFormat.IntermediaryFormat format();
   }
}
