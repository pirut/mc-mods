package net.minecraft.world.level.levelgen.structure.pools;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import org.jspecify.annotations.Nullable;

public abstract class StructurePoolElement {
   public static final Codec<StructurePoolElement> CODEC = BuiltInRegistries.STRUCTURE_POOL_ELEMENT
      .byNameCodec()
      .dispatch("element_type", StructurePoolElement::getType, StructurePoolElementType::codec);
   private static final Holder<StructureProcessorList> EMPTY = Holder.direct(new StructureProcessorList(List.of()));
   @Nullable
   private volatile StructureTemplatePool.Projection projection;

   protected static <E extends StructurePoolElement> RecordCodecBuilder<E, StructureTemplatePool.Projection> projectionCodec() {
      return StructureTemplatePool.Projection.CODEC.fieldOf("projection").forGetter(StructurePoolElement::getProjection);
   }

   protected StructurePoolElement(StructureTemplatePool.Projection p_210471_) {
      this.projection = p_210471_;
   }

   public abstract Vec3i getSize(StructureTemplateManager var1, Rotation var2);

   public abstract List<StructureTemplate.JigsawBlockInfo> getShuffledJigsawBlocks(
      StructureTemplateManager var1, BlockPos var2, Rotation var3, RandomSource var4
   );

   public abstract BoundingBox getBoundingBox(StructureTemplateManager var1, BlockPos var2, Rotation var3);

   public abstract boolean place(
      StructureTemplateManager var1,
      WorldGenLevel var2,
      StructureManager var3,
      ChunkGenerator var4,
      BlockPos var5,
      BlockPos var6,
      Rotation var7,
      BoundingBox var8,
      RandomSource var9,
      LiquidSettings var10,
      boolean var11
   );

   public abstract StructurePoolElementType<?> getType();

   public void handleDataMarker(
      LevelAccessor p_227330_,
      StructureTemplate.StructureBlockInfo p_227331_,
      BlockPos p_227332_,
      Rotation p_227333_,
      RandomSource p_227334_,
      BoundingBox p_227335_
   ) {
   }

   public StructurePoolElement setProjection(StructureTemplatePool.Projection p_210479_) {
      this.projection = p_210479_;
      return this;
   }

   public StructureTemplatePool.Projection getProjection() {
      StructureTemplatePool.Projection structuretemplatepool$projection = this.projection;
      if (structuretemplatepool$projection == null) {
         throw new IllegalStateException();
      } else {
         return structuretemplatepool$projection;
      }
   }

   public int getGroundLevelDelta() {
      return 1;
   }

   public static Function<StructureTemplatePool.Projection, EmptyPoolElement> empty() {
      return p_210525_ -> EmptyPoolElement.INSTANCE;
   }

   public static Function<StructureTemplatePool.Projection, LegacySinglePoolElement> legacy(String p_210508_) {
      return p_466736_ -> new LegacySinglePoolElement(Either.left(Identifier.parse(p_210508_)), EMPTY, p_466736_, Optional.empty());
   }

   public static Function<StructureTemplatePool.Projection, LegacySinglePoolElement> legacy(String p_210513_, Holder<StructureProcessorList> p_210514_) {
      return p_466739_ -> new LegacySinglePoolElement(Either.left(Identifier.parse(p_210513_)), p_210514_, p_466739_, Optional.empty());
   }

   public static Function<StructureTemplatePool.Projection, SinglePoolElement> single(String p_210527_) {
      return p_466744_ -> new SinglePoolElement(Either.left(Identifier.parse(p_210527_)), EMPTY, p_466744_, Optional.empty());
   }

   public static Function<StructureTemplatePool.Projection, SinglePoolElement> single(String p_210532_, Holder<StructureProcessorList> p_210533_) {
      return p_466742_ -> new SinglePoolElement(Either.left(Identifier.parse(p_210532_)), p_210533_, p_466742_, Optional.empty());
   }

   public static Function<StructureTemplatePool.Projection, SinglePoolElement> single(String p_352251_, LiquidSettings p_352165_) {
      return p_466730_ -> new SinglePoolElement(Either.left(Identifier.parse(p_352251_)), EMPTY, p_466730_, Optional.of(p_352165_));
   }

   public static Function<StructureTemplatePool.Projection, SinglePoolElement> single(
      String p_352097_, Holder<StructureProcessorList> p_352452_, LiquidSettings p_352329_
   ) {
      return p_466734_ -> new SinglePoolElement(Either.left(Identifier.parse(p_352097_)), p_352452_, p_466734_, Optional.of(p_352329_));
   }

   public static Function<StructureTemplatePool.Projection, FeaturePoolElement> feature(Holder<PlacedFeature> p_210503_) {
      return p_210506_ -> new FeaturePoolElement(p_210503_, p_210506_);
   }

   public static Function<StructureTemplatePool.Projection, ListPoolElement> list(
      List<Function<StructureTemplatePool.Projection, ? extends StructurePoolElement>> p_210520_
   ) {
      return p_210523_ -> new ListPoolElement(p_210520_.stream().map(p_210482_ -> p_210482_.apply(p_210523_)).collect(Collectors.toList()), p_210523_);
   }
}
