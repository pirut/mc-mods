package net.neoforged.neoforge.client.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import java.io.File;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.neoforged.fml.config.ModConfigs;
import net.neoforged.fml.config.ModConfig.Type;
import net.neoforged.neoforge.server.command.ModIdArgument;

public class ClientConfigCommand {
   public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
      dispatcher.register((LiteralArgumentBuilder)Commands.literal("config").then(ClientConfigCommand.ShowFile.register()));
   }

   public static class ShowFile {
      static ArgumentBuilder<CommandSourceStack, ?> register() {
         return ((LiteralArgumentBuilder)Commands.literal("showfileclient").requires(Commands.hasPermission(Commands.LEVEL_ALL)))
            .then(Commands.argument("mod", ModIdArgument.modIdArgument()).executes(ClientConfigCommand.ShowFile::showFile));
      }

      private static int showFile(CommandContext<CommandSourceStack> context) {
         String modId = (String)context.getArgument("mod", String.class);
         Type type = Type.CLIENT;
         List<String> configFileNames = ModConfigs.getConfigFileNames(modId, type);

         for (String configFileName : configFileNames) {
            File f = new File(configFileName);
            MutableComponent fileComponent = Component.literal(f.getName())
               .withStyle(ChatFormatting.UNDERLINE)
               .withStyle(style -> style.withClickEvent(new ClickEvent.OpenFile(f)));
            ((CommandSourceStack)context.getSource())
               .sendSuccess(() -> Component.translatable("commands.config.getwithtype", modId, type.toString(), fileComponent), true);
         }

         if (configFileNames.isEmpty()) {
            ((CommandSourceStack)context.getSource()).sendSuccess(() -> Component.translatable("commands.config.noconfig", modId, type.toString()), true);
         }

         return 0;
      }
   }
}
