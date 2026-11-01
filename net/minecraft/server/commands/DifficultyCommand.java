package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.Difficulty;

public class DifficultyCommand {
   private static final DynamicCommandExceptionType ERROR_ALREADY_DIFFICULT = new DynamicCommandExceptionType(
      p_304204_ -> Component.translatableEscape("commands.difficulty.failure", p_304204_)
   );

   public static void register(CommandDispatcher<CommandSourceStack> p_136939_) {
      LiteralArgumentBuilder<CommandSourceStack> $$1 = Commands.literal("difficulty");

      for (Difficulty $$2 : Difficulty.values()) {
         $$1.then(Commands.literal($$2.getKey()).executes(p_136937_ -> setDifficulty((CommandSourceStack)p_136937_.getSource(), $$2)));
      }

      p_136939_.register((LiteralArgumentBuilder)((LiteralArgumentBuilder)$$1.requires(p_136943_ -> p_136943_.hasPermission(2))).executes(p_340656_ -> {
         Difficulty $$1x = ((CommandSourceStack)p_340656_.getSource()).getLevel().getDifficulty();
         ((CommandSourceStack)p_340656_.getSource()).sendSuccess(() -> Component.translatable("commands.difficulty.query", $$1x.getDisplayName()), false);
         return $$1x.getId();
      }));
   }

   public static int setDifficulty(CommandSourceStack p_136945_, Difficulty p_136946_) throws CommandSyntaxException {
      MinecraftServer $$2 = p_136945_.getServer();
      if ($$2.getWorldData().getDifficulty() == p_136946_) {
         throw ERROR_ALREADY_DIFFICULT.create(p_136946_.getKey());
      } else {
         $$2.setDifficulty(p_136946_, true);
         p_136945_.sendSuccess(() -> Component.translatable("commands.difficulty.success", p_136946_.getDisplayName()), true);
         return 0;
      }
   }
}
