package net.minecraft.client.renderer.block.model;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.math.MatrixUtil;
import com.mojang.math.Quadrant;
import com.mojang.math.Transformation;
import java.util.Objects;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.FaceInfo;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.core.Direction;
import org.joml.GeometryUtils;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class FaceBakery {
   private static final Vector3fc BLOCK_MIDDLE = new Vector3f(0.5F, 0.5F, 0.5F);

   @VisibleForTesting
   static BlockElementFace.UVs defaultFaceUV(Vector3fc posFrom, Vector3fc posTo, Direction facing) {
      return switch (facing) {
         case DOWN -> new BlockElementFace.UVs(posFrom.x(), 16.0F - posTo.z(), posTo.x(), 16.0F - posFrom.z());
         case UP -> new BlockElementFace.UVs(posFrom.x(), posFrom.z(), posTo.x(), posTo.z());
         case NORTH -> new BlockElementFace.UVs(16.0F - posTo.x(), 16.0F - posTo.y(), 16.0F - posFrom.x(), 16.0F - posFrom.y());
         case SOUTH -> new BlockElementFace.UVs(posFrom.x(), 16.0F - posTo.y(), posTo.x(), 16.0F - posFrom.y());
         case WEST -> new BlockElementFace.UVs(posFrom.z(), 16.0F - posTo.y(), posTo.z(), 16.0F - posFrom.y());
         case EAST -> new BlockElementFace.UVs(16.0F - posTo.z(), 16.0F - posTo.y(), 16.0F - posFrom.z(), 16.0F - posFrom.y());
      };
   }

   public static BakedQuad bakeQuad(
      ModelBaker.PartCache partCache,
      Vector3fc posFrom,
      Vector3fc posTo,
      BlockElementFace face,
      TextureAtlasSprite sprite,
      Direction facing,
      ModelState modelState,
      @Nullable BlockElementRotation rotation,
      boolean shade,
      int lightEmission
   ) {
      BlockElementFace.UVs uVs = face.uvs();
      if (uVs == null) {
         uVs = defaultFaceUV(posFrom, posTo, facing);
      }

      Matrix4fc matrix4fc = modelState.inverseFaceTransformation(facing);
      Vector3fc[] vector3fcs = new Vector3fc[4];
      long[] ls = new long[4];
      FaceInfo faceInfo = FaceInfo.fromFacing(facing);

      for (int i = 0; i < 4; i++) {
         bakeVertex(i, faceInfo, uVs, face.rotation(), matrix4fc, posFrom, posTo, sprite, modelState.transformation(), rotation, vector3fcs, ls, partCache);
      }

      Direction direction = calculateFacing(vector3fcs);
      if (rotation == null && direction != null) {
         recalculateWinding(vector3fcs, ls, direction);
      }

      return new BakedQuad(
         vector3fcs[0],
         vector3fcs[1],
         vector3fcs[2],
         vector3fcs[3],
         ls[0],
         ls[1],
         ls[2],
         ls[3],
         face.tintIndex(),
         Objects.requireNonNullElse(direction, Direction.UP),
         sprite,
         shade,
         lightEmission
      );
   }

   private static void bakeVertex(
      int vertexIndex,
      FaceInfo faceInfo,
      BlockElementFace.UVs uvs,
      Quadrant faceRotation,
      Matrix4fc inverseFaceTransform,
      Vector3fc posFrom,
      Vector3fc posTo,
      TextureAtlasSprite sprite,
      Transformation transformation,
      @Nullable BlockElementRotation rotation,
      Vector3fc[] positions,
      long[] packedUVs,
      ModelBaker.PartCache partCache
   ) {
      FaceInfo.VertexInfo vertexInfo = faceInfo.getVertexInfo(vertexIndex);
      Vector3f vector3f = vertexInfo.select(posFrom, posTo).div(16.0F);
      if (rotation != null) {
         rotateVertexBy(vector3f, rotation.origin(), rotation.transform());
      }

      if (transformation != Transformation.identity()) {
         rotateVertexBy(vector3f, BLOCK_MIDDLE, transformation.getMatrix());
      }

      float f = BlockElementFace.getU(uvs, faceRotation, vertexIndex);
      float g = BlockElementFace.getV(uvs, faceRotation, vertexIndex);
      float i;
      float h;
      if (MatrixUtil.isIdentity(inverseFaceTransform)) {
         h = f;
         i = g;
      } else {
         Vector3f vector3f2 = inverseFaceTransform.transformPosition(new Vector3f(cornerToCenter(f), cornerToCenter(g), 0.0F));
         h = centerToCorner(vector3f2.x);
         i = centerToCorner(vector3f2.y);
      }

      positions[vertexIndex] = partCache.vector(vector3f);
      packedUVs[vertexIndex] = UVPair.pack(sprite.getU(h), sprite.getV(i));
   }

   private static float cornerToCenter(float coord) {
      return coord - 0.5F;
   }

   private static float centerToCorner(float coord) {
      return coord + 0.5F;
   }

   private static void rotateVertexBy(Vector3f vertex, Vector3fc origin, Matrix4fc transform) {
      vertex.sub(origin);
      transform.transformPosition(vertex);
      vertex.add(origin);
   }

   @Nullable
   private static Direction calculateFacing(Vector3fc[] positions) {
      Vector3f vector3f = new Vector3f();
      GeometryUtils.normal(positions[0], positions[1], positions[2], vector3f);
      return findClosestDirection(vector3f);
   }

   @Nullable
   private static Direction findClosestDirection(Vector3f pos) {
      if (!pos.isFinite()) {
         return null;
      } else {
         Direction direction = null;
         float f = 0.0F;

         for (Direction direction2 : Direction.values()) {
            float g = pos.dot(direction2.getUnitVec3f());
            if (g >= 0.0F && g > f) {
               f = g;
               direction = direction2;
            }
         }

         return direction;
      }
   }

   private static void recalculateWinding(Vector3fc[] positions, long[] packedUVs, Direction facing) {
      float f = 999.0F;
      float g = 999.0F;
      float h = 999.0F;
      float i = -999.0F;
      float j = -999.0F;
      float k = -999.0F;

      for (int l = 0; l < 4; l++) {
         Vector3fc vector3fc = positions[l];
         float m = vector3fc.x();
         float n = vector3fc.y();
         float o = vector3fc.z();
         if (m < f) {
            f = m;
         }

         if (n < g) {
            g = n;
         }

         if (o < h) {
            h = o;
         }

         if (m > i) {
            i = m;
         }

         if (n > j) {
            j = n;
         }

         if (o > k) {
            k = o;
         }
      }

      FaceInfo faceInfo = FaceInfo.fromFacing(facing);

      for (int p = 0; p < 4; p++) {
         FaceInfo.VertexInfo vertexInfo = faceInfo.getVertexInfo(p);
         float nx = vertexInfo.xFace().select(f, g, h, i, j, k);
         float ox = vertexInfo.yFace().select(f, g, h, i, j, k);
         float q = vertexInfo.zFace().select(f, g, h, i, j, k);
         int r = findVertex(positions, p, nx, ox, q);
         if (r == -1) {
            throw new IllegalStateException("Can't find vertex to swap");
         }

         if (r != p) {
            swap(positions, r, p);
            swap(packedUVs, r, p);
         }
      }
   }

   private static int findVertex(Vector3fc[] positions, int startIndex, float x, float y, float z) {
      for (int i = startIndex; i < 4; i++) {
         Vector3fc vector3fc = positions[i];
         if (x == vector3fc.x() && y == vector3fc.y() && z == vector3fc.z()) {
            return i;
         }
      }

      return -1;
   }

   private static void swap(Vector3fc[] array, int index1, int index2) {
      Vector3fc vector3fc = array[index1];
      array[index1] = array[index2];
      array[index2] = vector3fc;
   }

   private static void swap(long[] array, int index1, int index2) {
      long l = array[index1];
      array[index1] = array[index2];
      array[index2] = l;
   }
}
