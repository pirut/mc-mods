package net.minecraft.world.level.levelgen.structure.structures;

import com.google.common.collect.Lists;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Tuple;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;

public class EndCityPieces {
   private static final int MAX_GEN_DEPTH = 8;
   static final EndCityPieces.SectionGenerator HOUSE_TOWER_GENERATOR = new EndCityPieces.SectionGenerator() {
      @Override
      public void init() {
      }

      @Override
      public boolean generate(
         StructureTemplateManager p_227456_,
         int p_227457_,
         EndCityPieces.EndCityPiece p_227458_,
         BlockPos p_227459_,
         List<StructurePiece> p_227460_,
         RandomSource p_227461_
      ) {
         if (p_227457_ > 8) {
            return false;
         } else {
            Rotation $$6 = p_227458_.placeSettings().getRotation();
            EndCityPieces.EndCityPiece $$7 = EndCityPieces.addHelper(
               p_227460_, EndCityPieces.addPiece(p_227456_, p_227458_, p_227459_, "base_floor", $$6, true)
            );
            int $$8 = p_227461_.nextInt(3);
            if ($$8 == 0) {
               $$7 = EndCityPieces.addHelper(p_227460_, EndCityPieces.addPiece(p_227456_, $$7, new BlockPos(-1, 4, -1), "base_roof", $$6, true));
            } else if ($$8 == 1) {
               $$7 = EndCityPieces.addHelper(p_227460_, EndCityPieces.addPiece(p_227456_, $$7, new BlockPos(-1, 0, -1), "second_floor_2", $$6, false));
               $$7 = EndCityPieces.addHelper(p_227460_, EndCityPieces.addPiece(p_227456_, $$7, new BlockPos(-1, 8, -1), "second_roof", $$6, false));
               EndCityPieces.recursiveChildren(p_227456_, EndCityPieces.TOWER_GENERATOR, p_227457_ + 1, $$7, null, p_227460_, p_227461_);
            } else if ($$8 == 2) {
               $$7 = EndCityPieces.addHelper(p_227460_, EndCityPieces.addPiece(p_227456_, $$7, new BlockPos(-1, 0, -1), "second_floor_2", $$6, false));
               $$7 = EndCityPieces.addHelper(p_227460_, EndCityPieces.addPiece(p_227456_, $$7, new BlockPos(-1, 4, -1), "third_floor_2", $$6, false));
               $$7 = EndCityPieces.addHelper(p_227460_, EndCityPieces.addPiece(p_227456_, $$7, new BlockPos(-1, 8, -1), "third_roof", $$6, true));
               EndCityPieces.recursiveChildren(p_227456_, EndCityPieces.TOWER_GENERATOR, p_227457_ + 1, $$7, null, p_227460_, p_227461_);
            }

            return true;
         }
      }
   };
   static final List<Tuple<Rotation, BlockPos>> TOWER_BRIDGES = Lists.newArrayList(
      new Tuple[]{
         new Tuple<>(Rotation.NONE, new BlockPos(1, -1, 0)),
         new Tuple<>(Rotation.CLOCKWISE_90, new BlockPos(6, -1, 1)),
         new Tuple<>(Rotation.COUNTERCLOCKWISE_90, new BlockPos(0, -1, 5)),
         new Tuple<>(Rotation.CLOCKWISE_180, new BlockPos(5, -1, 6))
      }
   );
   static final EndCityPieces.SectionGenerator TOWER_GENERATOR = new EndCityPieces.SectionGenerator() {
      @Override
      public void init() {
      }

      @Override
      public boolean generate(
         StructureTemplateManager p_227465_,
         int p_227466_,
         EndCityPieces.EndCityPiece p_227467_,
         BlockPos p_227468_,
         List<StructurePiece> p_227469_,
         RandomSource p_227470_
      ) {
         Rotation $$6 = p_227467_.placeSettings().getRotation();
         EndCityPieces.EndCityPiece $$7 = EndCityPieces.addHelper(
            p_227469_,
            EndCityPieces.addPiece(p_227465_, p_227467_, new BlockPos(3 + p_227470_.nextInt(2), -3, 3 + p_227470_.nextInt(2)), "tower_base", $$6, true)
         );
         $$7 = EndCityPieces.addHelper(p_227469_, EndCityPieces.addPiece(p_227465_, $$7, new BlockPos(0, 7, 0), "tower_piece", $$6, true));
         EndCityPieces.EndCityPiece $$8 = p_227470_.nextInt(3) == 0 ? $$7 : null;
         int $$9 = 1 + p_227470_.nextInt(3);

         for (int $$10 = 0; $$10 < $$9; $$10++) {
            $$7 = EndCityPieces.addHelper(p_227469_, EndCityPieces.addPiece(p_227465_, $$7, new BlockPos(0, 4, 0), "tower_piece", $$6, true));
            if ($$10 < $$9 - 1 && p_227470_.nextBoolean()) {
               $$8 = $$7;
            }
         }

         if ($$8 != null) {
            for (Tuple<Rotation, BlockPos> $$11 : EndCityPieces.TOWER_BRIDGES) {
               if (p_227470_.nextBoolean()) {
                  EndCityPieces.EndCityPiece $$12 = EndCityPieces.addHelper(
                     p_227469_, EndCityPieces.addPiece(p_227465_, $$8, $$11.getB(), "bridge_end", $$6.getRotated($$11.getA()), true)
                  );
                  EndCityPieces.recursiveChildren(p_227465_, EndCityPieces.TOWER_BRIDGE_GENERATOR, p_227466_ + 1, $$12, null, p_227469_, p_227470_);
               }
            }

            $$7 = EndCityPieces.addHelper(p_227469_, EndCityPieces.addPiece(p_227465_, $$7, new BlockPos(-1, 4, -1), "tower_top", $$6, true));
         } else {
            if (p_227466_ != 7) {
               return EndCityPieces.recursiveChildren(p_227465_, EndCityPieces.FAT_TOWER_GENERATOR, p_227466_ + 1, $$7, null, p_227469_, p_227470_);
            }

            $$7 = EndCityPieces.addHelper(p_227469_, EndCityPieces.addPiece(p_227465_, $$7, new BlockPos(-1, 4, -1), "tower_top", $$6, true));
         }

         return true;
      }
   };
   static final EndCityPieces.SectionGenerator TOWER_BRIDGE_GENERATOR = new EndCityPieces.SectionGenerator() {
      public boolean shipCreated;

      @Override
      public void init() {
         this.shipCreated = false;
      }

      @Override
      public boolean generate(
         StructureTemplateManager p_227475_,
         int p_227476_,
         EndCityPieces.EndCityPiece p_227477_,
         BlockPos p_227478_,
         List<StructurePiece> p_227479_,
         RandomSource p_227480_
      ) {
         Rotation $$6 = p_227477_.placeSettings().getRotation();
         int $$7 = p_227480_.nextInt(4) + 1;
         EndCityPieces.EndCityPiece $$8 = EndCityPieces.addHelper(
            p_227479_, EndCityPieces.addPiece(p_227475_, p_227477_, new BlockPos(0, 0, -4), "bridge_piece", $$6, true)
         );
         $$8.setGenDepth(-1);
         int $$9 = 0;

         for (int $$10 = 0; $$10 < $$7; $$10++) {
            if (p_227480_.nextBoolean()) {
               $$8 = EndCityPieces.addHelper(p_227479_, EndCityPieces.addPiece(p_227475_, $$8, new BlockPos(0, $$9, -4), "bridge_piece", $$6, true));
               $$9 = 0;
            } else {
               if (p_227480_.nextBoolean()) {
                  $$8 = EndCityPieces.addHelper(p_227479_, EndCityPieces.addPiece(p_227475_, $$8, new BlockPos(0, $$9, -4), "bridge_steep_stairs", $$6, true));
               } else {
                  $$8 = EndCityPieces.addHelper(p_227479_, EndCityPieces.addPiece(p_227475_, $$8, new BlockPos(0, $$9, -8), "bridge_gentle_stairs", $$6, true));
               }

               $$9 = 4;
            }
         }

         if (!this.shipCreated && p_227480_.nextInt(10 - p_227476_) == 0) {
            EndCityPieces.addHelper(
               p_227479_, EndCityPieces.addPiece(p_227475_, $$8, new BlockPos(-8 + p_227480_.nextInt(8), $$9, -70 + p_227480_.nextInt(10)), "ship", $$6, true)
            );
            this.shipCreated = true;
         } else if (!EndCityPieces.recursiveChildren(
            p_227475_, EndCityPieces.HOUSE_TOWER_GENERATOR, p_227476_ + 1, $$8, new BlockPos(-3, $$9 + 1, -11), p_227479_, p_227480_
         )) {
            return false;
         }

         $$8 = EndCityPieces.addHelper(
            p_227479_, EndCityPieces.addPiece(p_227475_, $$8, new BlockPos(4, $$9, 0), "bridge_end", $$6.getRotated(Rotation.CLOCKWISE_180), true)
         );
         $$8.setGenDepth(-1);
         return true;
      }
   };
   static final List<Tuple<Rotation, BlockPos>> FAT_TOWER_BRIDGES = Lists.newArrayList(
      new Tuple[]{
         new Tuple<>(Rotation.NONE, new BlockPos(4, -1, 0)),
         new Tuple<>(Rotation.CLOCKWISE_90, new BlockPos(12, -1, 4)),
         new Tuple<>(Rotation.COUNTERCLOCKWISE_90, new BlockPos(0, -1, 8)),
         new Tuple<>(Rotation.CLOCKWISE_180, new BlockPos(8, -1, 12))
      }
   );
   static final EndCityPieces.SectionGenerator FAT_TOWER_GENERATOR = new EndCityPieces.SectionGenerator() {
      @Override
      public void init() {
      }

      @Override
      public boolean generate(
         StructureTemplateManager p_227484_,
         int p_227485_,
         EndCityPieces.EndCityPiece p_227486_,
         BlockPos p_227487_,
         List<StructurePiece> p_227488_,
         RandomSource p_227489_
      ) {
         Rotation $$6 = p_227486_.placeSettings().getRotation();
         EndCityPieces.EndCityPiece $$7 = EndCityPieces.addHelper(
            p_227488_, EndCityPieces.addPiece(p_227484_, p_227486_, new BlockPos(-3, 4, -3), "fat_tower_base", $$6, true)
         );
         $$7 = EndCityPieces.addHelper(p_227488_, EndCityPieces.addPiece(p_227484_, $$7, new BlockPos(0, 4, 0), "fat_tower_middle", $$6, true));

         for (int $$8 = 0; $$8 < 2 && p_227489_.nextInt(3) != 0; $$8++) {
            $$7 = EndCityPieces.addHelper(p_227488_, EndCityPieces.addPiece(p_227484_, $$7, new BlockPos(0, 8, 0), "fat_tower_middle", $$6, true));

            for (Tuple<Rotation, BlockPos> $$9 : EndCityPieces.FAT_TOWER_BRIDGES) {
               if (p_227489_.nextBoolean()) {
                  EndCityPieces.EndCityPiece $$10 = EndCityPieces.addHelper(
                     p_227488_, EndCityPieces.addPiece(p_227484_, $$7, $$9.getB(), "bridge_end", $$6.getRotated($$9.getA()), true)
                  );
                  EndCityPieces.recursiveChildren(p_227484_, EndCityPieces.TOWER_BRIDGE_GENERATOR, p_227485_ + 1, $$10, null, p_227488_, p_227489_);
               }
            }
         }

         $$7 = EndCityPieces.addHelper(p_227488_, EndCityPieces.addPiece(p_227484_, $$7, new BlockPos(-2, 8, -2), "fat_tower_top", $$6, true));
         return true;
      }
   };

