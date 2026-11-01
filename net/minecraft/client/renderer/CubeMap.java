package net.minecraft.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexSorting;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;

@OnlyIn(Dist.CLIENT)
public class CubeMap {
   private static final int SIDES = 6;
   private final ResourceLocation[] images = new ResourceLocation[6];

   public CubeMap(ResourceLocation p_108848_) {
      for (int $$1 = 0; $$1 < 6; $$1++) {
         this.images[$$1] = p_108848_.withPath(p_108848_.getPath() + "_" + $$1 + ".png");
      }
   }

   public void render(Minecraft p_108850_, float p_108851_, float p_108852_, float p_108853_) {
      Tesselator $$4 = Tesselator.getInstance();
      Matrix4f $$5 = new Matrix4f().setPerspective(1.4835298F, (float)p_108850_.getWindow().getWidth() / p_108850_.getWindow().getHeight(), 0.05F, 10.0F);
      RenderSystem.backupProjectionMatrix();
      RenderSystem.setProjectionMatrix($$5, VertexSorting.DISTANCE_TO_ORIGIN);
      Matrix4fStack $$6 = RenderSystem.getModelViewStack();
      $$6.pushMatrix();
      $$6.rotationX((float) Math.PI);
      RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
      RenderSystem.enableBlend();
      RenderSystem.disableCull();
      RenderSystem.depthMask(false);
      int $$7 = 2;

      for (int $$8 = 0; $$8 < 4; $$8++) {
         $$6.pushMatrix();
         float $$9 = ($$8 % 2 / 2.0F - 0.5F) / 256.0F;
         float $$10 = ($$8 / 2 / 2.0F - 0.5F) / 256.0F;
         float $$11 = 0.0F;
         $$6.translate($$9, $$10, 0.0F);
         $$6.rotateX(p_108851_ * (float) (Math.PI / 180.0));
         $$6.rotateY(p_108852_ * (float) (Math.PI / 180.0));
         RenderSystem.applyModelViewMatrix();

         for (int $$12 = 0; $$12 < 6; $$12++) {
            RenderSystem.setShaderTexture(0, this.images[$$12]);
            BufferBuilder $$13 = $$4.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
            int $$14 = Math.round(255.0F * p_108853_) / ($$8 + 1);
            if ($$12 == 0) {
               $$13.addVertex(-1.0F, -1.0F, 1.0F).setUv(0.0F, 0.0F).setWhiteAlpha($$14);
               $$13.addVertex(-1.0F, 1.0F, 1.0F).setUv(0.0F, 1.0F).setWhiteAlpha($$14);
               $$13.addVertex(1.0F, 1.0F, 1.0F).setUv(1.0F, 1.0F).setWhiteAlpha($$14);
               $$13.addVertex(1.0F, -1.0F, 1.0F).setUv(1.0F, 0.0F).setWhiteAlpha($$14);
            }

            if ($$12 == 1) {
               $$13.addVertex(1.0F, -1.0F, 1.0F).setUv(0.0F, 0.0F).setWhiteAlpha($$14);
               $$13.addVertex(1.0F, 1.0F, 1.0F).setUv(0.0F, 1.0F).setWhiteAlpha($$14);
               $$13.addVertex(1.0F, 1.0F, -1.0F).setUv(1.0F, 1.0F).setWhiteAlpha($$14);
               $$13.addVertex(1.0F, -1.0F, -1.0F).setUv(1.0F, 0.0F).setWhiteAlpha($$14);
            }

            if ($$12 == 2) {
               $$13.addVertex(1.0F, -1.0F, -1.0F).setUv(0.0F, 0.0F).setWhiteAlpha($$14);
               $$13.addVertex(1.0F, 1.0F, -1.0F).setUv(0.0F, 1.0F).setWhiteAlpha($$14);
               $$13.addVertex(-1.0F, 1.0F, -1.0F).setUv(1.0F, 1.0F).setWhiteAlpha($$14);
               $$13.addVertex(-1.0F, -1.0F, -1.0F).setUv(1.0F, 0.0F).setWhiteAlpha($$14);
            }

            if ($$12 == 3) {
               $$13.addVertex(-1.0F, -1.0F, -1.0F).setUv(0.0F, 0.0F).setWhiteAlpha($$14);
               $$13.addVertex(-1.0F, 1.0F, -1.0F).setUv(0.0F, 1.0F).setWhiteAlpha($$14);
               $$13.addVertex(-1.0F, 1.0F, 1.0F).setUv(1.0F, 1.0F).setWhiteAlpha($$14);
               $$13.addVertex(-1.0F, -1.0F, 1.0F).setUv(1.0F, 0.0F).setWhiteAlpha($$14);
            }

            if ($$12 == 4) {
               $$13.addVertex(-1.0F, -1.0F, -1.0F).setUv(0.0F, 0.0F).setWhiteAlpha($$14);
               $$13.addVertex(-1.0F, -1.0F, 1.0F).setUv(0.0F, 1.0F).setWhiteAlpha($$14);
               $$13.addVertex(1.0F, -1.0F, 1.0F).setUv(1.0F, 1.0F).setWhiteAlpha($$14);
               $$13.addVertex(1.0F, -1.0F, -1.0F).setUv(1.0F, 0.0F).setWhiteAlpha($$14);
            }

            if ($$12 == 5) {
               $$13.addVertex(-1.0F, 1.0F, 1.0F).setUv(0.0F, 0.0F).setWhiteAlpha($$14);
               $$13.addVertex(-1.0F, 1.0F, -1.0F).setUv(0.0F, 1.0F).setWhiteAlpha($$14);
               $$13.addVertex(1.0F, 1.0F, -1.0F).setUv(1.0F, 1.0F).setWhiteAlpha($$14);
               $$13.addVertex(1.0F, 1.0F, 1.0F).setUv(1.0F, 0.0F).setWhiteAlpha($$14);
            }

            BufferUploader.drawWithShader($$13.buildOrThrow());
         }

         $$6.popMatrix();
         RenderSystem.colorMask(true, true, true, false);
      }

      RenderSystem.colorMask(true, true, true, true);
      RenderSystem.restoreProjectionMatrix();
      $$6.popMatrix();
      RenderSystem.applyModelViewMatrix();
      RenderSystem.depthMask(true);
      RenderSystem.enableCull();
      RenderSystem.enableDepthTest();
   }

   public CompletableFuture<Void> preload(TextureManager p_108855_, Executor p_108856_) {
      CompletableFuture<?>[] $$2 = new CompletableFuture[6];

      for (int $$3 = 0; $$3 < $$2.length; $$3++) {
         $$2[$$3] = p_108855_.preload(this.images[$$3], p_108856_);
      }

      return CompletableFuture.allOf($$2);
   }
}
