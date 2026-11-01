package net.minecraft.advancements;

import com.google.common.collect.Lists;
import java.util.List;
import javax.annotation.Nullable;

public class TreeNodePosition {
   private final AdvancementNode node;
   @Nullable
   private final TreeNodePosition parent;
   @Nullable
   private final TreeNodePosition previousSibling;
   private final int childIndex;
   private final List<TreeNodePosition> children = Lists.newArrayList();
   private TreeNodePosition ancestor;
   @Nullable
   private TreeNodePosition thread;
   private int x;
   private float y;
   private float mod;
   private float change;
   private float shift;

   public TreeNodePosition(AdvancementNode p_301306_, @Nullable TreeNodePosition p_16568_, @Nullable TreeNodePosition p_16569_, int p_16570_, int p_16571_) {
      if (p_301306_.advancement().display().isEmpty()) {
         throw new IllegalArgumentException("Can't position an invisible advancement!");
      } else {
         this.node = p_301306_;
         this.parent = p_16568_;
         this.previousSibling = p_16569_;
         this.childIndex = p_16570_;
         this.ancestor = this;
         this.x = p_16571_;
         this.y = -1.0F;
         TreeNodePosition $$5 = null;

         for (AdvancementNode $$6 : p_301306_.children()) {
            $$5 = this.addChild($$6, $$5);
         }
      }
   }

   @Nullable
   private TreeNodePosition addChild(AdvancementNode p_301122_, @Nullable TreeNodePosition p_16591_) {
      if (p_301122_.advancement().display().isPresent()) {
         p_16591_ = new TreeNodePosition(p_301122_, this, p_16591_, this.children.size() + 1, this.x + 1);
         this.children.add(p_16591_);
      } else {
         for (AdvancementNode $$2 : p_301122_.children()) {
            p_16591_ = this.addChild($$2, p_16591_);
         }
      }

      return p_16591_;
   }

   private void firstWalk() {
      if (this.children.isEmpty()) {
         if (this.previousSibling != null) {
            this.y = this.previousSibling.y + 1.0F;
         } else {
            this.y = 0.0F;
         }
      } else {
         TreeNodePosition $$0 = null;

         for (TreeNodePosition $$1 : this.children) {
            $$1.firstWalk();
            $$0 = $$1.apportion($$0 == null ? $$1 : $$0);
         }

         this.executeShifts();
         float $$2 = (this.children.get(0).y + this.children.get(this.children.size() - 1).y) / 2.0F;
         if (this.previousSibling != null) {
            this.y = this.previousSibling.y + 1.0F;
            this.mod = this.y - $$2;
         } else {
            this.y = $$2;
         }
      }
   }

   private float secondWalk(float p_16576_, int p_16577_, float p_16578_) {
      this.y += p_16576_;
      this.x = p_16577_;
      if (this.y < p_16578_) {
         p_16578_ = this.y;
      }

      for (TreeNodePosition $$3 : this.children) {
         p_16578_ = $$3.secondWalk(p_16576_ + this.mod, p_16577_ + 1, p_16578_);
      }

      return p_16578_;
   }

   private void thirdWalk(float p_16574_) {
      this.y += p_16574_;

      for (TreeNodePosition $$1 : this.children) {
         $$1.thirdWalk(p_16574_);
      }
   }

   private void executeShifts() {
      float $$0 = 0.0F;
      float $$1 = 0.0F;

      for (int $$2 = this.children.size() - 1; $$2 >= 0; $$2--) {
         TreeNodePosition $$3 = this.children.get($$2);
         $$3.y += $$0;
         $$3.mod += $$0;
         $$1 += $$3.change;
         $$0 += $$3.shift + $$1;
      }
   }

   @Nullable
   private TreeNodePosition previousOrThread() {
      if (this.thread != null) {
         return this.thread;
      } else {
         return !this.children.isEmpty() ? this.children.get(0) : null;
      }
   }

   @Nullable
   private TreeNodePosition nextOrThread() {
      if (this.thread != null) {
         return this.thread;
      } else {
         return !this.children.isEmpty() ? this.children.get(this.children.size() - 1) : null;
      }
   }

   private TreeNodePosition apportion(TreeNodePosition p_16580_) {
      if (this.previousSibling == null) {
         return p_16580_;
      } else {
         TreeNodePosition $$1 = this;
         TreeNodePosition $$2 = this;
         TreeNodePosition $$3 = this.previousSibling;
         TreeNodePosition $$4 = this.parent.children.get(0);
         float $$5 = this.mod;
         float $$6 = this.mod;
         float $$7 = $$3.mod;

         float $$8;
         for ($$8 = $$4.mod; $$3.nextOrThread() != null && $$1.previousOrThread() != null; $$6 += $$2.mod) {
            $$3 = $$3.nextOrThread();
            $$1 = $$1.previousOrThread();
            $$4 = $$4.previousOrThread();
            $$2 = $$2.nextOrThread();
            $$2.ancestor = this;
            float $$9 = $$3.y + $$7 - ($$1.y + $$5) + 1.0F;
            if ($$9 > 0.0F) {
               $$3.getAncestor(this, p_16580_).moveSubtree(this, $$9);
               $$5 += $$9;
               $$6 += $$9;
            }

            $$7 += $$3.mod;
            $$5 += $$1.mod;
            $$8 += $$4.mod;
         }

         if ($$3.nextOrThread() != null && $$2.nextOrThread() == null) {
            $$2.thread = $$3.nextOrThread();
            $$2.mod += $$7 - $$6;
         } else {
            if ($$1.previousOrThread() != null && $$4.previousOrThread() == null) {
               $$4.thread = $$1.previousOrThread();
               $$4.mod += $$5 - $$8;
            }

            p_16580_ = this;
         }

         return p_16580_;
      }
   }

   private void moveSubtree(TreeNodePosition p_16582_, float p_16583_) {
      float $$2 = p_16582_.childIndex - this.childIndex;
      if ($$2 != 0.0F) {
         p_16582_.change -= p_16583_ / $$2;
         this.change += p_16583_ / $$2;
      }

      p_16582_.shift += p_16583_;
      p_16582_.y += p_16583_;
      p_16582_.mod += p_16583_;
   }

   private TreeNodePosition getAncestor(TreeNodePosition p_16585_, TreeNodePosition p_16586_) {
      return this.ancestor != null && p_16585_.parent.children.contains(this.ancestor) ? this.ancestor : p_16586_;
   }

   private void finalizePosition() {
      this.node.advancement().display().ifPresent(p_300991_ -> p_300991_.setLocation(this.x, this.y));
      if (!this.children.isEmpty()) {
         for (TreeNodePosition $$0 : this.children) {
            $$0.finalizePosition();
         }
      }
   }

   public static void run(AdvancementNode p_301111_) {
      if (p_301111_.advancement().display().isEmpty()) {
         throw new IllegalArgumentException("Can't position children of an invisible root!");
      } else {
         TreeNodePosition $$1 = new TreeNodePosition(p_301111_, null, null, 1, 0);
         $$1.firstWalk();
         float $$2 = $$1.secondWalk(0.0F, 0, $$1.y);
         if ($$2 < 0.0F) {
            $$1.thirdWalk(-$$2);
         }

         $$1.finalizePosition();
      }
   }
}
