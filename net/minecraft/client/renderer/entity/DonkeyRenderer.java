package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.animal.equine.DonkeyModel;
import net.minecraft.client.model.animal.equine.EquineSaddleModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.layers.SimpleEquipmentLayer;
import net.minecraft.client.renderer.entity.state.DonkeyRenderState;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.animal.equine.AbstractChestedHorse;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class DonkeyRenderer<T extends AbstractChestedHorse> extends AbstractHorseRenderer<T, DonkeyRenderState, DonkeyModel> {
   private final Identifier texture;

   public DonkeyRenderer(EntityRendererProvider.Context p_363633_, DonkeyRenderer.Type p_397858_) {
      super(p_363633_, new DonkeyModel(p_363633_.bakeLayer(p_397858_.model)), new DonkeyModel(p_363633_.bakeLayer(p_397858_.babyModel)));
      this.texture = p_397858_.texture;
      this.addLayer(
         new SimpleEquipmentLayer<>(
            this,
            p_363633_.getEquipmentRenderer(),
            p_397858_.saddleLayer,
            p_397707_ -> p_397707_.saddle,
            new EquineSaddleModel(p_363633_.bakeLayer(p_397858_.saddleModel)),
            new EquineSaddleModel(p_363633_.bakeLayer(p_397858_.babySaddleModel))
         )
      );
   }

   public Identifier getTextureLocation(DonkeyRenderState p_363990_) {
      return this.texture;
   }

   public DonkeyRenderState createRenderState() {
      return new DonkeyRenderState();
   }

   public void extractRenderState(T p_481599_, DonkeyRenderState p_363620_, float p_363304_) {
      super.extractRenderState(p_481599_, p_363620_, p_363304_);
      p_363620_.hasChest = p_481599_.hasChest();
   }

   @OnlyIn(Dist.CLIENT)
   public static enum Type {
      DONKEY(
         Identifier.withDefaultNamespace("textures/entity/horse/donkey.png"),
         ModelLayers.DONKEY,
         ModelLayers.DONKEY_BABY,
         EquipmentClientInfo.LayerType.DONKEY_SADDLE,
         ModelLayers.DONKEY_SADDLE,
         ModelLayers.DONKEY_BABY_SADDLE
      ),
      MULE(
         Identifier.withDefaultNamespace("textures/entity/horse/mule.png"),
         ModelLayers.MULE,
         ModelLayers.MULE_BABY,
         EquipmentClientInfo.LayerType.MULE_SADDLE,
         ModelLayers.MULE_SADDLE,
         ModelLayers.MULE_BABY_SADDLE
      );

      final Identifier texture;
      final ModelLayerLocation model;
      final ModelLayerLocation babyModel;
      final EquipmentClientInfo.LayerType saddleLayer;
      final ModelLayerLocation saddleModel;
      final ModelLayerLocation babySaddleModel;

      private Type(
         Identifier p_467802_,
         ModelLayerLocation p_397808_,
         ModelLayerLocation p_397757_,
         EquipmentClientInfo.LayerType p_397247_,
         ModelLayerLocation p_397114_,
         ModelLayerLocation p_398054_
      ) {
         this.texture = p_467802_;
         this.model = p_397808_;
         this.babyModel = p_397757_;
         this.saddleLayer = p_397247_;
         this.saddleModel = p_397114_;
         this.babySaddleModel = p_398054_;
      }
   }
}
