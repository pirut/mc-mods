package net.minecraft.client.data.models.model;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import net.minecraft.client.color.item.Constant;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.renderer.item.BlockModelWrapper;
import net.minecraft.client.renderer.item.CompositeModel;
import net.minecraft.client.renderer.item.ConditionalItemModel;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.RangeSelectItemModel;
import net.minecraft.client.renderer.item.SelectItemModel;
import net.minecraft.client.renderer.item.SpecialModelWrapper;
import net.minecraft.client.renderer.item.properties.conditional.ConditionalItemModelProperty;
import net.minecraft.client.renderer.item.properties.conditional.HasComponent;
import net.minecraft.client.renderer.item.properties.conditional.IsUsingItem;
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperty;
import net.minecraft.client.renderer.item.properties.select.ContextDimension;
import net.minecraft.client.renderer.item.properties.select.ItemBlockState;
import net.minecraft.client.renderer.item.properties.select.LocalTime;
import net.minecraft.client.renderer.item.properties.select.SelectItemModelProperty;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.resources.Identifier;
import net.minecraft.util.SpecialDates;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.properties.Property;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ItemModelUtils {
   public static ItemModel.Unbaked plainModel(Identifier p_467851_) {
      return new BlockModelWrapper.Unbaked(p_467851_, List.of());
   }

   public static ItemModel.Unbaked tintedModel(Identifier p_468762_, ItemTintSource... p_387952_) {
      return new BlockModelWrapper.Unbaked(p_468762_, List.of(p_387952_));
   }

   public static ItemTintSource constantTint(int p_388504_) {
      return new Constant(p_388504_);
   }

   public static ItemModel.Unbaked composite(ItemModel.Unbaked... p_388517_) {
      return new CompositeModel.Unbaked(List.of(p_388517_));
   }

   public static ItemModel.Unbaked specialModel(Identifier p_467567_, SpecialModelRenderer.Unbaked p_388113_) {
      return new SpecialModelWrapper.Unbaked(p_467567_, p_388113_);
   }

   public static RangeSelectItemModel.Entry override(ItemModel.Unbaked p_387878_, float p_387777_) {
      return new RangeSelectItemModel.Entry(p_387777_, p_387878_);
   }

   public static ItemModel.Unbaked rangeSelect(RangeSelectItemModelProperty p_387346_, ItemModel.Unbaked p_387381_, RangeSelectItemModel.Entry... p_388600_) {
      return new RangeSelectItemModel.Unbaked(p_387346_, 1.0F, List.of(p_388600_), Optional.of(p_387381_));
   }

   public static ItemModel.Unbaked rangeSelect(
      RangeSelectItemModelProperty p_388147_, float p_388666_, ItemModel.Unbaked p_386754_, RangeSelectItemModel.Entry... p_387862_
   ) {
      return new RangeSelectItemModel.Unbaked(p_388147_, p_388666_, List.of(p_387862_), Optional.of(p_386754_));
   }

   public static ItemModel.Unbaked rangeSelect(RangeSelectItemModelProperty p_388948_, ItemModel.Unbaked p_387298_, List<RangeSelectItemModel.Entry> p_387580_) {
      return new RangeSelectItemModel.Unbaked(p_388948_, 1.0F, p_387580_, Optional.of(p_387298_));
   }

   public static ItemModel.Unbaked rangeSelect(RangeSelectItemModelProperty p_387249_, List<RangeSelectItemModel.Entry> p_388502_) {
      return new RangeSelectItemModel.Unbaked(p_387249_, 1.0F, p_388502_, Optional.empty());
   }

   public static ItemModel.Unbaked rangeSelect(RangeSelectItemModelProperty p_388663_, float p_387398_, List<RangeSelectItemModel.Entry> p_388826_) {
      return new RangeSelectItemModel.Unbaked(p_388663_, p_387398_, p_388826_, Optional.empty());
   }

   public static ItemModel.Unbaked conditional(ConditionalItemModelProperty p_386882_, ItemModel.Unbaked p_387299_, ItemModel.Unbaked p_388803_) {
      return new ConditionalItemModel.Unbaked(p_386882_, p_387299_, p_388803_);
   }

   public static <T> SelectItemModel.SwitchCase<T> when(T p_386607_, ItemModel.Unbaked p_387168_) {
      return new SelectItemModel.SwitchCase<>(List.of(p_386607_), p_387168_);
   }

   public static <T> SelectItemModel.SwitchCase<T> when(List<T> p_387239_, ItemModel.Unbaked p_387616_) {
      return new SelectItemModel.SwitchCase<>(p_387239_, p_387616_);
   }

   @SafeVarargs
   public static <T> ItemModel.Unbaked select(SelectItemModelProperty<T> p_388227_, ItemModel.Unbaked p_388472_, SelectItemModel.SwitchCase<T>... p_388931_) {
      return select(p_388227_, p_388472_, List.of(p_388931_));
   }

   public static <T> ItemModel.Unbaked select(SelectItemModelProperty<T> p_388370_, ItemModel.Unbaked p_386464_, List<SelectItemModel.SwitchCase<T>> p_386901_) {
      return new SelectItemModel.Unbaked(new SelectItemModel.UnbakedSwitch<>(p_388370_, p_386901_), Optional.of(p_386464_));
   }

   @SafeVarargs
   public static <T> ItemModel.Unbaked select(SelectItemModelProperty<T> p_388303_, SelectItemModel.SwitchCase<T>... p_387370_) {
      return select(p_388303_, List.of(p_387370_));
   }

   public static <T> ItemModel.Unbaked select(SelectItemModelProperty<T> p_386947_, List<SelectItemModel.SwitchCase<T>> p_388763_) {
      return new SelectItemModel.Unbaked(new SelectItemModel.UnbakedSwitch<>(p_386947_, p_388763_), Optional.empty());
   }

   public static ConditionalItemModelProperty isUsingItem() {
      return new IsUsingItem();
   }

   public static ConditionalItemModelProperty hasComponent(DataComponentType<?> p_388440_) {
      return new HasComponent(p_388440_, false);
   }

   public static ItemModel.Unbaked inOverworld(ItemModel.Unbaked p_390531_, ItemModel.Unbaked p_390373_) {
      return select(new ContextDimension(), p_390373_, when(Level.OVERWORLD, p_390531_));
   }

   public static <T extends Comparable<T>> ItemModel.Unbaked selectBlockItemProperty(
      Property<T> p_388749_, ItemModel.Unbaked p_386703_, Map<T, ItemModel.Unbaked> p_388827_
   ) {
      List<SelectItemModel.SwitchCase<String>> list = p_388827_.entrySet().stream().sorted(Entry.comparingByKey()).map(p_388831_ -> {
         String s = p_388749_.getName(p_388831_.getKey());
         return new SelectItemModel.SwitchCase<>(List.of(s), p_388831_.getValue());
      }).toList();
      return select(new ItemBlockState(p_388749_.getName()), p_386703_, list);
   }

   public static ItemModel.Unbaked isXmas(ItemModel.Unbaked p_389525_, ItemModel.Unbaked p_389654_) {
      DateTimeFormatter datetimeformatter = DateTimeFormatter.ofPattern("MM-dd", Locale.ROOT);
      List<String> list = SpecialDates.CHRISTMAS_RANGE.stream().map(datetimeformatter::format).toList();
      return select(LocalTime.create("MM-dd", "", Optional.empty()), p_389654_, List.of(when(list, p_389525_)));
   }
}
