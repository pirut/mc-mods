package net.neoforged.neoforge.common.world.chunk;

import net.minecraft.server.level.ServerLevel;

@FunctionalInterface
public interface LoadingValidationCallback {
   void validateTickets(ServerLevel var1, TicketHelper var2);
}
