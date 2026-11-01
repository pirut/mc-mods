package net.minecraft.client.data.models.blockstates;

import com.google.common.collect.ImmutableList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import net.minecraft.util.Util;
import net.minecraft.world.level.block.state.properties.Property;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record PropertyValueList(List<Property.Value<?>> values) {
   public static final PropertyValueList EMPTY = new PropertyValueList(List.of());
   private static final Comparator<Property.Value<?>> COMPARE_BY_NAME = Comparator.comparing(p_404853_ -> p_404853_.property().getName());

   public PropertyValueList extend(Property.Value<?> p_404909_) {
      return new PropertyValueList(Util.copyAndAdd(this.values, p_404909_));
   }

   public PropertyValueList extend(PropertyValueList p_405394_) {
      return new PropertyValueList(ImmutableList.builder().addAll(this.values).addAll(p_405394_.values).build());
   }

   public static PropertyValueList of(Property.Value<?>... p_405119_) {
      return new PropertyValueList(List.of(p_405119_));
   }

   public String getKey() {
      return this.values.stream().sorted(COMPARE_BY_NAME).map(Property.Value::toString).collect(Collectors.joining(","));
   }

   @Override
   public String toString() {
      return this.getKey();
   }
}
