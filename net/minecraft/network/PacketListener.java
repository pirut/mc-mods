package net.minecraft.network;

import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.PacketUtils;

public interface PacketListener {
   PacketFlow flow();

   ConnectionProtocol protocol();

   void onDisconnect(DisconnectionDetails var1);

   default void onPacketError(Packet p_341593_, Exception p_341607_) throws ReportedException {
      throw PacketUtils.makeReportedException(p_341607_, p_341593_, this);
   }

   default DisconnectionDetails createDisconnectionInfo(Component p_350812_, Throwable p_351028_) {
      return new DisconnectionDetails(p_350812_);
   }

   boolean isAcceptingMessages();

   default boolean shouldHandleMessage(Packet<?> p_295210_) {
      return this.isAcceptingMessages();
   }

   default void fillCrashReport(CrashReport p_314927_) {
      CrashReportCategory $$1 = p_314927_.addCategory("Connection");
      $$1.setDetail("Protocol", () -> this.protocol().id());
      $$1.setDetail("Flow", () -> this.flow().toString());
      this.fillListenerSpecificCrashDetails(p_314927_, $$1);
   }

   default void fillListenerSpecificCrashDetails(CrashReport p_350278_, CrashReportCategory p_314965_) {
   }
}
