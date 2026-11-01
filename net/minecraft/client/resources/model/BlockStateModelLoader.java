package net.minecraft.client.resources.model;

import com.google.common.base.Splitter;
import com.google.common.collect.Sets;
import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.Collection;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.block.model.BlockModelDefinition;
import net.minecraft.client.renderer.block.model.multipart.MultiPart;
import net.minecraft.client.renderer.block.model.multipart.Selector;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class BlockStateModelLoader {
   private static final Logger LOGGER = LogUtils.getLogger();
   static final int SINGLETON_MODEL_GROUP = -1;
   private static final int INVISIBLE_MODEL_GROUP = 0;
   public static final FileToIdConverter BLOCKSTATE_LISTER = FileToIdConverter.json("blockstates");
   private static final Splitter COMMA_SPLITTER = Splitter.on(',');
   private static final Splitter EQUAL_SPLITTER = Splitter.on('=').limit(2);
   private static final StateDefinition<Block, BlockState> ITEM_FRAME_FAKE_DEFINITION = new StateDefinition.Builder<Block, BlockState>(Blocks.AIR)
      .add(BooleanProperty.create("map"))
      .create(Block::defaultBlockState, BlockState::new);
   private static final Map<ResourceLocation, StateDefinition<Block, BlockState>> STATIC_DEFINITIONS = Map.of(
      ResourceLocation.withDefaultNamespace("item_frame"),
      ITEM_FRAME_FAKE_DEFINITION,
      ResourceLocation.withDefaultNamespace("glow_item_frame"),
      ITEM_FRAME_FAKE_DEFINITION
   );
   private final Map<ResourceLocation, List<BlockStateModelLoader.LoadedJson>> blockStateResources;
   private final ProfilerFiller profiler;
   private final BlockColors blockColors;
   private final BiConsumer<ModelResourceLocation, UnbakedModel> discoveredModelOutput;
   private int nextModelGroup = 1;
   private final Object2IntMap<BlockState> modelGroups = Util.make(new Object2IntOpenHashMap(), p_352094_ -> p_352094_.defaultReturnValue(-1));
   private final BlockStateModelLoader.LoadedModel missingModel;
   private final BlockModelDefinition.Context context = new BlockModelDefinition.Context();

   public BlockStateModelLoader(
      Map<ResourceLocation, List<BlockStateModelLoader.LoadedJson>> p_352185_,
      ProfilerFiller p_352252_,
      UnbakedModel p_352193_,
      BlockColors p_352120_,
      BiConsumer<ModelResourceLocation, UnbakedModel> p_352414_
   ) {
      this.blockStateResources = p_352185_;
      this.profiler = p_352252_;
      this.blockColors = p_352120_;
      this.discoveredModelOutput = p_352414_;
      BlockStateModelLoader.ModelGroupKey $$5 = new BlockStateModelLoader.ModelGroupKey(List.of(p_352193_), List.of());
      this.missingModel = new BlockStateModelLoader.LoadedModel(p_352193_, () -> $$5);
   }

   public void loadAllBlockStates() {
      this.profiler.push("static_definitions");
      STATIC_DEFINITIONS.forEach(this::loadBlockStateDefinitions);
      this.profiler.popPush("blocks");

      for (Block $$0 : BuiltInRegistries.BLOCK) {
         this.loadBlockStateDefinitions($$0.builtInRegistryHolder().key().location(), $$0.getStateDefinition());
      }

      this.profiler.pop();
   }

   private void loadBlockStateDefinitions(ResourceLocation p_352059_, StateDefinition<Block, BlockState> p_352064_) {
      this.context.setDefinition(p_352064_);
      List<Property<?>> $$2 = List.copyOf(this.blockColors.getColoringProperties(p_352064_.getOwner()));
      List<BlockState> $$3 = p_352064_.getPossibleStates();
      Map<ModelResourceLocation, BlockState> $$4 = new HashMap<>();
      $$3.forEach(p_352345_ -> $$4.put(BlockModelShaper.stateToModelLocation(p_352059_, p_352345_), p_352345_));
      Map<BlockState, BlockStateModelLoader.LoadedModel> $$5 = new HashMap<>();
      ResourceLocation $$6 = BLOCKSTATE_LISTER.idToFile(p_352059_);

      try {
         for (BlockStateModelLoader.LoadedJson $$7 : this.blockStateResources.getOrDefault($$6, List.of())) {
            BlockModelDefinition $$8 = $$7.parse(p_352059_, this.context);
            Map<BlockState, BlockStateModelLoader.LoadedModel> $$9 = new IdentityHashMap<>();
            MultiPart $$10;
            if ($$8.isMultiPart()) {
               $$10 = $$8.getMultiPart();
               $$3.forEach(
                  p_352430_ -> $$9.put(
                     p_352430_, new BlockStateModelLoader.LoadedModel($$10, () -> BlockStateModelLoader.ModelGroupKey.create(p_352430_, $$10, $$2))
                  )
               );
            } else {
               $$10 = null;
            }

            $$8.getVariants()
               .forEach(
                  (p_352346_, p_352105_) -> {
                     try {
                        $$3.stream()
                           .filter(predicate(p_352064_, p_352346_))
                           .forEach(
                              p_352306_ -> {
                                 BlockStateModelLoader.LoadedModel $$6x = $$9.put(
                                    p_352306_,
                                    new BlockStateModelLoader.LoadedModel(
                                       p_352105_, () -> BlockStateModelLoader.ModelGroupKey.create(p_352306_, p_352105_, $$2)
                                    )
                                 );
                                 if ($$6x != null && $$6x.model != $$10) {
                                    $$9.put(p_352306_, this.missingModel);
                                    throw new RuntimeException(
                                       "Overlapping definition with: "
                                          + $$8.getVariants()
                                             .entrySet()
                                             .stream()
                                             .filter(p_352224_ -> p_352224_.getValue() == $$6x.model)
                                             .findFirst()
                                             .get()
                                             .getKey()
                                    );
                                 }
                              }
                           );
                     } catch (Exception var12x) {
                        LOGGER.warn(
                           "Exception loading blockstate definition: '{}' in resourcepack: '{}' for variant: '{}': {}",
                           new Object[]{$$6, $$7.source, p_352346_, var12x.getMessage()}
                        );
                     }
                  }
               );
            $$5.putAll($$9);
         }
      } catch (BlockStateModelLoader.BlockStateDefinitionException var18) {
         LOGGER.warn("{}", var18.getMessage());
      } catch (Exception var19) {
         LOGGER.warn("Exception loading blockstate definition: '{}'", $$6, var19);
      } finally {
         Map<BlockStateModelLoader.ModelGroupKey, Set<BlockState>> $$17 = new HashMap<>();
         $$4.forEach((p_352171_, p_352134_) -> {
            BlockStateModelLoader.LoadedModel $$5x = $$5.get(p_352134_);
            if ($$5x == null) {
               LOGGER.warn("Exception loading blockstate definition: '{}' missing model for variant: '{}'", $$6, p_352171_);
               $$5x = this.missingModel;
            }

            this.discoveredModelOutput.accept(p_352171_, $$5x.model);

            try {
               BlockStateModelLoader.ModelGroupKey $$6x = $$5x.key().get();
               $$17.computeIfAbsent($$6x, p_352112_ -> Sets.newIdentityHashSet()).add(p_352134_);
            } catch (Exception var8) {
               LOGGER.warn("Exception evaluating model definition: '{}'", p_352171_, var8);
            }
         });
         $$17.forEach((p_352263_, p_352463_) -> {
            Iterator<BlockState> $$2x = p_352463_.iterator();

            while ($$2x.hasNext()) {
               BlockState $$3x = $$2x.next();
               if ($$3x.getRenderShape() != RenderShape.MODEL) {
                  $$2x.remove();
                  this.modelGroups.put($$3x, 0);
               }
            }

            if (p_352463_.size() > 1) {
               this.registerModelGroup(p_352463_);
            }
         });
      }
   }

   private static Predicate<BlockState> predicate(StateDefinition<Block, BlockState> p_352099_, String p_352078_) {
      Map<Property<?>, Comparable<?>> $$2 = new HashMap<>();

      for (String $$3 : COMMA_SPLITTER.split(p_352078_)) {
         Iterator<String> $$4 = EQUAL_SPLITTER.split($$3).iterator();
         if ($$4.hasNext()) {
            String $$5 = $$4.next();
            Property<?> $$6 = p_352099_.getProperty($$5);
            if ($$6 != null && $$4.hasNext()) {
               String $$7 = $$4.next();
               Comparable<?> $$8 = getValueHelper((Property<Comparable<?>>)$$6, $$7);
               if ($$8 == null) {
                  throw new RuntimeException("Unknown value: '" + $$7 + "' for blockstate property: '" + $$5 + "' " + $$6.getPossibleValues());
               }

               $$2.put($$6, $$8);
            } else if (!$$5.isEmpty()) {
               throw new RuntimeException("Unknown blockstate property: '" + $$5 + "'");
            }
         }
      }

      Block $$9 = p_352099_.getOwner();
      return p_352368_ -> {
         if (p_352368_ != null && p_352368_.is($$9)) {
            for (Entry<Property<?>, Comparable<?>> $$3x : $$2.entrySet()) {
               if (!Objects.equals(p_352368_.getValue($$3x.getKey()), $$3x.getValue())) {
                  return false;
               }
            }

            return true;
         } else {
            return false;
         }
      };
   }

   @Nullable
   static <T extends Comparable<T>> T getValueHelper(Property<T> p_352301_, String p_352273_) {
      return p_352301_.getValue(p_352273_).orElse(null);
   }

   private void registerModelGroup(Iterable<BlockState> p_352382_) {
      int $$1 = this.nextModelGroup++;
      p_352382_.forEach(p_352170_ -> this.modelGroups.put(p_352170_, $$1));
   }

   public Object2IntMap<BlockState> getModelGroups() {
      return this.modelGroups;
   }

   @OnlyIn(Dist.CLIENT)
   static class BlockStateDefinitionException extends RuntimeException {
      public BlockStateDefinitionException(String p_352118_) {
         super(p_352118_);
      }
   }

   @OnlyIn(Dist.CLIENT)
   public record LoadedJson(String source, JsonElement data) {
      BlockModelDefinition parse(ResourceLocation p_352102_, BlockModelDefinition.Context p_352217_) {
         try {
            return BlockModelDefinition.fromJsonElement(p_352217_, this.data);
         } catch (Exception var4) {
            throw new BlockStateModelLoader.BlockStateDefinitionException(
               String.format(Locale.ROOT, "Exception loading blockstate definition: '%s' in resourcepack: '%s': %s", p_352102_, this.source, var4.getMessage())
            );
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   record LoadedModel(UnbakedModel model, Supplier<BlockStateModelLoader.ModelGroupKey> key) {
   }

   @OnlyIn(Dist.CLIENT)
   record ModelGroupKey(List<UnbakedModel> models, List<Object> coloringValues) {
      public static BlockStateModelLoader.ModelGroupKey create(BlockState p_352381_, MultiPart p_352248_, Collection<Property<?>> p_352227_) {
         StateDefinition<Block, BlockState> $$3 = p_352381_.getBlock().getStateDefinition();
         List<UnbakedModel> $$4 = p_352248_.getSelectors()
            .stream()
            .filter(p_352283_ -> p_352283_.getPredicate($$3).test(p_352381_))
            .map(Selector::getVariant)
            .collect(Collectors.toUnmodifiableList());
         List<Object> $$5 = getColoringValues(p_352381_, p_352227_);
         return new BlockStateModelLoader.ModelGroupKey($$4, $$5);
      }

      public static BlockStateModelLoader.ModelGroupKey create(BlockState p_352440_, UnbakedModel p_352383_, Collection<Property<?>> p_352085_) {
         List<Object> $$3 = getColoringValues(p_352440_, p_352085_);
         return new BlockStateModelLoader.ModelGroupKey(List.of(p_352383_), $$3);
      }

      private static List<Object> getColoringValues(BlockState p_352214_, Collection<Property<?>> p_352157_) {
         return p_352157_.stream().map(p_352214_::getValue).collect(Collectors.toUnmodifiableList());
      }
   }
}
