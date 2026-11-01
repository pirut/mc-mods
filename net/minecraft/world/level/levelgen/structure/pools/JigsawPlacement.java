package net.minecraft.world.level.levelgen.structure.pools;

import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.Pools;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.SequencedPriorityIterator;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.block.JigsawBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;
import net.minecraft.world.level.levelgen.structure.pools.alias.PoolAliasLookup;
import net.minecraft.world.level.levelgen.structure.structures.JigsawStructure;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.apache.commons.lang3.mutable.MutableObject;
import org.slf4j.Logger;

public class JigsawPlacement {
   static final Logger LOGGER = LogUtils.getLogger();

   public static Optional<Structure.GenerationStub> addPieces(
      Structure.GenerationContext p_227239_,
      Holder<StructureTemplatePool> p_227240_,
      Optional<ResourceLocation> p_227241_,
      int p_227242_,
      BlockPos p_227243_,
      boolean p_227244_,
      Optional<Heightmap.Types> p_227245_,
      int p_227246_,
      PoolAliasLookup p_307522_,
      DimensionPadding p_348489_,
      LiquidSettings p_352161_
   ) {
      RegistryAccess $$11 = p_227239_.registryAccess();
      ChunkGenerator $$12 = p_227239_.chunkGenerator();
      StructureTemplateManager $$13 = p_227239_.structureTemplateManager();
      LevelHeightAccessor $$14 = p_227239_.heightAccessor();
      WorldgenRandom $$15 = p_227239_.random();
      Registry<StructureTemplatePool> $$16 = $$11.registryOrThrow(Registries.TEMPLATE_POOL);
      Rotation $$17 = Rotation.getRandom($$15);
      StructureTemplatePool $$18 = p_227240_.unwrapKey()
         .flatMap(p_314915_ -> $$16.getOptional(p_307522_.lookup((ResourceKey<StructureTemplatePool>)p_314915_)))
         .orElse(p_227240_.value());
      StructurePoolElement $$19 = $$18.getRandomTemplate($$15);
      if ($$19 == EmptyPoolElement.INSTANCE) {
         return Optional.empty();
      } else {
         BlockPos $$22;
         if (p_227241_.isPresent()) {
            ResourceLocation $$20 = p_227241_.get();
            Optional<BlockPos> $$21 = getRandomNamedJigsaw($$19, $$20, p_227243_, $$17, $$13, $$15);
            if ($$21.isEmpty()) {
               LOGGER.error(
                  "No starting jigsaw {} found in start pool {}",
                  $$20,
                  p_227240_.unwrapKey().map(p_248484_ -> p_248484_.location().toString()).orElse("<unregistered>")
               );
               return Optional.empty();
            }

            $$22 = $$21.get();
         } else {
            $$22 = p_227243_;
         }

         Vec3i $$24 = $$22.subtract(p_227243_);
         BlockPos $$25 = p_227243_.subtract($$24);
         PoolElementStructurePiece $$26 = new PoolElementStructurePiece(
            $$13, $$19, $$25, $$19.getGroundLevelDelta(), $$17, $$19.getBoundingBox($$13, $$25, $$17), p_352161_
         );
         BoundingBox $$27 = $$26.getBoundingBox();
         int $$28 = ($$27.maxX() + $$27.minX()) / 2;
         int $$29 = ($$27.maxZ() + $$27.minZ()) / 2;
         int $$30;
         if (p_227245_.isPresent()) {
            $$30 = p_227243_.getY() + $$12.getFirstFreeHeight($$28, $$29, p_227245_.get(), $$14, p_227239_.randomState());
         } else {
            $$30 = $$25.getY();
         }

         int $$32 = $$27.minY() + $$26.getGroundLevelDelta();
         $$26.move(0, $$30 - $$32, 0);
         int $$33 = $$30 + $$24.getY();
         return Optional.of(
            new Structure.GenerationStub(
               new BlockPos($$28, $$33, $$29),
               (Consumer<StructurePiecesBuilder>)(p_352014_ -> {
                  List<PoolElementStructurePiece> $$18x = Lists.newArrayList();
                  $$18x.add($$26);
                  if (p_227242_ > 0) {
                     AABB $$19x = new AABB(
                        $$28 - p_227246_,
                        Math.max($$33 - p_227246_, $$14.getMinBuildHeight() + p_348489_.bottom()),
                        $$29 - p_227246_,
                        $$28 + p_227246_ + 1,
                        Math.min($$33 + p_227246_ + 1, $$14.getMaxBuildHeight() - p_348489_.top()),
                        $$29 + p_227246_ + 1
                     );
                     VoxelShape $$20 = Shapes.join(Shapes.create($$19x), Shapes.create(AABB.of($$27)), BooleanOp.ONLY_FIRST);
                     addPieces(p_227239_.randomState(), p_227242_, p_227244_, $$12, $$13, $$14, $$15, $$16, $$26, $$18x, $$20, p_307522_, p_352161_);
                     $$18x.forEach(p_352014_::addPiece);
                  }
               })
            )
         );
      }
   }

