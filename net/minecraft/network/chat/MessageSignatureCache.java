package net.minecraft.network.chat;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

public class MessageSignatureCache {
   public static final int NOT_FOUND = -1;
   private static final int DEFAULT_CAPACITY = 128;
   private final MessageSignature[] entries;

   public MessageSignatureCache(int p_250894_) {
      this.entries = new MessageSignature[p_250894_];
   }

   public static MessageSignatureCache createDefault() {
      return new MessageSignatureCache(128);
   }

   public int pack(MessageSignature p_254157_) {
      for (int $$1 = 0; $$1 < this.entries.length; $$1++) {
         if (p_254157_.equals(this.entries[$$1])) {
            return $$1;
         }
      }

      return -1;
   }

   @Nullable
   public MessageSignature unpack(int p_253967_) {
      return this.entries[p_253967_];
   }

   public void push(SignedMessageBody p_314937_, @Nullable MessageSignature p_314936_) {
      List<MessageSignature> $$2 = p_314937_.lastSeen().entries();
      ArrayDeque<MessageSignature> $$3 = new ArrayDeque<>($$2.size() + 1);
      $$3.addAll($$2);
      if (p_314936_ != null) {
         $$3.add(p_314936_);
      }

      this.push($$3);
   }

   @VisibleForTesting
   void push(List<MessageSignature> p_248560_) {
      this.push(new ArrayDeque<>(p_248560_));
   }

   private void push(ArrayDeque<MessageSignature> p_251419_) {
      Set<MessageSignature> $$1 = new ObjectOpenHashSet(p_251419_);

      for (int $$2 = 0; !p_251419_.isEmpty() && $$2 < this.entries.length; $$2++) {
         MessageSignature $$3 = this.entries[$$2];
         this.entries[$$2] = p_251419_.removeLast();
         if ($$3 != null && !$$1.contains($$3)) {
            p_251419_.addFirst($$3);
         }
      }
   }
}
