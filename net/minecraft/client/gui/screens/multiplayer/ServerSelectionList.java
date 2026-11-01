package net.minecraft.client.gui.screens.multiplayer;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.DefaultUncaughtExceptionHandler;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.FaviconTexture;
import net.minecraft.client.gui.screens.LoadingDotsText;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.client.server.LanServer;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class ServerSelectionList extends ObjectSelectionList<ServerSelectionList.Entry> {
   static final ResourceLocation INCOMPATIBLE_SPRITE = ResourceLocation.withDefaultNamespace("server_list/incompatible");
   static final ResourceLocation UNREACHABLE_SPRITE = ResourceLocation.withDefaultNamespace("server_list/unreachable");
   static final ResourceLocation PING_1_SPRITE = ResourceLocation.withDefaultNamespace("server_list/ping_1");
   static final ResourceLocation PING_2_SPRITE = ResourceLocation.withDefaultNamespace("server_list/ping_2");
   static final ResourceLocation PING_3_SPRITE = ResourceLocation.withDefaultNamespace("server_list/ping_3");
   static final ResourceLocation PING_4_SPRITE = ResourceLocation.withDefaultNamespace("server_list/ping_4");
   static final ResourceLocation PING_5_SPRITE = ResourceLocation.withDefaultNamespace("server_list/ping_5");
   static final ResourceLocation PINGING_1_SPRITE = ResourceLocation.withDefaultNamespace("server_list/pinging_1");
   static final ResourceLocation PINGING_2_SPRITE = ResourceLocation.withDefaultNamespace("server_list/pinging_2");
   static final ResourceLocation PINGING_3_SPRITE = ResourceLocation.withDefaultNamespace("server_list/pinging_3");
   static final ResourceLocation PINGING_4_SPRITE = ResourceLocation.withDefaultNamespace("server_list/pinging_4");
   static final ResourceLocation PINGING_5_SPRITE = ResourceLocation.withDefaultNamespace("server_list/pinging_5");
   static final ResourceLocation JOIN_HIGHLIGHTED_SPRITE = ResourceLocation.withDefaultNamespace("server_list/join_highlighted");
   static final ResourceLocation JOIN_SPRITE = ResourceLocation.withDefaultNamespace("server_list/join");
   static final ResourceLocation MOVE_UP_HIGHLIGHTED_SPRITE = ResourceLocation.withDefaultNamespace("server_list/move_up_highlighted");
   static final ResourceLocation MOVE_UP_SPRITE = ResourceLocation.withDefaultNamespace("server_list/move_up");
   static final ResourceLocation MOVE_DOWN_HIGHLIGHTED_SPRITE = ResourceLocation.withDefaultNamespace("server_list/move_down_highlighted");
   static final ResourceLocation MOVE_DOWN_SPRITE = ResourceLocation.withDefaultNamespace("server_list/move_down");
   static final Logger LOGGER = LogUtils.getLogger();
   static final ThreadPoolExecutor THREAD_POOL = new ScheduledThreadPoolExecutor(
      5,
      new ThreadFactoryBuilder()
         .setNameFormat("Server Pinger #%d")
         .setDaemon(true)
         .setUncaughtExceptionHandler(new DefaultUncaughtExceptionHandler(LOGGER))
         .build()
   );
   static final Component SCANNING_LABEL = Component.translatable("lanServer.scanning");
   static final Component CANT_RESOLVE_TEXT = Component.translatable("multiplayer.status.cannot_resolve").withColor(-65536);
   static final Component CANT_CONNECT_TEXT = Component.translatable("multiplayer.status.cannot_connect").withColor(-65536);
   static final Component INCOMPATIBLE_STATUS = Component.translatable("multiplayer.status.incompatible");
   static final Component NO_CONNECTION_STATUS = Component.translatable("multiplayer.status.no_connection");
   static final Component PINGING_STATUS = Component.translatable("multiplayer.status.pinging");
   static final Component ONLINE_STATUS = Component.translatable("multiplayer.status.online");
   private final JoinMultiplayerScreen screen;
   private final List<ServerSelectionList.OnlineServerEntry> onlineServers = Lists.newArrayList();
   private final ServerSelectionList.Entry lanHeader = new ServerSelectionList.LANHeader();
   private final List<ServerSelectionList.NetworkServerEntry> networkServers = Lists.newArrayList();

   public ServerSelectionList(JoinMultiplayerScreen p_99771_, Minecraft p_99772_, int p_99773_, int p_99774_, int p_99775_, int p_99776_) {
      super(p_99772_, p_99773_, p_99774_, p_99775_, p_99776_);
      this.screen = p_99771_;
   }

   private void refreshEntries() {
      this.clearEntries();
      this.onlineServers.forEach(p_169979_ -> this.addEntry(p_169979_));
      this.addEntry(this.lanHeader);
      this.networkServers.forEach(p_169976_ -> this.addEntry(p_169976_));
   }

   public void setSelected(@Nullable ServerSelectionList.Entry p_99790_) {
      super.setSelected(p_99790_);
      this.screen.onSelectedChange();
   }

   @Override
   public boolean keyPressed(int p_99782_, int p_99783_, int p_99784_) {
      ServerSelectionList.Entry $$3 = this.getSelected();
      return $$3 != null && $$3.keyPressed(p_99782_, p_99783_, p_99784_) || super.keyPressed(p_99782_, p_99783_, p_99784_);
   }

   public void updateOnlineServers(ServerList p_99798_) {
      this.onlineServers.clear();

      for (int $$1 = 0; $$1 < p_99798_.size(); $$1++) {
         this.onlineServers.add(new ServerSelectionList.OnlineServerEntry(this.screen, p_99798_.get($$1)));
      }

      this.refreshEntries();
   }

   public void updateNetworkServers(List<LanServer> p_99800_) {
      int $$1 = p_99800_.size() - this.networkServers.size();
      this.networkServers.clear();

      for (LanServer $$2 : p_99800_) {
         this.networkServers.add(new ServerSelectionList.NetworkServerEntry(this.screen, $$2));
      }

      this.refreshEntries();

      for (int $$3 = this.networkServers.size() - $$1; $$3 < this.networkServers.size(); $$3++) {
         ServerSelectionList.NetworkServerEntry $$4 = this.networkServers.get($$3);
         int $$5 = $$3 - this.networkServers.size() + this.children().size();
         int $$6 = this.getRowTop($$5);
         int $$7 = this.getRowBottom($$5);
         if ($$7 >= this.getY() && $$6 <= this.getBottom()) {
            this.minecraft.getNarrator().say(Component.translatable("multiplayer.lan.server_found", $$4.getServerNarration()));
         }
      }
   }

   @Override
   public int getRowWidth() {
      return 305;
   }

   public void removed() {
   }

   @OnlyIn(Dist.CLIENT)
   public abstract static class Entry extends ObjectSelectionList.Entry<ServerSelectionList.Entry> implements AutoCloseable {
      @Override
      public void close() {
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class LANHeader extends ServerSelectionList.Entry {
      private final Minecraft minecraft = Minecraft.getInstance();

      @Override
      public void render(
         GuiGraphics p_281475_,
         int p_282477_,
         int p_282819_,
         int p_282001_,
         int p_281911_,
         int p_283126_,
         int p_282303_,
         int p_281998_,
         boolean p_282625_,
         float p_281811_
      ) {
         int $$10 = p_282819_ + p_283126_ / 2 - 9 / 2;
         p_281475_.drawString(
            this.minecraft.font,
            ServerSelectionList.SCANNING_LABEL,
            this.minecraft.screen.width / 2 - this.minecraft.font.width(ServerSelectionList.SCANNING_LABEL) / 2,
            $$10,
            16777215,
            false
         );
         String $$11 = LoadingDotsText.get(Util.getMillis());
         p_281475_.drawString(this.minecraft.font, $$11, this.minecraft.screen.width / 2 - this.minecraft.font.width($$11) / 2, $$10 + 9, -8355712, false);
      }

      @Override
      public Component getNarration() {
         return ServerSelectionList.SCANNING_LABEL;
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class NetworkServerEntry extends ServerSelectionList.Entry {
      private static final int ICON_WIDTH = 32;
      private static final Component LAN_SERVER_HEADER = Component.translatable("lanServer.title");
      private static final Component HIDDEN_ADDRESS_TEXT = Component.translatable("selectServer.hiddenAddress");
      private final JoinMultiplayerScreen screen;
      protected final Minecraft minecraft;
      protected final LanServer serverData;
      private long lastClickTime;

      protected NetworkServerEntry(JoinMultiplayerScreen p_99836_, LanServer p_99837_) {
         this.screen = p_99836_;
         this.serverData = p_99837_;
         this.minecraft = Minecraft.getInstance();
      }

      @Override
      public void render(
         GuiGraphics p_282600_,
         int p_282649_,
         int p_283641_,
         int p_282277_,
         int p_283034_,
         int p_281533_,
         int p_282746_,
         int p_281454_,
         boolean p_283673_,
         float p_282694_
      ) {
         p_282600_.drawString(this.minecraft.font, LAN_SERVER_HEADER, p_282277_ + 32 + 3, p_283641_ + 1, 16777215, false);
         p_282600_.drawString(this.minecraft.font, this.serverData.getMotd(), p_282277_ + 32 + 3, p_283641_ + 12, -8355712, false);
         if (this.minecraft.options.hideServerAddress) {
            p_282600_.drawString(this.minecraft.font, HIDDEN_ADDRESS_TEXT, p_282277_ + 32 + 3, p_283641_ + 12 + 11, 3158064, false);
         } else {
            p_282600_.drawString(this.minecraft.font, this.serverData.getAddress(), p_282277_ + 32 + 3, p_283641_ + 12 + 11, 3158064, false);
         }
      }

      @Override
      public boolean mouseClicked(double p_99840_, double p_99841_, int p_99842_) {
         this.screen.setSelected(this);
         if (Util.getMillis() - this.lastClickTime < 250L) {
            this.screen.joinSelectedServer();
         }

         this.lastClickTime = Util.getMillis();
         return super.mouseClicked(p_99840_, p_99841_, p_99842_);
      }

      public LanServer getServerData() {
         return this.serverData;
      }

      @Override
      public Component getNarration() {
         return Component.translatable("narrator.select", this.getServerNarration());
      }

      public Component getServerNarration() {
         return Component.empty().append(LAN_SERVER_HEADER).append(CommonComponents.SPACE).append(this.serverData.getMotd());
      }
   }

   @OnlyIn(Dist.CLIENT)
   public class OnlineServerEntry extends ServerSelectionList.Entry {
      private static final int ICON_WIDTH = 32;
      private static final int ICON_HEIGHT = 32;
      private static final int SPACING = 5;
      private static final int STATUS_ICON_WIDTH = 10;
      private static final int STATUS_ICON_HEIGHT = 8;
      private final JoinMultiplayerScreen screen;
      private final Minecraft minecraft;
      private final ServerData serverData;
      private final FaviconTexture icon;
      @Nullable
      private byte[] lastIconBytes;
      private long lastClickTime;
      @Nullable
      private List<Component> onlinePlayersTooltip;
      @Nullable
      private ResourceLocation statusIcon;
      @Nullable
      private Component statusIconTooltip;

      protected OnlineServerEntry(JoinMultiplayerScreen p_99864_, ServerData p_99865_) {
         this.screen = p_99864_;
         this.serverData = p_99865_;
         this.minecraft = Minecraft.getInstance();
         this.icon = FaviconTexture.forServer(this.minecraft.getTextureManager(), p_99865_.ip);
         this.refreshStatus();
      }

      @Override
      public void render(
         GuiGraphics p_281406_,
         int p_281506_,
         int p_282921_,
         int p_281363_,
         int p_283596_,
         int p_281630_,
         int p_283567_,
         int p_282864_,
         boolean p_282999_,
         float p_281423_
      ) {
         if (this.serverData.state() == ServerData.State.INITIAL) {
            this.serverData.setState(ServerData.State.PINGING);
            this.serverData.motd = CommonComponents.EMPTY;
            this.serverData.status = CommonComponents.EMPTY;
            ServerSelectionList.THREAD_POOL
               .submit(
                  () -> {
                     try {
                        this.screen
                           .getPinger()
                           .pingServer(
                              this.serverData,
                              () -> this.minecraft.execute(this::updateServerList),
                              () -> {
                                 this.serverData
                                    .setState(
                                       this.serverData.protocol == SharedConstants.getCurrentVersion().getProtocolVersion()
                                          ? ServerData.State.SUCCESSFUL
                                          : ServerData.State.INCOMPATIBLE
                                    );
                                 this.minecraft.execute(this::refreshStatus);
                              }
                           );
                     } catch (UnknownHostException var2x) {
                        this.serverData.setState(ServerData.State.UNREACHABLE);
                        this.serverData.motd = ServerSelectionList.CANT_RESOLVE_TEXT;
                        this.minecraft.execute(this::refreshStatus);
                     } catch (Exception var3x) {
                        this.serverData.setState(ServerData.State.UNREACHABLE);
                        this.serverData.motd = ServerSelectionList.CANT_CONNECT_TEXT;
                        this.minecraft.execute(this::refreshStatus);
                     }
                  }
               );
         }

         p_281406_.drawString(this.minecraft.font, this.serverData.name, p_281363_ + 32 + 3, p_282921_ + 1, 16777215, false);
         List<FormattedCharSequence> $$10 = this.minecraft.font.split(this.serverData.motd, p_283596_ - 32 - 2);

         for (int $$11 = 0; $$11 < Math.min($$10.size(), 2); $$11++) {
            p_281406_.drawString(this.minecraft.font, $$10.get($$11), p_281363_ + 32 + 3, p_282921_ + 12 + 9 * $$11, -8355712, false);
         }

         this.drawIcon(p_281406_, p_281363_, p_282921_, this.icon.textureLocation());
         if (this.serverData.state() == ServerData.State.PINGING) {
            int $$12 = (int)(Util.getMillis() / 100L + p_281506_ * 2 & 7L);
            if ($$12 > 4) {
               $$12 = 8 - $$12;
            }
            this.statusIcon = switch ($$12) {
               case 1 -> ServerSelectionList.PINGING_2_SPRITE;
               case 2 -> ServerSelectionList.PINGING_3_SPRITE;
               case 3 -> ServerSelectionList.PINGING_4_SPRITE;
               case 4 -> ServerSelectionList.PINGING_5_SPRITE;
               default -> ServerSelectionList.PINGING_1_SPRITE;
            };
         }

         int $$13 = p_281363_ + p_283596_ - 10 - 5;
         if (this.statusIcon != null) {
            p_281406_.blitSprite(this.statusIcon, $$13, p_282921_, 10, 8);
         }

         byte[] $$14 = this.serverData.getIconBytes();
         if (!Arrays.equals($$14, this.lastIconBytes)) {
            if (this.uploadServerIcon($$14)) {
               this.lastIconBytes = $$14;
            } else {
               this.serverData.setIconBytes(null);
               this.updateServerList();
            }
         }

         Component $$15 = (Component)(this.serverData.state() == ServerData.State.INCOMPATIBLE
            ? this.serverData.version.copy().withStyle(ChatFormatting.RED)
            : this.serverData.status);
         int $$16 = this.minecraft.font.width($$15);
         int $$17 = $$13 - $$16 - 5;
         p_281406_.drawString(this.minecraft.font, $$15, $$17, p_282921_ + 1, -8355712, false);
         if (this.statusIconTooltip != null && p_283567_ >= $$13 && p_283567_ <= $$13 + 10 && p_282864_ >= p_282921_ && p_282864_ <= p_282921_ + 8) {
            this.screen.setTooltipForNextRenderPass(this.statusIconTooltip);
         } else if (this.onlinePlayersTooltip != null
            && p_283567_ >= $$17
            && p_283567_ <= $$17 + $$16
            && p_282864_ >= p_282921_
            && p_282864_ <= p_282921_ - 1 + 9) {
            this.screen.setTooltipForNextRenderPass(Lists.transform(this.onlinePlayersTooltip, Component::getVisualOrderText));
         }

         if (this.minecraft.options.touchscreen().get() || p_282999_) {
            p_281406_.fill(p_281363_, p_282921_, p_281363_ + 32, p_282921_ + 32, -1601138544);
            int $$18 = p_283567_ - p_281363_;
            int $$19 = p_282864_ - p_282921_;
            if (this.canJoin()) {
               if ($$18 < 32 && $$18 > 16) {
                  p_281406_.blitSprite(ServerSelectionList.JOIN_HIGHLIGHTED_SPRITE, p_281363_, p_282921_, 32, 32);
               } else {
                  p_281406_.blitSprite(ServerSelectionList.JOIN_SPRITE, p_281363_, p_282921_, 32, 32);
               }
            }

            if (p_281506_ > 0) {
               if ($$18 < 16 && $$19 < 16) {
                  p_281406_.blitSprite(ServerSelectionList.MOVE_UP_HIGHLIGHTED_SPRITE, p_281363_, p_282921_, 32, 32);
               } else {
                  p_281406_.blitSprite(ServerSelectionList.MOVE_UP_SPRITE, p_281363_, p_282921_, 32, 32);
               }
            }

            if (p_281506_ < this.screen.getServers().size() - 1) {
               if ($$18 < 16 && $$19 > 16) {
                  p_281406_.blitSprite(ServerSelectionList.MOVE_DOWN_HIGHLIGHTED_SPRITE, p_281363_, p_282921_, 32, 32);
               } else {
                  p_281406_.blitSprite(ServerSelectionList.MOVE_DOWN_SPRITE, p_281363_, p_282921_, 32, 32);
               }
            }
         }
      }

      private void refreshStatus() {
         this.onlinePlayersTooltip = null;
         switch (this.serverData.state()) {
            case INITIAL:
            case PINGING:
               this.statusIcon = ServerSelectionList.PING_1_SPRITE;
               this.statusIconTooltip = ServerSelectionList.PINGING_STATUS;
               break;
            case INCOMPATIBLE:
               this.statusIcon = ServerSelectionList.INCOMPATIBLE_SPRITE;
               this.statusIconTooltip = ServerSelectionList.INCOMPATIBLE_STATUS;
               this.onlinePlayersTooltip = this.serverData.playerList;
               break;
            case UNREACHABLE:
               this.statusIcon = ServerSelectionList.UNREACHABLE_SPRITE;
               this.statusIconTooltip = ServerSelectionList.NO_CONNECTION_STATUS;
               break;
            case SUCCESSFUL:
               if (this.serverData.ping < 150L) {
                  this.statusIcon = ServerSelectionList.PING_5_SPRITE;
               } else if (this.serverData.ping < 300L) {
                  this.statusIcon = ServerSelectionList.PING_4_SPRITE;
               } else if (this.serverData.ping < 600L) {
                  this.statusIcon = ServerSelectionList.PING_3_SPRITE;
               } else if (this.serverData.ping < 1000L) {
                  this.statusIcon = ServerSelectionList.PING_2_SPRITE;
               } else {
                  this.statusIcon = ServerSelectionList.PING_1_SPRITE;
               }

               this.statusIconTooltip = Component.translatable("multiplayer.status.ping", this.serverData.ping);
               this.onlinePlayersTooltip = this.serverData.playerList;
         }
      }

      public void updateServerList() {
         this.screen.getServers().save();
      }

      protected void drawIcon(GuiGraphics p_281338_, int p_283001_, int p_282834_, ResourceLocation p_282534_) {
         RenderSystem.enableBlend();
         p_281338_.blit(p_282534_, p_283001_, p_282834_, 0.0F, 0.0F, 32, 32, 32, 32);
         RenderSystem.disableBlend();
      }

      private boolean canJoin() {
         return true;
      }

      private boolean uploadServerIcon(@Nullable byte[] p_273176_) {
         if (p_273176_ == null) {
            this.icon.clear();
         } else {
            try {
               this.icon.upload(NativeImage.read(p_273176_));
            } catch (Throwable var3) {
               ServerSelectionList.LOGGER.error("Invalid icon for server {} ({})", new Object[]{this.serverData.name, this.serverData.ip, var3});
               return false;
            }
         }

         return true;
      }

      @Override
      public boolean keyPressed(int p_99875_, int p_99876_, int p_99877_) {
         if (Screen.hasShiftDown()) {
            ServerSelectionList $$3 = this.screen.serverSelectionList;
            int $$4 = $$3.children().indexOf(this);
            if ($$4 == -1) {
               return true;
            }

            if (p_99875_ == 264 && $$4 < this.screen.getServers().size() - 1 || p_99875_ == 265 && $$4 > 0) {
               this.swap($$4, p_99875_ == 264 ? $$4 + 1 : $$4 - 1);
               return true;
            }
         }

         return super.keyPressed(p_99875_, p_99876_, p_99877_);
      }

      private void swap(int p_99872_, int p_99873_) {
         this.screen.getServers().swap(p_99872_, p_99873_);
         this.screen.serverSelectionList.updateOnlineServers(this.screen.getServers());
         ServerSelectionList.Entry $$2 = this.screen.serverSelectionList.children().get(p_99873_);
         this.screen.serverSelectionList.setSelected($$2);
         ServerSelectionList.this.ensureVisible($$2);
      }

      @Override
      public boolean mouseClicked(double p_99868_, double p_99869_, int p_99870_) {
         double $$3 = p_99868_ - ServerSelectionList.this.getRowLeft();
         double $$4 = p_99869_ - ServerSelectionList.this.getRowTop(ServerSelectionList.this.children().indexOf(this));
         if ($$3 <= 32.0) {
            if ($$3 < 32.0 && $$3 > 16.0 && this.canJoin()) {
               this.screen.setSelected(this);
               this.screen.joinSelectedServer();
               return true;
            }

            int $$5 = this.screen.serverSelectionList.children().indexOf(this);
            if ($$3 < 16.0 && $$4 < 16.0 && $$5 > 0) {
               this.swap($$5, $$5 - 1);
               return true;
            }

            if ($$3 < 16.0 && $$4 > 16.0 && $$5 < this.screen.getServers().size() - 1) {
               this.swap($$5, $$5 + 1);
               return true;
            }
         }

         this.screen.setSelected(this);
         if (Util.getMillis() - this.lastClickTime < 250L) {
            this.screen.joinSelectedServer();
         }

         this.lastClickTime = Util.getMillis();
         return super.mouseClicked(p_99868_, p_99869_, p_99870_);
      }

      public ServerData getServerData() {
         return this.serverData;
      }

      @Override
      public Component getNarration() {
         MutableComponent $$0 = Component.empty();
         $$0.append(Component.translatable("narrator.select", this.serverData.name));
         $$0.append(CommonComponents.NARRATION_SEPARATOR);
         switch (this.serverData.state()) {
            case PINGING:
               $$0.append(ServerSelectionList.PINGING_STATUS);
               break;
            case INCOMPATIBLE:
               $$0.append(ServerSelectionList.INCOMPATIBLE_STATUS);
               $$0.append(CommonComponents.NARRATION_SEPARATOR);
               $$0.append(Component.translatable("multiplayer.status.version.narration", this.serverData.version));
               $$0.append(CommonComponents.NARRATION_SEPARATOR);
               $$0.append(Component.translatable("multiplayer.status.motd.narration", this.serverData.motd));
               break;
            case UNREACHABLE:
               $$0.append(ServerSelectionList.NO_CONNECTION_STATUS);
               break;
            default:
               $$0.append(ServerSelectionList.ONLINE_STATUS);
               $$0.append(CommonComponents.NARRATION_SEPARATOR);
               $$0.append(Component.translatable("multiplayer.status.ping.narration", this.serverData.ping));
               $$0.append(CommonComponents.NARRATION_SEPARATOR);
               $$0.append(Component.translatable("multiplayer.status.motd.narration", this.serverData.motd));
               if (this.serverData.players != null) {
                  $$0.append(CommonComponents.NARRATION_SEPARATOR);
                  $$0.append(
                     Component.translatable("multiplayer.status.player_count.narration", this.serverData.players.online(), this.serverData.players.max())
                  );
                  $$0.append(CommonComponents.NARRATION_SEPARATOR);
                  $$0.append(ComponentUtils.formatList(this.serverData.playerList, Component.literal(", ")));
               }
         }

         return $$0;
      }

      @Override
      public void close() {
         this.icon.close();
      }
   }
}
