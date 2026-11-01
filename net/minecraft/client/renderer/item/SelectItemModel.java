package net.minecraft.client.renderer.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.multiplayer.CacheSlot;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.select.SelectItemModelProperties;
import net.minecraft.client.renderer.item.properties.select.SelectItemModelProperty;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.RegistryContextSwapper;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class SelectItemModel<T> implements ItemModel {
   private final SelectItemModelProperty<T> property;
   private final SelectItemModel.ModelSelector<T> models;

   public SelectItemModel(SelectItemModelProperty<T> p_387418_, SelectItemModel.ModelSelector<T> p_400015_) {
      this.property = p_387418_;
      this.models = p_400015_;
   }

   @Override
   public void update(
      ItemStackRenderState p_387349_,
      ItemStack p_386764_,
      ItemModelResolver p_388842_,
      ItemDisplayContext p_386961_,
      @Nullable ClientLevel p_386907_,
      @Nullable ItemOwner p_435803_,
      int p_386608_
   ) {
      p_387349_.appendModelIdentityElement(this);
      T t = this.property.get(p_386764_, p_386907_, p_435803_ == null ? null : p_435803_.asLivingEntity(), p_386608_, p_386961_);
      ItemModel itemmodel = this.models.get(t, p_386907_);
      if (itemmodel != null) {
         itemmodel.update(p_387349_, p_386764_, p_388842_, p_386961_, p_386907_, p_435803_, p_386608_);
      }
   }

   @FunctionalInterface
   @OnlyIn(Dist.CLIENT)
   public interface ModelSelector<T> {
      @Nullable
      ItemModel get(@Nullable T var1, @Nullable ClientLevel var2);
   }

   @OnlyIn(Dist.CLIENT)
   public record SwitchCase<T>(List<T> values, ItemModel.Unbaked model) {
      public static <T> Codec<SelectItemModel.SwitchCase<T>> codec(Codec<T> p_387015_) {
         return RecordCodecBuilder.create(
            p_387815_ -> p_387815_.group(
                  ExtraCodecs.nonEmptyList(ExtraCodecs.compactListCodec(p_387015_)).fieldOf("when").forGetter(SelectItemModel.SwitchCase::values),
                  ItemModels.CODEC.fieldOf("model").forGetter(SelectItemModel.SwitchCase::model)
               )
               .apply(p_387815_, SelectItemModel.SwitchCase::new)
         );
      }
   }

   @OnlyIn(Dist.CLIENT)
   public record Unbaked(SelectItemModel.UnbakedSwitch<?, ?> unbakedSwitch, Optional<ItemModel.Unbaked> fallback) implements ItemModel.Unbaked {
      public static final MapCodec<SelectItemModel.Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(
         p_386872_ -> p_386872_.group(
               SelectItemModel.UnbakedSwitch.MAP_CODEC.forGetter(SelectItemModel.Unbaked::unbakedSwitch),
               ItemModels.CODEC.optionalFieldOf("fallback").forGetter(SelectItemModel.Unbaked::fallback)
            )
            .apply(p_386872_, SelectItemModel.Unbaked::new)
      );

      @Override
      public MapCodec<SelectItemModel.Unbaked> type() {
         return MAP_CODEC;
      }

      @Override
      public ItemModel bake(ItemModel.BakingContext p_388757_) {
         ItemModel itemmodel = this.fallback.<ItemModel>map(p_387332_ -> p_387332_.bake(p_388757_)).orElse(p_388757_.missingItemModel());
         return this.unbakedSwitch.bake(p_388757_, itemmodel);
      }

      @Override
      public void resolveDependencies(ResolvableModel.Resolver p_386548_) {
         this.unbakedSwitch.resolveDependencies(p_386548_);
         this.fallback.ifPresent(p_388441_ -> p_388441_.resolveDependencies(p_386548_));
      }
   }

   @OnlyIn(Dist.CLIENT)
   public record UnbakedSwitch<P extends SelectItemModelProperty<T>, T>(P property, List<SelectItemModel.SwitchCase<T>> cases) {
      public static final MapCodec<SelectItemModel.UnbakedSwitch<?, ?>> MAP_CODEC = SelectItemModelProperties.CODEC
         .dispatchMap("property", p_387573_ -> p_387573_.property().type(), SelectItemModelProperty.Type::switchCodec);

      public ItemModel bake(ItemModel.BakingContext p_386650_, ItemModel p_388617_) {
         Object2ObjectMap<T, ItemModel> object2objectmap = new Object2ObjectOpenHashMap();

         for (SelectItemModel.SwitchCase<T> switchcase : this.cases) {
            ItemModel.Unbaked itemmodel$unbaked = switchcase.model;
            ItemModel itemmodel = itemmodel$unbaked.bake(p_386650_);

            for (T t : switchcase.values) {
               object2objectmap.put(t, itemmodel);
            }
         }

         object2objectmap.defaultReturnValue(p_388617_);
         return new SelectItemModel<>(this.property, this.createModelGetter(object2objectmap, p_386650_.contextSwapper()));
      }

      private SelectItemModel.ModelSelector<T> createModelGetter(Object2ObjectMap<T, ItemModel> p_400045_, @Nullable RegistryContextSwapper p_399763_) {
         if (p_399763_ == null) {
            return (p_399341_, p_399342_) -> (ItemModel)p_400045_.get(p_399341_);
         } else {
            ItemModel itemmodel = (ItemModel)p_400045_.defaultReturnValue();
            CacheSlot<ClientLevel, Object2ObjectMap<T, ItemModel>> cacheslot = new CacheSlot<>(
               p_399349_ -> {
                  Object2ObjectMap<T, ItemModel> object2objectmap = new Object2ObjectOpenHashMap(p_400045_.size());
                  object2objectmap.defaultReturnValue(itemmodel);
                  p_400045_.forEach(
                     (p_432304_, p_432305_) -> p_399763_.swapTo(this.property.valueCodec(), p_432304_, p_399349_.registryAccess())
                        .ifSuccess(p_399345_ -> object2objectmap.put(p_399345_, p_432305_))
                  );
                  return object2objectmap;
               }
            );
            return (p_399333_, p_399334_) -> {
               if (p_399334_ == null) {
                  return (ItemModel)p_400045_.get(p_399333_);
               } else {
                  return p_399333_ == null ? itemmodel : (ItemModel)cacheslot.compute(p_399334_).get(p_399333_);
               }
            };
         }
      }

      public void resolveDependencies(ResolvableModel.Resolver p_388258_) {
         for (SelectItemModel.SwitchCase<?> switchcase : this.cases) {
            switchcase.model.resolveDependencies(p_388258_);
         }
      }
   }
}
