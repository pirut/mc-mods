package net.minecraft.world.level.levelgen.structure.structures;

import java.util.Map;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePieceAccessor;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootTable;

public class ShipwreckPieces {
   private static final int NUMBER_OF_BLOCKS_ALLOWED_IN_WORLD_GEN_REGION = 32;
   static final BlockPos PIVOT = new BlockPos(4, 0, 15);
   private static final ResourceLocation[] STRUCTURE_LOCATION_BEACHED = new ResourceLocation[]{
      ResourceLocation.withDefaultNamespace("shipwreck/with_mast"),
      ResourceLocation.withDefaultNamespace("shipwreck/sideways_full"),
      ResourceLocation.withDefaultNamespace("shipwreck/sideways_fronthalf"),
      ResourceLocation.withDefaultNamespace("shipwreck/sideways_backhalf"),
      ResourceLocation.withDefaultNamespace("shipwreck/rightsideup_full"),
      ResourceLocation.withDefaultNamespace("shipwreck/rightsideup_fronthalf"),
      ResourceLocation.withDefaultNamespace("shipwreck/rightsideup_backhalf"),
      ResourceLocation.withDefaultNamespace("shipwreck/with_mast_degraded"),
      ResourceLocation.withDefaultNamespace("shipwreck/rightsideup_full_degraded"),
      ResourceLocation.withDefaultNamespace("shipwreck/rightsideup_fronthalf_degraded"),
      ResourceLocation.withDefaultNamespace("shipwreck/rightsideup_backhalf_degraded")
   };
   private static final ResourceLocation[] STRUCTURE_LOCATION_OCEAN = new ResourceLocation[]{
      ResourceLocation.withDefaultNamespace("shipwreck/with_mast"),
      ResourceLocation.withDefaultNamespace("shipwreck/upsidedown_full"),
      ResourceLocation.withDefaultNamespace("shipwreck/upsidedown_fronthalf"),
      ResourceLocation.withDefaultNamespace("shipwreck/upsidedown_backhalf"),
      ResourceLocation.withDefaultNamespace("shipwreck/sideways_full"),
      ResourceLocation.withDefaultNamespace("shipwreck/sideways_fronthalf"),
      ResourceLocation.withDefaultNamespace("shipwreck/sideways_backhalf"),
      ResourceLocation.withDefaultNamespace("shipwreck/rightsideup_full"),
      ResourceLocation.withDefaultNamespace("shipwreck/rightsideup_fronthalf"),
      ResourceLocation.withDefaultNamespace("shipwreck/rightsideup_backhalf"),
      ResourceLocation.withDefaultNamespace("shipwreck/with_mast_degraded"),
      ResourceLocation.withDefaultNamespace("shipwreck/upsidedown_full_degraded"),
      ResourceLocation.withDefaultNamespace("shipwreck/upsidedown_fronthalf_degraded"),
      ResourceLocation.withDefaultNamespace("shipwreck/upsidedown_backhalf_degraded"),
      ResourceLocation.withDefaultNamespace("shipwreck/sideways_full_degraded"),
      ResourceLocation.withDefaultNamespace("shipwreck/sideways_fronthalf_degraded"),
      ResourceLocation.withDefaultNamespace("shipwreck/sideways_backhalf_degraded"),
      ResourceLocation.withDefaultNamespace("shipwreck/rightsideup_full_degraded"),
      ResourceLocation.withDefaultNamespace("shipwreck/rightsideup_fronthalf_degraded"),
      ResourceLocation.withDefaultNamespace("shipwreck/rightsideup_backhalf_degraded")
   };
   static final Map<String, ResourceKey<LootTable>> MARKERS_TO_LOOT = Map.of(
      "map_chest", BuiltInLootTables.SHIPWRECK_MAP, "treasure_chest", BuiltInLootTables.SHIPWRECK_TREASURE, "supply_chest", BuiltInLootTables.SHIPWRECK_SUPPLY
   );

   public static ShipwreckPieces.ShipwreckPiece addRandomPiece(
      StructureTemplateManager p_341897_, BlockPos p_341911_, Rotation p_341901_, StructurePieceAccessor p_341889_, RandomSource p_341928_, boolean p_341941_
   ) {
      ResourceLocation $$6 = Util.getRandom(p_341941_ ? STRUCTURE_LOCATION_BEACHED : STRUCTURE_LOCATION_OCEAN, p_341928_);
      ShipwreckPieces.ShipwreckPiece $$7 = new ShipwreckPieces.ShipwreckPiece(p_341897_, $$6, p_341911_, p_341901_, p_341941_);
      p_341889_.addPiece($$7);
      return $$7;
   }

