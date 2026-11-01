package net.minecraft.client.renderer.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperties;
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperty;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class RangeSelectItemModel implements ItemModel {
   private static final int LINEAR_SEARCH_THRESHOLD = 16;
   private final RangeSelectItemModelProperty property;
   private final float scale;
   private final float[] thresholds;
   private final ItemModel[] models;
   private final ItemModel fallback;

   RangeSelectItemModel(RangeSelectItemModelProperty p_387657_, float p_387313_, float[] p_388061_, ItemModel[] p_387161_, ItemModel p_388319_) {
      this.property = p_387657_;
      this.thresholds = p_388061_;
      this.models = p_387161_;
      this.fallback = p_388319_;
      this.scale = p_387313_;
   }

   private static int lastIndexLessOrEqual(float[] p_386461_, float p_386934_) {
      if (p_386461_.length < 16) {
         for (int k = 0; k < p_386461_.length; k++) {
            if (p_386461_[k] > p_386934_) {
               return k - 1;
            }
         }

         return p_386461_.length - 1;
      } else {
         int i = Arrays.binarySearch(p_386461_, p_386934_);
         if (i < 0) {
            int j = ~i;
            return j - 1;
         } else {
            return i;
         }
      }
   }

   @Override
   public void update(
      ItemStackRenderState p_387732_,
      ItemStack p_386891_,
      ItemModelResolver p_388786_,
      ItemDisplayContext p_387570_,
      @Nullable ClientLevel p_388512_,
      @Nullable ItemOwner p_435703_,
      int p_388577_
   ) {
      p_387732_.appendModelIdentityElement(this);
      float f = this.property.get(p_386891_, p_388512_, p_435703_, p_388577_) * this.scale;
      ItemModel itemmodel;
      if (Float.isNaN(f)) {
         itemmodel = this.fallback;
      } else {
         int i = lastIndexLessOrEqual(this.thresholds, f);
         itemmodel = i == -1 ? this.fallback : this.models[i];
      }

      itemmodel.update(p_387732_, p_386891_, p_388786_, p_387570_, p_388512_, p_435703_, p_388577_);
   }

   @OnlyIn(Dist.CLIENT)
   public record Entry(float threshold, ItemModel.Unbaked model) {
      public static final Codec<RangeSelectItemModel.Entry> CODEC = RecordCodecBuilder.create(
         p_388203_ -> p_388203_.group(
               Codec.FLOAT.fieldOf("threshold").forGetter(RangeSelectItemModel.Entry::threshold),
               ItemModels.CODEC.fieldOf("model").forGetter(RangeSelectItemModel.Entry::model)
            )
            .apply(p_388203_, RangeSelectItemModel.Entry::new)
      );
      public static final Comparator<RangeSelectItemModel.Entry> BY_THRESHOLD = Comparator.comparingDouble(RangeSelectItemModel.Entry::threshold);
   }

   @OnlyIn(Dist.CLIENT)
   public record Unbaked(RangeSelectItemModelProperty property, float scale, List<RangeSelectItemModel.Entry> entries, Optional<ItemModel.Unbaked> fallback)
      implements ItemModel.Unbaked {
      public static final MapCodec<RangeSelectItemModel.Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(
         p_387435_ -> p_387435_.group(
               RangeSelectItemModelProperties.MAP_CODEC.forGetter(RangeSelectItemModel.Unbaked::property),
               Codec.FLOAT.optionalFieldOf("scale", 1.0F).forGetter(RangeSelectItemModel.Unbaked::scale),
               RangeSelectItemModel.Entry.CODEC.listOf().fieldOf("entries").forGetter(RangeSelectItemModel.Unbaked::entries),
               ItemModels.CODEC.optionalFieldOf("fallback").forGetter(RangeSelectItemModel.Unbaked::fallback)
            )
            .apply(p_387435_, RangeSelectItemModel.Unbaked::new)
      );

      @Override
      public MapCodec<RangeSelectItemModel.Unbaked> type() {
         return MAP_CODEC;
      }

      @Override
      public ItemModel bake(ItemModel.BakingContext p_388005_) {
         float[] afloat = new float[this.entries.size()];
         ItemModel[] aitemmodel = new ItemModel[this.entries.size()];
         List<RangeSelectItemModel.Entry> list = new ArrayList<>(this.entries);
         list.sort(RangeSelectItemModel.Entry.BY_THRESHOLD);

         for (int i = 0; i < list.size(); i++) {
            RangeSelectItemModel.Entry rangeselectitemmodel$entry = list.get(i);
            afloat[i] = rangeselectitemmodel$entry.threshold;
            aitemmodel[i] = rangeselectitemmodel$entry.model.bake(p_388005_);
         }

         ItemModel itemmodel = this.fallback.<ItemModel>map(p_387030_ -> p_387030_.bake(p_388005_)).orElse(p_388005_.missingItemModel());
         return new RangeSelectItemModel(this.property, this.scale, afloat, aitemmodel, itemmodel);
      }

      @Override
      public void resolveDependencies(ResolvableModel.Resolver p_387826_) {
         this.fallback.ifPresent(p_387900_ -> p_387900_.resolveDependencies(p_387826_));
         this.entries.forEach(p_387986_ -> p_387986_.model.resolveDependencies(p_387826_));
      }
   }
}
