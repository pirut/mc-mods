package net.minecraft.util.debugchart;

import net.minecraft.network.protocol.game.ClientboundDebugSamplePacket;
import net.minecraft.util.debug.ServerDebugSubscribers;

public class RemoteSampleLogger extends AbstractSampleLogger {
   private final ServerDebugSubscribers subscribers;
   private final RemoteDebugSampleType sampleType;

   public RemoteSampleLogger(int p_324142_, ServerDebugSubscribers p_449608_, RemoteDebugSampleType p_324327_) {
      this(p_324142_, p_449608_, p_324327_, new long[p_324142_]);
   }

   public RemoteSampleLogger(int p_324307_, ServerDebugSubscribers p_449487_, RemoteDebugSampleType p_323476_, long[] p_323539_) {
      super(p_324307_, p_323539_);
      this.subscribers = p_449487_;
      this.sampleType = p_323476_;
   }

   @Override
   protected void useSample() {
      if (this.subscribers.hasAnySubscriberFor(this.sampleType.subscription())) {
         this.subscribers.broadcastToAll(this.sampleType.subscription(), new ClientboundDebugSamplePacket((long[])this.sample.clone(), this.sampleType));
      }
   }
}
