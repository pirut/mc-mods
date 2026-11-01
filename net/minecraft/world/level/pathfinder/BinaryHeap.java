package net.minecraft.world.level.pathfinder;

import java.util.Arrays;

public class BinaryHeap {
   private Node[] heap = new Node[128];
   private int size;

   public Node insert(Node p_77085_) {
      if (p_77085_.heapIdx >= 0) {
         throw new IllegalStateException("OW KNOWS!");
      } else {
         if (this.size == this.heap.length) {
            Node[] $$1 = new Node[this.size << 1];
            System.arraycopy(this.heap, 0, $$1, 0, this.size);
            this.heap = $$1;
         }

         this.heap[this.size] = p_77085_;
         p_77085_.heapIdx = this.size;
         this.upHeap(this.size++);
         return p_77085_;
      }
   }

   public void clear() {
      this.size = 0;
   }

   public Node peek() {
      return this.heap[0];
   }

   public Node pop() {
      Node $$0 = this.heap[0];
      this.heap[0] = this.heap[--this.size];
      this.heap[this.size] = null;
      if (this.size > 0) {
         this.downHeap(0);
      }

      $$0.heapIdx = -1;
      return $$0;
   }

   public void remove(Node p_164682_) {
      this.heap[p_164682_.heapIdx] = this.heap[--this.size];
      this.heap[this.size] = null;
      if (this.size > p_164682_.heapIdx) {
         if (this.heap[p_164682_.heapIdx].f < p_164682_.f) {
            this.upHeap(p_164682_.heapIdx);
         } else {
            this.downHeap(p_164682_.heapIdx);
         }
      }

      p_164682_.heapIdx = -1;
   }

   public void changeCost(Node p_77087_, float p_77088_) {
      float $$2 = p_77087_.f;
      p_77087_.f = p_77088_;
      if (p_77088_ < $$2) {
         this.upHeap(p_77087_.heapIdx);
      } else {
         this.downHeap(p_77087_.heapIdx);
      }
   }

   public int size() {
      return this.size;
   }

   private void upHeap(int p_77083_) {
      Node $$1 = this.heap[p_77083_];
      float $$2 = $$1.f;

      while (p_77083_ > 0) {
         int $$3 = p_77083_ - 1 >> 1;
         Node $$4 = this.heap[$$3];
         if (!($$2 < $$4.f)) {
            break;
         }

         this.heap[p_77083_] = $$4;
         $$4.heapIdx = p_77083_;
         p_77083_ = $$3;
      }

      this.heap[p_77083_] = $$1;
      $$1.heapIdx = p_77083_;
   }

   private void downHeap(int p_77090_) {
      Node $$1 = this.heap[p_77090_];
      float $$2 = $$1.f;

      while (true) {
         int $$3 = 1 + (p_77090_ << 1);
         int $$4 = $$3 + 1;
         if ($$3 >= this.size) {
            break;
         }

         Node $$5 = this.heap[$$3];
         float $$6 = $$5.f;
         Node $$7;
         float $$8;
         if ($$4 >= this.size) {
            $$7 = null;
            $$8 = Float.POSITIVE_INFINITY;
         } else {
            $$7 = this.heap[$$4];
            $$8 = $$7.f;
         }

         if ($$6 < $$8) {
            if (!($$6 < $$2)) {
               break;
            }

            this.heap[p_77090_] = $$5;
            $$5.heapIdx = p_77090_;
            p_77090_ = $$3;
         } else {
            if (!($$8 < $$2)) {
               break;
            }

            this.heap[p_77090_] = $$7;
            $$7.heapIdx = p_77090_;
            p_77090_ = $$4;
         }
      }

      this.heap[p_77090_] = $$1;
      $$1.heapIdx = p_77090_;
   }

   public boolean isEmpty() {
      return this.size == 0;
   }

   public Node[] getHeap() {
      return Arrays.copyOf(this.heap, this.size);
   }
}
