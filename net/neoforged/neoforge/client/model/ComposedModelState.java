package net.neoforged.neoforge.client.model;

import com.mojang.math.Transformation;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.core.Direction;
import org.joml.Matrix4fc;

public final class ComposedModelState implements ModelState {
   private final ModelState parent;
   private final Transformation transformation;

   public ComposedModelState(ModelState parent, Transformation transformation) {
      this.parent = parent;
      this.transformation = parent.transformation().compose(transformation);
   }

   @Override
   public Transformation transformation() {
      return this.transformation;
   }

   @Override
   public Matrix4fc faceTransformation(Direction side) {
      return this.parent.faceTransformation(side);
   }

   @Override
   public Matrix4fc inverseFaceTransformation(Direction side) {
      return this.parent.inverseFaceTransformation(side);
   }
}
