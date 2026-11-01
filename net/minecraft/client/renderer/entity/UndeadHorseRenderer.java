package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.animal.equine.AbstractEquineModel;
import net.minecraft.client.model.animal.equine.EquineSaddleModel;
import net.minecraft.client.model.animal.equine.HorseModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.layers.SimpleEquipmentLayer;
import net.minecraft.client.renderer.entity.state.EquineRenderState;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class UndeadHorseRenderer extends AbstractHorseRenderer<AbstractHorse, EquineRenderState, AbstractEquineModel<EquineRenderState>> {
   private final Identifier texture;

   public UndeadHorseRenderer(EntityRendererProvider.Context p_174432_, UndeadHorseRenderer.Type p_398062_) {
      super(p_174432_, new HorseModel(p_174432_.bakeLayer(p_398062_.model)), new HorseModel(p_174432_.bakeLayer(p_398062_.babyModel)));
      this.texture = p_398062_.texture;
      this.addLayer(
         new SimpleEquipmentLayer<>(
            this,
            p_174432_.getEquipmentRenderer(),
            EquipmentClientInfo.LayerType.HORSE_BODY,
            p_454364_ -> p_454364_.bodyArmorItem,
            new HorseModel(p_174432_.bakeLayer(ModelLayers.UNDEAD_HORSE_ARMOR)),
            new HorseModel(p_174432_.bakeLayer(ModelLayers.UNDEAD_HORSE_BABY_ARMOR))
         )
      );
      this.addLayer(
         new SimpleEquipmentLayer<>(
            this,
            p_174432_.getEquipmentRenderer(),
            p_398062_.saddleLayer,
            p_397407_ -> p_397407_.saddle,
            new EquineSaddleModel(p_174432_.bakeLayer(p_398062_.saddleModel)),
            new EquineSaddleModel(p_174432_.bakeLayer(p_398062_.babySaddleModel))
         )
      );
   }

   public Identifier getTextureLocation(EquineRenderState p_362480_) {
      return this.texture;
   }

   public EquineRenderState createRenderState() {
      return new EquineRenderState();
   }

   @OnlyIn(Dist.CLIENT)
   public static enum Type {
      SKELETON(
         Identifier.withDefaultNamespace("textures/entity/horse/horse_skeleton.png"),
         ModelLayers.SKELETON_HORSE,
         ModelLayers.SKELETON_HORSE_BABY,
         EquipmentClientInfo.LayerType.SKELETON_HORSE_SADDLE,
         ModelLayers.SKELETON_HORSE_SADDLE,
         ModelLayers.SKELETON_HORSE_BABY_SADDLE
      ),
      ZOMBIE(
         Identifier.withDefaultNamespace("textures/entity/horse/horse_zombie.png"),
         ModelLayers.ZOMBIE_HORSE,
         ModelLayers.ZOMBIE_HORSE_BABY,
         EquipmentClientInfo.LayerType.ZOMBIE_HORSE_SADDLE,
         ModelLayers.ZOMBIE_HORSE_SADDLE,
         ModelLayers.ZOMBIE_HORSE_BABY_SADDLE
      );

      final Identifier texture;
      final ModelLayerLocation model;
      final ModelLayerLocation babyModel;
      final EquipmentClientInfo.LayerType saddleLayer;
      final ModelLayerLocation saddleModel;
      final ModelLayerLocation babySaddleModel;

      private Type(
         Identifier p_468288_,
         ModelLayerLocation p_397879_,
         ModelLayerLocation p_397732_,
         EquipmentClientInfo.LayerType p_397884_,
         ModelLayerLocation p_397255_,
         ModelLayerLocation p_397298_
      ) {
         this.texture = p_468288_;
         this.model = p_397879_;
         this.babyModel = p_397732_;
         this.saddleLayer = p_397884_;
         this.saddleModel = p_397255_;
         this.babySaddleModel = p_397298_;
      }
   }
}