   private static Optional<BlockPos> getRandomNamedJigsaw(
      StructurePoolElement p_227248_,
      ResourceLocation p_227249_,
      BlockPos p_227250_,
      Rotation p_227251_,
      StructureTemplateManager p_227252_,
      WorldgenRandom p_227253_
   ) {
      List<StructureTemplate.StructureBlockInfo> $$6 = p_227248_.getShuffledJigsawBlocks(p_227252_, p_227250_, p_227251_, p_227253_);
      Optional<BlockPos> $$7 = Optional.empty();

      for (StructureTemplate.StructureBlockInfo $$8 : $$6) {
         ResourceLocation $$9 = ResourceLocation.tryParse(Objects.requireNonNull($$8.nbt(), () -> $$8 + " nbt was null").getString("name"));
         if (p_227249_.equals($$9)) {
            $$7 = Optional.of($$8.pos());
            break;
         }
      }

      return $$7;
   }

   private static void addPieces(
      RandomState p_227211_,
      int p_227212_,
      boolean p_227213_,
      ChunkGenerator p_227214_,
      StructureTemplateManager p_227215_,
      LevelHeightAccessor p_227216_,
      RandomSource p_227217_,
      Registry<StructureTemplatePool> p_227218_,
      PoolElementStructurePiece p_227219_,
      List<PoolElementStructurePiece> p_227220_,
      VoxelShape p_227221_,
      PoolAliasLookup p_307210_,
      LiquidSettings p_352331_
   ) {
      JigsawPlacement.Placer $$13 = new JigsawPlacement.Placer(p_227218_, p_227212_, p_227214_, p_227215_, p_227220_, p_227217_);
      $$13.tryPlacingChildren(p_227219_, new MutableObject(p_227221_), 0, p_227213_, p_227216_, p_227211_, p_307210_, p_352331_);

      while ($$13.placing.hasNext()) {
         JigsawPlacement.PieceState $$14 = (JigsawPlacement.PieceState)$$13.placing.next();
         $$13.tryPlacingChildren($$14.piece, $$14.free, $$14.depth, p_227213_, p_227216_, p_227211_, p_307210_, p_352331_);
      }
   }

   public static boolean generateJigsaw(
      ServerLevel p_227204_, Holder<StructureTemplatePool> p_227205_, ResourceLocation p_227206_, int p_227207_, BlockPos p_227208_, boolean p_227209_
   ) {
      ChunkGenerator $$6 = p_227204_.getChunkSource().getGenerator();
      StructureTemplateManager $$7 = p_227204_.getStructureManager();
      StructureManager $$8 = p_227204_.structureManager();
      RandomSource $$9 = p_227204_.getRandom();
      Structure.GenerationContext $$10 = new Structure.GenerationContext(
         p_227204_.registryAccess(),
         $$6,
         $$6.getBiomeSource(),
         p_227204_.getChunkSource().randomState(),
         $$7,
         p_227204_.getSeed(),
         new ChunkPos(p_227208_),
         p_227204_,
         p_227255_ -> true
      );
      Optional<Structure.GenerationStub> $$11 = addPieces(
         $$10,
         p_227205_,
         Optional.of(p_227206_),
         p_227207_,
         p_227208_,
         false,
         Optional.empty(),
         128,
         PoolAliasLookup.EMPTY,
         JigsawStructure.DEFAULT_DIMENSION_PADDING,
         JigsawStructure.DEFAULT_LIQUID_SETTINGS
      );
      if ($$11.isPresent()) {
         StructurePiecesBuilder $$12 = $$11.get().getPiecesBuilder();

         for (StructurePiece $$13 : $$12.build().pieces()) {
            if ($$13 instanceof PoolElementStructurePiece $$14) {
               $$14.place(p_227204_, $$8, $$6, $$9, BoundingBox.infinite(), p_227208_, p_227209_);
            }
         }

         return true;
      } else {
         return false;
      }
   }

   record PieceState(PoolElementStructurePiece piece, MutableObject<VoxelShape> free, int depth) {
   }

   static final class Placer {
      private final Registry<StructureTemplatePool> pools;
      private final int maxDepth;
      private final ChunkGenerator chunkGenerator;
      private final StructureTemplateManager structureTemplateManager;
      private final List<? super PoolElementStructurePiece> pieces;
      private final RandomSource random;
      final SequencedPriorityIterator<JigsawPlacement.PieceState> placing = new SequencedPriorityIterator<>();

