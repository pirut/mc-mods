package net.minecraft.world.level.levelgen.structure.structures;

import com.google.common.collect.Lists;
import java.util.List;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Drowned;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePieceAccessor;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.levelgen.structure.templatesystem.AlwaysTrueTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockMatchTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockRotProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.CappedProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.PosAlwaysTrueTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.ProcessorRule;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.level.levelgen.structure.templatesystem.rule.blockentity.AppendLoot;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootTable;

public class OceanRuinPieces {
   static final StructureProcessor WARM_SUSPICIOUS_BLOCK_PROCESSOR = archyRuleProcessor(
      Blocks.SAND, Blocks.SUSPICIOUS_SAND, BuiltInLootTables.OCEAN_RUIN_WARM_ARCHAEOLOGY
   );
   static final StructureProcessor COLD_SUSPICIOUS_BLOCK_PROCESSOR = archyRuleProcessor(
      Blocks.GRAVEL, Blocks.SUSPICIOUS_GRAVEL, BuiltInLootTables.OCEAN_RUIN_COLD_ARCHAEOLOGY
   );
   private static final ResourceLocation[] WARM_RUINS = new ResourceLocation[]{
      ResourceLocation.withDefaultNamespace("underwater_ruin/warm_1"),
      ResourceLocation.withDefaultNamespace("underwater_ruin/warm_2"),
      ResourceLocation.withDefaultNamespace("underwater_ruin/warm_3"),
      ResourceLocation.withDefaultNamespace("underwater_ruin/warm_4"),
      ResourceLocation.withDefaultNamespace("underwater_ruin/warm_5"),
      ResourceLocation.withDefaultNamespace("underwater_ruin/warm_6"),
      ResourceLocation.withDefaultNamespace("underwater_ruin/warm_7"),
      ResourceLocation.withDefaultNamespace("underwater_ruin/warm_8")
   };
   private static final ResourceLocation[] RUINS_BRICK = new ResourceLocation[]{
      ResourceLocation.withDefaultNamespace("underwater_ruin/brick_1"),
      ResourceLocation.withDefaultNamespace("underwater_ruin/brick_2"),
      ResourceLocation.withDefaultNamespace("underwater_ruin/brick_3"),
      ResourceLocation.withDefaultNamespace("underwater_ruin/brick_4"),
      ResourceLocation.withDefaultNamespace("underwater_ruin/brick_5"),
      ResourceLocation.withDefaultNamespace("underwater_ruin/brick_6"),
      ResourceLocation.withDefaultNamespace("underwater_ruin/brick_7"),
      ResourceLocation.withDefaultNamespace("underwater_ruin/brick_8")
   };
   private static final ResourceLocation[] RUINS_CRACKED = new ResourceLocation[]{
      ResourceLocation.withDefaultNamespace("underwater_ruin/cracked_1"),
      ResourceLocation.withDefaultNamespace("underwater_ruin/cracked_2"),
      ResourceLocation.withDefaultNamespace("underwater_ruin/cracked_3"),
      ResourceLocation.withDefaultNamespace("underwater_ruin/cracked_4"),
      ResourceLocation.withDefaultNamespace("underwater_ruin/cracked_5"),
      ResourceLocation.withDefaultNamespace("underwater_ruin/cracked_6"),
      ResourceLocation.withDefaultNamespace("underwater_ruin/cracked_7"),
      ResourceLocation.withDefaultNamespace("underwater_ruin/cracked_8")
   };
   private static final ResourceLocation[] RUINS_MOSSY = new ResourceLocation[]{
      ResourceLocation.withDefaultNamespace("underwater_ruin/mossy_1"),
      ResourceLocation.withDefaultNamespace("underwater_ruin/mossy_2"),
      ResourceLocation.withDefaultNamespace("underwater_ruin/mossy_3"),
      ResourceLocation.withDefaultNamespace("underwater_ruin/mossy_4"),
      ResourceLocation.withDefaultNamespace("underwater_ruin/mossy_5"),
      ResourceLocation.withDefaultNamespace("underwater_ruin/mossy_6"),
      ResourceLocation.withDefaultNamespace("underwater_ruin/mossy_7"),
      ResourceLocation.withDefaultNamespace("underwater_ruin/mossy_8")
   };
   private static final ResourceLocation[] BIG_RUINS_BRICK = new ResourceLocation[]{
      ResourceLocation.withDefaultNamespace("underwater_ruin/big_brick_1"),
      ResourceLocation.withDefaultNamespace("underwater_ruin/big_brick_2"),
      ResourceLocation.withDefaultNamespace("underwater_ruin/big_brick_3"),
      ResourceLocation.withDefaultNamespace("underwater_ruin/big_brick_8")
   };
   private static final ResourceLocation[] BIG_RUINS_MOSSY = new ResourceLocation[]{
      ResourceLocation.withDefaultNamespace("underwater_ruin/big_mossy_1"),
      ResourceLocation.withDefaultNamespace("underwater_ruin/big_mossy_2"),
      ResourceLocation.withDefaultNamespace("underwater_ruin/big_mossy_3"),
      ResourceLocation.withDefaultNamespace("underwater_ruin/big_mossy_8")
   };
   private static final ResourceLocation[] BIG_RUINS_CRACKED = new ResourceLocation[]{
      ResourceLocation.withDefaultNamespace("underwater_ruin/big_cracked_1"),
      ResourceLocation.withDefaultNamespace("underwater_ruin/big_cracked_2"),
      ResourceLocation.withDefaultNamespace("underwater_ruin/big_cracked_3"),
      ResourceLocation.withDefaultNamespace("underwater_ruin/big_cracked_8")
   };
   private static final ResourceLocation[] BIG_WARM_RUINS = new ResourceLocation[]{
      ResourceLocation.withDefaultNamespace("underwater_ruin/big_warm_4"),
      ResourceLocation.withDefaultNamespace("underwater_ruin/big_warm_5"),
      ResourceLocation.withDefaultNamespace("underwater_ruin/big_warm_6"),
      ResourceLocation.withDefaultNamespace("underwater_ruin/big_warm_7")
   };

