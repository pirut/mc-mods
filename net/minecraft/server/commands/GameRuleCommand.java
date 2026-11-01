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
   public static void register(CommandDispatcher<CommandSourceStack> p_137745_, CommandBuildContext p_373017_) {
      final LiteralArgumentBuilder<CommandSourceStack> literalargumentbuilder = (LiteralArgumentBuilder<CommandSourceStack>)Commands.literal("gamerule")
         .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));
      new GameRules(p_373017_.enabledFeatures())
         .visitGameRuleTypes(
            new GameRuleTypeVisitor() {
               @Override
               public <T> void visit(GameRule<T> p_460992_) {
                  LiteralArgumentBuilder<CommandSourceStack> literalargumentbuilder1 = Commands.literal(p_460992_.id());
                  LiteralArgumentBuilder<CommandSourceStack> literalargumentbuilder2 = Commands.literal(p_460992_.getIdentifier().toString());
                  ((LiteralArgumentBuilder)literalargumentbuilder.then(GameRuleCommand.buildRuleArguments(p_460992_, literalargumentbuilder1)))
                     .then(GameRuleCommand.buildRuleArguments(p_460992_, literalargumentbuilder2));
               }
            }
         );
      p_137745_.register(literalargumentbuilder);
   }

   static <T> LiteralArgumentBuilder<CommandSourceStack> buildRuleArguments(GameRule<T> p_480085_, LiteralArgumentBuilder<CommandSourceStack> p_479071_) {
      return (LiteralArgumentBuilder<CommandSourceStack>)((LiteralArgumentBuilder)p_479071_.executes(
            p_477765_ -> queryRule((CommandSourceStack)p_477765_.getSource(), p_480085_)
         ))
         .then(Commands.argument("value", p_480085_.argument()).executes(p_477763_ -> setRule(p_477763_, p_480085_)));
   }

   private static <T> int setRule(CommandContext<CommandSourceStack> p_137755_, GameRule<T> p_460978_) {
      CommandSourceStack commandsourcestack = (CommandSourceStack)p_137755_.getSource();
      T t = (T)p_137755_.getArgument("value", p_460978_.valueClass());
      commandsourcestack.getLevel().getGameRules().set(p_460978_, t, ((CommandSourceStack)p_137755_.getSource()).getServer());
      commandsourcestack.sendSuccess(() -> Component.translatable("commands.gamerule.set", p_460978_.id(), p_460978_.serialize(t)), true);
      return p_460978_.getCommandResult(t);
   }

   private static <T> int queryRule(CommandSourceStack p_137758_, GameRule<T> p_461230_) {
      T t = p_137758_.getLevel().getGameRules().get(p_461230_);
      p_137758_.sendSuccess(() -> Component.translatable("commands.gamerule.query", p_461230_.id(), p_461230_.serialize(t)), false);
      return p_461230_.getCommandResult(t);
   }
}
