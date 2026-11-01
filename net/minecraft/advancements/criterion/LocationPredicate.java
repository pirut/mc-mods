package net.minecraft.advancements.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.levelgen.structure.Structure;

public record LocationPredicate(
   Optional<LocationPredicate.PositionPredicate> position,
   Optional<HolderSet<Biome>> biomes,
   Optional<HolderSet<Structure>> structures,
   Optional<ResourceKey<Level>> dimension,
   Optional<Boolean> smokey,
   Optional<LightPredicate> light,
   Optional<BlockPredicate> block,
   Optional<FluidPredicate> fluid,
   Optional<Boolean> canSeeSky
) {
   public static final Codec<LocationPredicate> CODEC = RecordCodecBuilder.create(
      p_467669_ -> p_467669_.group(
            LocationPredicate.PositionPredicate.CODEC.optionalFieldOf("position").forGetter(LocationPredicate::position),
            RegistryCodecs.homogeneousList(Registries.BIOME).optionalFieldOf("biomes").forGetter(LocationPredicate::biomes),
            RegistryCodecs.homogeneousList(Registries.STRUCTURE).optionalFieldOf("structures").forGetter(LocationPredicate::structures),
            ResourceKey.codec(Registries.DIMENSION).optionalFieldOf("dimension").forGetter(LocationPredicate::dimension),
            Codec.BOOL.optionalFieldOf("smokey").forGetter(LocationPredicate::smokey),
            LightPredicate.CODEC.optionalFieldOf("light").forGetter(LocationPredicate::light),
            BlockPredicate.CODEC.optionalFieldOf("block").forGetter(LocationPredicate::block),
            FluidPredicate.CODEC.optionalFieldOf("fluid").forGetter(LocationPredicate::fluid),
            Codec.BOOL.optionalFieldOf("can_see_sky").forGetter(LocationPredicate::canSeeSky)
         )
         .apply(p_467669_, LocationPredicate::new)
   );

   public boolean matches(ServerLevel p_469105_, double p_468476_, double p_467441_, double p_468998_) {
      if (this.position.isPresent() && !this.position.get().matches(p_468476_, p_467441_, p_468998_)) {
         return false;
      } else if (this.dimension.isPresent() && this.dimension.get() != p_469105_.dimension()) {
         return false;
      } else {
         BlockPos blockpos = BlockPos.containing(p_468476_, p_467441_, p_468998_);
         boolean flag = p_469105_.isLoaded(blockpos);
         if (!this.biomes.isPresent() || flag && this.biomes.get().contains(p_469105_.getBiome(blockpos))) {
            if (!this.structures.isPresent() || flag && p_469105_.structureManager().getStructureWithPieceAt(blockpos, this.structures.get()).isValid()) {
               if (!this.smokey.isPresent() || flag && this.smokey.get() == CampfireBlock.isSmokeyPos(p_469105_, blockpos)) {
                  if (this.light.isPresent() && !this.light.get().matches(p_469105_, blockpos)) {
                     return false;
                  } else if (this.block.isPresent() && !this.block.get().matches(p_469105_, blockpos)) {
                     return false;
                  } else {
                     return this.fluid.isPresent() && !this.fluid.get().matches(p_469105_, blockpos)
                        ? false
                        : !this.canSeeSky.isPresent() || this.canSeeSky.get() == p_469105_.canSeeSky(blockpos);
                  }
               } else {
                  return false;
               }
            } else {
               return false;
            }
         } else {
            return false;
         }
      }
   }

   public static class Builder {
      private MinMaxBounds.Doubles x = MinMaxBounds.Doubles.ANY;
      private MinMaxBounds.Doubles y = MinMaxBounds.Doubles.ANY;
      private MinMaxBounds.Doubles z = MinMaxBounds.Doubles.ANY;
      private Optional<HolderSet<Biome>> biomes = Optional.empty();
      private Optional<HolderSet<Structure>> structures = Optional.empty();
      private Optional<ResourceKey<Level>> dimension = Optional.empty();
      private Optional<Boolean> smokey = Optional.empty();
      private Optional<LightPredicate> light = Optional.empty();
      private Optional<BlockPredicate> block = Optional.empty();
      private Optional<FluidPredicate> fluid = Optional.empty();
      private Optional<Boolean> canSeeSky = Optional.empty();

      public static LocationPredicate.Builder location() {
         return new LocationPredicate.Builder();
      }

      public static LocationPredicate.Builder inBiome(Holder<Biome> p_467321_) {
         return location().setBiomes(HolderSet.direct(p_467321_));
      }

      public static LocationPredicate.Builder inDimension(ResourceKey<Level> p_469469_) {
         return location().setDimension(p_469469_);
      }

      public static LocationPredicate.Builder inStructure(Holder<Structure> p_468337_) {
         return location().setStructures(HolderSet.direct(p_468337_));
      }

      public static LocationPredicate.Builder atYLocation(MinMaxBounds.Doubles p_467067_) {
         return location().setY(p_467067_);
      }

      public LocationPredicate.Builder setX(MinMaxBounds.Doubles p_467729_) {
         this.x = p_467729_;
         return this;
      }

      public LocationPredicate.Builder setY(MinMaxBounds.Doubles p_467351_) {
         this.y = p_467351_;
         return this;
      }

      public LocationPredicate.Builder setZ(MinMaxBounds.Doubles p_467807_) {
         this.z = p_467807_;
         return this;
      }

      public LocationPredicate.Builder setBiomes(HolderSet<Biome> p_469186_) {
         this.biomes = Optional.of(p_469186_);
         return this;
      }

      public LocationPredicate.Builder setStructures(HolderSet<Structure> p_468759_) {
         this.structures = Optional.of(p_468759_);
         return this;
      }

      public LocationPredicate.Builder setDimension(ResourceKey<Level> p_467978_) {
         this.dimension = Optional.of(p_467978_);
         return this;
      }

      public LocationPredicate.Builder setLight(LightPredicate.Builder p_469571_) {
         this.light = Optional.of(p_469571_.build());
         return this;
      }

      public LocationPredicate.Builder setBlock(BlockPredicate.Builder p_468959_) {
         this.block = Optional.of(p_468959_.build());
         return this;
      }

      public LocationPredicate.Builder setFluid(FluidPredicate.Builder p_468298_) {
         this.fluid = Optional.of(p_468298_.build());
         return this;
      }

      public LocationPredicate.Builder setSmokey(boolean p_469879_) {
         this.smokey = Optional.of(p_469879_);
         return this;
      }

      public LocationPredicate.Builder setCanSeeSky(boolean p_469932_) {
         this.canSeeSky = Optional.of(p_469932_);
         return this;
      }

      public LocationPredicate build() {
         Optional<LocationPredicate.PositionPredicate> optional = LocationPredicate.PositionPredicate.of(this.x, this.y, this.z);
         return new LocationPredicate(optional, this.biomes, this.structures, this.dimension, this.smokey, this.light, this.block, this.fluid, this.canSeeSky);
      }
   }

   record PositionPredicate(MinMaxBounds.Doubles x, MinMaxBounds.Doubles y, MinMaxBounds.Doubles z) {
      public static final Codec<LocationPredicate.PositionPredicate> CODEC = RecordCodecBuilder.create(
         p_468084_ -> p_468084_.group(
               MinMaxBounds.Doubles.CODEC.optionalFieldOf("x", MinMaxBounds.Doubles.ANY).forGetter(LocationPredicate.PositionPredicate::x),
               MinMaxBounds.Doubles.CODEC.optionalFieldOf("y", MinMaxBounds.Doubles.ANY).forGetter(LocationPredicate.PositionPredicate::y),
               MinMaxBounds.Doubles.CODEC.optionalFieldOf("z", MinMaxBounds.Doubles.ANY).forGetter(LocationPredicate.PositionPredicate::z)
            )
            .apply(p_468084_, LocationPredicate.PositionPredicate::new)
      );

      static Optional<LocationPredicate.PositionPredicate> of(MinMaxBounds.Doubles p_467989_, MinMaxBounds.Doubles p_468138_, MinMaxBounds.Doubles p_467199_) {
         return p_467989_.isAny() && p_468138_.isAny() && p_467199_.isAny()
            ? Optional.empty()
            : Optional.of(new LocationPredicate.PositionPredicate(p_467989_, p_468138_, p_467199_));
      }

      public boolean matches(double p_467857_, double p_469580_, double p_469480_) {
         return this.x.matches(p_467857_) && this.y.matches(p_469580_) && this.z.matches(p_469480_);
      }
   }
}
