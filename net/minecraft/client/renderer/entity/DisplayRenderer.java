package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.mojang.math.Transformation;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Display;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

@OnlyIn(Dist.CLIENT)
public abstract class DisplayRenderer<T extends Display, S> extends EntityRenderer<T> {
   private final EntityRenderDispatcher entityRenderDispatcher;

   protected DisplayRenderer(EntityRendererProvider.Context p_270168_) {
      super(p_270168_);
      this.entityRenderDispatcher = p_270168_.getEntityRenderDispatcher();
   }

   public ResourceLocation getTextureLocation(T p_270675_) {
      return TextureAtlas.LOCATION_BLOCKS;
   }

   public void render(T p_270405_, float p_270225_, float p_270279_, PoseStack p_270728_, MultiBufferSource p_270209_, int p_270298_) {
      Display.RenderState $$6 = p_270405_.renderState();
      if ($$6 != null) {
         S $$7 = this.getSubState(p_270405_);
         if ($$7 != null) {
            float $$8 = p_270405_.calculateInterpolationProgress(p_270279_);
            this.shadowRadius = $$6.shadowRadius().get($$8);
            this.shadowStrength = $$6.shadowStrength().get($$8);
            int $$9 = $$6.brightnessOverride();
            int $$10 = $$9 != -1 ? $$9 : p_270298_;
            super.render(p_270405_, p_270225_, p_270279_, p_270728_, p_270209_, $$10);
            p_270728_.pushPose();
            p_270728_.mulPose(this.calculateOrientation($$6, p_270405_, p_270279_, new Quaternionf()));
            Transformation $$11 = $$6.transformation().get($$8);
            p_270728_.mulPose($$11.getMatrix());
            this.renderInner(p_270405_, $$7, p_270728_, p_270209_, $$10, $$8);
            p_270728_.popPose();
         }
      }
   }

   private Quaternionf calculateOrientation(Display.RenderState p_277846_, T p_271013_, float p_294432_, Quaternionf p_295809_) {
      Camera $$4 = this.entityRenderDispatcher.camera;

      return switch (p_277846_.billboardConstraints()) {
         case FIXED -> p_295809_.rotationYXZ(
            (float) (-Math.PI / 180.0) * entityYRot(p_271013_, p_294432_), (float) (Math.PI / 180.0) * entityXRot(p_271013_, p_294432_), 0.0F
         );
         case HORIZONTAL -> p_295809_.rotationYXZ(
            (float) (-Math.PI / 180.0) * entityYRot(p_271013_, p_294432_), (float) (Math.PI / 180.0) * cameraXRot($$4), 0.0F
         );
         case VERTICAL -> p_295809_.rotationYXZ(
            (float) (-Math.PI / 180.0) * cameraYrot($$4), (float) (Math.PI / 180.0) * entityXRot(p_271013_, p_294432_), 0.0F
         );
         case CENTER -> p_295809_.rotationYXZ((float) (-Math.PI / 180.0) * cameraYrot($$4), (float) (Math.PI / 180.0) * cameraXRot($$4), 0.0F);
      };
   }

   private static float cameraYrot(Camera p_295988_) {
      return p_295988_.getYRot() - 180.0F;
   }

   private static float cameraXRot(Camera p_295299_) {
      return -p_295299_.getXRot();
   }

   private static <T extends Display> float entityYRot(T p_295109_, float p_295760_) {
      return Mth.rotLerp(p_295760_, p_295109_.yRotO, p_295109_.getYRot());
   }

   private static <T extends Display> float entityXRot(T p_295884_, float p_294935_) {
      return Mth.lerp(p_294935_, p_295884_.xRotO, p_295884_.getXRot());
   }

   @Nullable
   protected abstract S getSubState(T var1);

   protected abstract void renderInner(T var1, S var2, PoseStack var3, MultiBufferSource var4, int var5, float var6);

   @OnlyIn(Dist.CLIENT)
   public static class BlockDisplayRenderer extends DisplayRenderer<Display.BlockDisplay, Display.BlockDisplay.BlockRenderState> {
      private final BlockRenderDispatcher blockRenderer;

      protected BlockDisplayRenderer(EntityRendererProvider.Context p_270283_) {
         super(p_270283_);
         this.blockRenderer = p_270283_.getBlockRenderDispatcher();
      }

      @Nullable
      protected Display.BlockDisplay.BlockRenderState getSubState(Display.BlockDisplay p_277721_) {
         return p_277721_.blockRenderState();
      }

