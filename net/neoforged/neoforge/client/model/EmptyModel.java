package net.neoforged.neoforge.client.model;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.SimpleBakedModel;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.client.RenderTypeGroup;
import net.neoforged.neoforge.client.model.geometry.IGeometryBakingContext;
import net.neoforged.neoforge.client.model.geometry.IGeometryLoader;
import net.neoforged.neoforge.client.model.geometry.SimpleUnbakedGeometry;
import net.neoforged.neoforge.client.textures.UnitTextureAtlasSprite;

public class EmptyModel extends SimpleUnbakedGeometry<EmptyModel> {
   public static final BakedModel BAKED = new EmptyModel.Baked();
   public static final EmptyModel INSTANCE = new EmptyModel();
   public static final IGeometryLoader<EmptyModel> LOADER = (json, ctx) -> INSTANCE;

   private EmptyModel() {
   }

   @Override
   protected void addQuads(
      IGeometryBakingContext owner,
      IModelBuilder<?> modelBuilder,
      ModelBaker baker,
      Function<Material, TextureAtlasSprite> spriteGetter,
      ModelState modelTransform
   ) {
   }

   @Override
   public BakedModel bake(
      IGeometryBakingContext context, ModelBaker baker, Function<Material, TextureAtlasSprite> spriteGetter, ModelState modelState, ItemOverrides overrides
   ) {
      return BAKED;
   }

   private static class Baked extends SimpleBakedModel {
      private static final Material MISSING_TEXTURE = new Material(TextureAtlas.LOCATION_BLOCKS, MissingTextureAtlasSprite.getLocation());

      private static Map<Direction, List<BakedQuad>> makeEmptyCulledFaces() {
         Map<Direction, List<BakedQuad>> map = new EnumMap<>(Direction.class);

         for (Direction direction : Direction.values()) {
            map.put(direction, List.of());
         }

         return map;
      }

      public Baked() {
         super(
            List.of(),
            makeEmptyCulledFaces(),
            false,
            false,
            false,
            UnitTextureAtlasSprite.INSTANCE,
            ItemTransforms.NO_TRANSFORMS,
            ItemOverrides.EMPTY,
            RenderTypeGroup.EMPTY
         );
      }

      @Override
      public TextureAtlasSprite getParticleIcon() {
         return MISSING_TEXTURE.sprite();
      }
   }
}
