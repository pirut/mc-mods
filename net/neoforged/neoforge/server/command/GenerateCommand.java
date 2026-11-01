package net.neoforged.neoforge.server.command;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.neoforge.server.command.generation.GenerationBar;
import net.neoforged.neoforge.server.command.generation.GenerationTask;

class GenerateCommand {
   private static GenerationTask activeTask;
   private static GenerationBar generationBar;

   static ArgumentBuilder<CommandSourceStack, ?> register() {
      LiteralArgumentBuilder<CommandSourceStack> builder = (LiteralArgumentBuilder<CommandSourceStack>)Commands.literal("generate")
         .requires(cs -> cs.hasPermission(4));
      builder.then(
         Commands.literal("start")
            .then(
               Commands.argument("pos", BlockPosArgument.blockPos())
                  .then(
                     ((RequiredArgumentBuilder)Commands.argument("chunkRadius", IntegerArgumentType.integer(1, 2500))
                           .then(
                              Commands.argument("progressBar", BoolArgumentType.bool())
                                 .executes(
                                    ctx -> executeGeneration(
                                       (CommandSourceStack)ctx.getSource(),
                                       BlockPosArgument.getSpawnablePos(ctx, "pos"),
                                       getInt(ctx, "chunkRadius"),
                                       getBool(ctx, "progressBar")
                                    )
                                 )
                           ))
                        .executes(
                           ctx -> executeGeneration(
                              (CommandSourceStack)ctx.getSource(), BlockPosArgument.getSpawnablePos(ctx, "pos"), getInt(ctx, "chunkRadius"), true
                           )
                        )
                  )
            )
      );
      builder.then(Commands.literal("stop").executes(ctx -> stopGeneration((CommandSourceStack)ctx.getSource())));
      builder.then(Commands.literal("status").executes(ctx -> getGenerationStatus((CommandSourceStack)ctx.getSource())));
      builder.then(Commands.literal("help").executes(ctx -> getGenerationHelp((CommandSourceStack)ctx.getSource())));
      return builder;
   }

   private static int getInt(CommandContext<CommandSourceStack> ctx, String name) {
      return IntegerArgumentType.getInteger(ctx, name);
   }

   private static boolean getBool(CommandContext<CommandSourceStack> ctx, String name) {
      return BoolArgumentType.getBool(ctx, name);
   }

   private static int executeGeneration(CommandSourceStack source, BlockPos pos, int chunkRadius, boolean progressBar) {
      if (activeTask != null) {
         source.sendSuccess(() -> CommandUtils.makeTranslatableWithFallback("commands.neoforge.chunkgen.already_running"), true);
         return 1;
      } else {
         ChunkPos origin = new ChunkPos(pos);
         activeTask = new GenerationTask(source.getLevel(), origin.x, origin.z, chunkRadius);
         int diameter = chunkRadius * 2 + 1;
         if (progressBar) {
            generationBar = new GenerationBar();
            if (source.getEntity() instanceof ServerPlayer) {
               generationBar.addPlayer(source.getPlayer());
            }
         }

         source.sendSuccess(
            () -> CommandUtils.makeTranslatableWithFallback(
               "commands.neoforge.chunkgen.started", activeTask.getTotalCount(), diameter, diameter, diameter * 16, diameter * 16
            ),
            true
         );
         activeTask.run(createPregenListener(source));
         return 1;
      }
   }

   private static int stopGeneration(CommandSourceStack source) {
      if (activeTask != null) {
         activeTask.stop();
         int count = activeTask.getOkCount() + activeTask.getErrorCount() + activeTask.getSkippedCount();
         int total = activeTask.getTotalCount();
         double percent = (double)count / total * 100.0;
         source.sendSuccess(() -> CommandUtils.makeTranslatableWithFallback("commands.neoforge.chunkgen.stopped", count, total, percent), true);
         if (generationBar != null) {
            generationBar.close();
            generationBar = null;
         }

         activeTask = null;
      } else {
         source.sendSuccess(() -> CommandUtils.makeTranslatableWithFallback("commands.neoforge.chunkgen.not_running"), false);
      }

      return 1;
   }

   private static int getGenerationStatus(CommandSourceStack source) {
      if (activeTask != null) {
         int count = activeTask.getOkCount() + activeTask.getErrorCount() + activeTask.getSkippedCount();
         int total = activeTask.getTotalCount();
         double percent = (double)count / total * 100.0;
         source.sendSuccess(() -> CommandUtils.makeTranslatableWithFallback("commands.neoforge.chunkgen.status", count, total, percent), true);
      } else {
         source.sendSuccess(() -> CommandUtils.makeTranslatableWithFallback("commands.neoforge.chunkgen.not_running"), false);
      }

      return 1;
   }

   private static int getGenerationHelp(CommandSourceStack source) {
      source.sendSuccess(() -> CommandUtils.makeTranslatableWithFallback("commands.neoforge.chunkgen.help_line"), false);
      return 1;
   }

   private static GenerationTask.Listener createPregenListener(final CommandSourceStack source) {
      return new GenerationTask.Listener() {
         @Override
         public void update(int ok, int error, int skipped, int total) {
            if (GenerateCommand.generationBar != null) {
               GenerateCommand.generationBar.update(ok, error, skipped, total);
            }
         }

         @Override
         public void complete(int error) {
            source.sendSuccess(() -> CommandUtils.makeTranslatableWithFallback("commands.neoforge.chunkgen.success"), true);
            if (error > 0) {
               source.sendFailure(CommandUtils.makeTranslatableWithFallback("commands.neoforge.chunkgen.error"));
            }

            if (GenerateCommand.generationBar != null) {
               GenerateCommand.generationBar.close();
               GenerateCommand.generationBar = null;
            }

            GenerateCommand.activeTask = null;
         }
      };
   }
}
