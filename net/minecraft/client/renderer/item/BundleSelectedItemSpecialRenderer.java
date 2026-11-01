package net.minecraft.client.renderer.item;

import com.mojang.serialization.MapCodec;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.BundleItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class BundleSelectedItemSpecialRenderer implements ItemModel {
   static final ItemModel INSTANCE = new BundleSelectedItemSpecialRenderer();

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
      ItemStack itemStack = BundleItem.getSelectedItemStack(stack);
      if (!itemStack.isEmpty()) {
         itemModelResolver.appendItemLayers(renderState, itemStack, displayContext, level, owner, seed);
      }
   }

   @Environment(EnvType.CLIENT)
   public record Unbaked() implements ItemModel.Unbaked {
      public static final MapCodec<BundleSelectedItemSpecialRenderer.Unbaked> MAP_CODEC = MapCodec.unit(new BundleSelectedItemSpecialRenderer.Unbaked());

      @Override
      public MapCodec<BundleSelectedItemSpecialRenderer.Unbaked> type() {
         return MAP_CODEC;
      }

      @Override
      public ItemModel bake(ItemModel.BakingContext context) {
         return BundleSelectedItemSpecialRenderer.INSTANCE;
      }

      @Override
      public void resolveDependencies(ResolvableModel.Resolver resolver) {
      }
   }
}
