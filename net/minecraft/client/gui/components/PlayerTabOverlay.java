package net.minecraft.client.gui.components;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.Optionull;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.numbers.NumberFormat;
import net.minecraft.network.chat.numbers.StyledFormat;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.level.GameType;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.ReadOnlyScoreInfo;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class PlayerTabOverlay {
   private static final ResourceLocation PING_UNKNOWN_SPRITE = ResourceLocation.withDefaultNamespace("icon/ping_unknown");
   private static final ResourceLocation PING_1_SPRITE = ResourceLocation.withDefaultNamespace("icon/ping_1");
   private static final ResourceLocation PING_2_SPRITE = ResourceLocation.withDefaultNamespace("icon/ping_2");
   private static final ResourceLocation PING_3_SPRITE = ResourceLocation.withDefaultNamespace("icon/ping_3");
   private static final ResourceLocation PING_4_SPRITE = ResourceLocation.withDefaultNamespace("icon/ping_4");
   private static final ResourceLocation PING_5_SPRITE = ResourceLocation.withDefaultNamespace("icon/ping_5");
   private static final ResourceLocation HEART_CONTAINER_BLINKING_SPRITE = ResourceLocation.withDefaultNamespace("hud/heart/container_blinking");
   private static final ResourceLocation HEART_CONTAINER_SPRITE = ResourceLocation.withDefaultNamespace("hud/heart/container");
   private static final ResourceLocation HEART_FULL_BLINKING_SPRITE = ResourceLocation.withDefaultNamespace("hud/heart/full_blinking");
   private static final ResourceLocation HEART_HALF_BLINKING_SPRITE = ResourceLocation.withDefaultNamespace("hud/heart/half_blinking");
   private static final ResourceLocation HEART_ABSORBING_FULL_BLINKING_SPRITE = ResourceLocation.withDefaultNamespace("hud/heart/absorbing_full_blinking");
   private static final ResourceLocation HEART_FULL_SPRITE = ResourceLocation.withDefaultNamespace("hud/heart/full");
   private static final ResourceLocation HEART_ABSORBING_HALF_BLINKING_SPRITE = ResourceLocation.withDefaultNamespace("hud/heart/absorbing_half_blinking");
   private static final ResourceLocation HEART_HALF_SPRITE = ResourceLocation.withDefaultNamespace("hud/heart/half");
   private static final Comparator<PlayerInfo> PLAYER_COMPARATOR = Comparator.<PlayerInfo>comparingInt(
         p_253306_ -> p_253306_.getGameMode() == GameType.SPECTATOR ? 1 : 0
      )
      .thenComparing(p_269613_ -> Optionull.mapOrDefault(p_269613_.getTeam(), PlayerTeam::getName, ""))
      .thenComparing(p_253305_ -> p_253305_.getProfile().getName(), String::compareToIgnoreCase);
   public static final int MAX_ROWS_PER_COL = 20;
   private final Minecraft minecraft;
   private final Gui gui;
   @Nullable
   private Component footer;
   @Nullable
   private Component header;
   private boolean visible;
   private final Map<UUID, PlayerTabOverlay.HealthState> healthStates = new Object2ObjectOpenHashMap();

   public PlayerTabOverlay(Minecraft p_94527_, Gui p_94528_) {
      this.minecraft = p_94527_;
      this.gui = p_94528_;
   }

   public Component getNameForDisplay(PlayerInfo p_94550_) {
      return p_94550_.getTabListDisplayName() != null
         ? this.decorateName(p_94550_, p_94550_.getTabListDisplayName().copy())
         : this.decorateName(p_94550_, PlayerTeam.formatNameForTeam(p_94550_.getTeam(), Component.literal(p_94550_.getProfile().getName())));
   }

   private Component decorateName(PlayerInfo p_94552_, MutableComponent p_94553_) {
      return p_94552_.getGameMode() == GameType.SPECTATOR ? p_94553_.withStyle(ChatFormatting.ITALIC) : p_94553_;
   }

   public void setVisible(boolean p_94557_) {
      if (this.visible != p_94557_) {
         this.healthStates.clear();
         this.visible = p_94557_;
         if (p_94557_) {
            Component $$1 = ComponentUtils.formatList(this.getPlayerInfos(), Component.literal(", "), this::getNameForDisplay);
            this.minecraft.getNarrator().sayNow(Component.translatable("multiplayer.player.list.narration", $$1));
         }
      }
   }

   private List<PlayerInfo> getPlayerInfos() {
      return this.minecraft.player.connection.getListedOnlinePlayers().stream().sorted(PLAYER_COMPARATOR).limit(80L).toList();
   }

   public void render(GuiGraphics p_281484_, int p_283602_, Scoreboard p_282338_, @Nullable Objective p_282369_) {
      List<PlayerInfo> $$4 = this.getPlayerInfos();
      List<PlayerTabOverlay.ScoreDisplayEntry> $$5 = new ArrayList<>($$4.size());
      int $$6 = this.minecraft.font.width(" ");
      int $$7 = 0;
      int $$8 = 0;

      for (PlayerInfo $$9 : $$4) {
         Component $$10 = this.getNameForDisplay($$9);
         $$7 = Math.max($$7, this.minecraft.font.width($$10));
         int $$11 = 0;
         Component $$12 = null;
         int $$13 = 0;
         if (p_282369_ != null) {
            ScoreHolder $$14 = ScoreHolder.fromGameProfile($$9.getProfile());
            ReadOnlyScoreInfo $$15 = p_282338_.getPlayerScoreInfo($$14, p_282369_);
            if ($$15 != null) {
               $$11 = $$15.value();
            }

            if (p_282369_.getRenderType() != ObjectiveCriteria.RenderType.HEARTS) {
               NumberFormat $$16 = p_282369_.numberFormatOrDefault(StyledFormat.PLAYER_LIST_DEFAULT);
               $$12 = ReadOnlyScoreInfo.safeFormatValue($$15, $$16);
               $$13 = this.minecraft.font.width($$12);
               $$8 = Math.max($$8, $$13 > 0 ? $$6 + $$13 : 0);
            }
         }

         $$5.add(new PlayerTabOverlay.ScoreDisplayEntry($$10, $$11, $$12, $$13));
      }

      if (!this.healthStates.isEmpty()) {
         Set<UUID> $$17 = $$4.stream().map(p_250472_ -> p_250472_.getProfile().getId()).collect(Collectors.toSet());
         this.healthStates.keySet().removeIf(p_248583_ -> !$$17.contains(p_248583_));
      }

      int $$18 = $$4.size();
      int $$19 = $$18;

      int $$20;
      for ($$20 = 1; $$19 > 20; $$19 = ($$18 + $$20 - 1) / $$20) {
         $$20++;
      }

      boolean $$21 = this.minecraft.isLocalServer() || this.minecraft.getConnection().getConnection().isEncrypted();
      int $$22;
      if (p_282369_ != null) {
         if (p_282369_.getRenderType() == ObjectiveCriteria.RenderType.HEARTS) {
            $$22 = 90;
         } else {
            $$22 = $$8;
         }
      } else {
         $$22 = 0;
      }

      int $$25 = Math.min($$20 * (($$21 ? 9 : 0) + $$7 + $$22 + 13), p_283602_ - 50) / $$20;
      int $$26 = p_283602_ / 2 - ($$25 * $$20 + ($$20 - 1) * 5) / 2;
      int $$27 = 10;
      int $$28 = $$25 * $$20 + ($$20 - 1) * 5;
      List<FormattedCharSequence> $$29 = null;
      if (this.header != null) {
         $$29 = this.minecraft.font.split(this.header, p_283602_ - 50);

         for (FormattedCharSequence $$30 : $$29) {
            $$28 = Math.max($$28, this.minecraft.font.width($$30));
         }
      }

      List<FormattedCharSequence> $$31 = null;
      if (this.footer != null) {
         $$31 = this.minecraft.font.split(this.footer, p_283602_ - 50);

         for (FormattedCharSequence $$32 : $$31) {
            $$28 = Math.max($$28, this.minecraft.font.width($$32));
         }
      }

      if ($$29 != null) {
         p_281484_.fill(p_283602_ / 2 - $$28 / 2 - 1, $$27 - 1, p_283602_ / 2 + $$28 / 2 + 1, $$27 + $$29.size() * 9, Integer.MIN_VALUE);

         for (FormattedCharSequence $$33 : $$29) {
            int $$34 = this.minecraft.font.width($$33);
            p_281484_.drawString(this.minecraft.font, $$33, p_283602_ / 2 - $$34 / 2, $$27, -1);
            $$27 += 9;
         }

         $$27++;
      }

      p_281484_.fill(p_283602_ / 2 - $$28 / 2 - 1, $$27 - 1, p_283602_ / 2 + $$28 / 2 + 1, $$27 + $$19 * 9, Integer.MIN_VALUE);
      int $$35 = this.minecraft.options.getBackgroundColor(553648127);

      for (int $$36 = 0; $$36 < $$18; $$36++) {
         int $$37 = $$36 / $$19;
         int $$38 = $$36 % $$19;
         int $$39 = $$26 + $$37 * $$25 + $$37 * 5;
         int $$40 = $$27 + $$38 * 9;
         p_281484_.fill($$39, $$40, $$39 + $$25, $$40 + 8, $$35);
         RenderSystem.enableBlend();
         if ($$36 < $$4.size()) {
            PlayerInfo $$41 = $$4.get($$36);
            PlayerTabOverlay.ScoreDisplayEntry $$42 = $$5.get($$36);
            GameProfile $$43 = $$41.getProfile();
            if ($$21) {
               Player $$44 = this.minecraft.level.getPlayerByUUID($$43.getId());
               boolean $$45 = $$44 != null && LivingEntityRenderer.isEntityUpsideDown($$44);
               boolean $$46 = $$44 != null && $$44.isModelPartShown(PlayerModelPart.HAT);
               PlayerFaceRenderer.draw(p_281484_, $$41.getSkin().texture(), $$39, $$40, 8, $$46, $$45);
               $$39 += 9;
            }

            p_281484_.drawString(this.minecraft.font, $$42.name, $$39, $$40, $$41.getGameMode() == GameType.SPECTATOR ? -1862270977 : -1);
            if (p_282369_ != null && $$41.getGameMode() != GameType.SPECTATOR) {
               int $$47 = $$39 + $$7 + 1;
               int $$48 = $$47 + $$22;
               if ($$48 - $$47 > 5) {
                  this.renderTablistScore(p_282369_, $$40, $$42, $$47, $$48, $$43.getId(), p_281484_);
               }
            }

            this.renderPingIcon(p_281484_, $$25, $$39 - ($$21 ? 9 : 0), $$40, $$41);
         }
      }

      if ($$31 != null) {
         $$27 += $$19 * 9 + 1;
         p_281484_.fill(p_283602_ / 2 - $$28 / 2 - 1, $$27 - 1, p_283602_ / 2 + $$28 / 2 + 1, $$27 + $$31.size() * 9, Integer.MIN_VALUE);

         for (FormattedCharSequence $$49 : $$31) {
            int $$50 = this.minecraft.font.width($$49);
            p_281484_.drawString(this.minecraft.font, $$49, p_283602_ / 2 - $$50 / 2, $$27, -1);
            $$27 += 9;
         }
      }
   }

   protected void renderPingIcon(GuiGraphics p_283286_, int p_281809_, int p_282801_, int p_282223_, PlayerInfo p_282986_) {
      ResourceLocation $$5;
      if (p_282986_.getLatency() < 0) {
         $$5 = PING_UNKNOWN_SPRITE;
      } else if (p_282986_.getLatency() < 150) {
         $$5 = PING_5_SPRITE;
      } else if (p_282986_.getLatency() < 300) {
         $$5 = PING_4_SPRITE;
      } else if (p_282986_.getLatency() < 600) {
         $$5 = PING_3_SPRITE;
      } else if (p_282986_.getLatency() < 1000) {
         $$5 = PING_2_SPRITE;
      } else {
         $$5 = PING_1_SPRITE;
      }

      p_283286_.pose().pushPose();
      p_283286_.pose().translate(0.0F, 0.0F, 100.0F);
      p_283286_.blitSprite($$5, p_282801_ + p_281809_ - 11, p_282223_, 10, 8);
      p_283286_.pose().popPose();
   }

   private void renderTablistScore(
      Objective p_283381_, int p_282557_, PlayerTabOverlay.ScoreDisplayEntry p_313810_, int p_283533_, int p_281254_, UUID p_283099_, GuiGraphics p_282280_
   ) {
      if (p_283381_.getRenderType() == ObjectiveCriteria.RenderType.HEARTS) {
         this.renderTablistHearts(p_282557_, p_283533_, p_281254_, p_283099_, p_282280_, p_313810_.score);
      } else if (p_313810_.formattedScore != null) {
         p_282280_.drawString(this.minecraft.font, p_313810_.formattedScore, p_281254_ - p_313810_.scoreWidth, p_282557_, 16777215);
      }
   }

   private void renderTablistHearts(int p_282904_, int p_283173_, int p_282149_, UUID p_283348_, GuiGraphics p_281723_, int p_281354_) {
      PlayerTabOverlay.HealthState $$6 = this.healthStates.computeIfAbsent(p_283348_, p_249546_ -> new PlayerTabOverlay.HealthState(p_281354_));
      $$6.update(p_281354_, this.gui.getGuiTicks());
      int $$7 = Mth.positiveCeilDiv(Math.max(p_281354_, $$6.displayedValue()), 2);
      int $$8 = Math.max(p_281354_, Math.max($$6.displayedValue(), 20)) / 2;
      boolean $$9 = $$6.isBlinking(this.gui.getGuiTicks());
      if ($$7 > 0) {
         int $$10 = Mth.floor(Math.min((float)(p_282149_ - p_283173_ - 4) / $$8, 9.0F));
         if ($$10 <= 3) {
            float $$11 = Mth.clamp(p_281354_ / 20.0F, 0.0F, 1.0F);
            int $$12 = (int)((1.0F - $$11) * 255.0F) << 16 | (int)($$11 * 255.0F) << 8;
            float $$13 = p_281354_ / 2.0F;
            Component $$14 = Component.translatable("multiplayer.player.list.hp", $$13);
            Component $$15;
            if (p_282149_ - this.minecraft.font.width($$14) >= p_283173_) {
               $$15 = $$14;
            } else {
               $$15 = Component.literal(Float.toString($$13));
            }

            p_281723_.drawString(this.minecraft.font, $$15, (p_282149_ + p_283173_ - this.minecraft.font.width($$15)) / 2, p_282904_, $$12);
         } else {
            ResourceLocation $$17 = $$9 ? HEART_CONTAINER_BLINKING_SPRITE : HEART_CONTAINER_SPRITE;

            for (int $$18 = $$7; $$18 < $$8; $$18++) {
               p_281723_.blitSprite($$17, p_283173_ + $$18 * $$10, p_282904_, 9, 9);
            }

            for (int $$19 = 0; $$19 < $$7; $$19++) {
               p_281723_.blitSprite($$17, p_283173_ + $$19 * $$10, p_282904_, 9, 9);
               if ($$9) {
                  if ($$19 * 2 + 1 < $$6.displayedValue()) {
                     p_281723_.blitSprite(HEART_FULL_BLINKING_SPRITE, p_283173_ + $$19 * $$10, p_282904_, 9, 9);
                  }

                  if ($$19 * 2 + 1 == $$6.displayedValue()) {
                     p_281723_.blitSprite(HEART_HALF_BLINKING_SPRITE, p_283173_ + $$19 * $$10, p_282904_, 9, 9);
                  }
               }

               if ($$19 * 2 + 1 < p_281354_) {
                  p_281723_.blitSprite($$19 >= 10 ? HEART_ABSORBING_FULL_BLINKING_SPRITE : HEART_FULL_SPRITE, p_283173_ + $$19 * $$10, p_282904_, 9, 9);
               }

               if ($$19 * 2 + 1 == p_281354_) {
                  p_281723_.blitSprite($$19 >= 10 ? HEART_ABSORBING_HALF_BLINKING_SPRITE : HEART_HALF_SPRITE, p_283173_ + $$19 * $$10, p_282904_, 9, 9);
               }
            }
         }
      }
   }

   public void setFooter(@Nullable Component p_94555_) {
      this.footer = p_94555_;
   }

   public void setHeader(@Nullable Component p_94559_) {
      this.header = p_94559_;
   }

   public void reset() {
      this.header = null;
      this.footer = null;
   }

   @OnlyIn(Dist.CLIENT)
   static class HealthState {
      private static final long DISPLAY_UPDATE_DELAY = 20L;
      private static final long DECREASE_BLINK_DURATION = 20L;
      private static final long INCREASE_BLINK_DURATION = 10L;
      private int lastValue;
      private int displayedValue;
      private long lastUpdateTick;
      private long blinkUntilTick;

      public HealthState(int p_250562_) {
         this.displayedValue = p_250562_;
         this.lastValue = p_250562_;
      }

      public void update(int p_251066_, long p_251460_) {
         if (p_251066_ != this.lastValue) {
            long $$2 = p_251066_ < this.lastValue ? 20L : 10L;
            this.blinkUntilTick = p_251460_ + $$2;
            this.lastValue = p_251066_;
            this.lastUpdateTick = p_251460_;
         }

         if (p_251460_ - this.lastUpdateTick > 20L) {
            this.displayedValue = p_251066_;
         }
      }

      public int displayedValue() {
         return this.displayedValue;
      }

      public boolean isBlinking(long p_251847_) {
         return this.blinkUntilTick > p_251847_ && (this.blinkUntilTick - p_251847_) % 6L >= 3L;
      }
   }

   @OnlyIn(Dist.CLIENT)
   record ScoreDisplayEntry(Component name, int score, @Nullable Component formattedScore, int scoreWidth) {
   }
}
