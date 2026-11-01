package net.minecraft.network.chat;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import java.util.BitSet;
import java.util.Objects;
import javax.annotation.Nullable;

public class LastSeenMessagesTracker {
   private final LastSeenTrackedEntry[] trackedMessages;
   private int tail;
   private int offset;
   @Nullable
   private MessageSignature lastTrackedMessage;

   public LastSeenMessagesTracker(int p_242388_) {
      this.trackedMessages = new LastSeenTrackedEntry[p_242388_];
   }

   public boolean addPending(MessageSignature p_248926_, boolean p_250312_) {
      if (Objects.equals(p_248926_, this.lastTrackedMessage)) {
         return false;
      } else {
         this.lastTrackedMessage = p_248926_;
         this.addEntry(p_250312_ ? new LastSeenTrackedEntry(p_248926_, true) : null);
         return true;
      }
   }

   private void addEntry(@Nullable LastSeenTrackedEntry p_250255_) {
      int $$1 = this.tail;
      this.tail = ($$1 + 1) % this.trackedMessages.length;
      this.offset++;
      this.trackedMessages[$$1] = p_250255_;
   }

   public void ignorePending(MessageSignature p_251020_) {
      for (int $$1 = 0; $$1 < this.trackedMessages.length; $$1++) {
         LastSeenTrackedEntry $$2 = this.trackedMessages[$$1];
         if ($$2 != null && $$2.pending() && p_251020_.equals($$2.signature())) {
            this.trackedMessages[$$1] = null;
            break;
         }
      }
   }

   public int getAndClearOffset() {
      int $$0 = this.offset;
      this.offset = 0;
      return $$0;
   }

   public LastSeenMessagesTracker.Update generateAndApplyUpdate() {
      int $$0 = this.getAndClearOffset();
      BitSet $$1 = new BitSet(this.trackedMessages.length);
      ObjectList<MessageSignature> $$2 = new ObjectArrayList(this.trackedMessages.length);

      for (int $$3 = 0; $$3 < this.trackedMessages.length; $$3++) {
         int $$4 = (this.tail + $$3) % this.trackedMessages.length;
         LastSeenTrackedEntry $$5 = this.trackedMessages[$$4];
         if ($$5 != null) {
            $$1.set($$3, true);
            $$2.add($$5.signature());
            this.trackedMessages[$$4] = $$5.acknowledge();
         }
      }

      LastSeenMessages $$6 = new LastSeenMessages($$2);
      LastSeenMessages.Update $$7 = new LastSeenMessages.Update($$0, $$1);
      return new LastSeenMessagesTracker.Update($$6, $$7);
   }

   public int offset() {
      return this.offset;
   }

   public record Update(LastSeenMessages lastSeen, LastSeenMessages.Update update) {
   }
}
