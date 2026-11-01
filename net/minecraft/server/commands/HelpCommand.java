package net.minecraft.server.commands;

import com.google.common.collect.Iterables;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.ParsedCommandNode;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.tree.CommandNode;
import java.util.Map;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class HelpCommand {
   private static final SimpleCommandExceptionType ERROR_FAILED = new SimpleCommandExceptionType(Component.translatable("commands.help.failed"));

   public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
      dispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("help").executes(commandContext -> {
               Map<CommandNode<CommandSourceStack>, String> map = dispatcher.getSmartUsage(dispatcher.getRoot(), (CommandSourceStack)commandContext.getSource());

               for (String string : map.values()) {
                  ((CommandSourceStack)commandContext.getSource()).sendSuccess(() -> Component.literal("/" + string), false);
               }

               return map.size();
            }))
            .then(
               Commands.argument("command", StringArgumentType.greedyString())
                  .executes(
                     commandContext -> {
                        ParseResults<CommandSourceStack> parseResults = dispatcher.parse(
                           StringArgumentType.getString(commandContext, "command"), (CommandSourceStack)commandContext.getSource()
                        );
                        if (parseResults.getContext().getNodes().isEmpty()) {
                           throw ERROR_FAILED.create();
                        } else {
                           Map<CommandNode<CommandSourceStack>, String> map = dispatcher.getSmartUsage(
                              ((ParsedCommandNode)Iterables.getLast(parseResults.getContext().getNodes())).getNode(),
                              (CommandSourceStack)commandContext.getSource()
                           );

                           for (String string : map.values()) {
                              ((CommandSourceStack)commandContext.getSource())
                                 .sendSuccess(() -> Component.literal("/" + parseResults.getReader().getString() + " " + string), false);
                           }

                           return map.size();
                        }
                     }
                  )
            )
      );
   }
}
