package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.ContextChain;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Locale;
import net.minecraft.Util;
import net.minecraft.commands.CommandResultCallback;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.FunctionInstantiationException;
import net.minecraft.commands.arguments.item.FunctionArgument;
import net.minecraft.commands.execution.ChainModifiers;
import net.minecraft.commands.execution.CustomCommandExecutor;
import net.minecraft.commands.execution.ExecutionContext;
import net.minecraft.commands.execution.ExecutionControl;
import net.minecraft.commands.execution.Frame;
import net.minecraft.commands.execution.TraceCallbacks;
import net.minecraft.commands.execution.tasks.CallFunction;
import net.minecraft.commands.functions.CommandFunction;
import net.minecraft.commands.functions.InstantiatedFunction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.TimeUtil;
import net.minecraft.util.profiling.ProfileResults;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;

public class DebugCommand {
   static final Logger LOGGER = LogUtils.getLogger();
   private static final SimpleCommandExceptionType ERROR_NOT_RUNNING = new SimpleCommandExceptionType(Component.translatable("commands.debug.notRunning"));
   private static final SimpleCommandExceptionType ERROR_ALREADY_RUNNING = new SimpleCommandExceptionType(
      Component.translatable("commands.debug.alreadyRunning")
   );
   static final SimpleCommandExceptionType NO_RECURSIVE_TRACES = new SimpleCommandExceptionType(Component.translatable("commands.debug.function.noRecursion"));
   static final SimpleCommandExceptionType NO_RETURN_RUN = new SimpleCommandExceptionType(Component.translatable("commands.debug.function.noReturnRun"));

