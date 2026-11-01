package net.minecraft.client.renderer.block.model;

import com.mojang.math.Transformation;
import java.util.List;
import java.util.Map.Entry;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelDebugName;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.QuadCollection;
import net.minecraft.client.resources.model.UnbakedGeometry;
import net.minecraft.core.Direction;
import net.minecraft.util.context.ContextMap;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.model.ExtendedUnbakedGeometry;
import net.neoforged.neoforge.client.model.NeoForgeModelProperties;
import net.neoforged.neoforge.client.model.UnbakedElementsHelper;
import org.joml.Vector3fc;

@OnlyIn(Dist.CLIENT)
public record SimpleUnbakedGeometry(List<BlockElement> elements) implements UnbakedGeometry, ExtendedUnbakedGeometry {
   @Override
   public QuadCollection bake(TextureSlots p_405009_, ModelBaker p_404737_, ModelState p_404670_, ModelDebugName p_404742_, ContextMap additionalProperties) {
      Transformation transform = additionalProperties.getOptional(NeoForgeModelProperties.TRANSFORM);
      if (transform != null) {
         p_404670_ = UnbakedElementsHelper.composeRootTransformIntoModelState(p_404670_, transform);
      }

      return bake(this.elements, p_405009_, p_404737_, p_404670_, p_404742_);
   }

   public static QuadCollection bake(List<BlockElement> p_405653_, TextureSlots p_404888_, ModelBaker p_470614_, ModelState p_405413_, ModelDebugName p_404876_) {
      QuadCollection.Builder quadcollection$builder = new QuadCollection.Builder();

      for (BlockElement blockelement : p_405653_) {
         boolean flag = true;
         boolean flag1 = true;
         boolean flag2 = true;
         Vector3fc vector3fc = blockelement.from();
         Vector3fc vector3fc1 = blockelement.to();
         if (vector3fc.x() == vector3fc1.x()) {
            flag1 = false;
            flag2 = false;
         }

         if (vector3fc.y() == vector3fc1.y()) {
            flag = false;
            flag2 = false;
         }

         if (vector3fc.z() == vector3fc1.z()) {
            flag = false;
            flag1 = false;
         }

         if (flag || flag1 || flag2) {
            for (Entry<Direction, BlockElementFace> entry : blockelement.faces().entrySet()) {
               Direction direction = entry.getKey();
               BlockElementFace blockelementface = entry.getValue();

               boolean flag3 = switch (direction.getAxis()) {
                  case X -> flag;
                  case Y -> flag1;
                  case Z -> flag2;
               };
               if (flag3) {
                  TextureAtlasSprite textureatlassprite = p_470614_.sprites().resolveSlot(p_404888_, blockelementface.texture(), p_404876_);
                  BakedQuad bakedquad = FaceBakery.bakeQuad(
                     p_470614_.parts(),
                     vector3fc,
                     vector3fc1,
                     blockelementface,
                     textureatlassprite,
                     direction,
                     p_405413_,
                     blockelement.rotation(),
                     blockelement.shade(),
                     blockelement.lightEmission()
                  );
                  if (blockelementface.cullForDirection() == null) {
                     quadcollection$builder.addUnculledFace(bakedquad);
                  } else {
                     quadcollection$builder.addCulledFace(
                        Direction.rotate(p_405413_.transformation().getMatrix(), blockelementface.cullForDirection()), bakedquad
                     );
                  }
               }
            }
         }
      }

      return quadcollection$builder.build();
   }
}
