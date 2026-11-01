package net.minecraft.client.resources.model;

import com.mojang.math.Transformation;
import net.minecraft.core.Direction;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.extensions.ModelStateExtension;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

@OnlyIn(Dist.CLIENT)
public interface ModelState extends ModelStateExtension {
   Matrix4fc NO_TRANSFORM = new Matrix4f();

   default Transformation transformation() {
      return Transformation.identity();
   }

   default Matrix4fc faceTransformation(Direction p_405422_) {
      return NO_TRANSFORM;
   }

   default Matrix4fc inverseFaceTransformation(Direction p_405142_) {
      return NO_TRANSFORM;
   }
}
