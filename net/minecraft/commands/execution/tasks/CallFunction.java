package net.minecraft.commands.execution.tasks;

import java.util.List;
import net.minecraft.commands.CommandResultCallback;
import net.minecraft.commands.ExecutionCommandSource;
import net.minecraft.commands.execution.CommandQueueEntry;
import net.minecraft.commands.execution.ExecutionContext;
import net.minecraft.commands.execution.Frame;
import net.minecraft.commands.execution.TraceCallbacks;
import net.minecraft.commands.execution.UnboundEntryAction;
import net.minecraft.commands.functions.InstantiatedFunction;

public class CallFunction<T extends ExecutionCommandSource<T>> implements UnboundEntryAction<T> {
   private final InstantiatedFunction<T> function;
   private final CommandResultCallback resultCallback;
   private final boolean returnParentFrame;

   public CallFunction(InstantiatedFunction<T> p_306069_, CommandResultCallback p_309620_, boolean p_309704_) {
      this.function = p_306069_;
      this.resultCallback = p_309620_;
      this.returnParentFrame = p_309704_;
   }

   public void execute(T p_309553_, ExecutionContext<T> p_305909_, Frame p_309718_) {
      p_305909_.incrementCost();
      List<UnboundEntryAction<T>> $$3 = this.function.entries();
      TraceCallbacks $$4 = p_305909_.tracer();
      if ($$4 != null) {
         $$4.onCall(p_309718_.depth(), this.function.id(), this.function.entries().size());
      }

      int $$5 = p_309718_.depth() + 1;
      Frame.FrameControl $$6 = this.returnParentFrame ? p_309718_.frameControl() : p_305909_.frameControlForDepth($$5);
      Frame $$7 = new Frame($$5, this.resultCallback, $$6);
      ContinuationTask.schedule(p_305909_, $$7, $$3, (p_309431_, p_309432_) -> new CommandQueueEntry<>(p_309431_, p_309432_.bind(p_309553_)));
   }
}
