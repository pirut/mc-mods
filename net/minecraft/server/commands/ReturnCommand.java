package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.ContextChain;
import java.util.List;
import net.minecraft.commands.ExecutionCommandSource;
import net.minecraft.commands.execution.ChainModifiers;
import net.minecraft.commands.execution.CustomCommandExecutor;
import net.minecraft.commands.execution.CustomModifierExecutor;
import net.minecraft.commands.execution.ExecutionControl;
import net.minecraft.commands.execution.Frame;
import net.minecraft.commands.execution.tasks.BuildContexts;
import net.minecraft.commands.execution.tasks.FallthroughTask;

public class ReturnCommand {
   public static <T extends ExecutionCommandSource<T>> void register(CommandDispatcher<T> p_282091_) {
      p_282091_.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)LiteralArgumentBuilder.literal("return")
                     .requires(p_305728_ -> p_305728_.hasPermission(2)))
                  .then(RequiredArgumentBuilder.argument("value", IntegerArgumentType.integer()).executes(new ReturnCommand.ReturnValueCustomExecutor())))
               .then(LiteralArgumentBuilder.literal("fail").executes(new ReturnCommand.ReturnFailCustomExecutor())))
            .then(LiteralArgumentBuilder.literal("run").forward(p_282091_.getRoot(), new ReturnCommand.ReturnFromCommandCustomModifier(), false))
      );
   }

   static class ReturnFailCustomExecutor<T extends ExecutionCommandSource<T>> implements CustomCommandExecutor.CommandAdapter<T> {
      public void run(T p_309679_, ContextChain<T> p_309539_, ChainModifiers p_309649_, ExecutionControl<T> p_309599_) {
         p_309679_.callback().onFailure();
         Frame $$4 = p_309599_.currentFrame();
         $$4.returnFailure();
         $$4.discard();
      }
   }

   static class ReturnFromCommandCustomModifier<T extends ExecutionCommandSource<T>> implements CustomModifierExecutor.ModifierAdapter<T> {
      public void apply(T p_309535_, List<T> p_305925_, ContextChain<T> p_305969_, ChainModifiers p_309676_, ExecutionControl<T> p_305873_) {
         if (p_305925_.isEmpty()) {
            if (p_309676_.isReturn()) {
               p_305873_.queueNext(FallthroughTask.instance());
            }
         } else {
            p_305873_.currentFrame().discard();
            ContextChain<T> $$5 = p_305969_.nextStage();
            String $$6 = $$5.getTopContext().getInput();
            p_305873_.queueNext(new BuildContexts.Continuation<>($$6, $$5, p_309676_.setReturn(), p_309535_, p_305925_));
         }
      }
   }

   static class ReturnValueCustomExecutor<T extends ExecutionCommandSource<T>> implements CustomCommandExecutor.CommandAdapter<T> {
      public void run(T p_309669_, ContextChain<T> p_306161_, ChainModifiers p_309665_, ExecutionControl<T> p_306298_) {
         int $$4 = IntegerArgumentType.getInteger(p_306161_.getTopContext(), "value");
         p_309669_.callback().onSuccess($$4);
         Frame $$5 = p_306298_.currentFrame();
         $$5.returnSuccess($$4);
         $$5.discard();
      }
   }
}
