package net.minecraft.client.data.models.blockstates;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import net.minecraft.client.data.models.MultiVariant;
import net.minecraft.client.renderer.block.model.BlockModelDefinition;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.block.model.VariantMutator;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.Property;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.model.generators.blockstate.UnbakedMutator;

@OnlyIn(Dist.CLIENT)
public class MultiVariantGenerator implements BlockModelDefinitionGenerator {
   private final Block block;
   private final List<MultiVariantGenerator.Entry> entries;
   private final Set<Property<?>> seenProperties;

   MultiVariantGenerator(Block p_388671_, List<MultiVariantGenerator.Entry> p_386519_, Set<Property<?>> p_405706_) {
      this.block = p_388671_;
      this.entries = p_386519_;
      this.seenProperties = p_405706_;
   }

   static Set<Property<?>> validateAndExpandProperties(Set<Property<?>> p_405585_, Block p_405741_, PropertyDispatch<?> p_405105_) {
      List<Property<?>> list = p_405105_.getDefinedProperties();
      list.forEach(p_403953_ -> {
         if (p_405741_.getStateDefinition().getProperty(p_403953_.getName()) != p_403953_) {
            throw new IllegalStateException("Property " + p_403953_ + " is not defined for block " + p_405741_);
         } else if (p_405585_.contains(p_403953_)) {
            throw new IllegalStateException("Values of property " + p_403953_ + " already defined for block " + p_405741_);
         }
      });
      Set<Property<?>> set = new HashSet<>(p_405585_);
      set.addAll(list);
      return set;
   }

   public MultiVariantGenerator with(PropertyDispatch<VariantMutator> p_388256_) {
      Set<Property<?>> set = validateAndExpandProperties(this.seenProperties, this.block, p_388256_);
      List<MultiVariantGenerator.Entry> list = this.entries.stream().flatMap(p_403955_ -> p_403955_.apply(p_388256_)).toList();
      return new MultiVariantGenerator(this.block, list, set);
   }

   public MultiVariantGenerator with(VariantMutator p_405854_) {
      List<MultiVariantGenerator.Entry> list = this.entries.stream().flatMap(p_403950_ -> p_403950_.apply(p_405854_)).toList();
      return new MultiVariantGenerator(this.block, list, this.seenProperties);
   }

   public MultiVariantGenerator withUnbaked(PropertyDispatch<UnbakedMutator> dispatch) {
      Set<Property<?>> set = validateAndExpandProperties(this.seenProperties, this.block, dispatch);
      List<MultiVariantGenerator.Entry> list = this.entries.stream().flatMap(e -> e.applyToUnbaked(dispatch)).toList();
      return new MultiVariantGenerator(this.block, list, set);
   }

   public MultiVariantGenerator withUnbaked(UnbakedMutator dispatch) {
      List<MultiVariantGenerator.Entry> list = this.entries.stream().flatMap(e -> e.applyToUnbaked(dispatch)).toList();
      return new MultiVariantGenerator(this.block, list, this.seenProperties);
   }

   @Override
   public BlockModelDefinition create() {
      Map<String, BlockStateModel.Unbaked> map = new HashMap<>();

      for (MultiVariantGenerator.Entry multivariantgenerator$entry : this.entries) {
         map.put(multivariantgenerator$entry.properties.getKey(), multivariantgenerator$entry.variant.toUnbaked());
      }

      return new BlockModelDefinition(Optional.of(new BlockModelDefinition.SimpleModelSelectors(map)), Optional.empty());
   }

   @Override
   public Block block() {
      return this.block;
   }

   public static MultiVariantGenerator.Empty dispatch(Block p_405256_) {
      return new MultiVariantGenerator.Empty(p_405256_);
   }

   public static MultiVariantGenerator dispatch(Block p_405526_, MultiVariant p_409935_) {
      return new MultiVariantGenerator(p_405526_, List.of(new MultiVariantGenerator.Entry(PropertyValueList.EMPTY, p_409935_)), Set.of());
   }

   @OnlyIn(Dist.CLIENT)
   public static class Empty {
      private final Block block;

      public Empty(Block p_404725_) {
         this.block = p_404725_;
      }

      public MultiVariantGenerator with(PropertyDispatch<MultiVariant> p_405221_) {
         Set<Property<?>> set = MultiVariantGenerator.validateAndExpandProperties(Set.of(), this.block, p_405221_);
         List<MultiVariantGenerator.Entry> list = p_405221_.getEntries()
            .entrySet()
            .stream()
            .map(p_409006_ -> new MultiVariantGenerator.Entry(p_409006_.getKey(), p_409006_.getValue()))
            .toList();
         return new MultiVariantGenerator(this.block, list, set);
      }
   }

   @OnlyIn(Dist.CLIENT)
   record Entry(PropertyValueList properties, MultiVariant variant) {
      public Stream<MultiVariantGenerator.Entry> apply(PropertyDispatch<VariantMutator> p_404760_) {
         return p_404760_.getEntries().entrySet().stream().map(p_409007_ -> {
            PropertyValueList propertyvaluelist = this.properties.extend(p_409007_.getKey());
            MultiVariant multivariant = this.variant.with(p_409007_.getValue());
            return new MultiVariantGenerator.Entry(propertyvaluelist, multivariant);
         });
      }

      public Stream<MultiVariantGenerator.Entry> apply(VariantMutator p_404774_) {
         return Stream.of(new MultiVariantGenerator.Entry(this.properties, this.variant.with(p_404774_)));
      }

      public Stream<MultiVariantGenerator.Entry> applyToUnbaked(PropertyDispatch<UnbakedMutator> dispatch) {
         return dispatch.getEntries()
            .entrySet()
            .stream()
            .map(entry -> new MultiVariantGenerator.Entry(this.properties.extend(entry.getKey()), this.variant.with(entry.getValue())));
      }

      public Stream<MultiVariantGenerator.Entry> applyToUnbaked(UnbakedMutator mutator) {
         return Stream.of(new MultiVariantGenerator.Entry(this.properties, this.variant.with(mutator)));
      }
   }
}