   private static StructureProcessor archyRuleProcessor(Block p_277376_, Block p_277934_, ResourceKey<LootTable> p_335398_) {
      return new CappedProcessor(
         new RuleProcessor(
            List.of(
               new ProcessorRule(
                  new BlockMatchTest(p_277376_), AlwaysTrueTest.INSTANCE, PosAlwaysTrueTest.INSTANCE, p_277934_.defaultBlockState(), new AppendLoot(p_335398_)
               )
            )
         ),
         ConstantInt.of(5)
      );
   }

   private static ResourceLocation getSmallWarmRuin(RandomSource p_228983_) {
      return Util.getRandom(WARM_RUINS, p_228983_);
   }

   private static ResourceLocation getBigWarmRuin(RandomSource p_229011_) {
      return Util.getRandom(BIG_WARM_RUINS, p_229011_);
   }

   public static void addPieces(
      StructureTemplateManager p_228995_,
      BlockPos p_228996_,
      Rotation p_228997_,
      StructurePieceAccessor p_228998_,
      RandomSource p_228999_,
      OceanRuinStructure p_229000_
   ) {
      boolean $$6 = p_228999_.nextFloat() <= p_229000_.largeProbability;
      float $$7 = $$6 ? 0.9F : 0.8F;
      addPiece(p_228995_, p_228996_, p_228997_, p_228998_, p_228999_, p_229000_, $$6, $$7);
      if ($$6 && p_228999_.nextFloat() <= p_229000_.clusterProbability) {
         addClusterRuins(p_228995_, p_228999_, p_228997_, p_228996_, p_229000_, p_228998_);
      }
   }

