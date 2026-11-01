package net.minecraft.client.quickplay;

import com.mojang.realmsclient.RealmsMainScreen;
import com.mojang.realmsclient.client.RealmsClient;
import com.mojang.realmsclient.dto.RealmsServer;
import com.mojang.realmsclient.dto.RealmsServerList;
import com.mojang.realmsclient.exception.RealmsServiceException;
import com.mojang.realmsclient.gui.screens.RealmsLongRunningMcoTaskScreen;
import com.mojang.realmsclient.util.task.GetServerDetailsTask;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.main.GameConfig;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringUtil;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class QuickPlay {
   public static final Component ERROR_TITLE = Component.translatable("quickplay.error.title");
   private static final Component INVALID_IDENTIFIER = Component.translatable("quickplay.error.invalid_identifier");
   private static final Component REALM_CONNECT = Component.translatable("quickplay.error.realm_connect");
   private static final Component REALM_PERMISSION = Component.translatable("quickplay.error.realm_permission");
   private static final Component TO_TITLE = Component.translatable("gui.toTitle");
   private static final Component TO_WORLD_LIST = Component.translatable("gui.toWorld");
   private static final Component TO_REALMS_LIST = Component.translatable("gui.toRealms");

   public static void connect(Minecraft p_279319_, GameConfig.QuickPlayData p_279291_, RealmsClient p_279322_) {
      String $$3 = p_279291_.singleplayer();
      String $$4 = p_279291_.multiplayer();
      String $$5 = p_279291_.realms();
      if (!StringUtil.isBlank($$3)) {
         joinSingleplayerWorld(p_279319_, $$3);
      } else if (!StringUtil.isBlank($$4)) {
         joinMultiplayerWorld(p_279319_, $$4);
      } else if (!StringUtil.isBlank($$5)) {
         joinRealmsWorld(p_279319_, p_279322_, $$5);
      }
   }

   private static void joinSingleplayerWorld(Minecraft p_279420_, String p_279459_) {
      if (!p_279420_.getLevelSource().levelExists(p_279459_)) {
         Screen $$2 = new SelectWorldScreen(new TitleScreen());
         p_279420_.setScreen(new DisconnectedScreen($$2, ERROR_TITLE, INVALID_IDENTIFIER, TO_WORLD_LIST));
      } else {
         p_279420_.createWorldOpenFlows().openWorld(p_279459_, () -> p_279420_.setScreen(new TitleScreen()));
      }
   }

   private static void joinMultiplayerWorld(Minecraft p_279276_, String p_279128_) {
      ServerList $$2 = new ServerList(p_279276_);
      $$2.load();
      ServerData $$3 = $$2.get(p_279128_);
      if ($$3 == null) {
         $$3 = new ServerData(I18n.get("selectServer.defaultName"), p_279128_, ServerData.Type.OTHER);
         $$2.add($$3, true);
         $$2.save();
      }

      ServerAddress $$4 = ServerAddress.parseString(p_279128_);
      ConnectScreen.startConnecting(new JoinMultiplayerScreen(new TitleScreen()), p_279276_, $$4, $$3, true, null);
   }

   private static void joinRealmsWorld(Minecraft p_279320_, RealmsClient p_279468_, String p_279371_) {
      long $$3;
      RealmsServerList $$4;
      try {
         $$3 = Long.parseLong(p_279371_);
         $$4 = p_279468_.listRealms();
      } catch (NumberFormatException var9) {
         Screen $$6 = new RealmsMainScreen(new TitleScreen());
         p_279320_.setScreen(new DisconnectedScreen($$6, ERROR_TITLE, INVALID_IDENTIFIER, TO_REALMS_LIST));
         return;
      } catch (RealmsServiceException var10) {
         Screen $$8 = new TitleScreen();
         p_279320_.setScreen(new DisconnectedScreen($$8, ERROR_TITLE, REALM_CONNECT, TO_TITLE));
         return;
      }

      RealmsServer $$11 = $$4.servers.stream().filter(p_279424_ -> p_279424_.id == $$3).findFirst().orElse(null);
      if ($$11 == null) {
         Screen $$12 = new RealmsMainScreen(new TitleScreen());
         p_279320_.setScreen(new DisconnectedScreen($$12, ERROR_TITLE, REALM_PERMISSION, TO_REALMS_LIST));
      } else {
         TitleScreen $$13 = new TitleScreen();
         GetServerDetailsTask $$14 = new GetServerDetailsTask($$13, $$11);
         p_279320_.setScreen(new RealmsLongRunningMcoTaskScreen($$13, $$14));
      }
   }
}
