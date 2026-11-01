package net.minecraft.world.level.levelgen.structure.pools;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.Optionull;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.properties.StructureMode;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.JigsawReplacementProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

public class SinglePoolElement extends StructurePoolElement {
   private static final Codec<Either<ResourceLocation, StructureTemplate>> TEMPLATE_CODEC = Codec.of(
      SinglePoolElement::encodeTemplate, ResourceLocation.CODEC.map(Either::left)
   );
   public static final MapCodec<SinglePoolElement> CODEC = RecordCodecBuilder.mapCodec(
      p_352018_ -> p_352018_.group(templateCodec(), processorsCodec(), projectionCodec(), overrideLiquidSettingsCodec())
         .apply(p_352018_, SinglePoolElement::new)
   );
   protected final Either<ResourceLocation, StructureTemplate> template;
   protected final Holder<StructureProcessorList> processors;
   protected final Optional<LiquidSettings> overrideLiquidSettings;

   private static <T> DataResult<T> encodeTemplate(Either<ResourceLocation, StructureTemplate> p_210425_, DynamicOps<T> p_210426_, T p_210427_) {
      Optional<ResourceLocation> $$3 = p_210425_.left();
      return $$3.isEmpty()
         ? DataResult.error(() -> "Can not serialize a runtime pool element")
         : ResourceLocation.CODEC.encode($$3.get(), p_210426_, p_210427_);
   }

   protected static <E extends SinglePoolElement> RecordCodecBuilder<E, Holder<StructureProcessorList>> processorsCodec() {
      return StructureProcessorType.LIST_CODEC.fieldOf("processors").forGetter(p_210464_ -> p_210464_.processors);
   }

   protected static <E extends SinglePoolElement> RecordCodecBuilder<E, Optional<LiquidSettings>> overrideLiquidSettingsCodec() {
      return LiquidSettings.CODEC.optionalFieldOf("override_liquid_settings").forGetter(p_352017_ -> p_352017_.overrideLiquidSettings);
   }

   protected static <E extends SinglePoolElement> RecordCodecBuilder<E, Either<ResourceLocation, StructureTemplate>> templateCodec() {
      return TEMPLATE_CODEC.fieldOf("location").forGetter(p_210431_ -> p_210431_.template);
   }

   protected SinglePoolElement(
      Either<ResourceLocation, StructureTemplate> p_210415_,
      Holder<StructureProcessorList> p_210416_,
      StructureTemplatePool.Projection p_210417_,
      Optional<LiquidSettings> p_352236_
   ) {
      super(p_210417_);
      this.template = p_210415_;
      this.processors = p_210416_;
      this.overrideLiquidSettings = p_352236_;
   }

   @Override
   public Vec3i getSize(StructureTemplateManager p_227313_, Rotation p_227314_) {
      StructureTemplate $$2 = this.getTemplate(p_227313_);
      return $$2.getSize(p_227314_);
   }

   private StructureTemplate getTemplate(StructureTemplateManager p_227300_) {
      return (StructureTemplate)this.template.map(p_227300_::getOrCreate, Function.identity());
   }

   public List<StructureTemplate.StructureBlockInfo> getDataMarkers(
      StructureTemplateManager p_227325_, BlockPos p_227326_, Rotation p_227327_, boolean p_227328_
   ) {
      StructureTemplate $$4 = this.getTemplate(p_227325_);
      List<StructureTemplate.StructureBlockInfo> $$5 = $$4.filterBlocks(
         p_227326_, new StructurePlaceSettings().setRotation(p_227327_), Blocks.STRUCTURE_BLOCK, p_227328_
      );
      List<StructureTemplate.StructureBlockInfo> $$6 = Lists.newArrayList();

      for (StructureTemplate.StructureBlockInfo $$7 : $$5) {
         CompoundTag $$8 = $$7.nbt();
         if ($$8 != null) {
            StructureMode $$9 = StructureMode.valueOf($$8.getString("mode"));
            if ($$9 == StructureMode.DATA) {
               $$6.add($$7);
            }
         }
      }

      return $$6;
   }

