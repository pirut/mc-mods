package net.neoforged.neoforge.client.extensions.common;

import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.fml.ModLoader;
import net.neoforged.neoforge.data.loading.DatagenModLoader;
import net.neoforged.neoforge.fluids.FluidType;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
public final class ClientExtensionsManager {
   static final Map<Block, IClientBlockExtensions> BLOCK_EXTENSIONS = new Reference2ObjectOpenHashMap();
   static final Map<Item, IClientItemExtensions> ITEM_EXTENSIONS = new Reference2ObjectOpenHashMap();
   static final Map<MobEffect, IClientMobEffectExtensions> MOB_EFFECT_EXTENSIONS = new Reference2ObjectOpenHashMap();
   static final Map<FluidType, IClientFluidTypeExtensions> FLUID_TYPE_EXTENSIONS = new Reference2ObjectOpenHashMap();
   private static boolean initialized = false;

   private ClientExtensionsManager() {
   }

   @SafeVarargs
   static <T, E> void register(E extensions, Map<T, E> target, T... objects) {
      if (objects.length == 0) {
         throw new IllegalArgumentException("At least one target must be provided");
      } else {
         Objects.requireNonNull(extensions, "Extensions must not be null");

         for (T object : objects) {
            Objects.requireNonNull((T)objects, "Target must not be null");
            E oldExtensions = target.put(object, extensions);
            if (oldExtensions != null) {
               throw new IllegalStateException(
                  String.format(Locale.ROOT, "Duplicate client extensions registration for %s (old: %s, new: %s)", object, oldExtensions, extensions)
               );
            }
         }
      }
   }

   public static void init() {
      if (!DatagenModLoader.isRunningDataGen()) {
         if (initialized) {
            throw new IllegalStateException("Duplicate initialization of ClientExtensionsManager");
         } else {
            initialized = true;
            ModLoader.postEvent(new RegisterClientExtensionsEvent());
         }
      }
   }
}
