package net.minecraft.network.chat;

import javax.annotation.Nullable;
import net.minecraft.server.level.ServerPlayer;

@FunctionalInterface
public interface ChatDecorator {
   ChatDecorator PLAIN = (p_300715_, p_300716_) -> p_300716_;

   Component decorate(@Nullable ServerPlayer var1, Component var2);
}
