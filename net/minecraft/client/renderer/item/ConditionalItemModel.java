package net.minecraft.client.renderer.item;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.multiplayer.CacheSlot;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.conditional.ConditionalItemModelProperties;
import net.minecraft.client.renderer.item.properties.conditional.ConditionalItemModelProperty;
import net.minecraft.client.renderer.item.properties.conditional.ItemModelPropertyTest;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.util.RegistryContextSwapper;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class ConditionalItemModel implements ItemModel {
   private final ItemModelPropertyTest property;
   private final ItemModel onTrue;
   private final ItemModel onFalse;

   public ConditionalItemModel(ItemModelPropertyTest p_400221_, ItemModel p_387738_, ItemModel p_388649_) {
      this.property = p_400221_;
      this.onTrue = p_387738_;
      this.onFalse = p_388649_;
   }

   @Override
   public void update(
      ItemStackRenderState p_387756_,
      ItemStack p_387286_,
      ItemModelResolver p_386644_,
      ItemDisplayContext p_387754_,
      @Nullable ClientLevel p_388301_,
      @Nullable ItemOwner p_435949_,
      int p_387025_
   ) {
      p_387756_.appendModelIdentityElement(this);
      (this.property.get(p_387286_, p_388301_, p_435949_ == null ? null : p_435949_.asLivingEntity(), p_387025_, p_387754_) ? this.onTrue : this.onFalse)
         .update(p_387756_, p_387286_, p_386644_, p_387754_, p_388301_, p_435949_, p_387025_);
   }

   @OnlyIn(Dist.CLIENT)
   public record Unbaked(ConditionalItemModelProperty property, ItemModel.Unbaked onTrue, ItemModel.Unbaked onFalse) implements ItemModel.Unbaked {
      public static final MapCodec<ConditionalItemModel.Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(
         p_388916_ -> p_388916_.group(
               ConditionalItemModelProperties.MAP_CODEC.forGetter(ConditionalItemModel.Unbaked::property),
               ItemModels.CODEC.fieldOf("on_true").forGetter(ConditionalItemModel.Unbaked::onTrue),
               ItemModels.CODEC.fieldOf("on_false").forGetter(ConditionalItemModel.Unbaked::onFalse)
            )
            .apply(p_388916_, ConditionalItemModel.Unbaked::new)
      );

      @Override
      public MapCodec<ConditionalItemModel.Unbaked> type() {
         return MAP_CODEC;
      }

      @Override
      public ItemModel bake(ItemModel.BakingContext p_388309_) {
         return new ConditionalItemModel(
            this.adaptProperty(this.property, p_388309_.contextSwapper()), this.onTrue.bake(p_388309_), this.onFalse.bake(p_388309_)
         );
      }

      private ItemModelPropertyTest adaptProperty(ConditionalItemModelProperty p_399789_, @Nullable RegistryContextSwapper p_400165_) {
         if (p_400165_ == null) {
            return p_399789_;
         } else {
            CacheSlot<ClientLevel, ItemModelPropertyTest> cacheslot = new CacheSlot<>(p_399329_ -> swapContext(p_399789_, p_400165_, p_399329_));
            return (p_399322_, p_399323_, p_399324_, p_399325_, p_399326_) -> {
               ItemModelPropertyTest itemmodelpropertytest = (ItemModelPropertyTest)(p_399323_ == null ? p_399789_ : cacheslot.compute(p_399323_));
               return itemmodelpropertytest.get(p_399322_, p_399323_, p_399324_, p_399325_, p_399326_);
            };
         }
      }

      private static <T extends ConditionalItemModelProperty> T swapContext(T p_399529_, RegistryContextSwapper p_399879_, ClientLevel p_399861_) {
         return (T)p_399879_.swapTo(p_399529_.type().codec(), p_399529_, p_399861_.registryAccess()).result().orElse(p_399529_);
      }

      @Override
      public void resolveDependencies(ResolvableModel.Resolver p_388796_) {
         this.onTrue.resolveDependencies(p_388796_);
         this.onFalse.resolveDependencies(p_388796_);
      }
   }
}
