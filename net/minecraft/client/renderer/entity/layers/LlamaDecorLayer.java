package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
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

@Environment(EnvType.CLIENT)
public class LlamaDecorLayer extends RenderLayer<LlamaRenderState, LlamaModel> {
   private final LlamaModel adultModel;
   private final LlamaModel babyModel;
   private final EquipmentLayerRenderer equipmentRenderer;

   public LlamaDecorLayer(RenderLayerParent<LlamaRenderState, LlamaModel> renderer, EntityModelSet models, EquipmentLayerRenderer equipmentRenderer) {
      super(renderer);
      this.equipmentRenderer = equipmentRenderer;
      this.adultModel = new LlamaModel(models.bakeLayer(ModelLayers.LLAMA_DECOR));
      this.babyModel = new LlamaModel(models.bakeLayer(ModelLayers.LLAMA_BABY_DECOR));
   }

   public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int i, LlamaRenderState llamaRenderState, float f, float g) {
      ItemStack itemStack = llamaRenderState.bodyItem;
      Equippable equippable = itemStack.get(DataComponents.EQUIPPABLE);
      if (equippable != null && equippable.assetId().isPresent()) {
         this.renderEquipment(poseStack, submitNodeCollector, llamaRenderState, itemStack, equippable.assetId().get(), i);
      } else if (llamaRenderState.isTraderLlama) {
         this.renderEquipment(poseStack, submitNodeCollector, llamaRenderState, ItemStack.EMPTY, EquipmentAssets.TRADER_LLAMA, i);
      }
   }

   private void renderEquipment(
      PoseStack poseStack,
      SubmitNodeCollector nodeCollector,
      LlamaRenderState renderState,
      ItemStack item,
      ResourceKey<EquipmentAsset> equipmentAseet,
      int packedLight
   ) {
      LlamaModel llamaModel = renderState.isBaby ? this.babyModel : this.adultModel;
      this.equipmentRenderer
         .renderLayers(
            EquipmentClientInfo.LayerType.LLAMA_BODY,
            equipmentAseet,
            llamaModel,
            renderState,
            item,
            poseStack,
            nodeCollector,
            packedLight,
            renderState.outlineColor
         );
   }
}
