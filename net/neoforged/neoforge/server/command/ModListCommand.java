package net.neoforged.neoforge.server.command;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.util.Locale;
import java.util.stream.Collectors;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.neoforged.fml.ModList;
import net.neoforged.neoforgespi.language.IModInfo;

class ModListCommand {
   static ArgumentBuilder<CommandSourceStack, ?> register() {
      return ((LiteralArgumentBuilder)Commands.literal("mods").requires(Commands.hasPermission(Commands.LEVEL_ALL)))
         .executes(
            ctx -> {
               ((CommandSourceStack)ctx.getSource())
                  .sendSuccess(
                     () -> CommandUtils.makeTranslatableWithFallback(
                        "commands.neoforge.mods.list",
                        ModList.get()
                           .applyForEachModFile(
                              modFile -> String.format(
                                 Locale.ROOT,
                                 "%s : %s (%s) - %d %s",
                                 modFile.getFileName(),
                                 ((IModInfo)modFile.getModInfos().get(0)).getModId(),
                                 ((IModInfo)modFile.getModInfos().get(0)).getVersion(),
                                 modFile.getModInfos().size(),
                                 modFile.getDiscoveryAttributes()
                              )
                           )
                           .collect(Collectors.joining("\n• ", "\n• ", ""))
                     ),
                     false
                  );
               return 0;
            }
         );
   }
}
