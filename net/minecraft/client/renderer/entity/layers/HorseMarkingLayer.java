package net.minecraft.client.renderer.entity.layers;

import com.google.common.collect.Maps;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Map;
import net.minecraft.Util;
import net.minecraft.client.model.HorseModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.entity.animal.horse.Markings;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class HorseMarkingLayer extends RenderLayer<Horse, HorseModel<Horse>> {
   private static final Map<Markings, ResourceLocation> LOCATION_BY_MARKINGS = Util.make(Maps.newEnumMap(Markings.class), p_349908_ -> {
      p_349908_.put(Markings.NONE, null);
      p_349908_.put(Markings.WHITE, ResourceLocation.withDefaultNamespace("textures/entity/horse/horse_markings_white.png"));
      p_349908_.put(Markings.WHITE_FIELD, ResourceLocation.withDefaultNamespace("textures/entity/horse/horse_markings_whitefield.png"));
      p_349908_.put(Markings.WHITE_DOTS, ResourceLocation.withDefaultNamespace("textures/entity/horse/horse_markings_whitedots.png"));
      p_349908_.put(Markings.BLACK_DOTS, ResourceLocation.withDefaultNamespace("textures/entity/horse/horse_markings_blackdots.png"));
   });

   public HorseMarkingLayer(RenderLayerParent<Horse, HorseModel<Horse>> p_117045_) {
      super(p_117045_);
   }

   public void render(
      PoseStack p_117058_,
      MultiBufferSource p_117059_,
      int p_117060_,
      Horse p_117061_,
      float p_117062_,
      float p_117063_,
      float p_117064_,
      float p_117065_,
      float p_117066_,
      float p_117067_
   ) {
      ResourceLocation $$10 = LOCATION_BY_MARKINGS.get(p_117061_.getMarkings());
      if ($$10 != null && !p_117061_.isInvisible()) {
         VertexConsumer $$11 = p_117059_.getBuffer(RenderType.entityTranslucent($$10));
         this.getParentModel().renderToBuffer(p_117058_, $$11, p_117060_, LivingEntityRenderer.getOverlayCoords(p_117061_, 0.0F));
      }
   }
}
