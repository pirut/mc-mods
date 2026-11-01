package net.neoforged.neoforge.event;

import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import it.unimi.dsi.fastutil.objects.ReferenceSet;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Stream;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.Event;
import org.jspecify.annotations.Nullable;

public class OnDatapackSyncEvent extends Event {
   private final PlayerList playerList;
   @Nullable
   private final ServerPlayer player;
   private final ReferenceSet<RecipeType<?>> recipeTypesToSend = new ReferenceOpenHashSet();

   public OnDatapackSyncEvent(PlayerList playerList, @Nullable ServerPlayer player) {
      this.playerList = playerList;
      this.player = player;
   }

   public PlayerList getPlayerList() {
      return this.playerList;
   }

   public Stream<ServerPlayer> getRelevantPlayers() {
      return this.player == null ? this.playerList.getPlayers().stream() : Stream.of(this.player);
   }

   @Nullable
   public ServerPlayer getPlayer() {
      return this.player;
   }

   public void sendRecipes(RecipeType<?>... recipeTypes) {
      Collections.addAll(this.recipeTypesToSend, recipeTypes);
   }

   public void sendRecipes(Iterable<RecipeType<?>> recipeTypes) {
      for (RecipeType<?> recipeType : recipeTypes) {
         this.recipeTypesToSend.add(recipeType);
      }
   }

   public Set<RecipeType<?>> getRecipeTypesToSend() {
      return Collections.unmodifiableSet(this.recipeTypesToSend);
   }
}
