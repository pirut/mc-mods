package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.Equippable;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class HumanoidArmorLayer<S extends HumanoidRenderState, M extends HumanoidModel<S>, A extends HumanoidModel<S>> extends RenderLayer<S, M> {
   private final ArmorModelSet<A> modelSet;
   private final ArmorModelSet<A> babyModelSet;
   private final EquipmentLayerRenderer equipmentRenderer;

   public HumanoidArmorLayer(RenderLayerParent<S, M> p_360748_, ArmorModelSet<A> p_434098_, EquipmentLayerRenderer p_371733_) {
      this(p_360748_, p_434098_, p_434098_, p_371733_);
   }

   public HumanoidArmorLayer(RenderLayerParent<S, M> p_267286_, ArmorModelSet<A> p_433392_, ArmorModelSet<A> p_434792_, EquipmentLayerRenderer p_371362_) {
      super(p_267286_);
      this.modelSet = p_433392_;
      this.babyModelSet = p_434792_;
      this.equipmentRenderer = p_371362_;
   }

   public static boolean shouldRender(ItemStack p_371911_, EquipmentSlot p_371669_) {
      Equippable equippable = p_371911_.get(DataComponents.EQUIPPABLE);
      return equippable != null && shouldRender(equippable, p_371669_);
   }

   private static boolean shouldRender(Equippable p_371295_, EquipmentSlot p_371795_) {
      return p_371295_.assetId().isPresent() && p_371295_.slot() == p_371795_;
   }

   public void submit(PoseStack p_435921_, SubmitNodeCollector p_434130_, int p_434678_, S p_435902_, float p_435802_, float p_434554_) {
      this.renderArmorPiece(p_435921_, p_434130_, p_435902_.chestEquipment, EquipmentSlot.CHEST, p_434678_, p_435902_);
      this.renderArmorPiece(p_435921_, p_434130_, p_435902_.legsEquipment, EquipmentSlot.LEGS, p_434678_, p_435902_);
      this.renderArmorPiece(p_435921_, p_434130_, p_435902_.feetEquipment, EquipmentSlot.FEET, p_434678_, p_435902_);
      this.renderArmorPiece(p_435921_, p_434130_, p_435902_.headEquipment, EquipmentSlot.HEAD, p_434678_, p_435902_);
   }

   private void renderArmorPiece(PoseStack p_117119_, SubmitNodeCollector p_433453_, ItemStack p_362532_, EquipmentSlot p_117122_, int p_117123_, S p_435126_) {
      Equippable equippable = p_362532_.get(DataComponents.EQUIPPABLE);
      if (equippable != null && shouldRender(equippable, p_117122_)) {
         A a = this.getArmorModel(p_435126_, p_117122_);
         EquipmentClientInfo.LayerType equipmentclientinfo$layertype = this.usesInnerModel(p_117122_)
            ? EquipmentClientInfo.LayerType.HUMANOID_LEGGINGS
            : EquipmentClientInfo.LayerType.HUMANOID;
         this.equipmentRenderer
            .renderLayers(
               equipmentclientinfo$layertype,
               equippable.assetId().orElseThrow(),
               a,
               p_435126_,
               p_362532_,
               p_117119_,
               p_433453_,
               p_117123_,
               p_435126_.outlineColor
            );
      }
   }

   private A getArmorModel(S p_362738_, EquipmentSlot p_117079_) {
      return (p_362738_.isBaby ? this.babyModelSet : this.modelSet).get(p_117079_);
   }

   private boolean usesInnerModel(EquipmentSlot p_117129_) {
      return p_117129_ == EquipmentSlot.LEGS;
   }
}
