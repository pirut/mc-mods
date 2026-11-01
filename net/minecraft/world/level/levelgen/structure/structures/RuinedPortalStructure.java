package net.minecraft.world.level.levelgen.structure.structures;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

public class RuinedPortalStructure extends Structure {
   private static final String[] STRUCTURE_LOCATION_PORTALS = new String[]{
      "ruined_portal/portal_1",
      "ruined_portal/portal_2",
      "ruined_portal/portal_3",
      "ruined_portal/portal_4",
      "ruined_portal/portal_5",
      "ruined_portal/portal_6",
      "ruined_portal/portal_7",
      "ruined_portal/portal_8",
      "ruined_portal/portal_9",
      "ruined_portal/portal_10"
   };
   private static final String[] STRUCTURE_LOCATION_GIANT_PORTALS = new String[]{
      "ruined_portal/giant_portal_1", "ruined_portal/giant_portal_2", "ruined_portal/giant_portal_3"
   };
   private static final float PROBABILITY_OF_GIANT_PORTAL = 0.05F;
   private static final int MIN_Y_INDEX = 15;
   private final List<RuinedPortalStructure.Setup> setups;
   public static final MapCodec<RuinedPortalStructure> CODEC = RecordCodecBuilder.mapCodec(
      p_229304_ -> p_229304_.group(
            settingsCodec(p_229304_),
            ExtraCodecs.nonEmptyList(RuinedPortalStructure.Setup.CODEC.listOf()).fieldOf("setups").forGetter(p_229299_ -> p_229299_.setups)
         )
         .apply(p_229304_, RuinedPortalStructure::new)
   );

   public RuinedPortalStructure(Structure.StructureSettings p_229260_, List<RuinedPortalStructure.Setup> p_229261_) {
      super(p_229260_);
      this.setups = p_229261_;
   }

   public RuinedPortalStructure(Structure.StructureSettings p_229257_, RuinedPortalStructure.Setup p_229258_) {
      this(p_229257_, List.of(p_229258_));
   }

   @Override
   public Optional<Structure.GenerationStub> findGenerationPoint(Structure.GenerationContext p_229285_) {
      RuinedPortalPiece.Properties $$1 = new RuinedPortalPiece.Properties();
      WorldgenRandom $$2 = p_229285_.random();
      RuinedPortalStructure.Setup $$3 = null;
      if (this.setups.size() > 1) {
         float $$4 = 0.0F;

         for (RuinedPortalStructure.Setup $$5 : this.setups) {
            $$4 += $$5.weight();
         }

         float $$6 = $$2.nextFloat();

         for (RuinedPortalStructure.Setup $$7 : this.setups) {
            $$6 -= $$7.weight() / $$4;
            if ($$6 < 0.0F) {
               $$3 = $$7;
               break;
            }
         }
      } else {
         $$3 = this.setups.get(0);
      }

      if ($$3 == null) {
         throw new IllegalStateException();
      } else {
         RuinedPortalStructure.Setup $$8 = $$3;
         $$1.airPocket = sample($$2, $$8.airPocketProbability());
         $$1.mossiness = $$8.mossiness();
         $$1.overgrown = $$8.overgrown();
         $$1.vines = $$8.vines();
         $$1.replaceWithBlackstone = $$8.replaceWithBlackstone();
         ResourceLocation $$9;
         if ($$2.nextFloat() < 0.05F) {
            $$9 = ResourceLocation.withDefaultNamespace(STRUCTURE_LOCATION_GIANT_PORTALS[$$2.nextInt(STRUCTURE_LOCATION_GIANT_PORTALS.length)]);
         } else {
            $$9 = ResourceLocation.withDefaultNamespace(STRUCTURE_LOCATION_PORTALS[$$2.nextInt(STRUCTURE_LOCATION_PORTALS.length)]);
         }

         StructureTemplate $$11 = p_229285_.structureTemplateManager().getOrCreate($$9);
         Rotation $$12 = Util.getRandom(Rotation.values(), $$2);
         Mirror $$13 = $$2.nextFloat() < 0.5F ? Mirror.NONE : Mirror.FRONT_BACK;
         BlockPos $$14 = new BlockPos($$11.getSize().getX() / 2, 0, $$11.getSize().getZ() / 2);
         ChunkGenerator $$15 = p_229285_.chunkGenerator();
         LevelHeightAccessor $$16 = p_229285_.heightAccessor();
         RandomState $$17 = p_229285_.randomState();
         BlockPos $$18 = p_229285_.chunkPos().getWorldPosition();
         BoundingBox $$19 = $$11.getBoundingBox($$18, $$12, $$14, $$13);
         BlockPos $$20 = $$19.getCenter();
         int $$21 = $$15.getBaseHeight($$20.getX(), $$20.getZ(), RuinedPortalPiece.getHeightMapType($$8.placement()), $$16, $$17) - 1;
         int $$22 = findSuitableY($$2, $$15, $$8.placement(), $$1.airPocket, $$21, $$19.getYSpan(), $$19, $$16, $$17);
         BlockPos $$23 = new BlockPos($$18.getX(), $$22, $$18.getZ());
         return Optional.of(
            new Structure.GenerationStub(
               $$23,
               (Consumer<StructurePiecesBuilder>)(p_229297_ -> {
                  if ($$8.canBeCold()) {
                     $$1.cold = isCold(
                        $$23,
                        p_229285_.chunkGenerator()
                           .getBiomeSource()
                           .getNoiseBiome(QuartPos.fromBlock($$23.getX()), QuartPos.fromBlock($$23.getY()), QuartPos.fromBlock($$23.getZ()), $$17.sampler())
                     );
                  }

                  p_229297_.addPiece(new RuinedPortalPiece(p_229285_.structureTemplateManager(), $$23, $$8.placement(), $$1, $$9, $$11, $$12, $$13, $$14));
               })
            )
         );
      }
   }