   public static class ShipwreckPiece extends TemplateStructurePiece {
      private final boolean isBeached;

      public ShipwreckPiece(StructureTemplateManager p_229354_, ResourceLocation p_229355_, BlockPos p_229356_, Rotation p_229357_, boolean p_229358_) {
         super(StructurePieceType.SHIPWRECK_PIECE, 0, p_229354_, p_229355_, p_229355_.toString(), makeSettings(p_229357_), p_229356_);
         this.isBeached = p_229358_;
      }

      public ShipwreckPiece(StructureTemplateManager p_229360_, CompoundTag p_229361_) {
         super(StructurePieceType.SHIPWRECK_PIECE, p_229361_, p_229360_, p_229383_ -> makeSettings(Rotation.valueOf(p_229361_.getString("Rot"))));
         this.isBeached = p_229361_.getBoolean("isBeached");
      }

      @Override
      protected void addAdditionalSaveData(StructurePieceSerializationContext p_229373_, CompoundTag p_229374_) {
         super.addAdditionalSaveData(p_229373_, p_229374_);
         p_229374_.putBoolean("isBeached", this.isBeached);
         p_229374_.putString("Rot", this.placeSettings.getRotation().name());
      }

      private static StructurePlaceSettings makeSettings(Rotation p_229371_) {
         return new StructurePlaceSettings()
            .setRotation(p_229371_)
            .setMirror(Mirror.NONE)
            .setRotationPivot(ShipwreckPieces.PIVOT)
            .addProcessor(BlockIgnoreProcessor.STRUCTURE_AND_AIR);
      }

      @Override
      protected void handleDataMarker(String p_229376_, BlockPos p_229377_, ServerLevelAccessor p_229378_, RandomSource p_229379_, BoundingBox p_229380_) {
         ResourceKey<LootTable> $$5 = ShipwreckPieces.MARKERS_TO_LOOT.get(p_229376_);
         if ($$5 != null) {
            RandomizableContainer.setBlockEntityLootTable(p_229378_, p_229379_, p_229377_.below(), $$5);
         }
      }

      @Override
      public void postProcess(
         WorldGenLevel p_229363_,
         StructureManager p_229364_,
         ChunkGenerator p_229365_,
         RandomSource p_229366_,
         BoundingBox p_229367_,
         ChunkPos p_229368_,
         BlockPos p_229369_
      ) {
         if (this.isTooBigToFitInWorldGenRegion()) {
            super.postProcess(p_229363_, p_229364_, p_229365_, p_229366_, p_229367_, p_229368_, p_229369_);
         } else {
            int $$7 = p_229363_.getMaxBuildHeight();
            int $$8 = 0;
            Vec3i $$9 = this.template.getSize();
            Heightmap.Types $$10 = this.isBeached ? Heightmap.Types.WORLD_SURFACE_WG : Heightmap.Types.OCEAN_FLOOR_WG;
            int $$11 = $$9.getX() * $$9.getZ();
            if ($$11 == 0) {
               $$8 = p_229363_.getHeight($$10, this.templatePosition.getX(), this.templatePosition.getZ());
            } else {
               BlockPos $$12 = this.templatePosition.offset($$9.getX() - 1, 0, $$9.getZ() - 1);

               for (BlockPos $$13 : BlockPos.betweenClosed(this.templatePosition, $$12)) {
                  int $$14 = p_229363_.getHeight($$10, $$13.getX(), $$13.getZ());
                  $$8 += $$14;
                  $$7 = Math.min($$7, $$14);
               }

               $$8 /= $$11;
            }

            this.adjustPositionHeight(this.isBeached ? this.calculateBeachedPosition($$7, p_229366_) : $$8);
            super.postProcess(p_229363_, p_229364_, p_229365_, p_229366_, p_229367_, p_229368_, p_229369_);
         }
      }

      public boolean isTooBigToFitInWorldGenRegion() {
         Vec3i $$0 = this.template.getSize();
         return $$0.getX() > 32 || $$0.getY() > 32;
      }

      public int calculateBeachedPosition(int p_341902_, RandomSource p_341891_) {
         return p_341902_ - this.template.getSize().getY() / 2 - p_341891_.nextInt(3);
      }

      public void adjustPositionHeight(int p_341929_) {
         this.templatePosition = new BlockPos(this.templatePosition.getX(), p_341929_, this.templatePosition.getZ());
      }
   }
}
