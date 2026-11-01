package net.neoforged.neoforge.client.model.pipeline;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import org.joml.Vector3f;

public class RemappingVertexPipeline implements VertexConsumer {
   private static final Set<VertexFormatElement> KNOWN_ELEMENTS = Set.of(
      VertexFormatElement.POSITION,
      VertexFormatElement.COLOR,
      VertexFormatElement.UV,
      VertexFormatElement.UV1,
      VertexFormatElement.UV2,
      VertexFormatElement.NORMAL
   );
   private static final int[] EMPTY_INT_ARRAY = new int[0];
   private final VertexConsumer parent;
   private final VertexFormat targetFormat;
   private final Vector3f position = new Vector3f();
   private final Vector3f normal = new Vector3f();
   private final int[] color = new int[]{255, 255, 255, 255};
   private final float[] uv0 = new float[]{0.0F, 0.0F};
   private final int[] uv1 = new int[]{0, 10};
   private final int[] uv2 = new int[]{0, 0};
   private final Map<VertexFormatElement, Integer> miscElementIds;
   private final int[][] misc;

   public RemappingVertexPipeline(VertexConsumer parent, VertexFormat targetFormat) {
      this.parent = parent;
      this.targetFormat = targetFormat;
      this.miscElementIds = new IdentityHashMap<>();
      int i = 0;

      for (VertexFormatElement element : targetFormat.getElements()) {
         if (!KNOWN_ELEMENTS.contains(element)) {
            this.miscElementIds.put(element, i++);
         }
      }

      this.misc = new int[i][];
      Arrays.fill(this.misc, EMPTY_INT_ARRAY);
   }

   @Override
   public VertexConsumer addVertex(float x, float y, float z) {
      this.position.set(x, y, z);
      return this;
   }

   @Override
   public VertexConsumer setNormal(float x, float y, float z) {
      this.normal.set(x, y, z);
      return this;
   }

   @Override
   public VertexConsumer setColor(int r, int g, int b, int a) {
      this.color[0] = r;
      this.color[1] = g;
      this.color[2] = b;
      this.color[3] = a;
      return this;
   }

   @Override
   public VertexConsumer setUv(float u, float v) {
      this.uv0[0] = u;
      this.uv0[1] = v;
      return this;
   }

   @Override
   public VertexConsumer setUv1(int u, int v) {
      this.uv1[0] = u;
      this.uv1[1] = v;
      return this;
   }

   @Override
   public VertexConsumer setUv2(int u, int v) {
      this.uv2[0] = u;
      this.uv2[1] = v;
      return this;
   }

   @Override
   public VertexConsumer misc(VertexFormatElement element, int... values) {
      Integer id = this.miscElementIds.get(element);
      if (id != null) {
         this.misc[id] = Arrays.copyOf(values, values.length);
      }

      return this;
   }

   public void endVertex() {
      for (VertexFormatElement element : this.targetFormat.getElements()) {
         if (element.equals(VertexFormatElement.POSITION)) {
            this.parent.addVertex(this.position.x, this.position.y, this.position.z);
         } else if (element.equals(VertexFormatElement.NORMAL)) {
            this.parent.setNormal(this.normal.x(), this.normal.y(), this.normal.z());
         } else if (element.equals(VertexFormatElement.COLOR)) {
            this.parent.setColor(this.color[0], this.color[1], this.color[2], this.color[3]);
         } else if (element.equals(VertexFormatElement.UV0)) {
            this.parent.setUv(this.uv0[0], this.uv0[1]);
         } else if (element.equals(VertexFormatElement.UV1)) {
            this.parent.setUv1(this.uv1[0], this.uv1[1]);
         } else if (element.equals(VertexFormatElement.UV2)) {
            this.parent.setUv2(this.uv2[0], this.uv2[1]);
         } else {
            this.parent.misc(element, this.misc[this.miscElementIds.get(element)]);
         }
      }
   }
}
