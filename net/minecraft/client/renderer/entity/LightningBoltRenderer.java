package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LightningBolt;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

@OnlyIn(Dist.CLIENT)
public class LightningBoltRenderer extends EntityRenderer<LightningBolt> {
   public LightningBoltRenderer(EntityRendererProvider.Context p_174286_) {
      super(p_174286_);
   }

   public void render(LightningBolt p_115266_, float p_115267_, float p_115268_, PoseStack p_115269_, MultiBufferSource p_115270_, int p_115271_) {
      float[] $$6 = new float[8];
      float[] $$7 = new float[8];
      float $$8 = 0.0F;
      float $$9 = 0.0F;
      RandomSource $$10 = RandomSource.create(p_115266_.seed);

      for (int $$11 = 7; $$11 >= 0; $$11--) {
         $$6[$$11] = $$8;
         $$7[$$11] = $$9;
         $$8 += $$10.nextInt(11) - 5;
         $$9 += $$10.nextInt(11) - 5;
      }

      VertexConsumer $$12 = p_115270_.getBuffer(RenderType.lightning());
      Matrix4f $$13 = p_115269_.last().pose();

      for (int $$14 = 0; $$14 < 4; $$14++) {
         RandomSource $$15 = RandomSource.create(p_115266_.seed);

         for (int $$16 = 0; $$16 < 3; $$16++) {
            int $$17 = 7;
            int $$18 = 0;
            if ($$16 > 0) {
               $$17 = 7 - $$16;
            }

            if ($$16 > 0) {
               $$18 = $$17 - 2;
            }

            float $$19 = $$6[$$17] - $$8;
            float $$20 = $$7[$$17] - $$9;

            for (int $$21 = $$17; $$21 >= $$18; $$21--) {
               float $$22 = $$19;
               float $$23 = $$20;
               if ($$16 == 0) {
                  $$19 += $$15.nextInt(11) - 5;
                  $$20 += $$15.nextInt(11) - 5;
               } else {
                  $$19 += $$15.nextInt(31) - 15;
                  $$20 += $$15.nextInt(31) - 15;
               }

               float $$24 = 0.5F;
               float $$25 = 0.45F;
               float $$26 = 0.45F;
               float $$27 = 0.5F;
               float $$28 = 0.1F + $$14 * 0.2F;
               if ($$16 == 0) {
                  $$28 *= $$21 * 0.1F + 1.0F;
               }

               float $$29 = 0.1F + $$14 * 0.2F;
               if ($$16 == 0) {
                  $$29 *= ($$21 - 1.0F) * 0.1F + 1.0F;
               }

               quad($$13, $$12, $$19, $$20, $$21, $$22, $$23, 0.45F, 0.45F, 0.5F, $$28, $$29, false, false, true, false);
               quad($$13, $$12, $$19, $$20, $$21, $$22, $$23, 0.45F, 0.45F, 0.5F, $$28, $$29, true, false, true, true);
               quad($$13, $$12, $$19, $$20, $$21, $$22, $$23, 0.45F, 0.45F, 0.5F, $$28, $$29, true, true, false, true);
               quad($$13, $$12, $$19, $$20, $$21, $$22, $$23, 0.45F, 0.45F, 0.5F, $$28, $$29, false, true, false, false);
            }
         }
      }
   }

   private static void quad(
      Matrix4f p_253966_,
      VertexConsumer p_115274_,
      float p_115275_,
      float p_115276_,
      int p_115277_,
      float p_115278_,
      float p_115279_,
      float p_115280_,
      float p_115281_,
      float p_115282_,
      float p_115283_,
      float p_115284_,
      boolean p_115285_,
      boolean p_115286_,
      boolean p_115287_,
      boolean p_115288_
   ) {
      p_115274_.addVertex(
            p_253966_, p_115275_ + (p_115285_ ? p_115284_ : -p_115284_), (float)(p_115277_ * 16), p_115276_ + (p_115286_ ? p_115284_ : -p_115284_)
         )
         .setColor(p_115280_, p_115281_, p_115282_, 0.3F);
      p_115274_.addVertex(
            p_253966_, p_115278_ + (p_115285_ ? p_115283_ : -p_115283_), (float)((p_115277_ + 1) * 16), p_115279_ + (p_115286_ ? p_115283_ : -p_115283_)
         )
         .setColor(p_115280_, p_115281_, p_115282_, 0.3F);
      p_115274_.addVertex(
            p_253966_, p_115278_ + (p_115287_ ? p_115283_ : -p_115283_), (float)((p_115277_ + 1) * 16), p_115279_ + (p_115288_ ? p_115283_ : -p_115283_)
         )
         .setColor(p_115280_, p_115281_, p_115282_, 0.3F);
      p_115274_.addVertex(
            p_253966_, p_115275_ + (p_115287_ ? p_115284_ : -p_115284_), (float)(p_115277_ * 16), p_115276_ + (p_115288_ ? p_115284_ : -p_115284_)
         )
         .setColor(p_115280_, p_115281_, p_115282_, 0.3F);
   }

   public ResourceLocation getTextureLocation(LightningBolt p_115264_) {
      return TextureAtlas.LOCATION_BLOCKS;
   }
}
