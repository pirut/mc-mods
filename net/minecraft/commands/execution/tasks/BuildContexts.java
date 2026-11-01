package net.minecraft.commands.execution.tasks;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.brigadier.RedirectModifier;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.ContextChain;
import com.mojang.brigadier.context.ContextChain.Stage;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.Collection;
import java.util.List;
import net.minecraft.commands.CommandResultCallback;
import net.minecraft.commands.ExecutionCommandSource;
import net.minecraft.commands.execution.ChainModifiers;
import net.minecraft.commands.execution.CommandQueueEntry;
import net.minecraft.commands.execution.CustomCommandExecutor;
import net.minecraft.commands.execution.CustomModifierExecutor;
import net.minecraft.commands.execution.EntryAction;
import net.minecraft.commands.execution.ExecutionContext;
import net.minecraft.commands.execution.ExecutionControl;
import net.minecraft.commands.execution.Frame;
import net.minecraft.commands.execution.TraceCallbacks;
import net.minecraft.commands.execution.UnboundEntryAction;
import net.minecraft.network.chat.Component;

public class BuildContexts<T extends ExecutionCommandSource<T>> {
   @VisibleForTesting
   public static final DynamicCommandExceptionType ERROR_FORK_LIMIT_REACHED = new DynamicCommandExceptionType(
      p_306063_ -> Component.translatableEscape("command.forkLimit", p_306063_)
   );
   private final String commandInput;
   private final ContextChain<T> command;

   public BuildContexts(String p_306157_, ContextChain<T> p_305974_) {
      this.commandInput = p_306157_;
      this.command = p_305974_;
   }

   protected void execute(T p_309567_, List<T> p_306303_, ExecutionContext<T> p_305977_, Frame p_309603_, ChainModifiers p_309659_) {
      ContextChain<T> $$5 = this.command;
      ChainModifiers $$6 = p_309659_;
      List<T> $$7 = p_306303_;
      if ($$5.getStage() != Stage.EXECUTE) {
         p_305977_.profiler().push(() -> "prepare " + this.commandInput);

         try {
            for (int $$8 = p_305977_.forkLimit(); $$5.getStage() != Stage.EXECUTE; $$5 = $$5.nextStage()) {
               CommandContext<T> $$9 = $$5.getTopContext();
               if ($$9.isForked()) {
                  $$6 = $$6.setForked();
               }

               RedirectModifier<T> $$10 = $$9.getRedirectModifier();
               if ($$10 instanceof CustomModifierExecutor<T> $$11) {
                  $$11.apply(p_309567_, $$7, $$5, $$6, ExecutionControl.create(p_305977_, p_309603_));
                  return;
               }

               if ($$10 != null) {
                  p_305977_.incrementCost();
                  boolean $$12 = $$6.isForked();
                  List<T> $$13 = new ObjectArrayList();

                  for (T $$14 : $$7) {
                     try {
                        Collection<T> $$15 = ContextChain.runModifier($$9, $$14, (p_309424_, p_309425_, p_309426_) -> {}, $$12);
                        if ($$13.size() + $$15.size() >= $$8) {
                           p_309567_.handleError(ERROR_FORK_LIMIT_REACHED.create($$8), $$12, p_305977_.tracer());
                           return;
                        }

                        $$13.addAll($$15);
                     } catch (CommandSyntaxException var20) {
                        $$14.handleError(var20, $$12, p_305977_.tracer());
                        if (!$$12) {
                           return;
                        }
                     }
                  }

                  $$7 = $$13;
               }
            }
         } finally {
            p_305977_.profiler().pop();
         }
      }

      if ($$7.isEmpty()) {
         if ($$6.isReturn()) {
            p_305977_.queueNext(new CommandQueueEntry<>(p_309603_, FallthroughTask.instance()));
         }
      } else {
         CommandContext<T> $$17 = $$5.getTopContext();
         if ($$17.getCommand() instanceof CustomCommandExecutor<T> $$19) {
            ExecutionControl<T> $$20 = ExecutionControl.create(p_305977_, p_309603_);

            for (T $$21 : $$7) {
               $$19.run($$21, $$5, $$6, $$20);
            }
         } else {
            if ($$6.isReturn()) {
               T $$22 = $$7.get(0);
               $$22 = $$22.withCallback(CommandResultCallback.chain($$22.callback(), p_309603_.returnValueConsumer()));
               $$7 = List.of($$22);
            }

            ExecuteCommand<T> $$23 = new ExecuteCommand<>(this.commandInput, $$6, $$17);
            ContinuationTask.schedule(p_305977_, p_309603_, $$7, (p_309428_, p_309429_) -> new CommandQueueEntry<>(p_309428_, $$23.bind(p_309429_)));
         }
      }
   }

   protected void traceCommandStart(ExecutionContext<T> p_306237_, Frame p_309625_) {
      TraceCallbacks $$2 = p_306237_.tracer();
      if ($$2 != null) {
         $$2.onCommand(p_309625_.depth(), this.commandInput);
      }
   }

   @Override
   public String toString() {
      return this.commandInput;
   }

   public static class Continuation<T extends ExecutionCommandSource<T>> extends BuildContexts<T> implements EntryAction<T> {
      private final ChainModifiers modifiers;
      private final T originalSource;
      private final List<T> sources;

      public Continuation(String p_306217_, ContextChain<T> p_305988_, ChainModifiers p_309702_, T p_309701_, List<T> p_306017_) {
         super(p_306217_, p_305988_);
         this.originalSource = p_309701_;
         this.sources = p_306017_;
         this.modifiers = p_309702_;
      }

      @Override
      public void execute(ExecutionContext<T> p_305882_, Frame p_309577_) {
         this.execute(this.originalSource, this.sources, p_305882_, p_309577_, this.modifiers);
      }
   }

   public static class TopLevel<T extends ExecutionCommandSource<T>> extends BuildContexts<T> implements EntryAction<T> {
      private final T source;

      public TopLevel(String p_306314_, ContextChain<T> p_305844_, T p_306134_) {
         super(p_306314_, p_305844_);
         this.source = p_306134_;
      }

      @Override
      public void execute(ExecutionContext<T> p_306114_, Frame p_309583_) {
         this.traceCommandStart(p_306114_, p_309583_);
         this.execute(this.source, List.of(this.source), p_306114_, p_309583_, ChainModifiers.DEFAULT);
      }
   }

   public static class Unbound<T extends ExecutionCommandSource<T>> extends BuildContexts<T> implements UnboundEntryAction<T> {
      public Unbound(String p_305863_, ContextChain<T> p_305842_) {
         super(p_305863_, p_305842_);
      }

      public void execute(T p_306259_, ExecutionContext<T> p_305944_, Frame p_309714_) {
         this.traceCommandStart(p_305944_, p_309714_);
         this.execute(p_306259_, List.of(p_306259_), p_305944_, p_309714_, ChainModifiers.DEFAULT);
      }
   }
}
