package net.minecraft.server.commands;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.ContextChain;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.datafixers.util.Pair;
import java.util.Collection;
import net.minecraft.commands.CommandResultCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.ExecutionCommandSource;
import net.minecraft.commands.FunctionInstantiationException;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.CompoundTagArgument;
import net.minecraft.commands.arguments.NbtPathArgument;
import net.minecraft.commands.arguments.item.FunctionArgument;
import net.minecraft.commands.execution.ChainModifiers;
import net.minecraft.commands.execution.CustomCommandExecutor;
import net.minecraft.commands.execution.ExecutionControl;
import net.minecraft.commands.execution.tasks.CallFunction;
import net.minecraft.commands.execution.tasks.FallthroughTask;
import net.minecraft.commands.functions.CommandFunction;
import net.minecraft.commands.functions.InstantiatedFunction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.resources.Identifier;
import net.minecraft.server.ServerFunctionManager;
import net.minecraft.server.commands.data.DataAccessor;
import net.minecraft.server.commands.data.DataCommands;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import org.jspecify.annotations.Nullable;

public class FunctionCommand {
   private static final DynamicCommandExceptionType ERROR_ARGUMENT_NOT_COMPOUND = new DynamicCommandExceptionType(
      object -> Component.translatableEscape("commands.function.error.argument_not_compound", object)
   );
   static final DynamicCommandExceptionType ERROR_NO_FUNCTIONS = new DynamicCommandExceptionType(
      object -> Component.translatableEscape("commands.function.scheduled.no_functions", object)
   );
   @VisibleForTesting
   public static final Dynamic2CommandExceptionType ERROR_FUNCTION_INSTANTATION_FAILURE = new Dynamic2CommandExceptionType(
      (object, object2) -> Component.translatableEscape("commands.function.instantiationFailure", object, object2)
   );
   public static final SuggestionProvider<CommandSourceStack> SUGGEST_FUNCTION = (commandContext, suggestionsBuilder) -> {
      ServerFunctionManager serverFunctionManager = ((CommandSourceStack)commandContext.getSource()).getServer().getFunctions();
      SharedSuggestionProvider.suggestResource(serverFunctionManager.getTagNames(), suggestionsBuilder, "#");
      return SharedSuggestionProvider.suggestResource(serverFunctionManager.getFunctionNames(), suggestionsBuilder);
   };
   static final FunctionCommand.Callbacks<CommandSourceStack> FULL_CONTEXT_CALLBACKS = new FunctionCommand.Callbacks<CommandSourceStack>() {
      public void signalResult(CommandSourceStack commandSourceStack, Identifier identifier, int i) {
         commandSourceStack.sendSuccess(() -> Component.translatable("commands.function.result", Component.translationArg(identifier), i), true);
      }
   };

   public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
      LiteralArgumentBuilder<CommandSourceStack> literalArgumentBuilder = Commands.literal("with");

      for (DataCommands.DataProvider dataProvider : DataCommands.SOURCE_PROVIDERS) {
         dataProvider.wrap(literalArgumentBuilder, argumentBuilder -> argumentBuilder.executes(new FunctionCommand.FunctionCustomExecutor() {
            @Override
            protected CompoundTag arguments(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
               return dataProvider.access(context).getData();
            }
         }).then(Commands.argument("path", NbtPathArgument.nbtPath()).executes(new FunctionCommand.FunctionCustomExecutor() {
            @Override
            protected CompoundTag arguments(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
               return FunctionCommand.getArgumentTag(NbtPathArgument.getPath(context, "path"), dataProvider.access(context));
            }
         })));
      }

      dispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("function").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)))
            .then(
               ((RequiredArgumentBuilder)((RequiredArgumentBuilder)Commands.argument("name", FunctionArgument.functions())
                        .suggests(SUGGEST_FUNCTION)
                        .executes(new FunctionCommand.FunctionCustomExecutor() {
                           @Nullable
                           @Override
                           protected CompoundTag arguments(CommandContext<CommandSourceStack> context) {
                              return null;
                           }
                        }))
                     .then(Commands.argument("arguments", CompoundTagArgument.compoundTag()).executes(new FunctionCommand.FunctionCustomExecutor() {
                        @Override
                        protected CompoundTag arguments(CommandContext<CommandSourceStack> context) {
                           return CompoundTagArgument.getCompoundTag(context, "arguments");
                        }
                     })))
                  .then(literalArgumentBuilder)
            )
      );
   }

   static CompoundTag getArgumentTag(NbtPathArgument.NbtPath nbtPath, DataAccessor dataAccessor) throws CommandSyntaxException {
      Tag tag = DataCommands.getSingleTag(nbtPath, dataAccessor);
      if (tag instanceof CompoundTag compoundTag) {
         return compoundTag;
      } else {
         throw ERROR_ARGUMENT_NOT_COMPOUND.create(tag.getType().getName());
      }
   }

   public static CommandSourceStack modifySenderForExecution(CommandSourceStack source) {
      return source.withSuppressedOutput().withMaximumPermission(LevelBasedPermissionSet.GAMEMASTER);
   }

   public static <T extends ExecutionCommandSource<T>> void queueFunctions(
      Collection<CommandFunction<T>> functions,
      @Nullable CompoundTag arguments,
      T originalSource,
      T source,
      ExecutionControl<T> executionControl,
      FunctionCommand.Callbacks<T> callbacks,
      ChainModifiers chainModifiers
   ) throws CommandSyntaxException {
      if (chainModifiers.isReturn()) {
         queueFunctionsAsReturn(functions, arguments, originalSource, source, executionControl, callbacks);
      } else {
         queueFunctionsNoReturn(functions, arguments, originalSource, source, executionControl, callbacks);
      }
   }

   private static <T extends ExecutionCommandSource<T>> void instantiateAndQueueFunctions(
      @Nullable CompoundTag arguments,
      ExecutionControl<T> executionControl,
      CommandDispatcher<T> dispatcher,
      T source,
      CommandFunction<T> function,
      Identifier functionId,
      CommandResultCallback resultCallback,
      boolean returnParentFrame
   ) throws CommandSyntaxException {
      try {
         InstantiatedFunction<T> instantiatedFunction = function.instantiate(arguments, dispatcher);
         executionControl.queueNext(new CallFunction<>(instantiatedFunction, resultCallback, returnParentFrame).bind(source));
      } catch (FunctionInstantiationException var9) {
         throw ERROR_FUNCTION_INSTANTATION_FAILURE.create(functionId, var9.messageComponent());
      }
   }

   private static <T extends ExecutionCommandSource<T>> CommandResultCallback decorateOutputIfNeeded(
      T source, FunctionCommand.Callbacks<T> callbacks, Identifier function, CommandResultCallback resultCallback
   ) {
      return source.isSilent() ? resultCallback : (bl, i) -> {
         callbacks.signalResult(source, function, i);
         resultCallback.onResult(bl, i);
      };
   }

   private static <T extends ExecutionCommandSource<T>> void queueFunctionsAsReturn(
      Collection<CommandFunction<T>> functions,
      @Nullable CompoundTag arguments,
      T originalSource,
      T source,
      ExecutionControl<T> executionControl,
      FunctionCommand.Callbacks<T> callbacks
   ) throws CommandSyntaxException {
      CommandDispatcher<T> commandDispatcher = originalSource.dispatcher();
      T executionCommandSource = source.clearCallbacks();
      CommandResultCallback commandResultCallback = CommandResultCallback.chain(
         originalSource.callback(), executionControl.currentFrame().returnValueConsumer()
      );

      for (CommandFunction<T> commandFunction : functions) {
         Identifier identifier = commandFunction.id();
         CommandResultCallback commandResultCallback2 = decorateOutputIfNeeded(originalSource, callbacks, identifier, commandResultCallback);
         instantiateAndQueueFunctions(
            arguments, executionControl, commandDispatcher, executionCommandSource, commandFunction, identifier, commandResultCallback2, true
         );
      }

      executionControl.queueNext(FallthroughTask.instance());
   }

   private static <T extends ExecutionCommandSource<T>> void queueFunctionsNoReturn(
      Collection<CommandFunction<T>> functions,
      @Nullable CompoundTag arguments,
      T originalSource,
      T source,
      ExecutionControl<T> executionControl,
      FunctionCommand.Callbacks<T> callbacks
   ) throws CommandSyntaxException {
      CommandDispatcher<T> commandDispatcher = originalSource.dispatcher();
      T executionCommandSource = source.clearCallbacks();
      CommandResultCallback commandResultCallback = originalSource.callback();
      if (!functions.isEmpty()) {
         if (functions.size() == 1) {
            CommandFunction<T> commandFunction = functions.iterator().next();
            Identifier identifier = commandFunction.id();
            CommandResultCallback commandResultCallback2 = decorateOutputIfNeeded(originalSource, callbacks, identifier, commandResultCallback);
            instantiateAndQueueFunctions(
               arguments, executionControl, commandDispatcher, executionCommandSource, commandFunction, identifier, commandResultCallback2, false
            );
         } else if (commandResultCallback == CommandResultCallback.EMPTY) {
            for (CommandFunction<T> commandFunction2 : functions) {
               Identifier identifier2 = commandFunction2.id();
               CommandResultCallback commandResultCallback3 = decorateOutputIfNeeded(originalSource, callbacks, identifier2, commandResultCallback);
               instantiateAndQueueFunctions(
                  arguments, executionControl, commandDispatcher, executionCommandSource, commandFunction2, identifier2, commandResultCallback3, false
               );
            }
         } else {
            class Accumulator {
               boolean anyResult;
               int sum;

               public void add(int result) {
                  this.anyResult = true;
                  this.sum += result;
               }
            }

            Accumulator lv = new Accumulator();
            CommandResultCallback commandResultCallback4 = (bl, i) -> lv.add(i);

            for (CommandFunction<T> commandFunction3 : functions) {
               Identifier identifier3 = commandFunction3.id();
               CommandResultCallback commandResultCallback5 = decorateOutputIfNeeded(originalSource, callbacks, identifier3, commandResultCallback4);
               instantiateAndQueueFunctions(
                  arguments, executionControl, commandDispatcher, executionCommandSource, commandFunction3, identifier3, commandResultCallback5, false
               );
            }

            executionControl.queueNext((executionContext, frame) -> {
               if (lv.anyResult) {
                  commandResultCallback.onSuccess(lv.sum);
               }
            });
         }
      }
   }

   public interface Callbacks<T> {
      void signalResult(T source, Identifier function, int commands);
   }

   abstract static class FunctionCustomExecutor
      extends CustomCommandExecutor.WithErrorHandling<CommandSourceStack>
      implements CustomCommandExecutor.CommandAdapter<CommandSourceStack> {
      @Nullable
      protected abstract CompoundTag arguments(CommandContext<CommandSourceStack> context) throws CommandSyntaxException;

      public void runGuarded(
         CommandSourceStack commandSourceStack,
         ContextChain<CommandSourceStack> contextChain,
         ChainModifiers chainModifiers,
         ExecutionControl<CommandSourceStack> executionControl
      ) throws CommandSyntaxException {
         CommandContext<CommandSourceStack> commandContext = contextChain.getTopContext().copyFor(commandSourceStack);
         Pair<Identifier, Collection<CommandFunction<CommandSourceStack>>> pair = FunctionArgument.getFunctionCollection(commandContext, "name");
         Collection<CommandFunction<CommandSourceStack>> collection = (Collection<CommandFunction<CommandSourceStack>>)pair.getSecond();
         if (collection.isEmpty()) {
            throw FunctionCommand.ERROR_NO_FUNCTIONS.create(Component.translationArg((Identifier)pair.getFirst()));
         } else {
            CompoundTag compoundTag = this.arguments(commandContext);
            CommandSourceStack commandSourceStack2 = FunctionCommand.modifySenderForExecution(commandSourceStack);
            if (collection.size() == 1) {
               commandSourceStack.sendSuccess(
                  () -> Component.translatable("commands.function.scheduled.single", Component.translationArg(collection.iterator().next().id())), true
               );
            } else {
               commandSourceStack.sendSuccess(
                  () -> Component.translatable(
                     "commands.function.scheduled.multiple",
                     ComponentUtils.formatList(collection.stream().map(CommandFunction::id).toList(), Component::translationArg)
                  ),
                  true
               );
            }

            FunctionCommand.queueFunctions(
               collection, compoundTag, commandSourceStack, commandSourceStack2, executionControl, FunctionCommand.FULL_CONTEXT_CALLBACKS, chainModifiers
            );
         }
      }
   }
}
