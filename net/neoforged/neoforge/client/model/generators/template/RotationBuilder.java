package net.neoforged.neoforge.client.model.generators.template;

import com.google.common.base.Preconditions;
import net.minecraft.client.renderer.block.model.BlockElementRotation;
import net.minecraft.core.Direction;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

public final class RotationBuilder {
   @Nullable
   private Vector3f origin;
   @Nullable
   private BlockElementRotation.RotationValue value;
   private boolean rescale;

   public RotationBuilder origin(float x, float y, float z) {
      this.origin = new Vector3f(x, y, z);
      return this;
   }

   public RotationBuilder singleAxis(Direction.Axis axis, float angle) {
      Preconditions.checkNotNull(axis, "Axis must not be null");
      this.value = new BlockElementRotation.SingleAxisRotation(axis, angle);
      return this;
   }

   public RotationBuilder eulerXYZ(float angleX, float angleY, float angleZ) {
      this.value = new BlockElementRotation.EulerXYZRotation(angleX, angleY, angleZ);
      return this;
   }

   public RotationBuilder rescale(boolean rescale) {
      this.rescale = rescale;
      return this;
   }

   BlockElementRotation build() {
      Preconditions.checkNotNull(this.origin, "No origin specified");
      Preconditions.checkNotNull(this.value, "No value specified");
      return new BlockElementRotation(this.origin, this.value, this.rescale);
   }

   RotationBuilder copy() {
      RotationBuilder builder = new RotationBuilder();
      builder.origin = this.origin != null ? new Vector3f(this.origin) : null;
      builder.value = this.value;
      builder.rescale = this.rescale;
      return builder;
   }
}
