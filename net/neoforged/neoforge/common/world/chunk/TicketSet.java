package net.neoforged.neoforge.common.world.chunk;

import it.unimi.dsi.fastutil.longs.LongSet;

public record TicketSet(LongSet nonTicking, LongSet ticking) {
}
