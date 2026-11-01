package net.neoforged.neoforge.common.ticket;

import java.util.Collection;

public interface ITicketGetter<T> extends ITicketManager<T> {
   Collection<SimpleTicket<T>> getTickets();
}
