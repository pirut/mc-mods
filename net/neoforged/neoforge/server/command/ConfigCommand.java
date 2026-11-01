package net.neoforged.neoforge.server.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import java.io.File;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.config.ModConfigs;
import net.neoforged.fml.config.ModConfig.Type;
import net.neoforged.fml.loading.FMLEnvironment;

public class ConfigCommand {
   public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
      dispatcher.register((LiteralArgumentBuilder)Commands.literal("config").then(ConfigCommand.ShowFile.register()));
   }

   public static enum ServerModConfigType {
      COMMON,
      SERVER;

      public String extension() {
         return this.name().toLowerCase(Locale.ROOT);
      }
   }

   public static class ShowFile {
      static ArgumentBuilder<CommandSourceStack, ?> register() {
         return ((LiteralArgumentBuilder)Commands.literal("showfile").requires(Commands.hasPermission(Commands.LEVEL_ALL)))
            .then(
               Commands.argument("mod", ModIdArgument.modIdArgument())
                  .then(
                     Commands.argument("type", EnumArgument.enumArgument(ConfigCommand.ServerModConfigType.class)).executes(ConfigCommand.ShowFile::showFile)
                  )
            );
      }

      private static int showFile(CommandContext<CommandSourceStack> context) {
         String modId = (String)context.getArgument("mod", String.class);
         Type type = Type.valueOf(((ConfigCommand.ServerModConfigType)context.getArgument("type", ConfigCommand.ServerModConfigType.class)).toString());
         List<String> configFileNames = ModConfigs.getConfigFileNames(modId, type);

         for (String configFileName : configFileNames) {
            File f = new File(configFileName);
            MutableComponent fileComponent = Component.literal(f.getName()).withStyle(ChatFormatting.UNDERLINE);
            ServerPlayer caller = ((CommandSourceStack)context.getSource()).getPlayer();
            if (FMLEnvironment.getDist().isClient() && caller != null && caller.connection.getConnection().isMemoryConnection()) {
               fileComponent.withStyle(style -> style.withClickEvent(new ClickEvent.OpenFile(f)));
            }

            ((CommandSourceStack)context.getSource())
               .sendSuccess(() -> CommandUtils.makeTranslatableWithFallback("commands.config.getwithtype", modId, type.toString(), fileComponent), true);
         }

         if (configFileNames.isEmpty()) {
            ((CommandSourceStack)context.getSource())
               .sendSuccess(() -> CommandUtils.makeTranslatableWithFallback("commands.config.noconfig", modId, type.toString()), true);
         }

         return 0;
      }
   }
}
