package com.mojang.math;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Objects;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Util;
import net.neoforged.neoforge.common.extensions.ITransformationExtension;
import org.apache.commons.lang3.tuple.Triple;
import org.joml.Matrix3f;
import org.joml.Matrix3fc;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

public final class Transformation implements ITransformationExtension {
   private final Matrix4fc matrix;
   public static final Codec<Transformation> CODEC = RecordCodecBuilder.create(
      p_269604_ -> p_269604_.group(
            ExtraCodecs.VECTOR3F.fieldOf("translation").forGetter(p_454092_ -> p_454092_.translation),
            ExtraCodecs.QUATERNIONF.fieldOf("left_rotation").forGetter(p_454091_ -> p_454091_.leftRotation),
            ExtraCodecs.VECTOR3F.fieldOf("scale").forGetter(p_454090_ -> p_454090_.scale),
            ExtraCodecs.QUATERNIONF.fieldOf("right_rotation").forGetter(p_454089_ -> p_454089_.rightRotation)
         )
         .apply(p_269604_, Transformation::new)
   );
   public static final Codec<Transformation> EXTENDED_CODEC = Codec.withAlternative(
      CODEC, ExtraCodecs.MATRIX4F.xmap(Transformation::new, Transformation::getMatrix)
   );
   private boolean decomposed;
   @Nullable
   private Vector3fc translation;
   @Nullable
   private Quaternionfc leftRotation;
   @Nullable
   private Vector3fc scale;
   @Nullable
   private Quaternionfc rightRotation;
   private static final Transformation IDENTITY = Util.make(() -> {
      Transformation transformation = new Transformation(new Matrix4f());
      transformation.translation = new Vector3f();
      transformation.leftRotation = new Quaternionf();
      transformation.scale = new Vector3f(1.0F, 1.0F, 1.0F);
      transformation.rightRotation = new Quaternionf();
      transformation.decomposed = true;
      return transformation;
   });
   private Matrix3f normalTransform = null;

   public Transformation(@Nullable Matrix4fc p_404715_) {
      if (p_404715_ == null) {
         this.matrix = new Matrix4f();
      } else {
         this.matrix = p_404715_;
      }
   }

   public Transformation(@Nullable Vector3fc p_454983_, @Nullable Quaternionfc p_454920_, @Nullable Vector3fc p_455329_, @Nullable Quaternionfc p_456097_) {
      this.matrix = compose(p_454983_, p_454920_, p_455329_, p_456097_);
      this.translation = (Vector3fc)(p_454983_ != null ? p_454983_ : new Vector3f());
      this.leftRotation = (Quaternionfc)(p_454920_ != null ? p_454920_ : new Quaternionf());
      this.scale = (Vector3fc)(p_455329_ != null ? p_455329_ : new Vector3f(1.0F, 1.0F, 1.0F));
      this.rightRotation = (Quaternionfc)(p_456097_ != null ? p_456097_ : new Quaternionf());
      this.decomposed = true;
   }

   public static Transformation identity() {
      return IDENTITY;
   }

   public Transformation compose(Transformation p_121097_) {
      Matrix4f matrix4f = this.getMatrixCopy();
      matrix4f.mul(p_121097_.getMatrix());
      return new Transformation(matrix4f);
   }

   @Nullable
   public Transformation inverse() {
      if (this == IDENTITY) {
         return this;
      } else {
         Matrix4f matrix4f = this.getMatrixCopy().invertAffine();
         return matrix4f.isFinite() ? new Transformation(matrix4f) : null;
      }
   }

   private void ensureDecomposed() {
      if (!this.decomposed) {
         float f = 1.0F / this.matrix.m33();
         Triple<Quaternionf, Vector3f, Quaternionf> triple = MatrixUtil.svdDecompose(new Matrix3f(this.matrix).scale(f));
         this.translation = this.matrix.getTranslation(new Vector3f()).mul(f);
         this.leftRotation = new Quaternionf((Quaternionfc)triple.getLeft());
         this.scale = new Vector3f((Vector3fc)triple.getMiddle());
         this.rightRotation = new Quaternionf((Quaternionfc)triple.getRight());
         this.decomposed = true;
      }
   }

   private static Matrix4f compose(
      @Nullable Vector3fc p_454749_, @Nullable Quaternionfc p_456084_, @Nullable Vector3fc p_455162_, @Nullable Quaternionfc p_455294_
   ) {
      Matrix4f matrix4f = new Matrix4f();
      if (p_454749_ != null) {
         matrix4f.translation(p_454749_);
      }

      if (p_456084_ != null) {
         matrix4f.rotate(p_456084_);
      }

      if (p_455162_ != null) {
         matrix4f.scale(p_455162_);
      }

      if (p_455294_ != null) {
         matrix4f.rotate(p_455294_);
      }

      return matrix4f;
   }

   public Matrix4fc getMatrix() {
      return this.matrix;
   }

   public Matrix4f getMatrixCopy() {
      return new Matrix4f(this.matrix);
   }

   public Vector3fc getTranslation() {
      this.ensureDecomposed();
      return this.translation;
   }

   public Quaternionfc getLeftRotation() {
      this.ensureDecomposed();
      return this.leftRotation;
   }

   public Vector3fc getScale() {
      this.ensureDecomposed();
      return this.scale;
   }

   public Quaternionfc getRightRotation() {
      this.ensureDecomposed();
      return this.rightRotation;
   }

   @Override
   public boolean equals(Object p_121108_) {
      if (this == p_121108_) {
         return true;
      } else if (p_121108_ != null && this.getClass() == p_121108_.getClass()) {
         Transformation transformation = (Transformation)p_121108_;
         return Objects.equals(this.matrix, transformation.matrix);
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      return Objects.hash(this.matrix);
   }

   public Matrix3fc getNormalMatrix() {
      this.checkNormalTransform();
      return this.normalTransform;
   }

   private void checkNormalTransform() {
      if (this.normalTransform == null) {
         this.normalTransform = new Matrix3f(this.matrix);
         this.normalTransform.invert();
         this.normalTransform.transpose();
      }
   }

   public Transformation slerp(Transformation p_175938_, float p_175939_) {
      return new Transformation(
         this.getTranslation().lerp(p_175938_.getTranslation(), p_175939_, new Vector3f()),
         this.getLeftRotation().slerp(p_175938_.getLeftRotation(), p_175939_, new Quaternionf()),
         this.getScale().lerp(p_175938_.getScale(), p_175939_, new Vector3f()),
         this.getRightRotation().slerp(p_175938_.getRightRotation(), p_175939_, new Quaternionf())
      );
   }
}
