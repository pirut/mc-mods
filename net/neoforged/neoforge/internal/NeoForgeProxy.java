package net.neoforged.neoforge.internal;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.thread.BlockableEventLoop;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jspecify.annotations.Nullable;

@Internal
public class NeoForgeProxy {
   public static final NeoForgeProxy INSTANCE = instantiate();

   private static NeoForgeProxy instantiate() {
      return switch (FMLEnvironment.getDist()) {
         case CLIENT -> {
            NeoForgeProxy var3;
            try {
               var3 = (NeoForgeProxy)Class.forName("net.neoforged.neoforge.client.internal.NeoForgeClientProxy").getConstructor().newInstance();
            } catch (ReflectiveOperationException var2) {
               throw new RuntimeException("Failed to instantiate client proxy", var2);
            }

            yield var3;
         }
         case DEDICATED_SERVER -> {
            NeoForgeProxy var0 = new NeoForgeProxy();
            yield var0;
         }
         default -> throw new MatchException(null, null);
      };
   }

   public BlockableEventLoop<Runnable> getClientExecutor() {
      throw new UnsupportedOperationException("Cannot access client on the server");
   }

   public TooltipFlag getTooltipFlag() {
      return TooltipFlag.NORMAL;
   }

   @Nullable
   public <T> HolderLookup.RegistryLookup<T> resolveLookup(ResourceKey<? extends Registry<T>> key) {
      MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
      return server != null ? server.registryAccess().lookup(key).orElse(null) : null;
   }
}