      public void renderInner(
         Display.BlockDisplay p_277939_,
         Display.BlockDisplay.BlockRenderState p_277885_,
         PoseStack p_277831_,
         MultiBufferSource p_277554_,
         int p_278071_,
         float p_277847_
      ) {
         this.blockRenderer.renderSingleBlock(p_277885_.blockState(), p_277831_, p_277554_, p_278071_, OverlayTexture.NO_OVERLAY);
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class ItemDisplayRenderer extends DisplayRenderer<Display.ItemDisplay, Display.ItemDisplay.ItemRenderState> {
      private final ItemRenderer itemRenderer;

      protected ItemDisplayRenderer(EntityRendererProvider.Context p_270110_) {
         super(p_270110_);
         this.itemRenderer = p_270110_.getItemRenderer();
      }

      @Nullable
      protected Display.ItemDisplay.ItemRenderState getSubState(Display.ItemDisplay p_277464_) {
         return p_277464_.itemRenderState();
      }

      public void renderInner(
         Display.ItemDisplay p_277863_,
         Display.ItemDisplay.ItemRenderState p_277481_,
         PoseStack p_277889_,
         MultiBufferSource p_277509_,
         int p_277861_,
         float p_277670_
      ) {
         p_277889_.mulPose(Axis.YP.rotation((float) Math.PI));
         this.itemRenderer
            .renderStatic(
               p_277481_.itemStack(),
               p_277481_.itemTransform(),
               p_277861_,
               OverlayTexture.NO_OVERLAY,
               p_277889_,
               p_277509_,
               p_277863_.level(),
               p_277863_.getId()
            );
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class TextDisplayRenderer extends DisplayRenderer<Display.TextDisplay, Display.TextDisplay.TextRenderState> {
      private final Font font;

      protected TextDisplayRenderer(EntityRendererProvider.Context p_271012_) {
         super(p_271012_);
         this.font = p_271012_.getFont();
      }

      private Display.TextDisplay.CachedInfo splitLines(Component p_270823_, int p_270893_) {
         List<FormattedCharSequence> $$2 = this.font.split(p_270823_, p_270893_);
         List<Display.TextDisplay.CachedLine> $$3 = new ArrayList<>($$2.size());
         int $$4 = 0;

         for (FormattedCharSequence $$5 : $$2) {
            int $$6 = this.font.width($$5);
            $$4 = Math.max($$4, $$6);
            $$3.add(new Display.TextDisplay.CachedLine($$5, $$6));
         }

         return new Display.TextDisplay.CachedInfo($$3, $$4);
      }

      @Nullable
      protected Display.TextDisplay.TextRenderState getSubState(Display.TextDisplay p_277947_) {
         return p_277947_.textRenderState();
      }

      public void renderInner(
         Display.TextDisplay p_277522_,
         Display.TextDisplay.TextRenderState p_277620_,
         PoseStack p_277536_,
         MultiBufferSource p_277845_,
         int p_278046_,
         float p_277769_
      ) {
         byte $$6 = p_277620_.flags();
         boolean $$7 = ($$6 & 2) != 0;
         boolean $$8 = ($$6 & 4) != 0;
         boolean $$9 = ($$6 & 1) != 0;
         Display.TextDisplay.Align $$10 = Display.TextDisplay.getAlign($$6);
         byte $$11 = (byte)p_277620_.textOpacity().get(p_277769_);
         int $$13;
         if ($$8) {
            float $$12 = Minecraft.getInstance().options.getBackgroundOpacity(0.25F);
            $$13 = (int)($$12 * 255.0F) << 24;
         } else {
            $$13 = p_277620_.backgroundColor().get(p_277769_);
         }

         float $$15 = 0.0F;
         Matrix4f $$16 = p_277536_.last().pose();
         $$16.rotate((float) Math.PI, 0.0F, 1.0F, 0.0F);
         $$16.scale(-0.025F, -0.025F, -0.025F);
         Display.TextDisplay.CachedInfo $$17 = p_277522_.cacheDisplay(this::splitLines);
         int $$18 = 9 + 1;
         int $$19 = $$17.width();
         int $$20 = $$17.lines().size() * $$18;
         $$16.translate(1.0F - $$19 / 2.0F, -$$20, 0.0F);
         if ($$13 != 0) {
            VertexConsumer $$21 = p_277845_.getBuffer($$7 ? RenderType.textBackgroundSeeThrough() : RenderType.textBackground());
            $$21.addVertex($$16, -1.0F, -1.0F, 0.0F).setColor($$13).setLight(p_278046_);
            $$21.addVertex($$16, -1.0F, (float)$$20, 0.0F).setColor($$13).setLight(p_278046_);
            $$21.addVertex($$16, (float)$$19, (float)$$20, 0.0F).setColor($$13).setLight(p_278046_);
            $$21.addVertex($$16, (float)$$19, -1.0F, 0.0F).setColor($$13).setLight(p_278046_);
         }

         for (Display.TextDisplay.CachedLine $$22 : $$17.lines()) {
            float $$23 = switch ($$10) {
               case LEFT -> 0.0F;
               case RIGHT -> $$19 - $$22.width();
               case CENTER -> $$19 / 2.0F - $$22.width() / 2.0F;
            };
            this.font
               .drawInBatch(
                  $$22.contents(),
                  $$23,
                  $$15,
                  $$11 << 24 | 16777215,
                  $$9,
                  $$16,
                  p_277845_,
                  $$7 ? Font.DisplayMode.SEE_THROUGH : Font.DisplayMode.POLYGON_OFFSET,
                  0,
                  p_278046_
               );
            $$15 += $$18;
         }
      }
   }
}
