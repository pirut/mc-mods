package net.minecraft.client.gui.screens.social;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.mojang.authlib.GameProfile;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.multiplayer.chat.ChatLog;
import net.minecraft.client.multiplayer.chat.LoggedChatMessage;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class SocialInteractionsPlayerList extends ContainerObjectSelectionList<PlayerEntry> {
   private final SocialInteractionsScreen socialInteractionsScreen;
   private final List<PlayerEntry> players = Lists.newArrayList();
   @Nullable
   private String filter;

   public SocialInteractionsPlayerList(SocialInteractionsScreen socialInteractionsScreen, Minecraft minecraft, int width, int height, int y, int itemHeight) {
      super(minecraft, width, height, y, itemHeight);
      this.socialInteractionsScreen = socialInteractionsScreen;
   }

   @Override
   protected void renderListBackground(GuiGraphics guiGraphics) {
   }

   @Override
   protected void renderListSeparators(GuiGraphics guiGraphics) {
   }

   @Override
   protected void enableScissor(GuiGraphics guiGraphics) {
      guiGraphics.enableScissor(this.getX(), this.getY() + 4, this.getRight(), this.getBottom());
   }

   public void updatePlayerList(Collection<UUID> ids, double scrollAmount, boolean addChatLogPlayers) {
      Map<UUID, PlayerEntry> map = new HashMap<>();
      this.addOnlinePlayers(ids, map);
      if (addChatLogPlayers) {
         this.addSeenPlayers(map);
      }

      this.updatePlayersFromChatLog(map, addChatLogPlayers);
      this.updateFiltersAndScroll(map.values(), scrollAmount);
   }

   private void addOnlinePlayers(Collection<UUID> ids, Map<UUID, PlayerEntry> playerMap) {
      ClientPacketListener clientPacketListener = this.minecraft.player.connection;

      for (UUID uUID : ids) {
         PlayerInfo playerInfo = clientPacketListener.getPlayerInfo(uUID);
         if (playerInfo != null) {
            PlayerEntry playerEntry = this.makePlayerEntry(uUID, playerInfo);
            playerMap.put(uUID, playerEntry);
         }
      }
   }

   private void addSeenPlayers(Map<UUID, PlayerEntry> players) {
      Map<UUID, PlayerInfo> map = this.minecraft.player.connection.getSeenPlayers();

      for (Map.Entry<UUID, PlayerInfo> entry : map.entrySet()) {
         players.computeIfAbsent(entry.getKey(), uUID -> {
            PlayerEntry playerEntry = this.makePlayerEntry(uUID, entry.getValue());
            playerEntry.setRemoved(true);
            return playerEntry;
         });
      }
   }

   private PlayerEntry makePlayerEntry(UUID uuid, PlayerInfo playerInfo) {
      return new PlayerEntry(
         this.minecraft, this.socialInteractionsScreen, uuid, playerInfo.getProfile().name(), playerInfo::getSkin, playerInfo.hasVerifiableChat()
      );
   }

   private void updatePlayersFromChatLog(Map<UUID, PlayerEntry> playerMap, boolean addPlayers) {
      Map<UUID, GameProfile> map = collectProfilesFromChatLog(this.minecraft.getReportingContext().chatLog());
      map.forEach(
         (uUID, gameProfile) -> {
            PlayerEntry playerEntry;
            if (addPlayers) {
               playerEntry = playerMap.computeIfAbsent(
                  uUID,
                  uUIDx -> {
                     PlayerEntry playerEntryx = new PlayerEntry(
                        this.minecraft,
                        this.socialInteractionsScreen,
                        gameProfile.id(),
                        gameProfile.name(),
                        this.minecraft.getSkinManager().createLookup(gameProfile, true),
                        true
                     );
                     playerEntryx.setRemoved(true);
                     return playerEntryx;
                  }
               );
            } else {
               playerEntry = playerMap.get(uUID);
               if (playerEntry == null) {
                  return;
               }
            }

            playerEntry.setHasRecentMessages(true);
         }
      );
   }

   private static Map<UUID, GameProfile> collectProfilesFromChatLog(ChatLog chatLog) {
      Map<UUID, GameProfile> map = new Object2ObjectLinkedOpenHashMap();

      for (int i = chatLog.end(); i >= chatLog.start(); i--) {
         if (chatLog.lookup(i) instanceof LoggedChatMessage.Player player && player.message().hasSignature()) {
            map.put(player.profileId(), player.profile());
         }
      }

      return map;
   }

   private void sortPlayerEntries() {
      this.players.sort(Comparator.<PlayerEntry, Integer>comparing(playerEntry -> {
         if (this.minecraft.isLocalPlayer(playerEntry.getPlayerId())) {
            return 0;
         } else if (this.minecraft.getReportingContext().hasDraftReportFor(playerEntry.getPlayerId())) {
            return 1;
         } else if (playerEntry.getPlayerId().version() == 2) {
            return 4;
         } else {
            return playerEntry.hasRecentMessages() ? 2 : 3;
         }
      }).thenComparing(playerEntry -> {
         if (!playerEntry.getPlayerName().isBlank()) {
            int i = playerEntry.getPlayerName().codePointAt(0);
            if (i == 95 || i >= 97 && i <= 122 || i >= 65 && i <= 90 || i >= 48 && i <= 57) {
               return 0;
            }
         }

         return 1;
      }).thenComparing(PlayerEntry::getPlayerName, String::compareToIgnoreCase));
   }

   private void updateFiltersAndScroll(Collection<PlayerEntry> players, double scrollAmount) {
      this.players.clear();
      this.players.addAll(players);
      this.sortPlayerEntries();
      this.updateFilteredPlayers();
      this.replaceEntries(this.players);
      this.setScrollAmount(scrollAmount);
   }

   private void updateFilteredPlayers() {
      if (this.filter != null) {
         this.players.removeIf(playerEntry -> !playerEntry.getPlayerName().toLowerCase(Locale.ROOT).contains(this.filter));
         this.replaceEntries(this.players);
      }
   }

   public void setFilter(String filter) {
      this.filter = filter;
   }

   public boolean isEmpty() {
      return this.players.isEmpty();
   }

   public void addPlayer(PlayerInfo playerInfo, SocialInteractionsScreen.Page page) {
      UUID uUID = playerInfo.getProfile().id();

      for (PlayerEntry playerEntry : this.players) {
         if (playerEntry.getPlayerId().equals(uUID)) {
            playerEntry.setRemoved(false);
            return;
         }
      }

      if ((page == SocialInteractionsScreen.Page.ALL || this.minecraft.getPlayerSocialManager().shouldHideMessageFrom(uUID))
         && (Strings.isNullOrEmpty(this.filter) || playerInfo.getProfile().name().toLowerCase(Locale.ROOT).contains(this.filter))) {
         boolean bl = playerInfo.hasVerifiableChat();
         PlayerEntry playerEntryx = new PlayerEntry(
            this.minecraft, this.socialInteractionsScreen, playerInfo.getProfile().id(), playerInfo.getProfile().name(), playerInfo::getSkin, bl
         );
         this.addEntry(playerEntryx);
         this.players.add(playerEntryx);
      }
   }

   public void removePlayer(UUID id) {
      for (PlayerEntry playerEntry : this.players) {
         if (playerEntry.getPlayerId().equals(id)) {
            playerEntry.setRemoved(true);
            return;
         }
      }
   }

   public void refreshHasDraftReport() {
      this.players.forEach(playerEntry -> playerEntry.refreshHasDraftReport(this.minecraft.getReportingContext()));
   }
}
