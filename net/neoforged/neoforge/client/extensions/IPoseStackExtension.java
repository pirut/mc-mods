package net.neoforged.neoforge.client.extensions;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Transformation;
import org.joml.Vector3fc;

public interface IPoseStackExtension {
   private PoseStack self() {
      return (PoseStack)this;
   }

   default void pushTransformation(Transformation transformation) {
      PoseStack self = this.self();
      self.pushPose();
      Vector3fc trans = transformation.getTranslation();
      self.translate(trans.x(), trans.y(), trans.z());
      self.mulPose(transformation.getLeftRotation());
      Vector3fc scale = transformation.getScale();
      self.scale(scale.x(), scale.y(), scale.z());
      self.mulPose(transformation.getRightRotation());
   }
}
