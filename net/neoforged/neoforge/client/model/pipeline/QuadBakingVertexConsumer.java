package net.neoforged.neoforge.client.model.pipeline;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import java.util.Arrays;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.util.ARGB;
import net.neoforged.neoforge.client.model.quad.BakedColors;
import net.neoforged.neoforge.client.model.quad.BakedNormals;
import net.neoforged.neoforge.client.textures.UnitTextureAtlasSprite;
import org.joml.Vector3f;

public class QuadBakingVertexConsumer implements VertexConsumer {
   private final Vector3f[] positions = new Vector3f[4];
   private final long[] uvs = new long[4];
   private final int[] normals = new int[4];
   private final int[] colors = new int[4];
   private int vertexIndex = 0;
   private boolean building = false;
   private int tintIndex = -1;
   private Direction direction = Direction.DOWN;
   private TextureAtlasSprite sprite = UnitTextureAtlasSprite.INSTANCE;
   private boolean shade;
   private int lightEmission;
   private boolean hasAmbientOcclusion;

   public QuadBakingVertexConsumer() {
      this.clear();
   }

   @Override
   public VertexConsumer addVertex(float x, float y, float z) {
      if (this.building && ++this.vertexIndex > 4) {
         throw new IllegalStateException("Expected quad export after fourth vertex");
      } else {
         this.building = true;
         this.positions[this.vertexIndex].set(x, y, z);
         return this;
      }
   }

   @Override
   public VertexConsumer setNormal(float x, float y, float z) {
      this.normals[this.vertexIndex] = BakedNormals.pack(x, y, z);
      return this;
   }

   @Override
   public VertexConsumer setColor(int packedColor) {
      this.colors[this.vertexIndex] = packedColor;
      return this;
   }

   @Override
   public VertexConsumer setColor(int r, int g, int b, int a) {
      return this.setColor(ARGB.color(a, r, g, b));
   }

   @Override
   public VertexConsumer setUv(float u, float v) {
      this.uvs[this.vertexIndex] = UVPair.pack(u, v);
      return this;
   }

   @Override
   public VertexConsumer setUv1(int u, int v) {
      return this;
   }

   @Override
   public VertexConsumer setUv2(int u, int v) {
      return this;
   }

   @Override
   public VertexConsumer misc(VertexFormatElement element, int... rawData) {
      return this;
   }

   @Override
   public VertexConsumer setLineWidth(float lineWidth) {
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

   public void setLightEmission(int lightEmission) {
      this.lightEmission = lightEmission;
   }

   public void setHasAmbientOcclusion(boolean hasAmbientOcclusion) {
      this.hasAmbientOcclusion = hasAmbientOcclusion;
   }

   public BakedQuad bakeQuad() {
      if (this.building && ++this.vertexIndex == 4) {
         BakedQuad quad = new BakedQuad(
            this.positions[0],
            this.positions[1],
            this.positions[2],
            this.positions[3],
            this.uvs[0],
            this.uvs[1],
            this.uvs[2],
            this.uvs[3],
            this.tintIndex,
            this.direction,
            this.sprite,
            this.shade,
            this.lightEmission,
            BakedNormals.of(this.normals[0], this.normals[1], this.normals[2], this.normals[3]),
            BakedColors.of(this.colors[0], this.colors[1], this.colors[2], this.colors[3]),
            this.hasAmbientOcclusion
         );
         this.clear();
         return quad;
      } else {
         throw new IllegalStateException("Not enough vertices available. Vertices in buffer: " + this.vertexIndex);
      }
   }

   private void clear() {
      this.vertexIndex = 0;
      this.building = false;
      Arrays.setAll(this.positions, $ -> new Vector3f());
      Arrays.fill(this.uvs, 0L);
      Arrays.fill(this.normals, 0);
      Arrays.fill(this.colors, -1);
      this.direction = Direction.DOWN;
      this.sprite = UnitTextureAtlasSprite.INSTANCE;
      this.lightEmission = 0;
   }
}
