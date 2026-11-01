package net.minecraft.client.gui.screens.options.controls;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Options;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.client.input.InputQuirks;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.settings.KeyModifier;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class KeyBindsScreen extends OptionsSubScreen {
   private static final Component TITLE = Component.translatable("controls.keybinds.title");
   @Nullable
   public KeyMapping selectedKey;
   public long lastKeySelection;
   private KeyBindsList keyBindsList;
   private Button resetButton;
   private InputConstants.Key lastPressedKey = InputConstants.UNKNOWN;
   private InputConstants.Key lastPressedModifier = InputConstants.UNKNOWN;
   private boolean isLastKeyHeldDown = false;
   private boolean isLastModifierHeldDown = false;

   public KeyBindsScreen(Screen p_344996_, Options p_344771_) {
      super(p_344996_, p_344771_, TITLE);
   }

   @Override
   protected void addContents() {
      this.keyBindsList = this.layout.addToContents(new KeyBindsList(this, this.minecraft));
   }

   @Override
   protected void addOptions() {
   }

   @Override
   protected void addFooter() {
      this.resetButton = Button.builder(Component.translatable("controls.resetAll"), p_346345_ -> {
         for (KeyMapping keymapping : this.options.keyMappings) {
            keymapping.setToDefault();
         }

         this.keyBindsList.resetMappingAndUpdateButtons();
      }).build();
      LinearLayout linearlayout = this.layout.addToFooter(LinearLayout.horizontal().spacing(8));
      linearlayout.addChild(this.resetButton);
      linearlayout.addChild(Button.builder(CommonComponents.GUI_DONE, p_432229_ -> this.onClose()).build());
   }

   @Override
   protected void repositionElements() {
      this.layout.arrangeElements();
      this.keyBindsList.updateSize(this.width, this.layout);
   }

   @Override
   public boolean mouseClicked(MouseButtonEvent p_445878_, boolean p_434872_) {
      if (this.selectedKey != null) {
         this.selectedKey.setKey(InputConstants.Type.MOUSE.getOrCreate(p_445878_.button()));
         this.selectedKey = null;
         this.keyBindsList.resetMappingAndUpdateButtons();
         return true;
      } else {
         return super.mouseClicked(p_445878_, p_434872_);
      }
   }

   @Override
   public boolean keyPressed(KeyEvent p_446842_) {
      if (this.selectedKey == null) {
         return super.keyPressed(p_446842_);
      } else {
         InputConstants.Key key = InputConstants.getKey(p_446842_);
         if (this.lastPressedModifier == InputConstants.UNKNOWN && KeyModifier.isKeyCodeModifier(key)) {
            this.lastPressedModifier = key;
            this.isLastModifierHeldDown = true;
         } else {
            this.lastPressedKey = key;
            this.isLastKeyHeldDown = true;
         }

         return true;
      }
   }

   @Override
   public boolean keyReleased(KeyEvent event) {
      if (this.selectedKey != null && (!InputQuirks.ON_OSX || event.scancode() != 63)) {
         if (event.isEscape()) {
            this.selectedKey.setKeyModifierAndCode(KeyModifier.NONE, InputConstants.UNKNOWN);
            this.selectedKey.setKey(InputConstants.UNKNOWN);
            this.lastPressedKey = InputConstants.UNKNOWN;
            this.lastPressedModifier = InputConstants.UNKNOWN;
            this.isLastKeyHeldDown = false;
            this.isLastModifierHeldDown = false;
         } else {
            InputConstants.Key key = InputConstants.getKey(event);
            if (this.lastPressedKey.equals(key)) {
               this.isLastKeyHeldDown = false;
            } else if (this.lastPressedModifier.equals(key)) {
               this.isLastModifierHeldDown = false;
            }

            if (this.isLastKeyHeldDown || this.isLastModifierHeldDown) {
               return true;
            }

            if (!this.lastPressedKey.equals(InputConstants.UNKNOWN)) {
               this.selectedKey.setKeyModifierAndCode(KeyModifier.getKeyModifier(this.lastPressedModifier), this.lastPressedKey);
               this.selectedKey.setKey(this.lastPressedKey);
            } else {
               this.selectedKey.setKeyModifierAndCode(KeyModifier.NONE, this.lastPressedModifier);
               this.selectedKey.setKey(this.lastPressedModifier);
            }

            this.lastPressedKey = InputConstants.UNKNOWN;
            this.lastPressedModifier = InputConstants.UNKNOWN;
         }

         this.selectedKey = null;
         this.lastKeySelection = Util.getMillis();
         this.keyBindsList.resetMappingAndUpdateButtons();
         return true;
      } else {
         return super.keyReleased(event);
      }
   }

   @Override
   public void render(GuiGraphics p_346209_, int p_344846_, int p_346350_, float p_345601_) {
      super.render(p_346209_, p_344846_, p_346350_, p_345601_);
      boolean flag = false;

      for (KeyMapping keymapping : this.options.keyMappings) {
         if (!keymapping.isDefault()) {
            flag = true;
            break;
         }
      }

      this.resetButton.active = flag;
   }
}
