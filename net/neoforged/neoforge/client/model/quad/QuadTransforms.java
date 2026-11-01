package net.neoforged.neoforge.client.model.quad;

import com.mojang.math.Transformation;
import net.minecraft.client.renderer.block.model.BakedQuad;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.joml.Vector4f;

public final class QuadTransforms {
   private QuadTransforms() {
   }

   public static BakedQuad applyTransformation(BakedQuad quad, Transformation transformation) {
      if (transformation.isIdentity()) {
         return quad;
      } else {
         BakedNormals updatedNormals;
         if (quad.bakedNormals() == BakedNormals.UNSPECIFIED) {
            updatedNormals = BakedNormals.UNSPECIFIED;
         } else {
            Vector3f normalTemp = new Vector3f();

            updatedNormals = switch (quad.bakedNormals()) {
               case BakedNormals.PerQuad(int var20) -> BakedNormals.of(transformNormal(normalTemp, var20, transformation));
               case BakedNormals.PerVertex(int var13, int var21, int var22, int var23) -> BakedNormals.of(
                  transformNormal(normalTemp, var13, transformation),
                  transformNormal(normalTemp, var21, transformation),
                  transformNormal(normalTemp, var22, transformation),
                  transformNormal(normalTemp, var23, transformation)
               );
               default -> throw new MatchException(null, null);
            };
         }

         Vector4f posTemp = new Vector4f();
         return new BakedQuad(
            transformPosition(posTemp, quad.position0(), transformation),
            transformPosition(posTemp, quad.position1(), transformation),
            transformPosition(posTemp, quad.position2(), transformation),
            transformPosition(posTemp, quad.position3(), transformation),
            quad.packedUV0(),
            quad.packedUV1(),
            quad.packedUV2(),
            quad.packedUV3(),
            quad.tintIndex(),
            quad.direction(),
            quad.sprite(),
            quad.shade(),
            quad.lightEmission(),
            updatedNormals,
            quad.bakedColors(),
            quad.hasAmbientOcclusion()
         );
      }
   }

   private static Vector3fc transformPosition(Vector4f temp, Vector3fc pos, Transformation transformation) {
      temp.set(pos.x(), pos.y(), pos.z(), 1.0F);
      transformation.transformPosition(temp);
      temp.div(temp.w);
      return new Vector3f(temp.x(), temp.y(), temp.z());
   }

   private static int transformNormal(Vector3f temp, int packedNormal, Transformation transformation) {
      BakedNormals.unpack(packedNormal, temp);
      transformation.transformNormal(temp);
      return BakedNormals.pack(temp);
   }
}
