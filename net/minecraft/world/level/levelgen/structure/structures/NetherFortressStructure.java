package net.minecraft.world.level.levelgen.structure.structures;

import com.mojang.serialization.MapCodec;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.random.WeightedRandomList;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;

public class NetherFortressStructure extends Structure {
   public static final WeightedRandomList<MobSpawnSettings.SpawnerData> FORTRESS_ENEMIES = WeightedRandomList.create(
      new MobSpawnSettings.SpawnerData(EntityType.BLAZE, 10, 2, 3),
      new MobSpawnSettings.SpawnerData(EntityType.ZOMBIFIED_PIGLIN, 5, 4, 4),
      new MobSpawnSettings.SpawnerData(EntityType.WITHER_SKELETON, 8, 5, 5),
      new MobSpawnSettings.SpawnerData(EntityType.SKELETON, 2, 5, 5),
      new MobSpawnSettings.SpawnerData(EntityType.MAGMA_CUBE, 3, 4, 4)
   );
   public static final MapCodec<NetherFortressStructure> CODEC = simpleCodec(NetherFortressStructure::new);

   public NetherFortressStructure(Structure.StructureSettings p_228521_) {
      super(p_228521_);
   }

   @Override
   public Optional<Structure.GenerationStub> findGenerationPoint(Structure.GenerationContext p_228523_) {
      ChunkPos $$1 = p_228523_.chunkPos();
      BlockPos $$2 = new BlockPos($$1.getMinBlockX(), 64, $$1.getMinBlockZ());
      return Optional.of(new Structure.GenerationStub($$2, (Consumer<StructurePiecesBuilder>)(p_228526_ -> generatePieces(p_228526_, p_228523_))));
   }

   private static void generatePieces(StructurePiecesBuilder p_228528_, Structure.GenerationContext p_228529_) {
      NetherFortressPieces.StartPiece $$2 = new NetherFortressPieces.StartPiece(
         p_228529_.random(), p_228529_.chunkPos().getBlockX(2), p_228529_.chunkPos().getBlockZ(2)
      );
      p_228528_.addPiece($$2);
      $$2.addChildren($$2, p_228528_, p_228529_.random());
      List<StructurePiece> $$3 = $$2.pendingChildren;

      while (!$$3.isEmpty()) {
         int $$4 = p_228529_.random().nextInt($$3.size());
         StructurePiece $$5 = $$3.remove($$4);
         $$5.addChildren($$2, p_228528_, p_228529_.random());
      }

      p_228528_.moveInsideHeights(p_228529_.random(), 48, 70);
   }

   @Override
   public StructureType<?> type() {
      return StructureType.FORTRESS;
   }
}