   static EndCityPieces.EndCityPiece addPiece(
      StructureTemplateManager p_227430_, EndCityPieces.EndCityPiece p_227431_, BlockPos p_227432_, String p_227433_, Rotation p_227434_, boolean p_227435_
   ) {
      EndCityPieces.EndCityPiece $$6 = new EndCityPieces.EndCityPiece(p_227430_, p_227433_, p_227431_.templatePosition(), p_227434_, p_227435_);
      BlockPos $$7 = p_227431_.template().calculateConnectedPosition(p_227431_.placeSettings(), p_227432_, $$6.placeSettings(), BlockPos.ZERO);
      $$6.move($$7.getX(), $$7.getY(), $$7.getZ());
      return $$6;
   }

   public static void startHouseTower(
      StructureTemplateManager p_227445_, BlockPos p_227446_, Rotation p_227447_, List<StructurePiece> p_227448_, RandomSource p_227449_
   ) {
      FAT_TOWER_GENERATOR.init();
      HOUSE_TOWER_GENERATOR.init();
      TOWER_BRIDGE_GENERATOR.init();
      TOWER_GENERATOR.init();
      EndCityPieces.EndCityPiece $$5 = addHelper(p_227448_, new EndCityPieces.EndCityPiece(p_227445_, "base_floor", p_227446_, p_227447_, true));
      $$5 = addHelper(p_227448_, addPiece(p_227445_, $$5, new BlockPos(-1, 0, -1), "second_floor_1", p_227447_, false));
      $$5 = addHelper(p_227448_, addPiece(p_227445_, $$5, new BlockPos(-1, 4, -1), "third_floor_1", p_227447_, false));
      $$5 = addHelper(p_227448_, addPiece(p_227445_, $$5, new BlockPos(-1, 8, -1), "third_roof", p_227447_, true));
      recursiveChildren(p_227445_, TOWER_GENERATOR, 1, $$5, null, p_227448_, p_227449_);
   }