   private static void addClusterRuins(
      StructureTemplateManager p_228988_,
      RandomSource p_228989_,
      Rotation p_228990_,
      BlockPos p_228991_,
      OceanRuinStructure p_228992_,
      StructurePieceAccessor p_228993_
   ) {
      BlockPos $$6 = new BlockPos(p_228991_.getX(), 90, p_228991_.getZ());
      BlockPos $$7 = StructureTemplate.transform(new BlockPos(15, 0, 15), Mirror.NONE, p_228990_, BlockPos.ZERO).offset($$6);
      BoundingBox $$8 = BoundingBox.fromCorners($$6, $$7);
      BlockPos $$9 = new BlockPos(Math.min($$6.getX(), $$7.getX()), $$6.getY(), Math.min($$6.getZ(), $$7.getZ()));
      List<BlockPos> $$10 = allPositions(p_228989_, $$9);
      int $$11 = Mth.nextInt(p_228989_, 4, 8);

      for (int $$12 = 0; $$12 < $$11; $$12++) {
         if (!$$10.isEmpty()) {
            int $$13 = p_228989_.nextInt($$10.size());
            BlockPos $$14 = $$10.remove($$13);
            Rotation $$15 = Rotation.getRandom(p_228989_);
            BlockPos $$16 = StructureTemplate.transform(new BlockPos(5, 0, 6), Mirror.NONE, $$15, BlockPos.ZERO).offset($$14);
            BoundingBox $$17 = BoundingBox.fromCorners($$14, $$16);
            if (!$$17.intersects($$8)) {
               addPiece(p_228988_, $$14, $$15, p_228993_, p_228989_, p_228992_, false, 0.8F);
            }
         }
      }
   }

   private static List<BlockPos> allPositions(RandomSource p_228985_, BlockPos p_228986_) {
      List<BlockPos> $$2 = Lists.newArrayList();
      $$2.add(p_228986_.offset(-16 + Mth.nextInt(p_228985_, 1, 8), 0, 16 + Mth.nextInt(p_228985_, 1, 7)));
      $$2.add(p_228986_.offset(-16 + Mth.nextInt(p_228985_, 1, 8), 0, Mth.nextInt(p_228985_, 1, 7)));
      $$2.add(p_228986_.offset(-16 + Mth.nextInt(p_228985_, 1, 8), 0, -16 + Mth.nextInt(p_228985_, 4, 8)));
      $$2.add(p_228986_.offset(Mth.nextInt(p_228985_, 1, 7), 0, 16 + Mth.nextInt(p_228985_, 1, 7)));
      $$2.add(p_228986_.offset(Mth.nextInt(p_228985_, 1, 7), 0, -16 + Mth.nextInt(p_228985_, 4, 6)));
      $$2.add(p_228986_.offset(16 + Mth.nextInt(p_228985_, 1, 7), 0, 16 + Mth.nextInt(p_228985_, 3, 8)));
      $$2.add(p_228986_.offset(16 + Mth.nextInt(p_228985_, 1, 7), 0, Mth.nextInt(p_228985_, 1, 7)));
      $$2.add(p_228986_.offset(16 + Mth.nextInt(p_228985_, 1, 7), 0, -16 + Mth.nextInt(p_228985_, 4, 8)));
      return $$2;
   }

   private static void addPiece(
      StructureTemplateManager p_229002_,
      BlockPos p_229003_,
      Rotation p_229004_,
      StructurePieceAccessor p_229005_,
      RandomSource p_229006_,
      OceanRuinStructure p_229007_,
      boolean p_229008_,
      float p_229009_
   ) {
      switch (p_229007_.biomeTemp) {
         case WARM:
         default:
            ResourceLocation $$8 = p_229008_ ? getBigWarmRuin(p_229006_) : getSmallWarmRuin(p_229006_);
            p_229005_.addPiece(new OceanRuinPieces.OceanRuinPiece(p_229002_, $$8, p_229003_, p_229004_, p_229009_, p_229007_.biomeTemp, p_229008_));
            break;
         case COLD:
            ResourceLocation[] $$9 = p_229008_ ? BIG_RUINS_BRICK : RUINS_BRICK;
            ResourceLocation[] $$10 = p_229008_ ? BIG_RUINS_CRACKED : RUINS_CRACKED;
            ResourceLocation[] $$11 = p_229008_ ? BIG_RUINS_MOSSY : RUINS_MOSSY;
            int $$12 = p_229006_.nextInt($$9.length);
            p_229005_.addPiece(new OceanRuinPieces.OceanRuinPiece(p_229002_, $$9[$$12], p_229003_, p_229004_, p_229009_, p_229007_.biomeTemp, p_229008_));
            p_229005_.addPiece(new OceanRuinPieces.OceanRuinPiece(p_229002_, $$10[$$12], p_229003_, p_229004_, 0.7F, p_229007_.biomeTemp, p_229008_));
            p_229005_.addPiece(new OceanRuinPieces.OceanRuinPiece(p_229002_, $$11[$$12], p_229003_, p_229004_, 0.5F, p_229007_.biomeTemp, p_229008_));
      }
   }

