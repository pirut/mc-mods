package net.minecraft.client.renderer.block.model;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.math.MatrixUtil;
import com.mojang.math.Quadrant;
import com.mojang.math.Transformation;
import java.util.Objects;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.FaceInfo;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.core.Direction;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.model.quad.BakedColors;
import net.neoforged.neoforge.client.model.quad.BakedNormals;
import org.joml.GeometryUtils;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class FaceBakery {
   private static final Vector3fc BLOCK_MIDDLE = new Vector3f(0.5F, 0.5F, 0.5F);

   @VisibleForTesting
   public static BlockElementFace.UVs defaultFaceUV(Vector3fc p_404839_, Vector3fc p_405755_, Direction p_404690_) {
      return switch (p_404690_) {
         case DOWN -> new BlockElementFace.UVs(p_404839_.x(), 16.0F - p_405755_.z(), p_405755_.x(), 16.0F - p_404839_.z());
         case UP -> new BlockElementFace.UVs(p_404839_.x(), p_404839_.z(), p_405755_.x(), p_405755_.z());
         case NORTH -> new BlockElementFace.UVs(16.0F - p_405755_.x(), 16.0F - p_405755_.y(), 16.0F - p_404839_.x(), 16.0F - p_404839_.y());
         case SOUTH -> new BlockElementFace.UVs(p_404839_.x(), 16.0F - p_405755_.y(), p_405755_.x(), 16.0F - p_404839_.y());
         case WEST -> new BlockElementFace.UVs(p_404839_.z(), 16.0F - p_405755_.y(), p_405755_.z(), 16.0F - p_404839_.y());
         case EAST -> new BlockElementFace.UVs(16.0F - p_405755_.z(), 16.0F - p_405755_.y(), 16.0F - p_404839_.z(), 16.0F - p_404839_.y());
      };
   }

   public static BakedQuad bakeQuad(
      ModelBaker.PartCache p_470769_,
      Vector3fc p_404957_,
      Vector3fc p_405340_,
      BlockElementFace p_111603_,
      TextureAtlasSprite p_111604_,
      Direction p_111605_,
      ModelState p_111606_,
      @Nullable BlockElementRotation p_111607_,
      boolean p_111608_,
      int p_364857_
   ) {
      BlockElementFace.UVs blockelementface$uvs = p_111603_.uvs();
      if (blockelementface$uvs == null) {
         blockelementface$uvs = defaultFaceUV(p_404957_, p_405340_, p_111605_);
      }

      Matrix4fc matrix4fc = p_111606_.inverseFaceTransformation(p_111605_);
      Vector3fc[] avector3fc = new Vector3fc[4];
      long[] along = new long[4];
      FaceInfo faceinfo = FaceInfo.fromFacing(p_111605_);

      for (int i = 0; i < 4; i++) {
         bakeVertex(
            i,
            faceinfo,
            blockelementface$uvs,
            p_111603_.rotation(),
            matrix4fc,
            p_404957_,
            p_405340_,
            p_111604_,
            p_111606_.transformation(),
            p_111607_,
            avector3fc,
            along,
            p_470769_
         );
      }

      Direction direction = calculateFacing(avector3fc);
      if (p_111607_ == null && direction != null && !p_111606_.mayApplyArbitraryRotation()) {
         recalculateWinding(avector3fc, along, direction);
      }

      return new BakedQuad(
         avector3fc[0],
         avector3fc[1],
         avector3fc[2],
         avector3fc[3],
         along[0],
         along[1],
         along[2],
         along[3],
         p_111603_.tintIndex(),
         Objects.requireNonNullElse(direction, Direction.UP),
         p_111604_,
         p_111608_,
         Math.max(p_364857_, p_111603_.faceData().lightEmission()),
         p_470769_.normals(BakedNormals.of(BakedNormals.computeQuadNormal(avector3fc[0], avector3fc[1], avector3fc[2], avector3fc[3]))),
         p_470769_.colors(BakedColors.of(p_111603_.faceData().color())),
         p_111603_.faceData().ambientOcclusion()
      );
   }

   private static void bakeVertex(
      int p_111622_,
      FaceInfo p_405531_,
      BlockElementFace.UVs p_405537_,
      Quadrant p_404831_,
      Matrix4fc p_405449_,
      Vector3fc p_470623_,
      Vector3fc p_470785_,
      TextureAtlasSprite p_111626_,
      Transformation p_111627_,
      @Nullable BlockElementRotation p_111628_,
      Vector3fc[] p_470830_,
      long[] p_470601_,
      ModelBaker.PartCache p_470615_
   ) {
      FaceInfo.VertexInfo faceinfo$vertexinfo = p_405531_.getVertexInfo(p_111622_);
      Vector3f vector3f = faceinfo$vertexinfo.select(p_470623_, p_470785_).div(16.0F);
      if (p_111628_ != null) {
         rotateVertexBy(vector3f, p_111628_.origin(), p_111628_.transform());
      }

      if (p_111627_ != Transformation.identity()) {
         rotateVertexBy(vector3f, BLOCK_MIDDLE, p_111627_.getMatrix());
      }

      float f = BlockElementFace.getU(p_405537_, p_404831_, p_111622_);
      float f1 = BlockElementFace.getV(p_405537_, p_404831_, p_111622_);
      float f2;
      float f3;
      if (MatrixUtil.isIdentity(p_405449_)) {
         f3 = f;
         f2 = f1;
      } else {
         Vector3f vector3f1 = p_405449_.transformPosition(new Vector3f(cornerToCenter(f), cornerToCenter(f1), 0.0F));
         f3 = centerToCorner(vector3f1.x);
         f2 = centerToCorner(vector3f1.y);
      }

      p_470830_[p_111622_] = p_470615_.vector(vector3f);
      p_470601_[p_111622_] = UVPair.pack(p_111626_.getU(f3), p_111626_.getV(f2));
   }

   private static float cornerToCenter(float p_405629_) {
      return p_405629_ - 0.5F;
   }

   private static float centerToCorner(float p_405087_) {
      return p_405087_ + 0.5F;
   }

   private static void rotateVertexBy(Vector3f p_404849_, Vector3fc p_404934_, Matrix4fc p_405201_) {
      p_404849_.sub(p_404934_);
      p_405201_.transformPosition(p_404849_);
      p_404849_.add(p_404934_);
   }

   @Nullable
   private static Direction calculateFacing(Vector3fc[] p_470800_) {
      Vector3f vector3f = new Vector3f();
      GeometryUtils.normal(p_470800_[0], p_470800_[1], p_470800_[2], vector3f);
      return findClosestDirection(vector3f);
   }

   @Nullable
   private static Direction findClosestDirection(Vector3f p_470633_) {
      if (!p_470633_.isFinite()) {
         return null;
      } else {
         Direction direction = null;
         float f = 0.0F;

         for (Direction direction1 : Direction.values()) {
            float f1 = p_470633_.dot(direction1.getUnitVec3f());
            if (f1 >= 0.0F && f1 > f) {
               f = f1;
               direction = direction1;
            }
         }

         return direction;
      }
   }

   private static void recalculateWinding(Vector3fc[] p_470571_, long[] p_470693_, Direction p_111632_) {
      float f = 999.0F;
      float f1 = 999.0F;
      float f2 = 999.0F;
      float f3 = -999.0F;
      float f4 = -999.0F;
      float f5 = -999.0F;

      for (int i = 0; i < 4; i++) {
         Vector3fc vector3fc = p_470571_[i];
         float f6 = vector3fc.x();
         float f7 = vector3fc.y();
         float f8 = vector3fc.z();
         if (f6 < f) {
            f = f6;
         }

         if (f7 < f1) {
            f1 = f7;
         }

         if (f8 < f2) {
            f2 = f8;
         }

         if (f6 > f3) {
            f3 = f6;
         }

         if (f7 > f4) {
            f4 = f7;
         }

         if (f8 > f5) {
            f5 = f8;
         }
      }

      FaceInfo faceinfo = FaceInfo.fromFacing(p_111632_);

      for (int k = 0; k < 4; k++) {
         FaceInfo.VertexInfo faceinfo$vertexinfo = faceinfo.getVertexInfo(k);
         float f10 = faceinfo$vertexinfo.xFace().select(f, f1, f2, f3, f4, f5);
         float f11 = faceinfo$vertexinfo.yFace().select(f, f1, f2, f3, f4, f5);
         float f9 = faceinfo$vertexinfo.zFace().select(f, f1, f2, f3, f4, f5);
         int j = findVertex(p_470571_, k, f10, f11, f9);
         if (j == -1) {
            throw new IllegalStateException("Can't find vertex to swap");
         }

         if (j != k) {
            swap(p_470571_, j, k);
            swap(p_470693_, j, k);
         }
      }
   }

   private static int findVertex(Vector3fc[] p_470622_, int p_470703_, float p_470730_, float p_470540_, float p_470560_) {
      for (int i = p_470703_; i < 4; i++) {
         Vector3fc vector3fc = p_470622_[i];
         if (p_470730_ == vector3fc.x() && p_470540_ == vector3fc.y() && p_470560_ == vector3fc.z()) {
            return i;
         }
      }

      return -1;
   }

   private static void swap(Vector3fc[] p_470851_, int p_470708_, int p_470640_) {
      Vector3fc vector3fc = p_470851_[p_470708_];
      p_470851_[p_470708_] = p_470851_[p_470640_];
      p_470851_[p_470640_] = vector3fc;
   }

   private static void swap(long[] p_470532_, int p_470641_, int p_470793_) {
      long i = p_470532_[p_470641_];
      p_470532_[p_470641_] = p_470532_[p_470793_];
      p_470532_[p_470793_] = i;
   }
}
