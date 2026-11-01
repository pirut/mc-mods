package net.minecraft.data.models;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.gson.JsonElement;
import com.mojang.datafixers.util.Pair;
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
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.Direction;
import net.minecraft.core.FrontAndTop;
import net.minecraft.data.BlockFamilies;
import net.minecraft.data.BlockFamily;
import net.minecraft.data.models.blockstates.BlockStateGenerator;
import net.minecraft.data.models.blockstates.Condition;
import net.minecraft.data.models.blockstates.MultiPartGenerator;
import net.minecraft.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.data.models.blockstates.PropertyDispatch;
import net.minecraft.data.models.blockstates.Variant;
import net.minecraft.data.models.blockstates.VariantProperties;
import net.minecraft.data.models.model.DelegatedModel;
import net.minecraft.data.models.model.ModelLocationUtils;
import net.minecraft.data.models.model.ModelTemplate;
import net.minecraft.data.models.model.ModelTemplates;
import net.minecraft.data.models.model.TextureMapping;
import net.minecraft.data.models.model.TextureSlot;
import net.minecraft.data.models.model.TexturedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CrafterBlock;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.MangrovePropaguleBlock;
import net.minecraft.world.level.block.PitcherCropBlock;
import net.minecraft.world.level.block.SnifferEggBlock;
import net.minecraft.world.level.block.VaultBlock;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BambooLeaves;
import net.minecraft.world.level.block.state.properties.BellAttachType;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.ComparatorMode;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.DripstoneThickness;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.PistonType;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.level.block.state.properties.RedstoneSide;
import net.minecraft.world.level.block.state.properties.SculkSensorPhase;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.block.state.properties.StairsShape;
import net.minecraft.world.level.block.state.properties.Tilt;
import net.minecraft.world.level.block.state.properties.WallSide;

public class BlockModelGenerators {
   final Consumer<BlockStateGenerator> blockStateOutput;
   final BiConsumer<ResourceLocation, Supplier<JsonElement>> modelOutput;
   private final Consumer<Item> skippedAutoModelsOutput;
   final List<Block> nonOrientableTrapdoor = ImmutableList.of(Blocks.OAK_TRAPDOOR, Blocks.DARK_OAK_TRAPDOOR, Blocks.IRON_TRAPDOOR);
   final Map<Block, BlockModelGenerators.BlockStateGeneratorSupplier> fullBlockModelCustomGenerators = ImmutableMap.builder()
      .put(Blocks.STONE, BlockModelGenerators::createMirroredCubeGenerator)
      .put(Blocks.DEEPSLATE, BlockModelGenerators::createMirroredColumnGenerator)
      .put(Blocks.MUD_BRICKS, BlockModelGenerators::createNorthWestMirroredCubeGenerator)
      .build();
   final Map<Block, TexturedModel> texturedModels = ImmutableMap.builder()
      .put(Blocks.SANDSTONE, TexturedModel.TOP_BOTTOM_WITH_WALL.get(Blocks.SANDSTONE))
      .put(Blocks.RED_SANDSTONE, TexturedModel.TOP_BOTTOM_WITH_WALL.get(Blocks.RED_SANDSTONE))
      .put(Blocks.SMOOTH_SANDSTONE, TexturedModel.createAllSame(TextureMapping.getBlockTexture(Blocks.SANDSTONE, "_top")))
      .put(Blocks.SMOOTH_RED_SANDSTONE, TexturedModel.createAllSame(TextureMapping.getBlockTexture(Blocks.RED_SANDSTONE, "_top")))
      .put(
         Blocks.CUT_SANDSTONE,
         TexturedModel.COLUMN
            .get(Blocks.SANDSTONE)
            .updateTextures(p_176223_ -> p_176223_.put(TextureSlot.SIDE, TextureMapping.getBlockTexture(Blocks.CUT_SANDSTONE)))
      )
      .put(
         Blocks.CUT_RED_SANDSTONE,
         TexturedModel.COLUMN
            .get(Blocks.RED_SANDSTONE)
            .updateTextures(p_176211_ -> p_176211_.put(TextureSlot.SIDE, TextureMapping.getBlockTexture(Blocks.CUT_RED_SANDSTONE)))
      )
      .put(Blocks.QUARTZ_BLOCK, TexturedModel.COLUMN.get(Blocks.QUARTZ_BLOCK))
      .put(Blocks.SMOOTH_QUARTZ, TexturedModel.createAllSame(TextureMapping.getBlockTexture(Blocks.QUARTZ_BLOCK, "_bottom")))
      .put(Blocks.BLACKSTONE, TexturedModel.COLUMN_WITH_WALL.get(Blocks.BLACKSTONE))
      .put(Blocks.DEEPSLATE, TexturedModel.COLUMN_WITH_WALL.get(Blocks.DEEPSLATE))
      .put(
         Blocks.CHISELED_QUARTZ_BLOCK,
         TexturedModel.COLUMN
            .get(Blocks.CHISELED_QUARTZ_BLOCK)
            .updateTextures(p_176202_ -> p_176202_.put(TextureSlot.SIDE, TextureMapping.getBlockTexture(Blocks.CHISELED_QUARTZ_BLOCK)))
      )
      .put(Blocks.CHISELED_SANDSTONE, TexturedModel.COLUMN.get(Blocks.CHISELED_SANDSTONE).updateTextures(p_176190_ -> {
         p_176190_.put(TextureSlot.END, TextureMapping.getBlockTexture(Blocks.SANDSTONE, "_top"));
         p_176190_.put(TextureSlot.SIDE, TextureMapping.getBlockTexture(Blocks.CHISELED_SANDSTONE));
      }))
      .put(Blocks.CHISELED_RED_SANDSTONE, TexturedModel.COLUMN.get(Blocks.CHISELED_RED_SANDSTONE).updateTextures(p_176145_ -> {
         p_176145_.put(TextureSlot.END, TextureMapping.getBlockTexture(Blocks.RED_SANDSTONE, "_top"));
         p_176145_.put(TextureSlot.SIDE, TextureMapping.getBlockTexture(Blocks.CHISELED_RED_SANDSTONE));
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
   public static final List<Pair<BooleanProperty, Function<ResourceLocation, Variant>>> MULTIFACE_GENERATOR = List.of(
      Pair.of(BlockStateProperties.NORTH, (Function<ResourceLocation, Variant>)p_176234_ -> Variant.variant().with(VariantProperties.MODEL, p_176234_)),
      Pair.of(
         BlockStateProperties.EAST,
         (Function<ResourceLocation, Variant>)p_176229_ -> Variant.variant()
            .with(VariantProperties.MODEL, p_176229_)
            .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)
            .with(VariantProperties.UV_LOCK, true)
      ),
      Pair.of(
         BlockStateProperties.SOUTH,
         (Function<ResourceLocation, Variant>)p_176225_ -> Variant.variant()
            .with(VariantProperties.MODEL, p_176225_)
            .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180)
            .with(VariantProperties.UV_LOCK, true)
      ),
      Pair.of(
         BlockStateProperties.WEST,
         (Function<ResourceLocation, Variant>)p_176213_ -> Variant.variant()
            .with(VariantProperties.MODEL, p_176213_)
            .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)
            .with(VariantProperties.UV_LOCK, true)
      ),
      Pair.of(
         BlockStateProperties.UP,
         (Function<ResourceLocation, Variant>)p_176204_ -> Variant.variant()
            .with(VariantProperties.MODEL, p_176204_)
            .with(VariantProperties.X_ROT, VariantProperties.Rotation.R270)
            .with(VariantProperties.UV_LOCK, true)
      ),
      Pair.of(
         BlockStateProperties.DOWN,
         (Function<ResourceLocation, Variant>)p_176195_ -> Variant.variant()
            .with(VariantProperties.MODEL, p_176195_)
            .with(VariantProperties.X_ROT, VariantProperties.Rotation.R90)
            .with(VariantProperties.UV_LOCK, true)
      )
   );
   private static final Map<BlockModelGenerators.BookSlotModelCacheKey, ResourceLocation> CHISELED_BOOKSHELF_SLOT_MODEL_CACHE = new HashMap<>();

   private static BlockStateGenerator createMirroredCubeGenerator(
      Block p_176110_, ResourceLocation p_176111_, TextureMapping p_176112_, BiConsumer<ResourceLocation, Supplier<JsonElement>> p_176113_
   ) {
      ResourceLocation $$4 = ModelTemplates.CUBE_MIRRORED_ALL.create(p_176110_, p_176112_, p_176113_);
      return createRotatedVariant(p_176110_, p_176111_, $$4);
   }

   private static BlockStateGenerator createNorthWestMirroredCubeGenerator(
      Block p_236317_, ResourceLocation p_236318_, TextureMapping p_236319_, BiConsumer<ResourceLocation, Supplier<JsonElement>> p_236320_
   ) {
      ResourceLocation $$4 = ModelTemplates.CUBE_NORTH_WEST_MIRRORED_ALL.create(p_236317_, p_236319_, p_236320_);
      return createSimpleBlock(p_236317_, $$4);
   }

   private static BlockStateGenerator createMirroredColumnGenerator(
      Block p_176180_, ResourceLocation p_176181_, TextureMapping p_176182_, BiConsumer<ResourceLocation, Supplier<JsonElement>> p_176183_
   ) {
      ResourceLocation $$4 = ModelTemplates.CUBE_COLUMN_MIRRORED.create(p_176180_, p_176182_, p_176183_);
      return createRotatedVariant(p_176180_, p_176181_, $$4).with(createRotatedPillar());
   }

   public BlockModelGenerators(Consumer<BlockStateGenerator> p_124481_, BiConsumer<ResourceLocation, Supplier<JsonElement>> p_124482_, Consumer<Item> p_124483_) {
      this.blockStateOutput = p_124481_;
      this.modelOutput = p_124482_;
      this.skippedAutoModelsOutput = p_124483_;
   }

   void skipAutoItemBlock(Block p_124525_) {
      this.skippedAutoModelsOutput.accept(p_124525_.asItem());
   }

   void delegateItemModel(Block p_124798_, ResourceLocation p_124799_) {
      this.modelOutput.accept(ModelLocationUtils.getModelLocation(p_124798_.asItem()), new DelegatedModel(p_124799_));
   }

   private void delegateItemModel(Item p_124520_, ResourceLocation p_124521_) {
      this.modelOutput.accept(ModelLocationUtils.getModelLocation(p_124520_), new DelegatedModel(p_124521_));
   }

   void createSimpleFlatItemModel(Item p_124518_) {
      ModelTemplates.FLAT_ITEM.create(ModelLocationUtils.getModelLocation(p_124518_), TextureMapping.layer0(p_124518_), this.modelOutput);
   }

   private void createSimpleFlatItemModel(Block p_124729_) {
      Item $$1 = p_124729_.asItem();
      if ($$1 != Items.AIR) {
         ModelTemplates.FLAT_ITEM.create(ModelLocationUtils.getModelLocation($$1), TextureMapping.layer0(p_124729_), this.modelOutput);
      }
   }

   private void createSimpleFlatItemModel(Block p_124576_, String p_124577_) {
      Item $$2 = p_124576_.asItem();
      ModelTemplates.FLAT_ITEM
         .create(ModelLocationUtils.getModelLocation($$2), TextureMapping.layer0(TextureMapping.getBlockTexture(p_124576_, p_124577_)), this.modelOutput);
   }

