package net.minecraft.server.network;

import io.netty.buffer.ByteBuf;
import java.nio.charset.StandardCharsets;

public class LegacyProtocolUtils {
   public static final int CUSTOM_PAYLOAD_PACKET_ID = 250;
   public static final String CUSTOM_PAYLOAD_PACKET_PING_CHANNEL = "MC|PingHost";
   public static final int GET_INFO_PACKET_ID = 254;
   public static final int GET_INFO_PACKET_VERSION_1 = 1;
   public static final int DISCONNECT_PACKET_ID = 255;
   public static final int FAKE_PROTOCOL_VERSION = 127;

   public static void writeLegacyString(ByteBuf p_295942_, String p_294348_) {
      p_295942_.writeShort(p_294348_.length());
      p_295942_.writeCharSequence(p_294348_, StandardCharsets.UTF_16BE);
   }

   public static String readLegacyString(ByteBuf p_295603_) {
      int $$1 = p_295603_.readShort();
      int $$2 = $$1 * 2;
      String $$3 = p_295603_.toString(p_295603_.readerIndex(), $$2, StandardCharsets.UTF_16BE);
      p_295603_.skipBytes($$2);
      return $$3;
   }
}
