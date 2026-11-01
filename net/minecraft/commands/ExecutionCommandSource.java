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
   T withCallback(CommandResultCallback var1);

   CommandResultCallback callback();

   default T clearCallbacks() {
      return this.withCallback(CommandResultCallback.EMPTY);
   }

   CommandDispatcher<T> dispatcher();

   void handleError(CommandExceptionType var1, Message var2, boolean var3, @Nullable TraceCallbacks var4);

   boolean isSilent();

   default void handleError(CommandSyntaxException p_307419_, boolean p_307222_, @Nullable TraceCallbacks p_307604_) {
      this.handleError(p_307419_.getType(), p_307419_.getRawMessage(), p_307222_, p_307604_);
   }

   static <T extends ExecutionCommandSource<T>> ResultConsumer<T> resultConsumer() {
      return (p_309418_, p_309419_, p_309420_) -> ((ExecutionCommandSource)p_309418_.getSource()).callback().onResult(p_309419_, p_309420_);
   }
}
