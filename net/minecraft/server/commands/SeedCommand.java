package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;

public class SeedCommand {
   public static void register(CommandDispatcher<CommandSourceStack> p_138590_, boolean p_138591_) {
      p_138590_.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("seed").requires(p_138596_ -> !p_138591_ || p_138596_.hasPermission(2)))
            .executes(p_288608_ -> {
               long $$1 = ((CommandSourceStack)p_288608_.getSource()).getLevel().getSeed();
               Component $$2 = ComponentUtils.copyOnClickText(String.valueOf($$1));
               ((CommandSourceStack)p_288608_.getSource()).sendSuccess(() -> Component.translatable("commands.seed.success", $$2), false);
               return (int)$$1;
            })
      );
   }
}
