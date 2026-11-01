package net.minecraft.client.multiplayer.chat.report;

import it.unimi.dsi.fastutil.ints.IntCollection;
import it.unimi.dsi.fastutil.ints.IntRBTreeSet;
import it.unimi.dsi.fastutil.ints.IntSortedSet;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import net.minecraft.client.multiplayer.chat.ChatLog;
import net.minecraft.client.multiplayer.chat.LoggedChatMessage;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.network.chat.PlayerChatMessage;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ChatReportContextBuilder {
   final int leadingCount;
   private final List<ChatReportContextBuilder.Collector> activeCollectors = new ArrayList<>();

   public ChatReportContextBuilder(int p_252198_) {
      this.leadingCount = p_252198_;
   }

   public void collectAllContext(ChatLog p_249467_, IntCollection p_250295_, ChatReportContextBuilder.Handler p_251946_) {
      IntSortedSet $$3 = new IntRBTreeSet(p_250295_);

      for (int $$4 = $$3.lastInt(); $$4 >= p_249467_.start() && (this.isActive() || !$$3.isEmpty()); $$4--) {
         if (p_249467_.lookup($$4) instanceof LoggedChatMessage.Player $$5) {
            boolean $$6 = this.acceptContext($$5.message());
            if ($$3.remove($$4)) {
               this.trackContext($$5.message());
               p_251946_.accept($$4, $$5);
            } else if ($$6) {
               p_251946_.accept($$4, $$5);
            }
         }
      }
   }

   public void trackContext(PlayerChatMessage p_252057_) {
      this.activeCollectors.add(new ChatReportContextBuilder.Collector(p_252057_));
   }

   public boolean acceptContext(PlayerChatMessage p_250059_) {
      boolean $$1 = false;
      Iterator<ChatReportContextBuilder.Collector> $$2 = this.activeCollectors.iterator();

      while ($$2.hasNext()) {
         ChatReportContextBuilder.Collector $$3 = $$2.next();
         if ($$3.accept(p_250059_)) {
            $$1 = true;
            if ($$3.isComplete()) {
               $$2.remove();
            }
         }
      }

      return $$1;
   }

   public boolean isActive() {
      return !this.activeCollectors.isEmpty();
   }

   @OnlyIn(Dist.CLIENT)
   class Collector {
      private final Set<MessageSignature> lastSeenSignatures;
      private PlayerChatMessage lastChainMessage;
      private boolean collectingChain = true;
      private int count;

      Collector(PlayerChatMessage p_249708_) {
         this.lastSeenSignatures = new ObjectOpenHashSet(p_249708_.signedBody().lastSeen().entries());
         this.lastChainMessage = p_249708_;
      }

      boolean accept(PlayerChatMessage p_252313_) {
         if (p_252313_.equals(this.lastChainMessage)) {
            return false;
         } else {
            boolean $$1 = this.lastSeenSignatures.remove(p_252313_.signature());
            if (this.collectingChain && this.lastChainMessage.sender().equals(p_252313_.sender())) {
               if (this.lastChainMessage.link().isDescendantOf(p_252313_.link())) {
                  $$1 = true;
                  this.lastChainMessage = p_252313_;
               } else {
                  this.collectingChain = false;
               }
            }

            if ($$1) {
               this.count++;
            }

            return $$1;
         }
      }

      boolean isComplete() {
         return this.count >= ChatReportContextBuilder.this.leadingCount || !this.collectingChain && this.lastSeenSignatures.isEmpty();
      }
   }

   @OnlyIn(Dist.CLIENT)
   public interface Handler {
      void accept(int var1, LoggedChatMessage.Player var2);
   }
}
