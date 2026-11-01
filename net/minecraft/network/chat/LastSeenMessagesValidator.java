package net.minecraft.network.chat;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import java.util.Optional;
import javax.annotation.Nullable;

public class LastSeenMessagesValidator {
   private final int lastSeenCount;
   private final ObjectList<LastSeenTrackedEntry> trackedMessages = new ObjectArrayList();
   @Nullable
   private MessageSignature lastPendingMessage;

   public LastSeenMessagesValidator(int p_249951_) {
      this.lastSeenCount = p_249951_;

      for (int $$1 = 0; $$1 < p_249951_; $$1++) {
         this.trackedMessages.add(null);
      }
   }

   public void addPending(MessageSignature p_248841_) {
      if (!p_248841_.equals(this.lastPendingMessage)) {
         this.trackedMessages.add(new LastSeenTrackedEntry(p_248841_, true));
         this.lastPendingMessage = p_248841_;
      }
   }

   public int trackedMessagesCount() {
      return this.trackedMessages.size();
   }

   public boolean applyOffset(int p_251273_) {
      int $$1 = this.trackedMessages.size() - this.lastSeenCount;
      if (p_251273_ >= 0 && p_251273_ <= $$1) {
         this.trackedMessages.removeElements(0, p_251273_);
         return true;
      } else {
         return false;
      }
   }

   public Optional<LastSeenMessages> applyUpdate(LastSeenMessages.Update p_248868_) {
      if (!this.applyOffset(p_248868_.offset())) {
         return Optional.empty();
      } else {
         ObjectList<MessageSignature> $$1 = new ObjectArrayList(p_248868_.acknowledged().cardinality());
         if (p_248868_.acknowledged().length() > this.lastSeenCount) {
            return Optional.empty();
         } else {
            for (int $$2 = 0; $$2 < this.lastSeenCount; $$2++) {
               boolean $$3 = p_248868_.acknowledged().get($$2);
               LastSeenTrackedEntry $$4 = (LastSeenTrackedEntry)this.trackedMessages.get($$2);
               if ($$3) {
                  if ($$4 == null) {
                     return Optional.empty();
                  }

                  this.trackedMessages.set($$2, $$4.acknowledge());
                  $$1.add($$4.signature());
               } else {
                  if ($$4 != null && !$$4.pending()) {
                     return Optional.empty();
                  }

                  this.trackedMessages.set($$2, null);
               }
            }

            return Optional.of(new LastSeenMessages($$1));
         }
      }
   }
}
