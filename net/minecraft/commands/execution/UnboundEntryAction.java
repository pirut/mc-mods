package net.minecraft.commands.execution;

@FunctionalInterface
public interface UnboundEntryAction<T> {
   void execute(T var1, ExecutionContext<T> var2, Frame var3);

   default EntryAction<T> bind(T p_306075_) {
      return (p_309422_, p_309423_) -> this.execute(p_306075_, p_309422_, p_309423_);
   }
}
