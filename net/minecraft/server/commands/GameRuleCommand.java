package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.GameRules;

public class GameRuleCommand {
   public static void register(CommandDispatcher<CommandSourceStack> p_137745_) {
      final LiteralArgumentBuilder<CommandSourceStack> $$1 = (LiteralArgumentBuilder<CommandSourceStack>)Commands.literal("gamerule")
         .requires(p_137750_ -> p_137750_.hasPermission(2));
      GameRules.visitGameRuleTypes(
         new GameRules.GameRuleTypeVisitor() {
            @Override
            public <T extends GameRules.Value<T>> void visit(GameRules.Key<T> p_137764_, GameRules.Type<T> p_137765_) {
               $$1.then(
                  ((LiteralArgumentBuilder)Commands.literal(p_137764_.getId())
                        .executes(p_137771_ -> GameRuleCommand.queryRule((CommandSourceStack)p_137771_.getSource(), p_137764_)))
                     .then(p_137765_.createArgument("value").executes(p_137768_ -> GameRuleCommand.setRule(p_137768_, p_137764_)))
               );
            }
         }
      );
      p_137745_.register($$1);
   }

   static <T extends GameRules.Value<T>> int setRule(CommandContext<CommandSourceStack> p_137755_, GameRules.Key<T> p_137756_) {
      CommandSourceStack $$2 = (CommandSourceStack)p_137755_.getSource();
      T $$3 = $$2.getServer().getGameRules().getRule(p_137756_);
      $$3.setFromArgument(p_137755_, "value");
      $$2.sendSuccess(() -> Component.translatable("commands.gamerule.set", p_137756_.getId(), $$3.toString()), true);
      return $$3.getCommandResult();
   }

   static <T extends GameRules.Value<T>> int queryRule(CommandSourceStack p_137758_, GameRules.Key<T> p_137759_) {
      T $$2 = p_137758_.getServer().getGameRules().getRule(p_137759_);
      p_137758_.sendSuccess(() -> Component.translatable("commands.gamerule.query", p_137759_.getId(), $$2.toString()), false);
      return $$2.getCommandResult();
   }
}