   public static class OceanRuinPiece extends TemplateStructurePiece {
      private final OceanRuinStructure.Type biomeType;
      private final float integrity;
      private final boolean isLarge;

      public OceanRuinPiece(
         StructureTemplateManager p_229018_,
         ResourceLocation p_229019_,
         BlockPos p_229020_,
         Rotation p_229021_,
         float p_229022_,
         OceanRuinStructure.Type p_229023_,
         boolean p_229024_
      ) {
         super(StructurePieceType.OCEAN_RUIN, 0, p_229018_, p_229019_, p_229019_.toString(), makeSettings(p_229021_, p_229022_, p_229023_), p_229020_);
         this.integrity = p_229022_;
         this.biomeType = p_229023_;
         this.isLarge = p_229024_;
      }

      private OceanRuinPiece(
         StructureTemplateManager p_277563_, CompoundTag p_277610_, Rotation p_277637_, float p_277437_, OceanRuinStructure.Type p_277873_, boolean p_277924_
      ) {
         super(StructurePieceType.OCEAN_RUIN, p_277610_, p_277563_, p_277332_ -> makeSettings(p_277637_, p_277437_, p_277873_));
         this.integrity = p_277437_;
         this.biomeType = p_277873_;
         this.isLarge = p_277924_;
      }

      private static StructurePlaceSettings makeSettings(Rotation p_277572_, float p_277489_, OceanRuinStructure.Type p_277631_) {
         StructureProcessor $$3 = p_277631_ == OceanRuinStructure.Type.COLD
            ? OceanRuinPieces.COLD_SUSPICIOUS_BLOCK_PROCESSOR
            : OceanRuinPieces.WARM_SUSPICIOUS_BLOCK_PROCESSOR;
         return new StructurePlaceSettings()
            .setRotation(p_277572_)
            .setMirror(Mirror.NONE)
            .addProcessor(new BlockRotProcessor(p_277489_))
            .addProcessor(BlockIgnoreProcessor.STRUCTURE_AND_AIR)
            .addProcessor($$3);
      }

      public static OceanRuinPieces.OceanRuinPiece create(StructureTemplateManager p_277874_, CompoundTag p_277773_) {
         Rotation $$2 = Rotation.valueOf(p_277773_.getString("Rot"));
         float $$3 = p_277773_.getFloat("Integrity");
         OceanRuinStructure.Type $$4 = OceanRuinStructure.Type.valueOf(p_277773_.getString("BiomeType"));
         boolean $$5 = p_277773_.getBoolean("IsLarge");
         return new OceanRuinPieces.OceanRuinPiece(p_277874_, p_277773_, $$2, $$3, $$4, $$5);
      }

      @Override
      protected void addAdditionalSaveData(StructurePieceSerializationContext p_229039_, CompoundTag p_229040_) {
         super.addAdditionalSaveData(p_229039_, p_229040_);
         p_229040_.putString("Rot", this.placeSettings.getRotation().name());
         p_229040_.putFloat("Integrity", this.integrity);
         p_229040_.putString("BiomeType", this.biomeType.toString());
         p_229040_.putBoolean("IsLarge", this.isLarge);
      }

