package com.mojang.realmsclient.gui;

import com.mojang.realmsclient.RealmsMainScreen;
import com.mojang.realmsclient.dto.RealmsServer;
import com.mojang.realmsclient.dto.RealmsSlot;
import com.mojang.realmsclient.util.RealmsTextureManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class RealmsWorldSlotButton extends Button {
   private static final Identifier SLOT_FRAME_SPRITE = Identifier.withDefaultNamespace("widget/slot_frame");
   public static final Identifier EMPTY_SLOT_LOCATION = Identifier.withDefaultNamespace("textures/gui/realms/empty_frame.png");
   public static final Identifier DEFAULT_WORLD_SLOT_1 = Identifier.withDefaultNamespace("textures/gui/title/background/panorama_0.png");
   public static final Identifier DEFAULT_WORLD_SLOT_2 = Identifier.withDefaultNamespace("textures/gui/title/background/panorama_2.png");
   public static final Identifier DEFAULT_WORLD_SLOT_3 = Identifier.withDefaultNamespace("textures/gui/title/background/panorama_3.png");
   private static final Component SWITCH_TO_MINIGAME_SLOT_TOOLTIP = Component.translatable("mco.configure.world.slot.tooltip.minigame");
   private static final Component SWITCH_TO_WORLD_SLOT_TOOLTIP = Component.translatable("mco.configure.world.slot.tooltip");
   static final Component MINIGAME = Component.translatable("mco.worldSlot.minigame");
   private static final int WORLD_NAME_MAX_WIDTH = 64;
   private static final String DOTS = "...";
   private final int slotIndex;
   private RealmsWorldSlotButton.State state;

   public RealmsWorldSlotButton(int x, int y, int width, int height, int slotIndex, RealmsServer serverData, Button.OnPress onPress) {
      super(x, y, width, height, CommonComponents.EMPTY, onPress, DEFAULT_NARRATION);
      this.slotIndex = slotIndex;
      this.state = this.setServerData(serverData);
   }

   public RealmsWorldSlotButton.State getState() {
      return this.state;
   }

   public RealmsWorldSlotButton.State setServerData(RealmsServer serverData) {
      this.state = new RealmsWorldSlotButton.State(serverData, this.slotIndex);
      this.setTooltipAndNarration(this.state, serverData.minigameName);
      return this.state;
   }

   private void setTooltipAndNarration(RealmsWorldSlotButton.State state, @Nullable String minigameName) {
      Component component = switch (state.action) {
         case SWITCH_SLOT -> state.minigame ? SWITCH_TO_MINIGAME_SLOT_TOOLTIP : SWITCH_TO_WORLD_SLOT_TOOLTIP;
         default -> null;
      };
      if (component != null) {
         this.setTooltip(Tooltip.create(component));
      }

      MutableComponent mutableComponent = Component.literal(state.slotName);
      if (state.minigame && minigameName != null) {
         mutableComponent = mutableComponent.append(CommonComponents.SPACE).append(minigameName);
      }

      this.setMessage(mutableComponent);
   }

   static RealmsWorldSlotButton.Action getAction(boolean activeSlot, boolean empty, boolean expired) {
      return activeSlot || empty && expired ? RealmsWorldSlotButton.Action.NOTHING : RealmsWorldSlotButton.Action.SWITCH_SLOT;
   }

   @Override
   public boolean isActive() {
      return this.state.action != RealmsWorldSlotButton.Action.NOTHING && super.isActive();
   }

   @Override
   public void renderContents(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
      int i = this.getX();
      int j = this.getY();
      boolean bl = this.isHoveredOrFocused();
      Identifier identifier;
      if (this.state.minigame) {
         identifier = RealmsTextureManager.worldTemplate(String.valueOf(this.state.imageId), this.state.image);
      } else if (this.state.empty) {
         identifier = EMPTY_SLOT_LOCATION;
      } else if (this.state.image != null && this.state.imageId != -1L) {
         identifier = RealmsTextureManager.worldTemplate(String.valueOf(this.state.imageId), this.state.image);
      } else if (this.slotIndex == 1) {
         identifier = DEFAULT_WORLD_SLOT_1;
      } else if (this.slotIndex == 2) {
         identifier = DEFAULT_WORLD_SLOT_2;
      } else if (this.slotIndex == 3) {
         identifier = DEFAULT_WORLD_SLOT_3;
      } else {
         identifier = EMPTY_SLOT_LOCATION;
      }

      int k = -1;
      if (!this.state.activeSlot) {
         k = ARGB.colorFromFloat(1.0F, 0.56F, 0.56F, 0.56F);
      }

      guiGraphics.blit(RenderPipelines.GUI_TEXTURED, identifier, i + 1, j + 1, 0.0F, 0.0F, this.width - 2, this.height - 2, 74, 74, 74, 74, k);
      if (bl && this.state.action != RealmsWorldSlotButton.Action.NOTHING) {
         guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_FRAME_SPRITE, i, j, this.width, this.height);
      } else if (this.state.activeSlot) {
         guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_FRAME_SPRITE, i, j, this.width, this.height, ARGB.colorFromFloat(1.0F, 0.8F, 0.8F, 0.8F));
      } else {
         guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_FRAME_SPRITE, i, j, this.width, this.height, ARGB.colorFromFloat(1.0F, 0.56F, 0.56F, 0.56F));
      }

      if (this.state.hardcore) {
         guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, RealmsMainScreen.HARDCORE_MODE_SPRITE, i + 3, j + 4, 9, 8);
      }

      Font font = Minecraft.getInstance().font;
      String string = this.state.slotName;
      if (font.width(string) > 64) {
         string = font.plainSubstrByWidth(string, 64 - font.width("...")) + "...";
      }

      guiGraphics.drawCenteredString(font, string, i + this.width / 2, j + this.height - 14, -1);
      if (this.state.activeSlot) {
         guiGraphics.drawCenteredString(
            font,
            RealmsMainScreen.getVersionComponent(this.state.slotVersion, this.state.compatibility.isCompatible()),
            i + this.width / 2,
            j + this.height + 2,
            -1
         );
      }
   }

   @Environment(EnvType.CLIENT)
   public static enum Action {
      NOTHING,
      SWITCH_SLOT;
   }

   @Environment(EnvType.CLIENT)
   public static class State {
      final String slotName;
      final String slotVersion;
      final RealmsServer.Compatibility compatibility;
      final long imageId;
      @Nullable
      final String image;
      public final boolean empty;
      public final boolean minigame;
      public final RealmsWorldSlotButton.Action action;
      public final boolean hardcore;
      public final boolean activeSlot;

      public State(RealmsServer server, int slot) {
         this.minigame = slot == 4;
         if (this.minigame) {
            this.slotName = RealmsWorldSlotButton.MINIGAME.getString();
            this.imageId = server.minigameId;
            this.image = server.minigameImage;
            this.empty = server.minigameId == -1;
            this.slotVersion = "";
            this.compatibility = RealmsServer.Compatibility.UNVERIFIABLE;
            this.hardcore = false;
            this.activeSlot = server.isMinigameActive();
         } else {
            RealmsSlot realmsSlot = server.slots.get(slot);
            this.slotName = realmsSlot.options.getSlotName(slot);
            this.imageId = realmsSlot.options.templateId;
            this.image = realmsSlot.options.templateImage;
            this.empty = realmsSlot.options.empty;
            this.slotVersion = realmsSlot.options.version;
            this.compatibility = realmsSlot.options.compatibility;
            this.hardcore = realmsSlot.isHardcore();
            this.activeSlot = server.activeSlot == slot && !server.isMinigameActive();
         }

         this.action = RealmsWorldSlotButton.getAction(this.activeSlot, this.empty, server.expired);
      }
   }
}
