package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class StuckInBodyLayer<T extends LivingEntity, M extends PlayerModel<T>> extends RenderLayer<T, M> {
   public StuckInBodyLayer(LivingEntityRenderer<T, M> p_117564_) {
      super(p_117564_);
   }

   protected abstract int numStuck(T var1);

   protected abstract void renderStuckItem(PoseStack var1, MultiBufferSource var2, int var3, Entity var4, float var5, float var6, float var7, float var8);

   public void render(
      PoseStack p_117586_,
      MultiBufferSource p_117587_,
      int p_117588_,
      T p_117589_,
      float p_117590_,
      float p_117591_,
      float p_117592_,
      float p_117593_,
      float p_117594_,
      float p_117595_
   ) {
      int $$10 = this.numStuck(p_117589_);
      RandomSource $$11 = RandomSource.create(p_117589_.getId());
      if ($$10 > 0) {
         for (int $$12 = 0; $$12 < $$10; $$12++) {
            p_117586_.pushPose();
            ModelPart $$13 = this.getParentModel().getRandomModelPart($$11);
            ModelPart.Cube $$14 = $$13.getRandomCube($$11);
            $$13.translateAndRotate(p_117586_);
            float $$15 = $$11.nextFloat();
            float $$16 = $$11.nextFloat();
            float $$17 = $$11.nextFloat();
            float $$18 = Mth.lerp($$15, $$14.minX, $$14.maxX) / 16.0F;
            float $$19 = Mth.lerp($$16, $$14.minY, $$14.maxY) / 16.0F;
            float $$20 = Mth.lerp($$17, $$14.minZ, $$14.maxZ) / 16.0F;
            p_117586_.translate($$18, $$19, $$20);
            $$15 = -1.0F * ($$15 * 2.0F - 1.0F);
            $$16 = -1.0F * ($$16 * 2.0F - 1.0F);
            $$17 = -1.0F * ($$17 * 2.0F - 1.0F);
            this.renderStuckItem(p_117586_, p_117587_, p_117588_, p_117589_, $$15, $$16, $$17, p_117592_);
            p_117586_.popPose();
         }
      }
   }
}