      @Override
      protected void handleDataMarker(String p_229046_, BlockPos p_229047_, ServerLevelAccessor p_229048_, RandomSource p_229049_, BoundingBox p_229050_) {
         if ("chest".equals(p_229046_)) {
            p_229048_.setBlock(
               p_229047_, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.WATERLOGGED, p_229048_.getFluidState(p_229047_).is(FluidTags.WATER)), 2
            );
            BlockEntity $$5 = p_229048_.getBlockEntity(p_229047_);
            if ($$5 instanceof ChestBlockEntity) {
               ((ChestBlockEntity)$$5)
                  .setLootTable(this.isLarge ? BuiltInLootTables.UNDERWATER_RUIN_BIG : BuiltInLootTables.UNDERWATER_RUIN_SMALL, p_229049_.nextLong());
            }
         } else if ("drowned".equals(p_229046_)) {
            Drowned $$6 = EntityType.DROWNED.create(p_229048_.getLevel());
            if ($$6 != null) {
               $$6.setPersistenceRequired();
               $$6.moveTo(p_229047_, 0.0F, 0.0F);
               $$6.finalizeSpawn(p_229048_, p_229048_.getCurrentDifficultyAt(p_229047_), MobSpawnType.STRUCTURE, null);
               p_229048_.addFreshEntityWithPassengers($$6);
               if (p_229047_.getY() > p_229048_.getSeaLevel()) {
                  p_229048_.setBlock(p_229047_, Blocks.AIR.defaultBlockState(), 2);
               } else {
                  p_229048_.setBlock(p_229047_, Blocks.WATER.defaultBlockState(), 2);
               }
            }
         }
      }

      @Override
      public void postProcess(
         WorldGenLevel p_229029_,
         StructureManager p_229030_,
         ChunkGenerator p_229031_,
         RandomSource p_229032_,
         BoundingBox p_229033_,
         ChunkPos p_229034_,
         BlockPos p_229035_
      ) {
         int $$7 = p_229029_.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, this.templatePosition.getX(), this.templatePosition.getZ());
         this.templatePosition = new BlockPos(this.templatePosition.getX(), $$7, this.templatePosition.getZ());
         BlockPos $$8 = StructureTemplate.transform(
               new BlockPos(this.template.getSize().getX() - 1, 0, this.template.getSize().getZ() - 1),
               Mirror.NONE,
               this.placeSettings.getRotation(),
               BlockPos.ZERO
            )
            .offset(this.templatePosition);
         this.templatePosition = new BlockPos(this.templatePosition.getX(), this.getHeight(this.templatePosition, p_229029_, $$8), this.templatePosition.getZ());
         super.postProcess(p_229029_, p_229030_, p_229031_, p_229032_, p_229033_, p_229034_, p_229035_);
      }

      private int getHeight(BlockPos p_229042_, BlockGetter p_229043_, BlockPos p_229044_) {
         int $$3 = p_229042_.getY();
         int $$4 = 512;
         int $$5 = $$3 - 1;
         int $$6 = 0;

         for (BlockPos $$7 : BlockPos.betweenClosed(p_229042_, p_229044_)) {
            int $$8 = $$7.getX();
            int $$9 = $$7.getZ();
            int $$10 = p_229042_.getY() - 1;
            BlockPos.MutableBlockPos $$11 = new BlockPos.MutableBlockPos($$8, $$10, $$9);
            BlockState $$12 = p_229043_.getBlockState($$11);

            for (FluidState $$13 = p_229043_.getFluidState($$11);
               ($$12.isAir() || $$13.is(FluidTags.WATER) || $$12.is(BlockTags.ICE)) && $$10 > p_229043_.getMinBuildHeight() + 1;
               $$13 = p_229043_.getFluidState($$11)
            ) {
               $$11.set($$8, --$$10, $$9);
               $$12 = p_229043_.getBlockState($$11);
            }

            $$4 = Math.min($$4, $$10);
            if ($$10 < $$5 - 2) {
               $$6++;
            }
         }

         int $$14 = Math.abs(p_229042_.getX() - p_229044_.getX());
         if ($$5 - $$4 > 2 && $$6 > $$14 - 2) {
            $$3 = $$4 + 1;
         }

         return $$3;
      }
   }
}
