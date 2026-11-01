package net.neoforged.neoforge.client.model;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import java.util.Arrays;
import java.util.List;
import net.minecraft.client.renderer.block.model.BakedQuad;

public interface IQuadTransformer {
   int STRIDE = DefaultVertexFormat.BLOCK.getVertexSize() / 4;
   int POSITION = findOffset(VertexFormatElement.POSITION);
   int COLOR = findOffset(VertexFormatElement.COLOR);
   int UV0 = findOffset(VertexFormatElement.UV0);
   int UV1 = findOffset(VertexFormatElement.UV1);
   int UV2 = findOffset(VertexFormatElement.UV2);
   int NORMAL = findOffset(VertexFormatElement.NORMAL);

   void processInPlace(BakedQuad var1);

   default void processInPlace(List<BakedQuad> quads) {
      for (BakedQuad quad : quads) {
         this.processInPlace(quad);
      }
   }

   default BakedQuad process(BakedQuad quad) {
      BakedQuad copy = copy(quad);
      this.processInPlace(copy);
      return copy;
   }

   default List<BakedQuad> process(List<BakedQuad> inputs) {
      return inputs.stream().map(IQuadTransformer::copy).peek(this::processInPlace).toList();
   }

   default IQuadTransformer andThen(IQuadTransformer other) {
      return quad -> {
         this.processInPlace(quad);
         other.processInPlace(quad);
      };
   }

   private static BakedQuad copy(BakedQuad quad) {
      int[] vertices = quad.getVertices();
      return new BakedQuad(
         Arrays.copyOf(vertices, vertices.length), quad.getTintIndex(), quad.getDirection(), quad.getSprite(), quad.isShade(), quad.hasAmbientOcclusion()
      );
   }

   private static int findOffset(VertexFormatElement element) {
      return DefaultVertexFormat.BLOCK.contains(element) ? DefaultVertexFormat.BLOCK.getOffset(element) / 4 : -1;
   }
}
