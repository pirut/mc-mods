package net.minecraft.client.renderer.item;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
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
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class ConditionalItemModel implements ItemModel {
   private final ItemModelPropertyTest property;
   private final ItemModel onTrue;
   private final ItemModel onFalse;

   public ConditionalItemModel(ItemModelPropertyTest property, ItemModel onTrue, ItemModel onFalse) {
      this.property = property;
      this.onTrue = onTrue;
      this.onFalse = onFalse;
   }

   @Override
   public void update(
      ItemStackRenderState renderState,
      ItemStack stack,
      ItemModelResolver itemModelResolver,
      ItemDisplayContext displayContext,
      @Nullable ClientLevel level,
      @Nullable ItemOwner owner,
      int seed
   ) {
      renderState.appendModelIdentityElement(this);
      (this.property.get(stack, level, owner == null ? null : owner.asLivingEntity(), seed, displayContext) ? this.onTrue : this.onFalse)
         .update(renderState, stack, itemModelResolver, displayContext, level, owner, seed);
   }

   @Environment(EnvType.CLIENT)
   public record Unbaked(ConditionalItemModelProperty property, ItemModel.Unbaked onTrue, ItemModel.Unbaked onFalse) implements ItemModel.Unbaked {
      public static final MapCodec<ConditionalItemModel.Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(
         instance -> instance.group(
               ConditionalItemModelProperties.MAP_CODEC.forGetter(ConditionalItemModel.Unbaked::property),
               ItemModels.CODEC.fieldOf("on_true").forGetter(ConditionalItemModel.Unbaked::onTrue),
               ItemModels.CODEC.fieldOf("on_false").forGetter(ConditionalItemModel.Unbaked::onFalse)
            )
            .apply(instance, ConditionalItemModel.Unbaked::new)
      );

      @Override
      public MapCodec<ConditionalItemModel.Unbaked> type() {
         return MAP_CODEC;
      }

      @Override
      public ItemModel bake(ItemModel.BakingContext context) {
         return new ConditionalItemModel(this.adaptProperty(this.property, context.contextSwapper()), this.onTrue.bake(context), this.onFalse.bake(context));
      }

      private ItemModelPropertyTest adaptProperty(ConditionalItemModelProperty property, @Nullable RegistryContextSwapper contextSwapper) {
         if (contextSwapper == null) {
            return property;
         } else {
            CacheSlot<ClientLevel, ItemModelPropertyTest> cacheSlot = new CacheSlot<>(clientLevel -> swapContext(property, contextSwapper, clientLevel));
            return (itemStack, clientLevel, livingEntity, i, itemDisplayContext) -> {
               ItemModelPropertyTest itemModelPropertyTest = (ItemModelPropertyTest)(clientLevel == null ? property : cacheSlot.compute(clientLevel));
               return itemModelPropertyTest.get(itemStack, clientLevel, livingEntity, i, itemDisplayContext);
            };
         }
      }

      private static <T extends ConditionalItemModelProperty> T swapContext(T property, RegistryContextSwapper contextSwapper, ClientLevel level) {
         return (T)contextSwapper.swapTo(property.type().codec(), property, level.registryAccess()).result().orElse(property);
      }

      @Override
      public void resolveDependencies(ResolvableModel.Resolver resolver) {
         this.onTrue.resolveDependencies(resolver);
         this.onFalse.resolveDependencies(resolver);
      }
   }
}
