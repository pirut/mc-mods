package net.neoforged.neoforge.client.model;

import com.mojang.math.Quadrant;
import com.mojang.math.Transformation;
import java.util.BitSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockElement;
import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.client.renderer.block.model.FaceBakery;
import net.minecraft.client.renderer.block.model.ItemModelGenerator;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.QuadCollection;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

public final class UnbakedElementsHelper {
   private static final ModelBaker.PartCache DUMMY_PART_CACHE = vector -> vector;

   private UnbakedElementsHelper() {
   }

   public static List<BlockElement> createUnbakedItemElements(int layerIndex, TextureAtlasSprite sprite) {
      return createUnbakedItemElements(layerIndex, sprite, null);
   }

   public static List<BlockElement> createUnbakedItemElements(int layerIndex, TextureAtlasSprite sprite, @Nullable ExtraFaceData faceData) {
      List<BlockElement> elements = ItemModelGenerator.processFrames(layerIndex, "layer" + layerIndex, sprite.contents());
      if (faceData != null) {
         elements.replaceAll(elem -> new BlockElement(elem.from(), elem.to(), elem.faces(), elem.rotation(), elem.shade(), elem.lightEmission(), faceData));
      }

      return elements;
   }

   public static List<BlockElement> createUnbakedItemMaskElements(int layerIndex, TextureAtlasSprite sprite) {
      return createUnbakedItemMaskElements(layerIndex, sprite, null);
   }

   public static List<BlockElement> createUnbakedItemMaskElements(int layerIndex, TextureAtlasSprite sprite, @Nullable ExtraFaceData faceData) {
      List<BlockElement> elements = createUnbakedItemElements(layerIndex, sprite, faceData);
      elements.removeFirst();
      SpriteContents spriteContents = sprite.contents();
      int width = spriteContents.width();
      int height = spriteContents.height();
      BitSet bits = new BitSet(width * height);
      spriteContents.getUniqueFrames().forEach(frame -> {
         for (int xx = 0; xx < width; xx++) {
            for (int yx = 0; yx < height; yx++) {
               if (!spriteContents.isTransparent(frame, xx, yx)) {
                  bits.set(xx + yx * width);
               }
            }
         }
      });

      for (int y = 0; y < height; y++) {
         int xStart = -1;

         for (int x = 0; x < width; x++) {
            boolean opaque = bits.get(x + y * width);
            if (opaque == (xStart == -1)) {
               if (xStart == -1) {
                  xStart = x;
               } else {
                  int yEnd;
                  label63:
                  for (yEnd = y + 1; yEnd < height; yEnd++) {
                     for (int x2 = xStart; x2 <= x; x2++) {
                        if (!bits.get(x2 + yEnd * width)) {
                           break label63;
                        }
                     }
                  }

                  for (int i = xStart; i < x; i++) {
                     for (int j = y; j < yEnd; j++) {
                        bits.clear(i + j * width);
                     }
                  }

                  Vector3f from = new Vector3f((float)(16 * xStart) / width, 16.0F - (float)(16 * yEnd) / height, 7.5F);
                  Vector3f to = new Vector3f((float)(16 * x) / width, 16.0F - (float)(16 * y) / height, 8.5F);
                  BlockElementFace.UVs northUvs = FaceBakery.defaultFaceUV(from, to, Direction.NORTH);
                  BlockElementFace.UVs southUvs = FaceBakery.defaultFaceUV(from, to, Direction.SOUTH);
                  Map<Direction, BlockElementFace> faces = Map.of(
                     Direction.NORTH,
                     new BlockElementFace(null, layerIndex, "layer" + layerIndex, northUvs, Quadrant.R0),
                     Direction.SOUTH,
                     new BlockElementFace(null, layerIndex, "layer" + layerIndex, southUvs, Quadrant.R0)
                  );
                  elements.add(new BlockElement(from, to, faces, null, true, 0));
                  xStart = -1;
               }
            }
         }
      }

      return elements;
   }

   public static void bakeElements(
      QuadCollection.Builder builder, List<BlockElement> elements, Function<Material, TextureAtlasSprite> spriteGetter, ModelState modelState
   ) {
      for (BlockElement element : elements) {
         element.faces()
            .forEach(
               (side, face) -> {
                  TextureAtlasSprite sprite = spriteGetter.apply(new Material(TextureAtlas.LOCATION_BLOCKS, Identifier.parse(face.texture())));
                  BakedQuad quad = FaceBakery.bakeQuad(
                     DUMMY_PART_CACHE,
                     element.from(),
                     element.to(),
                     face,
                     sprite,
                     side,
                     modelState,
                     element.rotation(),
                     element.shade(),
                     element.lightEmission()
                  );
                  if (face.cullForDirection() == null) {
                     builder.addUnculledFace(quad);
                  } else {
                     builder.addCulledFace(Direction.rotate(modelState.transformation().getMatrix(), face.cullForDirection()), quad);
                  }
               }
            );
      }
   }

   public static List<BakedQuad> bakeElements(List<BlockElement> elements, Function<Material, TextureAtlasSprite> spriteGetter, ModelState modelState) {
      if (elements.isEmpty()) {
         return List.of();
      } else {
         QuadCollection.Builder builder = new QuadCollection.Builder();
         bakeElements(builder, elements, spriteGetter, modelState);
         return builder.build().getAll();
      }
   }

   public static ModelState composeRootTransformIntoModelState(ModelState modelState, Transformation rootTransform) {
      if (rootTransform.isIdentity()) {
         return modelState;
      } else {
         rootTransform = rootTransform.applyOrigin(new Vector3f(-0.5F, -0.5F, -0.5F));
         return new ComposedModelState(modelState, rootTransform);
      }
   }
}
