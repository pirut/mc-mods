package net.neoforged.neoforge.client.model.pipeline;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.Util;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.client.model.IQuadTransformer;
import net.neoforged.neoforge.client.textures.UnitTextureAtlasSprite;

public class QuadBakingVertexConsumer implements VertexConsumer {
   private final Map<VertexFormatElement, Integer> ELEMENT_OFFSETS = Util.make(new IdentityHashMap<>(), map -> {
      for (VertexFormatElement element : DefaultVertexFormat.BLOCK.getElements()) {
         map.put(element, DefaultVertexFormat.BLOCK.getOffset(element) / 4);
      }
   });
   private static final int QUAD_DATA_SIZE = IQuadTransformer.STRIDE * 4;
   private final int[] quadData = new int[QUAD_DATA_SIZE];
   private int vertexIndex = 0;
   private boolean building = false;
   private int tintIndex = -1;
   private Direction direction = Direction.DOWN;
   private TextureAtlasSprite sprite = UnitTextureAtlasSprite.INSTANCE;
   private boolean shade;
   private boolean hasAmbientOcclusion;

   @Override
   public VertexConsumer addVertex(float x, float y, float z) {
      if (this.building && ++this.vertexIndex > 4) {
         throw new IllegalStateException("Expected quad export after fourth vertex");
      } else {
         this.building = true;
         int offset = this.vertexIndex * IQuadTransformer.STRIDE + IQuadTransformer.POSITION;
         this.quadData[offset] = Float.floatToRawIntBits(x);
         this.quadData[offset + 1] = Float.floatToRawIntBits(y);
         this.quadData[offset + 2] = Float.floatToRawIntBits(z);
         return this;
      }
   }

   @Override
   public VertexConsumer setNormal(float x, float y, float z) {
      int offset = this.vertexIndex * IQuadTransformer.STRIDE + IQuadTransformer.NORMAL;
      this.quadData[offset] = (int)(x * 127.0F) & 0xFF | ((int)(y * 127.0F) & 0xFF) << 8 | ((int)(z * 127.0F) & 0xFF) << 16;
      return this;
   }

   @Override
   public VertexConsumer setColor(int r, int g, int b, int a) {
      int offset = this.vertexIndex * IQuadTransformer.STRIDE + IQuadTransformer.COLOR;
      this.quadData[offset] = (a & 0xFF) << 24 | (b & 0xFF) << 16 | (g & 0xFF) << 8 | r & 0xFF;
      return this;
   }

   @Override
   public VertexConsumer setUv(float u, float v) {
      int offset = this.vertexIndex * IQuadTransformer.STRIDE + IQuadTransformer.UV0;
      this.quadData[offset] = Float.floatToRawIntBits(u);
      this.quadData[offset + 1] = Float.floatToRawIntBits(v);
      return this;
   }

   @Override
   public VertexConsumer setUv1(int u, int v) {
      if (IQuadTransformer.UV1 >= 0) {
         int offset = this.vertexIndex * IQuadTransformer.STRIDE + IQuadTransformer.UV1;
         this.quadData[offset] = u & 65535 | (v & 65535) << 16;
      }

      return this;
   }

   @Override
   public VertexConsumer setUv2(int u, int v) {
      int offset = this.vertexIndex * IQuadTransformer.STRIDE + IQuadTransformer.UV2;
      this.quadData[offset] = u & 65535 | (v & 65535) << 16;
      return this;
   }

   @Override
   public VertexConsumer misc(VertexFormatElement element, int... rawData) {
      Integer baseOffset = this.ELEMENT_OFFSETS.get(element);
      if (baseOffset != null) {
         int offset = this.vertexIndex * IQuadTransformer.STRIDE + baseOffset;
         System.arraycopy(rawData, 0, this.quadData, offset, rawData.length);
      }

      return this;
   }

   public void setTintIndex(int tintIndex) {
      this.tintIndex = tintIndex;
   }

   public void setDirection(Direction direction) {
      this.direction = direction;
   }

   public void setSprite(TextureAtlasSprite sprite) {
      this.sprite = sprite;
   }

   public void setShade(boolean shade) {
      this.shade = shade;
   }

   public void setHasAmbientOcclusion(boolean hasAmbientOcclusion) {
      this.hasAmbientOcclusion = hasAmbientOcclusion;
   }

   public BakedQuad bakeQuad() {
      if (this.building && ++this.vertexIndex == 4) {
         BakedQuad quad = new BakedQuad((int[])this.quadData.clone(), this.tintIndex, this.direction, this.sprite, this.shade, this.hasAmbientOcclusion);
         this.vertexIndex = 0;
         this.building = false;
         Arrays.fill(this.quadData, 0);
         return quad;
      } else {
         throw new IllegalStateException("Not enough vertices available. Vertices in buffer: " + this.vertexIndex);
      }
   }
}