   @Override
   public List<StructureTemplate.StructureBlockInfo> getShuffledJigsawBlocks(
      StructureTemplateManager p_227320_, BlockPos p_227321_, Rotation p_227322_, RandomSource p_227323_
   ) {
      StructureTemplate $$4 = this.getTemplate(p_227320_);
      ObjectArrayList<StructureTemplate.StructureBlockInfo> $$5 = $$4.filterBlocks(
         p_227321_, new StructurePlaceSettings().setRotation(p_227322_), Blocks.JIGSAW, true
      );
      Util.shuffle($$5, p_227323_);
      sortBySelectionPriority($$5);
      return $$5;
   }

   @VisibleForTesting
   static void sortBySelectionPriority(List<StructureTemplate.StructureBlockInfo> p_308881_) {
      p_308881_.sort(
         Comparator.<StructureTemplate.StructureBlockInfo>comparingInt(
               p_308863_ -> Optionull.mapOrDefault(p_308863_.nbt(), p_308864_ -> p_308864_.getInt("selection_priority"), 0)
            )
            .reversed()
      );
   }

   @Override
   public BoundingBox getBoundingBox(StructureTemplateManager p_227316_, BlockPos p_227317_, Rotation p_227318_) {
      StructureTemplate $$3 = this.getTemplate(p_227316_);
      return $$3.getBoundingBox(new StructurePlaceSettings().setRotation(p_227318_), p_227317_);
   }

   @Override
   public boolean place(
      StructureTemplateManager p_227302_,
      WorldGenLevel p_227303_,
      StructureManager p_227304_,
      ChunkGenerator p_227305_,
      BlockPos p_227306_,
      BlockPos p_227307_,
      Rotation p_227308_,
      BoundingBox p_227309_,
      RandomSource p_227310_,
      LiquidSettings p_352088_,
      boolean p_227311_
   ) {
      StructureTemplate $$11 = this.getTemplate(p_227302_);
      StructurePlaceSettings $$12 = this.getSettings(p_227308_, p_227309_, p_352088_, p_227311_);
      if (!$$11.placeInWorld(p_227303_, p_227306_, p_227307_, $$12, p_227310_, 18)) {
         return false;
      } else {
         for (StructureTemplate.StructureBlockInfo $$14 : StructureTemplate.processBlockInfos(
            p_227303_, p_227306_, p_227307_, $$12, this.getDataMarkers(p_227302_, p_227306_, p_227308_, false)
         )) {
            this.handleDataMarker(p_227303_, $$14, p_227306_, p_227308_, p_227310_, p_227309_);
         }

         return true;
      }
   }

   protected StructurePlaceSettings getSettings(Rotation p_210421_, BoundingBox p_210422_, LiquidSettings p_352322_, boolean p_210423_) {
      StructurePlaceSettings $$4 = new StructurePlaceSettings();
      $$4.setBoundingBox(p_210422_);
      $$4.setRotation(p_210421_);
      $$4.setKnownShape(true);
      $$4.setIgnoreEntities(false);
      $$4.addProcessor(BlockIgnoreProcessor.STRUCTURE_BLOCK);
      $$4.setFinalizeEntities(true);
      $$4.setLiquidSettings(this.overrideLiquidSettings.orElse(p_352322_));
      if (!p_210423_) {
         $$4.addProcessor(JigsawReplacementProcessor.INSTANCE);
      }

      this.processors.value().list().forEach($$4::addProcessor);
      this.getProjection().getProcessors().forEach($$4::addProcessor);
      return $$4;
   }

   @Override
   public StructurePoolElementType<?> getType() {
      return StructurePoolElementType.SINGLE;
   }

   @Override
   public String toString() {
      return "Single[" + this.template + "]";
   }
}
