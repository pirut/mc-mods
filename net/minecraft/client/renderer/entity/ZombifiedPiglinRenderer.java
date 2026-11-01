package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.monster.piglin.ZombifiedPiglinModel;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.state.ZombifiedPiglinRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.monster.zombie.ZombifiedPiglin;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ZombifiedPiglinRenderer extends HumanoidMobRenderer<ZombifiedPiglin, ZombifiedPiglinRenderState, ZombifiedPiglinModel> {
   private static final Identifier ZOMBIFIED_PIGLIN_LOCATION = Identifier.withDefaultNamespace("textures/entity/piglin/zombified_piglin.png");

   public ZombifiedPiglinRenderer(
      EntityRendererProvider.Context p_361123_,
      ModelLayerLocation p_360957_,
      ModelLayerLocation p_363099_,
      ArmorModelSet<ModelLayerLocation> p_433299_,
      ArmorModelSet<ModelLayerLocation> p_434861_
   ) {
      super(
         p_361123_,
         new ZombifiedPiglinModel(p_361123_.bakeLayer(p_360957_)),
         new ZombifiedPiglinModel(p_361123_.bakeLayer(p_363099_)),
         0.5F,
         PiglinRenderer.PIGLIN_CUSTOM_HEAD_TRANSFORMS
      );
      this.addLayer(
         new HumanoidArmorLayer<>(
            this,
            ArmorModelSet.bake(p_433299_, p_361123_.getModelSet(), ZombifiedPiglinModel::new),
            ArmorModelSet.bake(p_434861_, p_361123_.getModelSet(), ZombifiedPiglinModel::new),
            p_361123_.getEquipmentRenderer()
         )
      );
   }

   public Identifier getTextureLocation(ZombifiedPiglinRenderState p_362156_) {
      return ZOMBIFIED_PIGLIN_LOCATION;
   }

   public ZombifiedPiglinRenderState createRenderState() {
      return new ZombifiedPiglinRenderState();
   }

   public void extractRenderState(ZombifiedPiglin p_481234_, ZombifiedPiglinRenderState p_361548_, float p_361739_) {
      super.extractRenderState(p_481234_, p_361548_, p_361739_);
      p_361548_.isAggressive = p_481234_.isAggressive();
   }
}
