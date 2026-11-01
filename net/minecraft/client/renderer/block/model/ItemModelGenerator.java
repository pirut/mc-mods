package net.minecraft.client.renderer.block.model;

import com.mojang.math.Quadrant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelDebugName;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.QuadCollection;
import net.minecraft.client.resources.model.UnbakedGeometry;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class ItemModelGenerator implements UnbakedModel {
   public static final Identifier GENERATED_ITEM_MODEL_ID = Identifier.withDefaultNamespace("builtin/generated");
   public static final List<String> LAYERS = List.of("layer0", "layer1", "layer2", "layer3", "layer4");
   private static final float MIN_Z = 7.5F;
   private static final float MAX_Z = 8.5F;
   private static final TextureSlots.Data TEXTURE_SLOTS = new TextureSlots.Data.Builder().addReference("particle", "layer0").build();
   private static final BlockElementFace.UVs SOUTH_FACE_UVS = new BlockElementFace.UVs(0.0F, 0.0F, 16.0F, 16.0F);
   private static final BlockElementFace.UVs NORTH_FACE_UVS = new BlockElementFace.UVs(16.0F, 0.0F, 0.0F, 16.0F);
   private static final float UV_SHRINK = 0.1F;

   @Override
   public TextureSlots.Data textureSlots() {
      return TEXTURE_SLOTS;
   }

   @Override
   public UnbakedGeometry geometry() {
      return ItemModelGenerator::bake;
   }

   @Nullable
   @Override
   public UnbakedModel.GuiLight guiLight() {
      return UnbakedModel.GuiLight.FRONT;
   }

   private static QuadCollection bake(TextureSlots textureSlots, ModelBaker baker, ModelState modelState, ModelDebugName debugName) {
      List<BlockElement> list = new ArrayList<>();

      for (int i = 0; i < LAYERS.size(); i++) {
         String string = LAYERS.get(i);
         Material material = textureSlots.getMaterial(string);
         if (material == null) {
            break;
         }

         SpriteContents spriteContents = baker.sprites().get(material, debugName).contents();
         list.addAll(processFrames(i, string, spriteContents));
      }

      return SimpleUnbakedGeometry.bake(list, textureSlots, baker, modelState, debugName);
   }

   private static List<BlockElement> processFrames(int tintIndex, String texture, SpriteContents sprite) {
      Map<Direction, BlockElementFace> map = Map.of(
         Direction.SOUTH,
         new BlockElementFace(null, tintIndex, texture, SOUTH_FACE_UVS, Quadrant.R0),
         Direction.NORTH,
         new BlockElementFace(null, tintIndex, texture, NORTH_FACE_UVS, Quadrant.R0)
      );
      List<BlockElement> list = new ArrayList<>();
      list.add(new BlockElement(new Vector3f(0.0F, 0.0F, 7.5F), new Vector3f(16.0F, 16.0F, 8.5F), map));
      list.addAll(createSideElements(sprite, texture, tintIndex));
      return list;
   }

   private static List<BlockElement> createSideElements(SpriteContents sprite, String texture, int tintIndex) {
      float f = 16.0F / sprite.width();
      float g = 16.0F / sprite.height();
      List<BlockElement> list = new ArrayList<>();

      for (ItemModelGenerator.SideFace sideFace : getSideFaces(sprite)) {
         float h = sideFace.x();
         float i = sideFace.y();
         ItemModelGenerator.SideDirection sideDirection = sideFace.facing();
         float j = h + 0.1F;
         float k = h + 1.0F - 0.1F;
         float l;
         float m;
         if (sideDirection.isHorizontal()) {
            l = i + 0.1F;
            m = i + 1.0F - 0.1F;
         } else {
            l = i + 1.0F - 0.1F;
            m = i + 0.1F;
         }

         float n = h;
         float o = i;
         float p = h;
         float q = i;
         switch (sideDirection) {
            case UP:
               p = h + 1.0F;
               break;
            case DOWN:
               p = h + 1.0F;
               o = i + 1.0F;
               q = i + 1.0F;
               break;
            case LEFT:
               q = i + 1.0F;
               break;
            case RIGHT:
               n = h + 1.0F;
               p = h + 1.0F;
               q = i + 1.0F;
         }

         n *= f;
         p *= f;
         o *= g;
         q *= g;
         o = 16.0F - o;
         q = 16.0F - q;
         Map<Direction, BlockElementFace> map = Map.of(
            sideDirection.getDirection(), new BlockElementFace(null, tintIndex, texture, new BlockElementFace.UVs(j * f, l * f, k * g, m * g), Quadrant.R0)
         );
         switch (sideDirection) {
            case UP:
               list.add(new BlockElement(new Vector3f(n, o, 7.5F), new Vector3f(p, o, 8.5F), map));
               break;
            case DOWN:
               list.add(new BlockElement(new Vector3f(n, q, 7.5F), new Vector3f(p, q, 8.5F), map));
               break;
            case LEFT:
               list.add(new BlockElement(new Vector3f(n, o, 7.5F), new Vector3f(n, q, 8.5F), map));
               break;
            case RIGHT:
               list.add(new BlockElement(new Vector3f(p, o, 7.5F), new Vector3f(p, q, 8.5F), map));
         }
      }

      return list;
   }

   private static Collection<ItemModelGenerator.SideFace> getSideFaces(SpriteContents sprite) {
      int i = sprite.width();
      int j = sprite.height();
      Set<ItemModelGenerator.SideFace> set = new HashSet<>();
      sprite.getUniqueFrames().forEach(k -> {
         for (int l = 0; l < j; l++) {
            for (int m = 0; m < i; m++) {
               boolean bl = !isTransparent(sprite, k, m, l, i, j);
               if (bl) {
                  checkTransition(ItemModelGenerator.SideDirection.UP, set, sprite, k, m, l, i, j);
                  checkTransition(ItemModelGenerator.SideDirection.DOWN, set, sprite, k, m, l, i, j);
                  checkTransition(ItemModelGenerator.SideDirection.LEFT, set, sprite, k, m, l, i, j);
                  checkTransition(ItemModelGenerator.SideDirection.RIGHT, set, sprite, k, m, l, i, j);
               }
            }
         }
      });
      return set;
   }

   private static void checkTransition(
      ItemModelGenerator.SideDirection sideDirection,
      Set<ItemModelGenerator.SideFace> output,
      SpriteContents sprite,
      int frame,
      int x,
      int y,
      int width,
      int height
   ) {
      if (isTransparent(sprite, frame, x - sideDirection.direction.getStepX(), y - sideDirection.direction.getStepY(), width, height)) {
         output.add(new ItemModelGenerator.SideFace(sideDirection, x, y));
      }
   }

   private static boolean isTransparent(SpriteContents sprite, int frameIndex, int pixelX, int pixelY, int spriteWidth, int spriteHeight) {
      return pixelX >= 0 && pixelY >= 0 && pixelX < spriteWidth && pixelY < spriteHeight ? sprite.isTransparent(frameIndex, pixelX, pixelY) : true;
   }

   @Environment(EnvType.CLIENT)
   static enum SideDirection {
      UP(Direction.UP),
      DOWN(Direction.DOWN),
      LEFT(Direction.EAST),
      RIGHT(Direction.WEST);

      final Direction direction;

      private SideDirection(final Direction direction) {
         this.direction = direction;
      }

      public Direction getDirection() {
         return this.direction;
      }

      boolean isHorizontal() {
         return this == DOWN || this == UP;
      }
   }

   @Environment(EnvType.CLIENT)
   record SideFace(ItemModelGenerator.SideDirection facing, int x, int y) {
   }
}
