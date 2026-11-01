package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.SheepFurModel;
import net.minecraft.client.model.SheepModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.item.DyeColor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SheepFurLayer extends RenderLayer<Sheep, SheepModel<Sheep>> {
   private static final ResourceLocation SHEEP_FUR_LOCATION = ResourceLocation.withDefaultNamespace("textures/entity/sheep/sheep_fur.png");
   private final SheepFurModel<Sheep> model;

   public SheepFurLayer(RenderLayerParent<Sheep, SheepModel<Sheep>> p_174533_, EntityModelSet p_174534_) {
      super(p_174533_);
      this.model = new SheepFurModel<>(p_174534_.bakeLayer(ModelLayers.SHEEP_FUR));
   }

   public void render(
      PoseStack p_117421_,
      MultiBufferSource p_117422_,
      int p_117423_,
      Sheep p_117424_,
      float p_117425_,
      float p_117426_,
      float p_117427_,
      float p_117428_,
      float p_117429_,
      float p_117430_
   ) {
      if (!p_117424_.isSheared()) {
         if (p_117424_.isInvisible()) {
            Minecraft $$10 = Minecraft.getInstance();
            boolean $$11 = $$10.shouldEntityAppearGlowing(p_117424_);
            if ($$11) {
               this.getParentModel().copyPropertiesTo(this.model);
               this.model.prepareMobModel(p_117424_, p_117425_, p_117426_, p_117427_);
               this.model.setupAnim(p_117424_, p_117425_, p_117426_, p_117428_, p_117429_, p_117430_);
               VertexConsumer $$12 = p_117422_.getBuffer(RenderType.outline(SHEEP_FUR_LOCATION));
               this.model.renderToBuffer(p_117421_, $$12, p_117423_, LivingEntityRenderer.getOverlayCoords(p_117424_, 0.0F), -16777216);
            }
         } else {
            int $$21;
            if (p_117424_.hasCustomName() && "jeb_".equals(p_117424_.getName().getString())) {
               int $$13 = 25;
               int $$14 = p_117424_.tickCount / 25 + p_117424_.getId();
               int $$15 = DyeColor.values().length;
               int $$16 = $$14 % $$15;
               int $$17 = ($$14 + 1) % $$15;
               float $$18 = (p_117424_.tickCount % 25 + p_117427_) / 25.0F;
               int $$19 = Sheep.getColor(DyeColor.byId($$16));
               int $$20 = Sheep.getColor(DyeColor.byId($$17));
               $$21 = FastColor.ARGB32.lerp($$18, $$19, $$20);
            } else {
               $$21 = Sheep.getColor(p_117424_.getColor());
            }

            coloredCutoutModelCopyLayerRender(
               this.getParentModel(),
               this.model,
               SHEEP_FUR_LOCATION,
               p_117421_,
               p_117422_,
               p_117423_,
               p_117424_,
               p_117425_,
               p_117426_,
               p_117428_,
               p_117429_,
               p_117430_,
               p_117427_,
               $$21
            );
         }
      }
   }
}
