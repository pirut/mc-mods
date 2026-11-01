package net.minecraft.commands.execution;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.ContextChain;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.ExecutionCommandSource;
import org.jspecify.annotations.Nullable;

public interface CustomCommandExecutor<T> {
   void run(T source, ContextChain<T> contextChain, ChainModifiers chainModifiers, ExecutionControl<T> executionControl);

   public interface CommandAdapter<T> extends Command<T>, CustomCommandExecutor<T> {
      default int run(CommandContext<T> commandContext) throws CommandSyntaxException {
         throw new UnsupportedOperationException("This function should not run");
      }
   }

   public abstract static class WithErrorHandling<T extends ExecutionCommandSource<T>> implements CustomCommandExecutor<T> {
      public final void run(T executionCommandSource, ContextChain<T> contextChain, ChainModifiers chainModifiers, ExecutionControl<T> executionControl) {
         try {
            this.runGuarded(executionCommandSource, contextChain, chainModifiers, executionControl);
         } catch (CommandSyntaxException var6) {
            this.onError(var6, executionCommandSource, chainModifiers, executionControl.tracer());
            executionCommandSource.callback().onFailure();
         }
      }

      protected void onError(CommandSyntaxException error, T source, ChainModifiers chainModifiers, @Nullable TraceCallbacks traceCallbacks) {
         source.handleError(error, chainModifiers.isForked(), traceCallbacks);
      }

      protected abstract void runGuarded(T source, ContextChain<T> contextChain, ChainModifiers chainModifiers, ExecutionControl<T> executionControl) throws CommandSyntaxException;
   }
}