   public static void register(CommandDispatcher<CommandSourceStack> p_136906_) {
      p_136906_.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("debug")
                     .requires(p_180073_ -> p_180073_.hasPermission(3)))
                  .then(Commands.literal("start").executes(p_180069_ -> start((CommandSourceStack)p_180069_.getSource()))))
               .then(Commands.literal("stop").executes(p_136918_ -> stop((CommandSourceStack)p_136918_.getSource()))))
            .then(
               ((LiteralArgumentBuilder)Commands.literal("function").requires(p_180071_ -> p_180071_.hasPermission(3)))
                  .then(
                     Commands.argument("name", FunctionArgument.functions())
                        .suggests(FunctionCommand.SUGGEST_FUNCTION)
                        .executes(new DebugCommand.TraceCustomExecutor())
                  )
            )
      );
   }

   private static int start(CommandSourceStack p_136910_) throws CommandSyntaxException {
      MinecraftServer $$1 = p_136910_.getServer();
      if ($$1.isTimeProfilerRunning()) {
         throw ERROR_ALREADY_RUNNING.create();
      } else {
         $$1.startTimeProfiler();
         p_136910_.sendSuccess(() -> Component.translatable("commands.debug.started"), true);
         return 0;
      }
   }

   private static int stop(CommandSourceStack p_136916_) throws CommandSyntaxException {
      MinecraftServer $$1 = p_136916_.getServer();
      if (!$$1.isTimeProfilerRunning()) {
         throw ERROR_NOT_RUNNING.create();
      } else {
         ProfileResults $$2 = $$1.stopTimeProfiler();
         double $$3 = (double)$$2.getNanoDuration() / TimeUtil.NANOSECONDS_PER_SECOND;
         double $$4 = $$2.getTickDuration() / $$3;
         p_136916_.sendSuccess(
            () -> Component.translatable(
               "commands.debug.stopped", String.format(Locale.ROOT, "%.2f", $$3), $$2.getTickDuration(), String.format(Locale.ROOT, "%.2f", $$4)
            ),
            true
         );
         return (int)$$4;
      }
   }

   static class TraceCustomExecutor
      extends CustomCommandExecutor.WithErrorHandling<CommandSourceStack>
      implements CustomCommandExecutor.CommandAdapter<CommandSourceStack> {
      public void runGuarded(
         CommandSourceStack p_309716_, ContextChain<CommandSourceStack> p_306169_, ChainModifiers p_309697_, ExecutionControl<CommandSourceStack> p_306283_
      ) throws CommandSyntaxException {
         if (p_309697_.isReturn()) {
            throw DebugCommand.NO_RETURN_RUN.create();
         } else if (p_306283_.tracer() != null) {
            throw DebugCommand.NO_RECURSIVE_TRACES.create();
         } else {
            CommandContext<CommandSourceStack> $$4 = p_306169_.getTopContext();
            Collection<CommandFunction<CommandSourceStack>> $$5 = FunctionArgument.getFunctions($$4, "name");
            MinecraftServer $$6 = p_309716_.getServer();
            String $$7 = "debug-trace-" + Util.getFilenameFormattedDateTime() + ".txt";
            CommandDispatcher<CommandSourceStack> $$8 = p_309716_.getServer().getFunctions().getDispatcher();
            int $$9 = 0;

            try {
               Path $$10 = $$6.getFile("debug");
               Files.createDirectories($$10);
               final PrintWriter $$11 = new PrintWriter(Files.newBufferedWriter($$10.resolve($$7), StandardCharsets.UTF_8));
               DebugCommand.Tracer $$12 = new DebugCommand.Tracer($$11);
               p_306283_.tracer($$12);

               for (final CommandFunction<CommandSourceStack> $$13 : $$5) {
                  try {
                     CommandSourceStack $$14 = p_309716_.withSource($$12).withMaximumPermission(2);
                     InstantiatedFunction<CommandSourceStack> $$15 = $$13.instantiate(null, $$8);
                     p_306283_.queueNext((new CallFunction<CommandSourceStack>($$15, CommandResultCallback.EMPTY, false) {
                        public void execute(CommandSourceStack p_309660_, ExecutionContext<CommandSourceStack> p_309654_, Frame p_309674_) {
                           $$11.println($$13.id());
                           super.execute(p_309660_, p_309654_, p_309674_);
                        }
                     }).bind($$14));
                     $$9 += $$15.entries().size();
                  } catch (FunctionInstantiationException var18) {
                     p_309716_.sendFailure(var18.messageComponent());
                  }
               }
            } catch (IOException | UncheckedIOException var19) {
               DebugCommand.LOGGER.warn("Tracing failed", var19);
               p_309716_.sendFailure(Component.translatable("commands.debug.function.traceFailed"));
            }

            int $$18 = $$9;
            p_306283_.queueNext(
               (p_305943_, p_309635_) -> {
                  if ($$5.size() == 1) {
                     p_309716_.sendSuccess(
                        () -> Component.translatable("commands.debug.function.success.single", $$18, Component.translationArg($$5.iterator().next().id()), $$7),
                        true
                     );
                  } else {
                     p_309716_.sendSuccess(() -> Component.translatable("commands.debug.function.success.multiple", $$18, $$5.size(), $$7), true);
                  }
               }
            );
         }
      }
   }

   static class Tracer implements CommandSource, TraceCallbacks {
      public static final int INDENT_OFFSET = 1;
      private final PrintWriter output;
      private int lastIndent;
      private boolean waitingForResult;

      Tracer(PrintWriter p_180079_) {
         this.output = p_180079_;
      }

      private void indentAndSave(int p_180082_) {
         this.printIndent(p_180082_);
         this.lastIndent = p_180082_;
      }

      private void printIndent(int p_180098_) {
         for (int $$1 = 0; $$1 < p_180098_ + 1; $$1++) {
            this.output.write("    ");
         }
      }

      private void newLine() {
         if (this.waitingForResult) {
            this.output.println();
            this.waitingForResult = false;
         }
      }

      @Override
      public void onCommand(int p_180084_, String p_180085_) {
         this.newLine();
         this.indentAndSave(p_180084_);
         this.output.print("[C] ");
         this.output.print(p_180085_);
         this.waitingForResult = true;
      }

      @Override
      public void onReturn(int p_180087_, String p_180088_, int p_180089_) {
         if (this.waitingForResult) {
            this.output.print(" -> ");
            this.output.println(p_180089_);
            this.waitingForResult = false;
         } else {
            this.indentAndSave(p_180087_);
            this.output.print("[R = ");
            this.output.print(p_180089_);
            this.output.print("] ");
            this.output.println(p_180088_);
         }
      }

      @Override
      public void onCall(int p_180091_, ResourceLocation p_180092_, int p_180093_) {
         this.newLine();
         this.indentAndSave(p_180091_);
         this.output.print("[F] ");
         this.output.print(p_180092_);
         this.output.print(" size=");
         this.output.println(p_180093_);
      }

      @Override
      public void onError(String p_180101_) {
         this.newLine();
         this.indentAndSave(this.lastIndent + 1);
         this.output.print("[E] ");
         this.output.print(p_180101_);
      }

      @Override
      public void sendSystemMessage(Component p_214427_) {
         this.newLine();
         this.printIndent(this.lastIndent + 1);
         this.output.print("[M] ");
         this.output.println(p_214427_.getString());
      }

      @Override
      public boolean acceptsSuccess() {
         return true;
      }

      @Override
      public boolean acceptsFailure() {
         return true;
      }

      @Override
      public boolean shouldInformAdmins() {
         return false;
      }

      @Override
      public boolean alwaysAccepts() {
         return true;
      }

      @Override
      public void close() {
         IOUtils.closeQuietly(this.output);
      }
   }
}
