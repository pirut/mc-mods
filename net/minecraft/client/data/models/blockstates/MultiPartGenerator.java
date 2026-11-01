package net.minecraft.client.data.models.blockstates;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.data.models.MultiVariant;
import net.minecraft.client.renderer.block.model.BlockModelDefinition;
import net.minecraft.client.renderer.block.model.multipart.Condition;
import net.minecraft.client.renderer.block.model.multipart.Selector;
import net.minecraft.world.level.block.Block;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class MultiPartGenerator implements BlockModelDefinitionGenerator {
   private final Block block;
   private final List<MultiPartGenerator.Entry> parts = new ArrayList<>();

   private MultiPartGenerator(Block p_387028_) {
      this.block = p_387028_;
   }

   @Override
   public Block block() {
      return this.block;
   }

   public static MultiPartGenerator multiPart(Block p_388674_) {
      return new MultiPartGenerator(p_388674_);
   }

   public MultiPartGenerator with(MultiVariant p_410084_) {
      this.parts.add(new MultiPartGenerator.Entry(Optional.empty(), p_410084_));
      return this;
   }

   private void validateCondition(Condition p_405286_) {
      p_405286_.instantiate(this.block.getStateDefinition());
   }

   public MultiPartGenerator with(Condition p_405613_, MultiVariant p_409963_) {
      this.validateCondition(p_405613_);
      this.parts.add(new MultiPartGenerator.Entry(Optional.of(p_405613_), p_409963_));
      return this;
   }

   public MultiPartGenerator with(ConditionBuilder p_410862_, MultiVariant p_410768_) {
      return this.with(p_410862_.build(), p_410768_);
   }

   @Override
   public BlockModelDefinition create() {
      return new BlockModelDefinition(
         Optional.empty(), Optional.of(new BlockModelDefinition.MultiPartDefinition(this.parts.stream().map(MultiPartGenerator.Entry::toUnbaked).toList()))
      );
   }

   @OnlyIn(Dist.CLIENT)
   record Entry(Optional<Condition> condition, MultiVariant variants) {
      public Selector toUnbaked() {
         return new Selector(this.condition, this.variants.toUnbaked());
      }
   }
}
