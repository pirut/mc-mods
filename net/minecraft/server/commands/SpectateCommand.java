package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.jspecify.annotations.Nullable;

public class SpectateCommand {
   private static final SimpleCommandExceptionType ERROR_SELF = new SimpleCommandExceptionType(Component.translatable("commands.spectate.self"));
   private static final DynamicCommandExceptionType ERROR_NOT_SPECTATOR = new DynamicCommandExceptionType(
      object -> Component.translatableEscape("commands.spectate.not_spectator", object)
   );
   private static final DynamicCommandExceptionType ERROR_CANNOT_SPECTATE = new DynamicCommandExceptionType(
      object -> Component.translatableEscape("commands.spectate.cannot_spectate", object)
   );

   public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
      dispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("spectate")
                  .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)))
               .executes(
                  commandContext -> spectate(
                     (CommandSourceStack)commandContext.getSource(), null, ((CommandSourceStack)commandContext.getSource()).getPlayerOrException()
                  )
               ))
            .then(
               ((RequiredArgumentBuilder)Commands.argument("target", EntityArgument.entity())
                     .executes(
                        commandContext -> spectate(
                           (CommandSourceStack)commandContext.getSource(),
                           EntityArgument.getEntity(commandContext, "target"),
                           ((CommandSourceStack)commandContext.getSource()).getPlayerOrException()
                        )
                     ))
                  .then(
                     Commands.argument("player", EntityArgument.player())
                        .executes(
                           commandContext -> spectate(
                              (CommandSourceStack)commandContext.getSource(),
                              EntityArgument.getEntity(commandContext, "target"),
                              EntityArgument.getPlayer(commandContext, "player")
                           )
                        )
                  )
            )
      );
   }

   private static int spectate(CommandSourceStack source, @Nullable Entity target, ServerPlayer player) throws CommandSyntaxException {
      if (player == target) {
         throw ERROR_SELF.create();
      } else if (!player.isSpectator()) {
         throw ERROR_NOT_SPECTATOR.create(player.getDisplayName());
      } else if (target != null && target.getType().clientTrackingRange() == 0) {
         throw ERROR_CANNOT_SPECTATE.create(target.getDisplayName());
      } else {
         player.setCamera(target);
         if (target != null) {
            source.sendSuccess(() -> Component.translatable("commands.spectate.success.started", target.getDisplayName()), false);
         } else {
            source.sendSuccess(() -> Component.translatable("commands.spectate.success.stopped"), false);
         }

         return 1;
      }
   }
}
