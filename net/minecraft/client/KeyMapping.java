package net.minecraft.client;

import com.google.common.collect.Maps;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class KeyMapping implements Comparable<KeyMapping> {
   private static final Map<String, KeyMapping> ALL = Maps.newHashMap();
   private static final Map<InputConstants.Key, List<KeyMapping>> MAP = Maps.newHashMap();
   private final String name;
   private final InputConstants.Key defaultKey;
   private final KeyMapping.Category category;
   protected InputConstants.Key key;
   private boolean isDown;
   private int clickCount;
   private final int order;

   public static void click(InputConstants.Key key) {
      forAllKeyMappings(key, keyMapping -> keyMapping.clickCount++);
   }

   public static void set(InputConstants.Key key, boolean held) {
      forAllKeyMappings(key, keyMapping -> keyMapping.setDown(held));
   }

   private static void forAllKeyMappings(InputConstants.Key key, Consumer<KeyMapping> action) {
      List<KeyMapping> list = MAP.get(key);
      if (list != null && !list.isEmpty()) {
         for (KeyMapping keyMapping : list) {
            action.accept(keyMapping);
         }
      }
   }

   public static void setAll() {
      Window window = Minecraft.getInstance().getWindow();

      for (KeyMapping keyMapping : ALL.values()) {
         if (keyMapping.shouldSetOnIngameFocus()) {
            keyMapping.setDown(InputConstants.isKeyDown(window, keyMapping.key.getValue()));
         }
      }
   }

   public static void releaseAll() {
      for (KeyMapping keyMapping : ALL.values()) {
         keyMapping.release();
      }
   }

   public static void restoreToggleStatesOnScreenClosed() {
      for (KeyMapping keyMapping : ALL.values()) {
         if (keyMapping instanceof ToggleKeyMapping toggleKeyMapping && toggleKeyMapping.shouldRestoreStateOnScreenClosed()) {
            toggleKeyMapping.setDown(true);
         }
      }
   }

   public static void resetToggleKeys() {
      for (KeyMapping keyMapping : ALL.values()) {
         if (keyMapping instanceof ToggleKeyMapping toggleKeyMapping) {
            toggleKeyMapping.reset();
         }
      }
   }

   public static void resetMapping() {
      MAP.clear();

      for (KeyMapping keyMapping : ALL.values()) {
         keyMapping.registerMapping(keyMapping.key);
      }
   }

   public KeyMapping(String name, int key, KeyMapping.Category category) {
      this(name, InputConstants.Type.KEYSYM, key, category);
   }

   public KeyMapping(String name, InputConstants.Type type, int key, KeyMapping.Category category) {
      this(name, type, key, category, 0);
   }

   public KeyMapping(String name, InputConstants.Type type, int key, KeyMapping.Category category, int order) {
      this.name = name;
      this.key = type.getOrCreate(key);
      this.defaultKey = this.key;
      this.category = category;
      this.order = order;
      ALL.put(name, this);
      this.registerMapping(this.key);
   }

   public boolean isDown() {
      return this.isDown;
   }

   public KeyMapping.Category getCategory() {
      return this.category;
   }

   public boolean consumeClick() {
      if (this.clickCount == 0) {
         return false;
      } else {
         this.clickCount--;
         return true;
      }
   }

   protected void release() {
      this.clickCount = 0;
      this.setDown(false);
   }

   protected boolean shouldSetOnIngameFocus() {
      return this.key.getType() == InputConstants.Type.KEYSYM && this.key.getValue() != InputConstants.UNKNOWN.getValue();
   }

   public String getName() {
      return this.name;
   }

   public InputConstants.Key getDefaultKey() {
      return this.defaultKey;
   }

   public void setKey(InputConstants.Key key) {
      this.key = key;
   }

   public int compareTo(KeyMapping other) {
      if (this.category == other.category) {
         return this.order == other.order ? I18n.get(this.name).compareTo(I18n.get(other.name)) : Integer.compare(this.order, other.order);
      } else {
         return Integer.compare(KeyMapping.Category.SORT_ORDER.indexOf(this.category), KeyMapping.Category.SORT_ORDER.indexOf(other.category));
      }
   }

   public static Supplier<Component> createNameSupplier(String key) {
      KeyMapping keyMapping = ALL.get(key);
      return keyMapping == null ? () -> Component.translatable(key) : keyMapping::getTranslatedKeyMessage;
   }

   public boolean same(KeyMapping binding) {
      return this.key.equals(binding.key);
   }

   public boolean isUnbound() {
      return this.key.equals(InputConstants.UNKNOWN);
   }

   public boolean matches(KeyEvent event) {
      return event.key() == InputConstants.UNKNOWN.getValue()
         ? this.key.getType() == InputConstants.Type.SCANCODE && this.key.getValue() == event.scancode()
         : this.key.getType() == InputConstants.Type.KEYSYM && this.key.getValue() == event.key();
   }

   public boolean matchesMouse(MouseButtonEvent event) {
      return this.key.getType() == InputConstants.Type.MOUSE && this.key.getValue() == event.button();
   }

   public Component getTranslatedKeyMessage() {
      return this.key.getDisplayName();
   }

   public boolean isDefault() {
      return this.key.equals(this.defaultKey);
   }

   public String saveString() {
      return this.key.getName();
   }

   public void setDown(boolean value) {
      this.isDown = value;
   }

   private void registerMapping(InputConstants.Key key) {
      MAP.computeIfAbsent(key, keyx -> new ArrayList<>()).add(this);
   }

   @Nullable
   public static KeyMapping get(String name) {
      return ALL.get(name);
   }

   @Environment(EnvType.CLIENT)
   public record Category(Identifier id) {
      static final List<KeyMapping.Category> SORT_ORDER = new ArrayList<>();
      public static final KeyMapping.Category MOVEMENT = register("movement");
      public static final KeyMapping.Category MISC = register("misc");
      public static final KeyMapping.Category MULTIPLAYER = register("multiplayer");
      public static final KeyMapping.Category GAMEPLAY = register("gameplay");
      public static final KeyMapping.Category INVENTORY = register("inventory");
      public static final KeyMapping.Category CREATIVE = register("creative");
      public static final KeyMapping.Category SPECTATOR = register("spectator");
      public static final KeyMapping.Category DEBUG = register("debug");

      private static KeyMapping.Category register(String name) {
         return register(Identifier.withDefaultNamespace(name));
      }

      public static KeyMapping.Category register(Identifier location) {
         KeyMapping.Category category = new KeyMapping.Category(location);
         if (SORT_ORDER.contains(category)) {
            throw new IllegalArgumentException(String.format(Locale.ROOT, "Category '%s' is already registered.", location));
         } else {
            SORT_ORDER.add(category);
            return category;
         }
      }

      public Component label() {
         return Component.translatable(this.id.toLanguageKey("key.category"));
      }
   }
}
