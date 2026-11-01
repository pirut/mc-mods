package net.minecraft.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.Message;
import com.mojang.brigadier.ResultConsumer;
import com.mojang.brigadier.exceptions.CommandExceptionType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.execution.TraceCallbacks;
import net.minecraft.server.permissions.PermissionSetSupplier;
import org.jspecify.annotations.Nullable;

public interface ExecutionCommandSource<T extends ExecutionCommandSource<T>> extends PermissionSetSupplier {
   T withCallback(CommandResultCallback callback);

   CommandResultCallback callback();

   default T clearCallbacks() {
      return this.withCallback(CommandResultCallback.EMPTY);
   }

   CommandDispatcher<T> dispatcher();

   void handleError(CommandExceptionType exceptionType, Message message, boolean success, @Nullable TraceCallbacks traceCallbacks);

   boolean isSilent();

   default void handleError(CommandSyntaxException exception, boolean success, @Nullable TraceCallbacks traceCallbacks) {
      this.handleError(exception.getType(), exception.getRawMessage(), success, traceCallbacks);
   }

   static <T extends ExecutionCommandSource<T>> ResultConsumer<T> resultConsumer() {
      return (commandContext, bl, i) -> ((ExecutionCommandSource)commandContext.getSource()).callback().onResult(bl, i);
   }
}
