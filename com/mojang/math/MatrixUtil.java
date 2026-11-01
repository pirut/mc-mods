package com.mojang.math;

import org.apache.commons.lang3.tuple.Triple;
import org.joml.Math;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class MatrixUtil {
   private static final float G = 3.0F + 2.0F * Math.sqrt(2.0F);
   private static final GivensParameters PI_4 = GivensParameters.fromPositiveAngle((float) (java.lang.Math.PI / 4));

   private MatrixUtil() {
   }

   public static Matrix4f mulComponentWise(Matrix4f p_254173_, float p_253864_) {
      return p_254173_.set(
         p_254173_.m00() * p_253864_,
         p_254173_.m01() * p_253864_,
         p_254173_.m02() * p_253864_,
         p_254173_.m03() * p_253864_,
         p_254173_.m10() * p_253864_,
         p_254173_.m11() * p_253864_,
         p_254173_.m12() * p_253864_,
         p_254173_.m13() * p_253864_,
         p_254173_.m20() * p_253864_,
         p_254173_.m21() * p_253864_,
         p_254173_.m22() * p_253864_,
         p_254173_.m23() * p_253864_,
         p_254173_.m30() * p_253864_,
         p_254173_.m31() * p_253864_,
         p_254173_.m32() * p_253864_,
         p_254173_.m33() * p_253864_
      );
   }

   private static GivensParameters approxGivensQuat(float p_276275_, float p_276276_, float p_276282_) {
      float $$3 = 2.0F * (p_276275_ - p_276282_);
      return G * p_276276_ * p_276276_ < $$3 * $$3 ? GivensParameters.fromUnnormalized(p_276276_, $$3) : PI_4;
   }

   private static GivensParameters qrGivensQuat(float p_253897_, float p_254413_) {
      float $$2 = (float)java.lang.Math.hypot(p_253897_, p_254413_);
      float $$3 = $$2 > 1.0E-6F ? p_254413_ : 0.0F;
      float $$4 = Math.abs(p_253897_) + Math.max($$2, 1.0E-6F);
      if (p_253897_ < 0.0F) {
         float $$5 = $$3;
         $$3 = $$4;
         $$4 = $$5;
      }

      return GivensParameters.fromUnnormalized($$3, $$4);
   }

   private static void similarityTransform(Matrix3f p_276319_, Matrix3f p_276263_) {
      p_276319_.mul(p_276263_);
      p_276263_.transpose();
      p_276263_.mul(p_276319_);
      p_276319_.set(p_276263_);
   }

   private static void stepJacobi(Matrix3f p_276262_, Matrix3f p_276279_, Quaternionf p_276314_, Quaternionf p_276299_) {
      if (p_276262_.m01 * p_276262_.m01 + p_276262_.m10 * p_276262_.m10 > 1.0E-6F) {
         GivensParameters $$4 = approxGivensQuat(p_276262_.m00, 0.5F * (p_276262_.m01 + p_276262_.m10), p_276262_.m11);
         Quaternionf $$5 = $$4.aroundZ(p_276314_);
         p_276299_.mul($$5);
         $$4.aroundZ(p_276279_);
         similarityTransform(p_276262_, p_276279_);
      }

      if (p_276262_.m02 * p_276262_.m02 + p_276262_.m20 * p_276262_.m20 > 1.0E-6F) {
         GivensParameters $$6 = approxGivensQuat(p_276262_.m00, 0.5F * (p_276262_.m02 + p_276262_.m20), p_276262_.m22).inverse();
         Quaternionf $$7 = $$6.aroundY(p_276314_);
         p_276299_.mul($$7);
         $$6.aroundY(p_276279_);
         similarityTransform(p_276262_, p_276279_);
      }

      if (p_276262_.m12 * p_276262_.m12 + p_276262_.m21 * p_276262_.m21 > 1.0E-6F) {
         GivensParameters $$8 = approxGivensQuat(p_276262_.m11, 0.5F * (p_276262_.m12 + p_276262_.m21), p_276262_.m22);
         Quaternionf $$9 = $$8.aroundX(p_276314_);
         p_276299_.mul($$9);
         $$8.aroundX(p_276279_);
         similarityTransform(p_276262_, p_276279_);
      }
   }

   public static Quaternionf eigenvalueJacobi(Matrix3f p_276278_, int p_276269_) {
      Quaternionf $$2 = new Quaternionf();
      Matrix3f $$3 = new Matrix3f();
      Quaternionf $$4 = new Quaternionf();

      for (int $$5 = 0; $$5 < p_276269_; $$5++) {
         stepJacobi(p_276278_, $$3, $$4, $$2);
      }

      $$2.normalize();
      return $$2;
   }

   public static Triple<Quaternionf, Vector3f, Quaternionf> svdDecompose(Matrix3f p_253947_) {
      Matrix3f $$1 = new Matrix3f(p_253947_);
      $$1.transpose();
      $$1.mul(p_253947_);
      Quaternionf $$2 = eigenvalueJacobi($$1, 5);
      float $$3 = $$1.m00;
      float $$4 = $$1.m11;
      boolean $$5 = $$3 < 1.0E-6;
      boolean $$6 = $$4 < 1.0E-6;
      Matrix3f $$8 = p_253947_.rotate($$2);
      Quaternionf $$9 = new Quaternionf();
      Quaternionf $$10 = new Quaternionf();
      GivensParameters $$11;
      if ($$5) {
         $$11 = qrGivensQuat($$8.m11, -$$8.m10);
      } else {
         $$11 = qrGivensQuat($$8.m00, $$8.m01);
      }

      Quaternionf $$13 = $$11.aroundZ($$10);
      Matrix3f $$14 = $$11.aroundZ($$1);
      $$9.mul($$13);
      $$14.transpose().mul($$8);
      if ($$5) {
         $$11 = qrGivensQuat($$14.m22, -$$14.m20);
      } else {
         $$11 = qrGivensQuat($$14.m00, $$14.m02);
      }

      $$11 = $$11.inverse();
      Quaternionf $$15 = $$11.aroundY($$10);
      Matrix3f $$16 = $$11.aroundY($$8);
      $$9.mul($$15);
      $$16.transpose().mul($$14);
      if ($$6) {
         $$11 = qrGivensQuat($$16.m22, -$$16.m21);
      } else {
         $$11 = qrGivensQuat($$16.m11, $$16.m12);
      }

      Quaternionf $$17 = $$11.aroundX($$10);
      Matrix3f $$18 = $$11.aroundX($$14);
      $$9.mul($$17);
      $$18.transpose().mul($$16);
      Vector3f $$19 = new Vector3f($$18.m00, $$18.m11, $$18.m22);
      return Triple.of($$9, $$19, $$2.conjugate());
   }

   public static boolean isPureTranslation(Matrix4f p_324331_) {
      return (p_324331_.properties() & 8) != 0;
   }

   public static boolean isOrthonormal(Matrix4f p_323931_) {
      return (p_323931_.properties() & 16) != 0;
   }
}
