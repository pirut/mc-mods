package net.neoforged.neoforge.client;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Scoreboard;
import org.jspecify.annotations.Nullable;

public class ClientCommandSourceStack extends CommandSourceStack {
   public ClientCommandSourceStack(
      CommandSource source, Vec3 position, Vec2 rotation, PermissionSet permissions, String plainTextName, Component displayName, Entity executing
   ) {
      super(source, position, rotation, null, permissions, plainTextName, displayName, null, executing);
   }

   private ClientPacketListener connection() {
      return Minecraft.getInstance().getConnection();
   }

   @Override
   public void sendSuccess(Supplier<Component> message, boolean sendToAdmins) {
      super.sendSuccess(message, false);
   }

   @Override
   public Collection<String> getAllTeams() {
      return this.getScoreboard().getTeamNames();
   }

   @Override
   public Collection<String> getOnlinePlayerNames() {
      return this.connection().getOnlinePlayers().stream().map(player -> player.getProfile().name()).collect(Collectors.toList());
   }

   @Override
   public CompletableFuture<Suggestions> suggestRegistryElements(
      ResourceKey<? extends Registry<?>> registry,
      SharedSuggestionProvider.ElementSuggestionType suggestionType,
      SuggestionsBuilder suggestionsBuilder,
      CommandContext<?> context
   ) {
      if (registry == Registries.RECIPE) {
         return Suggestions.empty();
      } else {
         return registry == Registries.ADVANCEMENT
            ? SharedSuggestionProvider.suggestResource(
               this.connection().getAdvancements().getTree().nodes().stream().map(node -> node.holder().id()), suggestionsBuilder
            )
            : super.suggestRegistryElements(registry, suggestionType, suggestionsBuilder, context);
      }
   }

   @Override
   public Set<ResourceKey<Level>> levels() {
      return this.connection().levels();
   }

   @Override
   public RegistryAccess registryAccess() {
      return this.connection().registryAccess();
   }

   @Override
   public FeatureFlagSet enabledFeatures() {
      return this.connection().enabledFeatures();
   }

   @Override
   public Scoreboard getScoreboard() {
      return this.connection().scoreboard();
   }

   @Nullable
   @Override
   public AdvancementHolder getAdvancement(Identifier id) {
      return this.connection().getAdvancements().get(id);
   }

   @Override
   public Level getUnsidedLevel() {
      return this.connection().getLevel();
   }

   @Override
   public MinecraftServer getServer() {
      throw new UnsupportedOperationException("Attempted to get server in client command");
   }

   @Override
   public ServerLevel getLevel() {
      throw new UnsupportedOperationException("Attempted to get server level in client command");
   }
}
