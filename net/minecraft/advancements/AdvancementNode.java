package net.minecraft.advancements;

import com.google.common.annotations.VisibleForTesting;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import java.util.Set;
import javax.annotation.Nullable;

public class AdvancementNode {
   private final AdvancementHolder holder;
   @Nullable
   private final AdvancementNode parent;
   private final Set<AdvancementNode> children = new ReferenceOpenHashSet();

   @VisibleForTesting
   public AdvancementNode(AdvancementHolder p_301037_, @Nullable AdvancementNode p_301236_) {
      this.holder = p_301037_;
      this.parent = p_301236_;
   }

   public Advancement advancement() {
      return this.holder.value();
   }

   public AdvancementHolder holder() {
      return this.holder;
   }

   @Nullable
   public AdvancementNode parent() {
      return this.parent;
   }

   public AdvancementNode root() {
      return getRoot(this);
   }

   public static AdvancementNode getRoot(AdvancementNode p_300869_) {
      AdvancementNode $$1 = p_300869_;

      while (true) {
         AdvancementNode $$2 = $$1.parent();
         if ($$2 == null) {
            return $$1;
         }

         $$1 = $$2;
      }
   }

   public Iterable<AdvancementNode> children() {
      return this.children;
   }

   @VisibleForTesting
   public void addChild(AdvancementNode p_301119_) {
      this.children.add(p_301119_);
   }

   @Override
   public boolean equals(Object p_300876_) {
      return this == p_300876_ ? true : p_300876_ instanceof AdvancementNode $$1 && this.holder.equals($$1.holder);
   }

   @Override
   public int hashCode() {
      return this.holder.hashCode();
   }

   @Override
   public String toString() {
      return this.holder.id().toString();
   }
}
