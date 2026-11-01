package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.animal.llama.LlamaModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.LlamaRenderState;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.item.equipment.Equippable;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class LlamaDecorLayer extends RenderLayer<LlamaRenderState, LlamaModel> {
   private final LlamaModel adultModel;
   private final LlamaModel babyModel;
   private final EquipmentLayerRenderer equipmentRenderer;

   public LlamaDecorLayer(RenderLayerParent<LlamaRenderState, LlamaModel> p_174499_, EntityModelSet p_174500_, EquipmentLayerRenderer p_371181_) {
      super(p_174499_);
      this.equipmentRenderer = p_371181_;
      this.adultModel = new LlamaModel(p_174500_.bakeLayer(ModelLayers.LLAMA_DECOR));
      this.babyModel = new LlamaModel(p_174500_.bakeLayer(ModelLayers.LLAMA_BABY_DECOR));
   }

   public void submit(PoseStack p_117232_, SubmitNodeCollector p_432905_, int p_117234_, LlamaRenderState p_364326_, float p_117236_, float p_117237_) {
      ItemStack itemstack = p_364326_.bodyItem;
      Equippable equippable = itemstack.get(DataComponents.EQUIPPABLE);
      if (equippable != null && equippable.assetId().isPresent()) {
         this.renderEquipment(p_117232_, p_432905_, p_364326_, itemstack, equippable.assetId().get(), p_117234_);
      } else if (p_364326_.isTraderLlama) {
         this.renderEquipment(p_117232_, p_432905_, p_364326_, ItemStack.EMPTY, EquipmentAssets.TRADER_LLAMA, p_117234_);
      }
   }

   private void renderEquipment(
      PoseStack p_371183_, SubmitNodeCollector p_433890_, LlamaRenderState p_371331_, ItemStack p_371708_, ResourceKey<EquipmentAsset> p_387965_, int p_371469_
   ) {
      LlamaModel llamamodel = p_371331_.isBaby ? this.babyModel : this.adultModel;
      this.equipmentRenderer
         .renderLayers(
            EquipmentClientInfo.LayerType.LLAMA_BODY, p_387965_, llamamodel, p_371331_, p_371708_, p_371183_, p_433890_, p_371469_, p_371331_.outlineColor
         );
   }
}
