package net.neoforged.neoforge.network.registration;

import io.netty.util.AttributeKey;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.network.Connection;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.connection.ConnectionType;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jspecify.annotations.Nullable;

@Internal
public class ChannelAttributes {
   public static final AttributeKey<NetworkPayloadSetup> PAYLOAD_SETUP = AttributeKey.valueOf("neoforge:payload_setup");
   public static final AttributeKey<Set<Identifier>> ADHOC_CHANNELS = AttributeKey.valueOf("neoforge:adhoc_channels");
   public static final AttributeKey<Map<ConnectionProtocol, Set<Identifier>>> COMMON_CHANNELS = AttributeKey.valueOf("neoforge:common_channels");
   public static final AttributeKey<ConnectionType> CONNECTION_TYPE = AttributeKey.valueOf("neoforge:connection_type");

   @Nullable
   public static NetworkPayloadSetup getPayloadSetup(Connection connection) {
      return (NetworkPayloadSetup)connection.channel().attr(PAYLOAD_SETUP).get();
   }

   public static void setPayloadSetup(Connection connection, NetworkPayloadSetup setup) {
      connection.channel().attr(PAYLOAD_SETUP).set(setup);
   }

   @Nullable
   public static ConnectionType getConnectionType(Connection connection) {
      return (ConnectionType)connection.channel().attr(CONNECTION_TYPE).get();
   }

   public static void setConnectionType(Connection connection, ConnectionType type) {
      connection.channel().attr(CONNECTION_TYPE).set(type);
   }

   public static Set<Identifier> getOrCreateAdHocChannels(Connection connection) {
      Set<Identifier> channels = (Set<Identifier>)connection.channel().attr(ADHOC_CHANNELS).get();
      if (channels == null) {
         channels = new HashSet<>();
         connection.channel().attr(ADHOC_CHANNELS).set(channels);
      }

      return channels;
   }

   public static Set<Identifier> getOrCreateCommonChannels(Connection connection, ConnectionProtocol protocol) {
      Map<ConnectionProtocol, Set<Identifier>> channels = (Map<ConnectionProtocol, Set<Identifier>>)connection.channel().attr(COMMON_CHANNELS).get();
      if (channels == null) {
         channels = new EnumMap<>(ConnectionProtocol.class);
         connection.channel().attr(COMMON_CHANNELS).set(channels);
      }

      return channels.computeIfAbsent(protocol, p -> new HashSet<>());
   }
}
