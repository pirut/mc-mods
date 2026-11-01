package net.minecraft.client.data.models.model;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.extensions.ITexturedModelExtension;

@OnlyIn(Dist.CLIENT)
public class TexturedModel implements ITexturedModelExtension {
   public static final TexturedModel.Provider CUBE = createDefault(TextureMapping::cube, ModelTemplates.CUBE_ALL);
   public static final TexturedModel.Provider CUBE_INNER_FACES = createDefault(TextureMapping::cube, ModelTemplates.CUBE_ALL_INNER_FACES);
   public static final TexturedModel.Provider CUBE_MIRRORED = createDefault(TextureMapping::cube, ModelTemplates.CUBE_MIRRORED_ALL);
   public static final TexturedModel.Provider COLUMN = createDefault(TextureMapping::column, ModelTemplates.CUBE_COLUMN);
   public static final TexturedModel.Provider COLUMN_HORIZONTAL = createDefault(TextureMapping::column, ModelTemplates.CUBE_COLUMN_HORIZONTAL);
   public static final TexturedModel.Provider CUBE_TOP_BOTTOM = createDefault(TextureMapping::cubeBottomTop, ModelTemplates.CUBE_BOTTOM_TOP);
   public static final TexturedModel.Provider CUBE_TOP = createDefault(TextureMapping::cubeTop, ModelTemplates.CUBE_TOP);
   public static final TexturedModel.Provider ORIENTABLE_ONLY_TOP = createDefault(TextureMapping::orientableCubeOnlyTop, ModelTemplates.CUBE_ORIENTABLE);
   public static final TexturedModel.Provider ORIENTABLE = createDefault(TextureMapping::orientableCube, ModelTemplates.CUBE_ORIENTABLE_TOP_BOTTOM);
   public static final TexturedModel.Provider CARPET = createDefault(TextureMapping::wool, ModelTemplates.CARPET);
   public static final TexturedModel.Provider MOSSY_CARPET_SIDE = createDefault(TextureMapping::side, ModelTemplates.MOSSY_CARPET_SIDE);
   public static final TexturedModel.Provider FLOWERBED_1 = createDefault(TextureMapping::flowerbed, ModelTemplates.FLOWERBED_1);
   public static final TexturedModel.Provider FLOWERBED_2 = createDefault(TextureMapping::flowerbed, ModelTemplates.FLOWERBED_2);
   public static final TexturedModel.Provider FLOWERBED_3 = createDefault(TextureMapping::flowerbed, ModelTemplates.FLOWERBED_3);
   public static final TexturedModel.Provider FLOWERBED_4 = createDefault(TextureMapping::flowerbed, ModelTemplates.FLOWERBED_4);
   public static final TexturedModel.Provider LEAF_LITTER_1 = createDefault(TextureMapping::defaultTexture, ModelTemplates.LEAF_LITTER_1);
   public static final TexturedModel.Provider LEAF_LITTER_2 = createDefault(TextureMapping::defaultTexture, ModelTemplates.LEAF_LITTER_2);
   public static final TexturedModel.Provider LEAF_LITTER_3 = createDefault(TextureMapping::defaultTexture, ModelTemplates.LEAF_LITTER_3);
   public static final TexturedModel.Provider LEAF_LITTER_4 = createDefault(TextureMapping::defaultTexture, ModelTemplates.LEAF_LITTER_4);
   public static final TexturedModel.Provider GLAZED_TERRACOTTA = createDefault(TextureMapping::pattern, ModelTemplates.GLAZED_TERRACOTTA);
   public static final TexturedModel.Provider CORAL_FAN = createDefault(TextureMapping::fan, ModelTemplates.CORAL_FAN);
   public static final TexturedModel.Provider ANVIL = createDefault(TextureMapping::top, ModelTemplates.ANVIL);
   public static final TexturedModel.Provider LEAVES = createDefault(TextureMapping::cube, ModelTemplates.LEAVES);
   public static final TexturedModel.Provider LANTERN = createDefault(TextureMapping::lantern, ModelTemplates.LANTERN);
   public static final TexturedModel.Provider HANGING_LANTERN = createDefault(TextureMapping::lantern, ModelTemplates.HANGING_LANTERN);
   public static final TexturedModel.Provider CHAIN = createDefault(TextureMapping::defaultTexture, ModelTemplates.CHAIN);
   public static final TexturedModel.Provider SEAGRASS = createDefault(TextureMapping::defaultTexture, ModelTemplates.SEAGRASS);
   public static final TexturedModel.Provider COLUMN_ALT = createDefault(TextureMapping::logColumn, ModelTemplates.CUBE_COLUMN);
   public static final TexturedModel.Provider COLUMN_HORIZONTAL_ALT = createDefault(TextureMapping::logColumn, ModelTemplates.CUBE_COLUMN_HORIZONTAL);
   public static final TexturedModel.Provider TOP_BOTTOM_WITH_WALL = createDefault(TextureMapping::cubeBottomTopWithWall, ModelTemplates.CUBE_BOTTOM_TOP);
   public static final TexturedModel.Provider COLUMN_WITH_WALL = createDefault(TextureMapping::columnWithWall, ModelTemplates.CUBE_COLUMN);
   private final TextureMapping mapping;
   private final ModelTemplate template;

   public TexturedModel(TextureMapping p_387169_, ModelTemplate p_387271_) {
      this.mapping = p_387169_;
      this.template = p_387271_;
   }

   public ModelTemplate getTemplate() {
      return this.template;
   }

   public TextureMapping getMapping() {
      return this.mapping;
   }

   public TexturedModel updateTextures(Consumer<TextureMapping> p_386813_) {
      p_386813_.accept(this.mapping);
      return this;
   }

   public Identifier create(Block p_386676_, BiConsumer<Identifier, ModelInstance> p_386790_) {
      return this.template.create(p_386676_, this.mapping, p_386790_);
   }

   public Identifier createWithSuffix(Block p_388536_, String p_387320_, BiConsumer<Identifier, ModelInstance> p_387896_) {
      return this.template.createWithSuffix(p_388536_, p_387320_, this.mapping, p_387896_);
   }

   public static TexturedModel.Provider createDefault(Function<Block, TextureMapping> p_386771_, ModelTemplate p_388272_) {
      return p_386939_ -> new TexturedModel(p_386771_.apply(p_386939_), p_388272_);
   }

   public static TexturedModel createAllSame(Identifier p_467139_) {
      return new TexturedModel(TextureMapping.cube(p_467139_), ModelTemplates.CUBE_ALL);
   }

   @FunctionalInterface
   @OnlyIn(Dist.CLIENT)
   public interface Provider extends ITexturedModelExtension.Provider {
      TexturedModel get(Block var1);

      default Identifier create(Block p_388828_, BiConsumer<Identifier, ModelInstance> p_386557_) {
         return this.get(p_388828_).create(p_388828_, p_386557_);
      }

      default Identifier createWithSuffix(Block p_387717_, String p_388478_, BiConsumer<Identifier, ModelInstance> p_388677_) {
         return this.get(p_387717_).createWithSuffix(p_387717_, p_388478_, p_388677_);
      }

      default TexturedModel.Provider updateTexture(Consumer<TextureMapping> p_387232_) {
         return p_388908_ -> this.get(p_388908_).updateTextures(p_387232_);
      }
   }
}
