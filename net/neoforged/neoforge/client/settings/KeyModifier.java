package net.neoforged.neoforge.client.settings;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public enum KeyModifier {
   CONTROL {
      private static final InputConstants.Key[] KEYS = new InputConstants.Key[]{
         InputConstants.Type.KEYSYM.getOrCreate(341), InputConstants.Type.KEYSYM.getOrCreate(345)
      };
      private static final InputConstants.Key[] OSX_KEYS = new InputConstants.Key[]{
         InputConstants.Type.KEYSYM.getOrCreate(343), InputConstants.Type.KEYSYM.getOrCreate(347)
      };

      @Override
      public boolean matches(InputConstants.Key key) {
         int keyCode = key.getValue();
         return Minecraft.ON_OSX ? keyCode == 343 || keyCode == 347 : keyCode == 341 || keyCode == 345;
      }

      @Override
      public boolean isActive(@Nullable IKeyConflictContext conflictContext) {
         return Screen.hasControlDown();
      }

      @Override
      public Component getCombinedName(InputConstants.Key key, Supplier<Component> defaultLogic) {
         String localizationFormatKey = Minecraft.ON_OSX ? "neoforge.controlsgui.control.mac" : "neoforge.controlsgui.control";
         return Component.translatable(localizationFormatKey, defaultLogic.get());
      }

      @Override
      public InputConstants.Key[] codes() {
         return Minecraft.ON_OSX ? OSX_KEYS : KEYS;
      }
   },
   SHIFT {
      private static final InputConstants.Key[] KEYS = new InputConstants.Key[]{
         InputConstants.Type.KEYSYM.getOrCreate(340), InputConstants.Type.KEYSYM.getOrCreate(344)
      };

      @Override
      public boolean matches(InputConstants.Key key) {
         return key.getValue() == 340 || key.getValue() == 344;
      }

      @Override
      public boolean isActive(@Nullable IKeyConflictContext conflictContext) {
         return Screen.hasShiftDown();
      }

      @Override
      public Component getCombinedName(InputConstants.Key key, Supplier<Component> defaultLogic) {
         return Component.translatable("neoforge.controlsgui.shift", defaultLogic.get());
      }

      @Override
      public InputConstants.Key[] codes() {
         return KEYS;
      }
   },
   ALT {
      private static final InputConstants.Key[] KEYS = new InputConstants.Key[]{
         InputConstants.Type.KEYSYM.getOrCreate(342), InputConstants.Type.KEYSYM.getOrCreate(346)
      };

      @Override
      public boolean matches(InputConstants.Key key) {
         return key.getValue() == 342 || key.getValue() == 346;
      }

      @Override
      public boolean isActive(@Nullable IKeyConflictContext conflictContext) {
         return Screen.hasAltDown();
      }

      @Override
      public Component getCombinedName(InputConstants.Key keyCode, Supplier<Component> defaultLogic) {
         return Component.translatable("neoforge.controlsgui.alt", defaultLogic.get());
      }

      @Override
      public InputConstants.Key[] codes() {
         return KEYS;
      }
   },
   NONE {
      private static final InputConstants.Key[] KEYS = new InputConstants.Key[0];

      @Override
      public boolean matches(InputConstants.Key key) {
         return false;
      }

      @Override
      public boolean isActive(@Nullable IKeyConflictContext conflictContext) {
         if (conflictContext != null && !conflictContext.conflicts(KeyConflictContext.IN_GAME)) {
            for (KeyModifier keyModifier : MODIFIER_VALUES) {
               if (keyModifier.isActive(conflictContext)) {
                  return false;
               }
            }
         }

         return true;
      }

      @Override
      public Component getCombinedName(InputConstants.Key key, Supplier<Component> defaultLogic) {
         return defaultLogic.get();
      }

      @Override
      public InputConstants.Key[] codes() {
         return KEYS;
      }
   };

   public static final KeyModifier[] MODIFIER_VALUES = new KeyModifier[]{SHIFT, CONTROL, ALT};

   @Deprecated(
      forRemoval = true,
      since = "1.21.4"
   )
   public static KeyModifier getActiveModifier() {
      for (KeyModifier keyModifier : MODIFIER_VALUES) {
         if (keyModifier.isActive(null)) {
            return keyModifier;
         }
      }

      return NONE;
   }

   public static List<KeyModifier> getActiveModifiers() {
      List<KeyModifier> modifiers = new ArrayList<>();

      for (KeyModifier keyModifier : MODIFIER_VALUES) {
         if (keyModifier.isActive(null)) {
            modifiers.add(keyModifier);
         }
      }

      return modifiers;
   }

   public static KeyModifier getKeyModifier(InputConstants.Key key) {
      for (KeyModifier keyModifier : MODIFIER_VALUES) {
         if (keyModifier.matches(key)) {
            return keyModifier;
         }
      }

      return NONE;
   }

   public static boolean isKeyCodeModifier(InputConstants.Key key) {
      return getKeyModifier(key) != NONE;
   }

   public static KeyModifier valueFromString(String stringValue) {
      try {
         return valueOf(stringValue);
      } catch (IllegalArgumentException | NullPointerException var2) {
         return NONE;
      }
   }

   public abstract boolean matches(InputConstants.Key var1);

   public abstract boolean isActive(@Nullable IKeyConflictContext var1);

   public abstract Component getCombinedName(InputConstants.Key var1, Supplier<Component> var2);

   public InputConstants.Key[] codes() {
      return null;
   }
}