   static EndCityPieces.EndCityPiece addHelper(List<StructurePiece> p_227451_, EndCityPieces.EndCityPiece p_227452_) {
      p_227451_.add(p_227452_);
      return p_227452_;
   }

   static boolean recursiveChildren(
      StructureTemplateManager p_227437_,
      EndCityPieces.SectionGenerator p_227438_,
      int p_227439_,
      EndCityPieces.EndCityPiece p_227440_,
      BlockPos p_227441_,
      List<StructurePiece> p_227442_,
      RandomSource p_227443_
   ) {
      if (p_227439_ > 8) {
         return false;
      } else {
         List<StructurePiece> $$7 = Lists.newArrayList();
         if (p_227438_.generate(p_227437_, p_227439_, p_227440_, p_227441_, $$7, p_227443_)) {
            boolean $$8 = false;
            int $$9 = p_227443_.nextInt();

            for (StructurePiece $$10 : $$7) {
               $$10.setGenDepth($$9);
               StructurePiece $$11 = StructurePiece.findCollisionPiece(p_227442_, $$10.getBoundingBox());
               if ($$11 != null && $$11.getGenDepth() != p_227440_.getGenDepth()) {
                  $$8 = true;
                  break;
               }
            }

            if (!$$8) {
               p_227442_.addAll($$7);
               return true;
            }
         }

         return false;
      }
   }

