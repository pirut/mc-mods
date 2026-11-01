package net.neoforged.neoforge.network.payload;

import com.mojang.logging.LogUtils;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.slf4j.Logger;

@Internal
public final class DinnerboneProtocolUtils {
   public static final Logger LOGGER = LogUtils.getLogger();
   public static final StreamCodec<FriendlyByteBuf, Set<ResourceLocation>> CHANNELS_CODEC = StreamCodec.of(
      DinnerboneProtocolUtils::writeChannels, DinnerboneProtocolUtils::readChannels
   );

   private DinnerboneProtocolUtils() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }

   private static Set<ResourceLocation> readChannels(FriendlyByteBuf buf) {
      StringBuilder builder = new StringBuilder();
      Set<ResourceLocation> channels = new HashSet<>();

      while (buf.isReadable()) {
         char c = (char)buf.readByte();
         if (c == 0) {
            parseAndAddChannel(builder, channels);
         } else {
            builder.append(c);
         }
      }

      parseAndAddChannel(builder, channels);
      return channels;
   }

   private static void parseAndAddChannel(StringBuilder builder, Set<ResourceLocation> channels) {
      if (!builder.isEmpty()) {
         String channel = builder.toString();

         try {
            channels.add(ResourceLocation.parse(channel));
         } catch (Exception var7) {
            LOGGER.error("Invalid channel: {}", channel, var7);
         } finally {
            builder.setLength(0);
         }
      }
   }

   private static void writeChannels(FriendlyByteBuf buf, Set<ResourceLocation> channels) {
      for (ResourceLocation channel : channels) {
         for (char c : channel.toString().toCharArray()) {
            buf.writeByte(c);
         }

         buf.writeByte(0);
      }
   }
}
