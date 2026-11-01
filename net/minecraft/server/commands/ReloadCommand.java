package net.minecraft.server.commands;

import com.google.common.collect.Lists;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.logging.LogUtils;
import java.util.Collection;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.world.level.storage.WorldData;
import org.slf4j.Logger;

public class ReloadCommand {
   private static final Logger LOGGER = LogUtils.getLogger();

   public static void reloadPacks(Collection<String> selectedIds, CommandSourceStack source) {
      source.getServer().reloadResources(selectedIds).exceptionally(throwable -> {
         LOGGER.warn("Failed to execute reload", throwable);
         source.sendFailure(Component.translatable("commands.reload.failure"));
         return null;
      });
   }

   private static Collection<String> discoverNewPacks(PackRepository packRepository, WorldData worldData, Collection<String> selectedIds) {
      packRepository.reload();
      Collection<String> collection = Lists.newArrayList(selectedIds);
      Collection<String> collection2 = worldData.getDataConfiguration().dataPacks().getDisabled();

      for (String string : packRepository.getAvailableIds()) {
         if (!collection2.contains(string) && !collection.contains(string)) {
            collection.add(string);
         }
      }

      return collection;
   }

   public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
      dispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("reload").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)))
            .executes(commandContext -> {
               CommandSourceStack commandSourceStack = (CommandSourceStack)commandContext.getSource();
               MinecraftServer minecraftServer = commandSourceStack.getServer();
               PackRepository packRepository = minecraftServer.getPackRepository();
               WorldData worldData = minecraftServer.getWorldData();
               Collection<String> collection = packRepository.getSelectedIds();
               Collection<String> collection2 = discoverNewPacks(packRepository, worldData, collection);
               commandSourceStack.sendSuccess(() -> Component.translatable("commands.reload.success"), true);
               reloadPacks(collection2, commandSourceStack);
               return 0;
            })
      );
   }
}