   public static class EndCityPiece extends TemplateStructurePiece {
      public EndCityPiece(StructureTemplateManager p_227491_, String p_227492_, BlockPos p_227493_, Rotation p_227494_, boolean p_227495_) {
         super(StructurePieceType.END_CITY_PIECE, 0, p_227491_, makeResourceLocation(p_227492_), p_227492_, makeSettings(p_227495_, p_227494_), p_227493_);
      }

      public EndCityPiece(StructureTemplateManager p_227497_, CompoundTag p_227498_) {
         super(
            StructurePieceType.END_CITY_PIECE,
            p_227498_,
            p_227497_,
            p_227512_ -> makeSettings(p_227498_.getBoolean("OW"), Rotation.valueOf(p_227498_.getString("Rot")))
         );
      }

      private static StructurePlaceSettings makeSettings(boolean p_227514_, Rotation p_227515_) {
         BlockIgnoreProcessor $$2 = p_227514_ ? BlockIgnoreProcessor.STRUCTURE_BLOCK : BlockIgnoreProcessor.STRUCTURE_AND_AIR;
         return new StructurePlaceSettings().setIgnoreEntities(true).addProcessor($$2).setRotation(p_227515_);
      }

      @Override
      protected ResourceLocation makeTemplateLocation() {
         return makeResourceLocation(this.templateName);
      }

      private static ResourceLocation makeResourceLocation(String p_227503_) {
         return ResourceLocation.withDefaultNamespace("end_city/" + p_227503_);
      }

      @Override
      protected void addAdditionalSaveData(StructurePieceSerializationContext p_227500_, CompoundTag p_227501_) {
         super.addAdditionalSaveData(p_227500_, p_227501_);
         p_227501_.putString("Rot", this.placeSettings.getRotation().name());
         p_227501_.putBoolean("OW", this.placeSettings.getProcessors().get(0) == BlockIgnoreProcessor.STRUCTURE_BLOCK);
      }

      @Override
      protected void handleDataMarker(String p_227505_, BlockPos p_227506_, ServerLevelAccessor p_227507_, RandomSource p_227508_, BoundingBox p_227509_) {
         if (p_227505_.startsWith("Chest")) {
            BlockPos $$5 = p_227506_.below();
            if (p_227509_.isInside($$5)) {
               RandomizableContainer.setBlockEntityLootTable(p_227507_, p_227508_, $$5, BuiltInLootTables.END_CITY_TREASURE);
            }
         } else if (p_227509_.isInside(p_227506_) && Level.isInSpawnableBounds(p_227506_)) {
            if (p_227505_.startsWith("Sentry")) {
               Shulker $$6 = EntityType.SHULKER.create(p_227507_.getLevel());
               if ($$6 != null) {
                  $$6.setPos(p_227506_.getX() + 0.5, p_227506_.getY(), p_227506_.getZ() + 0.5);
                  p_227507_.addFreshEntity($$6);
               }
            } else if (p_227505_.startsWith("Elytra")) {
               ItemFrame $$7 = new ItemFrame(p_227507_.getLevel(), p_227506_, this.placeSettings.getRotation().rotate(Direction.SOUTH));
               $$7.setItem(new ItemStack(Items.ELYTRA), false);
               p_227507_.addFreshEntity($$7);
            }
         }
      }
   }

   interface SectionGenerator {
      void init();

      boolean generate(StructureTemplateManager var1, int var2, EndCityPieces.EndCityPiece var3, BlockPos var4, List<StructurePiece> var5, RandomSource var6);
   }
}
