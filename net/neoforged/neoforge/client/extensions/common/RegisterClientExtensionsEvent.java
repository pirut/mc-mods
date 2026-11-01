package net.neoforged.neoforge.client.extensions.common;

import java.util.Arrays;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;
import net.neoforged.neoforge.fluids.FluidType;

public final class RegisterClientExtensionsEvent extends Event implements IModBusEvent {
   RegisterClientExtensionsEvent() {
   }

   public void registerBlock(IClientBlockExtensions extensions, Block... blocks) {
      ClientExtensionsManager.register(extensions, ClientExtensionsManager.BLOCK_EXTENSIONS, blocks);
   }

   @SafeVarargs
   public final void registerBlock(IClientBlockExtensions extensions, Holder<Block>... blocks) {
      this.registerBlock(extensions, Arrays.stream(blocks).map(Holder::value).toArray(Block[]::new));
   }

   public boolean isBlockRegistered(Block block) {
      return ClientExtensionsManager.BLOCK_EXTENSIONS.containsKey(block);
   }

   public void registerItem(IClientItemExtensions extensions, Item... items) {
      ClientExtensionsManager.register(extensions, ClientExtensionsManager.ITEM_EXTENSIONS, items);
   }

   @SafeVarargs
   public final void registerItem(IClientItemExtensions extensions, Holder<Item>... items) {
      this.registerItem(extensions, Arrays.stream(items).map(Holder::value).toArray(Item[]::new));
   }

   public boolean isItemRegistered(Item item) {
      return ClientExtensionsManager.ITEM_EXTENSIONS.containsKey(item);
   }

   public void registerMobEffect(IClientMobEffectExtensions extensions, MobEffect... mobEffects) {
      ClientExtensionsManager.register(extensions, ClientExtensionsManager.MOB_EFFECT_EXTENSIONS, mobEffects);
   }

   @SafeVarargs
   public final void registerMobEffect(IClientMobEffectExtensions extensions, Holder<MobEffect>... mobEffects) {
      this.registerMobEffect(extensions, Arrays.stream(mobEffects).map(Holder::value).toArray(MobEffect[]::new));
   }

   public boolean isMobEffectRegistered(MobEffect mobEffect) {
      return ClientExtensionsManager.MOB_EFFECT_EXTENSIONS.containsKey(mobEffect);
   }

   public void registerFluidType(IClientFluidTypeExtensions extensions, FluidType... fluidTypes) {
      ClientExtensionsManager.register(extensions, ClientExtensionsManager.FLUID_TYPE_EXTENSIONS, fluidTypes);
   }

   @SafeVarargs
   public final void registerFluidType(IClientFluidTypeExtensions extensions, Holder<FluidType>... fluidTypes) {
      this.registerFluidType(extensions, Arrays.stream(fluidTypes).map(Holder::value).toArray(FluidType[]::new));
   }

   public boolean isFluidTypeRegistered(FluidType fluidType) {
      return ClientExtensionsManager.FLUID_TYPE_EXTENSIONS.containsKey(fluidType);
   }
}
