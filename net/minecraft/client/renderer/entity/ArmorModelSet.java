package net.minecraft.client.renderer.entity;

import com.google.common.collect.ImmutableMap.Builder;
import java.util.function.Function;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.world.entity.EquipmentSlot;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record ArmorModelSet<T>(T head, T chest, T legs, T feet) {
   public T get(EquipmentSlot p_433682_) {
      return (T)(switch (p_433682_) {
         case HEAD -> this.head;
         case CHEST -> this.chest;
         case LEGS -> this.legs;
         case FEET -> this.feet;
         default -> throw new IllegalStateException("No model for slot: " + p_433682_);
      });
   }

   public <U> ArmorModelSet<U> map(Function<? super T, ? extends U> p_433658_) {
      return (ArmorModelSet<U>)(new ArmorModelSet<>(
         p_433658_.apply(this.head), p_433658_.apply(this.chest), p_433658_.apply(this.legs), p_433658_.apply(this.feet)
      ));
   }

   public void putFrom(ArmorModelSet<LayerDefinition> p_434236_, Builder<T, LayerDefinition> p_434711_) {
      p_434711_.put(this.head, p_434236_.head);
      p_434711_.put(this.chest, p_434236_.chest);
      p_434711_.put(this.legs, p_434236_.legs);
      p_434711_.put(this.feet, p_434236_.feet);
   }

   public static <M extends HumanoidModel<?>> ArmorModelSet<M> bake(
      ArmorModelSet<ModelLayerLocation> p_433819_, EntityModelSet p_435042_, Function<ModelPart, M> p_435205_
   ) {
      return p_433819_.map(p_435490_ -> p_435205_.apply(p_435042_.bakeLayer(p_435490_)));
   }
}