   private static boolean sample(WorldgenRandom p_229282_, float p_229283_) {
      if (p_229283_ == 0.0F) {
         return false;
      } else {
         return p_229283_ == 1.0F ? true : p_229282_.nextFloat() < p_229283_;
      }
   }

   private static boolean isCold(BlockPos p_229301_, Holder<Biome> p_229302_) {
      return p_229302_.value().coldEnoughToSnow(p_229301_);
   }

   private static int findSuitableY(
      RandomSource p_229267_,
      ChunkGenerator p_229268_,
      RuinedPortalPiece.VerticalPlacement p_229269_,
      boolean p_229270_,
      int p_229271_,
      int p_229272_,
      BoundingBox p_229273_,
      LevelHeightAccessor p_229274_,
      RandomState p_229275_
   ) {
      int $$9 = p_229274_.getMinBuildHeight() + 15;
      int $$10;
      if (p_229269_ == RuinedPortalPiece.VerticalPlacement.IN_NETHER) {
         if (p_229270_) {
            $$10 = Mth.randomBetweenInclusive(p_229267_, 32, 100);
         } else if (p_229267_.nextFloat() < 0.5F) {
            $$10 = Mth.randomBetweenInclusive(p_229267_, 27, 29);
         } else {
            $$10 = Mth.randomBetweenInclusive(p_229267_, 29, 100);
         }
      } else if (p_229269_ == RuinedPortalPiece.VerticalPlacement.IN_MOUNTAIN) {
         int $$13 = p_229271_ - p_229272_;
         $$10 = getRandomWithinInterval(p_229267_, 70, $$13);
      } else if (p_229269_ == RuinedPortalPiece.VerticalPlacement.UNDERGROUND) {
         int $$15 = p_229271_ - p_229272_;
         $$10 = getRandomWithinInterval(p_229267_, $$9, $$15);
      } else if (p_229269_ == RuinedPortalPiece.VerticalPlacement.PARTLY_BURIED) {
         $$10 = p_229271_ - p_229272_ + Mth.randomBetweenInclusive(p_229267_, 2, 8);
      } else {
         $$10 = p_229271_;
      }

      List<BlockPos> $$19 = ImmutableList.of(
         new BlockPos(p_229273_.minX(), 0, p_229273_.minZ()),
         new BlockPos(p_229273_.maxX(), 0, p_229273_.minZ()),
         new BlockPos(p_229273_.minX(), 0, p_229273_.maxZ()),
         new BlockPos(p_229273_.maxX(), 0, p_229273_.maxZ())
      );
      List<NoiseColumn> $$20 = $$19.stream()
         .map(p_229280_ -> p_229268_.getBaseColumn(p_229280_.getX(), p_229280_.getZ(), p_229274_, p_229275_))
         .collect(Collectors.toList());
      Heightmap.Types $$21 = p_229269_ == RuinedPortalPiece.VerticalPlacement.ON_OCEAN_FLOOR
         ? Heightmap.Types.OCEAN_FLOOR_WG
         : Heightmap.Types.WORLD_SURFACE_WG;

      int $$22;
      for ($$22 = $$10; $$22 > $$9; $$22--) {
         int $$23 = 0;

         for (NoiseColumn $$24 : $$20) {
            BlockState $$25 = $$24.getBlock($$22);
            if ($$21.isOpaque().test($$25)) {
               if (++$$23 == 3) {
                  return $$22;
               }
            }
         }
      }

      return $$22;
   }

   private static int getRandomWithinInterval(RandomSource p_229263_, int p_229264_, int p_229265_) {
      return p_229264_ < p_229265_ ? Mth.randomBetweenInclusive(p_229263_, p_229264_, p_229265_) : p_229265_;
   }

   @Override
   public StructureType<?> type() {
      return StructureType.RUINED_PORTAL;
   }

   public record Setup(
      RuinedPortalPiece.VerticalPlacement placement,
      float airPocketProbability,
      float mossiness,
      boolean overgrown,
      boolean vines,
      boolean canBeCold,
      boolean replaceWithBlackstone,
      float weight
   ) {
      public static final Codec<RuinedPortalStructure.Setup> CODEC = RecordCodecBuilder.create(
         p_229327_ -> p_229327_.group(
               RuinedPortalPiece.VerticalPlacement.CODEC.fieldOf("placement").forGetter(RuinedPortalStructure.Setup::placement),
               Codec.floatRange(0.0F, 1.0F).fieldOf("air_pocket_probability").forGetter(RuinedPortalStructure.Setup::airPocketProbability),
               Codec.floatRange(0.0F, 1.0F).fieldOf("mossiness").forGetter(RuinedPortalStructure.Setup::mossiness),
               Codec.BOOL.fieldOf("overgrown").forGetter(RuinedPortalStructure.Setup::overgrown),
               Codec.BOOL.fieldOf("vines").forGetter(RuinedPortalStructure.Setup::vines),
               Codec.BOOL.fieldOf("can_be_cold").forGetter(RuinedPortalStructure.Setup::canBeCold),
               Codec.BOOL.fieldOf("replace_with_blackstone").forGetter(RuinedPortalStructure.Setup::replaceWithBlackstone),
               ExtraCodecs.POSITIVE_FLOAT.fieldOf("weight").forGetter(RuinedPortalStructure.Setup::weight)
            )
            .apply(p_229327_, RuinedPortalStructure.Setup::new)
      );
   }
}
