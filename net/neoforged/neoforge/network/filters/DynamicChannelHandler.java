package net.neoforged.neoforge.network.filters;

import io.netty.channel.ChannelHandler;
import net.minecraft.network.Connection;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
public interface DynamicChannelHandler extends ChannelHandler {
   boolean isNecessary(Connection var1);
}
