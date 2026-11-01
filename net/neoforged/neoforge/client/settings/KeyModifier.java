package net.neoforged.neoforge.client.settings;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.InputQuirks;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

public enum KeyModifier {
   CONTROL {
      private static final InputConstants.Key[] KEYS = new InputConstants.Key[]{
         InputConstants.Type.KEYSYM.getOrCreate(341), InputConstants.Type.KEYSYM.getOrCreate(345)
      };

      @Override
      public boolean matches(InputConstants.Key key) {
         int keyCode = key.getValue();
         return keyCode == 341 || keyCode == 345;
      }

      @Override
      public boolean isActive(@Nullable IKeyConflictContext conflictContext) {
         return Minecraft.getInstance().hasControlDown();
      }

      @Override
      public Component getCombinedName(InputConstants.Key key, Supplier<Component> defaultLogic) {
         return Component.translatable("neoforge.controlsgui.control", defaultLogic.get());
      }

      @Override
      public InputConstants.Key[] codes() {
         return KEYS;
      }
   },
   CONTROL_OR_COMMAND {
      private static final InputConstants.Key[] COMMAND_KEYS = new InputConstants.Key[]{
         InputConstants.Type.KEYSYM.getOrCreate(343), InputConstants.Type.KEYSYM.getOrCreate(347)
      };

      @Override
      public boolean matches(InputConstants.Key key) {
         if (!InputQuirks.REPLACE_CTRL_KEY_WITH_CMD_KEY) {
            return CONTROL.matches(key);
         } else {
            int keyCode = key.getValue();
            return keyCode == 343 || keyCode == 347;
         }
      }

      @Override
      public boolean isActive(@Nullable IKeyConflictContext conflictContext) {
         if (!InputQuirks.REPLACE_CTRL_KEY_WITH_CMD_KEY) {
            return CONTROL.isActive(conflictContext);
         } else {
            Minecraft minecraft = Minecraft.getInstance();
            Window window = minecraft.getWindow();
            return InputConstants.isKeyDown(window, 343) || InputConstants.isKeyDown(window, 347);
         }
      }

      @Override
      public Component getCombinedName(InputConstants.Key key, Supplier<Component> defaultLogic) {
         return (Component)(InputQuirks.REPLACE_CTRL_KEY_WITH_CMD_KEY
            ? Component.translatable("neoforge.controlsgui.control.mac", defaultLogic.get())
            : CONTROL.getCombinedName(key, defaultLogic));
      }

      @Override
      public InputConstants.Key[] codes() {
         return InputQuirks.REPLACE_CTRL_KEY_WITH_CMD_KEY ? COMMAND_KEYS : CONTROL.codes();
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
         return Minecraft.getInstance().hasShiftDown();
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
         return Minecraft.getInstance().hasAltDown();
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

   public static final KeyModifier[] MODIFIER_VALUES = InputQuirks.ON_OSX
      ? new KeyModifier[]{SHIFT, CONTROL_OR_COMMAND, CONTROL, ALT}
      : new KeyModifier[]{SHIFT, CONTROL, ALT};

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

   public abstract InputConstants.Key[] codes();
}
