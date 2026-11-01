package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;

public class SeedCommand {
   public static void register(CommandDispatcher<CommandSourceStack> dispatcher, boolean isDedicatedServer) {
      dispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("seed")
               .requires(Commands.hasPermission(isDedicatedServer ? Commands.LEVEL_GAMEMASTERS : Commands.LEVEL_ALL)))
            .executes(commandContext -> {
               long l = ((CommandSourceStack)commandContext.getSource()).getLevel().getSeed();
               Component component = ComponentUtils.copyOnClickText(String.valueOf(l));
               ((CommandSourceStack)commandContext.getSource()).sendSuccess(() -> Component.translatable("commands.seed.success", component), false);
               return (int)l;
            })
      );
   }
}