   private static PropertyDispatch createHorizontalFacingDispatch() {
      return PropertyDispatch.property(BlockStateProperties.HORIZONTAL_FACING)
         .select(Direction.EAST, Variant.variant().with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90))
         .select(Direction.SOUTH, Variant.variant().with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180))
         .select(Direction.WEST, Variant.variant().with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270))
         .select(Direction.NORTH, Variant.variant());
   }

   private static PropertyDispatch createHorizontalFacingDispatchAlt() {
      return PropertyDispatch.property(BlockStateProperties.HORIZONTAL_FACING)
         .select(Direction.SOUTH, Variant.variant())
         .select(Direction.WEST, Variant.variant().with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90))
         .select(Direction.NORTH, Variant.variant().with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180))
         .select(Direction.EAST, Variant.variant().with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270));
   }

   private static PropertyDispatch createTorchHorizontalDispatch() {
      return PropertyDispatch.property(BlockStateProperties.HORIZONTAL_FACING)
         .select(Direction.EAST, Variant.variant())
         .select(Direction.SOUTH, Variant.variant().with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90))
         .select(Direction.WEST, Variant.variant().with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180))
         .select(Direction.NORTH, Variant.variant().with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270));
   }

   private static PropertyDispatch createFacingDispatch() {
      return PropertyDispatch.property(BlockStateProperties.FACING)
         .select(Direction.DOWN, Variant.variant().with(VariantProperties.X_ROT, VariantProperties.Rotation.R90))
         .select(Direction.UP, Variant.variant().with(VariantProperties.X_ROT, VariantProperties.Rotation.R270))
         .select(Direction.NORTH, Variant.variant())
         .select(Direction.SOUTH, Variant.variant().with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180))
         .select(Direction.WEST, Variant.variant().with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270))
         .select(Direction.EAST, Variant.variant().with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90));
   }

   private static MultiVariantGenerator createRotatedVariant(Block p_124832_, ResourceLocation p_124833_) {
      return MultiVariantGenerator.multiVariant(p_124832_, createRotatedVariants(p_124833_));
   }

   private static Variant[] createRotatedVariants(ResourceLocation p_124689_) {
      return new Variant[]{
         Variant.variant().with(VariantProperties.MODEL, p_124689_),
         Variant.variant().with(VariantProperties.MODEL, p_124689_).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90),
         Variant.variant().with(VariantProperties.MODEL, p_124689_).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180),
         Variant.variant().with(VariantProperties.MODEL, p_124689_).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)
      };
   }

   private static MultiVariantGenerator createRotatedVariant(Block p_124863_, ResourceLocation p_124864_, ResourceLocation p_124865_) {
      return MultiVariantGenerator.multiVariant(
         p_124863_,
         Variant.variant().with(VariantProperties.MODEL, p_124864_),
         Variant.variant().with(VariantProperties.MODEL, p_124865_),
         Variant.variant().with(VariantProperties.MODEL, p_124864_).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180),
         Variant.variant().with(VariantProperties.MODEL, p_124865_).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180)
      );
   }

   private static PropertyDispatch createBooleanModelDispatch(BooleanProperty p_124623_, ResourceLocation p_124624_, ResourceLocation p_124625_) {
      return PropertyDispatch.property(p_124623_)
         .select(true, Variant.variant().with(VariantProperties.MODEL, p_124624_))
         .select(false, Variant.variant().with(VariantProperties.MODEL, p_124625_));
   }

   private void createRotatedMirroredVariantBlock(Block p_124787_) {
      ResourceLocation $$1 = TexturedModel.CUBE.create(p_124787_, this.modelOutput);
      ResourceLocation $$2 = TexturedModel.CUBE_MIRRORED.create(p_124787_, this.modelOutput);
      this.blockStateOutput.accept(createRotatedVariant(p_124787_, $$1, $$2));
   }

   private void createRotatedVariantBlock(Block p_124824_) {
      ResourceLocation $$1 = TexturedModel.CUBE.create(p_124824_, this.modelOutput);
      this.blockStateOutput.accept(createRotatedVariant(p_124824_, $$1));
   }

   private void createBrushableBlock(Block p_277651_) {
      this.blockStateOutput
         .accept(
            MultiVariantGenerator.multiVariant(p_277651_)
               .with(
                  PropertyDispatch.property(BlockStateProperties.DUSTED)
                     .generate(
                        p_277253_ -> {
                           String $$2 = "_" + p_277253_;
                           ResourceLocation $$3 = TextureMapping.getBlockTexture(p_277651_, $$2);
                           return Variant.variant()
                              .with(
                                 VariantProperties.MODEL,
                                 ModelTemplates.CUBE_ALL.createWithSuffix(p_277651_, $$2, new TextureMapping().put(TextureSlot.ALL, $$3), this.modelOutput)
                              );
                        }
                     )
               )
         );
      this.delegateItemModel(p_277651_, TextureMapping.getBlockTexture(p_277651_, "_0"));
   }

   static BlockStateGenerator createButton(Block p_124885_, ResourceLocation p_124886_, ResourceLocation p_124887_) {
      return MultiVariantGenerator.multiVariant(p_124885_)
         .with(
            PropertyDispatch.property(BlockStateProperties.POWERED)
               .select(false, Variant.variant().with(VariantProperties.MODEL, p_124886_))
               .select(true, Variant.variant().with(VariantProperties.MODEL, p_124887_))
         )
         .with(
            PropertyDispatch.properties(BlockStateProperties.ATTACH_FACE, BlockStateProperties.HORIZONTAL_FACING)
               .select(AttachFace.FLOOR, Direction.EAST, Variant.variant().with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90))
               .select(AttachFace.FLOOR, Direction.WEST, Variant.variant().with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270))
               .select(AttachFace.FLOOR, Direction.SOUTH, Variant.variant().with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180))
               .select(AttachFace.FLOOR, Direction.NORTH, Variant.variant())
               .select(
                  AttachFace.WALL,
                  Direction.EAST,
                  Variant.variant()
                     .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)
                     .with(VariantProperties.X_ROT, VariantProperties.Rotation.R90)
                     .with(VariantProperties.UV_LOCK, true)
               )
               .select(
                  AttachFace.WALL,
                  Direction.WEST,
                  Variant.variant()
                     .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)
                     .with(VariantProperties.X_ROT, VariantProperties.Rotation.R90)
                     .with(VariantProperties.UV_LOCK, true)
               )
               .select(
                  AttachFace.WALL,
                  Direction.SOUTH,
                  Variant.variant()
                     .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180)
                     .with(VariantProperties.X_ROT, VariantProperties.Rotation.R90)
                     .with(VariantProperties.UV_LOCK, true)
               )
               .select(
                  AttachFace.WALL,
                  Direction.NORTH,
                  Variant.variant().with(VariantProperties.X_ROT, VariantProperties.Rotation.R90).with(VariantProperties.UV_LOCK, true)
               )
               .select(
                  AttachFace.CEILING,
                  Direction.EAST,
                  Variant.variant()
                     .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)
                     .with(VariantProperties.X_ROT, VariantProperties.Rotation.R180)
               )
               .select(
                  AttachFace.CEILING,
                  Direction.WEST,
                  Variant.variant()
                     .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)
                     .with(VariantProperties.X_ROT, VariantProperties.Rotation.R180)
               )
               .select(AttachFace.CEILING, Direction.SOUTH, Variant.variant().with(VariantProperties.X_ROT, VariantProperties.Rotation.R180))
               .select(
                  AttachFace.CEILING,
                  Direction.NORTH,
                  Variant.variant()
                     .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180)
                     .with(VariantProperties.X_ROT, VariantProperties.Rotation.R180)
               )
         );
   }

   private static PropertyDispatch.C4<Direction, DoubleBlockHalf, DoorHingeSide, Boolean> configureDoorHalf(
      PropertyDispatch.C4<Direction, DoubleBlockHalf, DoorHingeSide, Boolean> p_236305_,
      DoubleBlockHalf p_236306_,
      ResourceLocation p_236307_,
      ResourceLocation p_236308_,
      ResourceLocation p_236309_,
      ResourceLocation p_236310_
   ) {
      return p_236305_.select(Direction.EAST, p_236306_, DoorHingeSide.LEFT, false, Variant.variant().with(VariantProperties.MODEL, p_236307_))
         .select(
            Direction.SOUTH,
            p_236306_,
            DoorHingeSide.LEFT,
            false,
            Variant.variant().with(VariantProperties.MODEL, p_236307_).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)
         )
         .select(
            Direction.WEST,
            p_236306_,
            DoorHingeSide.LEFT,
            false,
            Variant.variant().with(VariantProperties.MODEL, p_236307_).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180)
         )
         .select(
            Direction.NORTH,
            p_236306_,
            DoorHingeSide.LEFT,
            false,
            Variant.variant().with(VariantProperties.MODEL, p_236307_).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)
         )
         .select(Direction.EAST, p_236306_, DoorHingeSide.RIGHT, false, Variant.variant().with(VariantProperties.MODEL, p_236309_))
         .select(
            Direction.SOUTH,
            p_236306_,
            DoorHingeSide.RIGHT,
            false,
            Variant.variant().with(VariantProperties.MODEL, p_236309_).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)
         )
         .select(
            Direction.WEST,
            p_236306_,
            DoorHingeSide.RIGHT,
            false,
            Variant.variant().with(VariantProperties.MODEL, p_236309_).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180)
         )
         .select(
            Direction.NORTH,
            p_236306_,
            DoorHingeSide.RIGHT,
            false,
            Variant.variant().with(VariantProperties.MODEL, p_236309_).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)
         )
         .select(
            Direction.EAST,
            p_236306_,
            DoorHingeSide.LEFT,
            true,
            Variant.variant().with(VariantProperties.MODEL, p_236308_).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)
         )
         .select(
            Direction.SOUTH,
            p_236306_,
            DoorHingeSide.LEFT,
            true,
            Variant.variant().with(VariantProperties.MODEL, p_236308_).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180)
         )
         .select(
            Direction.WEST,
            p_236306_,
            DoorHingeSide.LEFT,
            true,
            Variant.variant().with(VariantProperties.MODEL, p_236308_).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)
         )
         .select(Direction.NORTH, p_236306_, DoorHingeSide.LEFT, true, Variant.variant().with(VariantProperties.MODEL, p_236308_))
         .select(
            Direction.EAST,
            p_236306_,
            DoorHingeSide.RIGHT,
            true,
            Variant.variant().with(VariantProperties.MODEL, p_236310_).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)
         )
         .select(Direction.SOUTH, p_236306_, DoorHingeSide.RIGHT, true, Variant.variant().with(VariantProperties.MODEL, p_236310_))
         .select(
            Direction.WEST,
            p_236306_,
            DoorHingeSide.RIGHT,
            true,
            Variant.variant().with(VariantProperties.MODEL, p_236310_).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)
         )
         .select(
            Direction.NORTH,
            p_236306_,
            DoorHingeSide.RIGHT,
            true,
            Variant.variant().with(VariantProperties.MODEL, p_236310_).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180)
         );
   }

   private static BlockStateGenerator createDoor(
      Block p_236284_,
      ResourceLocation p_236285_,
      ResourceLocation p_236286_,
      ResourceLocation p_236287_,
      ResourceLocation p_236288_,
      ResourceLocation p_236289_,
      ResourceLocation p_236290_,
      ResourceLocation p_236291_,
      ResourceLocation p_236292_
   ) {
      return MultiVariantGenerator.multiVariant(p_236284_)
         .with(
            configureDoorHalf(
               configureDoorHalf(
                  PropertyDispatch.properties(
                     BlockStateProperties.HORIZONTAL_FACING, BlockStateProperties.DOUBLE_BLOCK_HALF, BlockStateProperties.DOOR_HINGE, BlockStateProperties.OPEN
                  ),
                  DoubleBlockHalf.LOWER,
                  p_236285_,
                  p_236286_,
                  p_236287_,
                  p_236288_
               ),
               DoubleBlockHalf.UPPER,
               p_236289_,
               p_236290_,
               p_236291_,
               p_236292_
            )
         );
   }

   static BlockStateGenerator createCustomFence(
      Block p_248625_,
      ResourceLocation p_248654_,
      ResourceLocation p_249827_,
      ResourceLocation p_248819_,
      ResourceLocation p_251062_,
      ResourceLocation p_249076_
   ) {
      return MultiPartGenerator.multiPart(p_248625_)
         .with(Variant.variant().with(VariantProperties.MODEL, p_248654_))
         .with(
            Condition.condition().term(BlockStateProperties.NORTH, true),
            Variant.variant().with(VariantProperties.MODEL, p_249827_).with(VariantProperties.UV_LOCK, false)
         )
         .with(
            Condition.condition().term(BlockStateProperties.EAST, true),
            Variant.variant().with(VariantProperties.MODEL, p_248819_).with(VariantProperties.UV_LOCK, false)
         )
         .with(
            Condition.condition().term(BlockStateProperties.SOUTH, true),
            Variant.variant().with(VariantProperties.MODEL, p_251062_).with(VariantProperties.UV_LOCK, false)
         )
         .with(
            Condition.condition().term(BlockStateProperties.WEST, true),
            Variant.variant().with(VariantProperties.MODEL, p_249076_).with(VariantProperties.UV_LOCK, false)
         );
   }

   static BlockStateGenerator createFence(Block p_124905_, ResourceLocation p_124906_, ResourceLocation p_124907_) {
      return MultiPartGenerator.multiPart(p_124905_)
         .with(Variant.variant().with(VariantProperties.MODEL, p_124906_))
         .with(
            Condition.condition().term(BlockStateProperties.NORTH, true),
            Variant.variant().with(VariantProperties.MODEL, p_124907_).with(VariantProperties.UV_LOCK, true)
         )
         .with(
            Condition.condition().term(BlockStateProperties.EAST, true),
            Variant.variant()
               .with(VariantProperties.MODEL, p_124907_)
               .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)
               .with(VariantProperties.UV_LOCK, true)
         )
         .with(
            Condition.condition().term(BlockStateProperties.SOUTH, true),
            Variant.variant()
               .with(VariantProperties.MODEL, p_124907_)
               .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180)
               .with(VariantProperties.UV_LOCK, true)
         )
         .with(
            Condition.condition().term(BlockStateProperties.WEST, true),
            Variant.variant()
               .with(VariantProperties.MODEL, p_124907_)
               .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)
               .with(VariantProperties.UV_LOCK, true)
         );
   }

   static BlockStateGenerator createWall(Block p_124839_, ResourceLocation p_124840_, ResourceLocation p_124841_, ResourceLocation p_124842_) {
      return MultiPartGenerator.multiPart(p_124839_)
         .with(Condition.condition().term(BlockStateProperties.UP, true), Variant.variant().with(VariantProperties.MODEL, p_124840_))
         .with(
            Condition.condition().term(BlockStateProperties.NORTH_WALL, WallSide.LOW),
            Variant.variant().with(VariantProperties.MODEL, p_124841_).with(VariantProperties.UV_LOCK, true)
         )
         .with(
            Condition.condition().term(BlockStateProperties.EAST_WALL, WallSide.LOW),
            Variant.variant()
               .with(VariantProperties.MODEL, p_124841_)
               .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)
               .with(VariantProperties.UV_LOCK, true)
         )
         .with(
            Condition.condition().term(BlockStateProperties.SOUTH_WALL, WallSide.LOW),
            Variant.variant()
               .with(VariantProperties.MODEL, p_124841_)
               .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180)
               .with(VariantProperties.UV_LOCK, true)
         )
         .with(
            Condition.condition().term(BlockStateProperties.WEST_WALL, WallSide.LOW),
            Variant.variant()
               .with(VariantProperties.MODEL, p_124841_)
               .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)
               .with(VariantProperties.UV_LOCK, true)
         )
         .with(
            Condition.condition().term(BlockStateProperties.NORTH_WALL, WallSide.TALL),
            Variant.variant().with(VariantProperties.MODEL, p_124842_).with(VariantProperties.UV_LOCK, true)
         )
         .with(
            Condition.condition().term(BlockStateProperties.EAST_WALL, WallSide.TALL),
            Variant.variant()
               .with(VariantProperties.MODEL, p_124842_)
               .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)
               .with(VariantProperties.UV_LOCK, true)
         )
         .with(
            Condition.condition().term(BlockStateProperties.SOUTH_WALL, WallSide.TALL),
            Variant.variant()
               .with(VariantProperties.MODEL, p_124842_)
               .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180)
               .with(VariantProperties.UV_LOCK, true)
         )
         .with(
            Condition.condition().term(BlockStateProperties.WEST_WALL, WallSide.TALL),
            Variant.variant()
               .with(VariantProperties.MODEL, p_124842_)
               .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)
               .with(VariantProperties.UV_LOCK, true)
         );
   }

   static BlockStateGenerator createFenceGate(
      Block p_124810_, ResourceLocation p_124811_, ResourceLocation p_124812_, ResourceLocation p_124813_, ResourceLocation p_124814_, boolean p_251730_
   ) {
      return MultiVariantGenerator.multiVariant(p_124810_, Variant.variant().with(VariantProperties.UV_LOCK, p_251730_))
         .with(createHorizontalFacingDispatchAlt())
         .with(
            PropertyDispatch.properties(BlockStateProperties.IN_WALL, BlockStateProperties.OPEN)
               .select(false, false, Variant.variant().with(VariantProperties.MODEL, p_124812_))
               .select(true, false, Variant.variant().with(VariantProperties.MODEL, p_124814_))
               .select(false, true, Variant.variant().with(VariantProperties.MODEL, p_124811_))
               .select(true, true, Variant.variant().with(VariantProperties.MODEL, p_124813_))
         );
   }

   static BlockStateGenerator createStairs(Block p_124867_, ResourceLocation p_124868_, ResourceLocation p_124869_, ResourceLocation p_124870_) {
      return MultiVariantGenerator.multiVariant(p_124867_)
         .with(
            PropertyDispatch.properties(BlockStateProperties.HORIZONTAL_FACING, BlockStateProperties.HALF, BlockStateProperties.STAIRS_SHAPE)
               .select(Direction.EAST, Half.BOTTOM, StairsShape.STRAIGHT, Variant.variant().with(VariantProperties.MODEL, p_124869_))
               .select(
                  Direction.WEST,
                  Half.BOTTOM,
                  StairsShape.STRAIGHT,
                  Variant.variant()
                     .with(VariantProperties.MODEL, p_124869_)
                     .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180)
                     .with(VariantProperties.UV_LOCK, true)
               )
               .select(
                  Direction.SOUTH,
                  Half.BOTTOM,
                  StairsShape.STRAIGHT,
                  Variant.variant()
                     .with(VariantProperties.MODEL, p_124869_)
                     .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)
                     .with(VariantProperties.UV_LOCK, true)
               )
               .select(
                  Direction.NORTH,
                  Half.BOTTOM,
                  StairsShape.STRAIGHT,
                  Variant.variant()
                     .with(VariantProperties.MODEL, p_124869_)
                     .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)
                     .with(VariantProperties.UV_LOCK, true)
               )
               .select(Direction.EAST, Half.BOTTOM, StairsShape.OUTER_RIGHT, Variant.variant().with(VariantProperties.MODEL, p_124870_))
               .select(
                  Direction.WEST,
                  Half.BOTTOM,
                  StairsShape.OUTER_RIGHT,
                  Variant.variant()
                     .with(VariantProperties.MODEL, p_124870_)
                     .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180)
                     .with(VariantProperties.UV_LOCK, true)
               )
               .select(
                  Direction.SOUTH,
                  Half.BOTTOM,
                  StairsShape.OUTER_RIGHT,
                  Variant.variant()
                     .with(VariantProperties.MODEL, p_124870_)
                     .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)
                     .with(VariantProperties.UV_LOCK, true)
               )
               .select(
                  Direction.NORTH,
                  Half.BOTTOM,
                  StairsShape.OUTER_RIGHT,
                  Variant.variant()
                     .with(VariantProperties.MODEL, p_124870_)
                     .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)
                     .with(VariantProperties.UV_LOCK, true)
               )
               .select(
                  Direction.EAST,
                  Half.BOTTOM,
                  StairsShape.OUTER_LEFT,
                  Variant.variant()
                     .with(VariantProperties.MODEL, p_124870_)
                     .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)
                     .with(VariantProperties.UV_LOCK, true)
               )
               .select(
                  Direction.WEST,
                  Half.BOTTOM,
                  StairsShape.OUTER_LEFT,
                  Variant.variant()
                     .with(VariantProperties.MODEL, p_124870_)
                     .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)
                     .with(VariantProperties.UV_LOCK, true)
               )
               .select(Direction.SOUTH, Half.BOTTOM, StairsShape.OUTER_LEFT, Variant.variant().with(VariantProperties.MODEL, p_124870_))
               .select(
                  Direction.NORTH,
                  Half.BOTTOM,
                  StairsShape.OUTER_LEFT,
                  Variant.variant()
                     .with(VariantProperties.MODEL, p_124870_)
                     .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180)
                     .with(VariantProperties.UV_LOCK, true)
               )
               .select(Direction.EAST, Half.BOTTOM, StairsShape.INNER_RIGHT, Variant.variant().with(VariantProperties.MODEL, p_124868_))
               .select(
                  Direction.WEST,
                  Half.BOTTOM,
                  StairsShape.INNER_RIGHT,
                  Variant.variant()
                     .with(VariantProperties.MODEL, p_124868_)
                     .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180)
                     .with(VariantProperties.UV_LOCK, true)
               )
               .select(
                  Direction.SOUTH,
                  Half.BOTTOM,
                  StairsShape.INNER_RIGHT,
                  Variant.variant()
                     .with(VariantProperties.MODEL, p_124868_)
                     .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)
                     .with(VariantProperties.UV_LOCK, true)
               )
               .select(
                  Direction.NORTH,
                  Half.BOTTOM,
                  StairsShape.INNER_RIGHT,
                  Variant.variant()
                     .with(VariantProperties.MODEL, p_124868_)
                     .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)
                     .with(VariantProperties.UV_LOCK, true)
               )
               .select(
                  Direction.EAST,
                  Half.BOTTOM,
                  StairsShape.INNER_LEFT,
                  Variant.variant()
                     .with(VariantProperties.MODEL, p_124868_)
                     .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)
                     .with(VariantProperties.UV_LOCK, true)
               )
               .select(
                  Direction.WEST,
                  Half.BOTTOM,
                  StairsShape.INNER_LEFT,
                  Variant.variant()
                     .with(VariantProperties.MODEL, p_124868_)
                     .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)
                     .with(VariantProperties.UV_LOCK, true)
               )
               .select(Direction.SOUTH, Half.BOTTOM, StairsShape.INNER_LEFT, Variant.variant().with(VariantProperties.MODEL, p_124868_))
               .select(
                  Direction.NORTH,
                  Half.BOTTOM,
                  StairsShape.INNER_LEFT,
                  Variant.variant()
                     .with(VariantProperties.MODEL, p_124868_)
                     .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180)
                     .with(VariantProperties.UV_LOCK, true)
               )
               .select(
                  Direction.EAST,
                  Half.TOP,
                  StairsShape.STRAIGHT,
                  Variant.variant()
                     .with(VariantProperties.MODEL, p_124869_)
                     .with(VariantProperties.X_ROT, VariantProperties.Rotation.R180)
                     .with(VariantProperties.UV_LOCK, true)
               )
               .select(
                  Direction.WEST,
                  Half.TOP,
                  StairsShape.STRAIGHT,
                  Variant.variant()
                     .with(VariantProperties.MODEL, p_124869_)
                     .with(VariantProperties.X_ROT, VariantProperties.Rotation.R180)
                     .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180)
                     .with(VariantProperties.UV_LOCK, true)
               )
               .select(
                  Direction.SOUTH,
                  Half.TOP,
                  StairsShape.STRAIGHT,
                  Variant.variant()
                     .with(VariantProperties.MODEL, p_124869_)
                     .with(VariantProperties.X_ROT, VariantProperties.Rotation.R180)
                     .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)
                     .with(VariantProperties.UV_LOCK, true)
               )
               .select(
                  Direction.NORTH,
                  Half.TOP,
                  StairsShape.STRAIGHT,
                  Variant.variant()
                     .with(VariantProperties.MODEL, p_124869_)
                     .with(VariantProperties.X_ROT, VariantProperties.Rotation.R180)
                     .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)
                     .with(VariantProperties.UV_LOCK, true)
               )
               .select(
                  Direction.EAST,
                  Half.TOP,
                  StairsShape.OUTER_RIGHT,
                  Variant.variant()
                     .with(VariantProperties.MODEL, p_124870_)
                     .with(VariantProperties.X_ROT, VariantProperties.Rotation.R180)
                     .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)
                     .with(VariantProperties.UV_LOCK, true)
               )
               .select(
                  Direction.WEST,
                  Half.TOP,
                  StairsShape.OUTER_RIGHT,
                  Variant.variant()
                     .with(VariantProperties.MODEL, p_124870_)
                     .with(VariantProperties.X_ROT, VariantProperties.Rotation.R180)
                     .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)
                     .with(VariantProperties.UV_LOCK, true)
               )
               .select(
                  Direction.SOUTH,
                  Half.TOP,
                  StairsShape.OUTER_RIGHT,
                  Variant.variant()
                     .with(VariantProperties.MODEL, p_124870_)
                     .with(VariantProperties.X_ROT, VariantProperties.Rotation.R180)
                     .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180)
                     .with(VariantProperties.UV_LOCK, true)
               )
               .select(
                  Direction.NORTH,
                  Half.TOP,
                  StairsShape.OUTER_RIGHT,
                  Variant.variant()
                     .with(VariantProperties.MODEL, p_124870_)
                     .with(VariantProperties.X_ROT, VariantProperties.Rotation.R180)
                     .with(VariantProperties.UV_LOCK, true)
               )
               .select(
                  Direction.EAST,
                  Half.TOP,
                  StairsShape.OUTER_LEFT,
                  Variant.variant()
                     .with(VariantProperties.MODEL, p_124870_)
                     .with(VariantProperties.X_ROT, VariantProperties.Rotation.R180)
                     .with(VariantProperties.UV_LOCK, true)
               )
               .select(
                  Direction.WEST,
                  Half.TOP,
                  StairsShape.OUTER_LEFT,
                  Variant.variant()
                     .with(VariantProperties.MODEL, p_124870_)
                     .with(VariantProperties.X_ROT, VariantProperties.Rotation.R180)
                     .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180)
                     .with(VariantProperties.UV_LOCK, true)
               )
               .select(
                  Direction.SOUTH,
                  Half.TOP,
                  StairsShape.OUTER_LEFT,
                  Variant.variant()
                     .with(VariantProperties.MODEL, p_124870_)
                     .with(VariantProperties.X_ROT, VariantProperties.Rotation.R180)
                     .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)
                     .with(VariantProperties.UV_LOCK, true)
               )
               .select(
                  Direction.NORTH,
                  Half.TOP,
                  StairsShape.OUTER_LEFT,
                  Variant.variant()
                     .with(VariantProperties.MODEL, p_124870_)
                     .with(VariantProperties.X_ROT, VariantProperties.Rotation.R180)
                     .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)
                     .with(VariantProperties.UV_LOCK, true)
               )
               .select(
                  Direction.EAST,
                  Half.TOP,
                  StairsShape.INNER_RIGHT,
                  Variant.variant()
                     .with(VariantProperties.MODEL, p_124868_)
                     .with(VariantProperties.X_ROT, VariantProperties.Rotation.R180)
                     .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)
                     .with(VariantProperties.UV_LOCK, true)
               )
               .select(
                  Direction.WEST,
                  Half.TOP,
                  StairsShape.INNER_RIGHT,
                  Variant.variant()
                     .with(VariantProperties.MODEL, p_124868_)
                     .with(VariantProperties.X_ROT, VariantProperties.Rotation.R180)
                     .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)
                     .with(VariantProperties.UV_LOCK, true)
               )
               .select(
                  Direction.SOUTH,
                  Half.TOP,
                  StairsShape.INNER_RIGHT,
                  Variant.variant()
                     .with(VariantProperties.MODEL, p_124868_)
                     .with(VariantProperties.X_ROT, VariantProperties.Rotation.R180)
                     .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180)
                     .with(VariantProperties.UV_LOCK, true)
               )
               .select(
                  Direction.NORTH,
                  Half.TOP,
                  StairsShape.INNER_RIGHT,
                  Variant.variant()
                     .with(VariantProperties.MODEL, p_124868_)
                     .with(VariantProperties.X_ROT, VariantProperties.Rotation.R180)
                     .with(VariantProperties.UV_LOCK, true)
               )
               .select(
                  Direction.EAST,
                  Half.TOP,
                  StairsShape.INNER_LEFT,
                  Variant.variant()
                     .with(VariantProperties.MODEL, p_124868_)
                     .with(VariantProperties.X_ROT, VariantProperties.Rotation.R180)
                     .with(VariantProperties.UV_LOCK, true)
               )
               .select(
                  Direction.WEST,
                  Half.TOP,
                  StairsShape.INNER_LEFT,
                  Variant.variant()
                     .with(VariantProperties.MODEL, p_124868_)
                     .with(VariantProperties.X_ROT, VariantProperties.Rotation.R180)
                     .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180)
                     .with(VariantProperties.UV_LOCK, true)
               )
               .select(
                  Direction.SOUTH,
                  Half.TOP,
                  StairsShape.INNER_LEFT,
                  Variant.variant()
                     .with(VariantProperties.MODEL, p_124868_)
                     .with(VariantProperties.X_ROT, VariantProperties.Rotation.R180)
                     .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)
                     .with(VariantProperties.UV_LOCK, true)
               )
               .select(
                  Direction.NORTH,
                  Half.TOP,
                  StairsShape.INNER_LEFT,
                  Variant.variant()
                     .with(VariantProperties.MODEL, p_124868_)
                     .with(VariantProperties.X_ROT, VariantProperties.Rotation.R180)
                     .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)
                     .with(VariantProperties.UV_LOCK, true)
               )
         );
   }

   private static BlockStateGenerator createOrientableTrapdoor(
      Block p_124889_, ResourceLocation p_124890_, ResourceLocation p_124891_, ResourceLocation p_124892_
   ) {
      return MultiVariantGenerator.multiVariant(p_124889_)
         .with(
            PropertyDispatch.properties(BlockStateProperties.HORIZONTAL_FACING, BlockStateProperties.HALF, BlockStateProperties.OPEN)
               .select(Direction.NORTH, Half.BOTTOM, false, Variant.variant().with(VariantProperties.MODEL, p_124891_))
               .select(
                  Direction.SOUTH,
                  Half.BOTTOM,
                  false,
                  Variant.variant().with(VariantProperties.MODEL, p_124891_).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180)
               )
               .select(
                  Direction.EAST,
                  Half.BOTTOM,
                  false,
                  Variant.variant().with(VariantProperties.MODEL, p_124891_).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)
               )
               .select(
                  Direction.WEST,
                  Half.BOTTOM,
                  false,
                  Variant.variant().with(VariantProperties.MODEL, p_124891_).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)
               )
               .select(Direction.NORTH, Half.TOP, false, Variant.variant().with(VariantProperties.MODEL, p_124890_))
               .select(
                  Direction.SOUTH,
                  Half.TOP,
                  false,
                  Variant.variant().with(VariantProperties.MODEL, p_124890_).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180)
               )
               .select(
                  Direction.EAST,
                  Half.TOP,
                  false,
                  Variant.variant().with(VariantProperties.MODEL, p_124890_).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)
               )
               .select(
                  Direction.WEST,
                  Half.TOP,
                  false,
                  Variant.variant().with(VariantProperties.MODEL, p_124890_).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)
               )
               .select(Direction.NORTH, Half.BOTTOM, true, Variant.variant().with(VariantProperties.MODEL, p_124892_))
               .select(
                  Direction.SOUTH,
                  Half.BOTTOM,
                  true,
                  Variant.variant().with(VariantProperties.MODEL, p_124892_).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180)
               )
               .select(
                  Direction.EAST,
                  Half.BOTTOM,
                  true,
                  Variant.variant().with(VariantProperties.MODEL, p_124892_).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)
               )
               .select(
                  Direction.WEST,
                  Half.BOTTOM,
                  true,
                  Variant.variant().with(VariantProperties.MODEL, p_124892_).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)
               )
               .select(
                  Direction.NORTH,
                  Half.TOP,
                  true,
                  Variant.variant()
                     .with(VariantProperties.MODEL, p_124892_)
                     .with(VariantProperties.X_ROT, VariantProperties.Rotation.R180)
                     .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180)
               )
               .select(
                  Direction.SOUTH,
                  Half.TOP,
                  true,
                  Variant.variant()
                     .with(VariantProperties.MODEL, p_124892_)
                     .with(VariantProperties.X_ROT, VariantProperties.Rotation.R180)
                     .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R0)
               )
               .select(
                  Direction.EAST,
                  Half.TOP,
                  true,
                  Variant.variant()
                     .with(VariantProperties.MODEL, p_124892_)
                     .with(VariantProperties.X_ROT, VariantProperties.Rotation.R180)
                     .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)
               )
               .select(
                  Direction.WEST,
                  Half.TOP,
                  true,
                  Variant.variant()
                     .with(VariantProperties.MODEL, p_124892_)
                     .with(VariantProperties.X_ROT, VariantProperties.Rotation.R180)
                     .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)
               )
         );
   }

   private static BlockStateGenerator createTrapdoor(Block p_124909_, ResourceLocation p_124910_, ResourceLocation p_124911_, ResourceLocation p_124912_) {
      return MultiVariantGenerator.multiVariant(p_124909_)
         .with(
            PropertyDispatch.properties(BlockStateProperties.HORIZONTAL_FACING, BlockStateProperties.HALF, BlockStateProperties.OPEN)
               .select(Direction.NORTH, Half.BOTTOM, false, Variant.variant().with(VariantProperties.MODEL, p_124911_))
               .select(Direction.SOUTH, Half.BOTTOM, false, Variant.variant().with(VariantProperties.MODEL, p_124911_))
               .select(Direction.EAST, Half.BOTTOM, false, Variant.variant().with(VariantProperties.MODEL, p_124911_))
               .select(Direction.WEST, Half.BOTTOM, false, Variant.variant().with(VariantProperties.MODEL, p_124911_))
               .select(Direction.NORTH, Half.TOP, false, Variant.variant().with(VariantProperties.MODEL, p_124910_))
               .select(Direction.SOUTH, Half.TOP, false, Variant.variant().with(VariantProperties.MODEL, p_124910_))
               .select(Direction.EAST, Half.TOP, false, Variant.variant().with(VariantProperties.MODEL, p_124910_))
               .select(Direction.WEST, Half.TOP, false, Variant.variant().with(VariantProperties.MODEL, p_124910_))
               .select(Direction.NORTH, Half.BOTTOM, true, Variant.variant().with(VariantProperties.MODEL, p_124912_))
               .select(
                  Direction.SOUTH,
                  Half.BOTTOM,
                  true,
                  Variant.variant().with(VariantProperties.MODEL, p_124912_).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180)
               )
               .select(
                  Direction.EAST,
                  Half.BOTTOM,
                  true,
                  Variant.variant().with(VariantProperties.MODEL, p_124912_).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)
               )
               .select(
                  Direction.WEST,
                  Half.BOTTOM,
                  true,
                  Variant.variant().with(VariantProperties.MODEL, p_124912_).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)
               )
               .select(Direction.NORTH, Half.TOP, true, Variant.variant().with(VariantProperties.MODEL, p_124912_))
               .select(
                  Direction.SOUTH,
                  Half.TOP,
                  true,
                  Variant.variant().with(VariantProperties.MODEL, p_124912_).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180)
               )
               .select(
                  Direction.EAST,
                  Half.TOP,
                  true,
                  Variant.variant().with(VariantProperties.MODEL, p_124912_).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)
               )
               .select(
                  Direction.WEST,
                  Half.TOP,
                  true,
                  Variant.variant().with(VariantProperties.MODEL, p_124912_).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)
               )
         );
   }

   static MultiVariantGenerator createSimpleBlock(Block p_124860_, ResourceLocation p_124861_) {
      return MultiVariantGenerator.multiVariant(p_124860_, Variant.variant().with(VariantProperties.MODEL, p_124861_));
   }

   private static PropertyDispatch createRotatedPillar() {
      return PropertyDispatch.property(BlockStateProperties.AXIS)
         .select(Direction.Axis.Y, Variant.variant())
         .select(Direction.Axis.Z, Variant.variant().with(VariantProperties.X_ROT, VariantProperties.Rotation.R90))
         .select(
            Direction.Axis.X,
            Variant.variant().with(VariantProperties.X_ROT, VariantProperties.Rotation.R90).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)
         );
   }

   static BlockStateGenerator createPillarBlockUVLocked(
      Block p_259670_, TextureMapping p_259852_, BiConsumer<ResourceLocation, Supplier<JsonElement>> p_259181_
   ) {
      ResourceLocation $$3 = ModelTemplates.CUBE_COLUMN_UV_LOCKED_X.create(p_259670_, p_259852_, p_259181_);
      ResourceLocation $$4 = ModelTemplates.CUBE_COLUMN_UV_LOCKED_Y.create(p_259670_, p_259852_, p_259181_);
      ResourceLocation $$5 = ModelTemplates.CUBE_COLUMN_UV_LOCKED_Z.create(p_259670_, p_259852_, p_259181_);
      ResourceLocation $$6 = ModelTemplates.CUBE_COLUMN.create(p_259670_, p_259852_, p_259181_);
      return MultiVariantGenerator.multiVariant(p_259670_, Variant.variant().with(VariantProperties.MODEL, $$6))
         .with(
            PropertyDispatch.property(BlockStateProperties.AXIS)
               .select(Direction.Axis.X, Variant.variant().with(VariantProperties.MODEL, $$3))
               .select(Direction.Axis.Y, Variant.variant().with(VariantProperties.MODEL, $$4))
               .select(Direction.Axis.Z, Variant.variant().with(VariantProperties.MODEL, $$5))
         );
   }

   static BlockStateGenerator createAxisAlignedPillarBlock(Block p_124882_, ResourceLocation p_124883_) {
      return MultiVariantGenerator.multiVariant(p_124882_, Variant.variant().with(VariantProperties.MODEL, p_124883_)).with(createRotatedPillar());
   }

   private void createAxisAlignedPillarBlockCustomModel(Block p_124902_, ResourceLocation p_124903_) {
      this.blockStateOutput.accept(createAxisAlignedPillarBlock(p_124902_, p_124903_));
   }

   public void createAxisAlignedPillarBlock(Block p_124587_, TexturedModel.Provider p_124588_) {
      ResourceLocation $$2 = p_124588_.create(p_124587_, this.modelOutput);
      this.blockStateOutput.accept(createAxisAlignedPillarBlock(p_124587_, $$2));
   }

   private void createHorizontallyRotatedBlock(Block p_124745_, TexturedModel.Provider p_124746_) {
      ResourceLocation $$2 = p_124746_.create(p_124745_, this.modelOutput);
      this.blockStateOutput
         .accept(MultiVariantGenerator.multiVariant(p_124745_, Variant.variant().with(VariantProperties.MODEL, $$2)).with(createHorizontalFacingDispatch()));
   }

   static BlockStateGenerator createRotatedPillarWithHorizontalVariant(Block p_124925_, ResourceLocation p_124926_, ResourceLocation p_124927_) {
      return MultiVariantGenerator.multiVariant(p_124925_)
         .with(
            PropertyDispatch.property(BlockStateProperties.AXIS)
               .select(Direction.Axis.Y, Variant.variant().with(VariantProperties.MODEL, p_124926_))
               .select(
                  Direction.Axis.Z, Variant.variant().with(VariantProperties.MODEL, p_124927_).with(VariantProperties.X_ROT, VariantProperties.Rotation.R90)
               )
               .select(
                  Direction.Axis.X,
                  Variant.variant()
                     .with(VariantProperties.MODEL, p_124927_)
                     .with(VariantProperties.X_ROT, VariantProperties.Rotation.R90)
                     .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)
               )
         );
   }

   private void createRotatedPillarWithHorizontalVariant(Block p_124590_, TexturedModel.Provider p_124591_, TexturedModel.Provider p_124592_) {
      ResourceLocation $$3 = p_124591_.create(p_124590_, this.modelOutput);
      ResourceLocation $$4 = p_124592_.create(p_124590_, this.modelOutput);
      this.blockStateOutput.accept(createRotatedPillarWithHorizontalVariant(p_124590_, $$3, $$4));
   }

   private ResourceLocation createSuffixedVariant(
      Block p_124579_, String p_124580_, ModelTemplate p_124581_, Function<ResourceLocation, TextureMapping> p_124582_
   ) {
      return p_124581_.createWithSuffix(p_124579_, p_124580_, p_124582_.apply(TextureMapping.getBlockTexture(p_124579_, p_124580_)), this.modelOutput);
   }

   static BlockStateGenerator createPressurePlate(Block p_124942_, ResourceLocation p_124943_, ResourceLocation p_124944_) {
      return MultiVariantGenerator.multiVariant(p_124942_).with(createBooleanModelDispatch(BlockStateProperties.POWERED, p_124944_, p_124943_));
   }

   static BlockStateGenerator createSlab(Block p_124929_, ResourceLocation p_124930_, ResourceLocation p_124931_, ResourceLocation p_124932_) {
      return MultiVariantGenerator.multiVariant(p_124929_)
         .with(
            PropertyDispatch.property(BlockStateProperties.SLAB_TYPE)
               .select(SlabType.BOTTOM, Variant.variant().with(VariantProperties.MODEL, p_124930_))
               .select(SlabType.TOP, Variant.variant().with(VariantProperties.MODEL, p_124931_))
               .select(SlabType.DOUBLE, Variant.variant().with(VariantProperties.MODEL, p_124932_))
         );
   }

   public void createTrivialCube(Block p_124852_) {
      this.createTrivialBlock(p_124852_, TexturedModel.CUBE);
   }

   public void createTrivialBlock(Block p_124795_, TexturedModel.Provider p_124796_) {
      this.blockStateOutput.accept(createSimpleBlock(p_124795_, p_124796_.create(p_124795_, this.modelOutput)));
   }

   private void createTrivialBlock(Block p_124568_, TextureMapping p_124569_, ModelTemplate p_124570_) {
      ResourceLocation $$3 = p_124570_.create(p_124568_, p_124569_, this.modelOutput);
      this.blockStateOutput.accept(createSimpleBlock(p_124568_, $$3));
   }

   private BlockModelGenerators.BlockFamilyProvider family(Block p_124877_) {
      TexturedModel $$1 = this.texturedModels.getOrDefault(p_124877_, TexturedModel.CUBE.get(p_124877_));
      return new BlockModelGenerators.BlockFamilyProvider($$1.getMapping()).fullBlock(p_124877_, $$1.getTemplate());
   }

   public void createHangingSign(Block p_249023_, Block p_250861_, Block p_250943_) {
      TextureMapping $$3 = TextureMapping.particle(p_249023_);
      ResourceLocation $$4 = ModelTemplates.PARTICLE_ONLY.create(p_250861_, $$3, this.modelOutput);
      this.blockStateOutput.accept(createSimpleBlock(p_250861_, $$4));
      this.blockStateOutput.accept(createSimpleBlock(p_250943_, $$4));
      this.createSimpleFlatItemModel(p_250861_.asItem());
      this.skipAutoItemBlock(p_250943_);
   }

   void createDoor(Block p_124897_) {
      TextureMapping $$1 = TextureMapping.door(p_124897_);
      ResourceLocation $$2 = ModelTemplates.DOOR_BOTTOM_LEFT.create(p_124897_, $$1, this.modelOutput);
      ResourceLocation $$3 = ModelTemplates.DOOR_BOTTOM_LEFT_OPEN.create(p_124897_, $$1, this.modelOutput);
      ResourceLocation $$4 = ModelTemplates.DOOR_BOTTOM_RIGHT.create(p_124897_, $$1, this.modelOutput);
      ResourceLocation $$5 = ModelTemplates.DOOR_BOTTOM_RIGHT_OPEN.create(p_124897_, $$1, this.modelOutput);
      ResourceLocation $$6 = ModelTemplates.DOOR_TOP_LEFT.create(p_124897_, $$1, this.modelOutput);
      ResourceLocation $$7 = ModelTemplates.DOOR_TOP_LEFT_OPEN.create(p_124897_, $$1, this.modelOutput);
      ResourceLocation $$8 = ModelTemplates.DOOR_TOP_RIGHT.create(p_124897_, $$1, this.modelOutput);
      ResourceLocation $$9 = ModelTemplates.DOOR_TOP_RIGHT_OPEN.create(p_124897_, $$1, this.modelOutput);
      this.createSimpleFlatItemModel(p_124897_.asItem());
      this.blockStateOutput.accept(createDoor(p_124897_, $$2, $$3, $$4, $$5, $$6, $$7, $$8, $$9));
   }

   private void copyDoorModel(Block p_308919_, Block p_308994_) {
      ResourceLocation $$2 = ModelTemplates.DOOR_BOTTOM_LEFT.getDefaultModelLocation(p_308919_);
      ResourceLocation $$3 = ModelTemplates.DOOR_BOTTOM_LEFT_OPEN.getDefaultModelLocation(p_308919_);
      ResourceLocation $$4 = ModelTemplates.DOOR_BOTTOM_RIGHT.getDefaultModelLocation(p_308919_);
      ResourceLocation $$5 = ModelTemplates.DOOR_BOTTOM_RIGHT_OPEN.getDefaultModelLocation(p_308919_);
      ResourceLocation $$6 = ModelTemplates.DOOR_TOP_LEFT.getDefaultModelLocation(p_308919_);
      ResourceLocation $$7 = ModelTemplates.DOOR_TOP_LEFT_OPEN.getDefaultModelLocation(p_308919_);
      ResourceLocation $$8 = ModelTemplates.DOOR_TOP_RIGHT.getDefaultModelLocation(p_308919_);
      ResourceLocation $$9 = ModelTemplates.DOOR_TOP_RIGHT_OPEN.getDefaultModelLocation(p_308919_);
      this.delegateItemModel(p_308994_, ModelLocationUtils.getModelLocation(p_308919_.asItem()));
      this.blockStateOutput.accept(createDoor(p_308994_, $$2, $$3, $$4, $$5, $$6, $$7, $$8, $$9));
   }

   void createOrientableTrapdoor(Block p_124917_) {
      TextureMapping $$1 = TextureMapping.defaultTexture(p_124917_);
      ResourceLocation $$2 = ModelTemplates.ORIENTABLE_TRAPDOOR_TOP.create(p_124917_, $$1, this.modelOutput);
      ResourceLocation $$3 = ModelTemplates.ORIENTABLE_TRAPDOOR_BOTTOM.create(p_124917_, $$1, this.modelOutput);
      ResourceLocation $$4 = ModelTemplates.ORIENTABLE_TRAPDOOR_OPEN.create(p_124917_, $$1, this.modelOutput);
      this.blockStateOutput.accept(createOrientableTrapdoor(p_124917_, $$2, $$3, $$4));
      this.delegateItemModel(p_124917_, $$3);
   }

   void createTrapdoor(Block p_124937_) {
      TextureMapping $$1 = TextureMapping.defaultTexture(p_124937_);
      ResourceLocation $$2 = ModelTemplates.TRAPDOOR_TOP.create(p_124937_, $$1, this.modelOutput);
      ResourceLocation $$3 = ModelTemplates.TRAPDOOR_BOTTOM.create(p_124937_, $$1, this.modelOutput);
      ResourceLocation $$4 = ModelTemplates.TRAPDOOR_OPEN.create(p_124937_, $$1, this.modelOutput);
      this.blockStateOutput.accept(createTrapdoor(p_124937_, $$2, $$3, $$4));
      this.delegateItemModel(p_124937_, $$3);
   }

   private void copyTrapdoorModel(Block p_309079_, Block p_309124_) {
      ResourceLocation $$2 = ModelTemplates.TRAPDOOR_TOP.getDefaultModelLocation(p_309079_);
      ResourceLocation $$3 = ModelTemplates.TRAPDOOR_BOTTOM.getDefaultModelLocation(p_309079_);
      ResourceLocation $$4 = ModelTemplates.TRAPDOOR_OPEN.getDefaultModelLocation(p_309079_);
      this.delegateItemModel(p_309124_, ModelLocationUtils.getModelLocation(p_309079_.asItem()));
      this.blockStateOutput.accept(createTrapdoor(p_309124_, $$2, $$3, $$4));
   }

   private void createBigDripLeafBlock() {
      this.skipAutoItemBlock(Blocks.BIG_DRIPLEAF);
      ResourceLocation $$0 = ModelLocationUtils.getModelLocation(Blocks.BIG_DRIPLEAF);
      ResourceLocation $$1 = ModelLocationUtils.getModelLocation(Blocks.BIG_DRIPLEAF, "_partial_tilt");
      ResourceLocation $$2 = ModelLocationUtils.getModelLocation(Blocks.BIG_DRIPLEAF, "_full_tilt");
      this.blockStateOutput
         .accept(
            MultiVariantGenerator.multiVariant(Blocks.BIG_DRIPLEAF)
               .with(createHorizontalFacingDispatch())
               .with(
                  PropertyDispatch.property(BlockStateProperties.TILT)
                     .select(Tilt.NONE, Variant.variant().with(VariantProperties.MODEL, $$0))
                     .select(Tilt.UNSTABLE, Variant.variant().with(VariantProperties.MODEL, $$0))
                     .select(Tilt.PARTIAL, Variant.variant().with(VariantProperties.MODEL, $$1))
                     .select(Tilt.FULL, Variant.variant().with(VariantProperties.MODEL, $$2))
               )
         );
   }

   private BlockModelGenerators.WoodProvider woodProvider(Block p_124949_) {
      return new BlockModelGenerators.WoodProvider(TextureMapping.logColumn(p_124949_));
   }

   private void createNonTemplateModelBlock(Block p_124961_) {
      this.createNonTemplateModelBlock(p_124961_, p_124961_);
   }

   private void createNonTemplateModelBlock(Block p_124534_, Block p_124535_) {
      this.blockStateOutput.accept(createSimpleBlock(p_124534_, ModelLocationUtils.getModelLocation(p_124535_)));
   }

   private void createCrossBlockWithDefaultItem(Block p_124558_, BlockModelGenerators.TintState p_124559_) {
      this.createSimpleFlatItemModel(p_124558_);
      this.createCrossBlock(p_124558_, p_124559_);
   }

   private void createCrossBlockWithDefaultItem(Block p_124561_, BlockModelGenerators.TintState p_124562_, TextureMapping p_124563_) {
      this.createSimpleFlatItemModel(p_124561_);
      this.createCrossBlock(p_124561_, p_124562_, p_124563_);
   }

   private void createCrossBlock(Block p_124738_, BlockModelGenerators.TintState p_124739_) {
      TextureMapping $$2 = TextureMapping.cross(p_124738_);
      this.createCrossBlock(p_124738_, p_124739_, $$2);
   }

   private void createCrossBlock(Block p_124741_, BlockModelGenerators.TintState p_124742_, TextureMapping p_124743_) {
      ResourceLocation $$3 = p_124742_.getCross().create(p_124741_, p_124743_, this.modelOutput);
      this.blockStateOutput.accept(createSimpleBlock(p_124741_, $$3));
   }

   private void createCrossBlock(Block p_273533_, BlockModelGenerators.TintState p_273521_, Property<Integer> p_273430_, int... p_273001_) {
      if (p_273430_.getPossibleValues().size() != p_273001_.length) {
         throw new IllegalArgumentException("missing values for property: " + p_273430_);
      } else {
         PropertyDispatch $$4 = PropertyDispatch.property(p_273430_).generate(p_272381_ -> {
            String $$4x = "_stage" + p_273001_[p_272381_];
            TextureMapping $$5 = TextureMapping.cross(TextureMapping.getBlockTexture(p_273533_, $$4x));
            ResourceLocation $$6 = p_273521_.getCross().createWithSuffix(p_273533_, $$4x, $$5, this.modelOutput);
            return Variant.variant().with(VariantProperties.MODEL, $$6);
         });
         this.createSimpleFlatItemModel(p_273533_.asItem());
         this.blockStateOutput.accept(MultiVariantGenerator.multiVariant(p_273533_).with($$4));
      }
   }

   private void createPlant(Block p_124546_, Block p_124547_, BlockModelGenerators.TintState p_124548_) {
      this.createCrossBlockWithDefaultItem(p_124546_, p_124548_);
      TextureMapping $$3 = TextureMapping.plant(p_124546_);
      ResourceLocation $$4 = p_124548_.getCrossPot().create(p_124547_, $$3, this.modelOutput);
      this.blockStateOutput.accept(createSimpleBlock(p_124547_, $$4));
   }

   private void createCoralFans(Block p_124731_, Block p_124732_) {
      TexturedModel $$2 = TexturedModel.CORAL_FAN.get(p_124731_);
      ResourceLocation $$3 = $$2.create(p_124731_, this.modelOutput);
      this.blockStateOutput.accept(createSimpleBlock(p_124731_, $$3));
      ResourceLocation $$4 = ModelTemplates.CORAL_WALL_FAN.create(p_124732_, $$2.getMapping(), this.modelOutput);
      this.blockStateOutput
         .accept(MultiVariantGenerator.multiVariant(p_124732_, Variant.variant().with(VariantProperties.MODEL, $$4)).with(createHorizontalFacingDispatch()));
      this.createSimpleFlatItemModel(p_124731_);
   }

   private void createStems(Block p_124789_, Block p_124790_) {
      this.createSimpleFlatItemModel(p_124789_.asItem());
      TextureMapping $$2 = TextureMapping.stem(p_124789_);
      TextureMapping $$3 = TextureMapping.attachedStem(p_124789_, p_124790_);
      ResourceLocation $$4 = ModelTemplates.ATTACHED_STEM.create(p_124790_, $$3, this.modelOutput);
      this.blockStateOutput
         .accept(
            MultiVariantGenerator.multiVariant(p_124790_, Variant.variant().with(VariantProperties.MODEL, $$4))
               .with(
                  PropertyDispatch.property(BlockStateProperties.HORIZONTAL_FACING)
                     .select(Direction.WEST, Variant.variant())
                     .select(Direction.SOUTH, Variant.variant().with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270))
                     .select(Direction.NORTH, Variant.variant().with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90))
                     .select(Direction.EAST, Variant.variant().with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180))
               )
         );
      this.blockStateOutput
         .accept(
            MultiVariantGenerator.multiVariant(p_124789_)
               .with(
                  PropertyDispatch.property(BlockStateProperties.AGE_7)
                     .generate(
                        p_176108_ -> Variant.variant().with(VariantProperties.MODEL, ModelTemplates.STEMS[p_176108_].create(p_124789_, $$2, this.modelOutput))
                     )
               )
         );
   }

   private void createPitcherPlant() {
      Block $$0 = Blocks.PITCHER_PLANT;
      this.createSimpleFlatItemModel($$0.asItem());
      ResourceLocation $$1 = ModelLocationUtils.getModelLocation($$0, "_top");
      ResourceLocation $$2 = ModelLocationUtils.getModelLocation($$0, "_bottom");
      this.createDoubleBlock($$0, $$1, $$2);
   }

   private void createPitcherCrop() {
      Block $$0 = Blocks.PITCHER_CROP;
      this.createSimpleFlatItemModel($$0.asItem());
      PropertyDispatch $$1 = PropertyDispatch.properties(PitcherCropBlock.AGE, BlockStateProperties.DOUBLE_BLOCK_HALF).generate((p_339371_, p_339372_) -> {
         return switch (p_339372_) {
            case UPPER -> Variant.variant().with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation($$0, "_top_stage_" + p_339371_));
            case LOWER -> Variant.variant().with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation($$0, "_bottom_stage_" + p_339371_));
         };
      });
      this.blockStateOutput.accept(MultiVariantGenerator.multiVariant($$0).with($$1));
   }

   private void createCoral(
      Block p_124537_, Block p_124538_, Block p_124539_, Block p_124540_, Block p_124541_, Block p_124542_, Block p_124543_, Block p_124544_
   ) {
      this.createCrossBlockWithDefaultItem(p_124537_, BlockModelGenerators.TintState.NOT_TINTED);
      this.createCrossBlockWithDefaultItem(p_124538_, BlockModelGenerators.TintState.NOT_TINTED);
      this.createTrivialCube(p_124539_);
      this.createTrivialCube(p_124540_);
      this.createCoralFans(p_124541_, p_124543_);
      this.createCoralFans(p_124542_, p_124544_);
   }

   private void createDoublePlant(Block p_124792_, BlockModelGenerators.TintState p_124793_) {
      this.createSimpleFlatItemModel(p_124792_, "_top");
      ResourceLocation $$2 = this.createSuffixedVariant(p_124792_, "_top", p_124793_.getCross(), TextureMapping::cross);
      ResourceLocation $$3 = this.createSuffixedVariant(p_124792_, "_bottom", p_124793_.getCross(), TextureMapping::cross);
      this.createDoubleBlock(p_124792_, $$2, $$3);
   }

   private void createSunflower() {
      this.createSimpleFlatItemModel(Blocks.SUNFLOWER, "_front");
      ResourceLocation $$0 = ModelLocationUtils.getModelLocation(Blocks.SUNFLOWER, "_top");
      ResourceLocation $$1 = this.createSuffixedVariant(
         Blocks.SUNFLOWER, "_bottom", BlockModelGenerators.TintState.NOT_TINTED.getCross(), TextureMapping::cross
      );
      this.createDoubleBlock(Blocks.SUNFLOWER, $$0, $$1);
   }

   private void createTallSeagrass() {
      ResourceLocation $$0 = this.createSuffixedVariant(Blocks.TALL_SEAGRASS, "_top", ModelTemplates.SEAGRASS, TextureMapping::defaultTexture);
      ResourceLocation $$1 = this.createSuffixedVariant(Blocks.TALL_SEAGRASS, "_bottom", ModelTemplates.SEAGRASS, TextureMapping::defaultTexture);
      this.createDoubleBlock(Blocks.TALL_SEAGRASS, $$0, $$1);
   }

   private void createSmallDripleaf() {
      this.skipAutoItemBlock(Blocks.SMALL_DRIPLEAF);
      ResourceLocation $$0 = ModelLocationUtils.getModelLocation(Blocks.SMALL_DRIPLEAF, "_top");
      ResourceLocation $$1 = ModelLocationUtils.getModelLocation(Blocks.SMALL_DRIPLEAF, "_bottom");
      this.blockStateOutput
         .accept(
            MultiVariantGenerator.multiVariant(Blocks.SMALL_DRIPLEAF)
               .with(createHorizontalFacingDispatch())
               .with(
                  PropertyDispatch.property(BlockStateProperties.DOUBLE_BLOCK_HALF)
                     .select(DoubleBlockHalf.LOWER, Variant.variant().with(VariantProperties.MODEL, $$1))
                     .select(DoubleBlockHalf.UPPER, Variant.variant().with(VariantProperties.MODEL, $$0))
               )
         );
   }

   private void createDoubleBlock(Block p_124954_, ResourceLocation p_124955_, ResourceLocation p_124956_) {
      this.blockStateOutput
         .accept(
            MultiVariantGenerator.multiVariant(p_124954_)
               .with(
                  PropertyDispatch.property(BlockStateProperties.DOUBLE_BLOCK_HALF)
                     .select(DoubleBlockHalf.LOWER, Variant.variant().with(VariantProperties.MODEL, p_124956_))
                     .select(DoubleBlockHalf.UPPER, Variant.variant().with(VariantProperties.MODEL, p_124955_))
               )
         );
   }

   private void createPassiveRail(Block p_124969_) {
      TextureMapping $$1 = TextureMapping.rail(p_124969_);
      TextureMapping $$2 = TextureMapping.rail(TextureMapping.getBlockTexture(p_124969_, "_corner"));
      ResourceLocation $$3 = ModelTemplates.RAIL_FLAT.create(p_124969_, $$1, this.modelOutput);
      ResourceLocation $$4 = ModelTemplates.RAIL_CURVED.create(p_124969_, $$2, this.modelOutput);
      ResourceLocation $$5 = ModelTemplates.RAIL_RAISED_NE.create(p_124969_, $$1, this.modelOutput);
      ResourceLocation $$6 = ModelTemplates.RAIL_RAISED_SW.create(p_124969_, $$1, this.modelOutput);
      this.createSimpleFlatItemModel(p_124969_);
      this.blockStateOutput
         .accept(
            MultiVariantGenerator.multiVariant(p_124969_)
               .with(
                  PropertyDispatch.property(BlockStateProperties.RAIL_SHAPE)
                     .select(RailShape.NORTH_SOUTH, Variant.variant().with(VariantProperties.MODEL, $$3))
                     .select(
                        RailShape.EAST_WEST, Variant.variant().with(VariantProperties.MODEL, $$3).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)
                     )
                     .select(
                        RailShape.ASCENDING_EAST,
                        Variant.variant().with(VariantProperties.MODEL, $$5).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)
                     )
                     .select(
                        RailShape.ASCENDING_WEST,
                        Variant.variant().with(VariantProperties.MODEL, $$6).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)
                     )
                     .select(RailShape.ASCENDING_NORTH, Variant.variant().with(VariantProperties.MODEL, $$5))
                     .select(RailShape.ASCENDING_SOUTH, Variant.variant().with(VariantProperties.MODEL, $$6))
                     .select(RailShape.SOUTH_EAST, Variant.variant().with(VariantProperties.MODEL, $$4))
                     .select(
                        RailShape.SOUTH_WEST,
                        Variant.variant().with(VariantProperties.MODEL, $$4).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)
                     )
                     .select(
                        RailShape.NORTH_WEST,
                        Variant.variant().with(VariantProperties.MODEL, $$4).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180)
                     )
                     .select(
                        RailShape.NORTH_EAST,
                        Variant.variant().with(VariantProperties.MODEL, $$4).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)
                     )
               )
         );
   }

   private void createActiveRail(Block p_124975_) {
      ResourceLocation $$1 = this.createSuffixedVariant(p_124975_, "", ModelTemplates.RAIL_FLAT, TextureMapping::rail);
      ResourceLocation $$2 = this.createSuffixedVariant(p_124975_, "", ModelTemplates.RAIL_RAISED_NE, TextureMapping::rail);
      ResourceLocation $$3 = this.createSuffixedVariant(p_124975_, "", ModelTemplates.RAIL_RAISED_SW, TextureMapping::rail);
      ResourceLocation $$4 = this.createSuffixedVariant(p_124975_, "_on", ModelTemplates.RAIL_FLAT, TextureMapping::rail);
      ResourceLocation $$5 = this.createSuffixedVariant(p_124975_, "_on", ModelTemplates.RAIL_RAISED_NE, TextureMapping::rail);
      ResourceLocation $$6 = this.createSuffixedVariant(p_124975_, "_on", ModelTemplates.RAIL_RAISED_SW, TextureMapping::rail);
      PropertyDispatch $$7 = PropertyDispatch.properties(BlockStateProperties.POWERED, BlockStateProperties.RAIL_SHAPE_STRAIGHT)
         .generate((p_176166_, p_176167_) -> {
            switch (p_176167_) {
               case NORTH_SOUTH:
                  return Variant.variant().with(VariantProperties.MODEL, p_176166_ ? $$4 : $$1);
               case EAST_WEST:
                  return Variant.variant().with(VariantProperties.MODEL, p_176166_ ? $$4 : $$1).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90);
               case ASCENDING_EAST:
                  return Variant.variant().with(VariantProperties.MODEL, p_176166_ ? $$5 : $$2).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90);
               case ASCENDING_WEST:
                  return Variant.variant().with(VariantProperties.MODEL, p_176166_ ? $$6 : $$3).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90);
               case ASCENDING_NORTH:
                  return Variant.variant().with(VariantProperties.MODEL, p_176166_ ? $$5 : $$2);
               case ASCENDING_SOUTH:
                  return Variant.variant().with(VariantProperties.MODEL, p_176166_ ? $$6 : $$3);
               default:
                  throw new UnsupportedOperationException("Fix you generator!");
            }
         });
      this.createSimpleFlatItemModel(p_124975_);
      this.blockStateOutput.accept(MultiVariantGenerator.multiVariant(p_124975_).with($$7));
   }

   private BlockModelGenerators.BlockEntityModelGenerator blockEntityModels(ResourceLocation p_124691_, Block p_124692_) {
      return new BlockModelGenerators.BlockEntityModelGenerator(p_124691_, p_124692_);
   }

   private BlockModelGenerators.BlockEntityModelGenerator blockEntityModels(Block p_124826_, Block p_124827_) {
      return new BlockModelGenerators.BlockEntityModelGenerator(ModelLocationUtils.getModelLocation(p_124826_), p_124827_);
   }

   private void createAirLikeBlock(Block p_124531_, Item p_124532_) {
      ResourceLocation $$2 = ModelTemplates.PARTICLE_ONLY.create(p_124531_, TextureMapping.particleFromItem(p_124532_), this.modelOutput);
      this.blockStateOutput.accept(createSimpleBlock(p_124531_, $$2));
   }

   private void createAirLikeBlock(Block p_124922_, ResourceLocation p_124923_) {
      ResourceLocation $$2 = ModelTemplates.PARTICLE_ONLY.create(p_124922_, TextureMapping.particle(p_124923_), this.modelOutput);
      this.blockStateOutput.accept(createSimpleBlock(p_124922_, $$2));
   }

   private void createFullAndCarpetBlocks(Block p_176218_, Block p_176219_) {
      this.createTrivialCube(p_176218_);
      ResourceLocation $$2 = TexturedModel.CARPET.get(p_176218_).create(p_176219_, this.modelOutput);
      this.blockStateOutput.accept(createSimpleBlock(p_176219_, $$2));
   }

   private void createFlowerBed(Block p_273441_) {
      this.createSimpleFlatItemModel(p_273441_.asItem());
      ResourceLocation $$1 = TexturedModel.FLOWERBED_1.create(p_273441_, this.modelOutput);
      ResourceLocation $$2 = TexturedModel.FLOWERBED_2.create(p_273441_, this.modelOutput);
      ResourceLocation $$3 = TexturedModel.FLOWERBED_3.create(p_273441_, this.modelOutput);
      ResourceLocation $$4 = TexturedModel.FLOWERBED_4.create(p_273441_, this.modelOutput);
      this.blockStateOutput
         .accept(
            MultiPartGenerator.multiPart(p_273441_)
               .with(
                  Condition.condition().term(BlockStateProperties.FLOWER_AMOUNT, 1, 2, 3, 4).term(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH),
                  Variant.variant().with(VariantProperties.MODEL, $$1)
               )
               .with(
                  Condition.condition().term(BlockStateProperties.FLOWER_AMOUNT, 1, 2, 3, 4).term(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST),
                  Variant.variant().with(VariantProperties.MODEL, $$1).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)
               )
               .with(
                  Condition.condition().term(BlockStateProperties.FLOWER_AMOUNT, 1, 2, 3, 4).term(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH),
                  Variant.variant().with(VariantProperties.MODEL, $$1).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180)
               )
               .with(
                  Condition.condition().term(BlockStateProperties.FLOWER_AMOUNT, 1, 2, 3, 4).term(BlockStateProperties.HORIZONTAL_FACING, Direction.WEST),
                  Variant.variant().with(VariantProperties.MODEL, $$1).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)
               )
               .with(
                  Condition.condition().term(BlockStateProperties.FLOWER_AMOUNT, 2, 3, 4).term(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH),
                  Variant.variant().with(VariantProperties.MODEL, $$2)
               )
               .with(
                  Condition.condition().term(BlockStateProperties.FLOWER_AMOUNT, 2, 3, 4).term(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST),
                  Variant.variant().with(VariantProperties.MODEL, $$2).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)
               )
               .with(
                  Condition.condition().term(BlockStateProperties.FLOWER_AMOUNT, 2, 3, 4).term(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH),
                  Variant.variant().with(VariantProperties.MODEL, $$2).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180)
               )
               .with(
                  Condition.condition().term(BlockStateProperties.FLOWER_AMOUNT, 2, 3, 4).term(BlockStateProperties.HORIZONTAL_FACING, Direction.WEST),
                  Variant.variant().with(VariantProperties.MODEL, $$2).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)
               )
               .with(
                  Condition.condition().term(BlockStateProperties.FLOWER_AMOUNT, 3, 4).term(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH),
                  Variant.variant().with(VariantProperties.MODEL, $$3)
               )
               .with(
                  Condition.condition().term(BlockStateProperties.FLOWER_AMOUNT, 3, 4).term(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST),
                  Variant.variant().with(VariantProperties.MODEL, $$3).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)
               )
               .with(
                  Condition.condition().term(BlockStateProperties.FLOWER_AMOUNT, 3, 4).term(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH),
                  Variant.variant().with(VariantProperties.MODEL, $$3).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180)
               )
               .with(
                  Condition.condition().term(BlockStateProperties.FLOWER_AMOUNT, 3, 4).term(BlockStateProperties.HORIZONTAL_FACING, Direction.WEST),
                  Variant.variant().with(VariantProperties.MODEL, $$3).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)
               )
               .with(
                  Condition.condition().term(BlockStateProperties.FLOWER_AMOUNT, 4).term(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH),
                  Variant.variant().with(VariantProperties.MODEL, $$4)
               )
               .with(
                  Condition.condition().term(BlockStateProperties.FLOWER_AMOUNT, 4).term(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST),
                  Variant.variant().with(VariantProperties.MODEL, $$4).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)
               )
               .with(
                  Condition.condition().term(BlockStateProperties.FLOWER_AMOUNT, 4).term(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH),
                  Variant.variant().with(VariantProperties.MODEL, $$4).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180)
               )
               .with(
                  Condition.condition().term(BlockStateProperties.FLOWER_AMOUNT, 4).term(BlockStateProperties.HORIZONTAL_FACING, Direction.WEST),
                  Variant.variant().with(VariantProperties.MODEL, $$4).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)
               )
         );
   }

   private void createColoredBlockWithRandomRotations(TexturedModel.Provider p_124686_, Block... p_124687_) {
      for (Block $$2 : p_124687_) {
         ResourceLocation $$3 = p_124686_.create($$2, this.modelOutput);
         this.blockStateOutput.accept(createRotatedVariant($$2, $$3));
      }
   }

   private void createColoredBlockWithStateRotations(TexturedModel.Provider p_124778_, Block... p_124779_) {
      for (Block $$2 : p_124779_) {
         ResourceLocation $$3 = p_124778_.create($$2, this.modelOutput);
         this.blockStateOutput
            .accept(MultiVariantGenerator.multiVariant($$2, Variant.variant().with(VariantProperties.MODEL, $$3)).with(createHorizontalFacingDispatchAlt()));
      }
   }

   private void createGlassBlocks(Block p_124879_, Block p_124880_) {
      this.createTrivialCube(p_124879_);
      TextureMapping $$2 = TextureMapping.pane(p_124879_, p_124880_);
      ResourceLocation $$3 = ModelTemplates.STAINED_GLASS_PANE_POST.create(p_124880_, $$2, this.modelOutput);
      ResourceLocation $$4 = ModelTemplates.STAINED_GLASS_PANE_SIDE.create(p_124880_, $$2, this.modelOutput);
      ResourceLocation $$5 = ModelTemplates.STAINED_GLASS_PANE_SIDE_ALT.create(p_124880_, $$2, this.modelOutput);
      ResourceLocation $$6 = ModelTemplates.STAINED_GLASS_PANE_NOSIDE.create(p_124880_, $$2, this.modelOutput);
      ResourceLocation $$7 = ModelTemplates.STAINED_GLASS_PANE_NOSIDE_ALT.create(p_124880_, $$2, this.modelOutput);
      Item $$8 = p_124880_.asItem();
      ModelTemplates.FLAT_ITEM.create(ModelLocationUtils.getModelLocation($$8), TextureMapping.layer0(p_124879_), this.modelOutput);
      this.blockStateOutput
         .accept(
            MultiPartGenerator.multiPart(p_124880_)
               .with(Variant.variant().with(VariantProperties.MODEL, $$3))
               .with(Condition.condition().term(BlockStateProperties.NORTH, true), Variant.variant().with(VariantProperties.MODEL, $$4))
               .with(
                  Condition.condition().term(BlockStateProperties.EAST, true),
                  Variant.variant().with(VariantProperties.MODEL, $$4).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)
               )
               .with(Condition.condition().term(BlockStateProperties.SOUTH, true), Variant.variant().with(VariantProperties.MODEL, $$5))
               .with(
                  Condition.condition().term(BlockStateProperties.WEST, true),
                  Variant.variant().with(VariantProperties.MODEL, $$5).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)
               )
               .with(Condition.condition().term(BlockStateProperties.NORTH, false), Variant.variant().with(VariantProperties.MODEL, $$6))
               .with(Condition.condition().term(BlockStateProperties.EAST, false), Variant.variant().with(VariantProperties.MODEL, $$7))
               .with(
                  Condition.condition().term(BlockStateProperties.SOUTH, false),
                  Variant.variant().with(VariantProperties.MODEL, $$7).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)
               )
               .with(
                  Condition.condition().term(BlockStateProperties.WEST, false),
                  Variant.variant().with(VariantProperties.MODEL, $$6).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)
               )
         );
   }

   private void createCommandBlock(Block p_124978_) {
      TextureMapping $$1 = TextureMapping.commandBlock(p_124978_);
      ResourceLocation $$2 = ModelTemplates.COMMAND_BLOCK.create(p_124978_, $$1, this.modelOutput);
      ResourceLocation $$3 = this.createSuffixedVariant(
         p_124978_, "_conditional", ModelTemplates.COMMAND_BLOCK, p_176193_ -> $$1.copyAndUpdate(TextureSlot.SIDE, p_176193_)
      );
      this.blockStateOutput
         .accept(
            MultiVariantGenerator.multiVariant(p_124978_)
               .with(createBooleanModelDispatch(BlockStateProperties.CONDITIONAL, $$3, $$2))
               .with(createFacingDispatch())
         );
   }

   private void createAnvil(Block p_124981_) {
      ResourceLocation $$1 = TexturedModel.ANVIL.create(p_124981_, this.modelOutput);
      this.blockStateOutput.accept(createSimpleBlock(p_124981_, $$1).with(createHorizontalFacingDispatchAlt()));
   }

   private List<Variant> createBambooModels(int p_124512_) {
      String $$1 = "_age" + p_124512_;
      return IntStream.range(1, 5)
         .mapToObj(p_176139_ -> Variant.variant().with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(Blocks.BAMBOO, p_176139_ + $$1)))
         .collect(Collectors.toList());
   }

   private void createBamboo() {
      this.skipAutoItemBlock(Blocks.BAMBOO);
      this.blockStateOutput
         .accept(
            MultiPartGenerator.multiPart(Blocks.BAMBOO)
               .with(Condition.condition().term(BlockStateProperties.AGE_1, 0), this.createBambooModels(0))
               .with(Condition.condition().term(BlockStateProperties.AGE_1, 1), this.createBambooModels(1))
               .with(
                  Condition.condition().term(BlockStateProperties.BAMBOO_LEAVES, BambooLeaves.SMALL),
                  Variant.variant().with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(Blocks.BAMBOO, "_small_leaves"))
               )
               .with(
                  Condition.condition().term(BlockStateProperties.BAMBOO_LEAVES, BambooLeaves.LARGE),
                  Variant.variant().with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(Blocks.BAMBOO, "_large_leaves"))
               )
         );
   }

   private PropertyDispatch createColumnWithFacing() {
      return PropertyDispatch.property(BlockStateProperties.FACING)
         .select(Direction.DOWN, Variant.variant().with(VariantProperties.X_ROT, VariantProperties.Rotation.R180))
         .select(Direction.UP, Variant.variant())
         .select(Direction.NORTH, Variant.variant().with(VariantProperties.X_ROT, VariantProperties.Rotation.R90))
         .select(
            Direction.SOUTH,
            Variant.variant().with(VariantProperties.X_ROT, VariantProperties.Rotation.R90).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180)
         )
         .select(
            Direction.WEST,
            Variant.variant().with(VariantProperties.X_ROT, VariantProperties.Rotation.R90).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)
         )
         .select(
            Direction.EAST,
            Variant.variant().with(VariantProperties.X_ROT, VariantProperties.Rotation.R90).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)
         );
   }

   private void createBarrel() {
      ResourceLocation $$0 = TextureMapping.getBlockTexture(Blocks.BARREL, "_top_open");
      this.blockStateOutput
         .accept(
            MultiVariantGenerator.multiVariant(Blocks.BARREL)
               .with(this.createColumnWithFacing())
               .with(
                  PropertyDispatch.property(BlockStateProperties.OPEN)
                     .select(false, Variant.variant().with(VariantProperties.MODEL, TexturedModel.CUBE_TOP_BOTTOM.create(Blocks.BARREL, this.modelOutput)))
                     .select(
                        true,
                        Variant.variant()
                           .with(
                              VariantProperties.MODEL,
                              TexturedModel.CUBE_TOP_BOTTOM
                                 .get(Blocks.BARREL)
                                 .updateTextures(p_176216_ -> p_176216_.put(TextureSlot.TOP, $$0))
                                 .createWithSuffix(Blocks.BARREL, "_open", this.modelOutput)
                           )
                     )
               )
         );
   }

   private static <T extends Comparable<T>> PropertyDispatch createEmptyOrFullDispatch(
      Property<T> p_124627_, T p_124628_, ResourceLocation p_124629_, ResourceLocation p_124630_
   ) {
      Variant $$4 = Variant.variant().with(VariantProperties.MODEL, p_124629_);
      Variant $$5 = Variant.variant().with(VariantProperties.MODEL, p_124630_);
      return PropertyDispatch.property(p_124627_).generate(p_176130_ -> {
         boolean $$4x = p_176130_.compareTo(p_124628_) >= 0;
         return $$4x ? $$4 : $$5;
      });
   }

   private void createBeeNest(Block p_124584_, Function<Block, TextureMapping> p_124585_) {
      TextureMapping $$2 = p_124585_.apply(p_124584_).copyForced(TextureSlot.SIDE, TextureSlot.PARTICLE);
      TextureMapping $$3 = $$2.copyAndUpdate(TextureSlot.FRONT, TextureMapping.getBlockTexture(p_124584_, "_front_honey"));
      ResourceLocation $$4 = ModelTemplates.CUBE_ORIENTABLE_TOP_BOTTOM.create(p_124584_, $$2, this.modelOutput);
      ResourceLocation $$5 = ModelTemplates.CUBE_ORIENTABLE_TOP_BOTTOM.createWithSuffix(p_124584_, "_honey", $$3, this.modelOutput);
      this.blockStateOutput
         .accept(
            MultiVariantGenerator.multiVariant(p_124584_)
               .with(createHorizontalFacingDispatch())
               .with(createEmptyOrFullDispatch(BlockStateProperties.LEVEL_HONEY, 5, $$5, $$4))
         );
   }

   private void createCropBlock(Block p_124554_, Property<Integer> p_124555_, int... p_124556_) {
      if (p_124555_.getPossibleValues().size() != p_124556_.length) {
         throw new IllegalArgumentException();
      } else {
         Int2ObjectMap<ResourceLocation> $$3 = new Int2ObjectOpenHashMap();
         PropertyDispatch $$4 = PropertyDispatch.property(p_124555_)
            .generate(
               p_176172_ -> {
                  int $$4x = p_124556_[p_176172_];
                  ResourceLocation $$5 = (ResourceLocation)$$3.computeIfAbsent(
                     $$4x, p_176098_ -> this.createSuffixedVariant(p_124554_, "_stage" + $$4x, ModelTemplates.CROP, TextureMapping::crop)
                  );
                  return Variant.variant().with(VariantProperties.MODEL, $$5);
               }
            );
         this.createSimpleFlatItemModel(p_124554_.asItem());
         this.blockStateOutput.accept(MultiVariantGenerator.multiVariant(p_124554_).with($$4));
      }
   }

   private void createBell() {
      ResourceLocation $$0 = ModelLocationUtils.getModelLocation(Blocks.BELL, "_floor");
      ResourceLocation $$1 = ModelLocationUtils.getModelLocation(Blocks.BELL, "_ceiling");
      ResourceLocation $$2 = ModelLocationUtils.getModelLocation(Blocks.BELL, "_wall");
      ResourceLocation $$3 = ModelLocationUtils.getModelLocation(Blocks.BELL, "_between_walls");
      this.createSimpleFlatItemModel(Items.BELL);
      this.blockStateOutput
         .accept(
            MultiVariantGenerator.multiVariant(Blocks.BELL)
               .with(
                  PropertyDispatch.properties(BlockStateProperties.HORIZONTAL_FACING, BlockStateProperties.BELL_ATTACHMENT)
                     .select(Direction.NORTH, BellAttachType.FLOOR, Variant.variant().with(VariantProperties.MODEL, $$0))
                     .select(
                        Direction.SOUTH,
                        BellAttachType.FLOOR,
                        Variant.variant().with(VariantProperties.MODEL, $$0).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180)
                     )
                     .select(
                        Direction.EAST,
                        BellAttachType.FLOOR,
                        Variant.variant().with(VariantProperties.MODEL, $$0).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)
                     )
                     .select(
                        Direction.WEST,
                        BellAttachType.FLOOR,
                        Variant.variant().with(VariantProperties.MODEL, $$0).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)
                     )
                     .select(Direction.NORTH, BellAttachType.CEILING, Variant.variant().with(VariantProperties.MODEL, $$1))
                     .select(
                        Direction.SOUTH,
                        BellAttachType.CEILING,
                        Variant.variant().with(VariantProperties.MODEL, $$1).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180)
                     )
                     .select(
                        Direction.EAST,
                        BellAttachType.CEILING,
                        Variant.variant().with(VariantProperties.MODEL, $$1).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)
                     )
                     .select(
                        Direction.WEST,
                        BellAttachType.CEILING,
                        Variant.variant().with(VariantProperties.MODEL, $$1).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)
                     )
                     .select(
                        Direction.NORTH,
                        BellAttachType.SINGLE_WALL,
                        Variant.variant().with(VariantProperties.MODEL, $$2).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)
                     )
                     .select(
                        Direction.SOUTH,
                        BellAttachType.SINGLE_WALL,
                        Variant.variant().with(VariantProperties.MODEL, $$2).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)
                     )
                     .select(Direction.EAST, BellAttachType.SINGLE_WALL, Variant.variant().with(VariantProperties.MODEL, $$2))
                     .select(
                        Direction.WEST,
                        BellAttachType.SINGLE_WALL,
                        Variant.variant().with(VariantProperties.MODEL, $$2).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180)
                     )
                     .select(
                        Direction.SOUTH,
                        BellAttachType.DOUBLE_WALL,
                        Variant.variant().with(VariantProperties.MODEL, $$3).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)
                     )
                     .select(
                        Direction.NORTH,
                        BellAttachType.DOUBLE_WALL,
                        Variant.variant().with(VariantProperties.MODEL, $$3).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)
                     )
                     .select(Direction.EAST, BellAttachType.DOUBLE_WALL, Variant.variant().with(VariantProperties.MODEL, $$3))
                     .select(
                        Direction.WEST,
                        BellAttachType.DOUBLE_WALL,
                        Variant.variant().with(VariantProperties.MODEL, $$3).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180)
                     )
               )
         );
   }

   private void createGrindstone() {
      this.blockStateOutput
         .accept(
            MultiVariantGenerator.multiVariant(
                  Blocks.GRINDSTONE, Variant.variant().with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(Blocks.GRINDSTONE))
               )
               .with(
                  PropertyDispatch.properties(BlockStateProperties.ATTACH_FACE, BlockStateProperties.HORIZONTAL_FACING)
                     .select(AttachFace.FLOOR, Direction.NORTH, Variant.variant())
                     .select(AttachFace.FLOOR, Direction.EAST, Variant.variant().with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90))
                     .select(AttachFace.FLOOR, Direction.SOUTH, Variant.variant().with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180))
                     .select(AttachFace.FLOOR, Direction.WEST, Variant.variant().with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270))
                     .select(AttachFace.WALL, Direction.NORTH, Variant.variant().with(VariantProperties.X_ROT, VariantProperties.Rotation.R90))
                     .select(
                        AttachFace.WALL,
                        Direction.EAST,
                        Variant.variant()
                           .with(VariantProperties.X_ROT, VariantProperties.Rotation.R90)
                           .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)
                     )
                     .select(
                        AttachFace.WALL,
                        Direction.SOUTH,
                        Variant.variant()
                           .with(VariantProperties.X_ROT, VariantProperties.Rotation.R90)
                           .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180)
                     )
                     .select(
                        AttachFace.WALL,
                        Direction.WEST,
                        Variant.variant()
                           .with(VariantProperties.X_ROT, VariantProperties.Rotation.R90)
                           .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)
                     )
                     .select(AttachFace.CEILING, Direction.SOUTH, Variant.variant().with(VariantProperties.X_ROT, VariantProperties.Rotation.R180))
                     .select(
                        AttachFace.CEILING,
                        Direction.WEST,
                        Variant.variant()
                           .with(VariantProperties.X_ROT, VariantProperties.Rotation.R180)
                           .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)
                     )
                     .select(
                        AttachFace.CEILING,
                        Direction.NORTH,
                        Variant.variant()
                           .with(VariantProperties.X_ROT, VariantProperties.Rotation.R180)
                           .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180)
                     )
                     .select(
                        AttachFace.CEILING,
                        Direction.EAST,
                        Variant.variant()
                           .with(VariantProperties.X_ROT, VariantProperties.Rotation.R180)
                           .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)
                     )
               )
         );
   }

   private void createFurnace(Block p_124857_, TexturedModel.Provider p_124858_) {
      ResourceLocation $$2 = p_124858_.create(p_124857_, this.modelOutput);
      ResourceLocation $$3 = TextureMapping.getBlockTexture(p_124857_, "_front_on");
      ResourceLocation $$4 = p_124858_.get(p_124857_)
         .updateTextures(p_176207_ -> p_176207_.put(TextureSlot.FRONT, $$3))
         .createWithSuffix(p_124857_, "_on", this.modelOutput);
      this.blockStateOutput
         .accept(
            MultiVariantGenerator.multiVariant(p_124857_)
               .with(createBooleanModelDispatch(BlockStateProperties.LIT, $$4, $$2))
               .with(createHorizontalFacingDispatch())
         );
   }

   private void createCampfires(Block... p_124714_) {
      ResourceLocation $$1 = ModelLocationUtils.decorateBlockModelLocation("campfire_off");

      for (Block $$2 : p_124714_) {
         ResourceLocation $$3 = ModelTemplates.CAMPFIRE.create($$2, TextureMapping.campfire($$2), this.modelOutput);
         this.createSimpleFlatItemModel($$2.asItem());
         this.blockStateOutput
            .accept(
               MultiVariantGenerator.multiVariant($$2)
                  .with(createBooleanModelDispatch(BlockStateProperties.LIT, $$3, $$1))
                  .with(createHorizontalFacingDispatchAlt())
            );
      }
   }

   private void createAzalea(Block p_176248_) {
      ResourceLocation $$1 = ModelTemplates.AZALEA.create(p_176248_, TextureMapping.cubeTop(p_176248_), this.modelOutput);
      this.blockStateOutput.accept(createSimpleBlock(p_176248_, $$1));
   }

   private void createPottedAzalea(Block p_176250_) {
      ResourceLocation $$1;
      if (p_176250_ == Blocks.POTTED_FLOWERING_AZALEA) {
         $$1 = ModelTemplates.POTTED_FLOWERING_AZALEA.create(p_176250_, TextureMapping.pottedAzalea(p_176250_), this.modelOutput);
      } else {
         $$1 = ModelTemplates.POTTED_AZALEA.create(p_176250_, TextureMapping.pottedAzalea(p_176250_), this.modelOutput);
      }

      this.blockStateOutput.accept(createSimpleBlock(p_176250_, $$1));
   }

   private void createBookshelf() {
      TextureMapping $$0 = TextureMapping.column(TextureMapping.getBlockTexture(Blocks.BOOKSHELF), TextureMapping.getBlockTexture(Blocks.OAK_PLANKS));
      ResourceLocation $$1 = ModelTemplates.CUBE_COLUMN.create(Blocks.BOOKSHELF, $$0, this.modelOutput);
      this.blockStateOutput.accept(createSimpleBlock(Blocks.BOOKSHELF, $$1));
   }

   private void createRedstoneWire() {
      this.createSimpleFlatItemModel(Items.REDSTONE);
      this.blockStateOutput
         .accept(
            MultiPartGenerator.multiPart(Blocks.REDSTONE_WIRE)
               .with(
                  Condition.or(
                     Condition.condition()
                        .term(BlockStateProperties.NORTH_REDSTONE, RedstoneSide.NONE)
                        .term(BlockStateProperties.EAST_REDSTONE, RedstoneSide.NONE)
                        .term(BlockStateProperties.SOUTH_REDSTONE, RedstoneSide.NONE)
                        .term(BlockStateProperties.WEST_REDSTONE, RedstoneSide.NONE),
                     Condition.condition()
                        .term(BlockStateProperties.NORTH_REDSTONE, RedstoneSide.SIDE, RedstoneSide.UP)
                        .term(BlockStateProperties.EAST_REDSTONE, RedstoneSide.SIDE, RedstoneSide.UP),
                     Condition.condition()
                        .term(BlockStateProperties.EAST_REDSTONE, RedstoneSide.SIDE, RedstoneSide.UP)
                        .term(BlockStateProperties.SOUTH_REDSTONE, RedstoneSide.SIDE, RedstoneSide.UP),
                     Condition.condition()
                        .term(BlockStateProperties.SOUTH_REDSTONE, RedstoneSide.SIDE, RedstoneSide.UP)
                        .term(BlockStateProperties.WEST_REDSTONE, RedstoneSide.SIDE, RedstoneSide.UP),
                     Condition.condition()
                        .term(BlockStateProperties.WEST_REDSTONE, RedstoneSide.SIDE, RedstoneSide.UP)
                        .term(BlockStateProperties.NORTH_REDSTONE, RedstoneSide.SIDE, RedstoneSide.UP)
                  ),
                  Variant.variant().with(VariantProperties.MODEL, ModelLocationUtils.decorateBlockModelLocation("redstone_dust_dot"))
               )
               .with(
                  Condition.condition().term(BlockStateProperties.NORTH_REDSTONE, RedstoneSide.SIDE, RedstoneSide.UP),
                  Variant.variant().with(VariantProperties.MODEL, ModelLocationUtils.decorateBlockModelLocation("redstone_dust_side0"))
               )
               .with(
                  Condition.condition().term(BlockStateProperties.SOUTH_REDSTONE, RedstoneSide.SIDE, RedstoneSide.UP),
                  Variant.variant().with(VariantProperties.MODEL, ModelLocationUtils.decorateBlockModelLocation("redstone_dust_side_alt0"))
               )
               .with(
                  Condition.condition().term(BlockStateProperties.EAST_REDSTONE, RedstoneSide.SIDE, RedstoneSide.UP),
                  Variant.variant()
                     .with(VariantProperties.MODEL, ModelLocationUtils.decorateBlockModelLocation("redstone_dust_side_alt1"))
                     .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)
               )
               .with(
                  Condition.condition().term(BlockStateProperties.WEST_REDSTONE, RedstoneSide.SIDE, RedstoneSide.UP),
                  Variant.variant()
                     .with(VariantProperties.MODEL, ModelLocationUtils.decorateBlockModelLocation("redstone_dust_side1"))
                     .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)
               )
               .with(
                  Condition.condition().term(BlockStateProperties.NORTH_REDSTONE, RedstoneSide.UP),
                  Variant.variant().with(VariantProperties.MODEL, ModelLocationUtils.decorateBlockModelLocation("redstone_dust_up"))
               )
               .with(
                  Condition.condition().term(BlockStateProperties.EAST_REDSTONE, RedstoneSide.UP),
                  Variant.variant()
                     .with(VariantProperties.MODEL, ModelLocationUtils.decorateBlockModelLocation("redstone_dust_up"))
                     .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)
               )
               .with(
                  Condition.condition().term(BlockStateProperties.SOUTH_REDSTONE, RedstoneSide.UP),
                  Variant.variant()
                     .with(VariantProperties.MODEL, ModelLocationUtils.decorateBlockModelLocation("redstone_dust_up"))
                     .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180)
               )
               .with(
                  Condition.condition().term(BlockStateProperties.WEST_REDSTONE, RedstoneSide.UP),
                  Variant.variant()
                     .with(VariantProperties.MODEL, ModelLocationUtils.decorateBlockModelLocation("redstone_dust_up"))
                     .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)
               )
         );
   }

   private void createComparator() {
      this.createSimpleFlatItemModel(Items.COMPARATOR);
      this.blockStateOutput
         .accept(
            MultiVariantGenerator.multiVariant(Blocks.COMPARATOR)
               .with(createHorizontalFacingDispatchAlt())
               .with(
                  PropertyDispatch.properties(BlockStateProperties.MODE_COMPARATOR, BlockStateProperties.POWERED)
                     .select(
                        ComparatorMode.COMPARE, false, Variant.variant().with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(Blocks.COMPARATOR))
                     )
                     .select(
                        ComparatorMode.COMPARE,
                        true,
                        Variant.variant().with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(Blocks.COMPARATOR, "_on"))
                     )
                     .select(
                        ComparatorMode.SUBTRACT,
                        false,
                        Variant.variant().with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(Blocks.COMPARATOR, "_subtract"))
                     )
                     .select(
                        ComparatorMode.SUBTRACT,
                        true,
                        Variant.variant().with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(Blocks.COMPARATOR, "_on_subtract"))
                     )
               )
         );
   }

   private void createSmoothStoneSlab() {
      TextureMapping $$0 = TextureMapping.cube(Blocks.SMOOTH_STONE);
      TextureMapping $$1 = TextureMapping.column(TextureMapping.getBlockTexture(Blocks.SMOOTH_STONE_SLAB, "_side"), $$0.get(TextureSlot.TOP));
      ResourceLocation $$2 = ModelTemplates.SLAB_BOTTOM.create(Blocks.SMOOTH_STONE_SLAB, $$1, this.modelOutput);
      ResourceLocation $$3 = ModelTemplates.SLAB_TOP.create(Blocks.SMOOTH_STONE_SLAB, $$1, this.modelOutput);
      ResourceLocation $$4 = ModelTemplates.CUBE_COLUMN.createWithOverride(Blocks.SMOOTH_STONE_SLAB, "_double", $$1, this.modelOutput);
      this.blockStateOutput.accept(createSlab(Blocks.SMOOTH_STONE_SLAB, $$2, $$3, $$4));
      this.blockStateOutput.accept(createSimpleBlock(Blocks.SMOOTH_STONE, ModelTemplates.CUBE_ALL.create(Blocks.SMOOTH_STONE, $$0, this.modelOutput)));
   }

   private void createBrewingStand() {
      this.createSimpleFlatItemModel(Items.BREWING_STAND);
      this.blockStateOutput
         .accept(
            MultiPartGenerator.multiPart(Blocks.BREWING_STAND)
               .with(Variant.variant().with(VariantProperties.MODEL, TextureMapping.getBlockTexture(Blocks.BREWING_STAND)))
               .with(
                  Condition.condition().term(BlockStateProperties.HAS_BOTTLE_0, true),
                  Variant.variant().with(VariantProperties.MODEL, TextureMapping.getBlockTexture(Blocks.BREWING_STAND, "_bottle0"))
               )
               .with(
                  Condition.condition().term(BlockStateProperties.HAS_BOTTLE_1, true),
                  Variant.variant().with(VariantProperties.MODEL, TextureMapping.getBlockTexture(Blocks.BREWING_STAND, "_bottle1"))
               )
               .with(
                  Condition.condition().term(BlockStateProperties.HAS_BOTTLE_2, true),
                  Variant.variant().with(VariantProperties.MODEL, TextureMapping.getBlockTexture(Blocks.BREWING_STAND, "_bottle2"))
               )
               .with(
                  Condition.condition().term(BlockStateProperties.HAS_BOTTLE_0, false),
                  Variant.variant().with(VariantProperties.MODEL, TextureMapping.getBlockTexture(Blocks.BREWING_STAND, "_empty0"))
               )
               .with(
                  Condition.condition().term(BlockStateProperties.HAS_BOTTLE_1, false),
                  Variant.variant().with(VariantProperties.MODEL, TextureMapping.getBlockTexture(Blocks.BREWING_STAND, "_empty1"))
               )
               .with(
                  Condition.condition().term(BlockStateProperties.HAS_BOTTLE_2, false),
                  Variant.variant().with(VariantProperties.MODEL, TextureMapping.getBlockTexture(Blocks.BREWING_STAND, "_empty2"))
               )
         );
   }

   private void createMushroomBlock(Block p_124984_) {
      ResourceLocation $$1 = ModelTemplates.SINGLE_FACE.create(p_124984_, TextureMapping.defaultTexture(p_124984_), this.modelOutput);
      ResourceLocation $$2 = ModelLocationUtils.decorateBlockModelLocation("mushroom_block_inside");
      this.blockStateOutput
         .accept(
            MultiPartGenerator.multiPart(p_124984_)
               .with(Condition.condition().term(BlockStateProperties.NORTH, true), Variant.variant().with(VariantProperties.MODEL, $$1))
               .with(
                  Condition.condition().term(BlockStateProperties.EAST, true),
                  Variant.variant()
                     .with(VariantProperties.MODEL, $$1)
                     .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)
                     .with(VariantProperties.UV_LOCK, true)
               )
               .with(
                  Condition.condition().term(BlockStateProperties.SOUTH, true),
                  Variant.variant()
                     .with(VariantProperties.MODEL, $$1)
                     .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180)
                     .with(VariantProperties.UV_LOCK, true)
               )
               .with(
                  Condition.condition().term(BlockStateProperties.WEST, true),
                  Variant.variant()
                     .with(VariantProperties.MODEL, $$1)
                     .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)
                     .with(VariantProperties.UV_LOCK, true)
               )
               .with(
                  Condition.condition().term(BlockStateProperties.UP, true),
                  Variant.variant()
                     .with(VariantProperties.MODEL, $$1)
                     .with(VariantProperties.X_ROT, VariantProperties.Rotation.R270)
                     .with(VariantProperties.UV_LOCK, true)
               )
               .with(
                  Condition.condition().term(BlockStateProperties.DOWN, true),
                  Variant.variant()
                     .with(VariantProperties.MODEL, $$1)
                     .with(VariantProperties.X_ROT, VariantProperties.Rotation.R90)
                     .with(VariantProperties.UV_LOCK, true)
               )
               .with(Condition.condition().term(BlockStateProperties.NORTH, false), Variant.variant().with(VariantProperties.MODEL, $$2))
               .with(
                  Condition.condition().term(BlockStateProperties.EAST, false),
                  Variant.variant()
                     .with(VariantProperties.MODEL, $$2)
                     .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)
                     .with(VariantProperties.UV_LOCK, false)
               )
               .with(
                  Condition.condition().term(BlockStateProperties.SOUTH, false),
                  Variant.variant()
                     .with(VariantProperties.MODEL, $$2)
                     .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180)
                     .with(VariantProperties.UV_LOCK, false)
               )
               .with(
                  Condition.condition().term(BlockStateProperties.WEST, false),
                  Variant.variant()
                     .with(VariantProperties.MODEL, $$2)
                     .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)
                     .with(VariantProperties.UV_LOCK, false)
               )
               .with(
                  Condition.condition().term(BlockStateProperties.UP, false),
                  Variant.variant()
                     .with(VariantProperties.MODEL, $$2)
                     .with(VariantProperties.X_ROT, VariantProperties.Rotation.R270)
                     .with(VariantProperties.UV_LOCK, false)
               )
               .with(
                  Condition.condition().term(BlockStateProperties.DOWN, false),
                  Variant.variant()
                     .with(VariantProperties.MODEL, $$2)
                     .with(VariantProperties.X_ROT, VariantProperties.Rotation.R90)
                     .with(VariantProperties.UV_LOCK, false)
               )
         );
      this.delegateItemModel(p_124984_, TexturedModel.CUBE.createWithSuffix(p_124984_, "_inventory", this.modelOutput));
   }

   private void createCakeBlock() {
      this.createSimpleFlatItemModel(Items.CAKE);
      this.blockStateOutput
         .accept(
            MultiVariantGenerator.multiVariant(Blocks.CAKE)
               .with(
                  PropertyDispatch.property(BlockStateProperties.BITES)
                     .select(0, Variant.variant().with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(Blocks.CAKE)))
                     .select(1, Variant.variant().with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(Blocks.CAKE, "_slice1")))
                     .select(2, Variant.variant().with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(Blocks.CAKE, "_slice2")))
                     .select(3, Variant.variant().with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(Blocks.CAKE, "_slice3")))
                     .select(4, Variant.variant().with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(Blocks.CAKE, "_slice4")))
                     .select(5, Variant.variant().with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(Blocks.CAKE, "_slice5")))
                     .select(6, Variant.variant().with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(Blocks.CAKE, "_slice6")))
               )
         );
   }

   private void createCartographyTable() {
      TextureMapping $$0 = new TextureMapping()
         .put(TextureSlot.PARTICLE, TextureMapping.getBlockTexture(Blocks.CARTOGRAPHY_TABLE, "_side3"))
         .put(TextureSlot.DOWN, TextureMapping.getBlockTexture(Blocks.DARK_OAK_PLANKS))
         .put(TextureSlot.UP, TextureMapping.getBlockTexture(Blocks.CARTOGRAPHY_TABLE, "_top"))
         .put(TextureSlot.NORTH, TextureMapping.getBlockTexture(Blocks.CARTOGRAPHY_TABLE, "_side3"))
         .put(TextureSlot.EAST, TextureMapping.getBlockTexture(Blocks.CARTOGRAPHY_TABLE, "_side3"))
         .put(TextureSlot.SOUTH, TextureMapping.getBlockTexture(Blocks.CARTOGRAPHY_TABLE, "_side1"))
         .put(TextureSlot.WEST, TextureMapping.getBlockTexture(Blocks.CARTOGRAPHY_TABLE, "_side2"));
      this.blockStateOutput.accept(createSimpleBlock(Blocks.CARTOGRAPHY_TABLE, ModelTemplates.CUBE.create(Blocks.CARTOGRAPHY_TABLE, $$0, this.modelOutput)));
   }

   private void createSmithingTable() {
      TextureMapping $$0 = new TextureMapping()
         .put(TextureSlot.PARTICLE, TextureMapping.getBlockTexture(Blocks.SMITHING_TABLE, "_front"))
         .put(TextureSlot.DOWN, TextureMapping.getBlockTexture(Blocks.SMITHING_TABLE, "_bottom"))
         .put(TextureSlot.UP, TextureMapping.getBlockTexture(Blocks.SMITHING_TABLE, "_top"))
         .put(TextureSlot.NORTH, TextureMapping.getBlockTexture(Blocks.SMITHING_TABLE, "_front"))
         .put(TextureSlot.SOUTH, TextureMapping.getBlockTexture(Blocks.SMITHING_TABLE, "_front"))
         .put(TextureSlot.EAST, TextureMapping.getBlockTexture(Blocks.SMITHING_TABLE, "_side"))
         .put(TextureSlot.WEST, TextureMapping.getBlockTexture(Blocks.SMITHING_TABLE, "_side"));
      this.blockStateOutput.accept(createSimpleBlock(Blocks.SMITHING_TABLE, ModelTemplates.CUBE.create(Blocks.SMITHING_TABLE, $$0, this.modelOutput)));
   }

   private void createCraftingTableLike(Block p_124550_, Block p_124551_, BiFunction<Block, Block, TextureMapping> p_124552_) {
      TextureMapping $$3 = p_124552_.apply(p_124550_, p_124551_);
      this.blockStateOutput.accept(createSimpleBlock(p_124550_, ModelTemplates.CUBE.create(p_124550_, $$3, this.modelOutput)));
   }

   public void createGenericCube(Block p_282830_) {
      TextureMapping $$1 = new TextureMapping()
         .put(TextureSlot.PARTICLE, TextureMapping.getBlockTexture(p_282830_, "_particle"))
         .put(TextureSlot.DOWN, TextureMapping.getBlockTexture(p_282830_, "_down"))
         .put(TextureSlot.UP, TextureMapping.getBlockTexture(p_282830_, "_up"))
         .put(TextureSlot.NORTH, TextureMapping.getBlockTexture(p_282830_, "_north"))
         .put(TextureSlot.SOUTH, TextureMapping.getBlockTexture(p_282830_, "_south"))
         .put(TextureSlot.EAST, TextureMapping.getBlockTexture(p_282830_, "_east"))
         .put(TextureSlot.WEST, TextureMapping.getBlockTexture(p_282830_, "_west"));
      this.blockStateOutput.accept(createSimpleBlock(p_282830_, ModelTemplates.CUBE.create(p_282830_, $$1, this.modelOutput)));
   }

   private void createPumpkins() {
      TextureMapping $$0 = TextureMapping.column(Blocks.PUMPKIN);
      this.blockStateOutput.accept(createSimpleBlock(Blocks.PUMPKIN, ModelLocationUtils.getModelLocation(Blocks.PUMPKIN)));
      this.createPumpkinVariant(Blocks.CARVED_PUMPKIN, $$0);
      this.createPumpkinVariant(Blocks.JACK_O_LANTERN, $$0);
   }

   private void createPumpkinVariant(Block p_124565_, TextureMapping p_124566_) {
      ResourceLocation $$2 = ModelTemplates.CUBE_ORIENTABLE
         .create(p_124565_, p_124566_.copyAndUpdate(TextureSlot.FRONT, TextureMapping.getBlockTexture(p_124565_)), this.modelOutput);
      this.blockStateOutput
         .accept(MultiVariantGenerator.multiVariant(p_124565_, Variant.variant().with(VariantProperties.MODEL, $$2)).with(createHorizontalFacingDispatch()));
   }

   private void createCauldrons() {
      this.createSimpleFlatItemModel(Items.CAULDRON);
      this.createNonTemplateModelBlock(Blocks.CAULDRON);
      this.blockStateOutput
         .accept(
            createSimpleBlock(
               Blocks.LAVA_CAULDRON,
               ModelTemplates.CAULDRON_FULL
                  .create(Blocks.LAVA_CAULDRON, TextureMapping.cauldron(TextureMapping.getBlockTexture(Blocks.LAVA, "_still")), this.modelOutput)
            )
         );
      this.blockStateOutput
         .accept(
            MultiVariantGenerator.multiVariant(Blocks.WATER_CAULDRON)
               .with(
                  PropertyDispatch.property(LayeredCauldronBlock.LEVEL)
                     .select(
                        1,
                        Variant.variant()
                           .with(
                              VariantProperties.MODEL,
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
                        Variant.variant()
                           .with(
                              VariantProperties.MODEL,
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
                        Variant.variant()
                           .with(
                              VariantProperties.MODEL,
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
            MultiVariantGenerator.multiVariant(Blocks.POWDER_SNOW_CAULDRON)
               .with(
                  PropertyDispatch.property(LayeredCauldronBlock.LEVEL)
                     .select(
                        1,
                        Variant.variant()
                           .with(
                              VariantProperties.MODEL,
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
                        Variant.variant()
                           .with(
                              VariantProperties.MODEL,
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
                        Variant.variant()
                           .with(
                              VariantProperties.MODEL,
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
      TextureMapping $$0 = TextureMapping.defaultTexture(Blocks.CHORUS_FLOWER);
      ResourceLocation $$1 = ModelTemplates.CHORUS_FLOWER.create(Blocks.CHORUS_FLOWER, $$0, this.modelOutput);
      ResourceLocation $$2 = this.createSuffixedVariant(
         Blocks.CHORUS_FLOWER, "_dead", ModelTemplates.CHORUS_FLOWER, p_176148_ -> $$0.copyAndUpdate(TextureSlot.TEXTURE, p_176148_)
      );
      this.blockStateOutput
         .accept(MultiVariantGenerator.multiVariant(Blocks.CHORUS_FLOWER).with(createEmptyOrFullDispatch(BlockStateProperties.AGE_5, 5, $$2, $$1)));
   }

   private void createCrafterBlock() {
      ResourceLocation $$0 = ModelLocationUtils.getModelLocation(Blocks.CRAFTER);
      ResourceLocation $$1 = ModelLocationUtils.getModelLocation(Blocks.CRAFTER, "_triggered");
      ResourceLocation $$2 = ModelLocationUtils.getModelLocation(Blocks.CRAFTER, "_crafting");
      ResourceLocation $$3 = ModelLocationUtils.getModelLocation(Blocks.CRAFTER, "_crafting_triggered");
      this.blockStateOutput
         .accept(
            MultiVariantGenerator.multiVariant(Blocks.CRAFTER)
               .with(PropertyDispatch.property(BlockStateProperties.ORIENTATION).generate(p_236301_ -> this.applyRotation(p_236301_, Variant.variant())))
               .with(
                  PropertyDispatch.properties(BlockStateProperties.TRIGGERED, CrafterBlock.CRAFTING)
                     .select(false, false, Variant.variant().with(VariantProperties.MODEL, $$0))
                     .select(true, true, Variant.variant().with(VariantProperties.MODEL, $$3))
                     .select(true, false, Variant.variant().with(VariantProperties.MODEL, $$1))
                     .select(false, true, Variant.variant().with(VariantProperties.MODEL, $$2))
               )
         );
   }

   private void createDispenserBlock(Block p_124987_) {
      TextureMapping $$1 = new TextureMapping()
         .put(TextureSlot.TOP, TextureMapping.getBlockTexture(Blocks.FURNACE, "_top"))
         .put(TextureSlot.SIDE, TextureMapping.getBlockTexture(Blocks.FURNACE, "_side"))
         .put(TextureSlot.FRONT, TextureMapping.getBlockTexture(p_124987_, "_front"));
      TextureMapping $$2 = new TextureMapping()
         .put(TextureSlot.SIDE, TextureMapping.getBlockTexture(Blocks.FURNACE, "_top"))
         .put(TextureSlot.FRONT, TextureMapping.getBlockTexture(p_124987_, "_front_vertical"));
      ResourceLocation $$3 = ModelTemplates.CUBE_ORIENTABLE.create(p_124987_, $$1, this.modelOutput);
      ResourceLocation $$4 = ModelTemplates.CUBE_ORIENTABLE_VERTICAL.create(p_124987_, $$2, this.modelOutput);
      this.blockStateOutput
         .accept(
            MultiVariantGenerator.multiVariant(p_124987_)
               .with(
                  PropertyDispatch.property(BlockStateProperties.FACING)
                     .select(
                        Direction.DOWN, Variant.variant().with(VariantProperties.MODEL, $$4).with(VariantProperties.X_ROT, VariantProperties.Rotation.R180)
                     )
                     .select(Direction.UP, Variant.variant().with(VariantProperties.MODEL, $$4))
                     .select(Direction.NORTH, Variant.variant().with(VariantProperties.MODEL, $$3))
                     .select(Direction.EAST, Variant.variant().with(VariantProperties.MODEL, $$3).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90))
                     .select(
                        Direction.SOUTH, Variant.variant().with(VariantProperties.MODEL, $$3).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180)
                     )
                     .select(
                        Direction.WEST, Variant.variant().with(VariantProperties.MODEL, $$3).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)
                     )
               )
         );
   }

   private void createEndPortalFrame() {
      ResourceLocation $$0 = ModelLocationUtils.getModelLocation(Blocks.END_PORTAL_FRAME);
      ResourceLocation $$1 = ModelLocationUtils.getModelLocation(Blocks.END_PORTAL_FRAME, "_filled");
      this.blockStateOutput
         .accept(
            MultiVariantGenerator.multiVariant(Blocks.END_PORTAL_FRAME)
               .with(
                  PropertyDispatch.property(BlockStateProperties.EYE)
                     .select(false, Variant.variant().with(VariantProperties.MODEL, $$0))
                     .select(true, Variant.variant().with(VariantProperties.MODEL, $$1))
               )
               .with(createHorizontalFacingDispatchAlt())
         );
   }

   private void createChorusPlant() {
      ResourceLocation $$0 = ModelLocationUtils.getModelLocation(Blocks.CHORUS_PLANT, "_side");
      ResourceLocation $$1 = ModelLocationUtils.getModelLocation(Blocks.CHORUS_PLANT, "_noside");
      ResourceLocation $$2 = ModelLocationUtils.getModelLocation(Blocks.CHORUS_PLANT, "_noside1");
      ResourceLocation $$3 = ModelLocationUtils.getModelLocation(Blocks.CHORUS_PLANT, "_noside2");
      ResourceLocation $$4 = ModelLocationUtils.getModelLocation(Blocks.CHORUS_PLANT, "_noside3");
      this.blockStateOutput
         .accept(
            MultiPartGenerator.multiPart(Blocks.CHORUS_PLANT)
               .with(Condition.condition().term(BlockStateProperties.NORTH, true), Variant.variant().with(VariantProperties.MODEL, $$0))
               .with(
                  Condition.condition().term(BlockStateProperties.EAST, true),
                  Variant.variant()
                     .with(VariantProperties.MODEL, $$0)
                     .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)
                     .with(VariantProperties.UV_LOCK, true)
               )
               .with(
                  Condition.condition().term(BlockStateProperties.SOUTH, true),
                  Variant.variant()
                     .with(VariantProperties.MODEL, $$0)
                     .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180)
                     .with(VariantProperties.UV_LOCK, true)
               )
               .with(
                  Condition.condition().term(BlockStateProperties.WEST, true),
                  Variant.variant()
                     .with(VariantProperties.MODEL, $$0)
                     .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)
                     .with(VariantProperties.UV_LOCK, true)
               )
               .with(
                  Condition.condition().term(BlockStateProperties.UP, true),
                  Variant.variant()
                     .with(VariantProperties.MODEL, $$0)
                     .with(VariantProperties.X_ROT, VariantProperties.Rotation.R270)
                     .with(VariantProperties.UV_LOCK, true)
               )
               .with(
                  Condition.condition().term(BlockStateProperties.DOWN, true),
                  Variant.variant()
                     .with(VariantProperties.MODEL, $$0)
                     .with(VariantProperties.X_ROT, VariantProperties.Rotation.R90)
                     .with(VariantProperties.UV_LOCK, true)
               )
               .with(
                  Condition.condition().term(BlockStateProperties.NORTH, false),
                  Variant.variant().with(VariantProperties.MODEL, $$1).with(VariantProperties.WEIGHT, 2),
                  Variant.variant().with(VariantProperties.MODEL, $$2),
                  Variant.variant().with(VariantProperties.MODEL, $$3),
                  Variant.variant().with(VariantProperties.MODEL, $$4)
               )
               .with(
                  Condition.condition().term(BlockStateProperties.EAST, false),
                  Variant.variant()
                     .with(VariantProperties.MODEL, $$2)
                     .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)
                     .with(VariantProperties.UV_LOCK, true),
                  Variant.variant()
                     .with(VariantProperties.MODEL, $$3)
                     .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)
                     .with(VariantProperties.UV_LOCK, true),
                  Variant.variant()
                     .with(VariantProperties.MODEL, $$4)
                     .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)
                     .with(VariantProperties.UV_LOCK, true),
                  Variant.variant()
                     .with(VariantProperties.MODEL, $$1)
                     .with(VariantProperties.WEIGHT, 2)
                     .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)
                     .with(VariantProperties.UV_LOCK, true)
               )
               .with(
                  Condition.condition().term(BlockStateProperties.SOUTH, false),
                  Variant.variant()
                     .with(VariantProperties.MODEL, $$3)
                     .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180)
                     .with(VariantProperties.UV_LOCK, true),
                  Variant.variant()
                     .with(VariantProperties.MODEL, $$4)
                     .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180)
                     .with(VariantProperties.UV_LOCK, true),
                  Variant.variant()
                     .with(VariantProperties.MODEL, $$1)
                     .with(VariantProperties.WEIGHT, 2)
                     .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180)
                     .with(VariantProperties.UV_LOCK, true),
                  Variant.variant()
                     .with(VariantProperties.MODEL, $$2)
                     .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180)
                     .with(VariantProperties.UV_LOCK, true)
               )
               .with(
                  Condition.condition().term(BlockStateProperties.WEST, false),
                  Variant.variant()
                     .with(VariantProperties.MODEL, $$4)
                     .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)
                     .with(VariantProperties.UV_LOCK, true),
                  Variant.variant()
                     .with(VariantProperties.MODEL, $$1)
                     .with(VariantProperties.WEIGHT, 2)
                     .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)
                     .with(VariantProperties.UV_LOCK, true),
                  Variant.variant()
                     .with(VariantProperties.MODEL, $$2)
                     .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)
                     .with(VariantProperties.UV_LOCK, true),
                  Variant.variant()
                     .with(VariantProperties.MODEL, $$3)
                     .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)
                     .with(VariantProperties.UV_LOCK, true)
               )
               .with(
                  Condition.condition().term(BlockStateProperties.UP, false),
                  Variant.variant()
                     .with(VariantProperties.MODEL, $$1)
                     .with(VariantProperties.WEIGHT, 2)
                     .with(VariantProperties.X_ROT, VariantProperties.Rotation.R270)
                     .with(VariantProperties.UV_LOCK, true),
                  Variant.variant()
                     .with(VariantProperties.MODEL, $$4)
                     .with(VariantProperties.X_ROT, VariantProperties.Rotation.R270)
                     .with(VariantProperties.UV_LOCK, true),
                  Variant.variant()
                     .with(VariantProperties.MODEL, $$2)
                     .with(VariantProperties.X_ROT, VariantProperties.Rotation.R270)
                     .with(VariantProperties.UV_LOCK, true),
                  Variant.variant()
                     .with(VariantProperties.MODEL, $$3)
                     .with(VariantProperties.X_ROT, VariantProperties.Rotation.R270)
                     .with(VariantProperties.UV_LOCK, true)
               )
               .with(
                  Condition.condition().term(BlockStateProperties.DOWN, false),
                  Variant.variant()
                     .with(VariantProperties.MODEL, $$4)
                     .with(VariantProperties.X_ROT, VariantProperties.Rotation.R90)
                     .with(VariantProperties.UV_LOCK, true),
                  Variant.variant()
                     .with(VariantProperties.MODEL, $$3)
                     .with(VariantProperties.X_ROT, VariantProperties.Rotation.R90)
                     .with(VariantProperties.UV_LOCK, true),
                  Variant.variant()
                     .with(VariantProperties.MODEL, $$2)
                     .with(VariantProperties.X_ROT, VariantProperties.Rotation.R90)
                     .with(VariantProperties.UV_LOCK, true),
                  Variant.variant()
                     .with(VariantProperties.MODEL, $$1)
                     .with(VariantProperties.WEIGHT, 2)
                     .with(VariantProperties.X_ROT, VariantProperties.Rotation.R90)
                     .with(VariantProperties.UV_LOCK, true)
               )
         );
   }

   private void createComposter() {
      this.blockStateOutput
         .accept(
            MultiPartGenerator.multiPart(Blocks.COMPOSTER)
               .with(Variant.variant().with(VariantProperties.MODEL, TextureMapping.getBlockTexture(Blocks.COMPOSTER)))
               .with(
                  Condition.condition().term(BlockStateProperties.LEVEL_COMPOSTER, 1),
                  Variant.variant().with(VariantProperties.MODEL, TextureMapping.getBlockTexture(Blocks.COMPOSTER, "_contents1"))
               )
               .with(
                  Condition.condition().term(BlockStateProperties.LEVEL_COMPOSTER, 2),
                  Variant.variant().with(VariantProperties.MODEL, TextureMapping.getBlockTexture(Blocks.COMPOSTER, "_contents2"))
               )
               .with(
                  Condition.condition().term(BlockStateProperties.LEVEL_COMPOSTER, 3),
                  Variant.variant().with(VariantProperties.MODEL, TextureMapping.getBlockTexture(Blocks.COMPOSTER, "_contents3"))
               )
               .with(
                  Condition.condition().term(BlockStateProperties.LEVEL_COMPOSTER, 4),
                  Variant.variant().with(VariantProperties.MODEL, TextureMapping.getBlockTexture(Blocks.COMPOSTER, "_contents4"))
               )
               .with(
                  Condition.condition().term(BlockStateProperties.LEVEL_COMPOSTER, 5),
                  Variant.variant().with(VariantProperties.MODEL, TextureMapping.getBlockTexture(Blocks.COMPOSTER, "_contents5"))
               )
               .with(
                  Condition.condition().term(BlockStateProperties.LEVEL_COMPOSTER, 6),
                  Variant.variant().with(VariantProperties.MODEL, TextureMapping.getBlockTexture(Blocks.COMPOSTER, "_contents6"))
               )
               .with(
                  Condition.condition().term(BlockStateProperties.LEVEL_COMPOSTER, 7),
                  Variant.variant().with(VariantProperties.MODEL, TextureMapping.getBlockTexture(Blocks.COMPOSTER, "_contents7"))
               )
               .with(
                  Condition.condition().term(BlockStateProperties.LEVEL_COMPOSTER, 8),
                  Variant.variant().with(VariantProperties.MODEL, TextureMapping.getBlockTexture(Blocks.COMPOSTER, "_contents_ready"))
               )
         );
   }

   private void createCopperBulb(Block p_308914_) {
      ResourceLocation $$1 = ModelTemplates.CUBE_ALL.create(p_308914_, TextureMapping.cube(p_308914_), this.modelOutput);
      ResourceLocation $$2 = this.createSuffixedVariant(p_308914_, "_powered", ModelTemplates.CUBE_ALL, TextureMapping::cube);
      ResourceLocation $$3 = this.createSuffixedVariant(p_308914_, "_lit", ModelTemplates.CUBE_ALL, TextureMapping::cube);
      ResourceLocation $$4 = this.createSuffixedVariant(p_308914_, "_lit_powered", ModelTemplates.CUBE_ALL, TextureMapping::cube);
      this.blockStateOutput.accept(this.createCopperBulb(p_308914_, $$1, $$3, $$2, $$4));
   }

   private BlockStateGenerator createCopperBulb(
      Block p_309175_, ResourceLocation p_309189_, ResourceLocation p_308957_, ResourceLocation p_308948_, ResourceLocation p_309019_
   ) {
      return MultiVariantGenerator.multiVariant(p_309175_)
         .with(
            PropertyDispatch.properties(BlockStateProperties.LIT, BlockStateProperties.POWERED)
               .generate(
                  (p_308471_, p_308472_) -> p_308471_
                     ? Variant.variant().with(VariantProperties.MODEL, p_308472_ ? p_309019_ : p_308957_)
                     : Variant.variant().with(VariantProperties.MODEL, p_308472_ ? p_308948_ : p_309189_)
               )
         );
   }

   private void copyCopperBulbModel(Block p_309045_, Block p_309092_) {
      ResourceLocation $$2 = ModelLocationUtils.getModelLocation(p_309045_);
      ResourceLocation $$3 = ModelLocationUtils.getModelLocation(p_309045_, "_powered");
      ResourceLocation $$4 = ModelLocationUtils.getModelLocation(p_309045_, "_lit");
      ResourceLocation $$5 = ModelLocationUtils.getModelLocation(p_309045_, "_lit_powered");
      this.delegateItemModel(p_309092_, ModelLocationUtils.getModelLocation(p_309045_.asItem()));
      this.blockStateOutput.accept(this.createCopperBulb(p_309092_, $$2, $$4, $$3, $$5));
   }

   private void createAmethystCluster(Block p_176252_) {
      this.skipAutoItemBlock(p_176252_);
      this.blockStateOutput
         .accept(
            MultiVariantGenerator.multiVariant(
                  p_176252_,
                  Variant.variant().with(VariantProperties.MODEL, ModelTemplates.CROSS.create(p_176252_, TextureMapping.cross(p_176252_), this.modelOutput))
               )
               .with(this.createColumnWithFacing())
         );
   }

   private void createAmethystClusters() {
      this.createAmethystCluster(Blocks.SMALL_AMETHYST_BUD);
      this.createAmethystCluster(Blocks.MEDIUM_AMETHYST_BUD);
      this.createAmethystCluster(Blocks.LARGE_AMETHYST_BUD);
      this.createAmethystCluster(Blocks.AMETHYST_CLUSTER);
   }

   private void createPointedDripstone() {
      this.skipAutoItemBlock(Blocks.POINTED_DRIPSTONE);
      PropertyDispatch.C2<Direction, DripstoneThickness> $$0 = PropertyDispatch.properties(
         BlockStateProperties.VERTICAL_DIRECTION, BlockStateProperties.DRIPSTONE_THICKNESS
      );

      for (DripstoneThickness $$1 : DripstoneThickness.values()) {
         $$0.select(Direction.UP, $$1, this.createPointedDripstoneVariant(Direction.UP, $$1));
      }

      for (DripstoneThickness $$2 : DripstoneThickness.values()) {
         $$0.select(Direction.DOWN, $$2, this.createPointedDripstoneVariant(Direction.DOWN, $$2));
      }

      this.blockStateOutput.accept(MultiVariantGenerator.multiVariant(Blocks.POINTED_DRIPSTONE).with($$0));
   }

   private Variant createPointedDripstoneVariant(Direction p_176117_, DripstoneThickness p_176118_) {
      String $$2 = "_" + p_176117_.getSerializedName() + "_" + p_176118_.getSerializedName();
      TextureMapping $$3 = TextureMapping.cross(TextureMapping.getBlockTexture(Blocks.POINTED_DRIPSTONE, $$2));
      return Variant.variant()
         .with(VariantProperties.MODEL, ModelTemplates.POINTED_DRIPSTONE.createWithSuffix(Blocks.POINTED_DRIPSTONE, $$2, $$3, this.modelOutput));
   }

   private void createNyliumBlock(Block p_124990_) {
      TextureMapping $$1 = new TextureMapping()
         .put(TextureSlot.BOTTOM, TextureMapping.getBlockTexture(Blocks.NETHERRACK))
         .put(TextureSlot.TOP, TextureMapping.getBlockTexture(p_124990_))
         .put(TextureSlot.SIDE, TextureMapping.getBlockTexture(p_124990_, "_side"));
      this.blockStateOutput.accept(createSimpleBlock(p_124990_, ModelTemplates.CUBE_BOTTOM_TOP.create(p_124990_, $$1, this.modelOutput)));
   }

   private void createDaylightDetector() {
      ResourceLocation $$0 = TextureMapping.getBlockTexture(Blocks.DAYLIGHT_DETECTOR, "_side");
      TextureMapping $$1 = new TextureMapping()
         .put(TextureSlot.TOP, TextureMapping.getBlockTexture(Blocks.DAYLIGHT_DETECTOR, "_top"))
         .put(TextureSlot.SIDE, $$0);
      TextureMapping $$2 = new TextureMapping()
         .put(TextureSlot.TOP, TextureMapping.getBlockTexture(Blocks.DAYLIGHT_DETECTOR, "_inverted_top"))
         .put(TextureSlot.SIDE, $$0);
      this.blockStateOutput
         .accept(
            MultiVariantGenerator.multiVariant(Blocks.DAYLIGHT_DETECTOR)
               .with(
                  PropertyDispatch.property(BlockStateProperties.INVERTED)
                     .select(
                        false,
                        Variant.variant()
                           .with(VariantProperties.MODEL, ModelTemplates.DAYLIGHT_DETECTOR.create(Blocks.DAYLIGHT_DETECTOR, $$1, this.modelOutput))
                     )
                     .select(
                        true,
                        Variant.variant()
                           .with(
                              VariantProperties.MODEL,
                              ModelTemplates.DAYLIGHT_DETECTOR
                                 .create(ModelLocationUtils.getModelLocation(Blocks.DAYLIGHT_DETECTOR, "_inverted"), $$2, this.modelOutput)
                           )
                     )
               )
         );
   }

   private void createRotatableColumn(Block p_124993_) {
      this.blockStateOutput
         .accept(
            MultiVariantGenerator.multiVariant(p_124993_, Variant.variant().with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(p_124993_)))
               .with(this.createColumnWithFacing())
         );
   }

   private void createLightningRod() {
      Block $$0 = Blocks.LIGHTNING_ROD;
      ResourceLocation $$1 = ModelLocationUtils.getModelLocation($$0, "_on");
      ResourceLocation $$2 = ModelLocationUtils.getModelLocation($$0);
      this.blockStateOutput
         .accept(
            MultiVariantGenerator.multiVariant($$0, Variant.variant().with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation($$0)))
               .with(this.createColumnWithFacing())
               .with(createBooleanModelDispatch(BlockStateProperties.POWERED, $$1, $$2))
         );
   }

   private void createFarmland() {
      TextureMapping $$0 = new TextureMapping()
         .put(TextureSlot.DIRT, TextureMapping.getBlockTexture(Blocks.DIRT))
         .put(TextureSlot.TOP, TextureMapping.getBlockTexture(Blocks.FARMLAND));
      TextureMapping $$1 = new TextureMapping()
         .put(TextureSlot.DIRT, TextureMapping.getBlockTexture(Blocks.DIRT))
         .put(TextureSlot.TOP, TextureMapping.getBlockTexture(Blocks.FARMLAND, "_moist"));
      ResourceLocation $$2 = ModelTemplates.FARMLAND.create(Blocks.FARMLAND, $$0, this.modelOutput);
      ResourceLocation $$3 = ModelTemplates.FARMLAND.create(TextureMapping.getBlockTexture(Blocks.FARMLAND, "_moist"), $$1, this.modelOutput);
      this.blockStateOutput
         .accept(MultiVariantGenerator.multiVariant(Blocks.FARMLAND).with(createEmptyOrFullDispatch(BlockStateProperties.MOISTURE, 7, $$3, $$2)));
   }

   private List<ResourceLocation> createFloorFireModels(Block p_124996_) {
      ResourceLocation $$1 = ModelTemplates.FIRE_FLOOR
         .create(ModelLocationUtils.getModelLocation(p_124996_, "_floor0"), TextureMapping.fire0(p_124996_), this.modelOutput);
      ResourceLocation $$2 = ModelTemplates.FIRE_FLOOR
         .create(ModelLocationUtils.getModelLocation(p_124996_, "_floor1"), TextureMapping.fire1(p_124996_), this.modelOutput);
      return ImmutableList.of($$1, $$2);
   }

   private List<ResourceLocation> createSideFireModels(Block p_124999_) {
      ResourceLocation $$1 = ModelTemplates.FIRE_SIDE
         .create(ModelLocationUtils.getModelLocation(p_124999_, "_side0"), TextureMapping.fire0(p_124999_), this.modelOutput);
      ResourceLocation $$2 = ModelTemplates.FIRE_SIDE
         .create(ModelLocationUtils.getModelLocation(p_124999_, "_side1"), TextureMapping.fire1(p_124999_), this.modelOutput);
      ResourceLocation $$3 = ModelTemplates.FIRE_SIDE_ALT
         .create(ModelLocationUtils.getModelLocation(p_124999_, "_side_alt0"), TextureMapping.fire0(p_124999_), this.modelOutput);
      ResourceLocation $$4 = ModelTemplates.FIRE_SIDE_ALT
         .create(ModelLocationUtils.getModelLocation(p_124999_, "_side_alt1"), TextureMapping.fire1(p_124999_), this.modelOutput);
      return ImmutableList.of($$1, $$2, $$3, $$4);
   }

   private List<ResourceLocation> createTopFireModels(Block p_125002_) {
      ResourceLocation $$1 = ModelTemplates.FIRE_UP
         .create(ModelLocationUtils.getModelLocation(p_125002_, "_up0"), TextureMapping.fire0(p_125002_), this.modelOutput);
      ResourceLocation $$2 = ModelTemplates.FIRE_UP
         .create(ModelLocationUtils.getModelLocation(p_125002_, "_up1"), TextureMapping.fire1(p_125002_), this.modelOutput);
      ResourceLocation $$3 = ModelTemplates.FIRE_UP_ALT
         .create(ModelLocationUtils.getModelLocation(p_125002_, "_up_alt0"), TextureMapping.fire0(p_125002_), this.modelOutput);
      ResourceLocation $$4 = ModelTemplates.FIRE_UP_ALT
         .create(ModelLocationUtils.getModelLocation(p_125002_, "_up_alt1"), TextureMapping.fire1(p_125002_), this.modelOutput);
      return ImmutableList.of($$1, $$2, $$3, $$4);
   }

   private static List<Variant> wrapModels(List<ResourceLocation> p_124683_, UnaryOperator<Variant> p_124684_) {
      return p_124683_.stream().map(p_176238_ -> Variant.variant().with(VariantProperties.MODEL, p_176238_)).map(p_124684_).collect(Collectors.toList());
   }

   private void createFire() {
      Condition $$0 = Condition.condition()
         .term(BlockStateProperties.NORTH, false)
         .term(BlockStateProperties.EAST, false)
         .term(BlockStateProperties.SOUTH, false)
         .term(BlockStateProperties.WEST, false)
         .term(BlockStateProperties.UP, false);
      List<ResourceLocation> $$1 = this.createFloorFireModels(Blocks.FIRE);
      List<ResourceLocation> $$2 = this.createSideFireModels(Blocks.FIRE);
      List<ResourceLocation> $$3 = this.createTopFireModels(Blocks.FIRE);
      this.blockStateOutput
         .accept(
            MultiPartGenerator.multiPart(Blocks.FIRE)
               .with($$0, wrapModels($$1, p_124894_ -> p_124894_))
               .with(Condition.or(Condition.condition().term(BlockStateProperties.NORTH, true), $$0), wrapModels($$2, p_176243_ -> p_176243_))
               .with(
                  Condition.or(Condition.condition().term(BlockStateProperties.EAST, true), $$0),
                  wrapModels($$2, p_176240_ -> p_176240_.with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90))
               )
               .with(
                  Condition.or(Condition.condition().term(BlockStateProperties.SOUTH, true), $$0),
                  wrapModels($$2, p_176236_ -> p_176236_.with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180))
               )
               .with(
                  Condition.or(Condition.condition().term(BlockStateProperties.WEST, true), $$0),
                  wrapModels($$2, p_176232_ -> p_176232_.with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270))
               )
               .with(Condition.condition().term(BlockStateProperties.UP, true), wrapModels($$3, p_176227_ -> p_176227_))
         );
   }

   private void createSoulFire() {
      List<ResourceLocation> $$0 = this.createFloorFireModels(Blocks.SOUL_FIRE);
      List<ResourceLocation> $$1 = this.createSideFireModels(Blocks.SOUL_FIRE);
      this.blockStateOutput
         .accept(
            MultiPartGenerator.multiPart(Blocks.SOUL_FIRE)
               .with(wrapModels($$0, p_176221_ -> p_176221_))
               .with(wrapModels($$1, p_176209_ -> p_176209_))
               .with(wrapModels($$1, p_176200_ -> p_176200_.with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)))
               .with(wrapModels($$1, p_176188_ -> p_176188_.with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180)))
               .with(wrapModels($$1, p_176143_ -> p_176143_.with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)))
         );
   }

   private void createLantern(Block p_125005_) {
      ResourceLocation $$1 = TexturedModel.LANTERN.create(p_125005_, this.modelOutput);
      ResourceLocation $$2 = TexturedModel.HANGING_LANTERN.create(p_125005_, this.modelOutput);
      this.createSimpleFlatItemModel(p_125005_.asItem());
      this.blockStateOutput.accept(MultiVariantGenerator.multiVariant(p_125005_).with(createBooleanModelDispatch(BlockStateProperties.HANGING, $$2, $$1)));
   }

   private void createMuddyMangroveRoots() {
      TextureMapping $$0 = TextureMapping.column(
         TextureMapping.getBlockTexture(Blocks.MUDDY_MANGROVE_ROOTS, "_side"), TextureMapping.getBlockTexture(Blocks.MUDDY_MANGROVE_ROOTS, "_top")
      );
      ResourceLocation $$1 = ModelTemplates.CUBE_COLUMN.create(Blocks.MUDDY_MANGROVE_ROOTS, $$0, this.modelOutput);
      this.blockStateOutput.accept(createAxisAlignedPillarBlock(Blocks.MUDDY_MANGROVE_ROOTS, $$1));
   }

   private void createMangrovePropagule() {
      this.createSimpleFlatItemModel(Items.MANGROVE_PROPAGULE);
      Block $$0 = Blocks.MANGROVE_PROPAGULE;
      PropertyDispatch.C2<Boolean, Integer> $$1 = PropertyDispatch.properties(MangrovePropaguleBlock.HANGING, MangrovePropaguleBlock.AGE);
      ResourceLocation $$2 = ModelLocationUtils.getModelLocation($$0);

      for (int $$3 = 0; $$3 <= 4; $$3++) {
         ResourceLocation $$4 = ModelLocationUtils.getModelLocation($$0, "_hanging_" + $$3);
         $$1.select(true, $$3, Variant.variant().with(VariantProperties.MODEL, $$4));
         $$1.select(false, $$3, Variant.variant().with(VariantProperties.MODEL, $$2));
      }

      this.blockStateOutput.accept(MultiVariantGenerator.multiVariant(Blocks.MANGROVE_PROPAGULE).with($$1));
   }

   private void createFrostedIce() {
      this.blockStateOutput
         .accept(
            MultiVariantGenerator.multiVariant(Blocks.FROSTED_ICE)
               .with(
                  PropertyDispatch.property(BlockStateProperties.AGE_3)
                     .select(
                        0,
                        Variant.variant()
                           .with(VariantProperties.MODEL, this.createSuffixedVariant(Blocks.FROSTED_ICE, "_0", ModelTemplates.CUBE_ALL, TextureMapping::cube))
                     )
                     .select(
                        1,
                        Variant.variant()
                           .with(VariantProperties.MODEL, this.createSuffixedVariant(Blocks.FROSTED_ICE, "_1", ModelTemplates.CUBE_ALL, TextureMapping::cube))
                     )
                     .select(
                        2,
                        Variant.variant()
                           .with(VariantProperties.MODEL, this.createSuffixedVariant(Blocks.FROSTED_ICE, "_2", ModelTemplates.CUBE_ALL, TextureMapping::cube))
                     )
                     .select(
                        3,
                        Variant.variant()
                           .with(VariantProperties.MODEL, this.createSuffixedVariant(Blocks.FROSTED_ICE, "_3", ModelTemplates.CUBE_ALL, TextureMapping::cube))
                     )
               )
         );
   }

   private void createGrassBlocks() {
      ResourceLocation $$0 = TextureMapping.getBlockTexture(Blocks.DIRT);
      TextureMapping $$1 = new TextureMapping()
         .put(TextureSlot.BOTTOM, $$0)
         .copyForced(TextureSlot.BOTTOM, TextureSlot.PARTICLE)
         .put(TextureSlot.TOP, TextureMapping.getBlockTexture(Blocks.GRASS_BLOCK, "_top"))
         .put(TextureSlot.SIDE, TextureMapping.getBlockTexture(Blocks.GRASS_BLOCK, "_snow"));
      Variant $$2 = Variant.variant()
         .with(VariantProperties.MODEL, ModelTemplates.CUBE_BOTTOM_TOP.createWithSuffix(Blocks.GRASS_BLOCK, "_snow", $$1, this.modelOutput));
      this.createGrassLikeBlock(Blocks.GRASS_BLOCK, ModelLocationUtils.getModelLocation(Blocks.GRASS_BLOCK), $$2);
      ResourceLocation $$3 = TexturedModel.CUBE_TOP_BOTTOM
         .get(Blocks.MYCELIUM)
         .updateTextures(p_176198_ -> p_176198_.put(TextureSlot.BOTTOM, $$0))
         .create(Blocks.MYCELIUM, this.modelOutput);
      this.createGrassLikeBlock(Blocks.MYCELIUM, $$3, $$2);
      ResourceLocation $$4 = TexturedModel.CUBE_TOP_BOTTOM
         .get(Blocks.PODZOL)
         .updateTextures(p_176154_ -> p_176154_.put(TextureSlot.BOTTOM, $$0))
         .create(Blocks.PODZOL, this.modelOutput);
      this.createGrassLikeBlock(Blocks.PODZOL, $$4, $$2);
   }

   private void createGrassLikeBlock(Block p_124600_, ResourceLocation p_124601_, Variant p_124602_) {
      List<Variant> $$3 = Arrays.asList(createRotatedVariants(p_124601_));
      this.blockStateOutput
         .accept(
            MultiVariantGenerator.multiVariant(p_124600_)
               .with(PropertyDispatch.property(BlockStateProperties.SNOWY).select(true, p_124602_).select(false, $$3))
         );
   }

   private void createCocoa() {
      this.createSimpleFlatItemModel(Items.COCOA_BEANS);
      this.blockStateOutput
         .accept(
            MultiVariantGenerator.multiVariant(Blocks.COCOA)
               .with(
                  PropertyDispatch.property(BlockStateProperties.AGE_2)
                     .select(0, Variant.variant().with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(Blocks.COCOA, "_stage0")))
                     .select(1, Variant.variant().with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(Blocks.COCOA, "_stage1")))
                     .select(2, Variant.variant().with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(Blocks.COCOA, "_stage2")))
               )
               .with(createHorizontalFacingDispatchAlt())
         );
   }

   private void createDirtPath() {
      this.blockStateOutput.accept(createRotatedVariant(Blocks.DIRT_PATH, ModelLocationUtils.getModelLocation(Blocks.DIRT_PATH)));
   }

   private void createWeightedPressurePlate(Block p_124919_, Block p_124920_) {
      TextureMapping $$2 = TextureMapping.defaultTexture(p_124920_);
      ResourceLocation $$3 = ModelTemplates.PRESSURE_PLATE_UP.create(p_124919_, $$2, this.modelOutput);
      ResourceLocation $$4 = ModelTemplates.PRESSURE_PLATE_DOWN.create(p_124919_, $$2, this.modelOutput);
      this.blockStateOutput.accept(MultiVariantGenerator.multiVariant(p_124919_).with(createEmptyOrFullDispatch(BlockStateProperties.POWER, 1, $$4, $$3)));
   }

   private void createHopper() {
      ResourceLocation $$0 = ModelLocationUtils.getModelLocation(Blocks.HOPPER);
      ResourceLocation $$1 = ModelLocationUtils.getModelLocation(Blocks.HOPPER, "_side");
      this.createSimpleFlatItemModel(Items.HOPPER);
      this.blockStateOutput
         .accept(
            MultiVariantGenerator.multiVariant(Blocks.HOPPER)
               .with(
                  PropertyDispatch.property(BlockStateProperties.FACING_HOPPER)
                     .select(Direction.DOWN, Variant.variant().with(VariantProperties.MODEL, $$0))
                     .select(Direction.NORTH, Variant.variant().with(VariantProperties.MODEL, $$1))
                     .select(Direction.EAST, Variant.variant().with(VariantProperties.MODEL, $$1).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90))
                     .select(
                        Direction.SOUTH, Variant.variant().with(VariantProperties.MODEL, $$1).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180)
                     )
                     .select(
                        Direction.WEST, Variant.variant().with(VariantProperties.MODEL, $$1).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)
                     )
               )
         );
   }

   private void copyModel(Block p_124939_, Block p_124940_) {
      ResourceLocation $$2 = ModelLocationUtils.getModelLocation(p_124939_);
      this.blockStateOutput.accept(MultiVariantGenerator.multiVariant(p_124940_, Variant.variant().with(VariantProperties.MODEL, $$2)));
      this.delegateItemModel(p_124940_, $$2);
   }

   private void createIronBars() {
      ResourceLocation $$0 = ModelLocationUtils.getModelLocation(Blocks.IRON_BARS, "_post_ends");
      ResourceLocation $$1 = ModelLocationUtils.getModelLocation(Blocks.IRON_BARS, "_post");
      ResourceLocation $$2 = ModelLocationUtils.getModelLocation(Blocks.IRON_BARS, "_cap");
      ResourceLocation $$3 = ModelLocationUtils.getModelLocation(Blocks.IRON_BARS, "_cap_alt");
      ResourceLocation $$4 = ModelLocationUtils.getModelLocation(Blocks.IRON_BARS, "_side");
      ResourceLocation $$5 = ModelLocationUtils.getModelLocation(Blocks.IRON_BARS, "_side_alt");
      this.blockStateOutput
         .accept(
            MultiPartGenerator.multiPart(Blocks.IRON_BARS)
               .with(Variant.variant().with(VariantProperties.MODEL, $$0))
               .with(
                  Condition.condition()
                     .term(BlockStateProperties.NORTH, false)
                     .term(BlockStateProperties.EAST, false)
                     .term(BlockStateProperties.SOUTH, false)
                     .term(BlockStateProperties.WEST, false),
                  Variant.variant().with(VariantProperties.MODEL, $$1)
               )
               .with(
                  Condition.condition()
                     .term(BlockStateProperties.NORTH, true)
                     .term(BlockStateProperties.EAST, false)
                     .term(BlockStateProperties.SOUTH, false)
                     .term(BlockStateProperties.WEST, false),
                  Variant.variant().with(VariantProperties.MODEL, $$2)
               )
               .with(
                  Condition.condition()
                     .term(BlockStateProperties.NORTH, false)
                     .term(BlockStateProperties.EAST, true)
                     .term(BlockStateProperties.SOUTH, false)
                     .term(BlockStateProperties.WEST, false),
                  Variant.variant().with(VariantProperties.MODEL, $$2).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)
               )
               .with(
                  Condition.condition()
                     .term(BlockStateProperties.NORTH, false)
                     .term(BlockStateProperties.EAST, false)
                     .term(BlockStateProperties.SOUTH, true)
                     .term(BlockStateProperties.WEST, false),
                  Variant.variant().with(VariantProperties.MODEL, $$3)
               )
               .with(
                  Condition.condition()
                     .term(BlockStateProperties.NORTH, false)
                     .term(BlockStateProperties.EAST, false)
                     .term(BlockStateProperties.SOUTH, false)
                     .term(BlockStateProperties.WEST, true),
                  Variant.variant().with(VariantProperties.MODEL, $$3).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)
               )
               .with(Condition.condition().term(BlockStateProperties.NORTH, true), Variant.variant().with(VariantProperties.MODEL, $$4))
               .with(
                  Condition.condition().term(BlockStateProperties.EAST, true),
                  Variant.variant().with(VariantProperties.MODEL, $$4).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)
               )
               .with(Condition.condition().term(BlockStateProperties.SOUTH, true), Variant.variant().with(VariantProperties.MODEL, $$5))
               .with(
                  Condition.condition().term(BlockStateProperties.WEST, true),
                  Variant.variant().with(VariantProperties.MODEL, $$5).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)
               )
         );
      this.createSimpleFlatItemModel(Blocks.IRON_BARS);
   }

   private void createNonTemplateHorizontalBlock(Block p_125008_) {
      this.blockStateOutput
         .accept(
            MultiVariantGenerator.multiVariant(p_125008_, Variant.variant().with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(p_125008_)))
               .with(createHorizontalFacingDispatch())
         );
   }

   private void createLever() {
      ResourceLocation $$0 = ModelLocationUtils.getModelLocation(Blocks.LEVER);
      ResourceLocation $$1 = ModelLocationUtils.getModelLocation(Blocks.LEVER, "_on");
      this.createSimpleFlatItemModel(Blocks.LEVER);
      this.blockStateOutput
         .accept(
            MultiVariantGenerator.multiVariant(Blocks.LEVER)
               .with(createBooleanModelDispatch(BlockStateProperties.POWERED, $$0, $$1))
               .with(
                  PropertyDispatch.properties(BlockStateProperties.ATTACH_FACE, BlockStateProperties.HORIZONTAL_FACING)
                     .select(
                        AttachFace.CEILING,
                        Direction.NORTH,
                        Variant.variant()
                           .with(VariantProperties.X_ROT, VariantProperties.Rotation.R180)
                           .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180)
                     )
                     .select(
                        AttachFace.CEILING,
                        Direction.EAST,
                        Variant.variant()
                           .with(VariantProperties.X_ROT, VariantProperties.Rotation.R180)
                           .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)
                     )
                     .select(AttachFace.CEILING, Direction.SOUTH, Variant.variant().with(VariantProperties.X_ROT, VariantProperties.Rotation.R180))
                     .select(
                        AttachFace.CEILING,
                        Direction.WEST,
                        Variant.variant()
                           .with(VariantProperties.X_ROT, VariantProperties.Rotation.R180)
                           .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)
                     )
                     .select(AttachFace.FLOOR, Direction.NORTH, Variant.variant())
                     .select(AttachFace.FLOOR, Direction.EAST, Variant.variant().with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90))
                     .select(AttachFace.FLOOR, Direction.SOUTH, Variant.variant().with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180))
                     .select(AttachFace.FLOOR, Direction.WEST, Variant.variant().with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270))
                     .select(AttachFace.WALL, Direction.NORTH, Variant.variant().with(VariantProperties.X_ROT, VariantProperties.Rotation.R90))
                     .select(
                        AttachFace.WALL,
                        Direction.EAST,
                        Variant.variant()
                           .with(VariantProperties.X_ROT, VariantProperties.Rotation.R90)
                           .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)
                     )
                     .select(
                        AttachFace.WALL,
                        Direction.SOUTH,
                        Variant.variant()
                           .with(VariantProperties.X_ROT, VariantProperties.Rotation.R90)
                           .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180)
                     )
                     .select(
                        AttachFace.WALL,
                        Direction.WEST,
                        Variant.variant()
                           .with(VariantProperties.X_ROT, VariantProperties.Rotation.R90)
                           .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)
                     )
               )
         );
   }

   private void createLilyPad() {
      this.createSimpleFlatItemModel(Blocks.LILY_PAD);
      this.blockStateOutput.accept(createRotatedVariant(Blocks.LILY_PAD, ModelLocationUtils.getModelLocation(Blocks.LILY_PAD)));
   }

   private void createFrogspawnBlock() {
      this.createSimpleFlatItemModel(Blocks.FROGSPAWN);
      this.blockStateOutput.accept(createSimpleBlock(Blocks.FROGSPAWN, ModelLocationUtils.getModelLocation(Blocks.FROGSPAWN)));
   }

   private void createNetherPortalBlock() {
      this.blockStateOutput
         .accept(
            MultiVariantGenerator.multiVariant(Blocks.NETHER_PORTAL)
               .with(
                  PropertyDispatch.property(BlockStateProperties.HORIZONTAL_AXIS)
                     .select(
                        Direction.Axis.X, Variant.variant().with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(Blocks.NETHER_PORTAL, "_ns"))
                     )
                     .select(
                        Direction.Axis.Z, Variant.variant().with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(Blocks.NETHER_PORTAL, "_ew"))
                     )
               )
         );
   }

   private void createNetherrack() {
      ResourceLocation $$0 = TexturedModel.CUBE.create(Blocks.NETHERRACK, this.modelOutput);
      this.blockStateOutput
         .accept(
            MultiVariantGenerator.multiVariant(
               Blocks.NETHERRACK,
               Variant.variant().with(VariantProperties.MODEL, $$0),
               Variant.variant().with(VariantProperties.MODEL, $$0).with(VariantProperties.X_ROT, VariantProperties.Rotation.R90),
               Variant.variant().with(VariantProperties.MODEL, $$0).with(VariantProperties.X_ROT, VariantProperties.Rotation.R180),
               Variant.variant().with(VariantProperties.MODEL, $$0).with(VariantProperties.X_ROT, VariantProperties.Rotation.R270),
               Variant.variant().with(VariantProperties.MODEL, $$0).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90),
               Variant.variant()
                  .with(VariantProperties.MODEL, $$0)
                  .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)
                  .with(VariantProperties.X_ROT, VariantProperties.Rotation.R90),
               Variant.variant()
                  .with(VariantProperties.MODEL, $$0)
                  .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)
                  .with(VariantProperties.X_ROT, VariantProperties.Rotation.R180),
               Variant.variant()
                  .with(VariantProperties.MODEL, $$0)
                  .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)
                  .with(VariantProperties.X_ROT, VariantProperties.Rotation.R270),
               Variant.variant().with(VariantProperties.MODEL, $$0).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180),
               Variant.variant()
                  .with(VariantProperties.MODEL, $$0)
                  .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180)
                  .with(VariantProperties.X_ROT, VariantProperties.Rotation.R90),
               Variant.variant()
                  .with(VariantProperties.MODEL, $$0)
                  .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180)
                  .with(VariantProperties.X_ROT, VariantProperties.Rotation.R180),
               Variant.variant()
                  .with(VariantProperties.MODEL, $$0)
                  .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180)
                  .with(VariantProperties.X_ROT, VariantProperties.Rotation.R270),
               Variant.variant().with(VariantProperties.MODEL, $$0).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270),
               Variant.variant()
                  .with(VariantProperties.MODEL, $$0)
                  .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)
                  .with(VariantProperties.X_ROT, VariantProperties.Rotation.R90),
               Variant.variant()
                  .with(VariantProperties.MODEL, $$0)
                  .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)
                  .with(VariantProperties.X_ROT, VariantProperties.Rotation.R180),
               Variant.variant()
                  .with(VariantProperties.MODEL, $$0)
                  .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)
                  .with(VariantProperties.X_ROT, VariantProperties.Rotation.R270)
            )
         );
   }

   private void createObserver() {
      ResourceLocation $$0 = ModelLocationUtils.getModelLocation(Blocks.OBSERVER);
      ResourceLocation $$1 = ModelLocationUtils.getModelLocation(Blocks.OBSERVER, "_on");
      this.blockStateOutput
         .accept(
            MultiVariantGenerator.multiVariant(Blocks.OBSERVER)
               .with(createBooleanModelDispatch(BlockStateProperties.POWERED, $$1, $$0))
               .with(createFacingDispatch())
         );
   }

   private void createPistons() {
      TextureMapping $$0 = new TextureMapping()
         .put(TextureSlot.BOTTOM, TextureMapping.getBlockTexture(Blocks.PISTON, "_bottom"))
         .put(TextureSlot.SIDE, TextureMapping.getBlockTexture(Blocks.PISTON, "_side"));
      ResourceLocation $$1 = TextureMapping.getBlockTexture(Blocks.PISTON, "_top_sticky");
      ResourceLocation $$2 = TextureMapping.getBlockTexture(Blocks.PISTON, "_top");
      TextureMapping $$3 = $$0.copyAndUpdate(TextureSlot.PLATFORM, $$1);
      TextureMapping $$4 = $$0.copyAndUpdate(TextureSlot.PLATFORM, $$2);
      ResourceLocation $$5 = ModelLocationUtils.getModelLocation(Blocks.PISTON, "_base");
      this.createPistonVariant(Blocks.PISTON, $$5, $$4);
      this.createPistonVariant(Blocks.STICKY_PISTON, $$5, $$3);
      ResourceLocation $$6 = ModelTemplates.CUBE_BOTTOM_TOP
         .createWithSuffix(Blocks.PISTON, "_inventory", $$0.copyAndUpdate(TextureSlot.TOP, $$2), this.modelOutput);
      ResourceLocation $$7 = ModelTemplates.CUBE_BOTTOM_TOP
         .createWithSuffix(Blocks.STICKY_PISTON, "_inventory", $$0.copyAndUpdate(TextureSlot.TOP, $$1), this.modelOutput);
      this.delegateItemModel(Blocks.PISTON, $$6);
      this.delegateItemModel(Blocks.STICKY_PISTON, $$7);
   }

   private void createPistonVariant(Block p_124604_, ResourceLocation p_124605_, TextureMapping p_124606_) {
      ResourceLocation $$3 = ModelTemplates.PISTON.create(p_124604_, p_124606_, this.modelOutput);
      this.blockStateOutput
         .accept(
            MultiVariantGenerator.multiVariant(p_124604_)
               .with(createBooleanModelDispatch(BlockStateProperties.EXTENDED, p_124605_, $$3))
               .with(createFacingDispatch())
         );
   }

   private void createPistonHeads() {
      TextureMapping $$0 = new TextureMapping()
         .put(TextureSlot.UNSTICKY, TextureMapping.getBlockTexture(Blocks.PISTON, "_top"))
         .put(TextureSlot.SIDE, TextureMapping.getBlockTexture(Blocks.PISTON, "_side"));
      TextureMapping $$1 = $$0.copyAndUpdate(TextureSlot.PLATFORM, TextureMapping.getBlockTexture(Blocks.PISTON, "_top_sticky"));
      TextureMapping $$2 = $$0.copyAndUpdate(TextureSlot.PLATFORM, TextureMapping.getBlockTexture(Blocks.PISTON, "_top"));
      this.blockStateOutput
         .accept(
            MultiVariantGenerator.multiVariant(Blocks.PISTON_HEAD)
               .with(
                  PropertyDispatch.properties(BlockStateProperties.SHORT, BlockStateProperties.PISTON_TYPE)
                     .select(
                        false,
                        PistonType.DEFAULT,
                        Variant.variant()
                           .with(VariantProperties.MODEL, ModelTemplates.PISTON_HEAD.createWithSuffix(Blocks.PISTON, "_head", $$2, this.modelOutput))
                     )
                     .select(
                        false,
                        PistonType.STICKY,
                        Variant.variant()
                           .with(VariantProperties.MODEL, ModelTemplates.PISTON_HEAD.createWithSuffix(Blocks.PISTON, "_head_sticky", $$1, this.modelOutput))
                     )
                     .select(
                        true,
                        PistonType.DEFAULT,
                        Variant.variant()
                           .with(
                              VariantProperties.MODEL, ModelTemplates.PISTON_HEAD_SHORT.createWithSuffix(Blocks.PISTON, "_head_short", $$2, this.modelOutput)
                           )
                     )
                     .select(
                        true,
                        PistonType.STICKY,
                        Variant.variant()
                           .with(
                              VariantProperties.MODEL,
                              ModelTemplates.PISTON_HEAD_SHORT.createWithSuffix(Blocks.PISTON, "_head_short_sticky", $$1, this.modelOutput)
                           )
                     )
               )
               .with(createFacingDispatch())
         );
   }

   private void createTrialSpawner() {
      Block $$0 = Blocks.TRIAL_SPAWNER;
      TextureMapping $$1 = TextureMapping.trialSpawner($$0, "_side_inactive", "_top_inactive");
      TextureMapping $$2 = TextureMapping.trialSpawner($$0, "_side_active", "_top_active");
      TextureMapping $$3 = TextureMapping.trialSpawner($$0, "_side_active", "_top_ejecting_reward");
      TextureMapping $$4 = TextureMapping.trialSpawner($$0, "_side_inactive_ominous", "_top_inactive_ominous");
      TextureMapping $$5 = TextureMapping.trialSpawner($$0, "_side_active_ominous", "_top_active_ominous");
      TextureMapping $$6 = TextureMapping.trialSpawner($$0, "_side_active_ominous", "_top_ejecting_reward_ominous");
      ResourceLocation $$7 = ModelTemplates.CUBE_BOTTOM_TOP_INNER_FACES.create($$0, $$1, this.modelOutput);
      ResourceLocation $$8 = ModelTemplates.CUBE_BOTTOM_TOP_INNER_FACES.createWithSuffix($$0, "_active", $$2, this.modelOutput);
      ResourceLocation $$9 = ModelTemplates.CUBE_BOTTOM_TOP_INNER_FACES.createWithSuffix($$0, "_ejecting_reward", $$3, this.modelOutput);
      ResourceLocation $$10 = ModelTemplates.CUBE_BOTTOM_TOP_INNER_FACES.createWithSuffix($$0, "_inactive_ominous", $$4, this.modelOutput);
      ResourceLocation $$11 = ModelTemplates.CUBE_BOTTOM_TOP_INNER_FACES.createWithSuffix($$0, "_active_ominous", $$5, this.modelOutput);
      ResourceLocation $$12 = ModelTemplates.CUBE_BOTTOM_TOP_INNER_FACES.createWithSuffix($$0, "_ejecting_reward_ominous", $$6, this.modelOutput);
      this.delegateItemModel($$0, $$7);
      this.blockStateOutput
         .accept(
            MultiVariantGenerator.multiVariant($$0)
               .with(PropertyDispatch.properties(BlockStateProperties.TRIAL_SPAWNER_STATE, BlockStateProperties.OMINOUS).generate((p_337483_, p_337484_) -> {
                  return switch (p_337483_) {
                     case INACTIVE, COOLDOWN -> Variant.variant().with(VariantProperties.MODEL, p_337484_ ? $$10 : $$7);
                     case WAITING_FOR_PLAYERS, ACTIVE, WAITING_FOR_REWARD_EJECTION -> Variant.variant().with(VariantProperties.MODEL, p_337484_ ? $$11 : $$8);
                     case EJECTING_REWARD -> Variant.variant().with(VariantProperties.MODEL, p_337484_ ? $$12 : $$9);
                  };
               }))
         );
   }

   private void createVault() {
      Block $$0 = Blocks.VAULT;
      TextureMapping $$1 = TextureMapping.vault($$0, "_front_off", "_side_off", "_top", "_bottom");
      TextureMapping $$2 = TextureMapping.vault($$0, "_front_on", "_side_on", "_top", "_bottom");
      TextureMapping $$3 = TextureMapping.vault($$0, "_front_ejecting", "_side_on", "_top", "_bottom");
      TextureMapping $$4 = TextureMapping.vault($$0, "_front_ejecting", "_side_on", "_top_ejecting", "_bottom");
      ResourceLocation $$5 = ModelTemplates.VAULT.create($$0, $$1, this.modelOutput);
      ResourceLocation $$6 = ModelTemplates.VAULT.createWithSuffix($$0, "_active", $$2, this.modelOutput);
      ResourceLocation $$7 = ModelTemplates.VAULT.createWithSuffix($$0, "_unlocking", $$3, this.modelOutput);
      ResourceLocation $$8 = ModelTemplates.VAULT.createWithSuffix($$0, "_ejecting_reward", $$4, this.modelOutput);
      TextureMapping $$9 = TextureMapping.vault($$0, "_front_off_ominous", "_side_off_ominous", "_top_ominous", "_bottom_ominous");
      TextureMapping $$10 = TextureMapping.vault($$0, "_front_on_ominous", "_side_on_ominous", "_top_ominous", "_bottom_ominous");
      TextureMapping $$11 = TextureMapping.vault($$0, "_front_ejecting_ominous", "_side_on_ominous", "_top_ominous", "_bottom_ominous");
      TextureMapping $$12 = TextureMapping.vault($$0, "_front_ejecting_ominous", "_side_on_ominous", "_top_ejecting_ominous", "_bottom_ominous");
      ResourceLocation $$13 = ModelTemplates.VAULT.createWithSuffix($$0, "_ominous", $$9, this.modelOutput);
      ResourceLocation $$14 = ModelTemplates.VAULT.createWithSuffix($$0, "_active_ominous", $$10, this.modelOutput);
      ResourceLocation $$15 = ModelTemplates.VAULT.createWithSuffix($$0, "_unlocking_ominous", $$11, this.modelOutput);
      ResourceLocation $$16 = ModelTemplates.VAULT.createWithSuffix($$0, "_ejecting_reward_ominous", $$12, this.modelOutput);
      this.delegateItemModel($$0, $$5);
      this.blockStateOutput
         .accept(
            MultiVariantGenerator.multiVariant($$0)
               .with(createHorizontalFacingDispatch())
               .with(PropertyDispatch.properties(VaultBlock.STATE, VaultBlock.OMINOUS).generate((p_337475_, p_337476_) -> {
                  return switch (p_337475_) {
                     case INACTIVE -> Variant.variant().with(VariantProperties.MODEL, p_337476_ ? $$13 : $$5);
                     case ACTIVE -> Variant.variant().with(VariantProperties.MODEL, p_337476_ ? $$14 : $$6);
                     case UNLOCKING -> Variant.variant().with(VariantProperties.MODEL, p_337476_ ? $$15 : $$7);
                     case EJECTING -> Variant.variant().with(VariantProperties.MODEL, p_337476_ ? $$16 : $$8);
                  };
               }))
         );
   }

   private void createSculkSensor() {
      ResourceLocation $$0 = ModelLocationUtils.getModelLocation(Blocks.SCULK_SENSOR, "_inactive");
      ResourceLocation $$1 = ModelLocationUtils.getModelLocation(Blocks.SCULK_SENSOR, "_active");
      this.delegateItemModel(Blocks.SCULK_SENSOR, $$0);
      this.blockStateOutput
         .accept(
            MultiVariantGenerator.multiVariant(Blocks.SCULK_SENSOR)
               .with(
                  PropertyDispatch.property(BlockStateProperties.SCULK_SENSOR_PHASE)
                     .generate(
                        p_284650_ -> Variant.variant()
                           .with(VariantProperties.MODEL, p_284650_ != SculkSensorPhase.ACTIVE && p_284650_ != SculkSensorPhase.COOLDOWN ? $$0 : $$1)
                     )
               )
         );
   }

   private void createCalibratedSculkSensor() {
      ResourceLocation $$0 = ModelLocationUtils.getModelLocation(Blocks.CALIBRATED_SCULK_SENSOR, "_inactive");
      ResourceLocation $$1 = ModelLocationUtils.getModelLocation(Blocks.CALIBRATED_SCULK_SENSOR, "_active");
      this.delegateItemModel(Blocks.CALIBRATED_SCULK_SENSOR, $$0);
      this.blockStateOutput
         .accept(
            MultiVariantGenerator.multiVariant(Blocks.CALIBRATED_SCULK_SENSOR)
               .with(
                  PropertyDispatch.property(BlockStateProperties.SCULK_SENSOR_PHASE)
                     .generate(
                        p_284647_ -> Variant.variant()
                           .with(VariantProperties.MODEL, p_284647_ != SculkSensorPhase.ACTIVE && p_284647_ != SculkSensorPhase.COOLDOWN ? $$0 : $$1)
                     )
               )
               .with(createHorizontalFacingDispatch())
         );
   }

   private void createSculkShrieker() {
      ResourceLocation $$0 = ModelTemplates.SCULK_SHRIEKER.create(Blocks.SCULK_SHRIEKER, TextureMapping.sculkShrieker(false), this.modelOutput);
      ResourceLocation $$1 = ModelTemplates.SCULK_SHRIEKER
         .createWithSuffix(Blocks.SCULK_SHRIEKER, "_can_summon", TextureMapping.sculkShrieker(true), this.modelOutput);
      this.delegateItemModel(Blocks.SCULK_SHRIEKER, $$0);
      this.blockStateOutput
         .accept(MultiVariantGenerator.multiVariant(Blocks.SCULK_SHRIEKER).with(createBooleanModelDispatch(BlockStateProperties.CAN_SUMMON, $$1, $$0)));
   }

   private void createScaffolding() {
      ResourceLocation $$0 = ModelLocationUtils.getModelLocation(Blocks.SCAFFOLDING, "_stable");
      ResourceLocation $$1 = ModelLocationUtils.getModelLocation(Blocks.SCAFFOLDING, "_unstable");
      this.delegateItemModel(Blocks.SCAFFOLDING, $$0);
      this.blockStateOutput
         .accept(MultiVariantGenerator.multiVariant(Blocks.SCAFFOLDING).with(createBooleanModelDispatch(BlockStateProperties.BOTTOM, $$1, $$0)));
   }

   private void createCaveVines() {
      ResourceLocation $$0 = this.createSuffixedVariant(Blocks.CAVE_VINES, "", ModelTemplates.CROSS, TextureMapping::cross);
      ResourceLocation $$1 = this.createSuffixedVariant(Blocks.CAVE_VINES, "_lit", ModelTemplates.CROSS, TextureMapping::cross);
      this.blockStateOutput
         .accept(MultiVariantGenerator.multiVariant(Blocks.CAVE_VINES).with(createBooleanModelDispatch(BlockStateProperties.BERRIES, $$1, $$0)));
      ResourceLocation $$2 = this.createSuffixedVariant(Blocks.CAVE_VINES_PLANT, "", ModelTemplates.CROSS, TextureMapping::cross);
      ResourceLocation $$3 = this.createSuffixedVariant(Blocks.CAVE_VINES_PLANT, "_lit", ModelTemplates.CROSS, TextureMapping::cross);
      this.blockStateOutput
         .accept(MultiVariantGenerator.multiVariant(Blocks.CAVE_VINES_PLANT).with(createBooleanModelDispatch(BlockStateProperties.BERRIES, $$3, $$2)));
   }

   private void createRedstoneLamp() {
      ResourceLocation $$0 = TexturedModel.CUBE.create(Blocks.REDSTONE_LAMP, this.modelOutput);
      ResourceLocation $$1 = this.createSuffixedVariant(Blocks.REDSTONE_LAMP, "_on", ModelTemplates.CUBE_ALL, TextureMapping::cube);
      this.blockStateOutput
         .accept(MultiVariantGenerator.multiVariant(Blocks.REDSTONE_LAMP).with(createBooleanModelDispatch(BlockStateProperties.LIT, $$1, $$0)));
   }

   private void createNormalTorch(Block p_124951_, Block p_124952_) {
      TextureMapping $$2 = TextureMapping.torch(p_124951_);
      this.blockStateOutput.accept(createSimpleBlock(p_124951_, ModelTemplates.TORCH.create(p_124951_, $$2, this.modelOutput)));
      this.blockStateOutput
         .accept(
            MultiVariantGenerator.multiVariant(
                  p_124952_, Variant.variant().with(VariantProperties.MODEL, ModelTemplates.WALL_TORCH.create(p_124952_, $$2, this.modelOutput))
               )
               .with(createTorchHorizontalDispatch())
         );
      this.createSimpleFlatItemModel(p_124951_);
      this.skipAutoItemBlock(p_124952_);
   }

   private void createRedstoneTorch() {
      TextureMapping $$0 = TextureMapping.torch(Blocks.REDSTONE_TORCH);
      TextureMapping $$1 = TextureMapping.torch(TextureMapping.getBlockTexture(Blocks.REDSTONE_TORCH, "_off"));
      ResourceLocation $$2 = ModelTemplates.TORCH.create(Blocks.REDSTONE_TORCH, $$0, this.modelOutput);
      ResourceLocation $$3 = ModelTemplates.TORCH.createWithSuffix(Blocks.REDSTONE_TORCH, "_off", $$1, this.modelOutput);
      this.blockStateOutput
         .accept(MultiVariantGenerator.multiVariant(Blocks.REDSTONE_TORCH).with(createBooleanModelDispatch(BlockStateProperties.LIT, $$2, $$3)));
      ResourceLocation $$4 = ModelTemplates.WALL_TORCH.create(Blocks.REDSTONE_WALL_TORCH, $$0, this.modelOutput);
      ResourceLocation $$5 = ModelTemplates.WALL_TORCH.createWithSuffix(Blocks.REDSTONE_WALL_TORCH, "_off", $$1, this.modelOutput);
      this.blockStateOutput
         .accept(
            MultiVariantGenerator.multiVariant(Blocks.REDSTONE_WALL_TORCH)
               .with(createBooleanModelDispatch(BlockStateProperties.LIT, $$4, $$5))
               .with(createTorchHorizontalDispatch())
         );
      this.createSimpleFlatItemModel(Blocks.REDSTONE_TORCH);
      this.skipAutoItemBlock(Blocks.REDSTONE_WALL_TORCH);
   }

   private void createRepeater() {
      this.createSimpleFlatItemModel(Items.REPEATER);
      this.blockStateOutput
         .accept(
            MultiVariantGenerator.multiVariant(Blocks.REPEATER)
               .with(
                  PropertyDispatch.properties(BlockStateProperties.DELAY, BlockStateProperties.LOCKED, BlockStateProperties.POWERED)
                     .generate((p_337485_, p_337486_, p_337487_) -> {
                        StringBuilder $$3 = new StringBuilder();
                        $$3.append('_').append(p_337485_).append("tick");
                        if (p_337487_) {
                           $$3.append("_on");
                        }

                        if (p_337486_) {
                           $$3.append("_locked");
                        }

                        return Variant.variant().with(VariantProperties.MODEL, TextureMapping.getBlockTexture(Blocks.REPEATER, $$3.toString()));
                     })
               )
               .with(createHorizontalFacingDispatchAlt())
         );
   }

   private void createSeaPickle() {
      this.createSimpleFlatItemModel(Items.SEA_PICKLE);
      this.blockStateOutput
         .accept(
            MultiVariantGenerator.multiVariant(Blocks.SEA_PICKLE)
               .with(
                  PropertyDispatch.properties(BlockStateProperties.PICKLES, BlockStateProperties.WATERLOGGED)
                     .select(1, false, Arrays.asList(createRotatedVariants(ModelLocationUtils.decorateBlockModelLocation("dead_sea_pickle"))))
                     .select(2, false, Arrays.asList(createRotatedVariants(ModelLocationUtils.decorateBlockModelLocation("two_dead_sea_pickles"))))
                     .select(3, false, Arrays.asList(createRotatedVariants(ModelLocationUtils.decorateBlockModelLocation("three_dead_sea_pickles"))))
                     .select(4, false, Arrays.asList(createRotatedVariants(ModelLocationUtils.decorateBlockModelLocation("four_dead_sea_pickles"))))
                     .select(1, true, Arrays.asList(createRotatedVariants(ModelLocationUtils.decorateBlockModelLocation("sea_pickle"))))
                     .select(2, true, Arrays.asList(createRotatedVariants(ModelLocationUtils.decorateBlockModelLocation("two_sea_pickles"))))
                     .select(3, true, Arrays.asList(createRotatedVariants(ModelLocationUtils.decorateBlockModelLocation("three_sea_pickles"))))
                     .select(4, true, Arrays.asList(createRotatedVariants(ModelLocationUtils.decorateBlockModelLocation("four_sea_pickles"))))
               )
         );
   }

   private void createSnowBlocks() {
      TextureMapping $$0 = TextureMapping.cube(Blocks.SNOW);
      ResourceLocation $$1 = ModelTemplates.CUBE_ALL.create(Blocks.SNOW_BLOCK, $$0, this.modelOutput);
      this.blockStateOutput
         .accept(
            MultiVariantGenerator.multiVariant(Blocks.SNOW)
               .with(
                  PropertyDispatch.property(BlockStateProperties.LAYERS)
                     .generate(
                        p_176151_ -> Variant.variant()
                           .with(VariantProperties.MODEL, p_176151_ < 8 ? ModelLocationUtils.getModelLocation(Blocks.SNOW, "_height" + p_176151_ * 2) : $$1)
                     )
               )
         );
      this.delegateItemModel(Blocks.SNOW, ModelLocationUtils.getModelLocation(Blocks.SNOW, "_height2"));
      this.blockStateOutput.accept(createSimpleBlock(Blocks.SNOW_BLOCK, $$1));
   }

   private void createStonecutter() {
      this.blockStateOutput
         .accept(
            MultiVariantGenerator.multiVariant(
                  Blocks.STONECUTTER, Variant.variant().with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(Blocks.STONECUTTER))
               )
               .with(createHorizontalFacingDispatch())
         );
   }

   private void createStructureBlock() {
      ResourceLocation $$0 = TexturedModel.CUBE.create(Blocks.STRUCTURE_BLOCK, this.modelOutput);
      this.delegateItemModel(Blocks.STRUCTURE_BLOCK, $$0);
      this.blockStateOutput
         .accept(
            MultiVariantGenerator.multiVariant(Blocks.STRUCTURE_BLOCK)
               .with(
                  PropertyDispatch.property(BlockStateProperties.STRUCTUREBLOCK_MODE)
                     .generate(
                        p_176115_ -> Variant.variant()
                           .with(
                              VariantProperties.MODEL,
                              this.createSuffixedVariant(
                                 Blocks.STRUCTURE_BLOCK, "_" + p_176115_.getSerializedName(), ModelTemplates.CUBE_ALL, TextureMapping::cube
                              )
                           )
                     )
               )
         );
   }

   private void createSweetBerryBush() {
      this.createSimpleFlatItemModel(Items.SWEET_BERRIES);
      this.blockStateOutput
         .accept(
            MultiVariantGenerator.multiVariant(Blocks.SWEET_BERRY_BUSH)
               .with(
                  PropertyDispatch.property(BlockStateProperties.AGE_3)
                     .generate(
                        p_176132_ -> Variant.variant()
                           .with(
                              VariantProperties.MODEL,
                              this.createSuffixedVariant(Blocks.SWEET_BERRY_BUSH, "_stage" + p_176132_, ModelTemplates.CROSS, TextureMapping::cross)
                           )
                     )
               )
         );
   }

   private void createTripwire() {
      this.createSimpleFlatItemModel(Items.STRING);
      this.blockStateOutput
         .accept(
            MultiVariantGenerator.multiVariant(Blocks.TRIPWIRE)
               .with(
                  PropertyDispatch.properties(
                        BlockStateProperties.ATTACHED,
                        BlockStateProperties.EAST,
                        BlockStateProperties.NORTH,
                        BlockStateProperties.SOUTH,
                        BlockStateProperties.WEST
                     )
                     .select(
                        false,
                        false,
                        false,
                        false,
                        false,
                        Variant.variant().with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(Blocks.TRIPWIRE, "_ns"))
                     )
                     .select(
                        false,
                        true,
                        false,
                        false,
                        false,
                        Variant.variant()
                           .with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(Blocks.TRIPWIRE, "_n"))
                           .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)
                     )
                     .select(
                        false,
                        false,
                        true,
                        false,
                        false,
                        Variant.variant().with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(Blocks.TRIPWIRE, "_n"))
                     )
                     .select(
                        false,
                        false,
                        false,
                        true,
                        false,
                        Variant.variant()
                           .with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(Blocks.TRIPWIRE, "_n"))
                           .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180)
                     )
                     .select(
                        false,
                        false,
                        false,
                        false,
                        true,
                        Variant.variant()
                           .with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(Blocks.TRIPWIRE, "_n"))
                           .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)
                     )
                     .select(
                        false,
                        true,
                        true,
                        false,
                        false,
                        Variant.variant().with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(Blocks.TRIPWIRE, "_ne"))
                     )
                     .select(
                        false,
                        true,
                        false,
                        true,
                        false,
                        Variant.variant()
                           .with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(Blocks.TRIPWIRE, "_ne"))
                           .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)
                     )
                     .select(
                        false,
                        false,
                        false,
                        true,
                        true,
                        Variant.variant()
                           .with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(Blocks.TRIPWIRE, "_ne"))
                           .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180)
                     )
                     .select(
                        false,
                        false,
                        true,
                        false,
                        true,
                        Variant.variant()
                           .with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(Blocks.TRIPWIRE, "_ne"))
                           .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)
                     )
                     .select(
                        false,
                        false,
                        true,
                        true,
                        false,
                        Variant.variant().with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(Blocks.TRIPWIRE, "_ns"))
                     )
                     .select(
                        false,
                        true,
                        false,
                        false,
                        true,
                        Variant.variant()
                           .with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(Blocks.TRIPWIRE, "_ns"))
                           .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)
                     )
                     .select(
                        false,
                        true,
                        true,
                        true,
                        false,
                        Variant.variant().with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(Blocks.TRIPWIRE, "_nse"))
                     )
                     .select(
                        false,
                        true,
                        false,
                        true,
                        true,
                        Variant.variant()
                           .with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(Blocks.TRIPWIRE, "_nse"))
                           .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)
                     )
                     .select(
                        false,
                        false,
                        true,
                        true,
                        true,
                        Variant.variant()
                           .with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(Blocks.TRIPWIRE, "_nse"))
                           .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180)
                     )
                     .select(
                        false,
                        true,
                        true,
                        false,
                        true,
                        Variant.variant()
                           .with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(Blocks.TRIPWIRE, "_nse"))
                           .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)
                     )
                     .select(
                        false,
                        true,
                        true,
                        true,
                        true,
                        Variant.variant().with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(Blocks.TRIPWIRE, "_nsew"))
                     )
                     .select(
                        true,
                        false,
                        false,
                        false,
                        false,
                        Variant.variant().with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(Blocks.TRIPWIRE, "_attached_ns"))
                     )
                     .select(
                        true,
                        false,
                        true,
                        false,
                        false,
                        Variant.variant().with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(Blocks.TRIPWIRE, "_attached_n"))
                     )
                     .select(
                        true,
                        false,
                        false,
                        true,
                        false,
                        Variant.variant()
                           .with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(Blocks.TRIPWIRE, "_attached_n"))
                           .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180)
                     )
                     .select(
                        true,
                        true,
                        false,
                        false,
                        false,
                        Variant.variant()
                           .with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(Blocks.TRIPWIRE, "_attached_n"))
                           .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)
                     )
                     .select(
                        true,
                        false,
                        false,
                        false,
                        true,
                        Variant.variant()
                           .with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(Blocks.TRIPWIRE, "_attached_n"))
                           .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)
                     )
                     .select(
                        true,
                        true,
                        true,
                        false,
                        false,
                        Variant.variant().with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(Blocks.TRIPWIRE, "_attached_ne"))
                     )
                     .select(
                        true,
                        true,
                        false,
                        true,
                        false,
                        Variant.variant()
                           .with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(Blocks.TRIPWIRE, "_attached_ne"))
                           .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)
                     )
                     .select(
                        true,
                        false,
                        false,
                        true,
                        true,
                        Variant.variant()
                           .with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(Blocks.TRIPWIRE, "_attached_ne"))
                           .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180)
                     )
                     .select(
                        true,
                        false,
                        true,
                        false,
                        true,
                        Variant.variant()
                           .with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(Blocks.TRIPWIRE, "_attached_ne"))
                           .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)
                     )
                     .select(
                        true,
                        false,
                        true,
                        true,
                        false,
                        Variant.variant().with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(Blocks.TRIPWIRE, "_attached_ns"))
                     )
                     .select(
                        true,
                        true,
                        false,
                        false,
                        true,
                        Variant.variant()
                           .with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(Blocks.TRIPWIRE, "_attached_ns"))
                           .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)
                     )
                     .select(
                        true,
                        true,
                        true,
                        true,
                        false,
                        Variant.variant().with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(Blocks.TRIPWIRE, "_attached_nse"))
                     )
                     .select(
                        true,
                        true,
                        false,
                        true,
                        true,
                        Variant.variant()
                           .with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(Blocks.TRIPWIRE, "_attached_nse"))
                           .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)
                     )
                     .select(
                        true,
                        false,
                        true,
                        true,
                        true,
                        Variant.variant()
                           .with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(Blocks.TRIPWIRE, "_attached_nse"))
                           .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180)
                     )
                     .select(
                        true,
                        true,
                        true,
                        false,
                        true,
                        Variant.variant()
                           .with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(Blocks.TRIPWIRE, "_attached_nse"))
                           .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)
                     )
                     .select(
                        true,
                        true,
                        true,
                        true,
                        true,
                        Variant.variant().with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(Blocks.TRIPWIRE, "_attached_nsew"))
                     )
               )
         );
   }

   private void createTripwireHook() {
      this.createSimpleFlatItemModel(Blocks.TRIPWIRE_HOOK);
      this.blockStateOutput
         .accept(
            MultiVariantGenerator.multiVariant(Blocks.TRIPWIRE_HOOK)
               .with(
                  PropertyDispatch.properties(BlockStateProperties.ATTACHED, BlockStateProperties.POWERED)
                     .generate(
                        (p_176124_, p_176125_) -> Variant.variant()
                           .with(
                              VariantProperties.MODEL,
                              TextureMapping.getBlockTexture(Blocks.TRIPWIRE_HOOK, (p_176124_ ? "_attached" : "") + (p_176125_ ? "_on" : ""))
                           )
                     )
               )
               .with(createHorizontalFacingDispatch())
         );
   }

   private ResourceLocation createTurtleEggModel(int p_124514_, String p_124515_, TextureMapping p_124516_) {
      switch (p_124514_) {
         case 1:
            return ModelTemplates.TURTLE_EGG.create(ModelLocationUtils.decorateBlockModelLocation(p_124515_ + "turtle_egg"), p_124516_, this.modelOutput);
         case 2:
            return ModelTemplates.TWO_TURTLE_EGGS
               .create(ModelLocationUtils.decorateBlockModelLocation("two_" + p_124515_ + "turtle_eggs"), p_124516_, this.modelOutput);
         case 3:
            return ModelTemplates.THREE_TURTLE_EGGS
               .create(ModelLocationUtils.decorateBlockModelLocation("three_" + p_124515_ + "turtle_eggs"), p_124516_, this.modelOutput);
         case 4:
            return ModelTemplates.FOUR_TURTLE_EGGS
               .create(ModelLocationUtils.decorateBlockModelLocation("four_" + p_124515_ + "turtle_eggs"), p_124516_, this.modelOutput);
         default:
            throw new UnsupportedOperationException();
      }
   }

   private ResourceLocation createTurtleEggModel(Integer p_124677_, Integer p_124678_) {
      switch (p_124678_) {
         case 0:
            return this.createTurtleEggModel(p_124677_, "", TextureMapping.cube(TextureMapping.getBlockTexture(Blocks.TURTLE_EGG)));
         case 1:
            return this.createTurtleEggModel(
               p_124677_, "slightly_cracked_", TextureMapping.cube(TextureMapping.getBlockTexture(Blocks.TURTLE_EGG, "_slightly_cracked"))
            );
         case 2:
            return this.createTurtleEggModel(
               p_124677_, "very_cracked_", TextureMapping.cube(TextureMapping.getBlockTexture(Blocks.TURTLE_EGG, "_very_cracked"))
            );
         default:
            throw new UnsupportedOperationException();
      }
   }

   private void createTurtleEgg() {
      this.createSimpleFlatItemModel(Items.TURTLE_EGG);
      this.blockStateOutput
         .accept(
            MultiVariantGenerator.multiVariant(Blocks.TURTLE_EGG)
               .with(
                  PropertyDispatch.properties(BlockStateProperties.EGGS, BlockStateProperties.HATCH)
                     .generateList((p_176185_, p_176186_) -> Arrays.asList(createRotatedVariants(this.createTurtleEggModel(p_176185_, p_176186_))))
               )
         );
   }

   private void createSnifferEgg() {
      this.createSimpleFlatItemModel(Items.SNIFFER_EGG);
      Function<Integer, ResourceLocation> $$0 = p_278206_ -> {
         String $$1 = switch (p_278206_) {
            case 1 -> "_slightly_cracked";
            case 2 -> "_very_cracked";
            default -> "_not_cracked";
         };
         TextureMapping $$2 = TextureMapping.snifferEgg($$1);
         return ModelTemplates.SNIFFER_EGG.createWithSuffix(Blocks.SNIFFER_EGG, $$1, $$2, this.modelOutput);
      };
      this.blockStateOutput
         .accept(
            MultiVariantGenerator.multiVariant(Blocks.SNIFFER_EGG)
               .with(
                  PropertyDispatch.property(SnifferEggBlock.HATCH).generate(p_277261_ -> Variant.variant().with(VariantProperties.MODEL, $$0.apply(p_277261_)))
               )
         );
   }

   private void createMultiface(Block p_176086_) {
      this.createSimpleFlatItemModel(p_176086_);
      ResourceLocation $$1 = ModelLocationUtils.getModelLocation(p_176086_);
      MultiPartGenerator $$2 = MultiPartGenerator.multiPart(p_176086_);
      Condition.TerminalCondition $$3 = Util.make(Condition.condition(), p_236295_ -> MULTIFACE_GENERATOR.stream().map(Pair::getFirst).forEach(p_236299_ -> {
         if (p_176086_.defaultBlockState().hasProperty(p_236299_)) {
            p_236295_.term(p_236299_, false);
         }
      }));

      for (Pair<BooleanProperty, Function<ResourceLocation, Variant>> $$4 : MULTIFACE_GENERATOR) {
         BooleanProperty $$5 = (BooleanProperty)$$4.getFirst();
         Function<ResourceLocation, Variant> $$6 = (Function<ResourceLocation, Variant>)$$4.getSecond();
         if (p_176086_.defaultBlockState().hasProperty($$5)) {
            $$2.with(Condition.condition().term($$5, true), $$6.apply($$1));
            $$2.with($$3, $$6.apply($$1));
         }
      }

      this.blockStateOutput.accept($$2);
   }

   private void createSculkCatalyst() {
      ResourceLocation $$0 = TextureMapping.getBlockTexture(Blocks.SCULK_CATALYST, "_bottom");
      TextureMapping $$1 = new TextureMapping()
         .put(TextureSlot.BOTTOM, $$0)
         .put(TextureSlot.TOP, TextureMapping.getBlockTexture(Blocks.SCULK_CATALYST, "_top"))
         .put(TextureSlot.SIDE, TextureMapping.getBlockTexture(Blocks.SCULK_CATALYST, "_side"));
      TextureMapping $$2 = new TextureMapping()
         .put(TextureSlot.BOTTOM, $$0)
         .put(TextureSlot.TOP, TextureMapping.getBlockTexture(Blocks.SCULK_CATALYST, "_top_bloom"))
         .put(TextureSlot.SIDE, TextureMapping.getBlockTexture(Blocks.SCULK_CATALYST, "_side_bloom"));
      ResourceLocation $$3 = ModelTemplates.CUBE_BOTTOM_TOP.createWithSuffix(Blocks.SCULK_CATALYST, "", $$1, this.modelOutput);
      ResourceLocation $$4 = ModelTemplates.CUBE_BOTTOM_TOP.createWithSuffix(Blocks.SCULK_CATALYST, "_bloom", $$2, this.modelOutput);
      this.blockStateOutput
         .accept(
            MultiVariantGenerator.multiVariant(Blocks.SCULK_CATALYST)
               .with(
                  PropertyDispatch.property(BlockStateProperties.BLOOM)
                     .generate(p_236280_ -> Variant.variant().with(VariantProperties.MODEL, p_236280_ ? $$4 : $$3))
               )
         );
      this.delegateItemModel(Items.SCULK_CATALYST, $$3);
   }

   private void createChiseledBookshelf() {
      Block $$0 = Blocks.CHISELED_BOOKSHELF;
      ResourceLocation $$1 = ModelLocationUtils.getModelLocation($$0);
      MultiPartGenerator $$2 = MultiPartGenerator.multiPart($$0);
      List.of(
            Pair.of(Direction.NORTH, VariantProperties.Rotation.R0),
            Pair.of(Direction.EAST, VariantProperties.Rotation.R90),
            Pair.of(Direction.SOUTH, VariantProperties.Rotation.R180),
            Pair.of(Direction.WEST, VariantProperties.Rotation.R270)
         )
         .forEach(p_329851_ -> {
            Direction $$3 = (Direction)p_329851_.getFirst();
            VariantProperties.Rotation $$4 = (VariantProperties.Rotation)p_329851_.getSecond();
            Condition.TerminalCondition $$5 = Condition.condition().term(BlockStateProperties.HORIZONTAL_FACING, $$3);
            $$2.with($$5, Variant.variant().with(VariantProperties.MODEL, $$1).with(VariantProperties.Y_ROT, $$4).with(VariantProperties.UV_LOCK, true));
            this.addSlotStateAndRotationVariants($$2, $$5, $$4);
         });
      this.blockStateOutput.accept($$2);
      this.delegateItemModel($$0, ModelLocationUtils.getModelLocation($$0, "_inventory"));
      CHISELED_BOOKSHELF_SLOT_MODEL_CACHE.clear();
   }

   private void addSlotStateAndRotationVariants(MultiPartGenerator p_261951_, Condition.TerminalCondition p_261482_, VariantProperties.Rotation p_262169_) {
      List.of(
            Pair.of(BlockStateProperties.CHISELED_BOOKSHELF_SLOT_0_OCCUPIED, ModelTemplates.CHISELED_BOOKSHELF_SLOT_TOP_LEFT),
            Pair.of(BlockStateProperties.CHISELED_BOOKSHELF_SLOT_1_OCCUPIED, ModelTemplates.CHISELED_BOOKSHELF_SLOT_TOP_MID),
            Pair.of(BlockStateProperties.CHISELED_BOOKSHELF_SLOT_2_OCCUPIED, ModelTemplates.CHISELED_BOOKSHELF_SLOT_TOP_RIGHT),
            Pair.of(BlockStateProperties.CHISELED_BOOKSHELF_SLOT_3_OCCUPIED, ModelTemplates.CHISELED_BOOKSHELF_SLOT_BOTTOM_LEFT),
            Pair.of(BlockStateProperties.CHISELED_BOOKSHELF_SLOT_4_OCCUPIED, ModelTemplates.CHISELED_BOOKSHELF_SLOT_BOTTOM_MID),
            Pair.of(BlockStateProperties.CHISELED_BOOKSHELF_SLOT_5_OCCUPIED, ModelTemplates.CHISELED_BOOKSHELF_SLOT_BOTTOM_RIGHT)
         )
         .forEach(p_307151_ -> {
            BooleanProperty $$4 = (BooleanProperty)p_307151_.getFirst();
            ModelTemplate $$5 = (ModelTemplate)p_307151_.getSecond();
            this.addBookSlotModel(p_261951_, p_261482_, p_262169_, $$4, $$5, true);
            this.addBookSlotModel(p_261951_, p_261482_, p_262169_, $$4, $$5, false);
         });
   }

   private void addBookSlotModel(
      MultiPartGenerator p_261839_,
      Condition.TerminalCondition p_261634_,
      VariantProperties.Rotation p_262044_,
      BooleanProperty p_262163_,
      ModelTemplate p_261986_,
      boolean p_261790_
   ) {
      String $$6 = p_261790_ ? "_occupied" : "_empty";
      TextureMapping $$7 = new TextureMapping().put(TextureSlot.TEXTURE, TextureMapping.getBlockTexture(Blocks.CHISELED_BOOKSHELF, $$6));
      BlockModelGenerators.BookSlotModelCacheKey $$8 = new BlockModelGenerators.BookSlotModelCacheKey(p_261986_, $$6);
      ResourceLocation $$9 = CHISELED_BOOKSHELF_SLOT_MODEL_CACHE.computeIfAbsent(
         $$8, p_261415_ -> p_261986_.createWithSuffix(Blocks.CHISELED_BOOKSHELF, $$6, $$7, this.modelOutput)
      );
      p_261839_.with(
         Condition.and(p_261634_, Condition.condition().term(p_262163_, p_261790_)),
         Variant.variant().with(VariantProperties.MODEL, $$9).with(VariantProperties.Y_ROT, p_262044_)
      );
   }

   private void createMagmaBlock() {
      this.blockStateOutput
         .accept(
            createSimpleBlock(
               Blocks.MAGMA_BLOCK,
               ModelTemplates.CUBE_ALL
                  .create(Blocks.MAGMA_BLOCK, TextureMapping.cube(ModelLocationUtils.decorateBlockModelLocation("magma")), this.modelOutput)
            )
         );
   }

   private void createShulkerBox(Block p_125011_) {
      this.createTrivialBlock(p_125011_, TexturedModel.PARTICLE_ONLY);
      ModelTemplates.SHULKER_BOX_INVENTORY
         .create(ModelLocationUtils.getModelLocation(p_125011_.asItem()), TextureMapping.particle(p_125011_), this.modelOutput);
   }

   private void createGrowingPlant(Block p_124734_, Block p_124735_, BlockModelGenerators.TintState p_124736_) {
      this.createCrossBlock(p_124734_, p_124736_);
      this.createCrossBlock(p_124735_, p_124736_);
   }

   private void createBedItem(Block p_124963_, Block p_124964_) {
      ModelTemplates.BED_INVENTORY.create(ModelLocationUtils.getModelLocation(p_124963_.asItem()), TextureMapping.particle(p_124964_), this.modelOutput);
   }

   private void createInfestedStone() {
      ResourceLocation $$0 = ModelLocationUtils.getModelLocation(Blocks.STONE);
      ResourceLocation $$1 = ModelLocationUtils.getModelLocation(Blocks.STONE, "_mirrored");
      this.blockStateOutput.accept(createRotatedVariant(Blocks.INFESTED_STONE, $$0, $$1));
      this.delegateItemModel(Blocks.INFESTED_STONE, $$0);
   }

   private void createInfestedDeepslate() {
      ResourceLocation $$0 = ModelLocationUtils.getModelLocation(Blocks.DEEPSLATE);
      ResourceLocation $$1 = ModelLocationUtils.getModelLocation(Blocks.DEEPSLATE, "_mirrored");
      this.blockStateOutput.accept(createRotatedVariant(Blocks.INFESTED_DEEPSLATE, $$0, $$1).with(createRotatedPillar()));
      this.delegateItemModel(Blocks.INFESTED_DEEPSLATE, $$0);
   }

   private void createNetherRoots(Block p_124971_, Block p_124972_) {
      this.createCrossBlockWithDefaultItem(p_124971_, BlockModelGenerators.TintState.NOT_TINTED);
      TextureMapping $$2 = TextureMapping.plant(TextureMapping.getBlockTexture(p_124971_, "_pot"));
      ResourceLocation $$3 = BlockModelGenerators.TintState.NOT_TINTED.getCrossPot().create(p_124972_, $$2, this.modelOutput);
      this.blockStateOutput.accept(createSimpleBlock(p_124972_, $$3));
   }

   private void createRespawnAnchor() {
      ResourceLocation $$0 = TextureMapping.getBlockTexture(Blocks.RESPAWN_ANCHOR, "_bottom");
      ResourceLocation $$1 = TextureMapping.getBlockTexture(Blocks.RESPAWN_ANCHOR, "_top_off");
      ResourceLocation $$2 = TextureMapping.getBlockTexture(Blocks.RESPAWN_ANCHOR, "_top");
      ResourceLocation[] $$3 = new ResourceLocation[5];

      for (int $$4 = 0; $$4 < 5; $$4++) {
         TextureMapping $$5 = new TextureMapping()
            .put(TextureSlot.BOTTOM, $$0)
            .put(TextureSlot.TOP, $$4 == 0 ? $$1 : $$2)
            .put(TextureSlot.SIDE, TextureMapping.getBlockTexture(Blocks.RESPAWN_ANCHOR, "_side" + $$4));
         $$3[$$4] = ModelTemplates.CUBE_BOTTOM_TOP.createWithSuffix(Blocks.RESPAWN_ANCHOR, "_" + $$4, $$5, this.modelOutput);
      }

      this.blockStateOutput
         .accept(
            MultiVariantGenerator.multiVariant(Blocks.RESPAWN_ANCHOR)
               .with(
                  PropertyDispatch.property(BlockStateProperties.RESPAWN_ANCHOR_CHARGES)
                     .generate(p_236313_ -> Variant.variant().with(VariantProperties.MODEL, $$3[p_236313_]))
               )
         );
      this.delegateItemModel(Items.RESPAWN_ANCHOR, $$3[0]);
   }

   private Variant applyRotation(FrontAndTop p_124636_, Variant p_124637_) {
      switch (p_124636_) {
         case DOWN_NORTH:
            return p_124637_.with(VariantProperties.X_ROT, VariantProperties.Rotation.R90);
         case DOWN_SOUTH:
            return p_124637_.with(VariantProperties.X_ROT, VariantProperties.Rotation.R90).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180);
         case DOWN_WEST:
            return p_124637_.with(VariantProperties.X_ROT, VariantProperties.Rotation.R90).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270);
         case DOWN_EAST:
            return p_124637_.with(VariantProperties.X_ROT, VariantProperties.Rotation.R90).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90);
         case UP_NORTH:
            return p_124637_.with(VariantProperties.X_ROT, VariantProperties.Rotation.R270).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180);
         case UP_SOUTH:
            return p_124637_.with(VariantProperties.X_ROT, VariantProperties.Rotation.R270);
         case UP_WEST:
            return p_124637_.with(VariantProperties.X_ROT, VariantProperties.Rotation.R270).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90);
         case UP_EAST:
            return p_124637_.with(VariantProperties.X_ROT, VariantProperties.Rotation.R270).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270);
         case NORTH_UP:
            return p_124637_;
         case SOUTH_UP:
            return p_124637_.with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180);
         case WEST_UP:
            return p_124637_.with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270);
         case EAST_UP:
            return p_124637_.with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90);
         default:
            throw new UnsupportedOperationException("Rotation " + p_124636_ + " can't be expressed with existing x and y values");
      }
   }

   private void createJigsaw() {
      ResourceLocation $$0 = TextureMapping.getBlockTexture(Blocks.JIGSAW, "_top");
      ResourceLocation $$1 = TextureMapping.getBlockTexture(Blocks.JIGSAW, "_bottom");
      ResourceLocation $$2 = TextureMapping.getBlockTexture(Blocks.JIGSAW, "_side");
      ResourceLocation $$3 = TextureMapping.getBlockTexture(Blocks.JIGSAW, "_lock");
      TextureMapping $$4 = new TextureMapping()
         .put(TextureSlot.DOWN, $$2)
         .put(TextureSlot.WEST, $$2)
         .put(TextureSlot.EAST, $$2)
         .put(TextureSlot.PARTICLE, $$0)
         .put(TextureSlot.NORTH, $$0)
         .put(TextureSlot.SOUTH, $$1)
         .put(TextureSlot.UP, $$3);
      ResourceLocation $$5 = ModelTemplates.CUBE_DIRECTIONAL.create(Blocks.JIGSAW, $$4, this.modelOutput);
      this.blockStateOutput
         .accept(
            MultiVariantGenerator.multiVariant(Blocks.JIGSAW, Variant.variant().with(VariantProperties.MODEL, $$5))
               .with(PropertyDispatch.property(BlockStateProperties.ORIENTATION).generate(p_307147_ -> this.applyRotation(p_307147_, Variant.variant())))
         );
   }

   private void createPetrifiedOakSlab() {
      Block $$0 = Blocks.OAK_PLANKS;
      ResourceLocation $$1 = ModelLocationUtils.getModelLocation($$0);
      TexturedModel $$2 = TexturedModel.CUBE.get($$0);
      Block $$3 = Blocks.PETRIFIED_OAK_SLAB;
      ResourceLocation $$4 = ModelTemplates.SLAB_BOTTOM.create($$3, $$2.getMapping(), this.modelOutput);
      ResourceLocation $$5 = ModelTemplates.SLAB_TOP.create($$3, $$2.getMapping(), this.modelOutput);
      this.blockStateOutput.accept(createSlab($$3, $$4, $$5, $$1));
   }

   public void run() {
      BlockFamilies.getAllFamilies()
         .filter(BlockFamily::shouldGenerateModel)
         .forEach(p_236303_ -> this.family(p_236303_.getBaseBlock()).generateFor(p_236303_));
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
      this.createSimpleFlatItemModel(Items.FLOWER_POT);
      this.createNonTemplateModelBlock(Blocks.HONEY_BLOCK);
      this.createNonTemplateModelBlock(Blocks.WATER);
      this.createNonTemplateModelBlock(Blocks.LAVA);
      this.createNonTemplateModelBlock(Blocks.SLIME_BLOCK);
      this.createSimpleFlatItemModel(Items.CHAIN);
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
      this.createFlowerBed(Blocks.PINK_PETALS);
      this.createAirLikeBlock(Blocks.BARRIER, Items.BARRIER);
      this.createSimpleFlatItemModel(Items.BARRIER);
      this.createLightBlock();
      this.createAirLikeBlock(Blocks.STRUCTURE_VOID, Items.STRUCTURE_VOID);
      this.createSimpleFlatItemModel(Items.STRUCTURE_VOID);
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
      this.createTrivialCube(Blocks.SPONGE);
      this.createTrivialBlock(Blocks.SEAGRASS, TexturedModel.SEAGRASS);
      this.createSimpleFlatItemModel(Items.SEAGRASS);
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
      this.createWeightedPressurePlate(Blocks.LIGHT_WEIGHTED_PRESSURE_PLATE, Blocks.GOLD_BLOCK);
      this.createWeightedPressurePlate(Blocks.HEAVY_WEIGHTED_PRESSURE_PLATE, Blocks.IRON_BLOCK);
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
      this.createLightningRod();
      this.createFarmland();
      this.createFire();
      this.createSoulFire();
      this.createFrostedIce();
      this.createGrassBlocks();
      this.createCocoa();
      this.createDirtPath();
      this.createGrindstone();
      this.createHopper();
      this.createIronBars();
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
      this.createTripwire();
      this.createTripwireHook();
      this.createTurtleEgg();
      this.createSnifferEgg();
      this.createMultiface(Blocks.VINE);
      this.createMultiface(Blocks.GLOW_LICHEN);
      this.createMultiface(Blocks.SCULK_VEIN);
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
      this.createSimpleFlatItemModel(Blocks.LADDER);
      this.createNonTemplateHorizontalBlock(Blocks.LECTERN);
      this.createBigDripLeafBlock();
      this.createNonTemplateHorizontalBlock(Blocks.BIG_DRIPLEAF_STEM);
      this.createNormalTorch(Blocks.TORCH, Blocks.WALL_TORCH);
      this.createNormalTorch(Blocks.SOUL_TORCH, Blocks.SOUL_WALL_TORCH);
      this.createCraftingTableLike(Blocks.CRAFTING_TABLE, Blocks.OAK_PLANKS, TextureMapping::craftingTable);
      this.createCraftingTableLike(Blocks.FLETCHING_TABLE, Blocks.BIRCH_PLANKS, TextureMapping::fletchingTable);
      this.createNyliumBlock(Blocks.CRIMSON_NYLIUM);
      this.createNyliumBlock(Blocks.WARPED_NYLIUM);
      this.createDispenserBlock(Blocks.DISPENSER);
      this.createDispenserBlock(Blocks.DROPPER);
      this.createCrafterBlock();
      this.createLantern(Blocks.LANTERN);
      this.createLantern(Blocks.SOUL_LANTERN);
      this.createAxisAlignedPillarBlockCustomModel(Blocks.CHAIN, ModelLocationUtils.getModelLocation(Blocks.CHAIN));
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
      this.createCrossBlock(Blocks.TORCHFLOWER_CROP, BlockModelGenerators.TintState.NOT_TINTED, BlockStateProperties.AGE_1, 0, 1);
      this.createPitcherCrop();
      this.createPitcherPlant();
      this.blockEntityModels(ModelLocationUtils.decorateBlockModelLocation("decorated_pot"), Blocks.TERRACOTTA).createWithoutBlockItem(Blocks.DECORATED_POT);
      this.blockEntityModels(ModelLocationUtils.decorateBlockModelLocation("banner"), Blocks.OAK_PLANKS)
         .createWithCustomBlockItemModel(
            ModelTemplates.BANNER_INVENTORY,
            Blocks.WHITE_BANNER,
            Blocks.ORANGE_BANNER,
            Blocks.MAGENTA_BANNER,
            Blocks.LIGHT_BLUE_BANNER,
            Blocks.YELLOW_BANNER,
            Blocks.LIME_BANNER,
            Blocks.PINK_BANNER,
            Blocks.GRAY_BANNER,
            Blocks.LIGHT_GRAY_BANNER,
            Blocks.CYAN_BANNER,
            Blocks.PURPLE_BANNER,
            Blocks.BLUE_BANNER,
            Blocks.BROWN_BANNER,
            Blocks.GREEN_BANNER,
            Blocks.RED_BANNER,
            Blocks.BLACK_BANNER
         )
         .createWithoutBlockItem(
            Blocks.WHITE_WALL_BANNER,
            Blocks.ORANGE_WALL_BANNER,
            Blocks.MAGENTA_WALL_BANNER,
            Blocks.LIGHT_BLUE_WALL_BANNER,
            Blocks.YELLOW_WALL_BANNER,
            Blocks.LIME_WALL_BANNER,
            Blocks.PINK_WALL_BANNER,
            Blocks.GRAY_WALL_BANNER,
            Blocks.LIGHT_GRAY_WALL_BANNER,
            Blocks.CYAN_WALL_BANNER,
            Blocks.PURPLE_WALL_BANNER,
            Blocks.BLUE_WALL_BANNER,
            Blocks.BROWN_WALL_BANNER,
            Blocks.GREEN_WALL_BANNER,
            Blocks.RED_WALL_BANNER,
            Blocks.BLACK_WALL_BANNER
         );
      this.blockEntityModels(ModelLocationUtils.decorateBlockModelLocation("bed"), Blocks.OAK_PLANKS)
         .createWithoutBlockItem(
            Blocks.WHITE_BED,
            Blocks.ORANGE_BED,
            Blocks.MAGENTA_BED,
            Blocks.LIGHT_BLUE_BED,
            Blocks.YELLOW_BED,
            Blocks.LIME_BED,
            Blocks.PINK_BED,
            Blocks.GRAY_BED,
            Blocks.LIGHT_GRAY_BED,
            Blocks.CYAN_BED,
            Blocks.PURPLE_BED,
            Blocks.BLUE_BED,
            Blocks.BROWN_BED,
            Blocks.GREEN_BED,
            Blocks.RED_BED,
            Blocks.BLACK_BED
         );
      this.createBedItem(Blocks.WHITE_BED, Blocks.WHITE_WOOL);
      this.createBedItem(Blocks.ORANGE_BED, Blocks.ORANGE_WOOL);
      this.createBedItem(Blocks.MAGENTA_BED, Blocks.MAGENTA_WOOL);
      this.createBedItem(Blocks.LIGHT_BLUE_BED, Blocks.LIGHT_BLUE_WOOL);
      this.createBedItem(Blocks.YELLOW_BED, Blocks.YELLOW_WOOL);
      this.createBedItem(Blocks.LIME_BED, Blocks.LIME_WOOL);
      this.createBedItem(Blocks.PINK_BED, Blocks.PINK_WOOL);
      this.createBedItem(Blocks.GRAY_BED, Blocks.GRAY_WOOL);
      this.createBedItem(Blocks.LIGHT_GRAY_BED, Blocks.LIGHT_GRAY_WOOL);
      this.createBedItem(Blocks.CYAN_BED, Blocks.CYAN_WOOL);
      this.createBedItem(Blocks.PURPLE_BED, Blocks.PURPLE_WOOL);
      this.createBedItem(Blocks.BLUE_BED, Blocks.BLUE_WOOL);
      this.createBedItem(Blocks.BROWN_BED, Blocks.BROWN_WOOL);
      this.createBedItem(Blocks.GREEN_BED, Blocks.GREEN_WOOL);
      this.createBedItem(Blocks.RED_BED, Blocks.RED_WOOL);
      this.createBedItem(Blocks.BLACK_BED, Blocks.BLACK_WOOL);
      this.blockEntityModels(ModelLocationUtils.decorateBlockModelLocation("skull"), Blocks.SOUL_SAND)
         .createWithCustomBlockItemModel(
            ModelTemplates.SKULL_INVENTORY,
            Blocks.CREEPER_HEAD,
            Blocks.PLAYER_HEAD,
            Blocks.ZOMBIE_HEAD,
            Blocks.SKELETON_SKULL,
            Blocks.WITHER_SKELETON_SKULL,
            Blocks.PIGLIN_HEAD
         )
         .create(Blocks.DRAGON_HEAD)
         .createWithoutBlockItem(
            Blocks.CREEPER_WALL_HEAD,
            Blocks.DRAGON_WALL_HEAD,
            Blocks.PLAYER_WALL_HEAD,
            Blocks.ZOMBIE_WALL_HEAD,
            Blocks.SKELETON_WALL_SKULL,
            Blocks.WITHER_SKELETON_WALL_SKULL,
            Blocks.PIGLIN_WALL_HEAD
         );
      this.createShulkerBox(Blocks.SHULKER_BOX);
      this.createShulkerBox(Blocks.WHITE_SHULKER_BOX);
      this.createShulkerBox(Blocks.ORANGE_SHULKER_BOX);
      this.createShulkerBox(Blocks.MAGENTA_SHULKER_BOX);
      this.createShulkerBox(Blocks.LIGHT_BLUE_SHULKER_BOX);
      this.createShulkerBox(Blocks.YELLOW_SHULKER_BOX);
      this.createShulkerBox(Blocks.LIME_SHULKER_BOX);
      this.createShulkerBox(Blocks.PINK_SHULKER_BOX);
      this.createShulkerBox(Blocks.GRAY_SHULKER_BOX);
      this.createShulkerBox(Blocks.LIGHT_GRAY_SHULKER_BOX);
      this.createShulkerBox(Blocks.CYAN_SHULKER_BOX);
      this.createShulkerBox(Blocks.PURPLE_SHULKER_BOX);
      this.createShulkerBox(Blocks.BLUE_SHULKER_BOX);
      this.createShulkerBox(Blocks.BROWN_SHULKER_BOX);
      this.createShulkerBox(Blocks.GREEN_SHULKER_BOX);
      this.createShulkerBox(Blocks.RED_SHULKER_BOX);
      this.createShulkerBox(Blocks.BLACK_SHULKER_BOX);
      this.createTrivialBlock(Blocks.CONDUIT, TexturedModel.PARTICLE_ONLY);
      this.skipAutoItemBlock(Blocks.CONDUIT);
      this.blockEntityModels(ModelLocationUtils.decorateBlockModelLocation("chest"), Blocks.OAK_PLANKS)
         .createWithoutBlockItem(Blocks.CHEST, Blocks.TRAPPED_CHEST);
      this.blockEntityModels(ModelLocationUtils.decorateBlockModelLocation("ender_chest"), Blocks.OBSIDIAN).createWithoutBlockItem(Blocks.ENDER_CHEST);
      this.blockEntityModels(Blocks.END_PORTAL, Blocks.OBSIDIAN).create(Blocks.END_PORTAL, Blocks.END_GATEWAY);
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
      this.createPlant(Blocks.FERN, Blocks.POTTED_FERN, BlockModelGenerators.TintState.TINTED);
      this.createPlant(Blocks.DANDELION, Blocks.POTTED_DANDELION, BlockModelGenerators.TintState.NOT_TINTED);
      this.createPlant(Blocks.POPPY, Blocks.POTTED_POPPY, BlockModelGenerators.TintState.NOT_TINTED);
      this.createPlant(Blocks.BLUE_ORCHID, Blocks.POTTED_BLUE_ORCHID, BlockModelGenerators.TintState.NOT_TINTED);
      this.createPlant(Blocks.ALLIUM, Blocks.POTTED_ALLIUM, BlockModelGenerators.TintState.NOT_TINTED);
      this.createPlant(Blocks.AZURE_BLUET, Blocks.POTTED_AZURE_BLUET, BlockModelGenerators.TintState.NOT_TINTED);
      this.createPlant(Blocks.RED_TULIP, Blocks.POTTED_RED_TULIP, BlockModelGenerators.TintState.NOT_TINTED);
      this.createPlant(Blocks.ORANGE_TULIP, Blocks.POTTED_ORANGE_TULIP, BlockModelGenerators.TintState.NOT_TINTED);
      this.createPlant(Blocks.WHITE_TULIP, Blocks.POTTED_WHITE_TULIP, BlockModelGenerators.TintState.NOT_TINTED);
      this.createPlant(Blocks.PINK_TULIP, Blocks.POTTED_PINK_TULIP, BlockModelGenerators.TintState.NOT_TINTED);
      this.createPlant(Blocks.OXEYE_DAISY, Blocks.POTTED_OXEYE_DAISY, BlockModelGenerators.TintState.NOT_TINTED);
      this.createPlant(Blocks.CORNFLOWER, Blocks.POTTED_CORNFLOWER, BlockModelGenerators.TintState.NOT_TINTED);
      this.createPlant(Blocks.LILY_OF_THE_VALLEY, Blocks.POTTED_LILY_OF_THE_VALLEY, BlockModelGenerators.TintState.NOT_TINTED);
      this.createPlant(Blocks.WITHER_ROSE, Blocks.POTTED_WITHER_ROSE, BlockModelGenerators.TintState.NOT_TINTED);
      this.createPlant(Blocks.RED_MUSHROOM, Blocks.POTTED_RED_MUSHROOM, BlockModelGenerators.TintState.NOT_TINTED);
      this.createPlant(Blocks.BROWN_MUSHROOM, Blocks.POTTED_BROWN_MUSHROOM, BlockModelGenerators.TintState.NOT_TINTED);
      this.createPlant(Blocks.DEAD_BUSH, Blocks.POTTED_DEAD_BUSH, BlockModelGenerators.TintState.NOT_TINTED);
      this.createPlant(Blocks.TORCHFLOWER, Blocks.POTTED_TORCHFLOWER, BlockModelGenerators.TintState.NOT_TINTED);
      this.createPointedDripstone();
      this.createMushroomBlock(Blocks.BROWN_MUSHROOM_BLOCK);
      this.createMushroomBlock(Blocks.RED_MUSHROOM_BLOCK);
      this.createMushroomBlock(Blocks.MUSHROOM_STEM);
      this.createCrossBlockWithDefaultItem(Blocks.SHORT_GRASS, BlockModelGenerators.TintState.TINTED);
      this.createCrossBlock(Blocks.SUGAR_CANE, BlockModelGenerators.TintState.TINTED);
      this.createSimpleFlatItemModel(Items.SUGAR_CANE);
      this.createGrowingPlant(Blocks.KELP, Blocks.KELP_PLANT, BlockModelGenerators.TintState.NOT_TINTED);
      this.createSimpleFlatItemModel(Items.KELP);
      this.skipAutoItemBlock(Blocks.KELP_PLANT);
      this.createCrossBlock(Blocks.HANGING_ROOTS, BlockModelGenerators.TintState.NOT_TINTED);
      this.skipAutoItemBlock(Blocks.HANGING_ROOTS);
      this.skipAutoItemBlock(Blocks.CAVE_VINES_PLANT);
      this.createGrowingPlant(Blocks.WEEPING_VINES, Blocks.WEEPING_VINES_PLANT, BlockModelGenerators.TintState.NOT_TINTED);
      this.createGrowingPlant(Blocks.TWISTING_VINES, Blocks.TWISTING_VINES_PLANT, BlockModelGenerators.TintState.NOT_TINTED);
      this.createSimpleFlatItemModel(Blocks.WEEPING_VINES, "_plant");
      this.skipAutoItemBlock(Blocks.WEEPING_VINES_PLANT);
      this.createSimpleFlatItemModel(Blocks.TWISTING_VINES, "_plant");
      this.skipAutoItemBlock(Blocks.TWISTING_VINES_PLANT);
      this.createCrossBlockWithDefaultItem(
         Blocks.BAMBOO_SAPLING, BlockModelGenerators.TintState.TINTED, TextureMapping.cross(TextureMapping.getBlockTexture(Blocks.BAMBOO, "_stage0"))
      );
      this.createBamboo();
      this.createCrossBlockWithDefaultItem(Blocks.COBWEB, BlockModelGenerators.TintState.NOT_TINTED);
      this.createDoublePlant(Blocks.LILAC, BlockModelGenerators.TintState.NOT_TINTED);
      this.createDoublePlant(Blocks.ROSE_BUSH, BlockModelGenerators.TintState.NOT_TINTED);
      this.createDoublePlant(Blocks.PEONY, BlockModelGenerators.TintState.NOT_TINTED);
      this.createDoublePlant(Blocks.TALL_GRASS, BlockModelGenerators.TintState.TINTED);
      this.createDoublePlant(Blocks.LARGE_FERN, BlockModelGenerators.TintState.TINTED);
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
      this.createTrivialBlock(Blocks.MANGROVE_LEAVES, TexturedModel.LEAVES);
      this.woodProvider(Blocks.ACACIA_LOG).logWithHorizontal(Blocks.ACACIA_LOG).wood(Blocks.ACACIA_WOOD);
      this.woodProvider(Blocks.STRIPPED_ACACIA_LOG).logWithHorizontal(Blocks.STRIPPED_ACACIA_LOG).wood(Blocks.STRIPPED_ACACIA_WOOD);
      this.createHangingSign(Blocks.STRIPPED_ACACIA_LOG, Blocks.ACACIA_HANGING_SIGN, Blocks.ACACIA_WALL_HANGING_SIGN);
      this.createPlant(Blocks.ACACIA_SAPLING, Blocks.POTTED_ACACIA_SAPLING, BlockModelGenerators.TintState.NOT_TINTED);
      this.createTrivialBlock(Blocks.ACACIA_LEAVES, TexturedModel.LEAVES);
      this.woodProvider(Blocks.CHERRY_LOG).logUVLocked(Blocks.CHERRY_LOG).wood(Blocks.CHERRY_WOOD);
      this.woodProvider(Blocks.STRIPPED_CHERRY_LOG).logUVLocked(Blocks.STRIPPED_CHERRY_LOG).wood(Blocks.STRIPPED_CHERRY_WOOD);
      this.createHangingSign(Blocks.STRIPPED_CHERRY_LOG, Blocks.CHERRY_HANGING_SIGN, Blocks.CHERRY_WALL_HANGING_SIGN);
      this.createPlant(Blocks.CHERRY_SAPLING, Blocks.POTTED_CHERRY_SAPLING, BlockModelGenerators.TintState.NOT_TINTED);
      this.createTrivialBlock(Blocks.CHERRY_LEAVES, TexturedModel.LEAVES);
      this.woodProvider(Blocks.BIRCH_LOG).logWithHorizontal(Blocks.BIRCH_LOG).wood(Blocks.BIRCH_WOOD);
      this.woodProvider(Blocks.STRIPPED_BIRCH_LOG).logWithHorizontal(Blocks.STRIPPED_BIRCH_LOG).wood(Blocks.STRIPPED_BIRCH_WOOD);
      this.createHangingSign(Blocks.STRIPPED_BIRCH_LOG, Blocks.BIRCH_HANGING_SIGN, Blocks.BIRCH_WALL_HANGING_SIGN);
      this.createPlant(Blocks.BIRCH_SAPLING, Blocks.POTTED_BIRCH_SAPLING, BlockModelGenerators.TintState.NOT_TINTED);
      this.createTrivialBlock(Blocks.BIRCH_LEAVES, TexturedModel.LEAVES);
      this.woodProvider(Blocks.OAK_LOG).logWithHorizontal(Blocks.OAK_LOG).wood(Blocks.OAK_WOOD);
      this.woodProvider(Blocks.STRIPPED_OAK_LOG).logWithHorizontal(Blocks.STRIPPED_OAK_LOG).wood(Blocks.STRIPPED_OAK_WOOD);
      this.createHangingSign(Blocks.STRIPPED_OAK_LOG, Blocks.OAK_HANGING_SIGN, Blocks.OAK_WALL_HANGING_SIGN);
      this.createPlant(Blocks.OAK_SAPLING, Blocks.POTTED_OAK_SAPLING, BlockModelGenerators.TintState.NOT_TINTED);
      this.createTrivialBlock(Blocks.OAK_LEAVES, TexturedModel.LEAVES);
      this.woodProvider(Blocks.SPRUCE_LOG).logWithHorizontal(Blocks.SPRUCE_LOG).wood(Blocks.SPRUCE_WOOD);
      this.woodProvider(Blocks.STRIPPED_SPRUCE_LOG).logWithHorizontal(Blocks.STRIPPED_SPRUCE_LOG).wood(Blocks.STRIPPED_SPRUCE_WOOD);
      this.createHangingSign(Blocks.STRIPPED_SPRUCE_LOG, Blocks.SPRUCE_HANGING_SIGN, Blocks.SPRUCE_WALL_HANGING_SIGN);
      this.createPlant(Blocks.SPRUCE_SAPLING, Blocks.POTTED_SPRUCE_SAPLING, BlockModelGenerators.TintState.NOT_TINTED);
      this.createTrivialBlock(Blocks.SPRUCE_LEAVES, TexturedModel.LEAVES);
      this.woodProvider(Blocks.DARK_OAK_LOG).logWithHorizontal(Blocks.DARK_OAK_LOG).wood(Blocks.DARK_OAK_WOOD);
      this.woodProvider(Blocks.STRIPPED_DARK_OAK_LOG).logWithHorizontal(Blocks.STRIPPED_DARK_OAK_LOG).wood(Blocks.STRIPPED_DARK_OAK_WOOD);
      this.createHangingSign(Blocks.STRIPPED_DARK_OAK_LOG, Blocks.DARK_OAK_HANGING_SIGN, Blocks.DARK_OAK_WALL_HANGING_SIGN);
      this.createPlant(Blocks.DARK_OAK_SAPLING, Blocks.POTTED_DARK_OAK_SAPLING, BlockModelGenerators.TintState.NOT_TINTED);
      this.createTrivialBlock(Blocks.DARK_OAK_LEAVES, TexturedModel.LEAVES);
      this.woodProvider(Blocks.JUNGLE_LOG).logWithHorizontal(Blocks.JUNGLE_LOG).wood(Blocks.JUNGLE_WOOD);
      this.woodProvider(Blocks.STRIPPED_JUNGLE_LOG).logWithHorizontal(Blocks.STRIPPED_JUNGLE_LOG).wood(Blocks.STRIPPED_JUNGLE_WOOD);
      this.createHangingSign(Blocks.STRIPPED_JUNGLE_LOG, Blocks.JUNGLE_HANGING_SIGN, Blocks.JUNGLE_WALL_HANGING_SIGN);
      this.createPlant(Blocks.JUNGLE_SAPLING, Blocks.POTTED_JUNGLE_SAPLING, BlockModelGenerators.TintState.NOT_TINTED);
      this.createTrivialBlock(Blocks.JUNGLE_LEAVES, TexturedModel.LEAVES);
      this.woodProvider(Blocks.CRIMSON_STEM).log(Blocks.CRIMSON_STEM).wood(Blocks.CRIMSON_HYPHAE);
      this.woodProvider(Blocks.STRIPPED_CRIMSON_STEM).log(Blocks.STRIPPED_CRIMSON_STEM).wood(Blocks.STRIPPED_CRIMSON_HYPHAE);
      this.createHangingSign(Blocks.STRIPPED_CRIMSON_STEM, Blocks.CRIMSON_HANGING_SIGN, Blocks.CRIMSON_WALL_HANGING_SIGN);
      this.createPlant(Blocks.CRIMSON_FUNGUS, Blocks.POTTED_CRIMSON_FUNGUS, BlockModelGenerators.TintState.NOT_TINTED);
      this.createNetherRoots(Blocks.CRIMSON_ROOTS, Blocks.POTTED_CRIMSON_ROOTS);
      this.woodProvider(Blocks.WARPED_STEM).log(Blocks.WARPED_STEM).wood(Blocks.WARPED_HYPHAE);
      this.woodProvider(Blocks.STRIPPED_WARPED_STEM).log(Blocks.STRIPPED_WARPED_STEM).wood(Blocks.STRIPPED_WARPED_HYPHAE);
      this.createHangingSign(Blocks.STRIPPED_WARPED_STEM, Blocks.WARPED_HANGING_SIGN, Blocks.WARPED_WALL_HANGING_SIGN);
      this.createPlant(Blocks.WARPED_FUNGUS, Blocks.POTTED_WARPED_FUNGUS, BlockModelGenerators.TintState.NOT_TINTED);
      this.createNetherRoots(Blocks.WARPED_ROOTS, Blocks.POTTED_WARPED_ROOTS);
      this.woodProvider(Blocks.BAMBOO_BLOCK).logUVLocked(Blocks.BAMBOO_BLOCK);
      this.woodProvider(Blocks.STRIPPED_BAMBOO_BLOCK).logUVLocked(Blocks.STRIPPED_BAMBOO_BLOCK);
      this.createHangingSign(Blocks.BAMBOO_PLANKS, Blocks.BAMBOO_HANGING_SIGN, Blocks.BAMBOO_WALL_HANGING_SIGN);
      this.createCrossBlock(Blocks.NETHER_SPROUTS, BlockModelGenerators.TintState.NOT_TINTED);
      this.createSimpleFlatItemModel(Items.NETHER_SPROUTS);
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
      SpawnEggItem.eggs().forEach(p_236282_ -> this.delegateItemModel(p_236282_, ModelLocationUtils.decorateItemModelLocation("template_spawn_egg")));
   }

   private void createLightBlock() {
      this.skipAutoItemBlock(Blocks.LIGHT);
      PropertyDispatch.C1<Integer> $$0 = PropertyDispatch.property(BlockStateProperties.LEVEL);

      for (int $$1 = 0; $$1 < 16; $$1++) {
         String $$2 = String.format(Locale.ROOT, "_%02d", $$1);
         ResourceLocation $$3 = TextureMapping.getItemTexture(Items.LIGHT, $$2);
         $$0.select(
            $$1,
            Variant.variant()
               .with(VariantProperties.MODEL, ModelTemplates.PARTICLE_ONLY.createWithSuffix(Blocks.LIGHT, $$2, TextureMapping.particle($$3), this.modelOutput))
         );
         ModelTemplates.FLAT_ITEM.create(ModelLocationUtils.getModelLocation(Items.LIGHT, $$2), TextureMapping.layer0($$3), this.modelOutput);
      }

      this.blockStateOutput.accept(MultiVariantGenerator.multiVariant(Blocks.LIGHT).with($$0));
   }

   private void createCandleAndCandleCake(Block p_176245_, Block p_176246_) {
      this.createSimpleFlatItemModel(p_176245_.asItem());
      TextureMapping $$2 = TextureMapping.cube(TextureMapping.getBlockTexture(p_176245_));
      TextureMapping $$3 = TextureMapping.cube(TextureMapping.getBlockTexture(p_176245_, "_lit"));
      ResourceLocation $$4 = ModelTemplates.CANDLE.createWithSuffix(p_176245_, "_one_candle", $$2, this.modelOutput);
      ResourceLocation $$5 = ModelTemplates.TWO_CANDLES.createWithSuffix(p_176245_, "_two_candles", $$2, this.modelOutput);
      ResourceLocation $$6 = ModelTemplates.THREE_CANDLES.createWithSuffix(p_176245_, "_three_candles", $$2, this.modelOutput);
      ResourceLocation $$7 = ModelTemplates.FOUR_CANDLES.createWithSuffix(p_176245_, "_four_candles", $$2, this.modelOutput);
      ResourceLocation $$8 = ModelTemplates.CANDLE.createWithSuffix(p_176245_, "_one_candle_lit", $$3, this.modelOutput);
      ResourceLocation $$9 = ModelTemplates.TWO_CANDLES.createWithSuffix(p_176245_, "_two_candles_lit", $$3, this.modelOutput);
      ResourceLocation $$10 = ModelTemplates.THREE_CANDLES.createWithSuffix(p_176245_, "_three_candles_lit", $$3, this.modelOutput);
      ResourceLocation $$11 = ModelTemplates.FOUR_CANDLES.createWithSuffix(p_176245_, "_four_candles_lit", $$3, this.modelOutput);
      this.blockStateOutput
         .accept(
            MultiVariantGenerator.multiVariant(p_176245_)
               .with(
                  PropertyDispatch.properties(BlockStateProperties.CANDLES, BlockStateProperties.LIT)
                     .select(1, false, Variant.variant().with(VariantProperties.MODEL, $$4))
                     .select(2, false, Variant.variant().with(VariantProperties.MODEL, $$5))
                     .select(3, false, Variant.variant().with(VariantProperties.MODEL, $$6))
                     .select(4, false, Variant.variant().with(VariantProperties.MODEL, $$7))
                     .select(1, true, Variant.variant().with(VariantProperties.MODEL, $$8))
                     .select(2, true, Variant.variant().with(VariantProperties.MODEL, $$9))
                     .select(3, true, Variant.variant().with(VariantProperties.MODEL, $$10))
                     .select(4, true, Variant.variant().with(VariantProperties.MODEL, $$11))
               )
         );
      ResourceLocation $$12 = ModelTemplates.CANDLE_CAKE.create(p_176246_, TextureMapping.candleCake(p_176245_, false), this.modelOutput);
      ResourceLocation $$13 = ModelTemplates.CANDLE_CAKE.createWithSuffix(p_176246_, "_lit", TextureMapping.candleCake(p_176245_, true), this.modelOutput);
      this.blockStateOutput.accept(MultiVariantGenerator.multiVariant(p_176246_).with(createBooleanModelDispatch(BlockStateProperties.LIT, $$13, $$12)));
   }

   class BlockEntityModelGenerator {
      private final ResourceLocation baseModel;

      public BlockEntityModelGenerator(ResourceLocation p_125020_, Block p_125021_) {
         this.baseModel = ModelTemplates.PARTICLE_ONLY.create(p_125020_, TextureMapping.particle(p_125021_), BlockModelGenerators.this.modelOutput);
      }

      public BlockModelGenerators.BlockEntityModelGenerator create(Block... p_125026_) {
         for (Block $$1 : p_125026_) {
            BlockModelGenerators.this.blockStateOutput.accept(BlockModelGenerators.createSimpleBlock($$1, this.baseModel));
         }

         return this;
      }

      public BlockModelGenerators.BlockEntityModelGenerator createWithoutBlockItem(Block... p_125028_) {
         for (Block $$1 : p_125028_) {
            BlockModelGenerators.this.skipAutoItemBlock($$1);
         }

         return this.create(p_125028_);
      }

      public BlockModelGenerators.BlockEntityModelGenerator createWithCustomBlockItemModel(ModelTemplate p_125023_, Block... p_125024_) {
         for (Block $$2 : p_125024_) {
            p_125023_.create(ModelLocationUtils.getModelLocation($$2.asItem()), TextureMapping.particle($$2), BlockModelGenerators.this.modelOutput);
         }

         return this.create(p_125024_);
      }
   }

   class BlockFamilyProvider {
      private final TextureMapping mapping;
      private final Map<ModelTemplate, ResourceLocation> models = Maps.newHashMap();
      @Nullable
      private BlockFamily family;
      @Nullable
      private ResourceLocation fullBlock;
      private final Set<Block> skipGeneratingModelsFor = new HashSet<>();

      public BlockFamilyProvider(TextureMapping p_125034_) {
         this.mapping = p_125034_;
      }

      public BlockModelGenerators.BlockFamilyProvider fullBlock(Block p_125041_, ModelTemplate p_125042_) {
         this.fullBlock = p_125042_.create(p_125041_, this.mapping, BlockModelGenerators.this.modelOutput);
         if (BlockModelGenerators.this.fullBlockModelCustomGenerators.containsKey(p_125041_)) {
            BlockModelGenerators.this.blockStateOutput
               .accept(
                  BlockModelGenerators.this.fullBlockModelCustomGenerators
                     .get(p_125041_)
                     .create(p_125041_, this.fullBlock, this.mapping, BlockModelGenerators.this.modelOutput)
               );
         } else {
            BlockModelGenerators.this.blockStateOutput.accept(BlockModelGenerators.createSimpleBlock(p_125041_, this.fullBlock));
         }

         return this;
      }

      public BlockModelGenerators.BlockFamilyProvider donateModelTo(Block p_308961_, Block p_309136_) {
         ResourceLocation $$2 = ModelLocationUtils.getModelLocation(p_308961_);
         BlockModelGenerators.this.blockStateOutput.accept(BlockModelGenerators.createSimpleBlock(p_309136_, $$2));
         BlockModelGenerators.this.delegateItemModel(p_309136_, $$2);
         this.skipGeneratingModelsFor.add(p_309136_);
         return this;
      }

      public BlockModelGenerators.BlockFamilyProvider button(Block p_125036_) {
         ResourceLocation $$1 = ModelTemplates.BUTTON.create(p_125036_, this.mapping, BlockModelGenerators.this.modelOutput);
         ResourceLocation $$2 = ModelTemplates.BUTTON_PRESSED.create(p_125036_, this.mapping, BlockModelGenerators.this.modelOutput);
         BlockModelGenerators.this.blockStateOutput.accept(BlockModelGenerators.createButton(p_125036_, $$1, $$2));
         ResourceLocation $$3 = ModelTemplates.BUTTON_INVENTORY.create(p_125036_, this.mapping, BlockModelGenerators.this.modelOutput);
         BlockModelGenerators.this.delegateItemModel(p_125036_, $$3);
         return this;
      }

      public BlockModelGenerators.BlockFamilyProvider wall(Block p_125046_) {
         ResourceLocation $$1 = ModelTemplates.WALL_POST.create(p_125046_, this.mapping, BlockModelGenerators.this.modelOutput);
         ResourceLocation $$2 = ModelTemplates.WALL_LOW_SIDE.create(p_125046_, this.mapping, BlockModelGenerators.this.modelOutput);
         ResourceLocation $$3 = ModelTemplates.WALL_TALL_SIDE.create(p_125046_, this.mapping, BlockModelGenerators.this.modelOutput);
         BlockModelGenerators.this.blockStateOutput.accept(BlockModelGenerators.createWall(p_125046_, $$1, $$2, $$3));
         ResourceLocation $$4 = ModelTemplates.WALL_INVENTORY.create(p_125046_, this.mapping, BlockModelGenerators.this.modelOutput);
         BlockModelGenerators.this.delegateItemModel(p_125046_, $$4);
         return this;
      }

      public BlockModelGenerators.BlockFamilyProvider customFence(Block p_250333_) {
         TextureMapping $$1 = TextureMapping.customParticle(p_250333_);
         ResourceLocation $$2 = ModelTemplates.CUSTOM_FENCE_POST.create(p_250333_, $$1, BlockModelGenerators.this.modelOutput);
         ResourceLocation $$3 = ModelTemplates.CUSTOM_FENCE_SIDE_NORTH.create(p_250333_, $$1, BlockModelGenerators.this.modelOutput);
         ResourceLocation $$4 = ModelTemplates.CUSTOM_FENCE_SIDE_EAST.create(p_250333_, $$1, BlockModelGenerators.this.modelOutput);
         ResourceLocation $$5 = ModelTemplates.CUSTOM_FENCE_SIDE_SOUTH.create(p_250333_, $$1, BlockModelGenerators.this.modelOutput);
         ResourceLocation $$6 = ModelTemplates.CUSTOM_FENCE_SIDE_WEST.create(p_250333_, $$1, BlockModelGenerators.this.modelOutput);
         BlockModelGenerators.this.blockStateOutput.accept(BlockModelGenerators.createCustomFence(p_250333_, $$2, $$3, $$4, $$5, $$6));
         ResourceLocation $$7 = ModelTemplates.CUSTOM_FENCE_INVENTORY.create(p_250333_, $$1, BlockModelGenerators.this.modelOutput);
         BlockModelGenerators.this.delegateItemModel(p_250333_, $$7);
         return this;
      }

      public BlockModelGenerators.BlockFamilyProvider fence(Block p_125048_) {
         ResourceLocation $$1 = ModelTemplates.FENCE_POST.create(p_125048_, this.mapping, BlockModelGenerators.this.modelOutput);
         ResourceLocation $$2 = ModelTemplates.FENCE_SIDE.create(p_125048_, this.mapping, BlockModelGenerators.this.modelOutput);
         BlockModelGenerators.this.blockStateOutput.accept(BlockModelGenerators.createFence(p_125048_, $$1, $$2));
         ResourceLocation $$3 = ModelTemplates.FENCE_INVENTORY.create(p_125048_, this.mapping, BlockModelGenerators.this.modelOutput);
         BlockModelGenerators.this.delegateItemModel(p_125048_, $$3);
         return this;
      }

      public BlockModelGenerators.BlockFamilyProvider customFenceGate(Block p_248640_) {
         TextureMapping $$1 = TextureMapping.customParticle(p_248640_);
         ResourceLocation $$2 = ModelTemplates.CUSTOM_FENCE_GATE_OPEN.create(p_248640_, $$1, BlockModelGenerators.this.modelOutput);
         ResourceLocation $$3 = ModelTemplates.CUSTOM_FENCE_GATE_CLOSED.create(p_248640_, $$1, BlockModelGenerators.this.modelOutput);
         ResourceLocation $$4 = ModelTemplates.CUSTOM_FENCE_GATE_WALL_OPEN.create(p_248640_, $$1, BlockModelGenerators.this.modelOutput);
         ResourceLocation $$5 = ModelTemplates.CUSTOM_FENCE_GATE_WALL_CLOSED.create(p_248640_, $$1, BlockModelGenerators.this.modelOutput);
         BlockModelGenerators.this.blockStateOutput.accept(BlockModelGenerators.createFenceGate(p_248640_, $$2, $$3, $$4, $$5, false));
         return this;
      }

      public BlockModelGenerators.BlockFamilyProvider fenceGate(Block p_125050_) {
         ResourceLocation $$1 = ModelTemplates.FENCE_GATE_OPEN.create(p_125050_, this.mapping, BlockModelGenerators.this.modelOutput);
         ResourceLocation $$2 = ModelTemplates.FENCE_GATE_CLOSED.create(p_125050_, this.mapping, BlockModelGenerators.this.modelOutput);
         ResourceLocation $$3 = ModelTemplates.FENCE_GATE_WALL_OPEN.create(p_125050_, this.mapping, BlockModelGenerators.this.modelOutput);
         ResourceLocation $$4 = ModelTemplates.FENCE_GATE_WALL_CLOSED.create(p_125050_, this.mapping, BlockModelGenerators.this.modelOutput);
         BlockModelGenerators.this.blockStateOutput.accept(BlockModelGenerators.createFenceGate(p_125050_, $$1, $$2, $$3, $$4, true));
         return this;
      }

      public BlockModelGenerators.BlockFamilyProvider pressurePlate(Block p_125052_) {
         ResourceLocation $$1 = ModelTemplates.PRESSURE_PLATE_UP.create(p_125052_, this.mapping, BlockModelGenerators.this.modelOutput);
         ResourceLocation $$2 = ModelTemplates.PRESSURE_PLATE_DOWN.create(p_125052_, this.mapping, BlockModelGenerators.this.modelOutput);
         BlockModelGenerators.this.blockStateOutput.accept(BlockModelGenerators.createPressurePlate(p_125052_, $$1, $$2));
         return this;
      }

      public BlockModelGenerators.BlockFamilyProvider sign(Block p_176270_) {
         if (this.family == null) {
            throw new IllegalStateException("Family not defined");
         } else {
            Block $$1 = this.family.getVariants().get(BlockFamily.Variant.WALL_SIGN);
            ResourceLocation $$2 = ModelTemplates.PARTICLE_ONLY.create(p_176270_, this.mapping, BlockModelGenerators.this.modelOutput);
            BlockModelGenerators.this.blockStateOutput.accept(BlockModelGenerators.createSimpleBlock(p_176270_, $$2));
            BlockModelGenerators.this.blockStateOutput.accept(BlockModelGenerators.createSimpleBlock($$1, $$2));
            BlockModelGenerators.this.createSimpleFlatItemModel(p_176270_.asItem());
            BlockModelGenerators.this.skipAutoItemBlock($$1);
            return this;
         }
      }

      public BlockModelGenerators.BlockFamilyProvider slab(Block p_125054_) {
         if (this.fullBlock == null) {
            throw new IllegalStateException("Full block not generated yet");
         } else {
            ResourceLocation $$1 = this.getOrCreateModel(ModelTemplates.SLAB_BOTTOM, p_125054_);
            ResourceLocation $$2 = this.getOrCreateModel(ModelTemplates.SLAB_TOP, p_125054_);
            BlockModelGenerators.this.blockStateOutput.accept(BlockModelGenerators.createSlab(p_125054_, $$1, $$2, this.fullBlock));
            BlockModelGenerators.this.delegateItemModel(p_125054_, $$1);
            return this;
         }
      }

      public BlockModelGenerators.BlockFamilyProvider stairs(Block p_125056_) {
         ResourceLocation $$1 = this.getOrCreateModel(ModelTemplates.STAIRS_INNER, p_125056_);
         ResourceLocation $$2 = this.getOrCreateModel(ModelTemplates.STAIRS_STRAIGHT, p_125056_);
         ResourceLocation $$3 = this.getOrCreateModel(ModelTemplates.STAIRS_OUTER, p_125056_);
         BlockModelGenerators.this.blockStateOutput.accept(BlockModelGenerators.createStairs(p_125056_, $$1, $$2, $$3));
         BlockModelGenerators.this.delegateItemModel(p_125056_, $$2);
         return this;
      }

      private BlockModelGenerators.BlockFamilyProvider fullBlockVariant(Block p_176272_) {
         TexturedModel $$1 = BlockModelGenerators.this.texturedModels.getOrDefault(p_176272_, TexturedModel.CUBE.get(p_176272_));
         ResourceLocation $$2 = $$1.create(p_176272_, BlockModelGenerators.this.modelOutput);
         BlockModelGenerators.this.blockStateOutput.accept(BlockModelGenerators.createSimpleBlock(p_176272_, $$2));
         return this;
      }

      private BlockModelGenerators.BlockFamilyProvider door(Block p_176274_) {
         BlockModelGenerators.this.createDoor(p_176274_);
         return this;
      }

      private void trapdoor(Block p_176276_) {
         if (BlockModelGenerators.this.nonOrientableTrapdoor.contains(p_176276_)) {
            BlockModelGenerators.this.createTrapdoor(p_176276_);
         } else {
            BlockModelGenerators.this.createOrientableTrapdoor(p_176276_);
         }
      }

      private ResourceLocation getOrCreateModel(ModelTemplate p_176262_, Block p_176263_) {
         return this.models.computeIfAbsent(p_176262_, p_176268_ -> p_176268_.create(p_176263_, this.mapping, BlockModelGenerators.this.modelOutput));
      }

      public BlockModelGenerators.BlockFamilyProvider generateFor(BlockFamily p_176260_) {
         this.family = p_176260_;
         p_176260_.getVariants().forEach((p_308473_, p_308474_) -> {
            if (!this.skipGeneratingModelsFor.contains(p_308474_)) {
               BiConsumer<BlockModelGenerators.BlockFamilyProvider, Block> $$2 = BlockModelGenerators.SHAPE_CONSUMERS.get(p_308473_);
               if ($$2 != null) {
                  $$2.accept(this, p_308474_);
               }
            }
         });
         return this;
      }
   }

   @FunctionalInterface
   interface BlockStateGeneratorSupplier {
      BlockStateGenerator create(Block var1, ResourceLocation var2, TextureMapping var3, BiConsumer<ResourceLocation, Supplier<JsonElement>> var4);
   }

   record BookSlotModelCacheKey(ModelTemplate template, String modelSuffix) {
   }

   static enum TintState {
      TINTED,
      NOT_TINTED;

      public ModelTemplate getCross() {
         return this == TINTED ? ModelTemplates.TINTED_CROSS : ModelTemplates.CROSS;
      }

      public ModelTemplate getCrossPot() {
         return this == TINTED ? ModelTemplates.TINTED_FLOWER_POT_CROSS : ModelTemplates.FLOWER_POT_CROSS;
      }
   }

   class WoodProvider {
      private final TextureMapping logMapping;

      public WoodProvider(TextureMapping p_125073_) {
         this.logMapping = p_125073_;
      }

      public BlockModelGenerators.WoodProvider wood(Block p_125075_) {
         TextureMapping $$1 = this.logMapping.copyAndUpdate(TextureSlot.END, this.logMapping.get(TextureSlot.SIDE));
         ResourceLocation $$2 = ModelTemplates.CUBE_COLUMN.create(p_125075_, $$1, BlockModelGenerators.this.modelOutput);
         BlockModelGenerators.this.blockStateOutput.accept(BlockModelGenerators.createAxisAlignedPillarBlock(p_125075_, $$2));
         return this;
      }

      public BlockModelGenerators.WoodProvider log(Block p_125077_) {
         ResourceLocation $$1 = ModelTemplates.CUBE_COLUMN.create(p_125077_, this.logMapping, BlockModelGenerators.this.modelOutput);
         BlockModelGenerators.this.blockStateOutput.accept(BlockModelGenerators.createAxisAlignedPillarBlock(p_125077_, $$1));
         return this;
      }

      public BlockModelGenerators.WoodProvider logWithHorizontal(Block p_125079_) {
         ResourceLocation $$1 = ModelTemplates.CUBE_COLUMN.create(p_125079_, this.logMapping, BlockModelGenerators.this.modelOutput);
         ResourceLocation $$2 = ModelTemplates.CUBE_COLUMN_HORIZONTAL.create(p_125079_, this.logMapping, BlockModelGenerators.this.modelOutput);
         BlockModelGenerators.this.blockStateOutput.accept(BlockModelGenerators.createRotatedPillarWithHorizontalVariant(p_125079_, $$1, $$2));
         return this;
      }

      public BlockModelGenerators.WoodProvider logUVLocked(Block p_259915_) {
         BlockModelGenerators.this.blockStateOutput
            .accept(BlockModelGenerators.createPillarBlockUVLocked(p_259915_, this.logMapping, BlockModelGenerators.this.modelOutput));
         return this;
      }
   }
}
