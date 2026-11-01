package net.minecraft.client.data.models;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.mojang.datafixers.util.Pair;
import com.mojang.math.Quadrant;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.color.item.GrassColorSource;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.data.models.blockstates.BlockModelDefinitionGenerator;
import net.minecraft.client.data.models.blockstates.ConditionBuilder;
import net.minecraft.client.data.models.blockstates.MultiPartGenerator;
import net.minecraft.client.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.client.data.models.blockstates.PropertyDispatch;
import net.minecraft.client.data.models.model.ItemModelUtils;
import net.minecraft.client.data.models.model.ModelInstance;
import net.minecraft.client.data.models.model.ModelLocationUtils;
import net.minecraft.client.data.models.model.ModelTemplate;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.client.data.models.model.TextureSlot;
import net.minecraft.client.data.models.model.TexturedModel;
import net.minecraft.client.renderer.block.model.Variant;
import net.minecraft.client.renderer.block.model.VariantMutator;
import net.minecraft.client.renderer.block.model.multipart.CombinedCondition;
import net.minecraft.client.renderer.block.model.multipart.Condition;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.special.BannerSpecialRenderer;
import net.minecraft.client.renderer.special.BedSpecialRenderer;
import net.minecraft.client.renderer.special.ChestSpecialRenderer;
import net.minecraft.client.renderer.special.ConduitSpecialRenderer;
import net.minecraft.client.renderer.special.CopperGolemStatueSpecialRenderer;
import net.minecraft.client.renderer.special.DecoratedPotSpecialRenderer;
import net.minecraft.client.renderer.special.PlayerHeadSpecialRenderer;
import net.minecraft.client.renderer.special.ShulkerBoxSpecialRenderer;
import net.minecraft.client.renderer.special.SkullSpecialRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.core.Direction;
import net.minecraft.core.FrontAndTop;
import net.minecraft.data.BlockFamilies;
import net.minecraft.data.BlockFamily;
import net.minecraft.resources.Identifier;
import net.minecraft.util.StringRepresentable;
import net.minecraft.util.random.Weighted;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChiseledBookShelfBlock;
import net.minecraft.world.level.block.CopperGolemStatueBlock;
import net.minecraft.world.level.block.CrafterBlock;
import net.minecraft.world.level.block.CreakingHeartBlock;
import net.minecraft.world.level.block.DriedGhastBlock;
import net.minecraft.world.level.block.HangingMossBlock;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.MangrovePropaguleBlock;
import net.minecraft.world.level.block.MossyCarpetBlock;
import net.minecraft.world.level.block.MultifaceBlock;
import net.minecraft.world.level.block.PitcherCropBlock;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.SnifferEggBlock;
import net.minecraft.world.level.block.TestBlock;
import net.minecraft.world.level.block.VaultBlock;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.block.state.StateHolder;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BambooLeaves;
import net.minecraft.world.level.block.state.properties.BellAttachType;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.ComparatorMode;
import net.minecraft.world.level.block.state.properties.CreakingHeartState;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.DripstoneThickness;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.PistonType;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.level.block.state.properties.RedstoneSide;
import net.minecraft.world.level.block.state.properties.SculkSensorPhase;
import net.minecraft.world.level.block.state.properties.SideChainPart;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.block.state.properties.StairsShape;
import net.minecraft.world.level.block.state.properties.TestBlockMode;
import net.minecraft.world.level.block.state.properties.Tilt;
import net.minecraft.world.level.block.state.properties.WallSide;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class BlockModelGenerators {
   final Consumer<BlockModelDefinitionGenerator> blockStateOutput;
   final ItemModelOutput itemModelOutput;
   final BiConsumer<Identifier, ModelInstance> modelOutput;
   static final List<Block> NON_ORIENTABLE_TRAPDOOR = List.of(Blocks.OAK_TRAPDOOR, Blocks.DARK_OAK_TRAPDOOR, Blocks.IRON_TRAPDOOR);
   public static final VariantMutator NOP = variant -> variant;
   public static final VariantMutator UV_LOCK = VariantMutator.UV_LOCK.withValue(true);
   public static final VariantMutator X_ROT_90 = VariantMutator.X_ROT.withValue(Quadrant.R90);
   public static final VariantMutator X_ROT_180 = VariantMutator.X_ROT.withValue(Quadrant.R180);
   public static final VariantMutator X_ROT_270 = VariantMutator.X_ROT.withValue(Quadrant.R270);
   public static final VariantMutator Y_ROT_90 = VariantMutator.Y_ROT.withValue(Quadrant.R90);
   public static final VariantMutator Y_ROT_180 = VariantMutator.Y_ROT.withValue(Quadrant.R180);
   public static final VariantMutator Y_ROT_270 = VariantMutator.Y_ROT.withValue(Quadrant.R270);
   private static final Function<ConditionBuilder, ConditionBuilder> FLOWER_BED_MODEL_1_SEGMENT_CONDITION = conditionBuilder -> conditionBuilder;
   private static final Function<ConditionBuilder, ConditionBuilder> FLOWER_BED_MODEL_2_SEGMENT_CONDITION = conditionBuilder -> conditionBuilder.term(
      BlockStateProperties.FLOWER_AMOUNT, 2, 3, 4
   );
   private static final Function<ConditionBuilder, ConditionBuilder> FLOWER_BED_MODEL_3_SEGMENT_CONDITION = conditionBuilder -> conditionBuilder.term(
      BlockStateProperties.FLOWER_AMOUNT, 3, 4
   );
   private static final Function<ConditionBuilder, ConditionBuilder> FLOWER_BED_MODEL_4_SEGMENT_CONDITION = conditionBuilder -> conditionBuilder.term(
      BlockStateProperties.FLOWER_AMOUNT, 4
   );
   private static final Function<ConditionBuilder, ConditionBuilder> LEAF_LITTER_MODEL_1_SEGMENT_CONDITION = conditionBuilder -> conditionBuilder.term(
      BlockStateProperties.SEGMENT_AMOUNT, 1
   );
   private static final Function<ConditionBuilder, ConditionBuilder> LEAF_LITTER_MODEL_2_SEGMENT_CONDITION = conditionBuilder -> conditionBuilder.term(
      BlockStateProperties.SEGMENT_AMOUNT, 2, 3
   );
   private static final Function<ConditionBuilder, ConditionBuilder> LEAF_LITTER_MODEL_3_SEGMENT_CONDITION = conditionBuilder -> conditionBuilder.term(
      BlockStateProperties.SEGMENT_AMOUNT, 3
   );
   private static final Function<ConditionBuilder, ConditionBuilder> LEAF_LITTER_MODEL_4_SEGMENT_CONDITION = conditionBuilder -> conditionBuilder.term(
      BlockStateProperties.SEGMENT_AMOUNT, 4
   );
   static final Map<Block, BlockModelGenerators.BlockStateGeneratorSupplier> FULL_BLOCK_MODEL_CUSTOM_GENERATORS = Map.of(
      Blocks.STONE,
      BlockModelGenerators::createMirroredCubeGenerator,
      Blocks.DEEPSLATE,
      BlockModelGenerators::createMirroredColumnGenerator,
      Blocks.MUD_BRICKS,
      BlockModelGenerators::createNorthWestMirroredCubeGenerator
   );
   private static final PropertyDispatch<VariantMutator> ROTATION_FACING = PropertyDispatch.modify(BlockStateProperties.FACING)
      .select(Direction.DOWN, X_ROT_90)
      .select(Direction.UP, X_ROT_270)
      .select(Direction.NORTH, NOP)
      .select(Direction.SOUTH, Y_ROT_180)
      .select(Direction.WEST, Y_ROT_270)
      .select(Direction.EAST, Y_ROT_90);
   private static final PropertyDispatch<VariantMutator> ROTATIONS_COLUMN_WITH_FACING = PropertyDispatch.modify(BlockStateProperties.FACING)
      .select(Direction.DOWN, X_ROT_180)
      .select(Direction.UP, NOP)
      .select(Direction.NORTH, X_ROT_90)
      .select(Direction.SOUTH, X_ROT_90.then(Y_ROT_180))
      .select(Direction.WEST, X_ROT_90.then(Y_ROT_270))
      .select(Direction.EAST, X_ROT_90.then(Y_ROT_90));
   private static final PropertyDispatch<VariantMutator> ROTATION_TORCH = PropertyDispatch.modify(BlockStateProperties.HORIZONTAL_FACING)
      .select(Direction.EAST, NOP)
      .select(Direction.SOUTH, Y_ROT_90)
      .select(Direction.WEST, Y_ROT_180)
      .select(Direction.NORTH, Y_ROT_270);
   private static final PropertyDispatch<VariantMutator> ROTATION_HORIZONTAL_FACING_ALT = PropertyDispatch.modify(BlockStateProperties.HORIZONTAL_FACING)
      .select(Direction.SOUTH, NOP)
      .select(Direction.WEST, Y_ROT_90)
      .select(Direction.NORTH, Y_ROT_180)
      .select(Direction.EAST, Y_ROT_270);
   private static final PropertyDispatch<VariantMutator> ROTATION_HORIZONTAL_FACING = PropertyDispatch.modify(BlockStateProperties.HORIZONTAL_FACING)
      .select(Direction.EAST, Y_ROT_90)
      .select(Direction.SOUTH, Y_ROT_180)
      .select(Direction.WEST, Y_ROT_270)
      .select(Direction.NORTH, NOP);
   static final Map<Block, TexturedModel> TEXTURED_MODELS = ImmutableMap.builder()
      .put(Blocks.SANDSTONE, TexturedModel.TOP_BOTTOM_WITH_WALL.get(Blocks.SANDSTONE))
      .put(Blocks.RED_SANDSTONE, TexturedModel.TOP_BOTTOM_WITH_WALL.get(Blocks.RED_SANDSTONE))
      .put(Blocks.SMOOTH_SANDSTONE, TexturedModel.createAllSame(TextureMapping.getBlockTexture(Blocks.SANDSTONE, "_top")))
      .put(Blocks.SMOOTH_RED_SANDSTONE, TexturedModel.createAllSame(TextureMapping.getBlockTexture(Blocks.RED_SANDSTONE, "_top")))
      .put(
         Blocks.CUT_SANDSTONE,
         TexturedModel.COLUMN
            .get(Blocks.SANDSTONE)
            .updateTextures(textureMapping -> textureMapping.put(TextureSlot.SIDE, TextureMapping.getBlockTexture(Blocks.CUT_SANDSTONE)))
      )
      .put(
         Blocks.CUT_RED_SANDSTONE,
         TexturedModel.COLUMN
            .get(Blocks.RED_SANDSTONE)
            .updateTextures(textureMapping -> textureMapping.put(TextureSlot.SIDE, TextureMapping.getBlockTexture(Blocks.CUT_RED_SANDSTONE)))
      )
      .put(Blocks.QUARTZ_BLOCK, TexturedModel.COLUMN.get(Blocks.QUARTZ_BLOCK))
      .put(Blocks.SMOOTH_QUARTZ, TexturedModel.createAllSame(TextureMapping.getBlockTexture(Blocks.QUARTZ_BLOCK, "_bottom")))
      .put(Blocks.BLACKSTONE, TexturedModel.COLUMN_WITH_WALL.get(Blocks.BLACKSTONE))
      .put(Blocks.DEEPSLATE, TexturedModel.COLUMN_WITH_WALL.get(Blocks.DEEPSLATE))
      .put(
         Blocks.CHISELED_QUARTZ_BLOCK,
         TexturedModel.COLUMN
            .get(Blocks.CHISELED_QUARTZ_BLOCK)
            .updateTextures(textureMapping -> textureMapping.put(TextureSlot.SIDE, TextureMapping.getBlockTexture(Blocks.CHISELED_QUARTZ_BLOCK)))
      )
      .put(Blocks.CHISELED_SANDSTONE, TexturedModel.COLUMN.get(Blocks.CHISELED_SANDSTONE).updateTextures(textureMapping -> {
         textureMapping.put(TextureSlot.END, TextureMapping.getBlockTexture(Blocks.SANDSTONE, "_top"));
         textureMapping.put(TextureSlot.SIDE, TextureMapping.getBlockTexture(Blocks.CHISELED_SANDSTONE));
      }))
      .put(Blocks.CHISELED_RED_SANDSTONE, TexturedModel.COLUMN.get(Blocks.CHISELED_RED_SANDSTONE).updateTextures(textureMapping -> {
         textureMapping.put(TextureSlot.END, TextureMapping.getBlockTexture(Blocks.RED_SANDSTONE, "_top"));
         textureMapping.put(TextureSlot.SIDE, TextureMapping.getBlockTexture(Blocks.CHISELED_RED_SANDSTONE));
      }))
      .put(Blocks.CHISELED_TUFF_BRICKS, TexturedModel.COLUMN_WITH_WALL.get(Blocks.CHISELED_TUFF_BRICKS))
      .put(Blocks.CHISELED_TUFF, TexturedModel.COLUMN_WITH_WALL.get(Blocks.CHISELED_TUFF))
      .build();
   static final Map<BlockFamily.Variant, BiConsumer<BlockModelGenerators.BlockFamilyProvider, Block>> SHAPE_CONSUMERS = ImmutableMap.builder()
      .put(BlockFamily.Variant.BUTTON, BlockModelGenerators.BlockFamilyProvider::button)
      .put(BlockFamily.Variant.DOOR, BlockModelGenerators.BlockFamilyProvider::door)
      .put(BlockFamily.Variant.CHISELED, BlockModelGenerators.BlockFamilyProvider::fullBlockVariant)
      .put(BlockFamily.Variant.CRACKED, BlockModelGenerators.BlockFamilyProvider::fullBlockVariant)
      .put(BlockFamily.Variant.CUSTOM_FENCE, BlockModelGenerators.BlockFamilyProvider::customFence)
      .put(BlockFamily.Variant.FENCE, BlockModelGenerators.BlockFamilyProvider::fence)
      .put(BlockFamily.Variant.CUSTOM_FENCE_GATE, BlockModelGenerators.BlockFamilyProvider::customFenceGate)
      .put(BlockFamily.Variant.FENCE_GATE, BlockModelGenerators.BlockFamilyProvider::fenceGate)
      .put(BlockFamily.Variant.SIGN, BlockModelGenerators.BlockFamilyProvider::sign)
      .put(BlockFamily.Variant.SLAB, BlockModelGenerators.BlockFamilyProvider::slab)
      .put(BlockFamily.Variant.STAIRS, BlockModelGenerators.BlockFamilyProvider::stairs)
      .put(BlockFamily.Variant.PRESSURE_PLATE, BlockModelGenerators.BlockFamilyProvider::pressurePlate)
      .put(BlockFamily.Variant.TRAPDOOR, BlockModelGenerators.BlockFamilyProvider::trapdoor)
      .put(BlockFamily.Variant.WALL, BlockModelGenerators.BlockFamilyProvider::wall)
      .build();
   private static final Map<Direction, VariantMutator> MULTIFACE_GENERATOR = ImmutableMap.of(
      Direction.NORTH,
      NOP,
      Direction.EAST,
      Y_ROT_90.then(UV_LOCK),
      Direction.SOUTH,
      Y_ROT_180.then(UV_LOCK),
      Direction.WEST,
      Y_ROT_270.then(UV_LOCK),
      Direction.UP,
      X_ROT_270.then(UV_LOCK),
      Direction.DOWN,
      X_ROT_90.then(UV_LOCK)
   );
   private static final Map<BlockModelGenerators.BookSlotModelCacheKey, Identifier> CHISELED_BOOKSHELF_SLOT_MODEL_CACHE = new HashMap<>();

   static Variant plainModel(Identifier modelLocation) {
      return new Variant(modelLocation);
   }

   static MultiVariant variant(Variant variant) {
      return new MultiVariant(WeightedList.of(variant));
   }

   private static MultiVariant variants(Variant... variants) {
      return new MultiVariant(WeightedList.of(Arrays.stream(variants).map(variant -> new Weighted<>(variant, 1)).toList()));
   }

   static MultiVariant plainVariant(Identifier id) {
      return variant(plainModel(id));
   }

   private static ConditionBuilder condition() {
      return new ConditionBuilder();
   }

   @SafeVarargs
   private static <T extends Enum<T> & StringRepresentable> ConditionBuilder condition(EnumProperty<T> property, T value, T... otherValues) {
      return condition().term(property, value, otherValues);
   }

   private static ConditionBuilder condition(BooleanProperty property, boolean value) {
      return condition().term(property, value);
   }

   private static Condition or(ConditionBuilder... conditions) {
      return new CombinedCondition(CombinedCondition.Operation.OR, Stream.of(conditions).map(ConditionBuilder::build).toList());
   }

   private static Condition and(ConditionBuilder... conditions) {
      return new CombinedCondition(CombinedCondition.Operation.AND, Stream.of(conditions).map(ConditionBuilder::build).toList());
   }

   private static BlockModelDefinitionGenerator createMirroredCubeGenerator(
      Block block, Variant variant, TextureMapping textureMapping, BiConsumer<Identifier, ModelInstance> modelOutput
   ) {
      Variant variant2 = plainModel(ModelTemplates.CUBE_MIRRORED_ALL.create(block, textureMapping, modelOutput));
      return MultiVariantGenerator.dispatch(block, createRotatedVariants(variant, variant2));
   }

   private static BlockModelDefinitionGenerator createNorthWestMirroredCubeGenerator(
      Block block, Variant variant, TextureMapping textureMapping, BiConsumer<Identifier, ModelInstance> modelOutput
   ) {
      MultiVariant multiVariant = plainVariant(ModelTemplates.CUBE_NORTH_WEST_MIRRORED_ALL.create(block, textureMapping, modelOutput));
      return createSimpleBlock(block, multiVariant);
   }

   private static BlockModelDefinitionGenerator createMirroredColumnGenerator(
      Block block, Variant variant, TextureMapping textureMapping, BiConsumer<Identifier, ModelInstance> modelOutput
   ) {
      Variant variant2 = plainModel(ModelTemplates.CUBE_COLUMN_MIRRORED.create(block, textureMapping, modelOutput));
      return MultiVariantGenerator.dispatch(block, createRotatedVariants(variant, variant2)).with(createRotatedPillar());
   }

   public BlockModelGenerators(
      Consumer<BlockModelDefinitionGenerator> blockStateOutput, ItemModelOutput itemModelOutput, BiConsumer<Identifier, ModelInstance> modelOutput
   ) {
      this.blockStateOutput = blockStateOutput;
      this.itemModelOutput = itemModelOutput;
      this.modelOutput = modelOutput;
   }

   private void registerSimpleItemModel(Item item, Identifier model) {
      this.itemModelOutput.accept(item, ItemModelUtils.plainModel(model));
   }

   void registerSimpleItemModel(Block block, Identifier model) {
      this.itemModelOutput.accept(block.asItem(), ItemModelUtils.plainModel(model));
   }

   private void registerSimpleTintedItemModel(Block block, Identifier model, ItemTintSource tintSource) {
      this.itemModelOutput.accept(block.asItem(), ItemModelUtils.tintedModel(model, tintSource));
   }

   private Identifier createFlatItemModel(Item item) {
      return ModelTemplates.FLAT_ITEM.create(ModelLocationUtils.getModelLocation(item), TextureMapping.layer0(item), this.modelOutput);
   }

   Identifier createFlatItemModelWithBlockTexture(Item item, Block block) {
      return ModelTemplates.FLAT_ITEM.create(ModelLocationUtils.getModelLocation(item), TextureMapping.layer0(block), this.modelOutput);
   }

   private Identifier createFlatItemModelWithBlockTexture(Item item, Block block, String suffix) {
      return ModelTemplates.FLAT_ITEM
         .create(ModelLocationUtils.getModelLocation(item), TextureMapping.layer0(TextureMapping.getBlockTexture(block, suffix)), this.modelOutput);
   }

   Identifier createFlatItemModelWithBlockTextureAndOverlay(Item item, Block block, String suffix) {
      Identifier identifier = TextureMapping.getBlockTexture(block);
      Identifier identifier2 = TextureMapping.getBlockTexture(block, suffix);
      return ModelTemplates.TWO_LAYERED_ITEM
         .create(ModelLocationUtils.getModelLocation(item), TextureMapping.layered(identifier, identifier2), this.modelOutput);
   }

   void registerSimpleFlatItemModel(Item item) {
      this.registerSimpleItemModel(item, this.createFlatItemModel(item));
   }

   private void registerSimpleFlatItemModel(Block block) {
      Item item = block.asItem();
      if (item != Items.AIR) {
         this.registerSimpleItemModel(item, this.createFlatItemModelWithBlockTexture(item, block));
      }
   }

   private void registerSimpleFlatItemModel(Block block, String suffix) {
      Item item = block.asItem();
      if (item != Items.AIR) {
         this.registerSimpleItemModel(item, this.createFlatItemModelWithBlockTexture(item, block, suffix));
      }
   }

   private void registerTwoLayerFlatItemModel(Block block, String suffix) {
      Item item = block.asItem();
      if (item != Items.AIR) {
         Identifier identifier = this.createFlatItemModelWithBlockTextureAndOverlay(item, block, suffix);
         this.registerSimpleItemModel(item, identifier);
      }
   }

   private static MultiVariant createRotatedVariants(Variant variant) {
      return variants(variant, variant.with(Y_ROT_90), variant.with(Y_ROT_180), variant.with(Y_ROT_270));
   }

   private static MultiVariant createRotatedVariants(Variant variant, Variant mirroredVariant) {
      return variants(variant, mirroredVariant, variant.with(Y_ROT_180), mirroredVariant.with(Y_ROT_180));
   }

   private static PropertyDispatch<MultiVariant> createBooleanModelDispatch(BooleanProperty property, MultiVariant onTrue, MultiVariant onFalse) {
      return PropertyDispatch.initial(property).select(true, onTrue).select(false, onFalse);
   }

   private void createRotatedMirroredVariantBlock(Block block) {
      Variant variant = plainModel(TexturedModel.CUBE.create(block, this.modelOutput));
      Variant variant2 = plainModel(TexturedModel.CUBE_MIRRORED.create(block, this.modelOutput));
      this.blockStateOutput.accept(MultiVariantGenerator.dispatch(block, createRotatedVariants(variant, variant2)));
   }

   private void createRotatedVariantBlock(Block block) {
      Variant variant = plainModel(TexturedModel.CUBE.create(block, this.modelOutput));
      this.blockStateOutput.accept(MultiVariantGenerator.dispatch(block, createRotatedVariants(variant)));
   }

   private void createBrushableBlock(Block block) {
      this.blockStateOutput
         .accept(
            MultiVariantGenerator.dispatch(block)
               .with(
                  PropertyDispatch.initial(BlockStateProperties.DUSTED)
                     .generate(
                        integer -> {
                           String string = "_" + integer;
                           Identifier identifier = TextureMapping.getBlockTexture(block, string);
                           Identifier identifier2 = ModelTemplates.CUBE_ALL
                              .createWithSuffix(block, string, new TextureMapping().put(TextureSlot.ALL, identifier), this.modelOutput);
                           return plainVariant(identifier2);
                        }
                     )
               )
         );
      this.registerSimpleItemModel(block, ModelLocationUtils.getModelLocation(block, "_0"));
   }

   static BlockModelDefinitionGenerator createButton(Block block, MultiVariant unpowered, MultiVariant powered) {
      return MultiVariantGenerator.dispatch(block)
         .with(PropertyDispatch.initial(BlockStateProperties.POWERED).select(false, unpowered).select(true, powered))
         .with(
            PropertyDispatch.modify(BlockStateProperties.ATTACH_FACE, BlockStateProperties.HORIZONTAL_FACING)
               .select(AttachFace.FLOOR, Direction.EAST, Y_ROT_90)
               .select(AttachFace.FLOOR, Direction.WEST, Y_ROT_270)
               .select(AttachFace.FLOOR, Direction.SOUTH, Y_ROT_180)
               .select(AttachFace.FLOOR, Direction.NORTH, NOP)
               .select(AttachFace.WALL, Direction.EAST, Y_ROT_90.then(X_ROT_90).then(UV_LOCK))
               .select(AttachFace.WALL, Direction.WEST, Y_ROT_270.then(X_ROT_90).then(UV_LOCK))
               .select(AttachFace.WALL, Direction.SOUTH, Y_ROT_180.then(X_ROT_90).then(UV_LOCK))
               .select(AttachFace.WALL, Direction.NORTH, X_ROT_90.then(UV_LOCK))
               .select(AttachFace.CEILING, Direction.EAST, Y_ROT_270.then(X_ROT_180))
               .select(AttachFace.CEILING, Direction.WEST, Y_ROT_90.then(X_ROT_180))
               .select(AttachFace.CEILING, Direction.SOUTH, X_ROT_180)
               .select(AttachFace.CEILING, Direction.NORTH, Y_ROT_180.then(X_ROT_180))
         );
   }

   private static BlockModelDefinitionGenerator createDoor(
      Block block,
      MultiVariant topLeft,
      MultiVariant topLeftOpen,
      MultiVariant topRight,
      MultiVariant topRightOpen,
      MultiVariant bottomLeft,
      MultiVariant bottomLeftOpen,
      MultiVariant bottomRight,
      MultiVariant bottomRightOpen
   ) {
      return MultiVariantGenerator.dispatch(block)
         .with(
            PropertyDispatch.initial(
                  BlockStateProperties.HORIZONTAL_FACING, BlockStateProperties.DOUBLE_BLOCK_HALF, BlockStateProperties.DOOR_HINGE, BlockStateProperties.OPEN
               )
               .select(Direction.EAST, DoubleBlockHalf.LOWER, DoorHingeSide.LEFT, false, topLeft)
               .select(Direction.SOUTH, DoubleBlockHalf.LOWER, DoorHingeSide.LEFT, false, topLeft.with(Y_ROT_90))
               .select(Direction.WEST, DoubleBlockHalf.LOWER, DoorHingeSide.LEFT, false, topLeft.with(Y_ROT_180))
               .select(Direction.NORTH, DoubleBlockHalf.LOWER, DoorHingeSide.LEFT, false, topLeft.with(Y_ROT_270))
               .select(Direction.EAST, DoubleBlockHalf.LOWER, DoorHingeSide.RIGHT, false, topRight)
               .select(Direction.SOUTH, DoubleBlockHalf.LOWER, DoorHingeSide.RIGHT, false, topRight.with(Y_ROT_90))
               .select(Direction.WEST, DoubleBlockHalf.LOWER, DoorHingeSide.RIGHT, false, topRight.with(Y_ROT_180))
               .select(Direction.NORTH, DoubleBlockHalf.LOWER, DoorHingeSide.RIGHT, false, topRight.with(Y_ROT_270))
               .select(Direction.EAST, DoubleBlockHalf.LOWER, DoorHingeSide.LEFT, true, topLeftOpen.with(Y_ROT_90))
               .select(Direction.SOUTH, DoubleBlockHalf.LOWER, DoorHingeSide.LEFT, true, topLeftOpen.with(Y_ROT_180))
               .select(Direction.WEST, DoubleBlockHalf.LOWER, DoorHingeSide.LEFT, true, topLeftOpen.with(Y_ROT_270))
               .select(Direction.NORTH, DoubleBlockHalf.LOWER, DoorHingeSide.LEFT, true, topLeftOpen)
               .select(Direction.EAST, DoubleBlockHalf.LOWER, DoorHingeSide.RIGHT, true, topRightOpen.with(Y_ROT_270))
               .select(Direction.SOUTH, DoubleBlockHalf.LOWER, DoorHingeSide.RIGHT, true, topRightOpen)
               .select(Direction.WEST, DoubleBlockHalf.LOWER, DoorHingeSide.RIGHT, true, topRightOpen.with(Y_ROT_90))
               .select(Direction.NORTH, DoubleBlockHalf.LOWER, DoorHingeSide.RIGHT, true, topRightOpen.with(Y_ROT_180))
               .select(Direction.EAST, DoubleBlockHalf.UPPER, DoorHingeSide.LEFT, false, bottomLeft)
               .select(Direction.SOUTH, DoubleBlockHalf.UPPER, DoorHingeSide.LEFT, false, bottomLeft.with(Y_ROT_90))
               .select(Direction.WEST, DoubleBlockHalf.UPPER, DoorHingeSide.LEFT, false, bottomLeft.with(Y_ROT_180))
               .select(Direction.NORTH, DoubleBlockHalf.UPPER, DoorHingeSide.LEFT, false, bottomLeft.with(Y_ROT_270))
               .select(Direction.EAST, DoubleBlockHalf.UPPER, DoorHingeSide.RIGHT, false, bottomRight)
               .select(Direction.SOUTH, DoubleBlockHalf.UPPER, DoorHingeSide.RIGHT, false, bottomRight.with(Y_ROT_90))
               .select(Direction.WEST, DoubleBlockHalf.UPPER, DoorHingeSide.RIGHT, false, bottomRight.with(Y_ROT_180))
               .select(Direction.NORTH, DoubleBlockHalf.UPPER, DoorHingeSide.RIGHT, false, bottomRight.with(Y_ROT_270))
               .select(Direction.EAST, DoubleBlockHalf.UPPER, DoorHingeSide.LEFT, true, bottomLeftOpen.with(Y_ROT_90))
               .select(Direction.SOUTH, DoubleBlockHalf.UPPER, DoorHingeSide.LEFT, true, bottomLeftOpen.with(Y_ROT_180))
               .select(Direction.WEST, DoubleBlockHalf.UPPER, DoorHingeSide.LEFT, true, bottomLeftOpen.with(Y_ROT_270))
               .select(Direction.NORTH, DoubleBlockHalf.UPPER, DoorHingeSide.LEFT, true, bottomLeftOpen)
               .select(Direction.EAST, DoubleBlockHalf.UPPER, DoorHingeSide.RIGHT, true, bottomRightOpen.with(Y_ROT_270))
               .select(Direction.SOUTH, DoubleBlockHalf.UPPER, DoorHingeSide.RIGHT, true, bottomRightOpen)
               .select(Direction.WEST, DoubleBlockHalf.UPPER, DoorHingeSide.RIGHT, true, bottomRightOpen.with(Y_ROT_90))
               .select(Direction.NORTH, DoubleBlockHalf.UPPER, DoorHingeSide.RIGHT, true, bottomRightOpen.with(Y_ROT_180))
         );
   }

   static BlockModelDefinitionGenerator createCustomFence(
      Block block, MultiVariant post, MultiVariant north, MultiVariant east, MultiVariant south, MultiVariant west
   ) {
      return MultiPartGenerator.multiPart(block)
         .with(post)
         .with(condition().term(BlockStateProperties.NORTH, true), north)
         .with(condition().term(BlockStateProperties.EAST, true), east)
         .with(condition().term(BlockStateProperties.SOUTH, true), south)
         .with(condition().term(BlockStateProperties.WEST, true), west);
   }

   static BlockModelDefinitionGenerator createFence(Block block, MultiVariant post, MultiVariant side) {
      return MultiPartGenerator.multiPart(block)
         .with(post)
         .with(condition().term(BlockStateProperties.NORTH, true), side.with(UV_LOCK))
         .with(condition().term(BlockStateProperties.EAST, true), side.with(Y_ROT_90).with(UV_LOCK))
         .with(condition().term(BlockStateProperties.SOUTH, true), side.with(Y_ROT_180).with(UV_LOCK))
         .with(condition().term(BlockStateProperties.WEST, true), side.with(Y_ROT_270).with(UV_LOCK));
   }

   static BlockModelDefinitionGenerator createWall(Block block, MultiVariant post, MultiVariant lowSide, MultiVariant tallSide) {
      return MultiPartGenerator.multiPart(block)
         .with(condition().term(BlockStateProperties.UP, true), post)
         .with(condition().term(BlockStateProperties.NORTH_WALL, WallSide.LOW), lowSide.with(UV_LOCK))
         .with(condition().term(BlockStateProperties.EAST_WALL, WallSide.LOW), lowSide.with(Y_ROT_90).with(UV_LOCK))
         .with(condition().term(BlockStateProperties.SOUTH_WALL, WallSide.LOW), lowSide.with(Y_ROT_180).with(UV_LOCK))
         .with(condition().term(BlockStateProperties.WEST_WALL, WallSide.LOW), lowSide.with(Y_ROT_270).with(UV_LOCK))
         .with(condition().term(BlockStateProperties.NORTH_WALL, WallSide.TALL), tallSide.with(UV_LOCK))
         .with(condition().term(BlockStateProperties.EAST_WALL, WallSide.TALL), tallSide.with(Y_ROT_90).with(UV_LOCK))
         .with(condition().term(BlockStateProperties.SOUTH_WALL, WallSide.TALL), tallSide.with(Y_ROT_180).with(UV_LOCK))
         .with(condition().term(BlockStateProperties.WEST_WALL, WallSide.TALL), tallSide.with(Y_ROT_270).with(UV_LOCK));
   }

   static BlockModelDefinitionGenerator createFenceGate(
      Block block, MultiVariant open, MultiVariant closed, MultiVariant wallOpen, MultiVariant wallClosed, boolean uvLock
   ) {
      return MultiVariantGenerator.dispatch(block)
         .with(
            PropertyDispatch.initial(BlockStateProperties.IN_WALL, BlockStateProperties.OPEN)
               .select(false, false, closed)
               .select(true, false, wallClosed)
               .select(false, true, open)
               .select(true, true, wallOpen)
         )
         .with(uvLock ? UV_LOCK : NOP)
         .with(ROTATION_HORIZONTAL_FACING_ALT);
   }

   static BlockModelDefinitionGenerator createStairs(Block block, MultiVariant inner, MultiVariant straight, MultiVariant outer) {
      return MultiVariantGenerator.dispatch(block)
         .with(
            PropertyDispatch.initial(BlockStateProperties.HORIZONTAL_FACING, BlockStateProperties.HALF, BlockStateProperties.STAIRS_SHAPE)
               .select(Direction.EAST, Half.BOTTOM, StairsShape.STRAIGHT, straight)
               .select(Direction.WEST, Half.BOTTOM, StairsShape.STRAIGHT, straight.with(Y_ROT_180).with(UV_LOCK))
               .select(Direction.SOUTH, Half.BOTTOM, StairsShape.STRAIGHT, straight.with(Y_ROT_90).with(UV_LOCK))
               .select(Direction.NORTH, Half.BOTTOM, StairsShape.STRAIGHT, straight.with(Y_ROT_270).with(UV_LOCK))
               .select(Direction.EAST, Half.BOTTOM, StairsShape.OUTER_RIGHT, outer)
               .select(Direction.WEST, Half.BOTTOM, StairsShape.OUTER_RIGHT, outer.with(Y_ROT_180).with(UV_LOCK))
               .select(Direction.SOUTH, Half.BOTTOM, StairsShape.OUTER_RIGHT, outer.with(Y_ROT_90).with(UV_LOCK))
               .select(Direction.NORTH, Half.BOTTOM, StairsShape.OUTER_RIGHT, outer.with(Y_ROT_270).with(UV_LOCK))
               .select(Direction.EAST, Half.BOTTOM, StairsShape.OUTER_LEFT, outer.with(Y_ROT_270).with(UV_LOCK))
               .select(Direction.WEST, Half.BOTTOM, StairsShape.OUTER_LEFT, outer.with(Y_ROT_90).with(UV_LOCK))
               .select(Direction.SOUTH, Half.BOTTOM, StairsShape.OUTER_LEFT, outer)
               .select(Direction.NORTH, Half.BOTTOM, StairsShape.OUTER_LEFT, outer.with(Y_ROT_180).with(UV_LOCK))
               .select(Direction.EAST, Half.BOTTOM, StairsShape.INNER_RIGHT, inner)
               .select(Direction.WEST, Half.BOTTOM, StairsShape.INNER_RIGHT, inner.with(Y_ROT_180).with(UV_LOCK))
               .select(Direction.SOUTH, Half.BOTTOM, StairsShape.INNER_RIGHT, inner.with(Y_ROT_90).with(UV_LOCK))
               .select(Direction.NORTH, Half.BOTTOM, StairsShape.INNER_RIGHT, inner.with(Y_ROT_270).with(UV_LOCK))
               .select(Direction.EAST, Half.BOTTOM, StairsShape.INNER_LEFT, inner.with(Y_ROT_270).with(UV_LOCK))
               .select(Direction.WEST, Half.BOTTOM, StairsShape.INNER_LEFT, inner.with(Y_ROT_90).with(UV_LOCK))
               .select(Direction.SOUTH, Half.BOTTOM, StairsShape.INNER_LEFT, inner)
               .select(Direction.NORTH, Half.BOTTOM, StairsShape.INNER_LEFT, inner.with(Y_ROT_180).with(UV_LOCK))
               .select(Direction.EAST, Half.TOP, StairsShape.STRAIGHT, straight.with(X_ROT_180).with(UV_LOCK))
               .select(Direction.WEST, Half.TOP, StairsShape.STRAIGHT, straight.with(X_ROT_180).with(Y_ROT_180).with(UV_LOCK))
               .select(Direction.SOUTH, Half.TOP, StairsShape.STRAIGHT, straight.with(X_ROT_180).with(Y_ROT_90).with(UV_LOCK))
               .select(Direction.NORTH, Half.TOP, StairsShape.STRAIGHT, straight.with(X_ROT_180).with(Y_ROT_270).with(UV_LOCK))
               .select(Direction.EAST, Half.TOP, StairsShape.OUTER_RIGHT, outer.with(X_ROT_180).with(Y_ROT_90).with(UV_LOCK))
               .select(Direction.WEST, Half.TOP, StairsShape.OUTER_RIGHT, outer.with(X_ROT_180).with(Y_ROT_270).with(UV_LOCK))
               .select(Direction.SOUTH, Half.TOP, StairsShape.OUTER_RIGHT, outer.with(X_ROT_180).with(Y_ROT_180).with(UV_LOCK))
               .select(Direction.NORTH, Half.TOP, StairsShape.OUTER_RIGHT, outer.with(X_ROT_180).with(UV_LOCK))
               .select(Direction.EAST, Half.TOP, StairsShape.OUTER_LEFT, outer.with(X_ROT_180).with(UV_LOCK))
               .select(Direction.WEST, Half.TOP, StairsShape.OUTER_LEFT, outer.with(X_ROT_180).with(Y_ROT_180).with(UV_LOCK))
               .select(Direction.SOUTH, Half.TOP, StairsShape.OUTER_LEFT, outer.with(X_ROT_180).with(Y_ROT_90).with(UV_LOCK))
               .select(Direction.NORTH, Half.TOP, StairsShape.OUTER_LEFT, outer.with(X_ROT_180).with(Y_ROT_270).with(UV_LOCK))
               .select(Direction.EAST, Half.TOP, StairsShape.INNER_RIGHT, inner.with(X_ROT_180).with(Y_ROT_90).with(UV_LOCK))
               .select(Direction.WEST, Half.TOP, StairsShape.INNER_RIGHT, inner.with(X_ROT_180).with(Y_ROT_270).with(UV_LOCK))
               .select(Direction.SOUTH, Half.TOP, StairsShape.INNER_RIGHT, inner.with(X_ROT_180).with(Y_ROT_180).with(UV_LOCK))
               .select(Direction.NORTH, Half.TOP, StairsShape.INNER_RIGHT, inner.with(X_ROT_180).with(UV_LOCK))
               .select(Direction.EAST, Half.TOP, StairsShape.INNER_LEFT, inner.with(X_ROT_180).with(UV_LOCK))
               .select(Direction.WEST, Half.TOP, StairsShape.INNER_LEFT, inner.with(X_ROT_180).with(Y_ROT_180).with(UV_LOCK))
               .select(Direction.SOUTH, Half.TOP, StairsShape.INNER_LEFT, inner.with(X_ROT_180).with(Y_ROT_90).with(UV_LOCK))
               .select(Direction.NORTH, Half.TOP, StairsShape.INNER_LEFT, inner.with(X_ROT_180).with(Y_ROT_270).with(UV_LOCK))
         );
   }

   private static BlockModelDefinitionGenerator createOrientableTrapdoor(Block block, MultiVariant top, MultiVariant bottom, MultiVariant open) {
      return MultiVariantGenerator.dispatch(block)
         .with(
            PropertyDispatch.initial(BlockStateProperties.HORIZONTAL_FACING, BlockStateProperties.HALF, BlockStateProperties.OPEN)
               .select(Direction.NORTH, Half.BOTTOM, false, bottom)
               .select(Direction.SOUTH, Half.BOTTOM, false, bottom.with(Y_ROT_180))
               .select(Direction.EAST, Half.BOTTOM, false, bottom.with(Y_ROT_90))
               .select(Direction.WEST, Half.BOTTOM, false, bottom.with(Y_ROT_270))
               .select(Direction.NORTH, Half.TOP, false, top)
               .select(Direction.SOUTH, Half.TOP, false, top.with(Y_ROT_180))
               .select(Direction.EAST, Half.TOP, false, top.with(Y_ROT_90))
               .select(Direction.WEST, Half.TOP, false, top.with(Y_ROT_270))
               .select(Direction.NORTH, Half.BOTTOM, true, open)
               .select(Direction.SOUTH, Half.BOTTOM, true, open.with(Y_ROT_180))
               .select(Direction.EAST, Half.BOTTOM, true, open.with(Y_ROT_90))
               .select(Direction.WEST, Half.BOTTOM, true, open.with(Y_ROT_270))
               .select(Direction.NORTH, Half.TOP, true, open.with(X_ROT_180).with(Y_ROT_180))
               .select(Direction.SOUTH, Half.TOP, true, open.with(X_ROT_180))
               .select(Direction.EAST, Half.TOP, true, open.with(X_ROT_180).with(Y_ROT_270))
               .select(Direction.WEST, Half.TOP, true, open.with(X_ROT_180).with(Y_ROT_90))
         );
   }

   private static BlockModelDefinitionGenerator createTrapdoor(Block block, MultiVariant top, MultiVariant bottom, MultiVariant open) {
      return MultiVariantGenerator.dispatch(block)
         .with(
            PropertyDispatch.initial(BlockStateProperties.HORIZONTAL_FACING, BlockStateProperties.HALF, BlockStateProperties.OPEN)
               .select(Direction.NORTH, Half.BOTTOM, false, bottom)
               .select(Direction.SOUTH, Half.BOTTOM, false, bottom)
               .select(Direction.EAST, Half.BOTTOM, false, bottom)
               .select(Direction.WEST, Half.BOTTOM, false, bottom)
               .select(Direction.NORTH, Half.TOP, false, top)
               .select(Direction.SOUTH, Half.TOP, false, top)
               .select(Direction.EAST, Half.TOP, false, top)
               .select(Direction.WEST, Half.TOP, false, top)
               .select(Direction.NORTH, Half.BOTTOM, true, open)
               .select(Direction.SOUTH, Half.BOTTOM, true, open.with(Y_ROT_180))
               .select(Direction.EAST, Half.BOTTOM, true, open.with(Y_ROT_90))
               .select(Direction.WEST, Half.BOTTOM, true, open.with(Y_ROT_270))
               .select(Direction.NORTH, Half.TOP, true, open)
               .select(Direction.SOUTH, Half.TOP, true, open.with(Y_ROT_180))
               .select(Direction.EAST, Half.TOP, true, open.with(Y_ROT_90))
               .select(Direction.WEST, Half.TOP, true, open.with(Y_ROT_270))
         );
   }

   static MultiVariantGenerator createSimpleBlock(Block block, MultiVariant variants) {
      return MultiVariantGenerator.dispatch(block, variants);
   }

   private static PropertyDispatch<VariantMutator> createRotatedPillar() {
      return PropertyDispatch.modify(BlockStateProperties.AXIS)
         .select(Direction.Axis.Y, NOP)
         .select(Direction.Axis.Z, X_ROT_90)
         .select(Direction.Axis.X, X_ROT_90.then(Y_ROT_90));
   }

   static BlockModelDefinitionGenerator createPillarBlockUVLocked(Block block, TextureMapping textureMapping, BiConsumer<Identifier, ModelInstance> modelOutput) {
      MultiVariant multiVariant = plainVariant(ModelTemplates.CUBE_COLUMN_UV_LOCKED_X.create(block, textureMapping, modelOutput));
      MultiVariant multiVariant2 = plainVariant(ModelTemplates.CUBE_COLUMN_UV_LOCKED_Y.create(block, textureMapping, modelOutput));
      MultiVariant multiVariant3 = plainVariant(ModelTemplates.CUBE_COLUMN_UV_LOCKED_Z.create(block, textureMapping, modelOutput));
      return MultiVariantGenerator.dispatch(block)
         .with(
            PropertyDispatch.initial(BlockStateProperties.AXIS)
               .select(Direction.Axis.X, multiVariant)
               .select(Direction.Axis.Y, multiVariant2)
               .select(Direction.Axis.Z, multiVariant3)
         );
   }

   static BlockModelDefinitionGenerator createAxisAlignedPillarBlock(Block block, MultiVariant variants) {
      return MultiVariantGenerator.dispatch(block, variants).with(createRotatedPillar());
   }

   private void createAxisAlignedPillarBlockCustomModel(Block block, MultiVariant variants) {
      this.blockStateOutput.accept(createAxisAlignedPillarBlock(block, variants));
   }

   public void createAxisAlignedPillarBlock(Block axisAlignedPillarBlock, TexturedModel.Provider provider) {
      MultiVariant multiVariant = plainVariant(provider.create(axisAlignedPillarBlock, this.modelOutput));
      this.blockStateOutput.accept(createAxisAlignedPillarBlock(axisAlignedPillarBlock, multiVariant));
   }

   private void createHorizontallyRotatedBlock(Block horizontallyRotatedBlock, TexturedModel.Provider provider) {
      MultiVariant multiVariant = plainVariant(provider.create(horizontallyRotatedBlock, this.modelOutput));
      this.blockStateOutput.accept(MultiVariantGenerator.dispatch(horizontallyRotatedBlock, multiVariant).with(ROTATION_HORIZONTAL_FACING));
   }

   static BlockModelDefinitionGenerator createRotatedPillarWithHorizontalVariant(Block block, MultiVariant variants, MultiVariant horizontalVariants) {
      return MultiVariantGenerator.dispatch(block)
         .with(
            PropertyDispatch.initial(BlockStateProperties.AXIS)
               .select(Direction.Axis.Y, variants)
               .select(Direction.Axis.Z, horizontalVariants.with(X_ROT_90))
               .select(Direction.Axis.X, horizontalVariants.with(X_ROT_90).with(Y_ROT_90))
         );
   }

   private void createRotatedPillarWithHorizontalVariant(
      Block rotatedPillarBlock, TexturedModel.Provider modelProvider, TexturedModel.Provider horizontalModelProvider
   ) {
      MultiVariant multiVariant = plainVariant(modelProvider.create(rotatedPillarBlock, this.modelOutput));
      MultiVariant multiVariant2 = plainVariant(horizontalModelProvider.create(rotatedPillarBlock, this.modelOutput));
      this.blockStateOutput.accept(createRotatedPillarWithHorizontalVariant(rotatedPillarBlock, multiVariant, multiVariant2));
   }

   private void createCreakingHeart(Block block) {
      MultiVariant multiVariant = plainVariant(TexturedModel.COLUMN_ALT.create(block, this.modelOutput));
      MultiVariant multiVariant2 = plainVariant(TexturedModel.COLUMN_HORIZONTAL_ALT.create(block, this.modelOutput));
      MultiVariant multiVariant3 = plainVariant(this.createCreakingHeartModel(TexturedModel.COLUMN_ALT, block, "_awake"));
      MultiVariant multiVariant4 = plainVariant(this.createCreakingHeartModel(TexturedModel.COLUMN_HORIZONTAL_ALT, block, "_awake"));
      MultiVariant multiVariant5 = plainVariant(this.createCreakingHeartModel(TexturedModel.COLUMN_ALT, block, "_dormant"));
      MultiVariant multiVariant6 = plainVariant(this.createCreakingHeartModel(TexturedModel.COLUMN_HORIZONTAL_ALT, block, "_dormant"));
      this.blockStateOutput
         .accept(
            MultiVariantGenerator.dispatch(block)
               .with(
                  PropertyDispatch.initial(BlockStateProperties.AXIS, CreakingHeartBlock.STATE)
                     .select(Direction.Axis.Y, CreakingHeartState.UPROOTED, multiVariant)
                     .select(Direction.Axis.Z, CreakingHeartState.UPROOTED, multiVariant2.with(X_ROT_90))
                     .select(Direction.Axis.X, CreakingHeartState.UPROOTED, multiVariant2.with(X_ROT_90).with(Y_ROT_90))
                     .select(Direction.Axis.Y, CreakingHeartState.DORMANT, multiVariant5)
                     .select(Direction.Axis.Z, CreakingHeartState.DORMANT, multiVariant6.with(X_ROT_90))
                     .select(Direction.Axis.X, CreakingHeartState.DORMANT, multiVariant6.with(X_ROT_90).with(Y_ROT_90))
                     .select(Direction.Axis.Y, CreakingHeartState.AWAKE, multiVariant3)
                     .select(Direction.Axis.Z, CreakingHeartState.AWAKE, multiVariant4.with(X_ROT_90))
                     .select(Direction.Axis.X, CreakingHeartState.AWAKE, multiVariant4.with(X_ROT_90).with(Y_ROT_90))
               )
         );
   }

   private Identifier createCreakingHeartModel(TexturedModel.Provider modelProvider, Block block, String suffix) {
      return modelProvider.updateTexture(
            textureMapping -> textureMapping.put(TextureSlot.SIDE, TextureMapping.getBlockTexture(block, suffix))
               .put(TextureSlot.END, TextureMapping.getBlockTexture(block, "_top" + suffix))
         )
         .createWithSuffix(block, suffix, this.modelOutput);
   }

   private Identifier createSuffixedVariant(Block block, String suffix, ModelTemplate modelTemplate, Function<Identifier, TextureMapping> textureMappingGetter) {
      return modelTemplate.createWithSuffix(block, suffix, textureMappingGetter.apply(TextureMapping.getBlockTexture(block, suffix)), this.modelOutput);
   }

   static BlockModelDefinitionGenerator createPressurePlate(Block block, MultiVariant unpowered, MultiVariant powered) {
      return MultiVariantGenerator.dispatch(block).with(createBooleanModelDispatch(BlockStateProperties.POWERED, powered, unpowered));
   }

   static BlockModelDefinitionGenerator createSlab(Block block, MultiVariant bottom, MultiVariant top, MultiVariant doubleVariants) {
      return MultiVariantGenerator.dispatch(block)
         .with(
            PropertyDispatch.initial(BlockStateProperties.SLAB_TYPE)
               .select(SlabType.BOTTOM, bottom)
               .select(SlabType.TOP, top)
               .select(SlabType.DOUBLE, doubleVariants)
         );
   }

   public void createTrivialCube(Block block) {
      this.createTrivialBlock(block, TexturedModel.CUBE);
   }

   public void createTrivialBlock(Block block, TexturedModel.Provider provider) {
      this.blockStateOutput.accept(createSimpleBlock(block, plainVariant(provider.create(block, this.modelOutput))));
   }

   public void createTintedLeaves(Block block, TexturedModel.Provider provider, int tint) {
      Identifier identifier = provider.create(block, this.modelOutput);
      this.blockStateOutput.accept(createSimpleBlock(block, plainVariant(identifier)));
      this.registerSimpleTintedItemModel(block, identifier, ItemModelUtils.constantTint(tint));
   }

   private void createVine() {
      this.createMultifaceBlockStates(Blocks.VINE);
      Identifier identifier = this.createFlatItemModelWithBlockTexture(Items.VINE, Blocks.VINE);
      this.registerSimpleTintedItemModel(Blocks.VINE, identifier, ItemModelUtils.constantTint(-12012264));
   }

   private void createItemWithGrassTint(Block block) {
      Identifier identifier = this.createFlatItemModelWithBlockTexture(block.asItem(), block);
      this.registerSimpleTintedItemModel(block, identifier, new GrassColorSource());
   }

   private BlockModelGenerators.BlockFamilyProvider family(Block block) {
      TexturedModel texturedModel = TEXTURED_MODELS.getOrDefault(block, TexturedModel.CUBE.get(block));
      return new BlockModelGenerators.BlockFamilyProvider(texturedModel.getMapping()).fullBlock(block, texturedModel.getTemplate());
   }

   public void createHangingSign(Block particleBlock, Block hangingSignBlock, Block wallHangingSignBlock) {
      MultiVariant multiVariant = this.createParticleOnlyBlockModel(hangingSignBlock, particleBlock);
      this.blockStateOutput.accept(createSimpleBlock(hangingSignBlock, multiVariant));
      this.blockStateOutput.accept(createSimpleBlock(wallHangingSignBlock, multiVariant));
      this.registerSimpleFlatItemModel(hangingSignBlock.asItem());
   }

   void createDoor(Block doorBlock) {
      TextureMapping textureMapping = TextureMapping.door(doorBlock);
      MultiVariant multiVariant = plainVariant(ModelTemplates.DOOR_BOTTOM_LEFT.create(doorBlock, textureMapping, this.modelOutput));
      MultiVariant multiVariant2 = plainVariant(ModelTemplates.DOOR_BOTTOM_LEFT_OPEN.create(doorBlock, textureMapping, this.modelOutput));
      MultiVariant multiVariant3 = plainVariant(ModelTemplates.DOOR_BOTTOM_RIGHT.create(doorBlock, textureMapping, this.modelOutput));
      MultiVariant multiVariant4 = plainVariant(ModelTemplates.DOOR_BOTTOM_RIGHT_OPEN.create(doorBlock, textureMapping, this.modelOutput));
      MultiVariant multiVariant5 = plainVariant(ModelTemplates.DOOR_TOP_LEFT.create(doorBlock, textureMapping, this.modelOutput));
      MultiVariant multiVariant6 = plainVariant(ModelTemplates.DOOR_TOP_LEFT_OPEN.create(doorBlock, textureMapping, this.modelOutput));
      MultiVariant multiVariant7 = plainVariant(ModelTemplates.DOOR_TOP_RIGHT.create(doorBlock, textureMapping, this.modelOutput));
      MultiVariant multiVariant8 = plainVariant(ModelTemplates.DOOR_TOP_RIGHT_OPEN.create(doorBlock, textureMapping, this.modelOutput));
      this.registerSimpleFlatItemModel(doorBlock.asItem());
      this.blockStateOutput
         .accept(createDoor(doorBlock, multiVariant, multiVariant2, multiVariant3, multiVariant4, multiVariant5, multiVariant6, multiVariant7, multiVariant8));
   }

   private void copyDoorModel(Block doorBlock, Block sourceBlock) {
      MultiVariant multiVariant = plainVariant(ModelTemplates.DOOR_BOTTOM_LEFT.getDefaultModelLocation(doorBlock));
      MultiVariant multiVariant2 = plainVariant(ModelTemplates.DOOR_BOTTOM_LEFT_OPEN.getDefaultModelLocation(doorBlock));
      MultiVariant multiVariant3 = plainVariant(ModelTemplates.DOOR_BOTTOM_RIGHT.getDefaultModelLocation(doorBlock));
      MultiVariant multiVariant4 = plainVariant(ModelTemplates.DOOR_BOTTOM_RIGHT_OPEN.getDefaultModelLocation(doorBlock));
      MultiVariant multiVariant5 = plainVariant(ModelTemplates.DOOR_TOP_LEFT.getDefaultModelLocation(doorBlock));
      MultiVariant multiVariant6 = plainVariant(ModelTemplates.DOOR_TOP_LEFT_OPEN.getDefaultModelLocation(doorBlock));
      MultiVariant multiVariant7 = plainVariant(ModelTemplates.DOOR_TOP_RIGHT.getDefaultModelLocation(doorBlock));
      MultiVariant multiVariant8 = plainVariant(ModelTemplates.DOOR_TOP_RIGHT_OPEN.getDefaultModelLocation(doorBlock));
      this.itemModelOutput.copy(doorBlock.asItem(), sourceBlock.asItem());
      this.blockStateOutput
         .accept(createDoor(sourceBlock, multiVariant, multiVariant2, multiVariant3, multiVariant4, multiVariant5, multiVariant6, multiVariant7, multiVariant8));
   }

   void createOrientableTrapdoor(Block orientableTrapdoorBlock) {
      TextureMapping textureMapping = TextureMapping.defaultTexture(orientableTrapdoorBlock);
      MultiVariant multiVariant = plainVariant(ModelTemplates.ORIENTABLE_TRAPDOOR_TOP.create(orientableTrapdoorBlock, textureMapping, this.modelOutput));
      Identifier identifier = ModelTemplates.ORIENTABLE_TRAPDOOR_BOTTOM.create(orientableTrapdoorBlock, textureMapping, this.modelOutput);
      MultiVariant multiVariant2 = plainVariant(ModelTemplates.ORIENTABLE_TRAPDOOR_OPEN.create(orientableTrapdoorBlock, textureMapping, this.modelOutput));
      this.blockStateOutput.accept(createOrientableTrapdoor(orientableTrapdoorBlock, multiVariant, plainVariant(identifier), multiVariant2));
      this.registerSimpleItemModel(orientableTrapdoorBlock, identifier);
   }

   void createTrapdoor(Block trapdoorBlock) {
      TextureMapping textureMapping = TextureMapping.defaultTexture(trapdoorBlock);
      MultiVariant multiVariant = plainVariant(ModelTemplates.TRAPDOOR_TOP.create(trapdoorBlock, textureMapping, this.modelOutput));
      Identifier identifier = ModelTemplates.TRAPDOOR_BOTTOM.create(trapdoorBlock, textureMapping, this.modelOutput);
      MultiVariant multiVariant2 = plainVariant(ModelTemplates.TRAPDOOR_OPEN.create(trapdoorBlock, textureMapping, this.modelOutput));
      this.blockStateOutput.accept(createTrapdoor(trapdoorBlock, multiVariant, plainVariant(identifier), multiVariant2));
      this.registerSimpleItemModel(trapdoorBlock, identifier);
   }

   private void copyTrapdoorModel(Block trapdoorBlock, Block sourceBlock) {
      MultiVariant multiVariant = plainVariant(ModelTemplates.TRAPDOOR_TOP.getDefaultModelLocation(trapdoorBlock));
      MultiVariant multiVariant2 = plainVariant(ModelTemplates.TRAPDOOR_BOTTOM.getDefaultModelLocation(trapdoorBlock));
      MultiVariant multiVariant3 = plainVariant(ModelTemplates.TRAPDOOR_OPEN.getDefaultModelLocation(trapdoorBlock));
      this.itemModelOutput.copy(trapdoorBlock.asItem(), sourceBlock.asItem());
      this.blockStateOutput.accept(createTrapdoor(sourceBlock, multiVariant, multiVariant2, multiVariant3));
   }

   private void createBigDripLeafBlock() {
      MultiVariant multiVariant = plainVariant(ModelLocationUtils.getModelLocation(Blocks.BIG_DRIPLEAF));
      MultiVariant multiVariant2 = plainVariant(ModelLocationUtils.getModelLocation(Blocks.BIG_DRIPLEAF, "_partial_tilt"));
      MultiVariant multiVariant3 = plainVariant(ModelLocationUtils.getModelLocation(Blocks.BIG_DRIPLEAF, "_full_tilt"));
      this.blockStateOutput
         .accept(
            MultiVariantGenerator.dispatch(Blocks.BIG_DRIPLEAF)
               .with(
                  PropertyDispatch.initial(BlockStateProperties.TILT)
                     .select(Tilt.NONE, multiVariant)
                     .select(Tilt.UNSTABLE, multiVariant)
                     .select(Tilt.PARTIAL, multiVariant2)
                     .select(Tilt.FULL, multiVariant3)
               )
               .with(ROTATION_HORIZONTAL_FACING)
         );
   }

   private BlockModelGenerators.WoodProvider woodProvider(Block logBlock) {
      return new BlockModelGenerators.WoodProvider(TextureMapping.logColumn(logBlock));
   }

   private void createNonTemplateModelBlock(Block block) {
      this.createNonTemplateModelBlock(block, block);
   }

   private void createNonTemplateModelBlock(Block block, Block modelBlock) {
      this.blockStateOutput.accept(createSimpleBlock(block, plainVariant(ModelLocationUtils.getModelLocation(modelBlock))));
   }

   private void createCrossBlockWithDefaultItem(Block block, BlockModelGenerators.PlantType plantType) {
      this.registerSimpleItemModel(block.asItem(), plantType.createItemModel(this, block));
      this.createCrossBlock(block, plantType);
   }

   private void createCrossBlockWithDefaultItem(Block block, BlockModelGenerators.PlantType plantType, TextureMapping textureMapping) {
      this.registerSimpleFlatItemModel(block);
      this.createCrossBlock(block, plantType, textureMapping);
   }

   private void createCrossBlock(Block block, BlockModelGenerators.PlantType plantType) {
      TextureMapping textureMapping = plantType.getTextureMapping(block);
      this.createCrossBlock(block, plantType, textureMapping);
   }

   private void createCrossBlock(Block block, BlockModelGenerators.PlantType plantType, TextureMapping textureMapping) {
      MultiVariant multiVariant = plainVariant(plantType.getCross().create(block, textureMapping, this.modelOutput));
      this.blockStateOutput.accept(createSimpleBlock(block, multiVariant));
   }

   private void createCrossBlock(Block block, BlockModelGenerators.PlantType plantType, Property<Integer> ageProperty, int... possibleValues) {
      if (ageProperty.getPossibleValues().size() != possibleValues.length) {
         throw new IllegalArgumentException("missing values for property: " + ageProperty);
      } else {
         this.registerSimpleFlatItemModel(block.asItem());
         this.blockStateOutput.accept(MultiVariantGenerator.dispatch(block).with(PropertyDispatch.initial(ageProperty).generate(integer -> {
            String string = "_stage" + possibleValues[integer];
            TextureMapping textureMapping = TextureMapping.cross(TextureMapping.getBlockTexture(block, string));
            return plainVariant(plantType.getCross().createWithSuffix(block, string, textureMapping, this.modelOutput));
         })));
      }
   }

   private void createPlantWithDefaultItem(Block block, Block pottedBlock, BlockModelGenerators.PlantType plantType) {
      this.registerSimpleItemModel(block.asItem(), plantType.createItemModel(this, block));
      this.createPlant(block, pottedBlock, plantType);
   }

   private void createPlant(Block block, Block pottedBlock, BlockModelGenerators.PlantType plantType) {
      this.createCrossBlock(block, plantType);
      TextureMapping textureMapping = plantType.getPlantTextureMapping(block);
      MultiVariant multiVariant = plainVariant(plantType.getCrossPot().create(pottedBlock, textureMapping, this.modelOutput));
      this.blockStateOutput.accept(createSimpleBlock(pottedBlock, multiVariant));
   }

   private void createCoralFans(Block coralFanBlock, Block coralWallFanBlock) {
      TexturedModel texturedModel = TexturedModel.CORAL_FAN.get(coralFanBlock);
      MultiVariant multiVariant = plainVariant(texturedModel.create(coralFanBlock, this.modelOutput));
      this.blockStateOutput.accept(createSimpleBlock(coralFanBlock, multiVariant));
      MultiVariant multiVariant2 = plainVariant(ModelTemplates.CORAL_WALL_FAN.create(coralWallFanBlock, texturedModel.getMapping(), this.modelOutput));
      this.blockStateOutput.accept(MultiVariantGenerator.dispatch(coralWallFanBlock, multiVariant2).with(ROTATION_HORIZONTAL_FACING));
      this.registerSimpleFlatItemModel(coralFanBlock);
   }

   private void createStems(Block unattachedStemBlock, Block attachedStemBlock) {
      this.registerSimpleFlatItemModel(unattachedStemBlock.asItem());
      TextureMapping textureMapping = TextureMapping.stem(unattachedStemBlock);
      TextureMapping textureMapping2 = TextureMapping.attachedStem(unattachedStemBlock, attachedStemBlock);
      MultiVariant multiVariant = plainVariant(ModelTemplates.ATTACHED_STEM.create(attachedStemBlock, textureMapping2, this.modelOutput));
      this.blockStateOutput
         .accept(
            MultiVariantGenerator.dispatch(attachedStemBlock, multiVariant)
               .with(
                  PropertyDispatch.modify(BlockStateProperties.HORIZONTAL_FACING)
                     .select(Direction.WEST, NOP)
                     .select(Direction.SOUTH, Y_ROT_270)
                     .select(Direction.NORTH, Y_ROT_90)
                     .select(Direction.EAST, Y_ROT_180)
               )
         );
      this.blockStateOutput
         .accept(
            MultiVariantGenerator.dispatch(unattachedStemBlock)
               .with(
                  PropertyDispatch.initial(BlockStateProperties.AGE_7)
                     .generate(integer -> plainVariant(ModelTemplates.STEMS[integer].create(unattachedStemBlock, textureMapping, this.modelOutput)))
               )
         );
   }

   private void createPitcherPlant() {
      Block block = Blocks.PITCHER_PLANT;
      this.registerSimpleFlatItemModel(block.asItem());
      MultiVariant multiVariant = plainVariant(ModelLocationUtils.getModelLocation(block, "_top"));
      MultiVariant multiVariant2 = plainVariant(ModelLocationUtils.getModelLocation(block, "_bottom"));
      this.createDoubleBlock(block, multiVariant, multiVariant2);
   }

   private void createPitcherCrop() {
      Block block = Blocks.PITCHER_CROP;
      this.registerSimpleFlatItemModel(block.asItem());
      this.blockStateOutput
         .accept(
            MultiVariantGenerator.dispatch(block)
               .with(PropertyDispatch.initial(PitcherCropBlock.AGE, BlockStateProperties.DOUBLE_BLOCK_HALF).generate((integer, doubleBlockHalf) -> {
                  return switch (doubleBlockHalf) {
                     case UPPER -> plainVariant(ModelLocationUtils.getModelLocation(block, "_top_stage_" + integer));
                     case LOWER -> plainVariant(ModelLocationUtils.getModelLocation(block, "_bottom_stage_" + integer));
                  };
               }))
         );
   }

   private void createCoral(
      Block coralBlock,
      Block deadCoralBlock,
      Block coralFullBlock,
      Block deadCoralFullBlock,
      Block coralFanBlock,
      Block deadCoralFanBlock,
      Block coralWallFanBlock,
      Block deadCoralWallFanBlock
   ) {
      this.createCrossBlockWithDefaultItem(coralBlock, BlockModelGenerators.PlantType.NOT_TINTED);
      this.createCrossBlockWithDefaultItem(deadCoralBlock, BlockModelGenerators.PlantType.NOT_TINTED);
      this.createTrivialCube(coralFullBlock);
      this.createTrivialCube(deadCoralFullBlock);
      this.createCoralFans(coralFanBlock, coralWallFanBlock);
      this.createCoralFans(deadCoralFanBlock, deadCoralWallFanBlock);
   }

   private void createDoublePlant(Block block, BlockModelGenerators.PlantType plantType) {
      MultiVariant multiVariant = plainVariant(this.createSuffixedVariant(block, "_top", plantType.getCross(), TextureMapping::cross));
      MultiVariant multiVariant2 = plainVariant(this.createSuffixedVariant(block, "_bottom", plantType.getCross(), TextureMapping::cross));
      this.createDoubleBlock(block, multiVariant, multiVariant2);
   }

   private void createDoublePlantWithDefaultItem(Block block, BlockModelGenerators.PlantType plantType) {
      this.registerSimpleFlatItemModel(block, "_top");
      this.createDoublePlant(block, plantType);
   }

   private void createTintedDoublePlant(Block block) {
      Identifier identifier = this.createFlatItemModelWithBlockTexture(block.asItem(), block, "_top");
      this.registerSimpleTintedItemModel(block, identifier, new GrassColorSource());
      this.createDoublePlant(block, BlockModelGenerators.PlantType.TINTED);
   }

   private void createSunflower() {
      this.registerSimpleFlatItemModel(Blocks.SUNFLOWER, "_front");
      MultiVariant multiVariant = plainVariant(ModelLocationUtils.getModelLocation(Blocks.SUNFLOWER, "_top"));
      MultiVariant multiVariant2 = plainVariant(
         this.createSuffixedVariant(Blocks.SUNFLOWER, "_bottom", BlockModelGenerators.PlantType.NOT_TINTED.getCross(), TextureMapping::cross)
      );
      this.createDoubleBlock(Blocks.SUNFLOWER, multiVariant, multiVariant2);
   }

   private void createTallSeagrass() {
      MultiVariant multiVariant = plainVariant(
         this.createSuffixedVariant(Blocks.TALL_SEAGRASS, "_top", ModelTemplates.SEAGRASS, TextureMapping::defaultTexture)
      );
      MultiVariant multiVariant2 = plainVariant(
         this.createSuffixedVariant(Blocks.TALL_SEAGRASS, "_bottom", ModelTemplates.SEAGRASS, TextureMapping::defaultTexture)
      );
      this.createDoubleBlock(Blocks.TALL_SEAGRASS, multiVariant, multiVariant2);
   }

   private void createSmallDripleaf() {
      MultiVariant multiVariant = plainVariant(ModelLocationUtils.getModelLocation(Blocks.SMALL_DRIPLEAF, "_top"));
      MultiVariant multiVariant2 = plainVariant(ModelLocationUtils.getModelLocation(Blocks.SMALL_DRIPLEAF, "_bottom"));
      this.blockStateOutput
         .accept(
            MultiVariantGenerator.dispatch(Blocks.SMALL_DRIPLEAF)
               .with(
                  PropertyDispatch.initial(BlockStateProperties.DOUBLE_BLOCK_HALF)
                     .select(DoubleBlockHalf.LOWER, multiVariant2)
                     .select(DoubleBlockHalf.UPPER, multiVariant)
               )
               .with(ROTATION_HORIZONTAL_FACING)
         );
   }

   private void createDoubleBlock(Block block, MultiVariant lower, MultiVariant upper) {
      this.blockStateOutput
         .accept(
            MultiVariantGenerator.dispatch(block)
               .with(PropertyDispatch.initial(BlockStateProperties.DOUBLE_BLOCK_HALF).select(DoubleBlockHalf.LOWER, upper).select(DoubleBlockHalf.UPPER, lower))
         );
   }

   private void createPassiveRail(Block railBlock) {
      TextureMapping textureMapping = TextureMapping.rail(railBlock);
      TextureMapping textureMapping2 = TextureMapping.rail(TextureMapping.getBlockTexture(railBlock, "_corner"));
      MultiVariant multiVariant = plainVariant(ModelTemplates.RAIL_FLAT.create(railBlock, textureMapping, this.modelOutput));
      MultiVariant multiVariant2 = plainVariant(ModelTemplates.RAIL_CURVED.create(railBlock, textureMapping2, this.modelOutput));
      MultiVariant multiVariant3 = plainVariant(ModelTemplates.RAIL_RAISED_NE.create(railBlock, textureMapping, this.modelOutput));
      MultiVariant multiVariant4 = plainVariant(ModelTemplates.RAIL_RAISED_SW.create(railBlock, textureMapping, this.modelOutput));
      this.registerSimpleFlatItemModel(railBlock);
      this.blockStateOutput
         .accept(
            MultiVariantGenerator.dispatch(railBlock)
               .with(
                  PropertyDispatch.initial(BlockStateProperties.RAIL_SHAPE)
                     .select(RailShape.NORTH_SOUTH, multiVariant)
                     .select(RailShape.EAST_WEST, multiVariant.with(Y_ROT_90))
                     .select(RailShape.ASCENDING_EAST, multiVariant3.with(Y_ROT_90))
                     .select(RailShape.ASCENDING_WEST, multiVariant4.with(Y_ROT_90))
                     .select(RailShape.ASCENDING_NORTH, multiVariant3)
                     .select(RailShape.ASCENDING_SOUTH, multiVariant4)
                     .select(RailShape.SOUTH_EAST, multiVariant2)
                     .select(RailShape.SOUTH_WEST, multiVariant2.with(Y_ROT_90))
                     .select(RailShape.NORTH_WEST, multiVariant2.with(Y_ROT_180))
                     .select(RailShape.NORTH_EAST, multiVariant2.with(Y_ROT_270))
               )
         );
   }

   private void createActiveRail(Block railBlock) {
      MultiVariant multiVariant = plainVariant(this.createSuffixedVariant(railBlock, "", ModelTemplates.RAIL_FLAT, TextureMapping::rail));
      MultiVariant multiVariant2 = plainVariant(this.createSuffixedVariant(railBlock, "", ModelTemplates.RAIL_RAISED_NE, TextureMapping::rail));
      MultiVariant multiVariant3 = plainVariant(this.createSuffixedVariant(railBlock, "", ModelTemplates.RAIL_RAISED_SW, TextureMapping::rail));
      MultiVariant multiVariant4 = plainVariant(this.createSuffixedVariant(railBlock, "_on", ModelTemplates.RAIL_FLAT, TextureMapping::rail));
      MultiVariant multiVariant5 = plainVariant(this.createSuffixedVariant(railBlock, "_on", ModelTemplates.RAIL_RAISED_NE, TextureMapping::rail));
      MultiVariant multiVariant6 = plainVariant(this.createSuffixedVariant(railBlock, "_on", ModelTemplates.RAIL_RAISED_SW, TextureMapping::rail));
      this.registerSimpleFlatItemModel(railBlock);
      this.blockStateOutput
         .accept(
            MultiVariantGenerator.dispatch(railBlock)
               .with(PropertyDispatch.initial(BlockStateProperties.POWERED, BlockStateProperties.RAIL_SHAPE_STRAIGHT).generate((boolean_, railShape) -> {
                  return switch (railShape) {
                     case NORTH_SOUTH -> boolean_ ? multiVariant4 : multiVariant;
                     case EAST_WEST -> (boolean_ ? multiVariant4 : multiVariant).with(Y_ROT_90);
                     case ASCENDING_EAST -> (boolean_ ? multiVariant5 : multiVariant2).with(Y_ROT_90);
                     case ASCENDING_WEST -> (boolean_ ? multiVariant6 : multiVariant3).with(Y_ROT_90);
                     case ASCENDING_NORTH -> boolean_ ? multiVariant5 : multiVariant2;
                     case ASCENDING_SOUTH -> boolean_ ? multiVariant6 : multiVariant3;
                     default -> throw new UnsupportedOperationException("Fix you generator!");
                  };
               }))
         );
   }

   private void createAirLikeBlock(Block airLikeBlock, Item particleItem) {
      MultiVariant multiVariant = plainVariant(
         ModelTemplates.PARTICLE_ONLY.create(airLikeBlock, TextureMapping.particleFromItem(particleItem), this.modelOutput)
      );
      this.blockStateOutput.accept(createSimpleBlock(airLikeBlock, multiVariant));
   }

   private void createAirLikeBlock(Block airLikeBlock, Identifier particleTexture) {
      MultiVariant multiVariant = plainVariant(ModelTemplates.PARTICLE_ONLY.create(airLikeBlock, TextureMapping.particle(particleTexture), this.modelOutput));
      this.blockStateOutput.accept(createSimpleBlock(airLikeBlock, multiVariant));
   }

   private MultiVariant createParticleOnlyBlockModel(Block block, Block particleBlock) {
      return plainVariant(ModelTemplates.PARTICLE_ONLY.create(block, TextureMapping.particle(particleBlock), this.modelOutput));
   }

   public void createParticleOnlyBlock(Block block, Block particleBlock) {
      this.blockStateOutput.accept(createSimpleBlock(block, this.createParticleOnlyBlockModel(block, particleBlock)));
   }

   private void createParticleOnlyBlock(Block block) {
      this.createParticleOnlyBlock(block, block);
   }

   private void createFullAndCarpetBlocks(Block fullBlock, Block carpetBlock) {
      this.createTrivialCube(fullBlock);
      MultiVariant multiVariant = plainVariant(TexturedModel.CARPET.get(fullBlock).create(carpetBlock, this.modelOutput));
      this.blockStateOutput.accept(createSimpleBlock(carpetBlock, multiVariant));
   }

   private void createLeafLitter(Block block) {
      MultiVariant multiVariant = plainVariant(TexturedModel.LEAF_LITTER_1.create(block, this.modelOutput));
      MultiVariant multiVariant2 = plainVariant(TexturedModel.LEAF_LITTER_2.create(block, this.modelOutput));
      MultiVariant multiVariant3 = plainVariant(TexturedModel.LEAF_LITTER_3.create(block, this.modelOutput));
      MultiVariant multiVariant4 = plainVariant(TexturedModel.LEAF_LITTER_4.create(block, this.modelOutput));
      this.registerSimpleFlatItemModel(block.asItem());
      this.createSegmentedBlock(
         block,
         multiVariant,
         LEAF_LITTER_MODEL_1_SEGMENT_CONDITION,
         multiVariant2,
         LEAF_LITTER_MODEL_2_SEGMENT_CONDITION,
         multiVariant3,
         LEAF_LITTER_MODEL_3_SEGMENT_CONDITION,
         multiVariant4,
         LEAF_LITTER_MODEL_4_SEGMENT_CONDITION
      );
   }

   private void createFlowerBed(Block flowerBedBlock) {
      MultiVariant multiVariant = plainVariant(TexturedModel.FLOWERBED_1.create(flowerBedBlock, this.modelOutput));
      MultiVariant multiVariant2 = plainVariant(TexturedModel.FLOWERBED_2.create(flowerBedBlock, this.modelOutput));
      MultiVariant multiVariant3 = plainVariant(TexturedModel.FLOWERBED_3.create(flowerBedBlock, this.modelOutput));
      MultiVariant multiVariant4 = plainVariant(TexturedModel.FLOWERBED_4.create(flowerBedBlock, this.modelOutput));
      this.registerSimpleFlatItemModel(flowerBedBlock.asItem());
      this.createSegmentedBlock(
         flowerBedBlock,
         multiVariant,
         FLOWER_BED_MODEL_1_SEGMENT_CONDITION,
         multiVariant2,
         FLOWER_BED_MODEL_2_SEGMENT_CONDITION,
         multiVariant3,
         FLOWER_BED_MODEL_3_SEGMENT_CONDITION,
         multiVariant4,
         FLOWER_BED_MODEL_4_SEGMENT_CONDITION
      );
   }

   private void createSegmentedBlock(
      Block block,
      MultiVariant segment1Variant,
      Function<ConditionBuilder, ConditionBuilder> segment1Condition,
      MultiVariant segment2Variant,
      Function<ConditionBuilder, ConditionBuilder> segment2Condition,
      MultiVariant segment3Variant,
      Function<ConditionBuilder, ConditionBuilder> segment3Condition,
      MultiVariant segment4Variant,
      Function<ConditionBuilder, ConditionBuilder> segment4Condition
   ) {
      this.blockStateOutput
         .accept(
            MultiPartGenerator.multiPart(block)
               .with(segment1Condition.apply(condition().term(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH)), segment1Variant)
               .with(segment1Condition.apply(condition().term(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST)), segment1Variant.with(Y_ROT_90))
               .with(segment1Condition.apply(condition().term(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH)), segment1Variant.with(Y_ROT_180))
               .with(segment1Condition.apply(condition().term(BlockStateProperties.HORIZONTAL_FACING, Direction.WEST)), segment1Variant.with(Y_ROT_270))
               .with(segment2Condition.apply(condition().term(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH)), segment2Variant)
               .with(segment2Condition.apply(condition().term(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST)), segment2Variant.with(Y_ROT_90))
               .with(segment2Condition.apply(condition().term(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH)), segment2Variant.with(Y_ROT_180))
               .with(segment2Condition.apply(condition().term(BlockStateProperties.HORIZONTAL_FACING, Direction.WEST)), segment2Variant.with(Y_ROT_270))
               .with(segment3Condition.apply(condition().term(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH)), segment3Variant)
               .with(segment3Condition.apply(condition().term(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST)), segment3Variant.with(Y_ROT_90))
               .with(segment3Condition.apply(condition().term(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH)), segment3Variant.with(Y_ROT_180))
               .with(segment3Condition.apply(condition().term(BlockStateProperties.HORIZONTAL_FACING, Direction.WEST)), segment3Variant.with(Y_ROT_270))
               .with(segment4Condition.apply(condition().term(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH)), segment4Variant)
               .with(segment4Condition.apply(condition().term(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST)), segment4Variant.with(Y_ROT_90))
               .with(segment4Condition.apply(condition().term(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH)), segment4Variant.with(Y_ROT_180))
               .with(segment4Condition.apply(condition().term(BlockStateProperties.HORIZONTAL_FACING, Direction.WEST)), segment4Variant.with(Y_ROT_270))
         );
   }

   private void createColoredBlockWithRandomRotations(TexturedModel.Provider modelProvider, Block... coloredBlocks) {
      for (Block block : coloredBlocks) {
         Variant variant = plainModel(modelProvider.create(block, this.modelOutput));
         this.blockStateOutput.accept(MultiVariantGenerator.dispatch(block, createRotatedVariants(variant)));
      }
   }

   private void createColoredBlockWithStateRotations(TexturedModel.Provider modelProvider, Block... coloredBlocks) {
      for (Block block : coloredBlocks) {
         MultiVariant multiVariant = plainVariant(modelProvider.create(block, this.modelOutput));
         this.blockStateOutput.accept(MultiVariantGenerator.dispatch(block, multiVariant).with(ROTATION_HORIZONTAL_FACING_ALT));
      }
   }

   private void createGlassBlocks(Block glassBlock, Block paneBlock) {
      this.createTrivialCube(glassBlock);
      TextureMapping textureMapping = TextureMapping.pane(glassBlock, paneBlock);
      MultiVariant multiVariant = plainVariant(ModelTemplates.STAINED_GLASS_PANE_POST.create(paneBlock, textureMapping, this.modelOutput));
      MultiVariant multiVariant2 = plainVariant(ModelTemplates.STAINED_GLASS_PANE_SIDE.create(paneBlock, textureMapping, this.modelOutput));
      MultiVariant multiVariant3 = plainVariant(ModelTemplates.STAINED_GLASS_PANE_SIDE_ALT.create(paneBlock, textureMapping, this.modelOutput));
      MultiVariant multiVariant4 = plainVariant(ModelTemplates.STAINED_GLASS_PANE_NOSIDE.create(paneBlock, textureMapping, this.modelOutput));
      MultiVariant multiVariant5 = plainVariant(ModelTemplates.STAINED_GLASS_PANE_NOSIDE_ALT.create(paneBlock, textureMapping, this.modelOutput));
      Item item = paneBlock.asItem();
      this.registerSimpleItemModel(item, this.createFlatItemModelWithBlockTexture(item, glassBlock));
      this.blockStateOutput
         .accept(
            MultiPartGenerator.multiPart(paneBlock)
               .with(multiVariant)
               .with(condition().term(BlockStateProperties.NORTH, true), multiVariant2)
               .with(condition().term(BlockStateProperties.EAST, true), multiVariant2.with(Y_ROT_90))
               .with(condition().term(BlockStateProperties.SOUTH, true), multiVariant3)
               .with(condition().term(BlockStateProperties.WEST, true), multiVariant3.with(Y_ROT_90))
               .with(condition().term(BlockStateProperties.NORTH, false), multiVariant4)
               .with(condition().term(BlockStateProperties.EAST, false), multiVariant5)
               .with(condition().term(BlockStateProperties.SOUTH, false), multiVariant5.with(Y_ROT_90))
               .with(condition().term(BlockStateProperties.WEST, false), multiVariant4.with(Y_ROT_270))
         );
   }

   private void createCommandBlock(Block commandBlock) {
      TextureMapping textureMapping = TextureMapping.commandBlock(commandBlock);
      MultiVariant multiVariant = plainVariant(ModelTemplates.COMMAND_BLOCK.create(commandBlock, textureMapping, this.modelOutput));
      MultiVariant multiVariant2 = plainVariant(
         this.createSuffixedVariant(
            commandBlock, "_conditional", ModelTemplates.COMMAND_BLOCK, identifier -> textureMapping.copyAndUpdate(TextureSlot.SIDE, identifier)
         )
      );
      this.blockStateOutput
         .accept(
            MultiVariantGenerator.dispatch(commandBlock)
               .with(createBooleanModelDispatch(BlockStateProperties.CONDITIONAL, multiVariant2, multiVariant))
               .with(ROTATION_FACING)
         );
   }

   private void createAnvil(Block anvilBlock) {
      MultiVariant multiVariant = plainVariant(TexturedModel.ANVIL.create(anvilBlock, this.modelOutput));
      this.blockStateOutput.accept(createSimpleBlock(anvilBlock, multiVariant).with(ROTATION_HORIZONTAL_FACING_ALT));
   }

   private static MultiVariant createBambooModels(int age) {
      String string = "_age" + age;
      return new MultiVariant(
         WeightedList.of(
            IntStream.range(1, 5)
               .mapToObj(i -> new Weighted<>(plainModel(ModelLocationUtils.getModelLocation(Blocks.BAMBOO, i + string)), 1))
               .collect(Collectors.toList())
         )
      );
   }

   private void createBamboo() {
      this.blockStateOutput
         .accept(
            MultiPartGenerator.multiPart(Blocks.BAMBOO)
               .with(condition().term(BlockStateProperties.AGE_1, 0), createBambooModels(0))
               .with(condition().term(BlockStateProperties.AGE_1, 1), createBambooModels(1))
               .with(
                  condition().term(BlockStateProperties.BAMBOO_LEAVES, BambooLeaves.SMALL),
                  plainVariant(ModelLocationUtils.getModelLocation(Blocks.BAMBOO, "_small_leaves"))
               )
               .with(
                  condition().term(BlockStateProperties.BAMBOO_LEAVES, BambooLeaves.LARGE),
                  plainVariant(ModelLocationUtils.getModelLocation(Blocks.BAMBOO, "_large_leaves"))
               )
         );
   }

   private void createBarrel() {
      Identifier identifier = TextureMapping.getBlockTexture(Blocks.BARREL, "_top_open");
      MultiVariant multiVariant = plainVariant(TexturedModel.CUBE_TOP_BOTTOM.create(Blocks.BARREL, this.modelOutput));
      MultiVariant multiVariant2 = plainVariant(
         TexturedModel.CUBE_TOP_BOTTOM
            .get(Blocks.BARREL)
            .updateTextures(textureMapping -> textureMapping.put(TextureSlot.TOP, identifier))
            .createWithSuffix(Blocks.BARREL, "_open", this.modelOutput)
      );
      this.blockStateOutput
         .accept(
            MultiVariantGenerator.dispatch(Blocks.BARREL)
               .with(PropertyDispatch.initial(BlockStateProperties.OPEN).select(false, multiVariant).select(true, multiVariant2))
               .with(ROTATIONS_COLUMN_WITH_FACING)
         );
   }

   private static <T extends Comparable<T>> PropertyDispatch<MultiVariant> createEmptyOrFullDispatch(
      Property<T> property, T threshold, MultiVariant full, MultiVariant empty
   ) {
      return PropertyDispatch.initial(property).generate(comparable2 -> {
         boolean bl = comparable2.compareTo(threshold) >= 0;
         return bl ? full : empty;
      });
   }

   private void createBeeNest(Block beeNestBlock, Function<Block, TextureMapping> textureMappingGetter) {
      TextureMapping textureMapping = textureMappingGetter.apply(beeNestBlock).copyForced(TextureSlot.SIDE, TextureSlot.PARTICLE);
      TextureMapping textureMapping2 = textureMapping.copyAndUpdate(TextureSlot.FRONT, TextureMapping.getBlockTexture(beeNestBlock, "_front_honey"));
      Identifier identifier = ModelTemplates.CUBE_ORIENTABLE_TOP_BOTTOM.createWithSuffix(beeNestBlock, "_empty", textureMapping, this.modelOutput);
      Identifier identifier2 = ModelTemplates.CUBE_ORIENTABLE_TOP_BOTTOM.createWithSuffix(beeNestBlock, "_honey", textureMapping2, this.modelOutput);
      this.itemModelOutput
         .accept(
            beeNestBlock.asItem(),
            ItemModelUtils.selectBlockItemProperty(
               BeehiveBlock.HONEY_LEVEL, ItemModelUtils.plainModel(identifier), Map.of(5, ItemModelUtils.plainModel(identifier2))
            )
         );
      this.blockStateOutput
         .accept(
            MultiVariantGenerator.dispatch(beeNestBlock)
               .with(createEmptyOrFullDispatch(BeehiveBlock.HONEY_LEVEL, 5, plainVariant(identifier2), plainVariant(identifier)))
               .with(ROTATION_HORIZONTAL_FACING)
         );
   }

   private void createCropBlock(Block cropBlock, Property<Integer> ageProperty, int... ageToVisualStageMapping) {
      this.registerSimpleFlatItemModel(cropBlock.asItem());
      if (ageProperty.getPossibleValues().size() != ageToVisualStageMapping.length) {
         throw new IllegalArgumentException();
      } else {
         Int2ObjectMap<Identifier> int2ObjectMap = new Int2ObjectOpenHashMap();
         this.blockStateOutput
            .accept(
               MultiVariantGenerator.dispatch(cropBlock)
                  .with(
                     PropertyDispatch.initial(ageProperty)
                        .generate(
                           integer -> {
                              int i = ageToVisualStageMapping[integer];
                              return plainVariant(
                                 (Identifier)int2ObjectMap.computeIfAbsent(
                                    i, ix -> this.createSuffixedVariant(cropBlock, "_stage" + ix, ModelTemplates.CROP, TextureMapping::crop)
                                 )
                              );
                           }
                        )
                  )
            );
      }
   }

   private void createBell() {
      MultiVariant multiVariant = plainVariant(ModelLocationUtils.getModelLocation(Blocks.BELL, "_floor"));
      MultiVariant multiVariant2 = plainVariant(ModelLocationUtils.getModelLocation(Blocks.BELL, "_ceiling"));
      MultiVariant multiVariant3 = plainVariant(ModelLocationUtils.getModelLocation(Blocks.BELL, "_wall"));
      MultiVariant multiVariant4 = plainVariant(ModelLocationUtils.getModelLocation(Blocks.BELL, "_between_walls"));
      this.registerSimpleFlatItemModel(Items.BELL);
      this.blockStateOutput
         .accept(
            MultiVariantGenerator.dispatch(Blocks.BELL)
               .with(
                  PropertyDispatch.initial(BlockStateProperties.HORIZONTAL_FACING, BlockStateProperties.BELL_ATTACHMENT)
                     .select(Direction.NORTH, BellAttachType.FLOOR, multiVariant)
                     .select(Direction.SOUTH, BellAttachType.FLOOR, multiVariant.with(Y_ROT_180))
                     .select(Direction.EAST, BellAttachType.FLOOR, multiVariant.with(Y_ROT_90))
                     .select(Direction.WEST, BellAttachType.FLOOR, multiVariant.with(Y_ROT_270))
                     .select(Direction.NORTH, BellAttachType.CEILING, multiVariant2)
                     .select(Direction.SOUTH, BellAttachType.CEILING, multiVariant2.with(Y_ROT_180))
                     .select(Direction.EAST, BellAttachType.CEILING, multiVariant2.with(Y_ROT_90))
                     .select(Direction.WEST, BellAttachType.CEILING, multiVariant2.with(Y_ROT_270))
                     .select(Direction.NORTH, BellAttachType.SINGLE_WALL, multiVariant3.with(Y_ROT_270))
                     .select(Direction.SOUTH, BellAttachType.SINGLE_WALL, multiVariant3.with(Y_ROT_90))
                     .select(Direction.EAST, BellAttachType.SINGLE_WALL, multiVariant3)
                     .select(Direction.WEST, BellAttachType.SINGLE_WALL, multiVariant3.with(Y_ROT_180))
                     .select(Direction.SOUTH, BellAttachType.DOUBLE_WALL, multiVariant4.with(Y_ROT_90))
                     .select(Direction.NORTH, BellAttachType.DOUBLE_WALL, multiVariant4.with(Y_ROT_270))
                     .select(Direction.EAST, BellAttachType.DOUBLE_WALL, multiVariant4)
                     .select(Direction.WEST, BellAttachType.DOUBLE_WALL, multiVariant4.with(Y_ROT_180))
               )
         );
   }

   private void createGrindstone() {
      this.blockStateOutput
         .accept(
            MultiVariantGenerator.dispatch(Blocks.GRINDSTONE, plainVariant(ModelLocationUtils.getModelLocation(Blocks.GRINDSTONE)))
               .with(
                  PropertyDispatch.modify(BlockStateProperties.ATTACH_FACE, BlockStateProperties.HORIZONTAL_FACING)
                     .select(AttachFace.FLOOR, Direction.NORTH, NOP)
                     .select(AttachFace.FLOOR, Direction.EAST, Y_ROT_90)
                     .select(AttachFace.FLOOR, Direction.SOUTH, Y_ROT_180)
                     .select(AttachFace.FLOOR, Direction.WEST, Y_ROT_270)
                     .select(AttachFace.WALL, Direction.NORTH, X_ROT_90)
                     .select(AttachFace.WALL, Direction.EAST, X_ROT_90.then(Y_ROT_90))
                     .select(AttachFace.WALL, Direction.SOUTH, X_ROT_90.then(Y_ROT_180))
                     .select(AttachFace.WALL, Direction.WEST, X_ROT_90.then(Y_ROT_270))
                     .select(AttachFace.CEILING, Direction.SOUTH, X_ROT_180)
                     .select(AttachFace.CEILING, Direction.WEST, X_ROT_180.then(Y_ROT_90))
                     .select(AttachFace.CEILING, Direction.NORTH, X_ROT_180.then(Y_ROT_180))
                     .select(AttachFace.CEILING, Direction.EAST, X_ROT_180.then(Y_ROT_270))
               )
         );
   }

   private void createFurnace(Block furnaceBlock, TexturedModel.Provider modelProvider) {
      MultiVariant multiVariant = plainVariant(modelProvider.create(furnaceBlock, this.modelOutput));
      Identifier identifier = TextureMapping.getBlockTexture(furnaceBlock, "_front_on");
      MultiVariant multiVariant2 = plainVariant(
         modelProvider.get(furnaceBlock)
            .updateTextures(textureMapping -> textureMapping.put(TextureSlot.FRONT, identifier))
            .createWithSuffix(furnaceBlock, "_on", this.modelOutput)
      );
      this.blockStateOutput
         .accept(
            MultiVariantGenerator.dispatch(furnaceBlock)
               .with(createBooleanModelDispatch(BlockStateProperties.LIT, multiVariant2, multiVariant))
               .with(ROTATION_HORIZONTAL_FACING)
         );
   }

   private void createCampfires(Block... campfireBlocks) {
      MultiVariant multiVariant = plainVariant(ModelLocationUtils.decorateBlockModelLocation("campfire_off"));

      for (Block block : campfireBlocks) {
         MultiVariant multiVariant2 = plainVariant(ModelTemplates.CAMPFIRE.create(block, TextureMapping.campfire(block), this.modelOutput));
         this.registerSimpleFlatItemModel(block.asItem());
         this.blockStateOutput
            .accept(
               MultiVariantGenerator.dispatch(block)
                  .with(createBooleanModelDispatch(BlockStateProperties.LIT, multiVariant2, multiVariant))
                  .with(ROTATION_HORIZONTAL_FACING_ALT)
            );
      }
   }

   private void createAzalea(Block azaleaBlock) {
      MultiVariant multiVariant = plainVariant(ModelTemplates.AZALEA.create(azaleaBlock, TextureMapping.cubeTop(azaleaBlock), this.modelOutput));
      this.blockStateOutput.accept(createSimpleBlock(azaleaBlock, multiVariant));
   }

   private void createPottedAzalea(Block pottedAzaleaBlock) {
      MultiVariant multiVariant;
      if (pottedAzaleaBlock == Blocks.POTTED_FLOWERING_AZALEA) {
         multiVariant = plainVariant(
            ModelTemplates.POTTED_FLOWERING_AZALEA.create(pottedAzaleaBlock, TextureMapping.pottedAzalea(pottedAzaleaBlock), this.modelOutput)
         );
      } else {
         multiVariant = plainVariant(ModelTemplates.POTTED_AZALEA.create(pottedAzaleaBlock, TextureMapping.pottedAzalea(pottedAzaleaBlock), this.modelOutput));
      }

      this.blockStateOutput.accept(createSimpleBlock(pottedAzaleaBlock, multiVariant));
   }

   private void createBookshelf() {
      TextureMapping textureMapping = TextureMapping.column(TextureMapping.getBlockTexture(Blocks.BOOKSHELF), TextureMapping.getBlockTexture(Blocks.OAK_PLANKS));
      MultiVariant multiVariant = plainVariant(ModelTemplates.CUBE_COLUMN.create(Blocks.BOOKSHELF, textureMapping, this.modelOutput));
      this.blockStateOutput.accept(createSimpleBlock(Blocks.BOOKSHELF, multiVariant));
   }

   private void createRedstoneWire() {
      this.registerSimpleFlatItemModel(Items.REDSTONE);
      this.blockStateOutput
         .accept(
            MultiPartGenerator.multiPart(Blocks.REDSTONE_WIRE)
               .with(
                  or(
                     condition()
                        .term(BlockStateProperties.NORTH_REDSTONE, RedstoneSide.NONE)
                        .term(BlockStateProperties.EAST_REDSTONE, RedstoneSide.NONE)
                        .term(BlockStateProperties.SOUTH_REDSTONE, RedstoneSide.NONE)
                        .term(BlockStateProperties.WEST_REDSTONE, RedstoneSide.NONE),
                     condition()
                        .term(BlockStateProperties.NORTH_REDSTONE, RedstoneSide.SIDE, RedstoneSide.UP)
                        .term(BlockStateProperties.EAST_REDSTONE, RedstoneSide.SIDE, RedstoneSide.UP),
                     condition()
                        .term(BlockStateProperties.EAST_REDSTONE, RedstoneSide.SIDE, RedstoneSide.UP)
                        .term(BlockStateProperties.SOUTH_REDSTONE, RedstoneSide.SIDE, RedstoneSide.UP),
                     condition()
                        .term(BlockStateProperties.SOUTH_REDSTONE, RedstoneSide.SIDE, RedstoneSide.UP)
                        .term(BlockStateProperties.WEST_REDSTONE, RedstoneSide.SIDE, RedstoneSide.UP),
                     condition()
                        .term(BlockStateProperties.WEST_REDSTONE, RedstoneSide.SIDE, RedstoneSide.UP)
                        .term(BlockStateProperties.NORTH_REDSTONE, RedstoneSide.SIDE, RedstoneSide.UP)
                  ),
                  plainVariant(ModelLocationUtils.decorateBlockModelLocation("redstone_dust_dot"))
               )
               .with(
                  condition().term(BlockStateProperties.NORTH_REDSTONE, RedstoneSide.SIDE, RedstoneSide.UP),
                  plainVariant(ModelLocationUtils.decorateBlockModelLocation("redstone_dust_side0"))
               )
               .with(
                  condition().term(BlockStateProperties.SOUTH_REDSTONE, RedstoneSide.SIDE, RedstoneSide.UP),
                  plainVariant(ModelLocationUtils.decorateBlockModelLocation("redstone_dust_side_alt0"))
               )
               .with(
                  condition().term(BlockStateProperties.EAST_REDSTONE, RedstoneSide.SIDE, RedstoneSide.UP),
                  plainVariant(ModelLocationUtils.decorateBlockModelLocation("redstone_dust_side_alt1")).with(Y_ROT_270)
               )
               .with(
                  condition().term(BlockStateProperties.WEST_REDSTONE, RedstoneSide.SIDE, RedstoneSide.UP),
                  plainVariant(ModelLocationUtils.decorateBlockModelLocation("redstone_dust_side1")).with(Y_ROT_270)
               )
               .with(
                  condition().term(BlockStateProperties.NORTH_REDSTONE, RedstoneSide.UP),
                  plainVariant(ModelLocationUtils.decorateBlockModelLocation("redstone_dust_up"))
               )
               .with(
                  condition().term(BlockStateProperties.EAST_REDSTONE, RedstoneSide.UP),
                  plainVariant(ModelLocationUtils.decorateBlockModelLocation("redstone_dust_up")).with(Y_ROT_90)
               )
               .with(
                  condition().term(BlockStateProperties.SOUTH_REDSTONE, RedstoneSide.UP),
                  plainVariant(ModelLocationUtils.decorateBlockModelLocation("redstone_dust_up")).with(Y_ROT_180)
               )
               .with(
                  condition().term(BlockStateProperties.WEST_REDSTONE, RedstoneSide.UP),
                  plainVariant(ModelLocationUtils.decorateBlockModelLocation("redstone_dust_up")).with(Y_ROT_270)
               )
         );
   }

   private void createComparator() {
      this.registerSimpleFlatItemModel(Items.COMPARATOR);
      this.blockStateOutput
         .accept(
            MultiVariantGenerator.dispatch(Blocks.COMPARATOR)
               .with(
                  PropertyDispatch.initial(BlockStateProperties.MODE_COMPARATOR, BlockStateProperties.POWERED)
                     .select(ComparatorMode.COMPARE, false, plainVariant(ModelLocationUtils.getModelLocation(Blocks.COMPARATOR)))
                     .select(ComparatorMode.COMPARE, true, plainVariant(ModelLocationUtils.getModelLocation(Blocks.COMPARATOR, "_on")))
                     .select(ComparatorMode.SUBTRACT, false, plainVariant(ModelLocationUtils.getModelLocation(Blocks.COMPARATOR, "_subtract")))
                     .select(ComparatorMode.SUBTRACT, true, plainVariant(ModelLocationUtils.getModelLocation(Blocks.COMPARATOR, "_on_subtract")))
               )
               .with(ROTATION_HORIZONTAL_FACING_ALT)
         );
   }

   private void createSmoothStoneSlab() {
      TextureMapping textureMapping = TextureMapping.cube(Blocks.SMOOTH_STONE);
      TextureMapping textureMapping2 = TextureMapping.column(
         TextureMapping.getBlockTexture(Blocks.SMOOTH_STONE_SLAB, "_side"), textureMapping.get(TextureSlot.TOP)
      );
      MultiVariant multiVariant = plainVariant(ModelTemplates.SLAB_BOTTOM.create(Blocks.SMOOTH_STONE_SLAB, textureMapping2, this.modelOutput));
      MultiVariant multiVariant2 = plainVariant(ModelTemplates.SLAB_TOP.create(Blocks.SMOOTH_STONE_SLAB, textureMapping2, this.modelOutput));
      MultiVariant multiVariant3 = plainVariant(
         ModelTemplates.CUBE_COLUMN.createWithOverride(Blocks.SMOOTH_STONE_SLAB, "_double", textureMapping2, this.modelOutput)
      );
      this.blockStateOutput.accept(createSlab(Blocks.SMOOTH_STONE_SLAB, multiVariant, multiVariant2, multiVariant3));
      this.blockStateOutput
         .accept(createSimpleBlock(Blocks.SMOOTH_STONE, plainVariant(ModelTemplates.CUBE_ALL.create(Blocks.SMOOTH_STONE, textureMapping, this.modelOutput))));
   }

   private void createBrewingStand() {
      this.registerSimpleFlatItemModel(Items.BREWING_STAND);
      this.blockStateOutput
         .accept(
            MultiPartGenerator.multiPart(Blocks.BREWING_STAND)
               .with(plainVariant(TextureMapping.getBlockTexture(Blocks.BREWING_STAND)))
               .with(condition().term(BlockStateProperties.HAS_BOTTLE_0, true), plainVariant(TextureMapping.getBlockTexture(Blocks.BREWING_STAND, "_bottle0")))
               .with(condition().term(BlockStateProperties.HAS_BOTTLE_1, true), plainVariant(TextureMapping.getBlockTexture(Blocks.BREWING_STAND, "_bottle1")))
               .with(condition().term(BlockStateProperties.HAS_BOTTLE_2, true), plainVariant(TextureMapping.getBlockTexture(Blocks.BREWING_STAND, "_bottle2")))
               .with(condition().term(BlockStateProperties.HAS_BOTTLE_0, false), plainVariant(TextureMapping.getBlockTexture(Blocks.BREWING_STAND, "_empty0")))
               .with(condition().term(BlockStateProperties.HAS_BOTTLE_1, false), plainVariant(TextureMapping.getBlockTexture(Blocks.BREWING_STAND, "_empty1")))
               .with(condition().term(BlockStateProperties.HAS_BOTTLE_2, false), plainVariant(TextureMapping.getBlockTexture(Blocks.BREWING_STAND, "_empty2")))
         );
   }

   private void createMushroomBlock(Block mushroomBlock) {
      MultiVariant multiVariant = plainVariant(ModelTemplates.SINGLE_FACE.create(mushroomBlock, TextureMapping.defaultTexture(mushroomBlock), this.modelOutput));
      MultiVariant multiVariant2 = plainVariant(ModelLocationUtils.decorateBlockModelLocation("mushroom_block_inside"));
      this.blockStateOutput
         .accept(
            MultiPartGenerator.multiPart(mushroomBlock)
               .with(condition().term(BlockStateProperties.NORTH, true), multiVariant)
               .with(condition().term(BlockStateProperties.EAST, true), multiVariant.with(Y_ROT_90).with(UV_LOCK))
               .with(condition().term(BlockStateProperties.SOUTH, true), multiVariant.with(Y_ROT_180).with(UV_LOCK))
               .with(condition().term(BlockStateProperties.WEST, true), multiVariant.with(Y_ROT_270).with(UV_LOCK))
               .with(condition().term(BlockStateProperties.UP, true), multiVariant.with(X_ROT_270).with(UV_LOCK))
               .with(condition().term(BlockStateProperties.DOWN, true), multiVariant.with(X_ROT_90).with(UV_LOCK))
               .with(condition().term(BlockStateProperties.NORTH, false), multiVariant2)
               .with(condition().term(BlockStateProperties.EAST, false), multiVariant2.with(Y_ROT_90))
               .with(condition().term(BlockStateProperties.SOUTH, false), multiVariant2.with(Y_ROT_180))
               .with(condition().term(BlockStateProperties.WEST, false), multiVariant2.with(Y_ROT_270))
               .with(condition().term(BlockStateProperties.UP, false), multiVariant2.with(X_ROT_270))
               .with(condition().term(BlockStateProperties.DOWN, false), multiVariant2.with(X_ROT_90))
         );
      this.registerSimpleItemModel(mushroomBlock, TexturedModel.CUBE.createWithSuffix(mushroomBlock, "_inventory", this.modelOutput));
   }

   private void createCakeBlock() {
      this.registerSimpleFlatItemModel(Items.CAKE);
      this.blockStateOutput
         .accept(
            MultiVariantGenerator.dispatch(Blocks.CAKE)
               .with(
                  PropertyDispatch.initial(BlockStateProperties.BITES)
                     .select(0, plainVariant(ModelLocationUtils.getModelLocation(Blocks.CAKE)))
                     .select(1, plainVariant(ModelLocationUtils.getModelLocation(Blocks.CAKE, "_slice1")))
                     .select(2, plainVariant(ModelLocationUtils.getModelLocation(Blocks.CAKE, "_slice2")))
                     .select(3, plainVariant(ModelLocationUtils.getModelLocation(Blocks.CAKE, "_slice3")))
                     .select(4, plainVariant(ModelLocationUtils.getModelLocation(Blocks.CAKE, "_slice4")))
                     .select(5, plainVariant(ModelLocationUtils.getModelLocation(Blocks.CAKE, "_slice5")))
                     .select(6, plainVariant(ModelLocationUtils.getModelLocation(Blocks.CAKE, "_slice6")))
               )
         );
   }

   private void createCartographyTable() {
      TextureMapping textureMapping = new TextureMapping()
         .put(TextureSlot.PARTICLE, TextureMapping.getBlockTexture(Blocks.CARTOGRAPHY_TABLE, "_side3"))
         .put(TextureSlot.DOWN, TextureMapping.getBlockTexture(Blocks.DARK_OAK_PLANKS))
         .put(TextureSlot.UP, TextureMapping.getBlockTexture(Blocks.CARTOGRAPHY_TABLE, "_top"))
         .put(TextureSlot.NORTH, TextureMapping.getBlockTexture(Blocks.CARTOGRAPHY_TABLE, "_side3"))
         .put(TextureSlot.EAST, TextureMapping.getBlockTexture(Blocks.CARTOGRAPHY_TABLE, "_side3"))
         .put(TextureSlot.SOUTH, TextureMapping.getBlockTexture(Blocks.CARTOGRAPHY_TABLE, "_side1"))
         .put(TextureSlot.WEST, TextureMapping.getBlockTexture(Blocks.CARTOGRAPHY_TABLE, "_side2"));
      this.blockStateOutput
         .accept(
            createSimpleBlock(Blocks.CARTOGRAPHY_TABLE, plainVariant(ModelTemplates.CUBE.create(Blocks.CARTOGRAPHY_TABLE, textureMapping, this.modelOutput)))
         );
   }

   private void createSmithingTable() {
      TextureMapping textureMapping = new TextureMapping()
         .put(TextureSlot.PARTICLE, TextureMapping.getBlockTexture(Blocks.SMITHING_TABLE, "_front"))
         .put(TextureSlot.DOWN, TextureMapping.getBlockTexture(Blocks.SMITHING_TABLE, "_bottom"))
         .put(TextureSlot.UP, TextureMapping.getBlockTexture(Blocks.SMITHING_TABLE, "_top"))
         .put(TextureSlot.NORTH, TextureMapping.getBlockTexture(Blocks.SMITHING_TABLE, "_front"))
         .put(TextureSlot.SOUTH, TextureMapping.getBlockTexture(Blocks.SMITHING_TABLE, "_front"))
         .put(TextureSlot.EAST, TextureMapping.getBlockTexture(Blocks.SMITHING_TABLE, "_side"))
         .put(TextureSlot.WEST, TextureMapping.getBlockTexture(Blocks.SMITHING_TABLE, "_side"));
      this.blockStateOutput
         .accept(createSimpleBlock(Blocks.SMITHING_TABLE, plainVariant(ModelTemplates.CUBE.create(Blocks.SMITHING_TABLE, textureMapping, this.modelOutput))));
   }

   private void createCraftingTableLike(
      Block craftingTableBlock, Block craftingTableMaterialBlock, BiFunction<Block, Block, TextureMapping> textureMappingGetter
   ) {
      TextureMapping textureMapping = textureMappingGetter.apply(craftingTableBlock, craftingTableMaterialBlock);
      this.blockStateOutput
         .accept(createSimpleBlock(craftingTableBlock, plainVariant(ModelTemplates.CUBE.create(craftingTableBlock, textureMapping, this.modelOutput))));
   }

   public void createGenericCube(Block block) {
      TextureMapping textureMapping = new TextureMapping()
         .put(TextureSlot.PARTICLE, TextureMapping.getBlockTexture(block, "_particle"))
         .put(TextureSlot.DOWN, TextureMapping.getBlockTexture(block, "_down"))
         .put(TextureSlot.UP, TextureMapping.getBlockTexture(block, "_up"))
         .put(TextureSlot.NORTH, TextureMapping.getBlockTexture(block, "_north"))
         .put(TextureSlot.SOUTH, TextureMapping.getBlockTexture(block, "_south"))
         .put(TextureSlot.EAST, TextureMapping.getBlockTexture(block, "_east"))
         .put(TextureSlot.WEST, TextureMapping.getBlockTexture(block, "_west"));
      this.blockStateOutput.accept(createSimpleBlock(block, plainVariant(ModelTemplates.CUBE.create(block, textureMapping, this.modelOutput))));
   }

   private void createPumpkins() {
      TextureMapping textureMapping = TextureMapping.column(Blocks.PUMPKIN);
      this.blockStateOutput.accept(createSimpleBlock(Blocks.PUMPKIN, plainVariant(ModelLocationUtils.getModelLocation(Blocks.PUMPKIN))));
      this.createPumpkinVariant(Blocks.CARVED_PUMPKIN, textureMapping);
      this.createPumpkinVariant(Blocks.JACK_O_LANTERN, textureMapping);
   }

   private void createPumpkinVariant(Block pumpkinBlock, TextureMapping columnTextureMapping) {
      MultiVariant multiVariant = plainVariant(
         ModelTemplates.CUBE_ORIENTABLE
            .create(pumpkinBlock, columnTextureMapping.copyAndUpdate(TextureSlot.FRONT, TextureMapping.getBlockTexture(pumpkinBlock)), this.modelOutput)
      );
      this.blockStateOutput.accept(MultiVariantGenerator.dispatch(pumpkinBlock, multiVariant).with(ROTATION_HORIZONTAL_FACING));
   }

   private void createCauldrons() {
      this.registerSimpleFlatItemModel(Items.CAULDRON);
      this.createNonTemplateModelBlock(Blocks.CAULDRON);
      this.blockStateOutput
         .accept(
            createSimpleBlock(
               Blocks.LAVA_CAULDRON,
               plainVariant(
                  ModelTemplates.CAULDRON_FULL
                     .create(Blocks.LAVA_CAULDRON, TextureMapping.cauldron(TextureMapping.getBlockTexture(Blocks.LAVA, "_still")), this.modelOutput)
               )
            )
         );
      this.blockStateOutput
         .accept(
            MultiVariantGenerator.dispatch(Blocks.WATER_CAULDRON)
               .with(
                  PropertyDispatch.initial(LayeredCauldronBlock.LEVEL)
                     .select(
                        1,
                        plainVariant(
                           ModelTemplates.CAULDRON_LEVEL1
                              .createWithSuffix(
                                 Blocks.WATER_CAULDRON,
                                 "_level1",
                                 TextureMapping.cauldron(TextureMapping.getBlockTexture(Blocks.WATER, "_still")),
                                 this.modelOutput
                              )
                        )
                     )
                     .select(
                        2,
                        plainVariant(
                           ModelTemplates.CAULDRON_LEVEL2
                              .createWithSuffix(
                                 Blocks.WATER_CAULDRON,
                                 "_level2",
                                 TextureMapping.cauldron(TextureMapping.getBlockTexture(Blocks.WATER, "_still")),
                                 this.modelOutput
                              )
                        )
                     )
                     .select(
                        3,
                        plainVariant(
                           ModelTemplates.CAULDRON_FULL
                              .createWithSuffix(
                                 Blocks.WATER_CAULDRON,
                                 "_full",
                                 TextureMapping.cauldron(TextureMapping.getBlockTexture(Blocks.WATER, "_still")),
                                 this.modelOutput
                              )
                        )
                     )
               )
         );
      this.blockStateOutput
         .accept(
            MultiVariantGenerator.dispatch(Blocks.POWDER_SNOW_CAULDRON)
               .with(
                  PropertyDispatch.initial(LayeredCauldronBlock.LEVEL)
                     .select(
                        1,
                        plainVariant(
                           ModelTemplates.CAULDRON_LEVEL1
                              .createWithSuffix(
                                 Blocks.POWDER_SNOW_CAULDRON,
                                 "_level1",
                                 TextureMapping.cauldron(TextureMapping.getBlockTexture(Blocks.POWDER_SNOW)),
                                 this.modelOutput
                              )
                        )
                     )
                     .select(
                        2,
                        plainVariant(
                           ModelTemplates.CAULDRON_LEVEL2
                              .createWithSuffix(
                                 Blocks.POWDER_SNOW_CAULDRON,
                                 "_level2",
                                 TextureMapping.cauldron(TextureMapping.getBlockTexture(Blocks.POWDER_SNOW)),
                                 this.modelOutput
                              )
                        )
                     )
                     .select(
                        3,
                        plainVariant(
                           ModelTemplates.CAULDRON_FULL
                              .createWithSuffix(
                                 Blocks.POWDER_SNOW_CAULDRON,
                                 "_full",
                                 TextureMapping.cauldron(TextureMapping.getBlockTexture(Blocks.POWDER_SNOW)),
                                 this.modelOutput
                              )
                        )
                     )
               )
         );
   }

   private void createChorusFlower() {
      TextureMapping textureMapping = TextureMapping.defaultTexture(Blocks.CHORUS_FLOWER);
      MultiVariant multiVariant = plainVariant(ModelTemplates.CHORUS_FLOWER.create(Blocks.CHORUS_FLOWER, textureMapping, this.modelOutput));
      MultiVariant multiVariant2 = plainVariant(
         this.createSuffixedVariant(
            Blocks.CHORUS_FLOWER, "_dead", ModelTemplates.CHORUS_FLOWER, identifier -> textureMapping.copyAndUpdate(TextureSlot.TEXTURE, identifier)
         )
      );
      this.blockStateOutput
         .accept(
            MultiVariantGenerator.dispatch(Blocks.CHORUS_FLOWER).with(createEmptyOrFullDispatch(BlockStateProperties.AGE_5, 5, multiVariant2, multiVariant))
         );
   }

   private void createCrafterBlock() {
      MultiVariant multiVariant = plainVariant(ModelLocationUtils.getModelLocation(Blocks.CRAFTER));
      MultiVariant multiVariant2 = plainVariant(ModelLocationUtils.getModelLocation(Blocks.CRAFTER, "_triggered"));
      MultiVariant multiVariant3 = plainVariant(ModelLocationUtils.getModelLocation(Blocks.CRAFTER, "_crafting"));
      MultiVariant multiVariant4 = plainVariant(ModelLocationUtils.getModelLocation(Blocks.CRAFTER, "_crafting_triggered"));
      this.blockStateOutput
         .accept(
            MultiVariantGenerator.dispatch(Blocks.CRAFTER)
               .with(
                  PropertyDispatch.initial(BlockStateProperties.TRIGGERED, CrafterBlock.CRAFTING)
                     .select(false, false, multiVariant)
                     .select(true, true, multiVariant4)
                     .select(true, false, multiVariant2)
                     .select(false, true, multiVariant3)
               )
               .with(PropertyDispatch.modify(BlockStateProperties.ORIENTATION).generate(BlockModelGenerators::applyRotation))
         );
   }

   private void createDispenserBlock(Block dispenserBlock) {
      TextureMapping textureMapping = new TextureMapping()
         .put(TextureSlot.TOP, TextureMapping.getBlockTexture(Blocks.FURNACE, "_top"))
         .put(TextureSlot.SIDE, TextureMapping.getBlockTexture(Blocks.FURNACE, "_side"))
         .put(TextureSlot.FRONT, TextureMapping.getBlockTexture(dispenserBlock, "_front"));
      TextureMapping textureMapping2 = new TextureMapping()
         .put(TextureSlot.SIDE, TextureMapping.getBlockTexture(Blocks.FURNACE, "_top"))
         .put(TextureSlot.FRONT, TextureMapping.getBlockTexture(dispenserBlock, "_front_vertical"));
      MultiVariant multiVariant = plainVariant(ModelTemplates.CUBE_ORIENTABLE.create(dispenserBlock, textureMapping, this.modelOutput));
      MultiVariant multiVariant2 = plainVariant(ModelTemplates.CUBE_ORIENTABLE_VERTICAL.create(dispenserBlock, textureMapping2, this.modelOutput));
      this.blockStateOutput
         .accept(
            MultiVariantGenerator.dispatch(dispenserBlock)
               .with(
                  PropertyDispatch.initial(BlockStateProperties.FACING)
                     .select(Direction.DOWN, multiVariant2.with(X_ROT_180))
                     .select(Direction.UP, multiVariant2)
                     .select(Direction.NORTH, multiVariant)
                     .select(Direction.EAST, multiVariant.with(Y_ROT_90))
                     .select(Direction.SOUTH, multiVariant.with(Y_ROT_180))
                     .select(Direction.WEST, multiVariant.with(Y_ROT_270))
               )
         );
   }

   private void createEndPortalFrame() {
      MultiVariant multiVariant = plainVariant(ModelLocationUtils.getModelLocation(Blocks.END_PORTAL_FRAME));
      MultiVariant multiVariant2 = plainVariant(ModelLocationUtils.getModelLocation(Blocks.END_PORTAL_FRAME, "_filled"));
      this.blockStateOutput
         .accept(
            MultiVariantGenerator.dispatch(Blocks.END_PORTAL_FRAME)
               .with(PropertyDispatch.initial(BlockStateProperties.EYE).select(false, multiVariant).select(true, multiVariant2))
               .with(ROTATION_HORIZONTAL_FACING_ALT)
         );
   }

   private void createChorusPlant() {
      MultiVariant multiVariant = plainVariant(ModelLocationUtils.getModelLocation(Blocks.CHORUS_PLANT, "_side"));
      Variant variant = plainModel(ModelLocationUtils.getModelLocation(Blocks.CHORUS_PLANT, "_noside"));
      Variant variant2 = plainModel(ModelLocationUtils.getModelLocation(Blocks.CHORUS_PLANT, "_noside1"));
      Variant variant3 = plainModel(ModelLocationUtils.getModelLocation(Blocks.CHORUS_PLANT, "_noside2"));
      Variant variant4 = plainModel(ModelLocationUtils.getModelLocation(Blocks.CHORUS_PLANT, "_noside3"));
      Variant variant5 = variant.with(UV_LOCK);
      Variant variant6 = variant2.with(UV_LOCK);
      Variant variant7 = variant3.with(UV_LOCK);
      Variant variant8 = variant4.with(UV_LOCK);
      this.blockStateOutput
         .accept(
            MultiPartGenerator.multiPart(Blocks.CHORUS_PLANT)
               .with(condition().term(BlockStateProperties.NORTH, true), multiVariant)
               .with(condition().term(BlockStateProperties.EAST, true), multiVariant.with(Y_ROT_90).with(UV_LOCK))
               .with(condition().term(BlockStateProperties.SOUTH, true), multiVariant.with(Y_ROT_180).with(UV_LOCK))
               .with(condition().term(BlockStateProperties.WEST, true), multiVariant.with(Y_ROT_270).with(UV_LOCK))
               .with(condition().term(BlockStateProperties.UP, true), multiVariant.with(X_ROT_270).with(UV_LOCK))
               .with(condition().term(BlockStateProperties.DOWN, true), multiVariant.with(X_ROT_90).with(UV_LOCK))
               .with(
                  condition().term(BlockStateProperties.NORTH, false),
                  new MultiVariant(
                     WeightedList.of(new Weighted<>(variant, 2), new Weighted<>(variant2, 1), new Weighted<>(variant3, 1), new Weighted<>(variant4, 1))
                  )
               )
               .with(
                  condition().term(BlockStateProperties.EAST, false),
                  new MultiVariant(
                     WeightedList.of(
                        new Weighted<>(variant6.with(Y_ROT_90), 1),
                        new Weighted<>(variant7.with(Y_ROT_90), 1),
                        new Weighted<>(variant8.with(Y_ROT_90), 1),
                        new Weighted<>(variant5.with(Y_ROT_90), 2)
                     )
                  )
               )
               .with(
                  condition().term(BlockStateProperties.SOUTH, false),
                  new MultiVariant(
                     WeightedList.of(
                        new Weighted<>(variant7.with(Y_ROT_180), 1),
                        new Weighted<>(variant8.with(Y_ROT_180), 1),
                        new Weighted<>(variant5.with(Y_ROT_180), 2),
                        new Weighted<>(variant6.with(Y_ROT_180), 1)
                     )
                  )
               )
               .with(
                  condition().term(BlockStateProperties.WEST, false),
                  new MultiVariant(
                     WeightedList.of(
                        new Weighted<>(variant8.with(Y_ROT_270), 1),
                        new Weighted<>(variant5.with(Y_ROT_270), 2),
                        new Weighted<>(variant6.with(Y_ROT_270), 1),
                        new Weighted<>(variant7.with(Y_ROT_270), 1)
                     )
                  )
               )
               .with(
                  condition().term(BlockStateProperties.UP, false),
                  new MultiVariant(
                     WeightedList.of(
                        new Weighted<>(variant5.with(X_ROT_270), 2),
                        new Weighted<>(variant8.with(X_ROT_270), 1),
                        new Weighted<>(variant6.with(X_ROT_270), 1),
                        new Weighted<>(variant7.with(X_ROT_270), 1)
                     )
                  )
               )
               .with(
                  condition().term(BlockStateProperties.DOWN, false),
                  new MultiVariant(
                     WeightedList.of(
                        new Weighted<>(variant8.with(X_ROT_90), 1),
                        new Weighted<>(variant7.with(X_ROT_90), 1),
                        new Weighted<>(variant6.with(X_ROT_90), 1),
                        new Weighted<>(variant5.with(X_ROT_90), 2)
                     )
                  )
               )
         );
   }

   private void createComposter() {
      this.blockStateOutput
         .accept(
            MultiPartGenerator.multiPart(Blocks.COMPOSTER)
               .with(plainVariant(TextureMapping.getBlockTexture(Blocks.COMPOSTER)))
               .with(condition().term(BlockStateProperties.LEVEL_COMPOSTER, 1), plainVariant(TextureMapping.getBlockTexture(Blocks.COMPOSTER, "_contents1")))
               .with(condition().term(BlockStateProperties.LEVEL_COMPOSTER, 2), plainVariant(TextureMapping.getBlockTexture(Blocks.COMPOSTER, "_contents2")))
               .with(condition().term(BlockStateProperties.LEVEL_COMPOSTER, 3), plainVariant(TextureMapping.getBlockTexture(Blocks.COMPOSTER, "_contents3")))
               .with(condition().term(BlockStateProperties.LEVEL_COMPOSTER, 4), plainVariant(TextureMapping.getBlockTexture(Blocks.COMPOSTER, "_contents4")))
               .with(condition().term(BlockStateProperties.LEVEL_COMPOSTER, 5), plainVariant(TextureMapping.getBlockTexture(Blocks.COMPOSTER, "_contents5")))
               .with(condition().term(BlockStateProperties.LEVEL_COMPOSTER, 6), plainVariant(TextureMapping.getBlockTexture(Blocks.COMPOSTER, "_contents6")))
               .with(condition().term(BlockStateProperties.LEVEL_COMPOSTER, 7), plainVariant(TextureMapping.getBlockTexture(Blocks.COMPOSTER, "_contents7")))
               .with(
                  condition().term(BlockStateProperties.LEVEL_COMPOSTER, 8), plainVariant(TextureMapping.getBlockTexture(Blocks.COMPOSTER, "_contents_ready"))
               )
         );
   }

   private void createCopperBulb(Block bulbBlock) {
      MultiVariant multiVariant = plainVariant(ModelTemplates.CUBE_ALL.create(bulbBlock, TextureMapping.cube(bulbBlock), this.modelOutput));
      MultiVariant multiVariant2 = plainVariant(this.createSuffixedVariant(bulbBlock, "_powered", ModelTemplates.CUBE_ALL, TextureMapping::cube));
      MultiVariant multiVariant3 = plainVariant(this.createSuffixedVariant(bulbBlock, "_lit", ModelTemplates.CUBE_ALL, TextureMapping::cube));
      MultiVariant multiVariant4 = plainVariant(this.createSuffixedVariant(bulbBlock, "_lit_powered", ModelTemplates.CUBE_ALL, TextureMapping::cube));
      this.blockStateOutput.accept(createCopperBulb(bulbBlock, multiVariant, multiVariant3, multiVariant2, multiVariant4));
   }

   private static BlockModelDefinitionGenerator createCopperBulb(
      Block block, MultiVariant unlit, MultiVariant unlitPowered, MultiVariant lit, MultiVariant litPowered
   ) {
      return MultiVariantGenerator.dispatch(block)
         .with(PropertyDispatch.initial(BlockStateProperties.LIT, BlockStateProperties.POWERED).generate((boolean_, boolean2) -> {
            if (boolean_) {
               return boolean2 ? litPowered : unlitPowered;
            } else {
               return boolean2 ? lit : unlit;
            }
         }));
   }

   private void copyCopperBulbModel(Block bulbBlock, Block sourceBlock) {
      MultiVariant multiVariant = plainVariant(ModelLocationUtils.getModelLocation(bulbBlock));
      MultiVariant multiVariant2 = plainVariant(ModelLocationUtils.getModelLocation(bulbBlock, "_powered"));
      MultiVariant multiVariant3 = plainVariant(ModelLocationUtils.getModelLocation(bulbBlock, "_lit"));
      MultiVariant multiVariant4 = plainVariant(ModelLocationUtils.getModelLocation(bulbBlock, "_lit_powered"));
      this.itemModelOutput.copy(bulbBlock.asItem(), sourceBlock.asItem());
      this.blockStateOutput.accept(createCopperBulb(sourceBlock, multiVariant, multiVariant3, multiVariant2, multiVariant4));
   }

   private void createAmethystCluster(Block amethystBlock) {
      MultiVariant multiVariant = plainVariant(ModelTemplates.CROSS.create(amethystBlock, TextureMapping.cross(amethystBlock), this.modelOutput));
      this.blockStateOutput.accept(MultiVariantGenerator.dispatch(amethystBlock, multiVariant).with(ROTATIONS_COLUMN_WITH_FACING));
   }

   private void createAmethystClusters() {
      this.createAmethystCluster(Blocks.SMALL_AMETHYST_BUD);
      this.createAmethystCluster(Blocks.MEDIUM_AMETHYST_BUD);
      this.createAmethystCluster(Blocks.LARGE_AMETHYST_BUD);
      this.createAmethystCluster(Blocks.AMETHYST_CLUSTER);
   }

   private void createPointedDripstone() {
      PropertyDispatch.C2<MultiVariant, Direction, DripstoneThickness> c2 = PropertyDispatch.initial(
         BlockStateProperties.VERTICAL_DIRECTION, BlockStateProperties.DRIPSTONE_THICKNESS
      );

      for (DripstoneThickness dripstoneThickness : DripstoneThickness.values()) {
         c2.select(Direction.UP, dripstoneThickness, this.createPointedDripstoneVariant(Direction.UP, dripstoneThickness));
      }

      for (DripstoneThickness dripstoneThickness : DripstoneThickness.values()) {
         c2.select(Direction.DOWN, dripstoneThickness, this.createPointedDripstoneVariant(Direction.DOWN, dripstoneThickness));
      }

      this.blockStateOutput.accept(MultiVariantGenerator.dispatch(Blocks.POINTED_DRIPSTONE).with(c2));
   }

   private MultiVariant createPointedDripstoneVariant(Direction direction, DripstoneThickness thickness) {
      String string = "_" + direction.getSerializedName() + "_" + thickness.getSerializedName();
      TextureMapping textureMapping = TextureMapping.cross(TextureMapping.getBlockTexture(Blocks.POINTED_DRIPSTONE, string));
      return plainVariant(ModelTemplates.POINTED_DRIPSTONE.createWithSuffix(Blocks.POINTED_DRIPSTONE, string, textureMapping, this.modelOutput));
   }

   private void createNyliumBlock(Block nyliumBlock) {
      TextureMapping textureMapping = new TextureMapping()
         .put(TextureSlot.BOTTOM, TextureMapping.getBlockTexture(Blocks.NETHERRACK))
         .put(TextureSlot.TOP, TextureMapping.getBlockTexture(nyliumBlock))
         .put(TextureSlot.SIDE, TextureMapping.getBlockTexture(nyliumBlock, "_side"));
      this.blockStateOutput
         .accept(createSimpleBlock(nyliumBlock, plainVariant(ModelTemplates.CUBE_BOTTOM_TOP.create(nyliumBlock, textureMapping, this.modelOutput))));
   }

   private void createDaylightDetector() {
      Identifier identifier = TextureMapping.getBlockTexture(Blocks.DAYLIGHT_DETECTOR, "_side");
      TextureMapping textureMapping = new TextureMapping()
         .put(TextureSlot.TOP, TextureMapping.getBlockTexture(Blocks.DAYLIGHT_DETECTOR, "_top"))
         .put(TextureSlot.SIDE, identifier);
      TextureMapping textureMapping2 = new TextureMapping()
         .put(TextureSlot.TOP, TextureMapping.getBlockTexture(Blocks.DAYLIGHT_DETECTOR, "_inverted_top"))
         .put(TextureSlot.SIDE, identifier);
      this.blockStateOutput
         .accept(
            MultiVariantGenerator.dispatch(Blocks.DAYLIGHT_DETECTOR)
               .with(
                  PropertyDispatch.initial(BlockStateProperties.INVERTED)
                     .select(false, plainVariant(ModelTemplates.DAYLIGHT_DETECTOR.create(Blocks.DAYLIGHT_DETECTOR, textureMapping, this.modelOutput)))
                     .select(
                        true,
                        plainVariant(
                           ModelTemplates.DAYLIGHT_DETECTOR
                              .create(ModelLocationUtils.getModelLocation(Blocks.DAYLIGHT_DETECTOR, "_inverted"), textureMapping2, this.modelOutput)
                        )
                     )
               )
         );
   }

   private void createRotatableColumn(Block rotatableColumnBlock) {
      this.blockStateOutput
         .accept(
            MultiVariantGenerator.dispatch(rotatableColumnBlock, plainVariant(ModelLocationUtils.getModelLocation(rotatableColumnBlock)))
               .with(ROTATIONS_COLUMN_WITH_FACING)
         );
   }

   private void createLightningRod(Block block, Block waxedBlock) {
      MultiVariant multiVariant = plainVariant(ModelLocationUtils.getModelLocation(Blocks.LIGHTNING_ROD, "_on"));
      MultiVariant multiVariant2 = plainVariant(ModelTemplates.LIGHTNING_ROD.create(block, TextureMapping.defaultTexture(block), this.modelOutput));
      this.blockStateOutput
         .accept(
            MultiVariantGenerator.dispatch(block)
               .with(createBooleanModelDispatch(BlockStateProperties.POWERED, multiVariant, multiVariant2))
               .with(ROTATIONS_COLUMN_WITH_FACING)
         );
      this.blockStateOutput
         .accept(
            MultiVariantGenerator.dispatch(waxedBlock)
               .with(createBooleanModelDispatch(BlockStateProperties.POWERED, multiVariant, multiVariant2))
               .with(ROTATIONS_COLUMN_WITH_FACING)
         );
      this.itemModelOutput.copy(block.asItem(), waxedBlock.asItem());
   }

   private void createFarmland() {
      TextureMapping textureMapping = new TextureMapping()
         .put(TextureSlot.DIRT, TextureMapping.getBlockTexture(Blocks.DIRT))
         .put(TextureSlot.TOP, TextureMapping.getBlockTexture(Blocks.FARMLAND));
      TextureMapping textureMapping2 = new TextureMapping()
         .put(TextureSlot.DIRT, TextureMapping.getBlockTexture(Blocks.DIRT))
         .put(TextureSlot.TOP, TextureMapping.getBlockTexture(Blocks.FARMLAND, "_moist"));
      MultiVariant multiVariant = plainVariant(ModelTemplates.FARMLAND.create(Blocks.FARMLAND, textureMapping, this.modelOutput));
      MultiVariant multiVariant2 = plainVariant(
         ModelTemplates.FARMLAND.create(TextureMapping.getBlockTexture(Blocks.FARMLAND, "_moist"), textureMapping2, this.modelOutput)
      );
      this.blockStateOutput
         .accept(MultiVariantGenerator.dispatch(Blocks.FARMLAND).with(createEmptyOrFullDispatch(BlockStateProperties.MOISTURE, 7, multiVariant2, multiVariant)));
   }

   private MultiVariant createFloorFireModels(Block block) {
      return variants(
         plainModel(ModelTemplates.FIRE_FLOOR.create(ModelLocationUtils.getModelLocation(block, "_floor0"), TextureMapping.fire0(block), this.modelOutput)),
         plainModel(ModelTemplates.FIRE_FLOOR.create(ModelLocationUtils.getModelLocation(block, "_floor1"), TextureMapping.fire1(block), this.modelOutput))
      );
   }

   private MultiVariant createSideFireModels(Block block) {
      return variants(
         plainModel(ModelTemplates.FIRE_SIDE.create(ModelLocationUtils.getModelLocation(block, "_side0"), TextureMapping.fire0(block), this.modelOutput)),
         plainModel(ModelTemplates.FIRE_SIDE.create(ModelLocationUtils.getModelLocation(block, "_side1"), TextureMapping.fire1(block), this.modelOutput)),
         plainModel(
            ModelTemplates.FIRE_SIDE_ALT.create(ModelLocationUtils.getModelLocation(block, "_side_alt0"), TextureMapping.fire0(block), this.modelOutput)
         ),
         plainModel(
            ModelTemplates.FIRE_SIDE_ALT.create(ModelLocationUtils.getModelLocation(block, "_side_alt1"), TextureMapping.fire1(block), this.modelOutput)
         )
      );
   }

   private MultiVariant createTopFireModels(Block block) {
      return variants(
         plainModel(ModelTemplates.FIRE_UP.create(ModelLocationUtils.getModelLocation(block, "_up0"), TextureMapping.fire0(block), this.modelOutput)),
         plainModel(ModelTemplates.FIRE_UP.create(ModelLocationUtils.getModelLocation(block, "_up1"), TextureMapping.fire1(block), this.modelOutput)),
         plainModel(ModelTemplates.FIRE_UP_ALT.create(ModelLocationUtils.getModelLocation(block, "_up_alt0"), TextureMapping.fire0(block), this.modelOutput)),
         plainModel(ModelTemplates.FIRE_UP_ALT.create(ModelLocationUtils.getModelLocation(block, "_up_alt1"), TextureMapping.fire1(block), this.modelOutput))
      );
   }

   private void createFire() {
      ConditionBuilder conditionBuilder = condition()
         .term(BlockStateProperties.NORTH, false)
         .term(BlockStateProperties.EAST, false)
         .term(BlockStateProperties.SOUTH, false)
         .term(BlockStateProperties.WEST, false)
         .term(BlockStateProperties.UP, false);
      MultiVariant multiVariant = this.createFloorFireModels(Blocks.FIRE);
      MultiVariant multiVariant2 = this.createSideFireModels(Blocks.FIRE);
      MultiVariant multiVariant3 = this.createTopFireModels(Blocks.FIRE);
      this.blockStateOutput
         .accept(
            MultiPartGenerator.multiPart(Blocks.FIRE)
               .with(conditionBuilder, multiVariant)
               .with(or(condition().term(BlockStateProperties.NORTH, true), conditionBuilder), multiVariant2)
               .with(or(condition().term(BlockStateProperties.EAST, true), conditionBuilder), multiVariant2.with(Y_ROT_90))
               .with(or(condition().term(BlockStateProperties.SOUTH, true), conditionBuilder), multiVariant2.with(Y_ROT_180))
               .with(or(condition().term(BlockStateProperties.WEST, true), conditionBuilder), multiVariant2.with(Y_ROT_270))
               .with(condition().term(BlockStateProperties.UP, true), multiVariant3)
         );
   }

   private void createSoulFire() {
      MultiVariant multiVariant = this.createFloorFireModels(Blocks.SOUL_FIRE);
      MultiVariant multiVariant2 = this.createSideFireModels(Blocks.SOUL_FIRE);
      this.blockStateOutput
         .accept(
            MultiPartGenerator.multiPart(Blocks.SOUL_FIRE)
               .with(multiVariant)
               .with(multiVariant2)
               .with(multiVariant2.with(Y_ROT_90))
               .with(multiVariant2.with(Y_ROT_180))
               .with(multiVariant2.with(Y_ROT_270))
         );
   }

   private void createLantern(Block lanternBlock) {
      MultiVariant multiVariant = plainVariant(TexturedModel.LANTERN.create(lanternBlock, this.modelOutput));
      MultiVariant multiVariant2 = plainVariant(TexturedModel.HANGING_LANTERN.create(lanternBlock, this.modelOutput));
      this.registerSimpleFlatItemModel(lanternBlock.asItem());
      this.blockStateOutput
         .accept(MultiVariantGenerator.dispatch(lanternBlock).with(createBooleanModelDispatch(BlockStateProperties.HANGING, multiVariant2, multiVariant)));
   }

   private void createCopperLantern(Block lanternBlock, Block waxed) {
      Identifier identifier = TexturedModel.LANTERN.create(lanternBlock, this.modelOutput);
      Identifier identifier2 = TexturedModel.HANGING_LANTERN.create(lanternBlock, this.modelOutput);
      this.registerSimpleFlatItemModel(lanternBlock.asItem());
      this.itemModelOutput.copy(lanternBlock.asItem(), waxed.asItem());
      this.blockStateOutput
         .accept(
            MultiVariantGenerator.dispatch(lanternBlock)
               .with(createBooleanModelDispatch(BlockStateProperties.HANGING, plainVariant(identifier2), plainVariant(identifier)))
         );
      this.blockStateOutput
         .accept(
            MultiVariantGenerator.dispatch(waxed)
               .with(createBooleanModelDispatch(BlockStateProperties.HANGING, plainVariant(identifier2), plainVariant(identifier)))
         );
   }

   private void createCopperChain(Block chain, Block waxed) {
      MultiVariant multiVariant = plainVariant(TexturedModel.CHAIN.create(chain, this.modelOutput));
      this.createAxisAlignedPillarBlockCustomModel(chain, multiVariant);
      this.createAxisAlignedPillarBlockCustomModel(waxed, multiVariant);
   }

   private void createMuddyMangroveRoots() {
      TextureMapping textureMapping = TextureMapping.column(
         TextureMapping.getBlockTexture(Blocks.MUDDY_MANGROVE_ROOTS, "_side"), TextureMapping.getBlockTexture(Blocks.MUDDY_MANGROVE_ROOTS, "_top")
      );
      MultiVariant multiVariant = plainVariant(ModelTemplates.CUBE_COLUMN.create(Blocks.MUDDY_MANGROVE_ROOTS, textureMapping, this.modelOutput));
      this.blockStateOutput.accept(createAxisAlignedPillarBlock(Blocks.MUDDY_MANGROVE_ROOTS, multiVariant));
   }

   private void createMangrovePropagule() {
      this.registerSimpleFlatItemModel(Items.MANGROVE_PROPAGULE);
      Block block = Blocks.MANGROVE_PROPAGULE;
      MultiVariant multiVariant = plainVariant(ModelLocationUtils.getModelLocation(block));
      this.blockStateOutput
         .accept(
            MultiVariantGenerator.dispatch(Blocks.MANGROVE_PROPAGULE)
               .with(
                  PropertyDispatch.initial(MangrovePropaguleBlock.HANGING, MangrovePropaguleBlock.AGE)
                     .generate((boolean_, integer) -> boolean_ ? plainVariant(ModelLocationUtils.getModelLocation(block, "_hanging_" + integer)) : multiVariant)
               )
         );
   }

   private void createFrostedIce() {
      this.blockStateOutput
         .accept(
            MultiVariantGenerator.dispatch(Blocks.FROSTED_ICE)
               .with(
                  PropertyDispatch.initial(BlockStateProperties.AGE_3)
                     .select(0, plainVariant(this.createSuffixedVariant(Blocks.FROSTED_ICE, "_0", ModelTemplates.CUBE_ALL, TextureMapping::cube)))
                     .select(1, plainVariant(this.createSuffixedVariant(Blocks.FROSTED_ICE, "_1", ModelTemplates.CUBE_ALL, TextureMapping::cube)))
                     .select(2, plainVariant(this.createSuffixedVariant(Blocks.FROSTED_ICE, "_2", ModelTemplates.CUBE_ALL, TextureMapping::cube)))
                     .select(3, plainVariant(this.createSuffixedVariant(Blocks.FROSTED_ICE, "_3", ModelTemplates.CUBE_ALL, TextureMapping::cube)))
               )
         );
   }

   private void createGrassBlocks() {
      Identifier identifier = TextureMapping.getBlockTexture(Blocks.DIRT);
      TextureMapping textureMapping = new TextureMapping()
         .put(TextureSlot.BOTTOM, identifier)
         .copyForced(TextureSlot.BOTTOM, TextureSlot.PARTICLE)
         .put(TextureSlot.TOP, TextureMapping.getBlockTexture(Blocks.GRASS_BLOCK, "_top"))
         .put(TextureSlot.SIDE, TextureMapping.getBlockTexture(Blocks.GRASS_BLOCK, "_snow"));
      MultiVariant multiVariant = plainVariant(ModelTemplates.CUBE_BOTTOM_TOP.createWithSuffix(Blocks.GRASS_BLOCK, "_snow", textureMapping, this.modelOutput));
      Identifier identifier2 = ModelLocationUtils.getModelLocation(Blocks.GRASS_BLOCK);
      this.createGrassLikeBlock(Blocks.GRASS_BLOCK, createRotatedVariants(plainModel(identifier2)), multiVariant);
      this.registerSimpleTintedItemModel(Blocks.GRASS_BLOCK, identifier2, new GrassColorSource());
      MultiVariant multiVariant2 = createRotatedVariants(
         plainModel(
            TexturedModel.CUBE_TOP_BOTTOM
               .get(Blocks.MYCELIUM)
               .updateTextures(textureMappingx -> textureMappingx.put(TextureSlot.BOTTOM, identifier))
               .create(Blocks.MYCELIUM, this.modelOutput)
         )
      );
      this.createGrassLikeBlock(Blocks.MYCELIUM, multiVariant2, multiVariant);
      MultiVariant multiVariant3 = createRotatedVariants(
         plainModel(
            TexturedModel.CUBE_TOP_BOTTOM
               .get(Blocks.PODZOL)
               .updateTextures(textureMappingx -> textureMappingx.put(TextureSlot.BOTTOM, identifier))
               .create(Blocks.PODZOL, this.modelOutput)
         )
      );
      this.createGrassLikeBlock(Blocks.PODZOL, multiVariant3, multiVariant);
   }

   private void createGrassLikeBlock(Block block, MultiVariant variants, MultiVariant snowyVariants) {
      this.blockStateOutput
         .accept(
            MultiVariantGenerator.dispatch(block)
               .with(PropertyDispatch.initial(BlockStateProperties.SNOWY).select(true, snowyVariants).select(false, variants))
         );
   }

   private void createCocoa() {
      this.registerSimpleFlatItemModel(Items.COCOA_BEANS);
      this.blockStateOutput
         .accept(
            MultiVariantGenerator.dispatch(Blocks.COCOA)
               .with(
                  PropertyDispatch.initial(BlockStateProperties.AGE_2)
                     .select(0, plainVariant(ModelLocationUtils.getModelLocation(Blocks.COCOA, "_stage0")))
                     .select(1, plainVariant(ModelLocationUtils.getModelLocation(Blocks.COCOA, "_stage1")))
                     .select(2, plainVariant(ModelLocationUtils.getModelLocation(Blocks.COCOA, "_stage2")))
               )
               .with(ROTATION_HORIZONTAL_FACING_ALT)
         );
   }

   private void createDirtPath() {
      Variant variant = plainModel(ModelLocationUtils.getModelLocation(Blocks.DIRT_PATH));
      this.blockStateOutput.accept(MultiVariantGenerator.dispatch(Blocks.DIRT_PATH, createRotatedVariants(variant)));
   }

   private void createWeightedPressurePlate(Block pressurePlateBlock, Block plateMaterialBlock) {
      TextureMapping textureMapping = TextureMapping.defaultTexture(plateMaterialBlock);
      MultiVariant multiVariant = plainVariant(ModelTemplates.PRESSURE_PLATE_UP.create(pressurePlateBlock, textureMapping, this.modelOutput));
      MultiVariant multiVariant2 = plainVariant(ModelTemplates.PRESSURE_PLATE_DOWN.create(pressurePlateBlock, textureMapping, this.modelOutput));
      this.blockStateOutput
         .accept(MultiVariantGenerator.dispatch(pressurePlateBlock).with(createEmptyOrFullDispatch(BlockStateProperties.POWER, 1, multiVariant2, multiVariant)));
   }

   private void createHopper() {
      MultiVariant multiVariant = plainVariant(ModelLocationUtils.getModelLocation(Blocks.HOPPER));
      MultiVariant multiVariant2 = plainVariant(ModelLocationUtils.getModelLocation(Blocks.HOPPER, "_side"));
      this.registerSimpleFlatItemModel(Items.HOPPER);
      this.blockStateOutput
         .accept(
            MultiVariantGenerator.dispatch(Blocks.HOPPER)
               .with(
                  PropertyDispatch.initial(BlockStateProperties.FACING_HOPPER)
                     .select(Direction.DOWN, multiVariant)
                     .select(Direction.NORTH, multiVariant2)
                     .select(Direction.EAST, multiVariant2.with(Y_ROT_90))
                     .select(Direction.SOUTH, multiVariant2.with(Y_ROT_180))
                     .select(Direction.WEST, multiVariant2.with(Y_ROT_270))
               )
         );
   }

   private void copyModel(Block sourceBlock, Block targetBlock) {
      MultiVariant multiVariant = plainVariant(ModelLocationUtils.getModelLocation(sourceBlock));
      this.blockStateOutput.accept(MultiVariantGenerator.dispatch(targetBlock, multiVariant));
      this.itemModelOutput.copy(sourceBlock.asItem(), targetBlock.asItem());
   }

   private void createBarsAndItem(Block block) {
      TextureMapping textureMapping = TextureMapping.bars(block);
      this.createBars(
         block,
         ModelTemplates.BARS_POST_ENDS.create(block, textureMapping, this.modelOutput),
         ModelTemplates.BARS_POST.create(block, textureMapping, this.modelOutput),
         ModelTemplates.BARS_CAP.create(block, textureMapping, this.modelOutput),
         ModelTemplates.BARS_CAP_ALT.create(block, textureMapping, this.modelOutput),
         ModelTemplates.BARS_POST_SIDE.create(block, textureMapping, this.modelOutput),
         ModelTemplates.BARS_POST_SIDE_ALT.create(block, textureMapping, this.modelOutput)
      );
      this.registerSimpleFlatItemModel(block);
   }

   private void createBarsAndItem(Block block, Block waxedBlock) {
      TextureMapping textureMapping = TextureMapping.bars(block);
      Identifier identifier = ModelTemplates.BARS_POST_ENDS.create(block, textureMapping, this.modelOutput);
      Identifier identifier2 = ModelTemplates.BARS_POST.create(block, textureMapping, this.modelOutput);
      Identifier identifier3 = ModelTemplates.BARS_CAP.create(block, textureMapping, this.modelOutput);
      Identifier identifier4 = ModelTemplates.BARS_CAP_ALT.create(block, textureMapping, this.modelOutput);
      Identifier identifier5 = ModelTemplates.BARS_POST_SIDE.create(block, textureMapping, this.modelOutput);
      Identifier identifier6 = ModelTemplates.BARS_POST_SIDE_ALT.create(block, textureMapping, this.modelOutput);
      this.createBars(block, identifier, identifier2, identifier3, identifier4, identifier5, identifier6);
      this.createBars(waxedBlock, identifier, identifier2, identifier3, identifier4, identifier5, identifier6);
      this.registerSimpleFlatItemModel(block);
      this.itemModelOutput.copy(block.asItem(), waxedBlock.asItem());
   }

   private void createBars(Block block, Identifier postEnds, Identifier post, Identifier cap, Identifier capAlt, Identifier postSide, Identifier postSideAlt) {
      MultiVariant multiVariant = plainVariant(postEnds);
      MultiVariant multiVariant2 = plainVariant(post);
      MultiVariant multiVariant3 = plainVariant(cap);
      MultiVariant multiVariant4 = plainVariant(capAlt);
      MultiVariant multiVariant5 = plainVariant(postSide);
      MultiVariant multiVariant6 = plainVariant(postSideAlt);
      this.blockStateOutput
         .accept(
            MultiPartGenerator.multiPart(block)
               .with(multiVariant)
               .with(
                  condition()
                     .term(BlockStateProperties.NORTH, false)
                     .term(BlockStateProperties.EAST, false)
                     .term(BlockStateProperties.SOUTH, false)
                     .term(BlockStateProperties.WEST, false),
                  multiVariant2
               )
               .with(
                  condition()
                     .term(BlockStateProperties.NORTH, true)
                     .term(BlockStateProperties.EAST, false)
                     .term(BlockStateProperties.SOUTH, false)
                     .term(BlockStateProperties.WEST, false),
                  multiVariant3
               )
               .with(
                  condition()
                     .term(BlockStateProperties.NORTH, false)
                     .term(BlockStateProperties.EAST, true)
                     .term(BlockStateProperties.SOUTH, false)
                     .term(BlockStateProperties.WEST, false),
                  multiVariant3.with(Y_ROT_90)
               )
               .with(
                  condition()
                     .term(BlockStateProperties.NORTH, false)
                     .term(BlockStateProperties.EAST, false)
                     .term(BlockStateProperties.SOUTH, true)
                     .term(BlockStateProperties.WEST, false),
                  multiVariant4
               )
               .with(
                  condition()
                     .term(BlockStateProperties.NORTH, false)
                     .term(BlockStateProperties.EAST, false)
                     .term(BlockStateProperties.SOUTH, false)
                     .term(BlockStateProperties.WEST, true),
                  multiVariant4.with(Y_ROT_90)
               )
               .with(condition().term(BlockStateProperties.NORTH, true), multiVariant5)
               .with(condition().term(BlockStateProperties.EAST, true), multiVariant5.with(Y_ROT_90))
               .with(condition().term(BlockStateProperties.SOUTH, true), multiVariant6)
               .with(condition().term(BlockStateProperties.WEST, true), multiVariant6.with(Y_ROT_90))
         );
   }

   private void createNonTemplateHorizontalBlock(Block horizontalBlock) {
      this.blockStateOutput
         .accept(
            MultiVariantGenerator.dispatch(horizontalBlock, plainVariant(ModelLocationUtils.getModelLocation(horizontalBlock)))
               .with(ROTATION_HORIZONTAL_FACING)
         );
   }

   private void createLever() {
      MultiVariant multiVariant = plainVariant(ModelLocationUtils.getModelLocation(Blocks.LEVER));
      MultiVariant multiVariant2 = plainVariant(ModelLocationUtils.getModelLocation(Blocks.LEVER, "_on"));
      this.registerSimpleFlatItemModel(Blocks.LEVER);
      this.blockStateOutput
         .accept(
            MultiVariantGenerator.dispatch(Blocks.LEVER)
               .with(createBooleanModelDispatch(BlockStateProperties.POWERED, multiVariant, multiVariant2))
               .with(
                  PropertyDispatch.modify(BlockStateProperties.ATTACH_FACE, BlockStateProperties.HORIZONTAL_FACING)
                     .select(AttachFace.CEILING, Direction.NORTH, X_ROT_180.then(Y_ROT_180))
                     .select(AttachFace.CEILING, Direction.EAST, X_ROT_180.then(Y_ROT_270))
                     .select(AttachFace.CEILING, Direction.SOUTH, X_ROT_180)
                     .select(AttachFace.CEILING, Direction.WEST, X_ROT_180.then(Y_ROT_90))
                     .select(AttachFace.FLOOR, Direction.NORTH, NOP)
                     .select(AttachFace.FLOOR, Direction.EAST, Y_ROT_90)
                     .select(AttachFace.FLOOR, Direction.SOUTH, Y_ROT_180)
                     .select(AttachFace.FLOOR, Direction.WEST, Y_ROT_270)
                     .select(AttachFace.WALL, Direction.NORTH, X_ROT_90)
                     .select(AttachFace.WALL, Direction.EAST, X_ROT_90.then(Y_ROT_90))
                     .select(AttachFace.WALL, Direction.SOUTH, X_ROT_90.then(Y_ROT_180))
                     .select(AttachFace.WALL, Direction.WEST, X_ROT_90.then(Y_ROT_270))
               )
         );
   }

   private void createLilyPad() {
      Identifier identifier = this.createFlatItemModelWithBlockTexture(Items.LILY_PAD, Blocks.LILY_PAD);
      this.registerSimpleTintedItemModel(Blocks.LILY_PAD, identifier, ItemModelUtils.constantTint(-9321636));
      Variant variant = plainModel(ModelLocationUtils.getModelLocation(Blocks.LILY_PAD));
      this.blockStateOutput.accept(MultiVariantGenerator.dispatch(Blocks.LILY_PAD, createRotatedVariants(variant)));
   }

   private void createFrogspawnBlock() {
      this.registerSimpleFlatItemModel(Blocks.FROGSPAWN);
      this.blockStateOutput.accept(createSimpleBlock(Blocks.FROGSPAWN, plainVariant(ModelLocationUtils.getModelLocation(Blocks.FROGSPAWN))));
   }

   private void createNetherPortalBlock() {
      this.blockStateOutput
         .accept(
            MultiVariantGenerator.dispatch(Blocks.NETHER_PORTAL)
               .with(
                  PropertyDispatch.initial(BlockStateProperties.HORIZONTAL_AXIS)
                     .select(Direction.Axis.X, plainVariant(ModelLocationUtils.getModelLocation(Blocks.NETHER_PORTAL, "_ns")))
                     .select(Direction.Axis.Z, plainVariant(ModelLocationUtils.getModelLocation(Blocks.NETHER_PORTAL, "_ew")))
               )
         );
   }

   private void createNetherrack() {
      Variant variant = plainModel(TexturedModel.CUBE.create(Blocks.NETHERRACK, this.modelOutput));
      this.blockStateOutput
         .accept(
            MultiVariantGenerator.dispatch(
               Blocks.NETHERRACK,
               variants(
                  variant,
                  variant.with(X_ROT_90),
                  variant.with(X_ROT_180),
                  variant.with(X_ROT_270),
                  variant.with(Y_ROT_90),
                  variant.with(Y_ROT_90.then(X_ROT_90)),
                  variant.with(Y_ROT_90.then(X_ROT_180)),
                  variant.with(Y_ROT_90.then(X_ROT_270)),
                  variant.with(Y_ROT_180),
                  variant.with(Y_ROT_180.then(X_ROT_90)),
                  variant.with(Y_ROT_180.then(X_ROT_180)),
                  variant.with(Y_ROT_180.then(X_ROT_270)),
                  variant.with(Y_ROT_270),
                  variant.with(Y_ROT_270.then(X_ROT_90)),
                  variant.with(Y_ROT_270.then(X_ROT_180)),
                  variant.with(Y_ROT_270.then(X_ROT_270))
               )
            )
         );
   }

   private void createObserver() {
      MultiVariant multiVariant = plainVariant(ModelLocationUtils.getModelLocation(Blocks.OBSERVER));
      MultiVariant multiVariant2 = plainVariant(ModelLocationUtils.getModelLocation(Blocks.OBSERVER, "_on"));
      this.blockStateOutput
         .accept(
            MultiVariantGenerator.dispatch(Blocks.OBSERVER)
               .with(createBooleanModelDispatch(BlockStateProperties.POWERED, multiVariant2, multiVariant))
               .with(ROTATION_FACING)
         );
   }

   private void createPistons() {
      TextureMapping textureMapping = new TextureMapping()
         .put(TextureSlot.BOTTOM, TextureMapping.getBlockTexture(Blocks.PISTON, "_bottom"))
         .put(TextureSlot.SIDE, TextureMapping.getBlockTexture(Blocks.PISTON, "_side"));
      Identifier identifier = TextureMapping.getBlockTexture(Blocks.PISTON, "_top_sticky");
      Identifier identifier2 = TextureMapping.getBlockTexture(Blocks.PISTON, "_top");
      TextureMapping textureMapping2 = textureMapping.copyAndUpdate(TextureSlot.PLATFORM, identifier);
      TextureMapping textureMapping3 = textureMapping.copyAndUpdate(TextureSlot.PLATFORM, identifier2);
      MultiVariant multiVariant = plainVariant(ModelLocationUtils.getModelLocation(Blocks.PISTON, "_base"));
      this.createPistonVariant(Blocks.PISTON, multiVariant, textureMapping3);
      this.createPistonVariant(Blocks.STICKY_PISTON, multiVariant, textureMapping2);
      Identifier identifier3 = ModelTemplates.CUBE_BOTTOM_TOP
         .createWithSuffix(Blocks.PISTON, "_inventory", textureMapping.copyAndUpdate(TextureSlot.TOP, identifier2), this.modelOutput);
      Identifier identifier4 = ModelTemplates.CUBE_BOTTOM_TOP
         .createWithSuffix(Blocks.STICKY_PISTON, "_inventory", textureMapping.copyAndUpdate(TextureSlot.TOP, identifier), this.modelOutput);
      this.registerSimpleItemModel(Blocks.PISTON, identifier3);
      this.registerSimpleItemModel(Blocks.STICKY_PISTON, identifier4);
   }

   private void createPistonVariant(Block block, MultiVariant variants, TextureMapping topTextureMapping) {
      MultiVariant multiVariant = plainVariant(ModelTemplates.PISTON.create(block, topTextureMapping, this.modelOutput));
      this.blockStateOutput
         .accept(
            MultiVariantGenerator.dispatch(block).with(createBooleanModelDispatch(BlockStateProperties.EXTENDED, variants, multiVariant)).with(ROTATION_FACING)
         );
   }

   private void createPistonHeads() {
      TextureMapping textureMapping = new TextureMapping()
         .put(TextureSlot.UNSTICKY, TextureMapping.getBlockTexture(Blocks.PISTON, "_top"))
         .put(TextureSlot.SIDE, TextureMapping.getBlockTexture(Blocks.PISTON, "_side"));
      TextureMapping textureMapping2 = textureMapping.copyAndUpdate(TextureSlot.PLATFORM, TextureMapping.getBlockTexture(Blocks.PISTON, "_top_sticky"));
      TextureMapping textureMapping3 = textureMapping.copyAndUpdate(TextureSlot.PLATFORM, TextureMapping.getBlockTexture(Blocks.PISTON, "_top"));
      this.blockStateOutput
         .accept(
            MultiVariantGenerator.dispatch(Blocks.PISTON_HEAD)
               .with(
                  PropertyDispatch.initial(BlockStateProperties.SHORT, BlockStateProperties.PISTON_TYPE)
                     .select(
                        false,
                        PistonType.DEFAULT,
                        plainVariant(ModelTemplates.PISTON_HEAD.createWithSuffix(Blocks.PISTON, "_head", textureMapping3, this.modelOutput))
                     )
                     .select(
                        false,
                        PistonType.STICKY,
                        plainVariant(ModelTemplates.PISTON_HEAD.createWithSuffix(Blocks.PISTON, "_head_sticky", textureMapping2, this.modelOutput))
                     )
                     .select(
                        true,
                        PistonType.DEFAULT,
                        plainVariant(ModelTemplates.PISTON_HEAD_SHORT.createWithSuffix(Blocks.PISTON, "_head_short", textureMapping3, this.modelOutput))
                     )
                     .select(
                        true,
                        PistonType.STICKY,
                        plainVariant(ModelTemplates.PISTON_HEAD_SHORT.createWithSuffix(Blocks.PISTON, "_head_short_sticky", textureMapping2, this.modelOutput))
                     )
               )
               .with(ROTATION_FACING)
         );
   }

   private void createTrialSpawner() {
      Block block = Blocks.TRIAL_SPAWNER;
      TextureMapping textureMapping = TextureMapping.trialSpawner(block, "_side_inactive", "_top_inactive");
      TextureMapping textureMapping2 = TextureMapping.trialSpawner(block, "_side_active", "_top_active");
      TextureMapping textureMapping3 = TextureMapping.trialSpawner(block, "_side_active", "_top_ejecting_reward");
      TextureMapping textureMapping4 = TextureMapping.trialSpawner(block, "_side_inactive_ominous", "_top_inactive_ominous");
      TextureMapping textureMapping5 = TextureMapping.trialSpawner(block, "_side_active_ominous", "_top_active_ominous");
      TextureMapping textureMapping6 = TextureMapping.trialSpawner(block, "_side_active_ominous", "_top_ejecting_reward_ominous");
      Identifier identifier = ModelTemplates.CUBE_BOTTOM_TOP_INNER_FACES.create(block, textureMapping, this.modelOutput);
      MultiVariant multiVariant = plainVariant(identifier);
      MultiVariant multiVariant2 = plainVariant(
         ModelTemplates.CUBE_BOTTOM_TOP_INNER_FACES.createWithSuffix(block, "_active", textureMapping2, this.modelOutput)
      );
      MultiVariant multiVariant3 = plainVariant(
         ModelTemplates.CUBE_BOTTOM_TOP_INNER_FACES.createWithSuffix(block, "_ejecting_reward", textureMapping3, this.modelOutput)
      );
      MultiVariant multiVariant4 = plainVariant(
         ModelTemplates.CUBE_BOTTOM_TOP_INNER_FACES.createWithSuffix(block, "_inactive_ominous", textureMapping4, this.modelOutput)
      );
      MultiVariant multiVariant5 = plainVariant(
         ModelTemplates.CUBE_BOTTOM_TOP_INNER_FACES.createWithSuffix(block, "_active_ominous", textureMapping5, this.modelOutput)
      );
      MultiVariant multiVariant6 = plainVariant(
         ModelTemplates.CUBE_BOTTOM_TOP_INNER_FACES.createWithSuffix(block, "_ejecting_reward_ominous", textureMapping6, this.modelOutput)
      );
      this.registerSimpleItemModel(block, identifier);
      this.blockStateOutput
         .accept(
            MultiVariantGenerator.dispatch(block)
               .with(
                  PropertyDispatch.initial(BlockStateProperties.TRIAL_SPAWNER_STATE, BlockStateProperties.OMINOUS).generate((trialSpawnerState, boolean_) -> {
                     return switch (trialSpawnerState) {
                        case INACTIVE, COOLDOWN -> boolean_ ? multiVariant4 : multiVariant;
                        case WAITING_FOR_PLAYERS, ACTIVE, WAITING_FOR_REWARD_EJECTION -> boolean_ ? multiVariant5 : multiVariant2;
                        case EJECTING_REWARD -> boolean_ ? multiVariant6 : multiVariant3;
                     };
                  })
               )
         );
   }

   private void createVault() {
      Block block = Blocks.VAULT;
      TextureMapping textureMapping = TextureMapping.vault(block, "_front_off", "_side_off", "_top", "_bottom");
      TextureMapping textureMapping2 = TextureMapping.vault(block, "_front_on", "_side_on", "_top", "_bottom");
      TextureMapping textureMapping3 = TextureMapping.vault(block, "_front_ejecting", "_side_on", "_top", "_bottom");
      TextureMapping textureMapping4 = TextureMapping.vault(block, "_front_ejecting", "_side_on", "_top_ejecting", "_bottom");
      Identifier identifier = ModelTemplates.VAULT.create(block, textureMapping, this.modelOutput);
      MultiVariant multiVariant = plainVariant(identifier);
      MultiVariant multiVariant2 = plainVariant(ModelTemplates.VAULT.createWithSuffix(block, "_active", textureMapping2, this.modelOutput));
      MultiVariant multiVariant3 = plainVariant(ModelTemplates.VAULT.createWithSuffix(block, "_unlocking", textureMapping3, this.modelOutput));
      MultiVariant multiVariant4 = plainVariant(ModelTemplates.VAULT.createWithSuffix(block, "_ejecting_reward", textureMapping4, this.modelOutput));
      TextureMapping textureMapping5 = TextureMapping.vault(block, "_front_off_ominous", "_side_off_ominous", "_top_ominous", "_bottom_ominous");
      TextureMapping textureMapping6 = TextureMapping.vault(block, "_front_on_ominous", "_side_on_ominous", "_top_ominous", "_bottom_ominous");
      TextureMapping textureMapping7 = TextureMapping.vault(block, "_front_ejecting_ominous", "_side_on_ominous", "_top_ominous", "_bottom_ominous");
      TextureMapping textureMapping8 = TextureMapping.vault(block, "_front_ejecting_ominous", "_side_on_ominous", "_top_ejecting_ominous", "_bottom_ominous");
      MultiVariant multiVariant5 = plainVariant(ModelTemplates.VAULT.createWithSuffix(block, "_ominous", textureMapping5, this.modelOutput));
      MultiVariant multiVariant6 = plainVariant(ModelTemplates.VAULT.createWithSuffix(block, "_active_ominous", textureMapping6, this.modelOutput));
      MultiVariant multiVariant7 = plainVariant(ModelTemplates.VAULT.createWithSuffix(block, "_unlocking_ominous", textureMapping7, this.modelOutput));
      MultiVariant multiVariant8 = plainVariant(ModelTemplates.VAULT.createWithSuffix(block, "_ejecting_reward_ominous", textureMapping8, this.modelOutput));
      this.registerSimpleItemModel(block, identifier);
      this.blockStateOutput
         .accept(MultiVariantGenerator.dispatch(block).with(PropertyDispatch.initial(VaultBlock.STATE, VaultBlock.OMINOUS).generate((vaultState, boolean_) -> {
            return switch (vaultState) {
               case INACTIVE -> boolean_ ? multiVariant5 : multiVariant;
               case ACTIVE -> boolean_ ? multiVariant6 : multiVariant2;
               case UNLOCKING -> boolean_ ? multiVariant7 : multiVariant3;
               case EJECTING -> boolean_ ? multiVariant8 : multiVariant4;
            };
         })).with(ROTATION_HORIZONTAL_FACING));
   }

   private void createSculkSensor() {
      Identifier identifier = ModelLocationUtils.getModelLocation(Blocks.SCULK_SENSOR, "_inactive");
      MultiVariant multiVariant = plainVariant(identifier);
      MultiVariant multiVariant2 = plainVariant(ModelLocationUtils.getModelLocation(Blocks.SCULK_SENSOR, "_active"));
      this.registerSimpleItemModel(Blocks.SCULK_SENSOR, identifier);
      this.blockStateOutput
         .accept(
            MultiVariantGenerator.dispatch(Blocks.SCULK_SENSOR)
               .with(
                  PropertyDispatch.initial(BlockStateProperties.SCULK_SENSOR_PHASE)
                     .generate(
                        sculkSensorPhase -> sculkSensorPhase != SculkSensorPhase.ACTIVE && sculkSensorPhase != SculkSensorPhase.COOLDOWN
                           ? multiVariant
                           : multiVariant2
                     )
               )
         );
   }

   private void createCalibratedSculkSensor() {
      Identifier identifier = ModelLocationUtils.getModelLocation(Blocks.CALIBRATED_SCULK_SENSOR, "_inactive");
      MultiVariant multiVariant = plainVariant(identifier);
      MultiVariant multiVariant2 = plainVariant(ModelLocationUtils.getModelLocation(Blocks.CALIBRATED_SCULK_SENSOR, "_active"));
      this.registerSimpleItemModel(Blocks.CALIBRATED_SCULK_SENSOR, identifier);
      this.blockStateOutput
         .accept(
            MultiVariantGenerator.dispatch(Blocks.CALIBRATED_SCULK_SENSOR)
               .with(
                  PropertyDispatch.initial(BlockStateProperties.SCULK_SENSOR_PHASE)
                     .generate(
                        sculkSensorPhase -> sculkSensorPhase != SculkSensorPhase.ACTIVE && sculkSensorPhase != SculkSensorPhase.COOLDOWN
                           ? multiVariant
                           : multiVariant2
                     )
               )
               .with(ROTATION_HORIZONTAL_FACING)
         );
   }

   private void createSculkShrieker() {
      Identifier identifier = ModelTemplates.SCULK_SHRIEKER.create(Blocks.SCULK_SHRIEKER, TextureMapping.sculkShrieker(false), this.modelOutput);
      MultiVariant multiVariant = plainVariant(identifier);
      MultiVariant multiVariant2 = plainVariant(
         ModelTemplates.SCULK_SHRIEKER.createWithSuffix(Blocks.SCULK_SHRIEKER, "_can_summon", TextureMapping.sculkShrieker(true), this.modelOutput)
      );
      this.registerSimpleItemModel(Blocks.SCULK_SHRIEKER, identifier);
      this.blockStateOutput
         .accept(
            MultiVariantGenerator.dispatch(Blocks.SCULK_SHRIEKER)
               .with(createBooleanModelDispatch(BlockStateProperties.CAN_SUMMON, multiVariant2, multiVariant))
         );
   }

   private void createScaffolding() {
      Identifier identifier = ModelLocationUtils.getModelLocation(Blocks.SCAFFOLDING, "_stable");
      MultiVariant multiVariant = plainVariant(identifier);
      MultiVariant multiVariant2 = plainVariant(ModelLocationUtils.getModelLocation(Blocks.SCAFFOLDING, "_unstable"));
      this.registerSimpleItemModel(Blocks.SCAFFOLDING, identifier);
      this.blockStateOutput
         .accept(MultiVariantGenerator.dispatch(Blocks.SCAFFOLDING).with(createBooleanModelDispatch(BlockStateProperties.BOTTOM, multiVariant2, multiVariant)));
   }

   private void createCaveVines() {
      MultiVariant multiVariant = plainVariant(this.createSuffixedVariant(Blocks.CAVE_VINES, "", ModelTemplates.CROSS, TextureMapping::cross));
      MultiVariant multiVariant2 = plainVariant(this.createSuffixedVariant(Blocks.CAVE_VINES, "_lit", ModelTemplates.CROSS, TextureMapping::cross));
      this.blockStateOutput
         .accept(MultiVariantGenerator.dispatch(Blocks.CAVE_VINES).with(createBooleanModelDispatch(BlockStateProperties.BERRIES, multiVariant2, multiVariant)));
      MultiVariant multiVariant3 = plainVariant(this.createSuffixedVariant(Blocks.CAVE_VINES_PLANT, "", ModelTemplates.CROSS, TextureMapping::cross));
      MultiVariant multiVariant4 = plainVariant(this.createSuffixedVariant(Blocks.CAVE_VINES_PLANT, "_lit", ModelTemplates.CROSS, TextureMapping::cross));
      this.blockStateOutput
         .accept(
            MultiVariantGenerator.dispatch(Blocks.CAVE_VINES_PLANT)
               .with(createBooleanModelDispatch(BlockStateProperties.BERRIES, multiVariant4, multiVariant3))
         );
   }

   private void createRedstoneLamp() {
      MultiVariant multiVariant = plainVariant(TexturedModel.CUBE.create(Blocks.REDSTONE_LAMP, this.modelOutput));
      MultiVariant multiVariant2 = plainVariant(this.createSuffixedVariant(Blocks.REDSTONE_LAMP, "_on", ModelTemplates.CUBE_ALL, TextureMapping::cube));
      this.blockStateOutput
         .accept(MultiVariantGenerator.dispatch(Blocks.REDSTONE_LAMP).with(createBooleanModelDispatch(BlockStateProperties.LIT, multiVariant2, multiVariant)));
   }

   private void createNormalTorch(Block torchBlock, Block wallTorchBlock) {
      TextureMapping textureMapping = TextureMapping.torch(torchBlock);
      this.blockStateOutput.accept(createSimpleBlock(torchBlock, plainVariant(ModelTemplates.TORCH.create(torchBlock, textureMapping, this.modelOutput))));
      this.blockStateOutput
         .accept(
            MultiVariantGenerator.dispatch(wallTorchBlock, plainVariant(ModelTemplates.WALL_TORCH.create(wallTorchBlock, textureMapping, this.modelOutput)))
               .with(ROTATION_TORCH)
         );
      this.registerSimpleFlatItemModel(torchBlock);
   }

   private void createRedstoneTorch() {
      TextureMapping textureMapping = TextureMapping.torch(Blocks.REDSTONE_TORCH);
      TextureMapping textureMapping2 = TextureMapping.torch(TextureMapping.getBlockTexture(Blocks.REDSTONE_TORCH, "_off"));
      MultiVariant multiVariant = plainVariant(ModelTemplates.REDSTONE_TORCH.create(Blocks.REDSTONE_TORCH, textureMapping, this.modelOutput));
      MultiVariant multiVariant2 = plainVariant(ModelTemplates.TORCH_UNLIT.createWithSuffix(Blocks.REDSTONE_TORCH, "_off", textureMapping2, this.modelOutput));
      this.blockStateOutput
         .accept(MultiVariantGenerator.dispatch(Blocks.REDSTONE_TORCH).with(createBooleanModelDispatch(BlockStateProperties.LIT, multiVariant, multiVariant2)));
      MultiVariant multiVariant3 = plainVariant(ModelTemplates.REDSTONE_WALL_TORCH.create(Blocks.REDSTONE_WALL_TORCH, textureMapping, this.modelOutput));
      MultiVariant multiVariant4 = plainVariant(
         ModelTemplates.WALL_TORCH_UNLIT.createWithSuffix(Blocks.REDSTONE_WALL_TORCH, "_off", textureMapping2, this.modelOutput)
      );
      this.blockStateOutput
         .accept(
            MultiVariantGenerator.dispatch(Blocks.REDSTONE_WALL_TORCH)
               .with(createBooleanModelDispatch(BlockStateProperties.LIT, multiVariant3, multiVariant4))
               .with(ROTATION_TORCH)
         );
      this.registerSimpleFlatItemModel(Blocks.REDSTONE_TORCH);
   }

   private void createRepeater() {
      this.registerSimpleFlatItemModel(Items.REPEATER);
      this.blockStateOutput
         .accept(
            MultiVariantGenerator.dispatch(Blocks.REPEATER)
               .with(
                  PropertyDispatch.initial(BlockStateProperties.DELAY, BlockStateProperties.LOCKED, BlockStateProperties.POWERED)
                     .generate((integer, boolean_, boolean2) -> {
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append('_').append(integer).append("tick");
                        if (boolean2) {
                           stringBuilder.append("_on");
                        }

                        if (boolean_) {
                           stringBuilder.append("_locked");
                        }

                        return plainVariant(TextureMapping.getBlockTexture(Blocks.REPEATER, stringBuilder.toString()));
                     })
               )
               .with(ROTATION_HORIZONTAL_FACING_ALT)
         );
   }

   private void createSeaPickle() {
      this.registerSimpleFlatItemModel(Items.SEA_PICKLE);
      this.blockStateOutput
         .accept(
            MultiVariantGenerator.dispatch(Blocks.SEA_PICKLE)
               .with(
                  PropertyDispatch.initial(BlockStateProperties.PICKLES, BlockStateProperties.WATERLOGGED)
                     .select(1, false, createRotatedVariants(plainModel(ModelLocationUtils.decorateBlockModelLocation("dead_sea_pickle"))))
                     .select(2, false, createRotatedVariants(plainModel(ModelLocationUtils.decorateBlockModelLocation("two_dead_sea_pickles"))))
                     .select(3, false, createRotatedVariants(plainModel(ModelLocationUtils.decorateBlockModelLocation("three_dead_sea_pickles"))))
                     .select(4, false, createRotatedVariants(plainModel(ModelLocationUtils.decorateBlockModelLocation("four_dead_sea_pickles"))))
                     .select(1, true, createRotatedVariants(plainModel(ModelLocationUtils.decorateBlockModelLocation("sea_pickle"))))
                     .select(2, true, createRotatedVariants(plainModel(ModelLocationUtils.decorateBlockModelLocation("two_sea_pickles"))))
                     .select(3, true, createRotatedVariants(plainModel(ModelLocationUtils.decorateBlockModelLocation("three_sea_pickles"))))
                     .select(4, true, createRotatedVariants(plainModel(ModelLocationUtils.decorateBlockModelLocation("four_sea_pickles"))))
               )
         );
   }

   private void createSnowBlocks() {
      TextureMapping textureMapping = TextureMapping.cube(Blocks.SNOW);
      MultiVariant multiVariant = plainVariant(ModelTemplates.CUBE_ALL.create(Blocks.SNOW_BLOCK, textureMapping, this.modelOutput));
      this.blockStateOutput
         .accept(
            MultiVariantGenerator.dispatch(Blocks.SNOW)
               .with(
                  PropertyDispatch.initial(BlockStateProperties.LAYERS)
                     .generate(integer -> integer < 8 ? plainVariant(ModelLocationUtils.getModelLocation(Blocks.SNOW, "_height" + integer * 2)) : multiVariant)
               )
         );
      this.registerSimpleItemModel(Blocks.SNOW, ModelLocationUtils.getModelLocation(Blocks.SNOW, "_height2"));
      this.blockStateOutput.accept(createSimpleBlock(Blocks.SNOW_BLOCK, multiVariant));
   }

   private void createStonecutter() {
      this.blockStateOutput
         .accept(
            MultiVariantGenerator.dispatch(Blocks.STONECUTTER, plainVariant(ModelLocationUtils.getModelLocation(Blocks.STONECUTTER)))
               .with(ROTATION_HORIZONTAL_FACING)
         );
   }

   private void createStructureBlock() {
      Identifier identifier = TexturedModel.CUBE.create(Blocks.STRUCTURE_BLOCK, this.modelOutput);
      this.registerSimpleItemModel(Blocks.STRUCTURE_BLOCK, identifier);
      this.blockStateOutput
         .accept(
            MultiVariantGenerator.dispatch(Blocks.STRUCTURE_BLOCK)
               .with(
                  PropertyDispatch.initial(BlockStateProperties.STRUCTUREBLOCK_MODE)
                     .generate(
                        structureMode -> plainVariant(
                           this.createSuffixedVariant(
                              Blocks.STRUCTURE_BLOCK, "_" + structureMode.getSerializedName(), ModelTemplates.CUBE_ALL, TextureMapping::cube
                           )
                        )
                     )
               )
         );
   }

   private void createTestBlock() {
      Map<TestBlockMode, Identifier> map = new HashMap<>();

      for (TestBlockMode testBlockMode : TestBlockMode.values()) {
         map.put(
            testBlockMode,
            this.createSuffixedVariant(Blocks.TEST_BLOCK, "_" + testBlockMode.getSerializedName(), ModelTemplates.CUBE_ALL, TextureMapping::cube)
         );
      }

      this.blockStateOutput
         .accept(
            MultiVariantGenerator.dispatch(Blocks.TEST_BLOCK)
               .with(PropertyDispatch.initial(BlockStateProperties.TEST_BLOCK_MODE).generate(testBlockModex -> plainVariant(map.get(testBlockModex))))
         );
      this.itemModelOutput
         .accept(
            Items.TEST_BLOCK,
            ItemModelUtils.selectBlockItemProperty(
               TestBlock.MODE,
               ItemModelUtils.plainModel(map.get(TestBlockMode.START)),
               Map.of(
                  TestBlockMode.FAIL,
                  ItemModelUtils.plainModel(map.get(TestBlockMode.FAIL)),
                  TestBlockMode.LOG,
                  ItemModelUtils.plainModel(map.get(TestBlockMode.LOG)),
                  TestBlockMode.ACCEPT,
                  ItemModelUtils.plainModel(map.get(TestBlockMode.ACCEPT))
               )
            )
         );
   }

   private void createSweetBerryBush() {
      this.registerSimpleFlatItemModel(Items.SWEET_BERRIES);
      this.blockStateOutput
         .accept(
            MultiVariantGenerator.dispatch(Blocks.SWEET_BERRY_BUSH)
               .with(
                  PropertyDispatch.initial(BlockStateProperties.AGE_3)
                     .generate(
                        integer -> plainVariant(
                           this.createSuffixedVariant(Blocks.SWEET_BERRY_BUSH, "_stage" + integer, ModelTemplates.CROSS, TextureMapping::cross)
                        )
                     )
               )
         );
   }

   private void createTripwire() {
      this.registerSimpleFlatItemModel(Items.STRING);
      this.blockStateOutput
         .accept(
            MultiVariantGenerator.dispatch(Blocks.TRIPWIRE)
               .with(
                  PropertyDispatch.initial(
                        BlockStateProperties.ATTACHED,
                        BlockStateProperties.EAST,
                        BlockStateProperties.NORTH,
                        BlockStateProperties.SOUTH,
                        BlockStateProperties.WEST
                     )
                     .select(false, false, false, false, false, plainVariant(ModelLocationUtils.getModelLocation(Blocks.TRIPWIRE, "_ns")))
                     .select(false, true, false, false, false, plainVariant(ModelLocationUtils.getModelLocation(Blocks.TRIPWIRE, "_n")).with(Y_ROT_90))
                     .select(false, false, true, false, false, plainVariant(ModelLocationUtils.getModelLocation(Blocks.TRIPWIRE, "_n")))
                     .select(false, false, false, true, false, plainVariant(ModelLocationUtils.getModelLocation(Blocks.TRIPWIRE, "_n")).with(Y_ROT_180))
                     .select(false, false, false, false, true, plainVariant(ModelLocationUtils.getModelLocation(Blocks.TRIPWIRE, "_n")).with(Y_ROT_270))
                     .select(false, true, true, false, false, plainVariant(ModelLocationUtils.getModelLocation(Blocks.TRIPWIRE, "_ne")))
                     .select(false, true, false, true, false, plainVariant(ModelLocationUtils.getModelLocation(Blocks.TRIPWIRE, "_ne")).with(Y_ROT_90))
                     .select(false, false, false, true, true, plainVariant(ModelLocationUtils.getModelLocation(Blocks.TRIPWIRE, "_ne")).with(Y_ROT_180))
                     .select(false, false, true, false, true, plainVariant(ModelLocationUtils.getModelLocation(Blocks.TRIPWIRE, "_ne")).with(Y_ROT_270))
                     .select(false, false, true, true, false, plainVariant(ModelLocationUtils.getModelLocation(Blocks.TRIPWIRE, "_ns")))
                     .select(false, true, false, false, true, plainVariant(ModelLocationUtils.getModelLocation(Blocks.TRIPWIRE, "_ns")).with(Y_ROT_90))
                     .select(false, true, true, true, false, plainVariant(ModelLocationUtils.getModelLocation(Blocks.TRIPWIRE, "_nse")))
                     .select(false, true, false, true, true, plainVariant(ModelLocationUtils.getModelLocation(Blocks.TRIPWIRE, "_nse")).with(Y_ROT_90))
                     .select(false, false, true, true, true, plainVariant(ModelLocationUtils.getModelLocation(Blocks.TRIPWIRE, "_nse")).with(Y_ROT_180))
                     .select(false, true, true, false, true, plainVariant(ModelLocationUtils.getModelLocation(Blocks.TRIPWIRE, "_nse")).with(Y_ROT_270))
                     .select(false, true, true, true, true, plainVariant(ModelLocationUtils.getModelLocation(Blocks.TRIPWIRE, "_nsew")))
                     .select(true, false, false, false, false, plainVariant(ModelLocationUtils.getModelLocation(Blocks.TRIPWIRE, "_attached_ns")))
                     .select(true, false, true, false, false, plainVariant(ModelLocationUtils.getModelLocation(Blocks.TRIPWIRE, "_attached_n")))
                     .select(true, false, false, true, false, plainVariant(ModelLocationUtils.getModelLocation(Blocks.TRIPWIRE, "_attached_n")).with(Y_ROT_180))
                     .select(true, true, false, false, false, plainVariant(ModelLocationUtils.getModelLocation(Blocks.TRIPWIRE, "_attached_n")).with(Y_ROT_90))
                     .select(true, false, false, false, true, plainVariant(ModelLocationUtils.getModelLocation(Blocks.TRIPWIRE, "_attached_n")).with(Y_ROT_270))
                     .select(true, true, true, false, false, plainVariant(ModelLocationUtils.getModelLocation(Blocks.TRIPWIRE, "_attached_ne")))
                     .select(true, true, false, true, false, plainVariant(ModelLocationUtils.getModelLocation(Blocks.TRIPWIRE, "_attached_ne")).with(Y_ROT_90))
                     .select(true, false, false, true, true, plainVariant(ModelLocationUtils.getModelLocation(Blocks.TRIPWIRE, "_attached_ne")).with(Y_ROT_180))
                     .select(true, false, true, false, true, plainVariant(ModelLocationUtils.getModelLocation(Blocks.TRIPWIRE, "_attached_ne")).with(Y_ROT_270))
                     .select(true, false, true, true, false, plainVariant(ModelLocationUtils.getModelLocation(Blocks.TRIPWIRE, "_attached_ns")))
                     .select(true, true, false, false, true, plainVariant(ModelLocationUtils.getModelLocation(Blocks.TRIPWIRE, "_attached_ns")).with(Y_ROT_90))
                     .select(true, true, true, true, false, plainVariant(ModelLocationUtils.getModelLocation(Blocks.TRIPWIRE, "_attached_nse")))
                     .select(true, true, false, true, true, plainVariant(ModelLocationUtils.getModelLocation(Blocks.TRIPWIRE, "_attached_nse")).with(Y_ROT_90))
                     .select(true, false, true, true, true, plainVariant(ModelLocationUtils.getModelLocation(Blocks.TRIPWIRE, "_attached_nse")).with(Y_ROT_180))
                     .select(true, true, true, false, true, plainVariant(ModelLocationUtils.getModelLocation(Blocks.TRIPWIRE, "_attached_nse")).with(Y_ROT_270))
                     .select(true, true, true, true, true, plainVariant(ModelLocationUtils.getModelLocation(Blocks.TRIPWIRE, "_attached_nsew")))
               )
         );
   }

   private void createTripwireHook() {
      this.registerSimpleFlatItemModel(Blocks.TRIPWIRE_HOOK);
      this.blockStateOutput
         .accept(
            MultiVariantGenerator.dispatch(Blocks.TRIPWIRE_HOOK)
               .with(
                  PropertyDispatch.initial(BlockStateProperties.ATTACHED, BlockStateProperties.POWERED)
                     .generate(
                        (boolean_, boolean2) -> plainVariant(
                           ModelLocationUtils.getModelLocation(Blocks.TRIPWIRE_HOOK, (boolean_ ? "_attached" : "") + (boolean2 ? "_on" : ""))
                        )
                     )
               )
               .with(ROTATION_HORIZONTAL_FACING)
         );
   }

   private Variant createTurtleEggModel(int eggs, String variantName, TextureMapping textureMapping) {
      return switch (eggs) {
         case 1 -> plainModel(
            ModelTemplates.TURTLE_EGG.create(ModelLocationUtils.decorateBlockModelLocation(variantName + "turtle_egg"), textureMapping, this.modelOutput)
         );
         case 2 -> plainModel(
            ModelTemplates.TWO_TURTLE_EGGS
               .create(ModelLocationUtils.decorateBlockModelLocation("two_" + variantName + "turtle_eggs"), textureMapping, this.modelOutput)
         );
         case 3 -> plainModel(
            ModelTemplates.THREE_TURTLE_EGGS
               .create(ModelLocationUtils.decorateBlockModelLocation("three_" + variantName + "turtle_eggs"), textureMapping, this.modelOutput)
         );
         case 4 -> plainModel(
            ModelTemplates.FOUR_TURTLE_EGGS
               .create(ModelLocationUtils.decorateBlockModelLocation("four_" + variantName + "turtle_eggs"), textureMapping, this.modelOutput)
         );
         default -> throw new UnsupportedOperationException();
      };
   }

   private Variant createTurtleEggModel(int eggs, int hatchAmount) {
      return switch (hatchAmount) {
         case 0 -> this.createTurtleEggModel(eggs, "", TextureMapping.cube(TextureMapping.getBlockTexture(Blocks.TURTLE_EGG)));
         case 1 -> this.createTurtleEggModel(
            eggs, "slightly_cracked_", TextureMapping.cube(TextureMapping.getBlockTexture(Blocks.TURTLE_EGG, "_slightly_cracked"))
         );
         case 2 -> this.createTurtleEggModel(eggs, "very_cracked_", TextureMapping.cube(TextureMapping.getBlockTexture(Blocks.TURTLE_EGG, "_very_cracked")));
         default -> throw new UnsupportedOperationException();
      };
   }

   private void createTurtleEgg() {
      this.registerSimpleFlatItemModel(Items.TURTLE_EGG);
      this.blockStateOutput
         .accept(
            MultiVariantGenerator.dispatch(Blocks.TURTLE_EGG)
               .with(
                  PropertyDispatch.initial(BlockStateProperties.EGGS, BlockStateProperties.HATCH)
                     .generate((integer, integer2) -> createRotatedVariants(this.createTurtleEggModel(integer, integer2)))
               )
         );
   }

   private void createDriedGhastBlock() {
      Identifier identifier = ModelLocationUtils.getModelLocation(Blocks.DRIED_GHAST, "_hydration_0");
      this.registerSimpleItemModel(Blocks.DRIED_GHAST, identifier);
      Function<Integer, Identifier> function = integer -> {
         String string = switch (integer) {
            case 1 -> "_hydration_1";
            case 2 -> "_hydration_2";
            case 3 -> "_hydration_3";
            default -> "_hydration_0";
         };
         TextureMapping textureMapping = TextureMapping.driedGhast(string);
         return ModelTemplates.DRIED_GHAST.createWithSuffix(Blocks.DRIED_GHAST, string, textureMapping, this.modelOutput);
      };
      this.blockStateOutput
         .accept(
            MultiVariantGenerator.dispatch(Blocks.DRIED_GHAST)
               .with(PropertyDispatch.initial(DriedGhastBlock.HYDRATION_LEVEL).generate(integer -> plainVariant(function.apply(integer))))
               .with(ROTATION_HORIZONTAL_FACING)
         );
   }

   private void createSnifferEgg() {
      this.registerSimpleFlatItemModel(Items.SNIFFER_EGG);
      this.blockStateOutput
         .accept(MultiVariantGenerator.dispatch(Blocks.SNIFFER_EGG).with(PropertyDispatch.initial(SnifferEggBlock.HATCH).generate(integer -> {
            String string = switch (integer) {
               case 1 -> "_slightly_cracked";
               case 2 -> "_very_cracked";
               default -> "_not_cracked";
            };
            TextureMapping textureMapping = TextureMapping.snifferEgg(string);
            return plainVariant(ModelTemplates.SNIFFER_EGG.createWithSuffix(Blocks.SNIFFER_EGG, string, textureMapping, this.modelOutput));
         })));
   }

   private void createMultiface(Block multifaceBlock) {
      this.registerSimpleFlatItemModel(multifaceBlock);
      this.createMultifaceBlockStates(multifaceBlock);
   }

   private void createMultiface(Block block, Item item) {
      this.registerSimpleFlatItemModel(item);
      this.createMultifaceBlockStates(block);
   }

   private static <T extends Property<?>> Map<T, VariantMutator> selectMultifaceProperties(StateHolder<?, ?> state, Function<Direction, T> facePropertyGetter) {
      Builder<T, VariantMutator> builder = ImmutableMap.builderWithExpectedSize(MULTIFACE_GENERATOR.size());
      MULTIFACE_GENERATOR.forEach((direction, variantMutator) -> {
         T property = facePropertyGetter.apply(direction);
         if (state.hasProperty(property)) {
            builder.put(property, variantMutator);
         }
      });
      return builder.build();
   }

   private void createMultifaceBlockStates(Block block) {
      Map<Property<Boolean>, VariantMutator> map = selectMultifaceProperties(block.defaultBlockState(), MultifaceBlock::getFaceProperty);
      ConditionBuilder conditionBuilder = condition();
      map.forEach((property, variantMutator) -> conditionBuilder.term((Property<Boolean>)property, false));
      MultiVariant multiVariant = plainVariant(ModelLocationUtils.getModelLocation(block));
      MultiPartGenerator multiPartGenerator = MultiPartGenerator.multiPart(block);
      map.forEach((property, variantMutator) -> {
         multiPartGenerator.with(condition().term((Property<Boolean>)property, true), multiVariant.with(variantMutator));
         multiPartGenerator.with(conditionBuilder, multiVariant.with(variantMutator));
      });
      this.blockStateOutput.accept(multiPartGenerator);
   }

   private void createMossyCarpet(Block block) {
      Map<Property<WallSide>, VariantMutator> map = selectMultifaceProperties(block.defaultBlockState(), MossyCarpetBlock::getPropertyForFace);
      ConditionBuilder conditionBuilder = condition().term(MossyCarpetBlock.BASE, false);
      map.forEach((property, variantMutator) -> conditionBuilder.term((Property<WallSide>)property, WallSide.NONE));
      MultiVariant multiVariant = plainVariant(TexturedModel.CARPET.create(block, this.modelOutput));
      MultiVariant multiVariant2 = plainVariant(
         TexturedModel.MOSSY_CARPET_SIDE
            .get(block)
            .updateTextures(textureMapping -> textureMapping.put(TextureSlot.SIDE, TextureMapping.getBlockTexture(block, "_side_tall")))
            .createWithSuffix(block, "_side_tall", this.modelOutput)
      );
      MultiVariant multiVariant3 = plainVariant(
         TexturedModel.MOSSY_CARPET_SIDE
            .get(block)
            .updateTextures(textureMapping -> textureMapping.put(TextureSlot.SIDE, TextureMapping.getBlockTexture(block, "_side_small")))
            .createWithSuffix(block, "_side_small", this.modelOutput)
      );
      MultiPartGenerator multiPartGenerator = MultiPartGenerator.multiPart(block);
      multiPartGenerator.with(condition().term(MossyCarpetBlock.BASE, true), multiVariant);
      multiPartGenerator.with(conditionBuilder, multiVariant);
      map.forEach((property, variantMutator) -> {
         multiPartGenerator.with(condition().term((Property<WallSide>)property, WallSide.TALL), multiVariant2.with(variantMutator));
         multiPartGenerator.with(condition().term((Property<WallSide>)property, WallSide.LOW), multiVariant3.with(variantMutator));
         multiPartGenerator.with(conditionBuilder, multiVariant2.with(variantMutator));
      });
      this.blockStateOutput.accept(multiPartGenerator);
   }

   private void createHangingMoss(Block block) {
      this.registerSimpleFlatItemModel(block);
      this.blockStateOutput.accept(MultiVariantGenerator.dispatch(block).with(PropertyDispatch.initial(HangingMossBlock.TIP).generate(boolean_ -> {
         String string = boolean_ ? "_tip" : "";
         TextureMapping textureMapping = TextureMapping.cross(TextureMapping.getBlockTexture(block, string));
         return plainVariant(BlockModelGenerators.PlantType.NOT_TINTED.getCross().createWithSuffix(block, string, textureMapping, this.modelOutput));
      })));
   }

   private void createSculkCatalyst() {
      Identifier identifier = TextureMapping.getBlockTexture(Blocks.SCULK_CATALYST, "_bottom");
      TextureMapping textureMapping = new TextureMapping()
         .put(TextureSlot.BOTTOM, identifier)
         .put(TextureSlot.TOP, TextureMapping.getBlockTexture(Blocks.SCULK_CATALYST, "_top"))
         .put(TextureSlot.SIDE, TextureMapping.getBlockTexture(Blocks.SCULK_CATALYST, "_side"));
      TextureMapping textureMapping2 = new TextureMapping()
         .put(TextureSlot.BOTTOM, identifier)
         .put(TextureSlot.TOP, TextureMapping.getBlockTexture(Blocks.SCULK_CATALYST, "_top_bloom"))
         .put(TextureSlot.SIDE, TextureMapping.getBlockTexture(Blocks.SCULK_CATALYST, "_side_bloom"));
      Identifier identifier2 = ModelTemplates.CUBE_BOTTOM_TOP.create(Blocks.SCULK_CATALYST, textureMapping, this.modelOutput);
      MultiVariant multiVariant = plainVariant(identifier2);
      MultiVariant multiVariant2 = plainVariant(
         ModelTemplates.CUBE_BOTTOM_TOP.createWithSuffix(Blocks.SCULK_CATALYST, "_bloom", textureMapping2, this.modelOutput)
      );
      this.blockStateOutput
         .accept(
            MultiVariantGenerator.dispatch(Blocks.SCULK_CATALYST)
               .with(PropertyDispatch.initial(BlockStateProperties.BLOOM).generate(boolean_ -> boolean_ ? multiVariant2 : multiVariant))
         );
      this.registerSimpleItemModel(Blocks.SCULK_CATALYST, identifier2);
   }

   private void createShelf(Block shelfBlock, Block particleBlock) {
      TextureMapping textureMapping = new TextureMapping()
         .put(TextureSlot.ALL, TextureMapping.getBlockTexture(shelfBlock))
         .put(TextureSlot.PARTICLE, TextureMapping.getBlockTexture(particleBlock));
      MultiPartGenerator multiPartGenerator = MultiPartGenerator.multiPart(shelfBlock);
      this.addShelfPart(shelfBlock, textureMapping, multiPartGenerator, ModelTemplates.SHELF_BODY, null, null);
      this.addShelfPart(shelfBlock, textureMapping, multiPartGenerator, ModelTemplates.SHELF_UNPOWERED, false, null);
      this.addShelfPart(shelfBlock, textureMapping, multiPartGenerator, ModelTemplates.SHELF_UNCONNECTED, true, SideChainPart.UNCONNECTED);
      this.addShelfPart(shelfBlock, textureMapping, multiPartGenerator, ModelTemplates.SHELF_LEFT, true, SideChainPart.LEFT);
      this.addShelfPart(shelfBlock, textureMapping, multiPartGenerator, ModelTemplates.SHELF_CENTER, true, SideChainPart.CENTER);
      this.addShelfPart(shelfBlock, textureMapping, multiPartGenerator, ModelTemplates.SHELF_RIGHT, true, SideChainPart.RIGHT);
      this.blockStateOutput.accept(multiPartGenerator);
      this.registerSimpleItemModel(shelfBlock, ModelTemplates.SHELF_INVENTORY.create(shelfBlock, textureMapping, this.modelOutput));
   }

   private void addShelfPart(
      Block block,
      TextureMapping textureMapping,
      MultiPartGenerator generator,
      ModelTemplate template,
      @Nullable Boolean powered,
      @Nullable SideChainPart sideChainPart
   ) {
      MultiVariant multiVariant = plainVariant(template.create(block, textureMapping, this.modelOutput));
      forEachHorizontalDirection(
         (direction, variantMutator) -> generator.with(shelfCondition(direction, powered, sideChainPart), multiVariant.with(variantMutator))
      );
   }

   private static void forEachHorizontalDirection(BiConsumer<Direction, VariantMutator> action) {
      List.of(Pair.of(Direction.NORTH, NOP), Pair.of(Direction.EAST, Y_ROT_90), Pair.of(Direction.SOUTH, Y_ROT_180), Pair.of(Direction.WEST, Y_ROT_270))
         .forEach(pair -> {
            Direction direction = (Direction)pair.getFirst();
            VariantMutator variantMutator = (VariantMutator)pair.getSecond();
            action.accept(direction, variantMutator);
         });
   }

   private static Condition shelfCondition(Direction direction, @Nullable Boolean powered, @Nullable SideChainPart sideChainPart) {
      ConditionBuilder conditionBuilder = condition(BlockStateProperties.HORIZONTAL_FACING, direction);
      if (powered == null) {
         return conditionBuilder.build();
      } else {
         ConditionBuilder conditionBuilder2 = condition(BlockStateProperties.POWERED, powered);
         return sideChainPart != null
            ? and(conditionBuilder, conditionBuilder2, condition(BlockStateProperties.SIDE_CHAIN_PART, sideChainPart))
            : and(conditionBuilder, conditionBuilder2);
      }
   }

   private void createChiseledBookshelf() {
      Block block = Blocks.CHISELED_BOOKSHELF;
      MultiVariant multiVariant = plainVariant(ModelLocationUtils.getModelLocation(block));
      MultiPartGenerator multiPartGenerator = MultiPartGenerator.multiPart(block);
      forEachHorizontalDirection((direction, variantMutator) -> {
         Condition condition = condition().term(BlockStateProperties.HORIZONTAL_FACING, direction).build();
         multiPartGenerator.with(condition, multiVariant.with(variantMutator).with(UV_LOCK));
         this.addSlotStateAndRotationVariants(multiPartGenerator, condition, variantMutator);
      });
      this.blockStateOutput.accept(multiPartGenerator);
      this.registerSimpleItemModel(block, ModelLocationUtils.getModelLocation(block, "_inventory"));
      CHISELED_BOOKSHELF_SLOT_MODEL_CACHE.clear();
   }

   private void addSlotStateAndRotationVariants(MultiPartGenerator generator, Condition condition, VariantMutator rotation) {
      List.of(
            Pair.of(ChiseledBookShelfBlock.SLOT_0_OCCUPIED, ModelTemplates.CHISELED_BOOKSHELF_SLOT_TOP_LEFT),
            Pair.of(ChiseledBookShelfBlock.SLOT_1_OCCUPIED, ModelTemplates.CHISELED_BOOKSHELF_SLOT_TOP_MID),
            Pair.of(ChiseledBookShelfBlock.SLOT_2_OCCUPIED, ModelTemplates.CHISELED_BOOKSHELF_SLOT_TOP_RIGHT),
            Pair.of(ChiseledBookShelfBlock.SLOT_3_OCCUPIED, ModelTemplates.CHISELED_BOOKSHELF_SLOT_BOTTOM_LEFT),
            Pair.of(ChiseledBookShelfBlock.SLOT_4_OCCUPIED, ModelTemplates.CHISELED_BOOKSHELF_SLOT_BOTTOM_MID),
            Pair.of(ChiseledBookShelfBlock.SLOT_5_OCCUPIED, ModelTemplates.CHISELED_BOOKSHELF_SLOT_BOTTOM_RIGHT)
         )
         .forEach(pair -> {
            BooleanProperty booleanProperty = (BooleanProperty)pair.getFirst();
            ModelTemplate modelTemplate = (ModelTemplate)pair.getSecond();
            this.addBookSlotModel(generator, condition, rotation, booleanProperty, modelTemplate, true);
            this.addBookSlotModel(generator, condition, rotation, booleanProperty, modelTemplate, false);
         });
   }

   private void addBookSlotModel(
      MultiPartGenerator generator, Condition conditon, VariantMutator rotation, BooleanProperty hasBookProperty, ModelTemplate template, boolean hasBook
   ) {
      String string = hasBook ? "_occupied" : "_empty";
      TextureMapping textureMapping = new TextureMapping().put(TextureSlot.TEXTURE, TextureMapping.getBlockTexture(Blocks.CHISELED_BOOKSHELF, string));
      BlockModelGenerators.BookSlotModelCacheKey bookSlotModelCacheKey = new BlockModelGenerators.BookSlotModelCacheKey(template, string);
      MultiVariant multiVariant = plainVariant(
         CHISELED_BOOKSHELF_SLOT_MODEL_CACHE.computeIfAbsent(
            bookSlotModelCacheKey, bookSlotModelCacheKeyx -> template.createWithSuffix(Blocks.CHISELED_BOOKSHELF, string, textureMapping, this.modelOutput)
         )
      );
      generator.with(
         new CombinedCondition(CombinedCondition.Operation.AND, List.of(conditon, condition().term(hasBookProperty, hasBook).build())),
         multiVariant.with(rotation)
      );
   }

   private void createMagmaBlock() {
      MultiVariant multiVariant = plainVariant(
         ModelTemplates.CUBE_ALL.create(Blocks.MAGMA_BLOCK, TextureMapping.cube(ModelLocationUtils.decorateBlockModelLocation("magma")), this.modelOutput)
      );
      this.blockStateOutput.accept(createSimpleBlock(Blocks.MAGMA_BLOCK, multiVariant));
   }

   private void createShulkerBox(Block block, @Nullable DyeColor color) {
      this.createParticleOnlyBlock(block);
      Item item = block.asItem();
      Identifier identifier = ModelTemplates.SHULKER_BOX_INVENTORY.create(item, TextureMapping.particle(block), this.modelOutput);
      ItemModel.Unbaked unbaked = color != null
         ? ItemModelUtils.specialModel(identifier, new ShulkerBoxSpecialRenderer.Unbaked(color))
         : ItemModelUtils.specialModel(identifier, new ShulkerBoxSpecialRenderer.Unbaked());
      this.itemModelOutput.accept(item, unbaked);
   }

   private void createGrowingPlant(Block plantBlock, Block tipBlock, BlockModelGenerators.PlantType plantType) {
      this.createCrossBlock(plantBlock, plantType);
      this.createCrossBlock(tipBlock, plantType);
   }

   private void createInfestedStone() {
      Identifier identifier = ModelLocationUtils.getModelLocation(Blocks.STONE);
      Variant variant = plainModel(identifier);
      Variant variant2 = plainModel(ModelLocationUtils.getModelLocation(Blocks.STONE, "_mirrored"));
      this.blockStateOutput.accept(MultiVariantGenerator.dispatch(Blocks.INFESTED_STONE, createRotatedVariants(variant, variant2)));
      this.registerSimpleItemModel(Blocks.INFESTED_STONE, identifier);
   }

   private void createInfestedDeepslate() {
      Identifier identifier = ModelLocationUtils.getModelLocation(Blocks.DEEPSLATE);
      Variant variant = plainModel(identifier);
      Variant variant2 = plainModel(ModelLocationUtils.getModelLocation(Blocks.DEEPSLATE, "_mirrored"));
      this.blockStateOutput
         .accept(MultiVariantGenerator.dispatch(Blocks.INFESTED_DEEPSLATE, createRotatedVariants(variant, variant2)).with(createRotatedPillar()));
      this.registerSimpleItemModel(Blocks.INFESTED_DEEPSLATE, identifier);
   }

   private void createNetherRoots(Block plantBlock, Block pottedPlantBlock) {
      this.createCrossBlockWithDefaultItem(plantBlock, BlockModelGenerators.PlantType.NOT_TINTED);
      TextureMapping textureMapping = TextureMapping.plant(TextureMapping.getBlockTexture(plantBlock, "_pot"));
      MultiVariant multiVariant = plainVariant(
         BlockModelGenerators.PlantType.NOT_TINTED.getCrossPot().create(pottedPlantBlock, textureMapping, this.modelOutput)
      );
      this.blockStateOutput.accept(createSimpleBlock(pottedPlantBlock, multiVariant));
   }

   private void createRespawnAnchor() {
      Identifier identifier = TextureMapping.getBlockTexture(Blocks.RESPAWN_ANCHOR, "_bottom");
      Identifier identifier2 = TextureMapping.getBlockTexture(Blocks.RESPAWN_ANCHOR, "_top_off");
      Identifier identifier3 = TextureMapping.getBlockTexture(Blocks.RESPAWN_ANCHOR, "_top");
      Identifier[] identifiers = new Identifier[5];

      for (int i = 0; i < 5; i++) {
         TextureMapping textureMapping = new TextureMapping()
            .put(TextureSlot.BOTTOM, identifier)
            .put(TextureSlot.TOP, i == 0 ? identifier2 : identifier3)
            .put(TextureSlot.SIDE, TextureMapping.getBlockTexture(Blocks.RESPAWN_ANCHOR, "_side" + i));
         identifiers[i] = ModelTemplates.CUBE_BOTTOM_TOP.createWithSuffix(Blocks.RESPAWN_ANCHOR, "_" + i, textureMapping, this.modelOutput);
      }

      this.blockStateOutput
         .accept(
            MultiVariantGenerator.dispatch(Blocks.RESPAWN_ANCHOR)
               .with(PropertyDispatch.initial(BlockStateProperties.RESPAWN_ANCHOR_CHARGES).generate(integer -> plainVariant(identifiers[integer])))
         );
      this.registerSimpleItemModel(Blocks.RESPAWN_ANCHOR, identifiers[0]);
   }

   private static VariantMutator applyRotation(FrontAndTop rotation) {
      return switch (rotation) {
         case DOWN_NORTH -> X_ROT_90;
         case DOWN_SOUTH -> X_ROT_90.then(Y_ROT_180);
         case DOWN_WEST -> X_ROT_90.then(Y_ROT_270);
         case DOWN_EAST -> X_ROT_90.then(Y_ROT_90);
         case UP_NORTH -> X_ROT_270.then(Y_ROT_180);
         case UP_SOUTH -> X_ROT_270;
         case UP_WEST -> X_ROT_270.then(Y_ROT_90);
         case UP_EAST -> X_ROT_270.then(Y_ROT_270);
         case NORTH_UP -> NOP;
         case SOUTH_UP -> Y_ROT_180;
         case WEST_UP -> Y_ROT_270;
         case EAST_UP -> Y_ROT_90;
      };
   }

   private void createJigsaw() {
      Identifier identifier = TextureMapping.getBlockTexture(Blocks.JIGSAW, "_top");
      Identifier identifier2 = TextureMapping.getBlockTexture(Blocks.JIGSAW, "_bottom");
      Identifier identifier3 = TextureMapping.getBlockTexture(Blocks.JIGSAW, "_side");
      Identifier identifier4 = TextureMapping.getBlockTexture(Blocks.JIGSAW, "_lock");
      TextureMapping textureMapping = new TextureMapping()
         .put(TextureSlot.DOWN, identifier3)
         .put(TextureSlot.WEST, identifier3)
         .put(TextureSlot.EAST, identifier3)
         .put(TextureSlot.PARTICLE, identifier)
         .put(TextureSlot.NORTH, identifier)
         .put(TextureSlot.SOUTH, identifier2)
         .put(TextureSlot.UP, identifier4);
      this.blockStateOutput
         .accept(
            MultiVariantGenerator.dispatch(Blocks.JIGSAW, plainVariant(ModelTemplates.CUBE_DIRECTIONAL.create(Blocks.JIGSAW, textureMapping, this.modelOutput)))
               .with(PropertyDispatch.modify(BlockStateProperties.ORIENTATION).generate(BlockModelGenerators::applyRotation))
         );
   }

   private void createPetrifiedOakSlab() {
      Block block = Blocks.OAK_PLANKS;
      MultiVariant multiVariant = plainVariant(ModelLocationUtils.getModelLocation(block));
      TextureMapping textureMapping = TextureMapping.cube(block);
      Block block2 = Blocks.PETRIFIED_OAK_SLAB;
      MultiVariant multiVariant2 = plainVariant(ModelTemplates.SLAB_BOTTOM.create(block2, textureMapping, this.modelOutput));
      MultiVariant multiVariant3 = plainVariant(ModelTemplates.SLAB_TOP.create(block2, textureMapping, this.modelOutput));
      this.blockStateOutput.accept(createSlab(block2, multiVariant2, multiVariant3, multiVariant));
   }

   private void createHead(Block headBlock, Block wallHeadBlock, SkullBlock.Type type, Identifier modelLocation) {
      MultiVariant multiVariant = plainVariant(ModelLocationUtils.decorateBlockModelLocation("skull"));
      this.blockStateOutput.accept(createSimpleBlock(headBlock, multiVariant));
      this.blockStateOutput.accept(createSimpleBlock(wallHeadBlock, multiVariant));
      if (type == SkullBlock.Types.PLAYER) {
         this.itemModelOutput.accept(headBlock.asItem(), ItemModelUtils.specialModel(modelLocation, new PlayerHeadSpecialRenderer.Unbaked()));
      } else {
         this.itemModelOutput.accept(headBlock.asItem(), ItemModelUtils.specialModel(modelLocation, new SkullSpecialRenderer.Unbaked(type)));
      }
   }

   private void createHeads() {
      Identifier identifier = ModelLocationUtils.decorateItemModelLocation("template_skull");
      this.createHead(Blocks.CREEPER_HEAD, Blocks.CREEPER_WALL_HEAD, SkullBlock.Types.CREEPER, identifier);
      this.createHead(Blocks.PLAYER_HEAD, Blocks.PLAYER_WALL_HEAD, SkullBlock.Types.PLAYER, identifier);
      this.createHead(Blocks.ZOMBIE_HEAD, Blocks.ZOMBIE_WALL_HEAD, SkullBlock.Types.ZOMBIE, identifier);
      this.createHead(Blocks.SKELETON_SKULL, Blocks.SKELETON_WALL_SKULL, SkullBlock.Types.SKELETON, identifier);
      this.createHead(Blocks.WITHER_SKELETON_SKULL, Blocks.WITHER_SKELETON_WALL_SKULL, SkullBlock.Types.WITHER_SKELETON, identifier);
      this.createHead(Blocks.PIGLIN_HEAD, Blocks.PIGLIN_WALL_HEAD, SkullBlock.Types.PIGLIN, identifier);
      this.createHead(Blocks.DRAGON_HEAD, Blocks.DRAGON_WALL_HEAD, SkullBlock.Types.DRAGON, ModelLocationUtils.getModelLocation(Items.DRAGON_HEAD));
   }

   private void createCopperGolemStatues() {
      this.createCopperGolemStatue(Blocks.COPPER_GOLEM_STATUE, Blocks.COPPER_BLOCK, WeatheringCopper.WeatherState.UNAFFECTED);
      this.createCopperGolemStatue(Blocks.EXPOSED_COPPER_GOLEM_STATUE, Blocks.EXPOSED_COPPER, WeatheringCopper.WeatherState.EXPOSED);
      this.createCopperGolemStatue(Blocks.WEATHERED_COPPER_GOLEM_STATUE, Blocks.WEATHERED_COPPER, WeatheringCopper.WeatherState.WEATHERED);
      this.createCopperGolemStatue(Blocks.OXIDIZED_COPPER_GOLEM_STATUE, Blocks.OXIDIZED_COPPER, WeatheringCopper.WeatherState.OXIDIZED);
      this.copyModel(Blocks.COPPER_GOLEM_STATUE, Blocks.WAXED_COPPER_GOLEM_STATUE);
      this.copyModel(Blocks.EXPOSED_COPPER_GOLEM_STATUE, Blocks.WAXED_EXPOSED_COPPER_GOLEM_STATUE);
      this.copyModel(Blocks.WEATHERED_COPPER_GOLEM_STATUE, Blocks.WAXED_WEATHERED_COPPER_GOLEM_STATUE);
      this.copyModel(Blocks.OXIDIZED_COPPER_GOLEM_STATUE, Blocks.WAXED_OXIDIZED_COPPER_GOLEM_STATUE);
   }

   private void createCopperGolemStatue(Block statueBlock, Block copperBlock, WeatheringCopper.WeatherState weatherState) {
      MultiVariant multiVariant = plainVariant(
         ModelTemplates.PARTICLE_ONLY.create(statueBlock, TextureMapping.particle(TextureMapping.getBlockTexture(copperBlock)), this.modelOutput)
      );
      Identifier identifier = ModelLocationUtils.decorateItemModelLocation("template_copper_golem_statue");
      this.blockStateOutput.accept(createSimpleBlock(statueBlock, multiVariant));
      this.itemModelOutput
         .accept(
            statueBlock.asItem(),
            ItemModelUtils.selectBlockItemProperty(
               CopperGolemStatueBlock.POSE,
               ItemModelUtils.specialModel(identifier, new CopperGolemStatueSpecialRenderer.Unbaked(weatherState, CopperGolemStatueBlock.Pose.STANDING)),
               Map.of(
                  CopperGolemStatueBlock.Pose.SITTING,
                  ItemModelUtils.specialModel(identifier, new CopperGolemStatueSpecialRenderer.Unbaked(weatherState, CopperGolemStatueBlock.Pose.SITTING)),
                  CopperGolemStatueBlock.Pose.STAR,
                  ItemModelUtils.specialModel(identifier, new CopperGolemStatueSpecialRenderer.Unbaked(weatherState, CopperGolemStatueBlock.Pose.STAR)),
                  CopperGolemStatueBlock.Pose.RUNNING,
                  ItemModelUtils.specialModel(identifier, new CopperGolemStatueSpecialRenderer.Unbaked(weatherState, CopperGolemStatueBlock.Pose.RUNNING))
               )
            )
         );
   }

   private void createBanner(Block block, Block wallBlock, DyeColor color) {
      MultiVariant multiVariant = plainVariant(ModelLocationUtils.decorateBlockModelLocation("banner"));
      Identifier identifier = ModelLocationUtils.decorateItemModelLocation("template_banner");
      this.blockStateOutput.accept(createSimpleBlock(block, multiVariant));
      this.blockStateOutput.accept(createSimpleBlock(wallBlock, multiVariant));
      Item item = block.asItem();
      this.itemModelOutput.accept(item, ItemModelUtils.specialModel(identifier, new BannerSpecialRenderer.Unbaked(color)));
   }

   private void createBanners() {
      this.createBanner(Blocks.WHITE_BANNER, Blocks.WHITE_WALL_BANNER, DyeColor.WHITE);
      this.createBanner(Blocks.ORANGE_BANNER, Blocks.ORANGE_WALL_BANNER, DyeColor.ORANGE);
      this.createBanner(Blocks.MAGENTA_BANNER, Blocks.MAGENTA_WALL_BANNER, DyeColor.MAGENTA);
      this.createBanner(Blocks.LIGHT_BLUE_BANNER, Blocks.LIGHT_BLUE_WALL_BANNER, DyeColor.LIGHT_BLUE);
      this.createBanner(Blocks.YELLOW_BANNER, Blocks.YELLOW_WALL_BANNER, DyeColor.YELLOW);
      this.createBanner(Blocks.LIME_BANNER, Blocks.LIME_WALL_BANNER, DyeColor.LIME);
      this.createBanner(Blocks.PINK_BANNER, Blocks.PINK_WALL_BANNER, DyeColor.PINK);
      this.createBanner(Blocks.GRAY_BANNER, Blocks.GRAY_WALL_BANNER, DyeColor.GRAY);
      this.createBanner(Blocks.LIGHT_GRAY_BANNER, Blocks.LIGHT_GRAY_WALL_BANNER, DyeColor.LIGHT_GRAY);
      this.createBanner(Blocks.CYAN_BANNER, Blocks.CYAN_WALL_BANNER, DyeColor.CYAN);
      this.createBanner(Blocks.PURPLE_BANNER, Blocks.PURPLE_WALL_BANNER, DyeColor.PURPLE);
      this.createBanner(Blocks.BLUE_BANNER, Blocks.BLUE_WALL_BANNER, DyeColor.BLUE);
      this.createBanner(Blocks.BROWN_BANNER, Blocks.BROWN_WALL_BANNER, DyeColor.BROWN);
      this.createBanner(Blocks.GREEN_BANNER, Blocks.GREEN_WALL_BANNER, DyeColor.GREEN);
      this.createBanner(Blocks.RED_BANNER, Blocks.RED_WALL_BANNER, DyeColor.RED);
      this.createBanner(Blocks.BLACK_BANNER, Blocks.BLACK_WALL_BANNER, DyeColor.BLACK);
   }

   private void createChest(Block chestBlock, Block particleBlock, Identifier texture, boolean useGiftTexture) {
      this.createParticleOnlyBlock(chestBlock, particleBlock);
      Item item = chestBlock.asItem();
      Identifier identifier = ModelTemplates.CHEST_INVENTORY.create(item, TextureMapping.particle(particleBlock), this.modelOutput);
      ItemModel.Unbaked unbaked = ItemModelUtils.specialModel(identifier, new ChestSpecialRenderer.Unbaked(texture));
      if (useGiftTexture) {
         ItemModel.Unbaked unbaked2 = ItemModelUtils.specialModel(identifier, new ChestSpecialRenderer.Unbaked(ChestSpecialRenderer.GIFT_CHEST_TEXTURE));
         this.itemModelOutput.accept(item, ItemModelUtils.isXmas(unbaked2, unbaked));
      } else {
         this.itemModelOutput.accept(item, unbaked);
      }
   }

   private void createChests() {
      this.createChest(Blocks.CHEST, Blocks.OAK_PLANKS, ChestSpecialRenderer.NORMAL_CHEST_TEXTURE, true);
      this.createChest(Blocks.TRAPPED_CHEST, Blocks.OAK_PLANKS, ChestSpecialRenderer.TRAPPED_CHEST_TEXTURE, true);
      this.createChest(Blocks.ENDER_CHEST, Blocks.OBSIDIAN, ChestSpecialRenderer.ENDER_CHEST_TEXTURE, false);
   }

   private void createCopperChests() {
      this.createChest(Blocks.COPPER_CHEST, Blocks.COPPER_BLOCK, ChestSpecialRenderer.COPPER_CHEST_TEXTURE, false);
      this.createChest(Blocks.EXPOSED_COPPER_CHEST, Blocks.EXPOSED_COPPER, ChestSpecialRenderer.EXPOSED_COPPER_CHEST_TEXTURE, false);
      this.createChest(Blocks.WEATHERED_COPPER_CHEST, Blocks.WEATHERED_COPPER, ChestSpecialRenderer.WEATHERED_COPPER_CHEST_TEXTURE, false);
      this.createChest(Blocks.OXIDIZED_COPPER_CHEST, Blocks.OXIDIZED_COPPER, ChestSpecialRenderer.OXIDIZED_COPPER_CHEST_TEXTURE, false);
      this.copyModel(Blocks.COPPER_CHEST, Blocks.WAXED_COPPER_CHEST);
      this.copyModel(Blocks.EXPOSED_COPPER_CHEST, Blocks.WAXED_EXPOSED_COPPER_CHEST);
      this.copyModel(Blocks.WEATHERED_COPPER_CHEST, Blocks.WAXED_WEATHERED_COPPER_CHEST);
      this.copyModel(Blocks.OXIDIZED_COPPER_CHEST, Blocks.WAXED_OXIDIZED_COPPER_CHEST);
   }

   private void createBed(Block block, Block particleBlock, DyeColor color) {
      MultiVariant multiVariant = plainVariant(ModelLocationUtils.decorateBlockModelLocation("bed"));
      this.blockStateOutput.accept(createSimpleBlock(block, multiVariant));
      Item item = block.asItem();
      Identifier identifier = ModelTemplates.BED_INVENTORY
         .create(ModelLocationUtils.getModelLocation(item), TextureMapping.particle(particleBlock), this.modelOutput);
      this.itemModelOutput.accept(item, ItemModelUtils.specialModel(identifier, new BedSpecialRenderer.Unbaked(color)));
   }

   private void createBeds() {
      this.createBed(Blocks.WHITE_BED, Blocks.WHITE_WOOL, DyeColor.WHITE);
      this.createBed(Blocks.ORANGE_BED, Blocks.ORANGE_WOOL, DyeColor.ORANGE);
      this.createBed(Blocks.MAGENTA_BED, Blocks.MAGENTA_WOOL, DyeColor.MAGENTA);
      this.createBed(Blocks.LIGHT_BLUE_BED, Blocks.LIGHT_BLUE_WOOL, DyeColor.LIGHT_BLUE);
      this.createBed(Blocks.YELLOW_BED, Blocks.YELLOW_WOOL, DyeColor.YELLOW);
      this.createBed(Blocks.LIME_BED, Blocks.LIME_WOOL, DyeColor.LIME);
      this.createBed(Blocks.PINK_BED, Blocks.PINK_WOOL, DyeColor.PINK);
      this.createBed(Blocks.GRAY_BED, Blocks.GRAY_WOOL, DyeColor.GRAY);
      this.createBed(Blocks.LIGHT_GRAY_BED, Blocks.LIGHT_GRAY_WOOL, DyeColor.LIGHT_GRAY);
      this.createBed(Blocks.CYAN_BED, Blocks.CYAN_WOOL, DyeColor.CYAN);
      this.createBed(Blocks.PURPLE_BED, Blocks.PURPLE_WOOL, DyeColor.PURPLE);
      this.createBed(Blocks.BLUE_BED, Blocks.BLUE_WOOL, DyeColor.BLUE);
      this.createBed(Blocks.BROWN_BED, Blocks.BROWN_WOOL, DyeColor.BROWN);
      this.createBed(Blocks.GREEN_BED, Blocks.GREEN_WOOL, DyeColor.GREEN);
      this.createBed(Blocks.RED_BED, Blocks.RED_WOOL, DyeColor.RED);
      this.createBed(Blocks.BLACK_BED, Blocks.BLACK_WOOL, DyeColor.BLACK);
   }

   private void generateSimpleSpecialItemModel(Block block, SpecialModelRenderer.Unbaked specialModel) {
      Item item = block.asItem();
      Identifier identifier = ModelLocationUtils.getModelLocation(item);
      this.itemModelOutput.accept(item, ItemModelUtils.specialModel(identifier, specialModel));
   }

   public void run() {
      BlockFamilies.getAllFamilies()
         .filter(BlockFamily::shouldGenerateModel)
         .forEach(blockFamily -> this.family(blockFamily.getBaseBlock()).generateFor(blockFamily));
      this.family(Blocks.CUT_COPPER)
         .generateFor(BlockFamilies.CUT_COPPER)
         .donateModelTo(Blocks.CUT_COPPER, Blocks.WAXED_CUT_COPPER)
         .donateModelTo(Blocks.CHISELED_COPPER, Blocks.WAXED_CHISELED_COPPER)
         .generateFor(BlockFamilies.WAXED_CUT_COPPER);
      this.family(Blocks.EXPOSED_CUT_COPPER)
         .generateFor(BlockFamilies.EXPOSED_CUT_COPPER)
         .donateModelTo(Blocks.EXPOSED_CUT_COPPER, Blocks.WAXED_EXPOSED_CUT_COPPER)
         .donateModelTo(Blocks.EXPOSED_CHISELED_COPPER, Blocks.WAXED_EXPOSED_CHISELED_COPPER)
         .generateFor(BlockFamilies.WAXED_EXPOSED_CUT_COPPER);
      this.family(Blocks.WEATHERED_CUT_COPPER)
         .generateFor(BlockFamilies.WEATHERED_CUT_COPPER)
         .donateModelTo(Blocks.WEATHERED_CUT_COPPER, Blocks.WAXED_WEATHERED_CUT_COPPER)
         .donateModelTo(Blocks.WEATHERED_CHISELED_COPPER, Blocks.WAXED_WEATHERED_CHISELED_COPPER)
         .generateFor(BlockFamilies.WAXED_WEATHERED_CUT_COPPER);
      this.family(Blocks.OXIDIZED_CUT_COPPER)
         .generateFor(BlockFamilies.OXIDIZED_CUT_COPPER)
         .donateModelTo(Blocks.OXIDIZED_CUT_COPPER, Blocks.WAXED_OXIDIZED_CUT_COPPER)
         .donateModelTo(Blocks.OXIDIZED_CHISELED_COPPER, Blocks.WAXED_OXIDIZED_CHISELED_COPPER)
         .generateFor(BlockFamilies.WAXED_OXIDIZED_CUT_COPPER);
      this.createCopperBulb(Blocks.COPPER_BULB);
      this.createCopperBulb(Blocks.EXPOSED_COPPER_BULB);
      this.createCopperBulb(Blocks.WEATHERED_COPPER_BULB);
      this.createCopperBulb(Blocks.OXIDIZED_COPPER_BULB);
      this.copyCopperBulbModel(Blocks.COPPER_BULB, Blocks.WAXED_COPPER_BULB);
      this.copyCopperBulbModel(Blocks.EXPOSED_COPPER_BULB, Blocks.WAXED_EXPOSED_COPPER_BULB);
      this.copyCopperBulbModel(Blocks.WEATHERED_COPPER_BULB, Blocks.WAXED_WEATHERED_COPPER_BULB);
      this.copyCopperBulbModel(Blocks.OXIDIZED_COPPER_BULB, Blocks.WAXED_OXIDIZED_COPPER_BULB);
      this.createNonTemplateModelBlock(Blocks.AIR);
      this.createNonTemplateModelBlock(Blocks.CAVE_AIR, Blocks.AIR);
      this.createNonTemplateModelBlock(Blocks.VOID_AIR, Blocks.AIR);
      this.createNonTemplateModelBlock(Blocks.BEACON);
      this.createNonTemplateModelBlock(Blocks.CACTUS);
      this.createNonTemplateModelBlock(Blocks.BUBBLE_COLUMN, Blocks.WATER);
      this.createNonTemplateModelBlock(Blocks.DRAGON_EGG);
      this.createNonTemplateModelBlock(Blocks.DRIED_KELP_BLOCK);
      this.createNonTemplateModelBlock(Blocks.ENCHANTING_TABLE);
      this.createNonTemplateModelBlock(Blocks.FLOWER_POT);
      this.registerSimpleFlatItemModel(Items.FLOWER_POT);
      this.createNonTemplateModelBlock(Blocks.HONEY_BLOCK);
      this.createNonTemplateModelBlock(Blocks.WATER);
      this.createNonTemplateModelBlock(Blocks.LAVA);
      this.createNonTemplateModelBlock(Blocks.SLIME_BLOCK);
      this.registerSimpleFlatItemModel(Items.IRON_CHAIN);
      Items.COPPER_CHAIN.waxedMapping().forEach(this::createCopperChainItem);
      this.createCandleAndCandleCake(Blocks.WHITE_CANDLE, Blocks.WHITE_CANDLE_CAKE);
      this.createCandleAndCandleCake(Blocks.ORANGE_CANDLE, Blocks.ORANGE_CANDLE_CAKE);
      this.createCandleAndCandleCake(Blocks.MAGENTA_CANDLE, Blocks.MAGENTA_CANDLE_CAKE);
      this.createCandleAndCandleCake(Blocks.LIGHT_BLUE_CANDLE, Blocks.LIGHT_BLUE_CANDLE_CAKE);
      this.createCandleAndCandleCake(Blocks.YELLOW_CANDLE, Blocks.YELLOW_CANDLE_CAKE);
      this.createCandleAndCandleCake(Blocks.LIME_CANDLE, Blocks.LIME_CANDLE_CAKE);
      this.createCandleAndCandleCake(Blocks.PINK_CANDLE, Blocks.PINK_CANDLE_CAKE);
      this.createCandleAndCandleCake(Blocks.GRAY_CANDLE, Blocks.GRAY_CANDLE_CAKE);
      this.createCandleAndCandleCake(Blocks.LIGHT_GRAY_CANDLE, Blocks.LIGHT_GRAY_CANDLE_CAKE);
      this.createCandleAndCandleCake(Blocks.CYAN_CANDLE, Blocks.CYAN_CANDLE_CAKE);
      this.createCandleAndCandleCake(Blocks.PURPLE_CANDLE, Blocks.PURPLE_CANDLE_CAKE);
      this.createCandleAndCandleCake(Blocks.BLUE_CANDLE, Blocks.BLUE_CANDLE_CAKE);
      this.createCandleAndCandleCake(Blocks.BROWN_CANDLE, Blocks.BROWN_CANDLE_CAKE);
      this.createCandleAndCandleCake(Blocks.GREEN_CANDLE, Blocks.GREEN_CANDLE_CAKE);
      this.createCandleAndCandleCake(Blocks.RED_CANDLE, Blocks.RED_CANDLE_CAKE);
      this.createCandleAndCandleCake(Blocks.BLACK_CANDLE, Blocks.BLACK_CANDLE_CAKE);
      this.createCandleAndCandleCake(Blocks.CANDLE, Blocks.CANDLE_CAKE);
      this.createNonTemplateModelBlock(Blocks.POTTED_BAMBOO);
      this.createNonTemplateModelBlock(Blocks.POTTED_CACTUS);
      this.createNonTemplateModelBlock(Blocks.POWDER_SNOW);
      this.createNonTemplateModelBlock(Blocks.SPORE_BLOSSOM);
      this.createAzalea(Blocks.AZALEA);
      this.createAzalea(Blocks.FLOWERING_AZALEA);
      this.createPottedAzalea(Blocks.POTTED_AZALEA);
      this.createPottedAzalea(Blocks.POTTED_FLOWERING_AZALEA);
      this.createCaveVines();
      this.createFullAndCarpetBlocks(Blocks.MOSS_BLOCK, Blocks.MOSS_CARPET);
      this.createMossyCarpet(Blocks.PALE_MOSS_CARPET);
      this.createHangingMoss(Blocks.PALE_HANGING_MOSS);
      this.createTrivialCube(Blocks.PALE_MOSS_BLOCK);
      this.createFlowerBed(Blocks.PINK_PETALS);
      this.createFlowerBed(Blocks.WILDFLOWERS);
      this.createLeafLitter(Blocks.LEAF_LITTER);
      this.createCrossBlock(Blocks.FIREFLY_BUSH, BlockModelGenerators.PlantType.EMISSIVE_NOT_TINTED);
      this.registerSimpleFlatItemModel(Items.FIREFLY_BUSH);
      this.createAirLikeBlock(Blocks.BARRIER, Items.BARRIER);
      this.registerSimpleFlatItemModel(Items.BARRIER);
      this.createLightBlock();
      this.createAirLikeBlock(Blocks.STRUCTURE_VOID, Items.STRUCTURE_VOID);
      this.registerSimpleFlatItemModel(Items.STRUCTURE_VOID);
      this.createAirLikeBlock(Blocks.MOVING_PISTON, TextureMapping.getBlockTexture(Blocks.PISTON, "_side"));
      this.createTrivialCube(Blocks.COAL_ORE);
      this.createTrivialCube(Blocks.DEEPSLATE_COAL_ORE);
      this.createTrivialCube(Blocks.COAL_BLOCK);
      this.createTrivialCube(Blocks.DIAMOND_ORE);
      this.createTrivialCube(Blocks.DEEPSLATE_DIAMOND_ORE);
      this.createTrivialCube(Blocks.DIAMOND_BLOCK);
      this.createTrivialCube(Blocks.EMERALD_ORE);
      this.createTrivialCube(Blocks.DEEPSLATE_EMERALD_ORE);
      this.createTrivialCube(Blocks.EMERALD_BLOCK);
      this.createTrivialCube(Blocks.GOLD_ORE);
      this.createTrivialCube(Blocks.NETHER_GOLD_ORE);
      this.createTrivialCube(Blocks.DEEPSLATE_GOLD_ORE);
      this.createTrivialCube(Blocks.GOLD_BLOCK);
      this.createTrivialCube(Blocks.IRON_ORE);
      this.createTrivialCube(Blocks.DEEPSLATE_IRON_ORE);
      this.createTrivialCube(Blocks.IRON_BLOCK);
      this.createTrivialBlock(Blocks.ANCIENT_DEBRIS, TexturedModel.COLUMN);
      this.createTrivialCube(Blocks.NETHERITE_BLOCK);
      this.createTrivialCube(Blocks.LAPIS_ORE);
      this.createTrivialCube(Blocks.DEEPSLATE_LAPIS_ORE);
      this.createTrivialCube(Blocks.LAPIS_BLOCK);
      this.createTrivialCube(Blocks.RESIN_BLOCK);
      this.createTrivialCube(Blocks.NETHER_QUARTZ_ORE);
      this.createTrivialCube(Blocks.REDSTONE_ORE);
      this.createTrivialCube(Blocks.DEEPSLATE_REDSTONE_ORE);
      this.createTrivialCube(Blocks.REDSTONE_BLOCK);
      this.createTrivialCube(Blocks.GILDED_BLACKSTONE);
      this.createTrivialCube(Blocks.BLUE_ICE);
      this.createTrivialCube(Blocks.CLAY);
      this.createTrivialCube(Blocks.COARSE_DIRT);
      this.createTrivialCube(Blocks.CRYING_OBSIDIAN);
      this.createTrivialCube(Blocks.END_STONE);
      this.createTrivialCube(Blocks.GLOWSTONE);
      this.createTrivialCube(Blocks.GRAVEL);
      this.createTrivialCube(Blocks.HONEYCOMB_BLOCK);
      this.createTrivialCube(Blocks.ICE);
      this.createTrivialBlock(Blocks.JUKEBOX, TexturedModel.CUBE_TOP);
      this.createTrivialBlock(Blocks.LODESTONE, TexturedModel.COLUMN);
      this.createTrivialBlock(Blocks.MELON, TexturedModel.COLUMN);
      this.createNonTemplateModelBlock(Blocks.MANGROVE_ROOTS);
      this.createNonTemplateModelBlock(Blocks.POTTED_MANGROVE_PROPAGULE);
      this.createTrivialCube(Blocks.NETHER_WART_BLOCK);
      this.createTrivialCube(Blocks.NOTE_BLOCK);
      this.createTrivialCube(Blocks.PACKED_ICE);
      this.createTrivialCube(Blocks.OBSIDIAN);
      this.createTrivialCube(Blocks.QUARTZ_BRICKS);
      this.createTrivialCube(Blocks.SEA_LANTERN);
      this.createTrivialCube(Blocks.SHROOMLIGHT);
      this.createTrivialCube(Blocks.SOUL_SAND);
      this.createTrivialCube(Blocks.SOUL_SOIL);
      this.createTrivialBlock(Blocks.SPAWNER, TexturedModel.CUBE_INNER_FACES);
      this.createCreakingHeart(Blocks.CREAKING_HEART);
      this.createTrivialCube(Blocks.SPONGE);
      this.createTrivialBlock(Blocks.SEAGRASS, TexturedModel.SEAGRASS);
      this.registerSimpleFlatItemModel(Items.SEAGRASS);
      this.createTrivialBlock(Blocks.TNT, TexturedModel.CUBE_TOP_BOTTOM);
      this.createTrivialBlock(Blocks.TARGET, TexturedModel.COLUMN);
      this.createTrivialCube(Blocks.WARPED_WART_BLOCK);
      this.createTrivialCube(Blocks.WET_SPONGE);
      this.createTrivialCube(Blocks.AMETHYST_BLOCK);
      this.createTrivialCube(Blocks.BUDDING_AMETHYST);
      this.createTrivialCube(Blocks.CALCITE);
      this.createTrivialCube(Blocks.DRIPSTONE_BLOCK);
      this.createTrivialCube(Blocks.RAW_IRON_BLOCK);
      this.createTrivialCube(Blocks.RAW_COPPER_BLOCK);
      this.createTrivialCube(Blocks.RAW_GOLD_BLOCK);
      this.createRotatedMirroredVariantBlock(Blocks.SCULK);
      this.createNonTemplateModelBlock(Blocks.HEAVY_CORE);
      this.createPetrifiedOakSlab();
      this.createTrivialCube(Blocks.COPPER_ORE);
      this.createTrivialCube(Blocks.DEEPSLATE_COPPER_ORE);
      this.createTrivialCube(Blocks.COPPER_BLOCK);
      this.createTrivialCube(Blocks.EXPOSED_COPPER);
      this.createTrivialCube(Blocks.WEATHERED_COPPER);
      this.createTrivialCube(Blocks.OXIDIZED_COPPER);
      this.copyModel(Blocks.COPPER_BLOCK, Blocks.WAXED_COPPER_BLOCK);
      this.copyModel(Blocks.EXPOSED_COPPER, Blocks.WAXED_EXPOSED_COPPER);
      this.copyModel(Blocks.WEATHERED_COPPER, Blocks.WAXED_WEATHERED_COPPER);
      this.copyModel(Blocks.OXIDIZED_COPPER, Blocks.WAXED_OXIDIZED_COPPER);
      this.createDoor(Blocks.COPPER_DOOR);
      this.createDoor(Blocks.EXPOSED_COPPER_DOOR);
      this.createDoor(Blocks.WEATHERED_COPPER_DOOR);
      this.createDoor(Blocks.OXIDIZED_COPPER_DOOR);
      this.copyDoorModel(Blocks.COPPER_DOOR, Blocks.WAXED_COPPER_DOOR);
      this.copyDoorModel(Blocks.EXPOSED_COPPER_DOOR, Blocks.WAXED_EXPOSED_COPPER_DOOR);
      this.copyDoorModel(Blocks.WEATHERED_COPPER_DOOR, Blocks.WAXED_WEATHERED_COPPER_DOOR);
      this.copyDoorModel(Blocks.OXIDIZED_COPPER_DOOR, Blocks.WAXED_OXIDIZED_COPPER_DOOR);
      this.createTrapdoor(Blocks.COPPER_TRAPDOOR);
      this.createTrapdoor(Blocks.EXPOSED_COPPER_TRAPDOOR);
      this.createTrapdoor(Blocks.WEATHERED_COPPER_TRAPDOOR);
      this.createTrapdoor(Blocks.OXIDIZED_COPPER_TRAPDOOR);
      this.copyTrapdoorModel(Blocks.COPPER_TRAPDOOR, Blocks.WAXED_COPPER_TRAPDOOR);
      this.copyTrapdoorModel(Blocks.EXPOSED_COPPER_TRAPDOOR, Blocks.WAXED_EXPOSED_COPPER_TRAPDOOR);
      this.copyTrapdoorModel(Blocks.WEATHERED_COPPER_TRAPDOOR, Blocks.WAXED_WEATHERED_COPPER_TRAPDOOR);
      this.copyTrapdoorModel(Blocks.OXIDIZED_COPPER_TRAPDOOR, Blocks.WAXED_OXIDIZED_COPPER_TRAPDOOR);
      this.createTrivialCube(Blocks.COPPER_GRATE);
      this.createTrivialCube(Blocks.EXPOSED_COPPER_GRATE);
      this.createTrivialCube(Blocks.WEATHERED_COPPER_GRATE);
      this.createTrivialCube(Blocks.OXIDIZED_COPPER_GRATE);
      this.copyModel(Blocks.COPPER_GRATE, Blocks.WAXED_COPPER_GRATE);
      this.copyModel(Blocks.EXPOSED_COPPER_GRATE, Blocks.WAXED_EXPOSED_COPPER_GRATE);
      this.copyModel(Blocks.WEATHERED_COPPER_GRATE, Blocks.WAXED_WEATHERED_COPPER_GRATE);
      this.copyModel(Blocks.OXIDIZED_COPPER_GRATE, Blocks.WAXED_OXIDIZED_COPPER_GRATE);
      this.createLightningRod(Blocks.LIGHTNING_ROD, Blocks.WAXED_LIGHTNING_ROD);
      this.createLightningRod(Blocks.EXPOSED_LIGHTNING_ROD, Blocks.WAXED_EXPOSED_LIGHTNING_ROD);
      this.createLightningRod(Blocks.WEATHERED_LIGHTNING_ROD, Blocks.WAXED_WEATHERED_LIGHTNING_ROD);
      this.createLightningRod(Blocks.OXIDIZED_LIGHTNING_ROD, Blocks.WAXED_OXIDIZED_LIGHTNING_ROD);
      this.createWeightedPressurePlate(Blocks.LIGHT_WEIGHTED_PRESSURE_PLATE, Blocks.GOLD_BLOCK);
      this.createWeightedPressurePlate(Blocks.HEAVY_WEIGHTED_PRESSURE_PLATE, Blocks.IRON_BLOCK);
      this.createShelf(Blocks.ACACIA_SHELF, Blocks.STRIPPED_ACACIA_LOG);
      this.createShelf(Blocks.BAMBOO_SHELF, Blocks.STRIPPED_BAMBOO_BLOCK);
      this.createShelf(Blocks.BIRCH_SHELF, Blocks.STRIPPED_BIRCH_LOG);
      this.createShelf(Blocks.CHERRY_SHELF, Blocks.STRIPPED_CHERRY_LOG);
      this.createShelf(Blocks.CRIMSON_SHELF, Blocks.STRIPPED_CRIMSON_STEM);
      this.createShelf(Blocks.DARK_OAK_SHELF, Blocks.STRIPPED_DARK_OAK_LOG);
      this.createShelf(Blocks.JUNGLE_SHELF, Blocks.STRIPPED_JUNGLE_LOG);
      this.createShelf(Blocks.MANGROVE_SHELF, Blocks.STRIPPED_MANGROVE_LOG);
      this.createShelf(Blocks.OAK_SHELF, Blocks.STRIPPED_OAK_LOG);
      this.createShelf(Blocks.PALE_OAK_SHELF, Blocks.STRIPPED_PALE_OAK_LOG);
      this.createShelf(Blocks.SPRUCE_SHELF, Blocks.STRIPPED_SPRUCE_LOG);
      this.createShelf(Blocks.WARPED_SHELF, Blocks.STRIPPED_WARPED_STEM);
      this.createAmethystClusters();
      this.createBookshelf();
      this.createChiseledBookshelf();
      this.createBrewingStand();
      this.createCakeBlock();
      this.createCampfires(Blocks.CAMPFIRE, Blocks.SOUL_CAMPFIRE);
      this.createCartographyTable();
      this.createCauldrons();
      this.createChorusFlower();
      this.createChorusPlant();
      this.createComposter();
      this.createDaylightDetector();
      this.createEndPortalFrame();
      this.createRotatableColumn(Blocks.END_ROD);
      this.createFarmland();
      this.createFire();
      this.createSoulFire();
      this.createFrostedIce();
      this.createGrassBlocks();
      this.createCocoa();
      this.createDirtPath();
      this.createGrindstone();
      this.createHopper();
      this.createBarsAndItem(Blocks.IRON_BARS);
      Blocks.COPPER_BARS.waxedMapping().forEach(this::createBarsAndItem);
      this.createLever();
      this.createLilyPad();
      this.createNetherPortalBlock();
      this.createNetherrack();
      this.createObserver();
      this.createPistons();
      this.createPistonHeads();
      this.createScaffolding();
      this.createRedstoneTorch();
      this.createRedstoneLamp();
      this.createRepeater();
      this.createSeaPickle();
      this.createSmithingTable();
      this.createSnowBlocks();
      this.createStonecutter();
      this.createStructureBlock();
      this.createSweetBerryBush();
      this.createTestBlock();
      this.createTrivialCube(Blocks.TEST_INSTANCE_BLOCK);
      this.createTripwire();
      this.createTripwireHook();
      this.createTurtleEgg();
      this.createSnifferEgg();
      this.createDriedGhastBlock();
      this.createVine();
      this.createMultiface(Blocks.GLOW_LICHEN);
      this.createMultiface(Blocks.SCULK_VEIN);
      this.createMultiface(Blocks.RESIN_CLUMP, Items.RESIN_CLUMP);
      this.createMagmaBlock();
      this.createJigsaw();
      this.createSculkSensor();
      this.createCalibratedSculkSensor();
      this.createSculkShrieker();
      this.createFrogspawnBlock();
      this.createMangrovePropagule();
      this.createMuddyMangroveRoots();
      this.createTrialSpawner();
      this.createVault();
      this.createNonTemplateHorizontalBlock(Blocks.LADDER);
      this.registerSimpleFlatItemModel(Blocks.LADDER);
      this.createNonTemplateHorizontalBlock(Blocks.LECTERN);
      this.createBigDripLeafBlock();
      this.createNonTemplateHorizontalBlock(Blocks.BIG_DRIPLEAF_STEM);
      this.createNormalTorch(Blocks.TORCH, Blocks.WALL_TORCH);
      this.createNormalTorch(Blocks.SOUL_TORCH, Blocks.SOUL_WALL_TORCH);
      this.createNormalTorch(Blocks.COPPER_TORCH, Blocks.COPPER_WALL_TORCH);
      this.createCraftingTableLike(Blocks.CRAFTING_TABLE, Blocks.OAK_PLANKS, TextureMapping::craftingTable);
      this.createCraftingTableLike(Blocks.FLETCHING_TABLE, Blocks.BIRCH_PLANKS, TextureMapping::fletchingTable);
      this.createNyliumBlock(Blocks.CRIMSON_NYLIUM);
      this.createNyliumBlock(Blocks.WARPED_NYLIUM);
      this.createDispenserBlock(Blocks.DISPENSER);
      this.createDispenserBlock(Blocks.DROPPER);
      this.createCrafterBlock();
      this.createLantern(Blocks.LANTERN);
      this.createLantern(Blocks.SOUL_LANTERN);
      Blocks.COPPER_LANTERN.waxedMapping().forEach(this::createCopperLantern);
      this.createAxisAlignedPillarBlockCustomModel(Blocks.IRON_CHAIN, plainVariant(TexturedModel.CHAIN.create(Blocks.IRON_CHAIN, this.modelOutput)));
      Blocks.COPPER_CHAIN.waxedMapping().forEach(this::createCopperChain);
      this.createAxisAlignedPillarBlock(Blocks.BASALT, TexturedModel.COLUMN);
      this.createAxisAlignedPillarBlock(Blocks.POLISHED_BASALT, TexturedModel.COLUMN);
      this.createTrivialCube(Blocks.SMOOTH_BASALT);
      this.createAxisAlignedPillarBlock(Blocks.BONE_BLOCK, TexturedModel.COLUMN);
      this.createRotatedVariantBlock(Blocks.DIRT);
      this.createRotatedVariantBlock(Blocks.ROOTED_DIRT);
      this.createRotatedVariantBlock(Blocks.SAND);
      this.createBrushableBlock(Blocks.SUSPICIOUS_SAND);
      this.createBrushableBlock(Blocks.SUSPICIOUS_GRAVEL);
      this.createRotatedVariantBlock(Blocks.RED_SAND);
      this.createRotatedMirroredVariantBlock(Blocks.BEDROCK);
      this.createTrivialBlock(Blocks.REINFORCED_DEEPSLATE, TexturedModel.CUBE_TOP_BOTTOM);
      this.createRotatedPillarWithHorizontalVariant(Blocks.HAY_BLOCK, TexturedModel.COLUMN, TexturedModel.COLUMN_HORIZONTAL);
      this.createRotatedPillarWithHorizontalVariant(Blocks.PURPUR_PILLAR, TexturedModel.COLUMN_ALT, TexturedModel.COLUMN_HORIZONTAL_ALT);
      this.createRotatedPillarWithHorizontalVariant(Blocks.QUARTZ_PILLAR, TexturedModel.COLUMN_ALT, TexturedModel.COLUMN_HORIZONTAL_ALT);
      this.createRotatedPillarWithHorizontalVariant(Blocks.OCHRE_FROGLIGHT, TexturedModel.COLUMN, TexturedModel.COLUMN_HORIZONTAL);
      this.createRotatedPillarWithHorizontalVariant(Blocks.VERDANT_FROGLIGHT, TexturedModel.COLUMN, TexturedModel.COLUMN_HORIZONTAL);
      this.createRotatedPillarWithHorizontalVariant(Blocks.PEARLESCENT_FROGLIGHT, TexturedModel.COLUMN, TexturedModel.COLUMN_HORIZONTAL);
      this.createHorizontallyRotatedBlock(Blocks.LOOM, TexturedModel.ORIENTABLE);
      this.createPumpkins();
      this.createBeeNest(Blocks.BEE_NEST, TextureMapping::orientableCube);
      this.createBeeNest(Blocks.BEEHIVE, TextureMapping::orientableCubeSameEnds);
      this.createCropBlock(Blocks.BEETROOTS, BlockStateProperties.AGE_3, 0, 1, 2, 3);
      this.createCropBlock(Blocks.CARROTS, BlockStateProperties.AGE_7, 0, 0, 1, 1, 2, 2, 2, 3);
      this.createCropBlock(Blocks.NETHER_WART, BlockStateProperties.AGE_3, 0, 1, 1, 2);
      this.createCropBlock(Blocks.POTATOES, BlockStateProperties.AGE_7, 0, 0, 1, 1, 2, 2, 2, 3);
      this.createCropBlock(Blocks.WHEAT, BlockStateProperties.AGE_7, 0, 1, 2, 3, 4, 5, 6, 7);
      this.createCrossBlock(Blocks.TORCHFLOWER_CROP, BlockModelGenerators.PlantType.NOT_TINTED, BlockStateProperties.AGE_1, 0, 1);
      this.createPitcherCrop();
      this.createPitcherPlant();
      this.createBanners();
      this.createBeds();
      this.createHeads();
      this.createChests();
      this.createCopperChests();
      this.createShulkerBox(Blocks.SHULKER_BOX, null);
      this.createShulkerBox(Blocks.WHITE_SHULKER_BOX, DyeColor.WHITE);
      this.createShulkerBox(Blocks.ORANGE_SHULKER_BOX, DyeColor.ORANGE);
      this.createShulkerBox(Blocks.MAGENTA_SHULKER_BOX, DyeColor.MAGENTA);
      this.createShulkerBox(Blocks.LIGHT_BLUE_SHULKER_BOX, DyeColor.LIGHT_BLUE);
      this.createShulkerBox(Blocks.YELLOW_SHULKER_BOX, DyeColor.YELLOW);
      this.createShulkerBox(Blocks.LIME_SHULKER_BOX, DyeColor.LIME);
      this.createShulkerBox(Blocks.PINK_SHULKER_BOX, DyeColor.PINK);
      this.createShulkerBox(Blocks.GRAY_SHULKER_BOX, DyeColor.GRAY);
      this.createShulkerBox(Blocks.LIGHT_GRAY_SHULKER_BOX, DyeColor.LIGHT_GRAY);
      this.createShulkerBox(Blocks.CYAN_SHULKER_BOX, DyeColor.CYAN);
      this.createShulkerBox(Blocks.PURPLE_SHULKER_BOX, DyeColor.PURPLE);
      this.createShulkerBox(Blocks.BLUE_SHULKER_BOX, DyeColor.BLUE);
      this.createShulkerBox(Blocks.BROWN_SHULKER_BOX, DyeColor.BROWN);
      this.createShulkerBox(Blocks.GREEN_SHULKER_BOX, DyeColor.GREEN);
      this.createShulkerBox(Blocks.RED_SHULKER_BOX, DyeColor.RED);
      this.createShulkerBox(Blocks.BLACK_SHULKER_BOX, DyeColor.BLACK);
      this.createCopperGolemStatues();
      this.createParticleOnlyBlock(Blocks.CONDUIT);
      this.generateSimpleSpecialItemModel(Blocks.CONDUIT, new ConduitSpecialRenderer.Unbaked());
      this.createParticleOnlyBlock(Blocks.DECORATED_POT, Blocks.TERRACOTTA);
      this.generateSimpleSpecialItemModel(Blocks.DECORATED_POT, new DecoratedPotSpecialRenderer.Unbaked());
      this.createParticleOnlyBlock(Blocks.END_PORTAL, Blocks.OBSIDIAN);
      this.createParticleOnlyBlock(Blocks.END_GATEWAY, Blocks.OBSIDIAN);
      this.createTrivialCube(Blocks.AZALEA_LEAVES);
      this.createTrivialCube(Blocks.FLOWERING_AZALEA_LEAVES);
      this.createTrivialCube(Blocks.WHITE_CONCRETE);
      this.createTrivialCube(Blocks.ORANGE_CONCRETE);
      this.createTrivialCube(Blocks.MAGENTA_CONCRETE);
      this.createTrivialCube(Blocks.LIGHT_BLUE_CONCRETE);
      this.createTrivialCube(Blocks.YELLOW_CONCRETE);
      this.createTrivialCube(Blocks.LIME_CONCRETE);
      this.createTrivialCube(Blocks.PINK_CONCRETE);
      this.createTrivialCube(Blocks.GRAY_CONCRETE);
      this.createTrivialCube(Blocks.LIGHT_GRAY_CONCRETE);
      this.createTrivialCube(Blocks.CYAN_CONCRETE);
      this.createTrivialCube(Blocks.PURPLE_CONCRETE);
      this.createTrivialCube(Blocks.BLUE_CONCRETE);
      this.createTrivialCube(Blocks.BROWN_CONCRETE);
      this.createTrivialCube(Blocks.GREEN_CONCRETE);
      this.createTrivialCube(Blocks.RED_CONCRETE);
      this.createTrivialCube(Blocks.BLACK_CONCRETE);
      this.createColoredBlockWithRandomRotations(
         TexturedModel.CUBE,
         Blocks.WHITE_CONCRETE_POWDER,
         Blocks.ORANGE_CONCRETE_POWDER,
         Blocks.MAGENTA_CONCRETE_POWDER,
         Blocks.LIGHT_BLUE_CONCRETE_POWDER,
         Blocks.YELLOW_CONCRETE_POWDER,
         Blocks.LIME_CONCRETE_POWDER,
         Blocks.PINK_CONCRETE_POWDER,
         Blocks.GRAY_CONCRETE_POWDER,
         Blocks.LIGHT_GRAY_CONCRETE_POWDER,
         Blocks.CYAN_CONCRETE_POWDER,
         Blocks.PURPLE_CONCRETE_POWDER,
         Blocks.BLUE_CONCRETE_POWDER,
         Blocks.BROWN_CONCRETE_POWDER,
         Blocks.GREEN_CONCRETE_POWDER,
         Blocks.RED_CONCRETE_POWDER,
         Blocks.BLACK_CONCRETE_POWDER
      );
      this.createTrivialCube(Blocks.TERRACOTTA);
      this.createTrivialCube(Blocks.WHITE_TERRACOTTA);
      this.createTrivialCube(Blocks.ORANGE_TERRACOTTA);
      this.createTrivialCube(Blocks.MAGENTA_TERRACOTTA);
      this.createTrivialCube(Blocks.LIGHT_BLUE_TERRACOTTA);
      this.createTrivialCube(Blocks.YELLOW_TERRACOTTA);
      this.createTrivialCube(Blocks.LIME_TERRACOTTA);
      this.createTrivialCube(Blocks.PINK_TERRACOTTA);
      this.createTrivialCube(Blocks.GRAY_TERRACOTTA);
      this.createTrivialCube(Blocks.LIGHT_GRAY_TERRACOTTA);
      this.createTrivialCube(Blocks.CYAN_TERRACOTTA);
      this.createTrivialCube(Blocks.PURPLE_TERRACOTTA);
      this.createTrivialCube(Blocks.BLUE_TERRACOTTA);
      this.createTrivialCube(Blocks.BROWN_TERRACOTTA);
      this.createTrivialCube(Blocks.GREEN_TERRACOTTA);
      this.createTrivialCube(Blocks.RED_TERRACOTTA);
      this.createTrivialCube(Blocks.BLACK_TERRACOTTA);
      this.createTrivialCube(Blocks.TINTED_GLASS);
      this.createGlassBlocks(Blocks.GLASS, Blocks.GLASS_PANE);
      this.createGlassBlocks(Blocks.WHITE_STAINED_GLASS, Blocks.WHITE_STAINED_GLASS_PANE);
      this.createGlassBlocks(Blocks.ORANGE_STAINED_GLASS, Blocks.ORANGE_STAINED_GLASS_PANE);
      this.createGlassBlocks(Blocks.MAGENTA_STAINED_GLASS, Blocks.MAGENTA_STAINED_GLASS_PANE);
      this.createGlassBlocks(Blocks.LIGHT_BLUE_STAINED_GLASS, Blocks.LIGHT_BLUE_STAINED_GLASS_PANE);
      this.createGlassBlocks(Blocks.YELLOW_STAINED_GLASS, Blocks.YELLOW_STAINED_GLASS_PANE);
      this.createGlassBlocks(Blocks.LIME_STAINED_GLASS, Blocks.LIME_STAINED_GLASS_PANE);
      this.createGlassBlocks(Blocks.PINK_STAINED_GLASS, Blocks.PINK_STAINED_GLASS_PANE);
      this.createGlassBlocks(Blocks.GRAY_STAINED_GLASS, Blocks.GRAY_STAINED_GLASS_PANE);
      this.createGlassBlocks(Blocks.LIGHT_GRAY_STAINED_GLASS, Blocks.LIGHT_GRAY_STAINED_GLASS_PANE);
      this.createGlassBlocks(Blocks.CYAN_STAINED_GLASS, Blocks.CYAN_STAINED_GLASS_PANE);
      this.createGlassBlocks(Blocks.PURPLE_STAINED_GLASS, Blocks.PURPLE_STAINED_GLASS_PANE);
      this.createGlassBlocks(Blocks.BLUE_STAINED_GLASS, Blocks.BLUE_STAINED_GLASS_PANE);
      this.createGlassBlocks(Blocks.BROWN_STAINED_GLASS, Blocks.BROWN_STAINED_GLASS_PANE);
      this.createGlassBlocks(Blocks.GREEN_STAINED_GLASS, Blocks.GREEN_STAINED_GLASS_PANE);
      this.createGlassBlocks(Blocks.RED_STAINED_GLASS, Blocks.RED_STAINED_GLASS_PANE);
      this.createGlassBlocks(Blocks.BLACK_STAINED_GLASS, Blocks.BLACK_STAINED_GLASS_PANE);
      this.createColoredBlockWithStateRotations(
         TexturedModel.GLAZED_TERRACOTTA,
         Blocks.WHITE_GLAZED_TERRACOTTA,
         Blocks.ORANGE_GLAZED_TERRACOTTA,
         Blocks.MAGENTA_GLAZED_TERRACOTTA,
         Blocks.LIGHT_BLUE_GLAZED_TERRACOTTA,
         Blocks.YELLOW_GLAZED_TERRACOTTA,
         Blocks.LIME_GLAZED_TERRACOTTA,
         Blocks.PINK_GLAZED_TERRACOTTA,
         Blocks.GRAY_GLAZED_TERRACOTTA,
         Blocks.LIGHT_GRAY_GLAZED_TERRACOTTA,
         Blocks.CYAN_GLAZED_TERRACOTTA,
         Blocks.PURPLE_GLAZED_TERRACOTTA,
         Blocks.BLUE_GLAZED_TERRACOTTA,
         Blocks.BROWN_GLAZED_TERRACOTTA,
         Blocks.GREEN_GLAZED_TERRACOTTA,
         Blocks.RED_GLAZED_TERRACOTTA,
         Blocks.BLACK_GLAZED_TERRACOTTA
      );
      this.createFullAndCarpetBlocks(Blocks.WHITE_WOOL, Blocks.WHITE_CARPET);
      this.createFullAndCarpetBlocks(Blocks.ORANGE_WOOL, Blocks.ORANGE_CARPET);
      this.createFullAndCarpetBlocks(Blocks.MAGENTA_WOOL, Blocks.MAGENTA_CARPET);
      this.createFullAndCarpetBlocks(Blocks.LIGHT_BLUE_WOOL, Blocks.LIGHT_BLUE_CARPET);
      this.createFullAndCarpetBlocks(Blocks.YELLOW_WOOL, Blocks.YELLOW_CARPET);
      this.createFullAndCarpetBlocks(Blocks.LIME_WOOL, Blocks.LIME_CARPET);
      this.createFullAndCarpetBlocks(Blocks.PINK_WOOL, Blocks.PINK_CARPET);
      this.createFullAndCarpetBlocks(Blocks.GRAY_WOOL, Blocks.GRAY_CARPET);
      this.createFullAndCarpetBlocks(Blocks.LIGHT_GRAY_WOOL, Blocks.LIGHT_GRAY_CARPET);
      this.createFullAndCarpetBlocks(Blocks.CYAN_WOOL, Blocks.CYAN_CARPET);
      this.createFullAndCarpetBlocks(Blocks.PURPLE_WOOL, Blocks.PURPLE_CARPET);
      this.createFullAndCarpetBlocks(Blocks.BLUE_WOOL, Blocks.BLUE_CARPET);
      this.createFullAndCarpetBlocks(Blocks.BROWN_WOOL, Blocks.BROWN_CARPET);
      this.createFullAndCarpetBlocks(Blocks.GREEN_WOOL, Blocks.GREEN_CARPET);
      this.createFullAndCarpetBlocks(Blocks.RED_WOOL, Blocks.RED_CARPET);
      this.createFullAndCarpetBlocks(Blocks.BLACK_WOOL, Blocks.BLACK_CARPET);
      this.createTrivialCube(Blocks.MUD);
      this.createTrivialCube(Blocks.PACKED_MUD);
      this.createPlant(Blocks.FERN, Blocks.POTTED_FERN, BlockModelGenerators.PlantType.TINTED);
      this.createItemWithGrassTint(Blocks.FERN);
      this.createPlantWithDefaultItem(Blocks.DANDELION, Blocks.POTTED_DANDELION, BlockModelGenerators.PlantType.NOT_TINTED);
      this.createPlantWithDefaultItem(Blocks.POPPY, Blocks.POTTED_POPPY, BlockModelGenerators.PlantType.NOT_TINTED);
      this.createPlantWithDefaultItem(Blocks.OPEN_EYEBLOSSOM, Blocks.POTTED_OPEN_EYEBLOSSOM, BlockModelGenerators.PlantType.EMISSIVE_NOT_TINTED);
      this.createPlantWithDefaultItem(Blocks.CLOSED_EYEBLOSSOM, Blocks.POTTED_CLOSED_EYEBLOSSOM, BlockModelGenerators.PlantType.NOT_TINTED);
      this.createPlantWithDefaultItem(Blocks.BLUE_ORCHID, Blocks.POTTED_BLUE_ORCHID, BlockModelGenerators.PlantType.NOT_TINTED);
      this.createPlantWithDefaultItem(Blocks.ALLIUM, Blocks.POTTED_ALLIUM, BlockModelGenerators.PlantType.NOT_TINTED);
      this.createPlantWithDefaultItem(Blocks.AZURE_BLUET, Blocks.POTTED_AZURE_BLUET, BlockModelGenerators.PlantType.NOT_TINTED);
      this.createPlantWithDefaultItem(Blocks.RED_TULIP, Blocks.POTTED_RED_TULIP, BlockModelGenerators.PlantType.NOT_TINTED);
      this.createPlantWithDefaultItem(Blocks.ORANGE_TULIP, Blocks.POTTED_ORANGE_TULIP, BlockModelGenerators.PlantType.NOT_TINTED);
      this.createPlantWithDefaultItem(Blocks.WHITE_TULIP, Blocks.POTTED_WHITE_TULIP, BlockModelGenerators.PlantType.NOT_TINTED);
      this.createPlantWithDefaultItem(Blocks.PINK_TULIP, Blocks.POTTED_PINK_TULIP, BlockModelGenerators.PlantType.NOT_TINTED);
      this.createPlantWithDefaultItem(Blocks.OXEYE_DAISY, Blocks.POTTED_OXEYE_DAISY, BlockModelGenerators.PlantType.NOT_TINTED);
      this.createPlantWithDefaultItem(Blocks.CORNFLOWER, Blocks.POTTED_CORNFLOWER, BlockModelGenerators.PlantType.NOT_TINTED);
      this.createPlantWithDefaultItem(Blocks.LILY_OF_THE_VALLEY, Blocks.POTTED_LILY_OF_THE_VALLEY, BlockModelGenerators.PlantType.NOT_TINTED);
      this.createPlantWithDefaultItem(Blocks.WITHER_ROSE, Blocks.POTTED_WITHER_ROSE, BlockModelGenerators.PlantType.NOT_TINTED);
      this.createPlantWithDefaultItem(Blocks.RED_MUSHROOM, Blocks.POTTED_RED_MUSHROOM, BlockModelGenerators.PlantType.NOT_TINTED);
      this.createPlantWithDefaultItem(Blocks.BROWN_MUSHROOM, Blocks.POTTED_BROWN_MUSHROOM, BlockModelGenerators.PlantType.NOT_TINTED);
      this.createPlantWithDefaultItem(Blocks.DEAD_BUSH, Blocks.POTTED_DEAD_BUSH, BlockModelGenerators.PlantType.NOT_TINTED);
      this.createPlantWithDefaultItem(Blocks.TORCHFLOWER, Blocks.POTTED_TORCHFLOWER, BlockModelGenerators.PlantType.NOT_TINTED);
      this.createPointedDripstone();
      this.createMushroomBlock(Blocks.BROWN_MUSHROOM_BLOCK);
      this.createMushroomBlock(Blocks.RED_MUSHROOM_BLOCK);
      this.createMushroomBlock(Blocks.MUSHROOM_STEM);
      this.createCrossBlock(Blocks.SHORT_GRASS, BlockModelGenerators.PlantType.TINTED);
      this.createItemWithGrassTint(Blocks.SHORT_GRASS);
      this.createCrossBlockWithDefaultItem(Blocks.SHORT_DRY_GRASS, BlockModelGenerators.PlantType.NOT_TINTED);
      this.createCrossBlockWithDefaultItem(Blocks.TALL_DRY_GRASS, BlockModelGenerators.PlantType.NOT_TINTED);
      this.createCrossBlock(Blocks.BUSH, BlockModelGenerators.PlantType.TINTED);
      this.createItemWithGrassTint(Blocks.BUSH);
      this.createCrossBlock(Blocks.SUGAR_CANE, BlockModelGenerators.PlantType.TINTED);
      this.registerSimpleFlatItemModel(Items.SUGAR_CANE);
      this.createGrowingPlant(Blocks.KELP, Blocks.KELP_PLANT, BlockModelGenerators.PlantType.NOT_TINTED);
      this.registerSimpleFlatItemModel(Items.KELP);
      this.createCrossBlock(Blocks.HANGING_ROOTS, BlockModelGenerators.PlantType.NOT_TINTED);
      this.createGrowingPlant(Blocks.WEEPING_VINES, Blocks.WEEPING_VINES_PLANT, BlockModelGenerators.PlantType.NOT_TINTED);
      this.createGrowingPlant(Blocks.TWISTING_VINES, Blocks.TWISTING_VINES_PLANT, BlockModelGenerators.PlantType.NOT_TINTED);
      this.registerSimpleFlatItemModel(Blocks.WEEPING_VINES, "_plant");
      this.registerSimpleFlatItemModel(Blocks.TWISTING_VINES, "_plant");
      this.createCrossBlockWithDefaultItem(
         Blocks.BAMBOO_SAPLING, BlockModelGenerators.PlantType.TINTED, TextureMapping.cross(TextureMapping.getBlockTexture(Blocks.BAMBOO, "_stage0"))
      );
      this.createBamboo();
      this.createCrossBlockWithDefaultItem(Blocks.CACTUS_FLOWER, BlockModelGenerators.PlantType.NOT_TINTED);
      this.createCrossBlockWithDefaultItem(Blocks.COBWEB, BlockModelGenerators.PlantType.NOT_TINTED);
      this.createDoublePlantWithDefaultItem(Blocks.LILAC, BlockModelGenerators.PlantType.NOT_TINTED);
      this.createDoublePlantWithDefaultItem(Blocks.ROSE_BUSH, BlockModelGenerators.PlantType.NOT_TINTED);
      this.createDoublePlantWithDefaultItem(Blocks.PEONY, BlockModelGenerators.PlantType.NOT_TINTED);
      this.createTintedDoublePlant(Blocks.TALL_GRASS);
      this.createTintedDoublePlant(Blocks.LARGE_FERN);
      this.createSunflower();
      this.createTallSeagrass();
      this.createSmallDripleaf();
      this.createCoral(
         Blocks.TUBE_CORAL,
         Blocks.DEAD_TUBE_CORAL,
         Blocks.TUBE_CORAL_BLOCK,
         Blocks.DEAD_TUBE_CORAL_BLOCK,
         Blocks.TUBE_CORAL_FAN,
         Blocks.DEAD_TUBE_CORAL_FAN,
         Blocks.TUBE_CORAL_WALL_FAN,
         Blocks.DEAD_TUBE_CORAL_WALL_FAN
      );
      this.createCoral(
         Blocks.BRAIN_CORAL,
         Blocks.DEAD_BRAIN_CORAL,
         Blocks.BRAIN_CORAL_BLOCK,
         Blocks.DEAD_BRAIN_CORAL_BLOCK,
         Blocks.BRAIN_CORAL_FAN,
         Blocks.DEAD_BRAIN_CORAL_FAN,
         Blocks.BRAIN_CORAL_WALL_FAN,
         Blocks.DEAD_BRAIN_CORAL_WALL_FAN
      );
      this.createCoral(
         Blocks.BUBBLE_CORAL,
         Blocks.DEAD_BUBBLE_CORAL,
         Blocks.BUBBLE_CORAL_BLOCK,
         Blocks.DEAD_BUBBLE_CORAL_BLOCK,
         Blocks.BUBBLE_CORAL_FAN,
         Blocks.DEAD_BUBBLE_CORAL_FAN,
         Blocks.BUBBLE_CORAL_WALL_FAN,
         Blocks.DEAD_BUBBLE_CORAL_WALL_FAN
      );
      this.createCoral(
         Blocks.FIRE_CORAL,
         Blocks.DEAD_FIRE_CORAL,
         Blocks.FIRE_CORAL_BLOCK,
         Blocks.DEAD_FIRE_CORAL_BLOCK,
         Blocks.FIRE_CORAL_FAN,
         Blocks.DEAD_FIRE_CORAL_FAN,
         Blocks.FIRE_CORAL_WALL_FAN,
         Blocks.DEAD_FIRE_CORAL_WALL_FAN
      );
      this.createCoral(
         Blocks.HORN_CORAL,
         Blocks.DEAD_HORN_CORAL,
         Blocks.HORN_CORAL_BLOCK,
         Blocks.DEAD_HORN_CORAL_BLOCK,
         Blocks.HORN_CORAL_FAN,
         Blocks.DEAD_HORN_CORAL_FAN,
         Blocks.HORN_CORAL_WALL_FAN,
         Blocks.DEAD_HORN_CORAL_WALL_FAN
      );
      this.createStems(Blocks.MELON_STEM, Blocks.ATTACHED_MELON_STEM);
      this.createStems(Blocks.PUMPKIN_STEM, Blocks.ATTACHED_PUMPKIN_STEM);
      this.woodProvider(Blocks.MANGROVE_LOG).logWithHorizontal(Blocks.MANGROVE_LOG).wood(Blocks.MANGROVE_WOOD);
      this.woodProvider(Blocks.STRIPPED_MANGROVE_LOG).logWithHorizontal(Blocks.STRIPPED_MANGROVE_LOG).wood(Blocks.STRIPPED_MANGROVE_WOOD);
      this.createHangingSign(Blocks.STRIPPED_MANGROVE_LOG, Blocks.MANGROVE_HANGING_SIGN, Blocks.MANGROVE_WALL_HANGING_SIGN);
      this.createTintedLeaves(Blocks.MANGROVE_LEAVES, TexturedModel.LEAVES, -7158200);
      this.woodProvider(Blocks.ACACIA_LOG).logWithHorizontal(Blocks.ACACIA_LOG).wood(Blocks.ACACIA_WOOD);
      this.woodProvider(Blocks.STRIPPED_ACACIA_LOG).logWithHorizontal(Blocks.STRIPPED_ACACIA_LOG).wood(Blocks.STRIPPED_ACACIA_WOOD);
      this.createHangingSign(Blocks.STRIPPED_ACACIA_LOG, Blocks.ACACIA_HANGING_SIGN, Blocks.ACACIA_WALL_HANGING_SIGN);
      this.createPlantWithDefaultItem(Blocks.ACACIA_SAPLING, Blocks.POTTED_ACACIA_SAPLING, BlockModelGenerators.PlantType.NOT_TINTED);
      this.createTintedLeaves(Blocks.ACACIA_LEAVES, TexturedModel.LEAVES, -12012264);
      this.woodProvider(Blocks.CHERRY_LOG).logUVLocked(Blocks.CHERRY_LOG).wood(Blocks.CHERRY_WOOD);
      this.woodProvider(Blocks.STRIPPED_CHERRY_LOG).logUVLocked(Blocks.STRIPPED_CHERRY_LOG).wood(Blocks.STRIPPED_CHERRY_WOOD);
      this.createHangingSign(Blocks.STRIPPED_CHERRY_LOG, Blocks.CHERRY_HANGING_SIGN, Blocks.CHERRY_WALL_HANGING_SIGN);
      this.createPlantWithDefaultItem(Blocks.CHERRY_SAPLING, Blocks.POTTED_CHERRY_SAPLING, BlockModelGenerators.PlantType.NOT_TINTED);
      this.createTrivialBlock(Blocks.CHERRY_LEAVES, TexturedModel.LEAVES);
      this.woodProvider(Blocks.BIRCH_LOG).logWithHorizontal(Blocks.BIRCH_LOG).wood(Blocks.BIRCH_WOOD);
      this.woodProvider(Blocks.STRIPPED_BIRCH_LOG).logWithHorizontal(Blocks.STRIPPED_BIRCH_LOG).wood(Blocks.STRIPPED_BIRCH_WOOD);
      this.createHangingSign(Blocks.STRIPPED_BIRCH_LOG, Blocks.BIRCH_HANGING_SIGN, Blocks.BIRCH_WALL_HANGING_SIGN);
      this.createPlantWithDefaultItem(Blocks.BIRCH_SAPLING, Blocks.POTTED_BIRCH_SAPLING, BlockModelGenerators.PlantType.NOT_TINTED);
      this.createTintedLeaves(Blocks.BIRCH_LEAVES, TexturedModel.LEAVES, -8345771);
      this.woodProvider(Blocks.OAK_LOG).logWithHorizontal(Blocks.OAK_LOG).wood(Blocks.OAK_WOOD);
      this.woodProvider(Blocks.STRIPPED_OAK_LOG).logWithHorizontal(Blocks.STRIPPED_OAK_LOG).wood(Blocks.STRIPPED_OAK_WOOD);
      this.createHangingSign(Blocks.STRIPPED_OAK_LOG, Blocks.OAK_HANGING_SIGN, Blocks.OAK_WALL_HANGING_SIGN);
      this.createPlantWithDefaultItem(Blocks.OAK_SAPLING, Blocks.POTTED_OAK_SAPLING, BlockModelGenerators.PlantType.NOT_TINTED);
      this.createTintedLeaves(Blocks.OAK_LEAVES, TexturedModel.LEAVES, -12012264);
      this.woodProvider(Blocks.SPRUCE_LOG).logWithHorizontal(Blocks.SPRUCE_LOG).wood(Blocks.SPRUCE_WOOD);
      this.woodProvider(Blocks.STRIPPED_SPRUCE_LOG).logWithHorizontal(Blocks.STRIPPED_SPRUCE_LOG).wood(Blocks.STRIPPED_SPRUCE_WOOD);
      this.createHangingSign(Blocks.STRIPPED_SPRUCE_LOG, Blocks.SPRUCE_HANGING_SIGN, Blocks.SPRUCE_WALL_HANGING_SIGN);
      this.createPlantWithDefaultItem(Blocks.SPRUCE_SAPLING, Blocks.POTTED_SPRUCE_SAPLING, BlockModelGenerators.PlantType.NOT_TINTED);
      this.createTintedLeaves(Blocks.SPRUCE_LEAVES, TexturedModel.LEAVES, -10380959);
      this.woodProvider(Blocks.DARK_OAK_LOG).logWithHorizontal(Blocks.DARK_OAK_LOG).wood(Blocks.DARK_OAK_WOOD);
      this.woodProvider(Blocks.STRIPPED_DARK_OAK_LOG).logWithHorizontal(Blocks.STRIPPED_DARK_OAK_LOG).wood(Blocks.STRIPPED_DARK_OAK_WOOD);
      this.createHangingSign(Blocks.STRIPPED_DARK_OAK_LOG, Blocks.DARK_OAK_HANGING_SIGN, Blocks.DARK_OAK_WALL_HANGING_SIGN);
      this.createPlantWithDefaultItem(Blocks.DARK_OAK_SAPLING, Blocks.POTTED_DARK_OAK_SAPLING, BlockModelGenerators.PlantType.NOT_TINTED);
      this.createTintedLeaves(Blocks.DARK_OAK_LEAVES, TexturedModel.LEAVES, -12012264);
      this.woodProvider(Blocks.PALE_OAK_LOG).logWithHorizontal(Blocks.PALE_OAK_LOG).wood(Blocks.PALE_OAK_WOOD);
      this.woodProvider(Blocks.STRIPPED_PALE_OAK_LOG).logWithHorizontal(Blocks.STRIPPED_PALE_OAK_LOG).wood(Blocks.STRIPPED_PALE_OAK_WOOD);
      this.createHangingSign(Blocks.STRIPPED_PALE_OAK_LOG, Blocks.PALE_OAK_HANGING_SIGN, Blocks.PALE_OAK_WALL_HANGING_SIGN);
      this.createPlantWithDefaultItem(Blocks.PALE_OAK_SAPLING, Blocks.POTTED_PALE_OAK_SAPLING, BlockModelGenerators.PlantType.NOT_TINTED);
      this.createTrivialBlock(Blocks.PALE_OAK_LEAVES, TexturedModel.LEAVES);
      this.woodProvider(Blocks.JUNGLE_LOG).logWithHorizontal(Blocks.JUNGLE_LOG).wood(Blocks.JUNGLE_WOOD);
      this.woodProvider(Blocks.STRIPPED_JUNGLE_LOG).logWithHorizontal(Blocks.STRIPPED_JUNGLE_LOG).wood(Blocks.STRIPPED_JUNGLE_WOOD);
      this.createHangingSign(Blocks.STRIPPED_JUNGLE_LOG, Blocks.JUNGLE_HANGING_SIGN, Blocks.JUNGLE_WALL_HANGING_SIGN);
      this.createPlantWithDefaultItem(Blocks.JUNGLE_SAPLING, Blocks.POTTED_JUNGLE_SAPLING, BlockModelGenerators.PlantType.NOT_TINTED);
      this.createTintedLeaves(Blocks.JUNGLE_LEAVES, TexturedModel.LEAVES, -12012264);
      this.woodProvider(Blocks.CRIMSON_STEM).log(Blocks.CRIMSON_STEM).wood(Blocks.CRIMSON_HYPHAE);
      this.woodProvider(Blocks.STRIPPED_CRIMSON_STEM).log(Blocks.STRIPPED_CRIMSON_STEM).wood(Blocks.STRIPPED_CRIMSON_HYPHAE);
      this.createHangingSign(Blocks.STRIPPED_CRIMSON_STEM, Blocks.CRIMSON_HANGING_SIGN, Blocks.CRIMSON_WALL_HANGING_SIGN);
      this.createPlantWithDefaultItem(Blocks.CRIMSON_FUNGUS, Blocks.POTTED_CRIMSON_FUNGUS, BlockModelGenerators.PlantType.NOT_TINTED);
      this.createNetherRoots(Blocks.CRIMSON_ROOTS, Blocks.POTTED_CRIMSON_ROOTS);
      this.woodProvider(Blocks.WARPED_STEM).log(Blocks.WARPED_STEM).wood(Blocks.WARPED_HYPHAE);
      this.woodProvider(Blocks.STRIPPED_WARPED_STEM).log(Blocks.STRIPPED_WARPED_STEM).wood(Blocks.STRIPPED_WARPED_HYPHAE);
      this.createHangingSign(Blocks.STRIPPED_WARPED_STEM, Blocks.WARPED_HANGING_SIGN, Blocks.WARPED_WALL_HANGING_SIGN);
      this.createPlantWithDefaultItem(Blocks.WARPED_FUNGUS, Blocks.POTTED_WARPED_FUNGUS, BlockModelGenerators.PlantType.NOT_TINTED);
      this.createNetherRoots(Blocks.WARPED_ROOTS, Blocks.POTTED_WARPED_ROOTS);
      this.woodProvider(Blocks.BAMBOO_BLOCK).logUVLocked(Blocks.BAMBOO_BLOCK);
      this.woodProvider(Blocks.STRIPPED_BAMBOO_BLOCK).logUVLocked(Blocks.STRIPPED_BAMBOO_BLOCK);
      this.createHangingSign(Blocks.BAMBOO_PLANKS, Blocks.BAMBOO_HANGING_SIGN, Blocks.BAMBOO_WALL_HANGING_SIGN);
      this.createCrossBlock(Blocks.NETHER_SPROUTS, BlockModelGenerators.PlantType.NOT_TINTED);
      this.registerSimpleFlatItemModel(Items.NETHER_SPROUTS);
      this.createDoor(Blocks.IRON_DOOR);
      this.createTrapdoor(Blocks.IRON_TRAPDOOR);
      this.createSmoothStoneSlab();
      this.createPassiveRail(Blocks.RAIL);
      this.createActiveRail(Blocks.POWERED_RAIL);
      this.createActiveRail(Blocks.DETECTOR_RAIL);
      this.createActiveRail(Blocks.ACTIVATOR_RAIL);
      this.createComparator();
      this.createCommandBlock(Blocks.COMMAND_BLOCK);
      this.createCommandBlock(Blocks.REPEATING_COMMAND_BLOCK);
      this.createCommandBlock(Blocks.CHAIN_COMMAND_BLOCK);
      this.createAnvil(Blocks.ANVIL);
      this.createAnvil(Blocks.CHIPPED_ANVIL);
      this.createAnvil(Blocks.DAMAGED_ANVIL);
      this.createBarrel();
      this.createBell();
      this.createFurnace(Blocks.FURNACE, TexturedModel.ORIENTABLE_ONLY_TOP);
      this.createFurnace(Blocks.BLAST_FURNACE, TexturedModel.ORIENTABLE_ONLY_TOP);
      this.createFurnace(Blocks.SMOKER, TexturedModel.ORIENTABLE);
      this.createRedstoneWire();
      this.createRespawnAnchor();
      this.createSculkCatalyst();
      this.copyModel(Blocks.CHISELED_STONE_BRICKS, Blocks.INFESTED_CHISELED_STONE_BRICKS);
      this.copyModel(Blocks.COBBLESTONE, Blocks.INFESTED_COBBLESTONE);
      this.copyModel(Blocks.CRACKED_STONE_BRICKS, Blocks.INFESTED_CRACKED_STONE_BRICKS);
      this.copyModel(Blocks.MOSSY_STONE_BRICKS, Blocks.INFESTED_MOSSY_STONE_BRICKS);
      this.createInfestedStone();
      this.copyModel(Blocks.STONE_BRICKS, Blocks.INFESTED_STONE_BRICKS);
      this.createInfestedDeepslate();
   }

   private void createLightBlock() {
      ItemModel.Unbaked unbaked = ItemModelUtils.plainModel(this.createFlatItemModel(Items.LIGHT));
      Map<Integer, ItemModel.Unbaked> map = new HashMap<>(16);
      PropertyDispatch.C1<MultiVariant, Integer> c1 = PropertyDispatch.initial(BlockStateProperties.LEVEL);

      for (int i = 0; i <= 15; i++) {
         String string = String.format(Locale.ROOT, "_%02d", i);
         Identifier identifier = TextureMapping.getItemTexture(Items.LIGHT, string);
         c1.select(i, plainVariant(ModelTemplates.PARTICLE_ONLY.createWithSuffix(Blocks.LIGHT, string, TextureMapping.particle(identifier), this.modelOutput)));
         ItemModel.Unbaked unbaked2 = ItemModelUtils.plainModel(
            ModelTemplates.FLAT_ITEM.create(ModelLocationUtils.getModelLocation(Items.LIGHT, string), TextureMapping.layer0(identifier), this.modelOutput)
         );
         map.put(i, unbaked2);
      }

      this.itemModelOutput.accept(Items.LIGHT, ItemModelUtils.selectBlockItemProperty(LightBlock.LEVEL, unbaked, map));
      this.blockStateOutput.accept(MultiVariantGenerator.dispatch(Blocks.LIGHT).with(c1));
   }

   private void createCopperChainItem(Item item, Item waxedItem) {
      Identifier identifier = this.createFlatItemModel(item);
      this.registerSimpleItemModel(item, identifier);
      this.registerSimpleItemModel(waxedItem, identifier);
   }

   private void createCandleAndCandleCake(Block candleBlock, Block candleCakeBlock) {
      this.registerSimpleFlatItemModel(candleBlock.asItem());
      TextureMapping textureMapping = TextureMapping.cube(TextureMapping.getBlockTexture(candleBlock));
      TextureMapping textureMapping2 = TextureMapping.cube(TextureMapping.getBlockTexture(candleBlock, "_lit"));
      MultiVariant multiVariant = plainVariant(ModelTemplates.CANDLE.createWithSuffix(candleBlock, "_one_candle", textureMapping, this.modelOutput));
      MultiVariant multiVariant2 = plainVariant(ModelTemplates.TWO_CANDLES.createWithSuffix(candleBlock, "_two_candles", textureMapping, this.modelOutput));
      MultiVariant multiVariant3 = plainVariant(ModelTemplates.THREE_CANDLES.createWithSuffix(candleBlock, "_three_candles", textureMapping, this.modelOutput));
      MultiVariant multiVariant4 = plainVariant(ModelTemplates.FOUR_CANDLES.createWithSuffix(candleBlock, "_four_candles", textureMapping, this.modelOutput));
      MultiVariant multiVariant5 = plainVariant(ModelTemplates.CANDLE.createWithSuffix(candleBlock, "_one_candle_lit", textureMapping2, this.modelOutput));
      MultiVariant multiVariant6 = plainVariant(ModelTemplates.TWO_CANDLES.createWithSuffix(candleBlock, "_two_candles_lit", textureMapping2, this.modelOutput));
      MultiVariant multiVariant7 = plainVariant(
         ModelTemplates.THREE_CANDLES.createWithSuffix(candleBlock, "_three_candles_lit", textureMapping2, this.modelOutput)
      );
      MultiVariant multiVariant8 = plainVariant(
         ModelTemplates.FOUR_CANDLES.createWithSuffix(candleBlock, "_four_candles_lit", textureMapping2, this.modelOutput)
      );
      this.blockStateOutput
         .accept(
            MultiVariantGenerator.dispatch(candleBlock)
               .with(
                  PropertyDispatch.initial(BlockStateProperties.CANDLES, BlockStateProperties.LIT)
                     .select(1, false, multiVariant)
                     .select(2, false, multiVariant2)
                     .select(3, false, multiVariant3)
                     .select(4, false, multiVariant4)
                     .select(1, true, multiVariant5)
                     .select(2, true, multiVariant6)
                     .select(3, true, multiVariant7)
                     .select(4, true, multiVariant8)
               )
         );
      MultiVariant multiVariant9 = plainVariant(
         ModelTemplates.CANDLE_CAKE.create(candleCakeBlock, TextureMapping.candleCake(candleBlock, false), this.modelOutput)
      );
      MultiVariant multiVariant10 = plainVariant(
         ModelTemplates.CANDLE_CAKE.createWithSuffix(candleCakeBlock, "_lit", TextureMapping.candleCake(candleBlock, true), this.modelOutput)
      );
      this.blockStateOutput
         .accept(MultiVariantGenerator.dispatch(candleCakeBlock).with(createBooleanModelDispatch(BlockStateProperties.LIT, multiVariant10, multiVariant9)));
   }

   @Environment(EnvType.CLIENT)
   class BlockFamilyProvider {
      private final TextureMapping mapping;
      private final Map<ModelTemplate, Identifier> models = new HashMap<>();
      @Nullable
      private BlockFamily family;
      @Nullable
      private Variant fullBlock;
      private final Set<Block> skipGeneratingModelsFor = new HashSet<>();

      public BlockFamilyProvider(final TextureMapping mapping) {
         this.mapping = mapping;
      }

      public BlockModelGenerators.BlockFamilyProvider fullBlock(Block block, ModelTemplate modelTemplate) {
         this.fullBlock = BlockModelGenerators.plainModel(modelTemplate.create(block, this.mapping, BlockModelGenerators.this.modelOutput));
         if (BlockModelGenerators.FULL_BLOCK_MODEL_CUSTOM_GENERATORS.containsKey(block)) {
            BlockModelGenerators.this.blockStateOutput
               .accept(
                  BlockModelGenerators.FULL_BLOCK_MODEL_CUSTOM_GENERATORS
                     .get(block)
                     .create(block, this.fullBlock, this.mapping, BlockModelGenerators.this.modelOutput)
               );
         } else {
            BlockModelGenerators.this.blockStateOutput.accept(BlockModelGenerators.createSimpleBlock(block, BlockModelGenerators.variant(this.fullBlock)));
         }

         return this;
      }

      public BlockModelGenerators.BlockFamilyProvider donateModelTo(Block sourceBlock, Block block) {
         Identifier identifier = ModelLocationUtils.getModelLocation(sourceBlock);
         BlockModelGenerators.this.blockStateOutput.accept(BlockModelGenerators.createSimpleBlock(block, BlockModelGenerators.plainVariant(identifier)));
         BlockModelGenerators.this.itemModelOutput.copy(sourceBlock.asItem(), block.asItem());
         this.skipGeneratingModelsFor.add(block);
         return this;
      }

      public BlockModelGenerators.BlockFamilyProvider button(Block buttonBlock) {
         MultiVariant multiVariant = BlockModelGenerators.plainVariant(
            ModelTemplates.BUTTON.create(buttonBlock, this.mapping, BlockModelGenerators.this.modelOutput)
         );
         MultiVariant multiVariant2 = BlockModelGenerators.plainVariant(
            ModelTemplates.BUTTON_PRESSED.create(buttonBlock, this.mapping, BlockModelGenerators.this.modelOutput)
         );
         BlockModelGenerators.this.blockStateOutput.accept(BlockModelGenerators.createButton(buttonBlock, multiVariant, multiVariant2));
         Identifier identifier = ModelTemplates.BUTTON_INVENTORY.create(buttonBlock, this.mapping, BlockModelGenerators.this.modelOutput);
         BlockModelGenerators.this.registerSimpleItemModel(buttonBlock, identifier);
         return this;
      }

      public BlockModelGenerators.BlockFamilyProvider wall(Block wallBlock) {
         MultiVariant multiVariant = BlockModelGenerators.plainVariant(
            ModelTemplates.WALL_POST.create(wallBlock, this.mapping, BlockModelGenerators.this.modelOutput)
         );
         MultiVariant multiVariant2 = BlockModelGenerators.plainVariant(
            ModelTemplates.WALL_LOW_SIDE.create(wallBlock, this.mapping, BlockModelGenerators.this.modelOutput)
         );
         MultiVariant multiVariant3 = BlockModelGenerators.plainVariant(
            ModelTemplates.WALL_TALL_SIDE.create(wallBlock, this.mapping, BlockModelGenerators.this.modelOutput)
         );
         BlockModelGenerators.this.blockStateOutput.accept(BlockModelGenerators.createWall(wallBlock, multiVariant, multiVariant2, multiVariant3));
         Identifier identifier = ModelTemplates.WALL_INVENTORY.create(wallBlock, this.mapping, BlockModelGenerators.this.modelOutput);
         BlockModelGenerators.this.registerSimpleItemModel(wallBlock, identifier);
         return this;
      }

      public BlockModelGenerators.BlockFamilyProvider customFence(Block fenceBlock) {
         TextureMapping textureMapping = TextureMapping.customParticle(fenceBlock);
         MultiVariant multiVariant = BlockModelGenerators.plainVariant(
            ModelTemplates.CUSTOM_FENCE_POST.create(fenceBlock, textureMapping, BlockModelGenerators.this.modelOutput)
         );
         MultiVariant multiVariant2 = BlockModelGenerators.plainVariant(
            ModelTemplates.CUSTOM_FENCE_SIDE_NORTH.create(fenceBlock, textureMapping, BlockModelGenerators.this.modelOutput)
         );
         MultiVariant multiVariant3 = BlockModelGenerators.plainVariant(
            ModelTemplates.CUSTOM_FENCE_SIDE_EAST.create(fenceBlock, textureMapping, BlockModelGenerators.this.modelOutput)
         );
         MultiVariant multiVariant4 = BlockModelGenerators.plainVariant(
            ModelTemplates.CUSTOM_FENCE_SIDE_SOUTH.create(fenceBlock, textureMapping, BlockModelGenerators.this.modelOutput)
         );
         MultiVariant multiVariant5 = BlockModelGenerators.plainVariant(
            ModelTemplates.CUSTOM_FENCE_SIDE_WEST.create(fenceBlock, textureMapping, BlockModelGenerators.this.modelOutput)
         );
         BlockModelGenerators.this.blockStateOutput
            .accept(BlockModelGenerators.createCustomFence(fenceBlock, multiVariant, multiVariant2, multiVariant3, multiVariant4, multiVariant5));
         Identifier identifier = ModelTemplates.CUSTOM_FENCE_INVENTORY.create(fenceBlock, textureMapping, BlockModelGenerators.this.modelOutput);
         BlockModelGenerators.this.registerSimpleItemModel(fenceBlock, identifier);
         return this;
      }

      public BlockModelGenerators.BlockFamilyProvider fence(Block fenceBlock) {
         MultiVariant multiVariant = BlockModelGenerators.plainVariant(
            ModelTemplates.FENCE_POST.create(fenceBlock, this.mapping, BlockModelGenerators.this.modelOutput)
         );
         MultiVariant multiVariant2 = BlockModelGenerators.plainVariant(
            ModelTemplates.FENCE_SIDE.create(fenceBlock, this.mapping, BlockModelGenerators.this.modelOutput)
         );
         BlockModelGenerators.this.blockStateOutput.accept(BlockModelGenerators.createFence(fenceBlock, multiVariant, multiVariant2));
         Identifier identifier = ModelTemplates.FENCE_INVENTORY.create(fenceBlock, this.mapping, BlockModelGenerators.this.modelOutput);
         BlockModelGenerators.this.registerSimpleItemModel(fenceBlock, identifier);
         return this;
      }

      public BlockModelGenerators.BlockFamilyProvider customFenceGate(Block customFenceGateBlock) {
         TextureMapping textureMapping = TextureMapping.customParticle(customFenceGateBlock);
         MultiVariant multiVariant = BlockModelGenerators.plainVariant(
            ModelTemplates.CUSTOM_FENCE_GATE_OPEN.create(customFenceGateBlock, textureMapping, BlockModelGenerators.this.modelOutput)
         );
         MultiVariant multiVariant2 = BlockModelGenerators.plainVariant(
            ModelTemplates.CUSTOM_FENCE_GATE_CLOSED.create(customFenceGateBlock, textureMapping, BlockModelGenerators.this.modelOutput)
         );
         MultiVariant multiVariant3 = BlockModelGenerators.plainVariant(
            ModelTemplates.CUSTOM_FENCE_GATE_WALL_OPEN.create(customFenceGateBlock, textureMapping, BlockModelGenerators.this.modelOutput)
         );
         MultiVariant multiVariant4 = BlockModelGenerators.plainVariant(
            ModelTemplates.CUSTOM_FENCE_GATE_WALL_CLOSED.create(customFenceGateBlock, textureMapping, BlockModelGenerators.this.modelOutput)
         );
         BlockModelGenerators.this.blockStateOutput
            .accept(BlockModelGenerators.createFenceGate(customFenceGateBlock, multiVariant, multiVariant2, multiVariant3, multiVariant4, false));
         return this;
      }

      public BlockModelGenerators.BlockFamilyProvider fenceGate(Block fenceGateBlock) {
         MultiVariant multiVariant = BlockModelGenerators.plainVariant(
            ModelTemplates.FENCE_GATE_OPEN.create(fenceGateBlock, this.mapping, BlockModelGenerators.this.modelOutput)
         );
         MultiVariant multiVariant2 = BlockModelGenerators.plainVariant(
            ModelTemplates.FENCE_GATE_CLOSED.create(fenceGateBlock, this.mapping, BlockModelGenerators.this.modelOutput)
         );
         MultiVariant multiVariant3 = BlockModelGenerators.plainVariant(
            ModelTemplates.FENCE_GATE_WALL_OPEN.create(fenceGateBlock, this.mapping, BlockModelGenerators.this.modelOutput)
         );
         MultiVariant multiVariant4 = BlockModelGenerators.plainVariant(
            ModelTemplates.FENCE_GATE_WALL_CLOSED.create(fenceGateBlock, this.mapping, BlockModelGenerators.this.modelOutput)
         );
         BlockModelGenerators.this.blockStateOutput
            .accept(BlockModelGenerators.createFenceGate(fenceGateBlock, multiVariant, multiVariant2, multiVariant3, multiVariant4, true));
         return this;
      }

      public BlockModelGenerators.BlockFamilyProvider pressurePlate(Block pressurePlateBlock) {
         MultiVariant multiVariant = BlockModelGenerators.plainVariant(
            ModelTemplates.PRESSURE_PLATE_UP.create(pressurePlateBlock, this.mapping, BlockModelGenerators.this.modelOutput)
         );
         MultiVariant multiVariant2 = BlockModelGenerators.plainVariant(
            ModelTemplates.PRESSURE_PLATE_DOWN.create(pressurePlateBlock, this.mapping, BlockModelGenerators.this.modelOutput)
         );
         BlockModelGenerators.this.blockStateOutput.accept(BlockModelGenerators.createPressurePlate(pressurePlateBlock, multiVariant, multiVariant2));
         return this;
      }

      public BlockModelGenerators.BlockFamilyProvider sign(Block signBlock) {
         if (this.family == null) {
            throw new IllegalStateException("Family not defined");
         } else {
            Block block = this.family.getVariants().get(BlockFamily.Variant.WALL_SIGN);
            MultiVariant multiVariant = BlockModelGenerators.plainVariant(
               ModelTemplates.PARTICLE_ONLY.create(signBlock, this.mapping, BlockModelGenerators.this.modelOutput)
            );
            BlockModelGenerators.this.blockStateOutput.accept(BlockModelGenerators.createSimpleBlock(signBlock, multiVariant));
            BlockModelGenerators.this.blockStateOutput.accept(BlockModelGenerators.createSimpleBlock(block, multiVariant));
            BlockModelGenerators.this.registerSimpleFlatItemModel(signBlock.asItem());
            return this;
         }
      }

      public BlockModelGenerators.BlockFamilyProvider slab(Block slabBlock) {
         if (this.fullBlock == null) {
            throw new IllegalStateException("Full block not generated yet");
         } else {
            Identifier identifier = this.getOrCreateModel(ModelTemplates.SLAB_BOTTOM, slabBlock);
            MultiVariant multiVariant = BlockModelGenerators.plainVariant(this.getOrCreateModel(ModelTemplates.SLAB_TOP, slabBlock));
            BlockModelGenerators.this.blockStateOutput
               .accept(
                  BlockModelGenerators.createSlab(
                     slabBlock, BlockModelGenerators.plainVariant(identifier), multiVariant, BlockModelGenerators.variant(this.fullBlock)
                  )
               );
            BlockModelGenerators.this.registerSimpleItemModel(slabBlock, identifier);
            return this;
         }
      }

      public BlockModelGenerators.BlockFamilyProvider stairs(Block stairsBlock) {
         MultiVariant multiVariant = BlockModelGenerators.plainVariant(this.getOrCreateModel(ModelTemplates.STAIRS_INNER, stairsBlock));
         Identifier identifier = this.getOrCreateModel(ModelTemplates.STAIRS_STRAIGHT, stairsBlock);
         MultiVariant multiVariant2 = BlockModelGenerators.plainVariant(this.getOrCreateModel(ModelTemplates.STAIRS_OUTER, stairsBlock));
         BlockModelGenerators.this.blockStateOutput
            .accept(BlockModelGenerators.createStairs(stairsBlock, multiVariant, BlockModelGenerators.plainVariant(identifier), multiVariant2));
         BlockModelGenerators.this.registerSimpleItemModel(stairsBlock, identifier);
         return this;
      }

      private BlockModelGenerators.BlockFamilyProvider fullBlockVariant(Block block) {
         TexturedModel texturedModel = BlockModelGenerators.TEXTURED_MODELS.getOrDefault(block, TexturedModel.CUBE.get(block));
         MultiVariant multiVariant = BlockModelGenerators.plainVariant(texturedModel.create(block, BlockModelGenerators.this.modelOutput));
         BlockModelGenerators.this.blockStateOutput.accept(BlockModelGenerators.createSimpleBlock(block, multiVariant));
         return this;
      }

      private BlockModelGenerators.BlockFamilyProvider door(Block doorBlock) {
         BlockModelGenerators.this.createDoor(doorBlock);
         return this;
      }

      private void trapdoor(Block trapdoorBlock) {
         if (BlockModelGenerators.NON_ORIENTABLE_TRAPDOOR.contains(trapdoorBlock)) {
            BlockModelGenerators.this.createTrapdoor(trapdoorBlock);
         } else {
            BlockModelGenerators.this.createOrientableTrapdoor(trapdoorBlock);
         }
      }

      private Identifier getOrCreateModel(ModelTemplate modelTemplate, Block block) {
         return this.models.computeIfAbsent(modelTemplate, modelTemplatex -> modelTemplatex.create(block, this.mapping, BlockModelGenerators.this.modelOutput));
      }

      public BlockModelGenerators.BlockFamilyProvider generateFor(BlockFamily family) {
         this.family = family;
         family.getVariants().forEach((variant, block) -> {
            if (!this.skipGeneratingModelsFor.contains(block)) {
               BiConsumer<BlockModelGenerators.BlockFamilyProvider, Block> biConsumer = BlockModelGenerators.SHAPE_CONSUMERS.get(variant);
               if (biConsumer != null) {
                  biConsumer.accept(this, block);
               }
            }
         });
         return this;
      }
   }

   @FunctionalInterface
   @Environment(EnvType.CLIENT)
   interface BlockStateGeneratorSupplier {
      BlockModelDefinitionGenerator create(Block block, Variant variant, TextureMapping textureMapping, BiConsumer<Identifier, ModelInstance> biConsumer);
   }

   @Environment(EnvType.CLIENT)
   record BookSlotModelCacheKey(ModelTemplate template, String modelSuffix) {
   }

   @Environment(EnvType.CLIENT)
   static enum PlantType {
      TINTED(ModelTemplates.TINTED_CROSS, ModelTemplates.TINTED_FLOWER_POT_CROSS, false),
      NOT_TINTED(ModelTemplates.CROSS, ModelTemplates.FLOWER_POT_CROSS, false),
      EMISSIVE_NOT_TINTED(ModelTemplates.CROSS_EMISSIVE, ModelTemplates.FLOWER_POT_CROSS_EMISSIVE, true);

      private final ModelTemplate blockTemplate;
      private final ModelTemplate flowerPotTemplate;
      private final boolean isEmissive;

      private PlantType(final ModelTemplate blockTemplate, final ModelTemplate flowerPotTemplate, final boolean isEmissive) {
         this.blockTemplate = blockTemplate;
         this.flowerPotTemplate = flowerPotTemplate;
         this.isEmissive = isEmissive;
      }

      public ModelTemplate getCross() {
         return this.blockTemplate;
      }

      public ModelTemplate getCrossPot() {
         return this.flowerPotTemplate;
      }

      public Identifier createItemModel(BlockModelGenerators generator, Block block) {
         Item item = block.asItem();
         return this.isEmissive
            ? generator.createFlatItemModelWithBlockTextureAndOverlay(item, block, "_emissive")
            : generator.createFlatItemModelWithBlockTexture(item, block);
      }

      public TextureMapping getTextureMapping(Block block) {
         return this.isEmissive ? TextureMapping.crossEmissive(block) : TextureMapping.cross(block);
      }

      public TextureMapping getPlantTextureMapping(Block block) {
         return this.isEmissive ? TextureMapping.plantEmissive(block) : TextureMapping.plant(block);
      }
   }

   @Environment(EnvType.CLIENT)
   class WoodProvider {
      private final TextureMapping logMapping;

      public WoodProvider(final TextureMapping logMapping) {
         this.logMapping = logMapping;
      }

      public BlockModelGenerators.WoodProvider wood(Block woodBlock) {
         TextureMapping textureMapping = this.logMapping.copyAndUpdate(TextureSlot.END, this.logMapping.get(TextureSlot.SIDE));
         Identifier identifier = ModelTemplates.CUBE_COLUMN.create(woodBlock, textureMapping, BlockModelGenerators.this.modelOutput);
         BlockModelGenerators.this.blockStateOutput
            .accept(BlockModelGenerators.createAxisAlignedPillarBlock(woodBlock, BlockModelGenerators.plainVariant(identifier)));
         BlockModelGenerators.this.registerSimpleItemModel(woodBlock, identifier);
         return this;
      }

      public BlockModelGenerators.WoodProvider log(Block logBlock) {
         Identifier identifier = ModelTemplates.CUBE_COLUMN.create(logBlock, this.logMapping, BlockModelGenerators.this.modelOutput);
         BlockModelGenerators.this.blockStateOutput
            .accept(BlockModelGenerators.createAxisAlignedPillarBlock(logBlock, BlockModelGenerators.plainVariant(identifier)));
         BlockModelGenerators.this.registerSimpleItemModel(logBlock, identifier);
         return this;
      }

      public BlockModelGenerators.WoodProvider logWithHorizontal(Block logBlock) {
         Identifier identifier = ModelTemplates.CUBE_COLUMN.create(logBlock, this.logMapping, BlockModelGenerators.this.modelOutput);
         MultiVariant multiVariant = BlockModelGenerators.plainVariant(
            ModelTemplates.CUBE_COLUMN_HORIZONTAL.create(logBlock, this.logMapping, BlockModelGenerators.this.modelOutput)
         );
         BlockModelGenerators.this.blockStateOutput
            .accept(BlockModelGenerators.createRotatedPillarWithHorizontalVariant(logBlock, BlockModelGenerators.plainVariant(identifier), multiVariant));
         BlockModelGenerators.this.registerSimpleItemModel(logBlock, identifier);
         return this;
      }

      public BlockModelGenerators.WoodProvider logUVLocked(Block logBlock) {
         BlockModelGenerators.this.blockStateOutput
            .accept(BlockModelGenerators.createPillarBlockUVLocked(logBlock, this.logMapping, BlockModelGenerators.this.modelOutput));
         BlockModelGenerators.this.registerSimpleItemModel(
            logBlock, ModelTemplates.CUBE_COLUMN.create(logBlock, this.logMapping, BlockModelGenerators.this.modelOutput)
         );
         return this;
      }
   }
}
