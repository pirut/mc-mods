package net.minecraft.nbt.visitors;

import java.util.ArrayDeque;
import java.util.Deque;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.StreamTagVisitor;
import net.minecraft.nbt.TagType;

public class SkipFields extends CollectToTag {
   private final Deque<FieldTree> stack = new ArrayDeque<>();

   public SkipFields(FieldSelector... p_202549_) {
      FieldTree $$1 = FieldTree.createRoot();

      for (FieldSelector $$2 : p_202549_) {
         $$1.addEntry($$2);
      }

      this.stack.push($$1);
   }

   @Override
   public StreamTagVisitor.EntryResult visitEntry(TagType<?> p_202551_, String p_202552_) {
      FieldTree $$2 = this.stack.element();
      if ($$2.isSelected(p_202551_, p_202552_)) {
         return StreamTagVisitor.EntryResult.SKIP;
      } else {
         if (p_202551_ == CompoundTag.TYPE) {
            FieldTree $$3 = $$2.fieldsToRecurse().get(p_202552_);
            if ($$3 != null) {
               this.stack.push($$3);
            }
         }

         return super.visitEntry(p_202551_, p_202552_);
      }
   }

   @Override
   public StreamTagVisitor.ValueResult visitContainerEnd() {
      if (this.depth() == this.stack.element().depth()) {
         this.stack.pop();
      }

      return super.visitContainerEnd();
   }
}
