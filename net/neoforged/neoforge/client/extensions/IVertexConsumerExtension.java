package net.neoforged.neoforge.client.extensions;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import net.neoforged.neoforge.client.model.quad.BakedNormals;
import org.joml.Matrix3f;
import org.joml.Vector3f;

public interface IVertexConsumerExtension {
   private VertexConsumer self() {
      return (VertexConsumer)this;
   }

   default VertexConsumer misc(VertexFormatElement element, int... rawData) {
      return this.self();
   }

   default void applyBakedNormals(Vector3f generated, BakedNormals data, int vertex, Matrix3f normalTransform) {
      int packed = data.normal(vertex);
      if (!BakedNormals.isUnspecified(packed)) {
         BakedNormals.unpack(packed, generated);
         generated.mul(normalTransform);
      }
   }
}
