package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HorseModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.FastColor;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.item.AnimalArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class HorseArmorLayer extends RenderLayer<Horse, HorseModel<Horse>> {
   private final HorseModel<Horse> model;

   public HorseArmorLayer(RenderLayerParent<Horse, HorseModel<Horse>> p_174496_, EntityModelSet p_174497_) {
      super(p_174496_);
      this.model = new HorseModel<>(p_174497_.bakeLayer(ModelLayers.HORSE_ARMOR));
   }

   public void render(
      PoseStack p_117032_,
      MultiBufferSource p_117033_,
      int p_117034_,
      Horse p_117035_,
      float p_117036_,
      float p_117037_,
      float p_117038_,
      float p_117039_,
      float p_117040_,
      float p_117041_
   ) {
      ItemStack $$10 = p_117035_.getBodyArmorItem();
      if ($$10.getItem() instanceof AnimalArmorItem $$11 && $$11.getBodyType() == AnimalArmorItem.BodyType.EQUESTRIAN) {
         this.getParentModel().copyPropertiesTo(this.model);
         this.model.prepareMobModel(p_117035_, p_117036_, p_117037_, p_117038_);
         this.model.setupAnim(p_117035_, p_117036_, p_117037_, p_117039_, p_117040_, p_117041_);
         int $$13;
         if ($$10.is(ItemTags.DYEABLE)) {
            $$13 = FastColor.ARGB32.opaque(DyedItemColor.getOrDefault($$10, -6265536));
         } else {
            $$13 = -1;
         }

         VertexConsumer $$15 = p_117033_.getBuffer(RenderType.entityCutoutNoCull($$11.getTexture()));
         this.model.renderToBuffer(p_117032_, $$15, p_117034_, OverlayTexture.NO_OVERLAY, $$13);
      }
   }
}