      Placer(
         Registry<StructureTemplatePool> p_227258_,
         int p_227259_,
         ChunkGenerator p_227260_,
         StructureTemplateManager p_227261_,
         List<? super PoolElementStructurePiece> p_227262_,
         RandomSource p_227263_
      ) {
         this.pools = p_227258_;
         this.maxDepth = p_227259_;
         this.chunkGenerator = p_227260_;
         this.structureTemplateManager = p_227261_;
         this.pieces = p_227262_;
         this.random = p_227263_;
      }

      void tryPlacingChildren(
         PoolElementStructurePiece p_227265_,
         MutableObject<VoxelShape> p_227266_,
         int p_227267_,
         boolean p_227268_,
         LevelHeightAccessor p_227269_,
         RandomState p_227270_,
         PoolAliasLookup p_307643_,
         LiquidSettings p_352442_
      ) {
         StructurePoolElement $$8 = p_227265_.getElement();
         BlockPos $$9 = p_227265_.getPosition();
         Rotation $$10 = p_227265_.getRotation();
         StructureTemplatePool.Projection $$11 = $$8.getProjection();
         boolean $$12 = $$11 == StructureTemplatePool.Projection.RIGID;
         MutableObject<VoxelShape> $$13 = new MutableObject();
         BoundingBox $$14 = p_227265_.getBoundingBox();
         int $$15 = $$14.minY();

         label134:
         for (StructureTemplate.StructureBlockInfo $$16 : $$8.getShuffledJigsawBlocks(this.structureTemplateManager, $$9, $$10, this.random)) {
            Direction $$17 = JigsawBlock.getFrontFacing($$16.state());
            BlockPos $$18 = $$16.pos();
            BlockPos $$19 = $$18.relative($$17);
            int $$20 = $$18.getY() - $$15;
            int $$21 = -1;
            ResourceKey<StructureTemplatePool> $$22 = readPoolKey($$16, p_307643_);
            Optional<? extends Holder<StructureTemplatePool>> $$23 = this.pools.getHolder($$22);
            if ($$23.isEmpty()) {
               JigsawPlacement.LOGGER.warn("Empty or non-existent pool: {}", $$22.location());
            } else {
               Holder<StructureTemplatePool> $$24 = (Holder<StructureTemplatePool>)$$23.get();
               if ($$24.value().size() == 0 && !$$24.is(Pools.EMPTY)) {
                  JigsawPlacement.LOGGER.warn("Empty or non-existent pool: {}", $$22.location());
               } else {
                  Holder<StructureTemplatePool> $$25 = $$24.value().getFallback();
                  if ($$25.value().size() == 0 && !$$25.is(Pools.EMPTY)) {
                     JigsawPlacement.LOGGER
                        .warn(
                           "Empty or non-existent fallback pool: {}",
                           $$25.unwrapKey().map(p_255599_ -> p_255599_.location().toString()).orElse("<unregistered>")
                        );
                  } else {
                     boolean $$26 = $$14.isInside($$19);
                     MutableObject<VoxelShape> $$27;
                     if ($$26) {
                        $$27 = $$13;
                        if ($$13.getValue() == null) {
                           $$13.setValue(Shapes.create(AABB.of($$14)));
                        }
                     } else {
                        $$27 = p_227266_;
                     }

                     List<StructurePoolElement> $$29 = Lists.newArrayList();
                     if (p_227267_ != this.maxDepth) {
                        $$29.addAll($$24.value().getShuffledTemplates(this.random));
                     }

                     $$29.addAll($$25.value().getShuffledTemplates(this.random));
                     int $$30 = $$16.nbt() != null ? $$16.nbt().getInt("placement_priority") : 0;

                     for (StructurePoolElement $$31 : $$29) {
                        if ($$31 == EmptyPoolElement.INSTANCE) {
                           break;
                        }

                        for (Rotation $$32 : Rotation.getShuffled(this.random)) {
                           List<StructureTemplate.StructureBlockInfo> $$33 = $$31.getShuffledJigsawBlocks(
                              this.structureTemplateManager, BlockPos.ZERO, $$32, this.random
                           );
                           BoundingBox $$34 = $$31.getBoundingBox(this.structureTemplateManager, BlockPos.ZERO, $$32);
                           int $$36;
                           if (p_227268_ && $$34.getYSpan() <= 16) {
                              $$36 = $$33.stream().mapToInt(p_255598_ -> {
                                 if (!$$34.isInside(p_255598_.pos().relative(JigsawBlock.getFrontFacing(p_255598_.state())))) {
                                    return 0;
                                 } else {
                                    ResourceKey<StructureTemplatePool> $$3 = readPoolKey(p_255598_, p_307643_);
                                    Optional<? extends Holder<StructureTemplatePool>> $$4 = this.pools.getHolder($$3);
                                    Optional<Holder<StructureTemplatePool>> $$5 = $$4.map(p_255600_ -> p_255600_.value().getFallback());
                                    int $$6 = $$4.<Integer>map(p_255596_ -> p_255596_.value().getMaxSize(this.structureTemplateManager)).orElse(0);
                                    int $$7 = $$5.<Integer>map(p_255601_ -> p_255601_.value().getMaxSize(this.structureTemplateManager)).orElse(0);
                                    return Math.max($$6, $$7);
                                 }
                              }).max().orElse(0);
                           } else {
                              $$36 = 0;
                           }

                           for (StructureTemplate.StructureBlockInfo $$37 : $$33) {
                              if (JigsawBlock.canAttach($$16, $$37)) {
                                 BlockPos $$38 = $$37.pos();
                                 BlockPos $$39 = $$19.subtract($$38);
                                 BoundingBox $$40 = $$31.getBoundingBox(this.structureTemplateManager, $$39, $$32);
                                 int $$41 = $$40.minY();
                                 StructureTemplatePool.Projection $$42 = $$31.getProjection();
                                 boolean $$43 = $$42 == StructureTemplatePool.Projection.RIGID;
                                 int $$44 = $$38.getY();
                                 int $$45 = $$20 - $$44 + JigsawBlock.getFrontFacing($$16.state()).getStepY();
                                 int $$46;
                                 if ($$12 && $$43) {
                                    $$46 = $$15 + $$45;
                                 } else {
                                    if ($$21 == -1) {
                                       $$21 = this.chunkGenerator
                                          .getFirstFreeHeight($$18.getX(), $$18.getZ(), Heightmap.Types.WORLD_SURFACE_WG, p_227269_, p_227270_);
                                    }

                                    $$46 = $$21 - $$44;
                                 }

                                 int $$48 = $$46 - $$41;
                                 BoundingBox $$49 = $$40.moved(0, $$48, 0);
                                 BlockPos $$50 = $$39.offset(0, $$48, 0);
                                 if ($$36 > 0) {
                                    int $$51 = Math.max($$36 + 1, $$49.maxY() - $$49.minY());
                                    $$49.encapsulate(new BlockPos($$49.minX(), $$49.minY() + $$51, $$49.minZ()));
                                 }

                                 if (!Shapes.joinIsNotEmpty((VoxelShape)$$27.getValue(), Shapes.create(AABB.of($$49).deflate(0.25)), BooleanOp.ONLY_SECOND)) {
                                    $$27.setValue(Shapes.joinUnoptimized((VoxelShape)$$27.getValue(), Shapes.create(AABB.of($$49)), BooleanOp.ONLY_FIRST));
                                    int $$52 = p_227265_.getGroundLevelDelta();
                                    int $$53;
                                    if ($$43) {
                                       $$53 = $$52 - $$45;
                                    } else {
                                       $$53 = $$31.getGroundLevelDelta();
                                    }

                                    PoolElementStructurePiece $$55 = new PoolElementStructurePiece(
                                       this.structureTemplateManager, $$31, $$50, $$53, $$32, $$49, p_352442_
                                    );
                                    int $$56;
                                    if ($$12) {
                                       $$56 = $$15 + $$20;
                                    } else if ($$43) {
                                       $$56 = $$46 + $$44;
                                    } else {
                                       if ($$21 == -1) {
                                          $$21 = this.chunkGenerator
                                             .getFirstFreeHeight($$18.getX(), $$18.getZ(), Heightmap.Types.WORLD_SURFACE_WG, p_227269_, p_227270_);
                                       }

                                       $$56 = $$21 + $$45 / 2;
                                    }

                                    p_227265_.addJunction(new JigsawJunction($$19.getX(), $$56 - $$20 + $$52, $$19.getZ(), $$45, $$42));
                                    $$55.addJunction(new JigsawJunction($$18.getX(), $$56 - $$44 + $$53, $$18.getZ(), -$$45, $$11));
                                    this.pieces.add($$55);
                                    if (p_227267_ + 1 <= this.maxDepth) {
                                       JigsawPlacement.PieceState $$59 = new JigsawPlacement.PieceState($$55, $$27, p_227267_ + 1);
                                       this.placing.add($$59, $$30);
                                    }
                                    continue label134;
                                 }
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      }

      private static ResourceKey<StructureTemplatePool> readPoolKey(StructureTemplate.StructureBlockInfo p_307327_, PoolAliasLookup p_307490_) {
         CompoundTag $$2 = Objects.requireNonNull(p_307327_.nbt(), () -> p_307327_ + " nbt was null");
         ResourceKey<StructureTemplatePool> $$3 = Pools.parseKey($$2.getString("pool"));
         return p_307490_.lookup($$3);
      }
   }
}
