package net.minecraft.client.gui.screens.social;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.mojang.authlib.GameProfile;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.multiplayer.chat.ChatLog;
import net.minecraft.client.multiplayer.chat.LoggedChatMessage;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SocialInteractionsPlayerList extends ContainerObjectSelectionList<PlayerEntry> {
   private final SocialInteractionsScreen socialInteractionsScreen;
   private final List<PlayerEntry> players = Lists.newArrayList();
   @Nullable
   private String filter;

   public SocialInteractionsPlayerList(SocialInteractionsScreen p_100697_, Minecraft p_100698_, int p_100699_, int p_100700_, int p_100701_, int p_100702_) {
      super(p_100698_, p_100699_, p_100700_, p_100701_, p_100702_);
      this.socialInteractionsScreen = p_100697_;
   }

   @Override
   protected void renderListBackground(GuiGraphics p_331377_) {
   }

   @Override
   protected void renderListSeparators(GuiGraphics p_331767_) {
   }

   @Override
   protected void enableScissor(GuiGraphics p_281892_) {
      p_281892_.enableScissor(this.getX(), this.getY() + 4, this.getRight(), this.getBottom());
   }

   public void updatePlayerList(Collection<UUID> p_240798_, double p_240792_, boolean p_240829_) {
      Map<UUID, PlayerEntry> $$3 = new HashMap<>();
      this.addOnlinePlayers(p_240798_, $$3);
      this.updatePlayersFromChatLog($$3, p_240829_);
      this.updateFiltersAndScroll($$3.values(), p_240792_);
   }

   private void addOnlinePlayers(Collection<UUID> p_240813_, Map<UUID, PlayerEntry> p_240796_) {
      ClientPacketListener $$2 = this.minecraft.player.connection;

      for (UUID $$3 : p_240813_) {
         PlayerInfo $$4 = $$2.getPlayerInfo($$3);
         if ($$4 != null) {
            boolean $$5 = $$4.hasVerifiableChat();
            p_240796_.put($$3, new PlayerEntry(this.minecraft, this.socialInteractionsScreen, $$3, $$4.getProfile().getName(), $$4::getSkin, $$5));
         }
      }
   }

   private void updatePlayersFromChatLog(Map<UUID, PlayerEntry> p_240780_, boolean p_240827_) {
      for (GameProfile $$3 : collectProfilesFromChatLog(this.minecraft.getReportingContext().chatLog())) {
         PlayerEntry $$4;
         if (p_240827_) {
            $$4 = p_240780_.computeIfAbsent(
               $$3.getId(),
               p_293608_ -> {
                  PlayerEntry $$2 = new PlayerEntry(
                     this.minecraft, this.socialInteractionsScreen, $$3.getId(), $$3.getName(), this.minecraft.getSkinManager().lookupInsecure($$3), true
                  );
                  $$2.setRemoved(true);
                  return $$2;
               }
            );
         } else {
            $$4 = p_240780_.get($$3.getId());
            if ($$4 == null) {
               continue;
            }
         }

         $$4.setHasRecentMessages(true);
      }
   }

   private static Collection<GameProfile> collectProfilesFromChatLog(ChatLog p_250748_) {
      Set<GameProfile> $$1 = new ObjectLinkedOpenHashSet();

      for (int $$2 = p_250748_.end(); $$2 >= p_250748_.start(); $$2--) {
         if (p_250748_.lookup($$2) instanceof LoggedChatMessage.Player $$4 && $$4.message().hasSignature()) {
            $$1.add($$4.profile());
         }
      }

      return $$1;
   }

   private void sortPlayerEntries() {
      this.players.sort(Comparator.<PlayerEntry, Integer>comparing(p_240744_ -> {
         if (this.minecraft.isLocalPlayer(p_240744_.getPlayerId())) {
            return 0;
         } else if (this.minecraft.getReportingContext().hasDraftReportFor(p_240744_.getPlayerId())) {
            return 1;
         } else if (p_240744_.getPlayerId().version() == 2) {
            return 4;
         } else {
            return p_240744_.hasRecentMessages() ? 2 : 3;
         }
      }).thenComparing(p_240745_ -> {
         if (!p_240745_.getPlayerName().isBlank()) {
            int $$1 = p_240745_.getPlayerName().codePointAt(0);
            if ($$1 == 95 || $$1 >= 97 && $$1 <= 122 || $$1 >= 65 && $$1 <= 90 || $$1 >= 48 && $$1 <= 57) {
               return 0;
            }
         }

         return 1;
      }).thenComparing(PlayerEntry::getPlayerName, String::compareToIgnoreCase));
   }

   private void updateFiltersAndScroll(Collection<PlayerEntry> p_240809_, double p_240830_) {
      this.players.clear();
      this.players.addAll(p_240809_);
      this.sortPlayerEntries();
      this.updateFilteredPlayers();
      this.replaceEntries(this.players);
      this.setScrollAmount(p_240830_);
   }

   private void updateFilteredPlayers() {
      if (this.filter != null) {
         this.players.removeIf(p_100710_ -> !p_100710_.getPlayerName().toLowerCase(Locale.ROOT).contains(this.filter));
         this.replaceEntries(this.players);
      }
   }

   public void setFilter(String p_100718_) {
      this.filter = p_100718_;
   }

   public boolean isEmpty() {
      return this.players.isEmpty();
   }

   public void addPlayer(PlayerInfo p_100715_, SocialInteractionsScreen.Page p_100716_) {
      UUID $$2 = p_100715_.getProfile().getId();

      for (PlayerEntry $$3 : this.players) {
         if ($$3.getPlayerId().equals($$2)) {
            $$3.setRemoved(false);
            return;
         }
      }

      if ((p_100716_ == SocialInteractionsScreen.Page.ALL || this.minecraft.getPlayerSocialManager().shouldHideMessageFrom($$2))
         && (Strings.isNullOrEmpty(this.filter) || p_100715_.getProfile().getName().toLowerCase(Locale.ROOT).contains(this.filter))) {
         boolean $$4 = p_100715_.hasVerifiableChat();
         PlayerEntry $$5 = new PlayerEntry(
            this.minecraft, this.socialInteractionsScreen, p_100715_.getProfile().getId(), p_100715_.getProfile().getName(), p_100715_::getSkin, $$4
         );
         this.addEntry($$5);
         this.players.add($$5);
      }
   }

   public void removePlayer(UUID p_100723_) {
      for (PlayerEntry $$1 : this.players) {
         if ($$1.getPlayerId().equals(p_100723_)) {
            $$1.setRemoved(true);
            return;
         }
      }
   }
}
