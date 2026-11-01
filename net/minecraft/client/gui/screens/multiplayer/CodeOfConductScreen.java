package net.minecraft.client.gui.screens.multiplayer;

import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.Layout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class CodeOfConductScreen extends WarningScreen {
   private static final Component TITLE = Component.translatable("multiplayer.codeOfConduct.title").withStyle(ChatFormatting.BOLD);
   private static final Component CHECK = Component.translatable("multiplayer.codeOfConduct.check");
   @Nullable
   private final ServerData serverData;
   private final String codeOfConductText;
   private final BooleanConsumer resultConsumer;
   private final Screen parent;

   private CodeOfConductScreen(@Nullable ServerData p_439363_, Screen p_445411_, Component p_440028_, String p_438953_, BooleanConsumer p_439317_) {
      super(TITLE, p_440028_, CHECK, TITLE.copy().append("\n").append(p_440028_));
      this.serverData = p_439363_;
      this.parent = p_445411_;
      this.codeOfConductText = p_438953_;
      this.resultConsumer = p_439317_;
   }

   public CodeOfConductScreen(@Nullable ServerData p_439282_, Screen p_446531_, String p_440211_, BooleanConsumer p_438979_) {
      this(p_439282_, p_446531_, Component.literal(p_440211_), p_440211_, p_438979_);
   }

   @Override
   protected Layout addFooterButtons() {
      LinearLayout linearlayout = LinearLayout.horizontal().spacing(8);
      linearlayout.addChild(Button.builder(CommonComponents.GUI_ACKNOWLEDGE, p_439148_ -> this.onResult(true)).build());
      linearlayout.addChild(Button.builder(CommonComponents.GUI_DISCONNECT, p_439910_ -> this.onResult(false)).build());
      return linearlayout;
   }

   private void onResult(boolean p_440610_) {
      this.resultConsumer.accept(p_440610_);
      if (this.serverData != null) {
         if (p_440610_ && this.stopShowing.selected()) {
            this.serverData.acceptCodeOfConduct(this.codeOfConductText);
         } else {
            this.serverData.clearCodeOfConduct();
         }

         ServerList.saveSingleServer(this.serverData);
      }
   }

   @Override
   public boolean shouldCloseOnEsc() {
      return false;
   }

   @Override
   public void tick() {
      super.tick();
      if (this.parent instanceof ConnectScreen || this.parent instanceof ServerReconfigScreen) {
         this.parent.tick();
      }
   }
}
