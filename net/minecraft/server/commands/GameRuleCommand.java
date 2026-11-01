package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRuleTypeVisitor;
import net.minecraft.world.level.gamerules.GameRules;

public class GameRuleCommand {
   public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext commandBuildContext) {
      final LiteralArgumentBuilder<CommandSourceStack> literalArgumentBuilder = (LiteralArgumentBuilder<CommandSourceStack>)Commands.literal("gamerule")
         .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));
      new GameRules(commandBuildContext.enabledFeatures())
         .visitGameRuleTypes(
            new GameRuleTypeVisitor() {
               @Override
               public <T> void visit(GameRule<T> gameRule) {
                  LiteralArgumentBuilder<CommandSourceStack> literalArgumentBuilderx = Commands.literal(gameRule.id());
                  LiteralArgumentBuilder<CommandSourceStack> literalArgumentBuilder2 = Commands.literal(gameRule.getIdentifier().toString());
                  ((LiteralArgumentBuilder)literalArgumentBuilder.then(GameRuleCommand.buildRuleArguments(gameRule, literalArgumentBuilderx)))
                     .then(GameRuleCommand.buildRuleArguments(gameRule, literalArgumentBuilder2));
               }
            }
         );
      dispatcher.register(literalArgumentBuilder);
   }

   static <T> LiteralArgumentBuilder<CommandSourceStack> buildRuleArguments(GameRule<T> gameRule, LiteralArgumentBuilder<CommandSourceStack> builder) {
      return (LiteralArgumentBuilder<CommandSourceStack>)((LiteralArgumentBuilder)builder.executes(
            commandContext -> queryRule((CommandSourceStack)commandContext.getSource(), gameRule)
         ))
         .then(Commands.argument("value", gameRule.argument()).executes(commandContext -> setRule(commandContext, gameRule)));
   }

   private static <T> int setRule(CommandContext<CommandSourceStack> context, GameRule<T> gameRule) {
      CommandSourceStack commandSourceStack = (CommandSourceStack)context.getSource();
      T object = (T)context.getArgument("value", gameRule.valueClass());
      commandSourceStack.getLevel().getGameRules().set(gameRule, object, ((CommandSourceStack)context.getSource()).getServer());
      commandSourceStack.sendSuccess(() -> Component.translatable("commands.gamerule.set", gameRule.id(), gameRule.serialize(object)), true);
      return gameRule.getCommandResult(object);
   }

   private static <T> int queryRule(CommandSourceStack source, GameRule<T> gameRule) {
      T object = source.getLevel().getGameRules().get(gameRule);
      source.sendSuccess(() -> Component.translatable("commands.gamerule.query", gameRule.id(), gameRule.serialize(object)), false);
      return gameRule.getCommandResult(object);
   }
}
