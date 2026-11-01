package net.neoforged.neoforge.network.connection;

import io.netty.channel.ChannelHandlerContext;
import io.netty.util.AttributeKey;
import net.minecraft.network.Connection;
import org.jetbrains.annotations.ApiStatus.Internal;

public class ConnectionUtils {
   private static AttributeKey<Connection> ATTRIBUTE_CONNECTION = AttributeKey.valueOf("neoforge:connection");

   private ConnectionUtils() {
      throw new IllegalStateException("Tried to create utility class!");
   }

   public static Connection getConnection(ChannelHandlerContext connection) {
      return (Connection)connection.attr(ATTRIBUTE_CONNECTION).get();
   }

   @Internal
   public static void setConnection(ChannelHandlerContext connection, Connection value) {
      connection.attr(ATTRIBUTE_CONNECTION).set(value);
   }

   @Internal
   public static void removeConnection(ChannelHandlerContext connection) {
      connection.attr(ATTRIBUTE_CONNECTION).remove();
   }
}
