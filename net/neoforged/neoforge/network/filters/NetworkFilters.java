package net.neoforged.neoforge.network.filters;

import com.google.common.collect.ImmutableMap;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelPipeline;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.network.Connection;
import net.neoforged.neoforge.network.connection.ConnectionType;
import net.neoforged.neoforge.network.registration.NetworkRegistry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
public class NetworkFilters {
   private static final Logger LOGGER = LogManager.getLogger();
   private static final Map<String, Function<ConnectionType, DynamicChannelHandler>> instances = ImmutableMap.of(
      "neoforge:vanilla_filter",
      VanillaConnectionNetworkFilter::new,
      "neoforge:splitter",
      (Function<ConnectionType, DynamicChannelHandler>)connectionType -> new GenericPacketSplitter()
   );

   public static void injectIfNecessary(Connection manager) {
      cleanIfNecessary(manager);
      ChannelPipeline pipeline = manager.channel().pipeline();
      if (pipeline.get("encoder") != null) {
         ConnectionType connectionType = NetworkRegistry.getConnectionType(manager);
         instances.forEach((key, filterFactory) -> {
            DynamicChannelHandler filter = filterFactory.apply(connectionType);
            if (filter.isNecessary(manager)) {
               pipeline.addAfter("encoder", key, filter);
               LOGGER.debug("Injected {} into {}", filter, manager);
            }
         });
      }
   }

   public static void cleanIfNecessary(Connection manager) {
      ChannelPipeline pipeline = manager.channel().pipeline();
      List<DynamicChannelHandler> toRemove = pipeline.names()
         .stream()
         .<ChannelHandler>map(pipeline::get)
         .filter(DynamicChannelHandler.class::isInstance)
         .map(DynamicChannelHandler.class::cast)
         .toList();
      toRemove.forEach(pipeline::remove);
   }

   private NetworkFilters() {
   }
}
