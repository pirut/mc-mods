package net.neoforged.neoforge.client;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.UnmodifiableIterator;
import com.google.common.collect.ImmutableList.Builder;
import java.util.Map;
import net.minecraft.client.color.block.BlockTintCache;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.ColorResolver;
import net.neoforged.fml.ModLoader;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import org.jetbrains.annotations.ApiStatus.Internal;

public final class ColorResolverManager {
   private static ImmutableList<ColorResolver> colorResolvers;

   @Internal
   public static void init() {
      Builder<ColorResolver> builder = ImmutableList.builder();
      ModLoader.postEvent(new RegisterColorHandlersEvent.ColorResolvers(builder));
      colorResolvers = builder.build();
   }

   public static ImmutableList<ColorResolver> getRegisteredResolvers() {
      return colorResolvers;
   }

   public static void registerBlockTintCaches(ClientLevel level, Map<ColorResolver, BlockTintCache> target) {
      UnmodifiableIterator var2 = colorResolvers.iterator();

      while (var2.hasNext()) {
         ColorResolver resolver = (ColorResolver)var2.next();
         target.put(resolver, new BlockTintCache(pos -> level.calculateBlockTint(pos, resolver)));
      }
   }

   private ColorResolverManager() {
   }
}
