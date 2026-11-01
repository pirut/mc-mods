package net.neoforged.neoforge.client.model.generators.template;

import com.google.common.base.Preconditions;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.math.Transformation;
import net.neoforged.neoforge.common.util.TransformationHelper;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

public class RootTransformsBuilder {
   private static final Vector3f ONE = new Vector3f(1.0F, 1.0F, 1.0F);
   private Vector3fc translation = new Vector3f();
   private Quaternionfc leftRotation = new Quaternionf();
   private Quaternionfc rightRotation = new Quaternionf();
   private Vector3fc scale = ONE;
   @Nullable
   private TransformationHelper.TransformOrigin origin;
   @Nullable
   private Vector3fc originVec;

   public RootTransformsBuilder translation(Vector3f translation) {
      this.translation = (Vector3fc)Preconditions.checkNotNull(translation, "Translation must not be null");
      return this;
   }

   public RootTransformsBuilder translation(float x, float y, float z) {
      return this.translation(new Vector3f(x, y, z));
   }

   public RootTransformsBuilder rotation(Quaternionf rotation) {
      this.leftRotation = (Quaternionfc)Preconditions.checkNotNull(rotation, "Rotation must not be null");
      return this;
   }

   public RootTransformsBuilder rotation(float x, float y, float z, boolean isDegrees) {
      return this.rotation(TransformationHelper.quatFromXYZ(x, y, z, isDegrees));
   }

   public RootTransformsBuilder leftRotation(Quaternionf leftRotation) {
      return this.rotation(leftRotation);
   }

   public RootTransformsBuilder leftRotation(float x, float y, float z, boolean isDegrees) {
      return this.leftRotation(TransformationHelper.quatFromXYZ(x, y, z, isDegrees));
   }

   public RootTransformsBuilder rightRotation(Quaternionf rightRotation) {
      this.rightRotation = (Quaternionfc)Preconditions.checkNotNull(rightRotation, "Rotation must not be null");
      return this;
   }

   public RootTransformsBuilder rightRotation(float x, float y, float z, boolean isDegrees) {
      return this.rightRotation(TransformationHelper.quatFromXYZ(x, y, z, isDegrees));
   }

   public RootTransformsBuilder postRotation(Quaternionf postRotation) {
      return this.rightRotation(postRotation);
   }

   public RootTransformsBuilder postRotation(float x, float y, float z, boolean isDegrees) {
      return this.postRotation(TransformationHelper.quatFromXYZ(x, y, z, isDegrees));
   }

   public RootTransformsBuilder scale(float scale) {
      return this.scale(new Vector3f(scale, scale, scale));
   }

   public RootTransformsBuilder scale(float xScale, float yScale, float zScale) {
      return this.scale(new Vector3f(xScale, yScale, zScale));
   }

   public RootTransformsBuilder scale(Vector3f scale) {
      this.scale = (Vector3fc)Preconditions.checkNotNull(scale, "Scale must not be null");
      return this;
   }

   public RootTransformsBuilder transform(Transformation transformation) {
      Preconditions.checkNotNull(transformation, "Transformation must not be null");
      this.translation = transformation.getTranslation();
      this.leftRotation = transformation.getLeftRotation();
      this.rightRotation = transformation.getRightRotation();
      this.scale = transformation.getScale();
      return this;
   }

   public RootTransformsBuilder origin(Vector3f origin) {
      this.originVec = (Vector3fc)Preconditions.checkNotNull(origin, "Origin must not be null");
      this.origin = null;
      return this;
   }

   public RootTransformsBuilder origin(TransformationHelper.TransformOrigin origin) {
      this.origin = (TransformationHelper.TransformOrigin)Preconditions.checkNotNull(origin, "Origin must not be null");
      this.originVec = null;
      return this;
   }

   JsonObject toJson() {
      JsonObject transform = new JsonObject();
      if (!this.translation.equals(0.0F, 0.0F, 0.0F)) {
         transform.add("translation", writeVec3(this.translation));
      }

      if (!this.scale.equals(ONE)) {
         transform.add("scale", writeVec3(this.scale));
      }

      if (!this.leftRotation.equals(0.0F, 0.0F, 0.0F, 1.0F)) {
         transform.add("rotation", writeQuaternion(this.leftRotation));
      }

      if (!this.rightRotation.equals(0.0F, 0.0F, 0.0F, 1.0F)) {
         transform.add("post_rotation", writeQuaternion(this.rightRotation));
      }

      if (this.origin != null) {
         transform.addProperty("origin", this.origin.getSerializedName());
      } else if (this.originVec != null && !this.originVec.equals(0.0F, 0.0F, 0.0F)) {
         transform.add("origin", writeVec3(this.originVec));
      }

      return transform;
   }

   private static JsonArray writeVec3(Vector3fc vector) {
      JsonArray array = new JsonArray();
      array.add(vector.x());
      array.add(vector.y());
      array.add(vector.z());
      return array;
   }

   private static JsonArray writeQuaternion(Quaternionfc quaternion) {
      JsonArray array = new JsonArray();
      array.add(quaternion.x());
      array.add(quaternion.y());
      array.add(quaternion.z());
      array.add(quaternion.w());
      return array;
   }

   void copyFrom(RootTransformsBuilder other) {
      this.translation = other.translation;
      this.leftRotation = other.leftRotation;
      this.rightRotation = other.rightRotation;
      this.scale = other.scale;
      this.origin = other.origin;
      this.originVec = other.originVec != null ? new Vector3f(other.originVec) : null;
   }
}
