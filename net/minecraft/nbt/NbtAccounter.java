package net.minecraft.nbt;

import com.google.common.annotations.VisibleForTesting;

public class NbtAccounter {
   private static final int MAX_STACK_DEPTH = 512;
   private final long quota;
   private long usage;
   private final int maxDepth;
   private int depth;

   public NbtAccounter(long p_128922_, int p_302350_) {
      this.quota = p_128922_;
      this.maxDepth = p_302350_;
   }

   public static NbtAccounter create(long p_302395_) {
      return new NbtAccounter(p_302395_, 512);
   }

   public static NbtAccounter unlimitedHeap() {
      return new NbtAccounter(Long.MAX_VALUE, 512);
   }

   public void accountBytes(long p_302490_, long p_302493_) {
      this.accountBytes(p_302490_ * p_302493_);
   }

   public void accountBytes(long p_263515_) {
      if (this.usage + p_263515_ > this.quota) {
         throw new NbtAccounterException(
            "Tried to read NBT tag that was too big; tried to allocate: " + this.usage + " + " + p_263515_ + " bytes where max allowed: " + this.quota
         );
      } else {
         this.usage += p_263515_;
      }
   }

   public void pushDepth() {
      if (this.depth >= this.maxDepth) {
         throw new NbtAccounterException("Tried to read NBT tag with too high complexity, depth > " + this.maxDepth);
      } else {
         this.depth++;
      }
   }

   public void popDepth() {
      if (this.depth <= 0) {
         throw new NbtAccounterException("NBT-Accounter tried to pop stack-depth at top-level");
      } else {
         this.depth--;
      }
   }

   public String readUTF(String data) {
      this.accountBytes(2L);
      if (data == null) {
         return data;
      } else {
         int len = data.length();
         int utflen = 0;

         for (int i = 0; i < len; i++) {
            int c = data.charAt(i);
            if (c >= 1 && c <= 127) {
               utflen++;
            } else if (c > 2047) {
               utflen += 3;
            } else {
               utflen += 2;
            }
         }

         this.accountBytes(utflen);
         return data;
      }
   }

   @VisibleForTesting
   public long getUsage() {
      return this.usage;
   }

   @VisibleForTesting
   public int getDepth() {
      return this.depth;
   }
}
