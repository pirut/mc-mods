package net.neoforged.neoforge.common.ticket;

public interface ITicketManager<T> {
   void add(SimpleTicket<T> var1);

   void remove(SimpleTicket<T> var1);
}
