package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Map;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.animal.wolf.WolfModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.WolfRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Crackiness;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.Equippable;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class WolfArmorLayer extends RenderLayer<WolfRenderState, WolfModel> {
   private final WolfModel adultModel;
   private final WolfModel babyModel;
   private final EquipmentLayerRenderer equipmentRenderer;
   private static final Map<Crackiness.Level, Identifier> ARMOR_CRACK_LOCATIONS = Map.of(
      Crackiness.Level.LOW,
      Identifier.withDefaultNamespace("textures/entity/wolf/wolf_armor_crackiness_low.png"),
      Crackiness.Level.MEDIUM,
      Identifier.withDefaultNamespace("textures/entity/wolf/wolf_armor_crackiness_medium.png"),
      Crackiness.Level.HIGH,
      Identifier.withDefaultNamespace("textures/entity/wolf/wolf_armor_crackiness_high.png")
   );

   public WolfArmorLayer(RenderLayerParent<WolfRenderState, WolfModel> p_316639_, EntityModelSet p_316756_, EquipmentLayerRenderer p_371602_) {
      super(p_316639_);
      this.adultModel = new WolfModel(p_316756_.bakeLayer(ModelLayers.WOLF_ARMOR));
      this.babyModel = new WolfModel(p_316756_.bakeLayer(ModelLayers.WOLF_BABY_ARMOR));
      this.equipmentRenderer = p_371602_;
   }

   public void submit(PoseStack p_436050_, SubmitNodeCollector p_434212_, int p_433618_, WolfRenderState p_435660_, float p_435015_, float p_434923_) {
      ItemStack itemstack = p_435660_.bodyArmorItem;
      Equippable equippable = itemstack.get(DataComponents.EQUIPPABLE);
      if (equippable != null && !equippable.assetId().isEmpty()) {
         WolfModel wolfmodel = p_435660_.isBaby ? this.babyModel : this.adultModel;
         this.equipmentRenderer
            .renderLayers(
               EquipmentClientInfo.LayerType.WOLF_BODY,
               equippable.assetId().get(),
               wolfmodel,
               p_435660_,
               itemstack,
               p_436050_,
               p_434212_,
               p_433618_,
               p_435660_.outlineColor
            );
         this.maybeRenderCracks(p_436050_, p_434212_, p_433618_, itemstack, wolfmodel, p_435660_);
      }
   }

   private void maybeRenderCracks(
      PoseStack p_331222_, SubmitNodeCollector p_434578_, int p_330931_, ItemStack p_331187_, Model<WolfRenderState> p_364428_, WolfRenderState p_433708_
   ) {
      Crackiness.Level crackiness$level = Crackiness.WOLF_ARMOR.byDamage(p_331187_);
      if (crackiness$level != Crackiness.Level.NONE) {
         Identifier identifier = ARMOR_CRACK_LOCATIONS.get(crackiness$level);
         p_434578_.submitModel(
            p_364428_, p_433708_, p_331222_, RenderTypes.armorTranslucent(identifier), p_330931_, OverlayTexture.NO_OVERLAY, p_433708_.outlineColor, null
         );
      }
   }
}
