package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.util.function.Consumer;
import net.minecraft.SharedConstants;
import net.minecraft.WorldVersion;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;

public class VersionCommand {
   private static final Component HEADER = Component.translatable("commands.version.header");
   private static final Component STABLE = Component.translatable("commands.version.stable.yes");
   private static final Component UNSTABLE = Component.translatable("commands.version.stable.no");

   public static void register(CommandDispatcher<CommandSourceStack> dispatcher, boolean isDedicatedServer) {
      dispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("version")
               .requires(Commands.hasPermission(isDedicatedServer ? Commands.LEVEL_GAMEMASTERS : Commands.LEVEL_ALL)))
            .executes(commandContext -> {
               CommandSourceStack commandSourceStack = (CommandSourceStack)commandContext.getSource();
               commandSourceStack.sendSystemMessage(HEADER);
               dumpVersion(commandSourceStack::sendSystemMessage);
               return 1;
            })
      );
   }

   public static void dumpVersion(Consumer<Component> output) {
      WorldVersion worldVersion = SharedConstants.getCurrentVersion();
      output.accept(Component.translatable("commands.version.id", worldVersion.id()));
      output.accept(Component.translatable("commands.version.name", worldVersion.name()));
      output.accept(Component.translatable("commands.version.data", worldVersion.dataVersion().version()));
      output.accept(Component.translatable("commands.version.series", worldVersion.dataVersion().series()));
      output.accept(
         Component.translatable("commands.version.protocol", worldVersion.protocolVersion(), "0x" + Integer.toHexString(worldVersion.protocolVersion()))
      );
      output.accept(Component.translatable("commands.version.build_time", Component.translationArg(worldVersion.buildTime())));
      output.accept(Component.translatable("commands.version.pack.resource", worldVersion.packVersion(PackType.CLIENT_RESOURCES).toString()));
      output.accept(Component.translatable("commands.version.pack.data", worldVersion.packVersion(PackType.SERVER_DATA).toString()));
      output.accept(worldVersion.stable() ? STABLE : UNSTABLE);
   }
}
