package net.neoforged.neoforge.client.gui;

import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.electronwill.nightconfig.core.UnmodifiableConfig.Entry;
import com.google.common.collect.ImmutableList;
import com.mojang.realmsclient.RealmsMainScreen;
import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import java.lang.runtime.SwitchBootstraps;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractContainerWidget;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.GenericMessageScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.data.models.blockstates.PropertyDispatch;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.config.ModConfigs;
import net.neoforged.fml.config.ModConfig.Type;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.NeoForgeConfig;
import net.neoforged.neoforge.common.TranslatableEnum;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.util.Strings;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.ApiStatus.Internal;

public final class ConfigurationScreen extends OptionsSubScreen {
   private static final String LANG_PREFIX = "neoforge.configuration.uitext.";
   private static final String SECTION = "neoforge.configuration.uitext.section";
   private static final String SECTION_TEXT = "neoforge.configuration.uitext.sectiontext";
   public static final Component CRUMB_SEPARATOR = Component.translatable("neoforge.configuration.uitext.breadcrumb.separator")
      .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
   private static final String CRUMB = "neoforge.configuration.uitext.breadcrumb.order";
   private static final String LIST_ELEMENT = "neoforge.configuration.uitext.listelement";
   private static final String RANGE_TOOLTIP = "neoforge.configuration.uitext.rangetooltip";
   private static final ChatFormatting RANGE_TOOLTIP_STYLE = ChatFormatting.GRAY;
   private static final String FILENAME_TOOLTIP = "neoforge.configuration.uitext.filenametooltip";
   private static final ChatFormatting FILENAME_TOOLTIP_STYLE = ChatFormatting.GRAY;
   private static final MutableComponent EMPTY_LINE = Component.literal("\n\n");
   public static final Component TOOLTIP_CANNOT_EDIT_THIS_WHILE_ONLINE = Component.translatable("neoforge.configuration.uitext.notonline")
      .withStyle(ChatFormatting.RED);
   public static final Component TOOLTIP_CANNOT_EDIT_THIS_WHILE_OPEN_TO_LAN = Component.translatable("neoforge.configuration.uitext.notlan")
      .withStyle(ChatFormatting.RED);
   public static final Component TOOLTIP_CANNOT_EDIT_NOT_LOADED = Component.translatable("neoforge.configuration.uitext.notloaded")
      .withStyle(ChatFormatting.RED);
   public static final Component NEW_LIST_ELEMENT = Component.translatable("neoforge.configuration.uitext.newlistelement");
   public static final Component MOVE_LIST_ELEMENT_UP = Component.translatable("neoforge.configuration.uitext.listelementup");
   public static final Component MOVE_LIST_ELEMENT_DOWN = Component.translatable("neoforge.configuration.uitext.listelementdown");
   public static final Component REMOVE_LIST_ELEMENT = Component.translatable("neoforge.configuration.uitext.listelementremove");
   public static final Component UNSUPPORTED_ELEMENT = Component.translatable("neoforge.configuration.uitext.unsupportedelement").withStyle(ChatFormatting.RED);
   public static final Component LONG_STRING = Component.translatable("neoforge.configuration.uitext.longstring").withStyle(ChatFormatting.RED);
   public static final Component GAME_RESTART_TITLE = Component.translatable("neoforge.configuration.uitext.restart.game.title");
   public static final Component GAME_RESTART_MESSAGE = Component.translatable("neoforge.configuration.uitext.restart.game.text");
   public static final Component GAME_RESTART_YES = Component.translatable("menu.quit");
   public static final Component SERVER_RESTART_TITLE = Component.translatable("neoforge.configuration.uitext.restart.server.title");
   public static final Component SERVER_RESTART_MESSAGE = Component.translatable("neoforge.configuration.uitext.restart.server.text");
   public static final Component RETURN_TO_MENU = Component.translatable("menu.returnToMenu");
   public static final Component SAVING_LEVEL = Component.translatable("menu.savingLevel");
   public static final Component RESTART_NO = Component.translatable("neoforge.configuration.uitext.restart.return");
   public static final Component RESTART_NO_TOOLTIP = Component.translatable("neoforge.configuration.uitext.restart.return.tooltip")
      .withStyle(ChatFormatting.RED, ChatFormatting.BOLD);
   public static final Component UNDO = Component.translatable("neoforge.configuration.uitext.undo");
   public static final Component UNDO_TOOLTIP = Component.translatable("neoforge.configuration.uitext.undo.tooltip");
   public static final Component RESET = Component.translatable("neoforge.configuration.uitext.reset");
   public static final Component RESET_TOOLTIP = Component.translatable("neoforge.configuration.uitext.reset.tooltip");
   public static final int BIG_BUTTON_WIDTH = 310;
   protected static final ConfigurationScreen.TranslationChecker translationChecker = new ConfigurationScreen.TranslationChecker();
   protected final ModContainer mod;
   private final PropertyDispatch.QuadFunction<ConfigurationScreen, Type, ModConfig, Component, Screen> sectionScreen;
   public ModConfigSpec.RestartType needsRestart = ModConfigSpec.RestartType.NONE;
   private boolean autoClose = false;

   public ConfigurationScreen(ModContainer mod, Screen parent) {
      this(mod, parent, ConfigurationScreen.ConfigurationSectionScreen::new);
   }

   public ConfigurationScreen(ModContainer mod, Screen parent, ConfigurationScreen.ConfigurationSectionScreen.Filter filter) {
      this(mod, parent, (a, b, c, d) -> new ConfigurationScreen.ConfigurationSectionScreen(a, b, c, d, filter));
   }

   public ConfigurationScreen(
      ModContainer mod, Screen parent, PropertyDispatch.QuadFunction<ConfigurationScreen, Type, ModConfig, Component, Screen> sectionScreen
   ) {
      super(
         parent,
         Minecraft.getInstance().options,
         Component.translatable(
            translationChecker.check(mod.getModId() + ".configuration.title", "neoforge.configuration.uitext.title"), mod.getModInfo().getDisplayName()
         )
      );
      this.mod = mod;
      this.sectionScreen = sectionScreen;
   }

   @Override
   protected void addOptions() {
      Button btn = null;
      int count = 0;

      for (Type type : Type.values()) {
         boolean headerAdded = false;

         for (ModConfig modConfig : ModConfigs.getConfigSet(type)) {
            if (modConfig.getModId().equals(this.mod.getModId())) {
               if (!headerAdded) {
                  this.list
                     .addSmall(
                        new StringWidget(
                              310,
                              20,
                              Component.translatable("neoforge.configuration.uitext." + type.name().toLowerCase(Locale.ENGLISH))
                                 .withStyle(ChatFormatting.UNDERLINE),
                              this.font
                           )
                           .alignLeft(),
                        null
                     );
                  headerAdded = true;
               }

               btn = Button.builder(
                     Component.translatable(
                        "neoforge.configuration.uitext.section",
                        this.translatableConfig(modConfig, "", "neoforge.configuration.uitext.type." + modConfig.getType().name().toLowerCase(Locale.ROOT))
                     ),
                     button -> this.minecraft
                        .setScreen(
                           this.sectionScreen
                              .apply(
                                 this,
                                 type,
                                 modConfig,
                                 this.translatableConfig(modConfig, ".title", "neoforge.configuration.uitext.title." + type.name().toLowerCase(Locale.ROOT))
                              )
                        )
                  )
                  .width(310)
                  .build();
               MutableComponent tooltip = Component.empty();
               if (!((ModConfigSpec)modConfig.getSpec()).isLoaded()) {
                  tooltip.append(TOOLTIP_CANNOT_EDIT_NOT_LOADED).append(EMPTY_LINE);
                  btn.active = false;
                  count = 99;
               } else if (type == Type.SERVER && this.minecraft.getCurrentServer() != null && !this.minecraft.isSingleplayer()) {
                  tooltip.append(TOOLTIP_CANNOT_EDIT_THIS_WHILE_ONLINE).append(EMPTY_LINE);
                  btn.active = false;
                  count = 99;
               } else if (type == Type.SERVER && this.minecraft.hasSingleplayerServer() && this.minecraft.getSingleplayerServer().isPublished()) {
                  tooltip.append(TOOLTIP_CANNOT_EDIT_THIS_WHILE_OPEN_TO_LAN).append(EMPTY_LINE);
                  btn.active = false;
                  count = 99;
               }

               tooltip.append(
                  Component.translatable("neoforge.configuration.uitext.filenametooltip", modConfig.getFileName()).withStyle(FILENAME_TOOLTIP_STYLE)
               );
               btn.setTooltip(Tooltip.create(tooltip));
               this.list.addSmall(btn, null);
               count++;
            }
         }
      }

      if (count == 1) {
         this.autoClose = true;
         btn.onPress();
      }
   }

   public Component translatableConfig(ModConfig modConfig, String suffix, String fallback) {
      return Component.translatable(
         translationChecker.check(
            this.mod.getModId()
               + ".configuration.section."
               + modConfig.getFileName().replaceAll("[^a-zA-Z0-9]+", ".").replaceFirst("^\\.", "").replaceFirst("\\.$", "").toLowerCase(Locale.ENGLISH)
               + suffix,
            fallback
         ),
         this.mod.getModInfo().getDisplayName()
      );
   }

   @Override
   public void added() {
      super.added();
      if (this.autoClose) {
         this.autoClose = false;
         this.onClose();
      }
   }

   @Override
   public void onClose() {
      translationChecker.finish();
      switch (this.needsRestart) {
         case GAME:
            this.minecraft.setScreen(new ConfigurationScreen.TooltipConfirmScreen(b -> {
               if (b) {
                  this.minecraft.stop();
               } else {
                  super.onClose();
               }
            }, GAME_RESTART_TITLE, GAME_RESTART_MESSAGE, GAME_RESTART_YES, RESTART_NO));
            return;
         case WORLD:
            if (this.minecraft.level != null) {
               this.minecraft.setScreen(new ConfigurationScreen.TooltipConfirmScreen(b -> {
                  if (b) {
                     this.onDisconnect();
                  } else {
                     super.onClose();
                  }
               }, SERVER_RESTART_TITLE, SERVER_RESTART_MESSAGE, this.minecraft.isLocalServer() ? RETURN_TO_MENU : CommonComponents.GUI_DISCONNECT, RESTART_NO));
               return;
            }
         default:
            super.onClose();
      }
   }

   private void onDisconnect() {
      boolean flag = this.minecraft.isLocalServer();
      ServerData serverdata = this.minecraft.getCurrentServer();
      this.minecraft.level.disconnect();
      if (flag) {
         this.minecraft.disconnect(new GenericMessageScreen(SAVING_LEVEL));
      } else {
         this.minecraft.disconnect();
      }

      TitleScreen titlescreen = new TitleScreen();
      if (flag) {
         this.minecraft.setScreen(titlescreen);
      } else if (serverdata != null && serverdata.isRealm()) {
         this.minecraft.setScreen(new RealmsMainScreen(titlescreen));
      } else {
         this.minecraft.setScreen(new JoinMultiplayerScreen(titlescreen));
      }
   }

   public static class ConfigurationListScreen<T> extends ConfigurationScreen.ConfigurationSectionScreen {
      protected final String key;
      protected final ModConfigSpec.ListValueSpec spec;
      protected final ModConfigSpec.ConfigValue<List<T>> valueList;
      protected List<T> cfgList;

      public ConfigurationListScreen(
         ConfigurationScreen.ConfigurationSectionScreen.Context context,
         String key,
         Component title,
         ModConfigSpec.ListValueSpec spec,
         ModConfigSpec.ConfigValue<List<T>> valueList
      ) {
         super(context, title);
         this.key = key;
         this.spec = spec;
         this.valueList = valueList;
         this.cfgList = new ArrayList<>(valueList.getRaw());
      }

      @Override
      protected ConfigurationScreen.ConfigurationSectionScreen rebuild() {
         if (this.list != null) {
            this.list.children().clear();

            for (int idx = 0; idx < this.cfgList.size(); idx++) {
               T entry = this.cfgList.get(idx);

               ConfigurationScreen.ConfigurationSectionScreen.Element element = switch (entry) {
                  case null -> null;
                  case Boolean value -> this.createBooleanListValue(idx, value);
                  case Integer valuex -> this.createIntegerListValue(idx, valuex);
                  case Long valuexx -> this.createLongListValue(idx, valuexx);
                  case Double valuexxx -> this.createDoubleListValue(idx, valuexxx);
                  case String valuexxxx -> this.createStringListValue(idx, valuexxxx);
                  default -> this.createOtherValue(idx, entry);
               };
               if (element != null) {
                  AbstractWidget widget = element.getWidget(this.options);
                  if (widget instanceof EditBox box) {
                     box.setValue(box.getValue());
                  }

                  this.list.addSmall(this.createListLabel(idx), widget);
               }
            }

            this.createAddElementButton();
            if (this.undoButton == null) {
               this.createUndoButton();
               this.createResetButton();
            }
         }

         return this;
      }

      @Override
      protected boolean isAnyNondefault() {
         return !this.cfgList.equals(this.valueList.getDefault());
      }

      protected void createAddElementButton() {
         Supplier<?> newElement = this.spec.getNewElementSupplier();
         ModConfigSpec.Range<Integer> sizeRange = this.spec.getSizeRange();
         if (newElement != null && sizeRange.test(this.cfgList.size() + 1)) {
            this.list.addSmall(new StringWidget(150, 20, Component.empty(), this.font), Button.builder(ConfigurationScreen.NEW_LIST_ELEMENT, button -> {
               List<T> newValue = new ArrayList<>(this.cfgList);
               newValue.add((T)newElement.get());
               this.undoManager.add(v -> {
                  this.cfgList = v;
                  this.onChanged(this.key);
               }, newValue, v -> {
                  this.cfgList = v;
                  this.onChanged(this.key);
               }, this.cfgList);
               this.rebuild();
            }).build());
         }
      }

      protected AbstractWidget createListLabel(int idx) {
         return new ConfigurationScreen.ConfigurationListScreen.ListLabelWidget(
            0, 0, 150, 20, Component.translatable("neoforge.configuration.uitext.listelement", idx), idx
         );
      }

      @Nullable
      protected ConfigurationScreen.ConfigurationSectionScreen.Element createOtherValue(int idx, T entry) {
         StringWidget label = new StringWidget(150, 20, Component.literal(Objects.toString(entry)), this.font).alignLeft();
         label.setTooltip(Tooltip.create(ConfigurationScreen.UNSUPPORTED_ELEMENT));
         return new ConfigurationScreen.ConfigurationSectionScreen.Element(
            this.getTranslationComponent(this.key), this.getTooltipComponent(this.key, null), label, false
         );
      }

      @Nullable
      protected ConfigurationScreen.ConfigurationSectionScreen.Element createStringListValue(int idx, String value) {
         return this.createStringValue(this.key, v -> this.spec.test(List.of(v)), () -> value, newValue -> this.cfgList.set(idx, (T)newValue));
      }

      @Nullable
      protected ConfigurationScreen.ConfigurationSectionScreen.Element createDoubleListValue(int idx, Double value) {
         return this.createNumberBox(
            this.key, this.spec, () -> value, newValue -> this.cfgList.set(idx, (T)newValue), v -> this.spec.test(List.of(v)), Double::parseDouble, 0.0
         );
      }

      @Nullable
      protected ConfigurationScreen.ConfigurationSectionScreen.Element createLongListValue(int idx, Long value) {
         return this.createNumberBox(
            this.key, this.spec, () -> value, newValue -> this.cfgList.set(idx, (T)newValue), v -> this.spec.test(List.of(v)), Long::decode, 0L
         );
      }

      @Nullable
      protected ConfigurationScreen.ConfigurationSectionScreen.Element createIntegerListValue(int idx, Integer value) {
         return this.createNumberBox(
            this.key, this.spec, () -> value, newValue -> this.cfgList.set(idx, (T)newValue), v -> this.spec.test(List.of(v)), Integer::decode, 0
         );
      }

      @Nullable
      protected ConfigurationScreen.ConfigurationSectionScreen.Element createBooleanListValue(int idx, Boolean value) {
         return this.createBooleanValue(this.key, this.spec, () -> value, newValue -> this.cfgList.set(idx, (T)newValue));
      }

      protected boolean swap(int idx, boolean simulate) {
         List<T> values = new ArrayList<>(this.cfgList);
         values.add(idx, values.remove(idx + 1));
         return this.addUndoListener(simulate, values);
      }

      protected boolean del(int idx, boolean simulate) {
         List<T> values = new ArrayList<>(this.cfgList);
         values.remove(idx);
         return this.addUndoListener(simulate, values);
      }

      private boolean addUndoListener(boolean simulate, List<T> values) {
         boolean valid = this.spec.test(values);
         if (!simulate && valid) {
            this.undoManager.add(v -> {
               this.cfgList = v;
               this.onChanged(this.key);
            }, values, v -> {
               this.cfgList = v;
               this.onChanged(this.key);
            }, this.cfgList);
            this.rebuild();
         }

         return valid;
      }

      @Override
      public void onClose() {
         if (this.changed && this.spec.test(this.cfgList)) {
            this.valueList.set(this.cfgList);
            if (this.context.parent instanceof ConfigurationScreen.ConfigurationSectionScreen parent) {
               parent.onChanged(this.key);
            }
         }

         super.onClose();
      }

      @Override
      public void render(GuiGraphics graphics, int p_281550_, int p_282878_, float p_282465_) {
         this.doneButton.active = this.spec.test(this.cfgList);
         super.render(graphics, p_281550_, p_282878_, p_282465_);
      }

      @Override
      protected void onChanged(String key) {
         this.changed = true;
      }

      @Override
      protected void createResetButton() {
         this.resetButton = Button.builder(ConfigurationScreen.RESET, button -> {
            this.undoManager.add(v -> {
               this.cfgList = v;
               this.onChanged(this.key);
            }, new ArrayList((List)this.getValueSpec(this.key).correct(null)), v -> {
               this.cfgList = v;
               this.onChanged(this.key);
            }, this.cfgList);
            this.rebuild();
         }).tooltip(Tooltip.create(ConfigurationScreen.RESET_TOOLTIP)).width(120).build();
      }

      public class ListLabelWidget extends AbstractContainerWidget {
         protected final Button upButton = Button.builder(ConfigurationScreen.MOVE_LIST_ELEMENT_UP, this::up).build();
         protected final Button downButton = Button.builder(ConfigurationScreen.MOVE_LIST_ELEMENT_DOWN, this::down).build();
         protected final Button delButton = Button.builder(ConfigurationScreen.REMOVE_LIST_ELEMENT, this::rem).build();
         protected final StringWidget label = new StringWidget(0, 0, 0, 0, Component.empty(), ConfigurationListScreen.this.font).alignLeft();
         protected final int idx;
         protected final boolean isFirst;
         protected final boolean isLast;

         public ListLabelWidget(final int x, final int y, final int width, final int height, final Component labelText, final int idx) {
            super(x, y, width, height, labelText);
            this.idx = idx;
            this.isFirst = idx == 0;
            this.isLast = idx + 1 == ConfigurationListScreen.this.cfgList.size();
            this.label.setMessage(labelText);
            this.checkButtons();
            this.updateLayout();
         }

         @Override
         public void setX(int pX) {
            super.setX(pX);
            this.updateLayout();
         }

         @Override
         public void setY(int pY) {
            super.setY(pY);
            this.updateLayout();
         }

         @Override
         public void setHeight(int pHeight) {
            super.setHeight(pHeight);
            this.updateLayout();
         }

         @Override
         public void setWidth(int pWidth) {
            super.setWidth(pWidth);
            this.updateLayout();
         }

         @Override
         public void setSize(int pWidth, int pHeight) {
            super.setSize(pWidth, pHeight);
            this.updateLayout();
         }

         protected void updateLayout() {
            this.upButton.setX(this.getX());
            this.downButton.setX(this.getX() + this.getHeight() + 2);
            this.delButton.setX(this.getX() + this.getWidth() - this.getHeight());
            this.label.setX(this.getX() + 44);
            this.upButton.setY(this.getY());
            this.downButton.setY(this.getY());
            this.delButton.setY(this.getY());
            this.label.setY(this.getY());
            this.upButton.setHeight(this.getHeight());
            this.downButton.setHeight(this.getHeight());
            this.delButton.setHeight(this.getHeight());
            this.label.setHeight(this.getHeight());
            this.upButton.setWidth(this.getHeight());
            this.downButton.setWidth(this.getHeight());
            this.delButton.setWidth(this.getHeight());
            this.label.setWidth(this.getWidth() - 3 * (this.getHeight() + 2));
         }

         void up(Button button) {
            ConfigurationListScreen.this.swap(this.idx - 1, false);
         }

         void down(Button button) {
            ConfigurationListScreen.this.swap(this.idx, false);
         }

         void rem(Button button) {
            ConfigurationListScreen.this.del(this.idx, false);
         }

         @Override
         public List<? extends GuiEventListener> children() {
            return List.of(this.upButton, this.label, this.downButton, this.delButton);
         }

         @Override
         protected void renderWidget(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
            this.checkButtons();
            this.label.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
            if (!this.isFirst) {
               this.upButton.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
            }

            if (!this.isLast) {
               this.downButton.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
            }

            this.delButton.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
         }

         protected void checkButtons() {
            this.upButton.visible = !this.isFirst;
            this.upButton.active = !this.isFirst && ConfigurationListScreen.this.swap(this.idx - 1, true);
            this.downButton.visible = !this.isLast;
            this.downButton.active = !this.isLast && ConfigurationListScreen.this.swap(this.idx, true);
            ModConfigSpec.Range<Integer> sizeRange = ConfigurationListScreen.this.spec.getSizeRange();
            this.delButton.active = !ConfigurationListScreen.this.cfgList.isEmpty()
               && (sizeRange == null || sizeRange.test(ConfigurationListScreen.this.cfgList.size() - 1))
               && ConfigurationListScreen.this.del(this.idx, true);
         }

         @Override
         protected void updateWidgetNarration(NarrationElementOutput pNarrationElementOutput) {
         }
      }
   }

   public static class ConfigurationSectionScreen extends OptionsSubScreen {
      protected static final long MAX_SLIDER_SIZE = 256L;
      protected final ConfigurationScreen.ConfigurationSectionScreen.Context context;
      protected boolean changed = false;
      protected ModConfigSpec.RestartType needsRestart = ModConfigSpec.RestartType.NONE;
      protected final Map<String, ConfigurationScreen.ConfigurationSectionScreen> sectionCache = new HashMap<>();
      @Nullable
      protected Button undoButton;
      @Nullable
      protected Button resetButton;
      protected final Button doneButton = Button.builder(CommonComponents.GUI_DONE, button -> this.onClose()).width(120).build();
      protected final ConfigurationScreen.UndoManager undoManager = new ConfigurationScreen.UndoManager();

      public ConfigurationSectionScreen(Screen parent, Type type, ModConfig modConfig, Component title) {
         this(parent, type, modConfig, title, (c, k, e) -> e);
      }

      public ConfigurationSectionScreen(
         Screen parent, Type type, ModConfig modConfig, Component title, ConfigurationScreen.ConfigurationSectionScreen.Filter filter
      ) {
         this(ConfigurationScreen.ConfigurationSectionScreen.Context.top(modConfig.getModId(), parent, modConfig, filter), title);
         this.needsRestart = type == Type.STARTUP ? ModConfigSpec.RestartType.GAME : ModConfigSpec.RestartType.NONE;
      }

      public ConfigurationSectionScreen(
         ConfigurationScreen.ConfigurationSectionScreen.Context parentContext,
         Screen parent,
         Map<String, Object> valueSpecs,
         String key,
         Set<? extends Entry> entrySet,
         Component title
      ) {
         this(
            ConfigurationScreen.ConfigurationSectionScreen.Context.section(parentContext, parent, entrySet, valueSpecs, key),
            Component.translatable("neoforge.configuration.uitext.breadcrumb.order", parent.getTitle(), ConfigurationScreen.CRUMB_SEPARATOR, title)
         );
      }

      protected ConfigurationSectionScreen(ConfigurationScreen.ConfigurationSectionScreen.Context context, Component title) {
         super(context.parent, Minecraft.getInstance().options, title);
         this.context = context;
      }

      @Nullable
      protected ModConfigSpec.ValueSpec getValueSpec(String key) {
         return this.context.valueSpecs.get(key) instanceof ModConfigSpec.ValueSpec vs ? vs : null;
      }

      protected String getTranslationKey(String key) {
         ModConfigSpec.ValueSpec valueSpec = this.getValueSpec(key);
         String result = valueSpec != null ? valueSpec.getTranslationKey() : this.context.modSpec.getLevelTranslationKey(this.context.makeKeyList(key));
         return result != null ? result : this.context.modId + ".configuration." + key;
      }

      protected MutableComponent getTranslationComponent(String key) {
         return Component.translatable(ConfigurationScreen.translationChecker.check(this.getTranslationKey(key)));
      }

      protected String getComment(String key) {
         ModConfigSpec.ValueSpec valueSpec = this.getValueSpec(key);
         return valueSpec != null ? this.getValueSpec(key).getComment() : this.context.modSpec.getLevelComment(this.context.makeKeyList(key));
      }

      protected <T> OptionInstance.TooltipSupplier<T> getTooltip(String key, @Nullable ModConfigSpec.Range<?> range) {
         return OptionInstance.cachedConstantTooltip(this.getTooltipComponent(key, range));
      }

      protected Component getTooltipComponent(String key, @Nullable ModConfigSpec.Range<?> range) {
         String tooltipKey = this.getTranslationKey(key) + ".tooltip";
         String comment = this.getComment(key);
         boolean hasTranslatedTooltip = ConfigurationScreen.translationChecker.existsWithFallback(tooltipKey);
         MutableComponent component = Component.empty().append(this.getTranslationComponent(key).withStyle(ChatFormatting.BOLD));
         if (hasTranslatedTooltip || !Strings.isBlank(comment)) {
            component = component.append(ConfigurationScreen.EMPTY_LINE).append(Component.translatableWithFallback(tooltipKey, comment));
         }

         component = component.append(
            ConfigurationScreen.translationChecker.optional(ConfigurationScreen.EMPTY_LINE, tooltipKey + ".warning", ChatFormatting.RED, ChatFormatting.BOLD)
         );
         if (hasTranslatedTooltip && range != null) {
            component = component.append(ConfigurationScreen.EMPTY_LINE)
               .append(
                  Component.translatable("neoforge.configuration.uitext.rangetooltip", range.toString()).withStyle(ConfigurationScreen.RANGE_TOOLTIP_STYLE)
               );
         }

         return component;
      }

      protected void onChanged(String key) {
         this.changed = true;
         ModConfigSpec.ValueSpec valueSpec = this.getValueSpec(key);
         if (valueSpec != null) {
            this.needsRestart = this.needsRestart.with(valueSpec.restartType());
         }
      }

      @Override
      protected void addOptions() {
         this.rebuild();
      }

      protected ConfigurationScreen.ConfigurationSectionScreen rebuild() {
         if (this.list != null) {
            this.list.children().clear();
            boolean hasUndoableElements = false;
            List<ConfigurationScreen.ConfigurationSectionScreen.Element> elements = new ArrayList<>();

            label114:
            for (Entry entry : this.context.entries) {
               String key = entry.getKey();
               Object rawValue = entry.getRawValue();
               Object var10000 = entry.getRawValue();
               Objects.requireNonNull(var10000);
               Object var7 = var10000;
               switch (var7) {
                  case ModConfigSpec.ConfigValue cv:
                     ModConfigSpec.ValueSpec valueSpec = this.getValueSpec(key);
                     ModConfigSpec.ValueSpec var32 = valueSpec;
                     byte var13 = 0;

                     while (true) {
                        switch (SwitchBootstraps.typeSwitch<"typeSwitch",ModConfigSpec.ListValueSpec,ModConfigSpec.ValueSpec,ModConfigSpec.ValueSpec,ModConfigSpec.ValueSpec,ModConfigSpec.ValueSpec,ModConfigSpec.ValueSpec>(
                           var32, var13
                        )) {
                           case -1:
                              var10000 = null;
                              break;
                           case 0:
                              ModConfigSpec.ListValueSpec listValueSpec = (ModConfigSpec.ListValueSpec)var32;
                              var10000 = this.createList(key, listValueSpec, cv);
                              break;
                           case 1:
                              if (cv.getClass() != ModConfigSpec.ConfigValue.class || !(var32.getDefault() instanceof String)) {
                                 var13 = 2;
                                 continue;
                              }

                              var10000 = this.createStringValue(key, valueSpec::test, () -> (String)cv.getRaw(), cv::set);
                              break;
                           case 2:
                              if (cv.getClass() != ModConfigSpec.ConfigValue.class || !(var32.getDefault() instanceof Integer)) {
                                 var13 = 3;
                                 continue;
                              }

                              var10000 = this.createIntegerValue(key, valueSpec, () -> (Integer)cv.getRaw(), cv::set);
                              break;
                           case 3:
                              if (cv.getClass() != ModConfigSpec.ConfigValue.class || !(var32.getDefault() instanceof Long)) {
                                 var13 = 4;
                                 continue;
                              }

                              var10000 = this.createLongValue(key, valueSpec, () -> (Long)cv.getRaw(), cv::set);
                              break;
                           case 4:
                              if (cv.getClass() != ModConfigSpec.ConfigValue.class || !(var32.getDefault() instanceof Double)) {
                                 var13 = 5;
                                 continue;
                              }

                              var10000 = this.createDoubleValue(key, valueSpec, () -> (Double)cv.getRaw(), cv::set);
                              break;
                           case 5:
                              if (cv.getClass() != ModConfigSpec.ConfigValue.class || !(var32.getDefault() instanceof Enum)) {
                                 var13 = 6;
                                 continue;
                              }

                              var10000 = this.createEnumValue(key, valueSpec, cv::getRaw, cv::set);
                              break;
                           default:
                              switch (cv) {
                                 case ModConfigSpec.BooleanValue value:
                                    var10000 = this.createBooleanValue(key, valueSpec, value::getRaw, value::set);
                                    break;
                                 case ModConfigSpec.IntValue valuex:
                                    var10000 = this.createIntegerValue(key, valueSpec, valuex::getRaw, valuex::set);
                                    break;
                                 case ModConfigSpec.LongValue valuexx:
                                    var10000 = this.createLongValue(key, valueSpec, valuexx::getRaw, valuexx::set);
                                    break;
                                 case ModConfigSpec.DoubleValue valuexxx:
                                    var10000 = this.createDoubleValue(key, valueSpec, valuexxx::getRaw, valuexxx::set);
                                    break;
                                 case ModConfigSpec.EnumValue valuexxxx:
                                    var10000 = this.createEnumValue(key, valueSpec, valuexxxx::getRaw, valuexxxx::set);
                                    break;
                                 default:
                                    var10000 = this.createOtherValue(key, cv);
                              }
                        }

                        ConfigurationScreen.ConfigurationSectionScreen.Element element = (ConfigurationScreen.ConfigurationSectionScreen.Element)var10000;
                        elements.add(this.context.filter.filterEntry(this.context, key, element));
                        continue label114;
                     }
                  case UnmodifiableConfig subsection when this.context.valueSpecs.get(key) instanceof UnmodifiableConfig subconfig:
                     elements.add(this.createSection(key, subconfig, subsection));
                     break;
                  default:
                     elements.add(this.context.filter.filterEntry(this.context, key, this.createOtherSection(key, rawValue)));
               }
            }

            elements.addAll(this.createSyntheticValues());

            for (ConfigurationScreen.ConfigurationSectionScreen.Element element : elements) {
               if (element != null) {
                  if (element.name() == null) {
                     this.list.addSmall(new StringWidget(150, 20, Component.empty(), this.font), element.getWidget(this.options));
                  } else {
                     StringWidget label = new StringWidget(150, 20, element.name, this.font).alignLeft();
                     label.setTooltip(Tooltip.create(element.tooltip));
                     this.list.addSmall(label, element.getWidget(this.options));
                  }

                  hasUndoableElements |= element.undoable;
               }
            }

            if (hasUndoableElements && this.undoButton == null) {
               this.createUndoButton();
               this.createResetButton();
            }
         }

         return this;
      }

      protected Collection<? extends ConfigurationScreen.ConfigurationSectionScreen.Element> createSyntheticValues() {
         return Collections.emptyList();
      }

      protected boolean isNonDefault(ModConfigSpec.ConfigValue<?> cv) {
         return !Objects.equals(cv.getRaw(), cv.getDefault());
      }

      protected boolean isAnyNondefault() {
         for (Entry entry : this.context.entries) {
            if (entry.getRawValue() instanceof ModConfigSpec.ConfigValue<?> cv
               && !(this.getValueSpec(entry.getKey()) instanceof ModConfigSpec.ListValueSpec)
               && this.isNonDefault(cv)) {
               return true;
            }
         }

         return false;
      }

      @Nullable
      protected ConfigurationScreen.ConfigurationSectionScreen.Element createStringValue(
         String key, Predicate<String> tester, Supplier<String> source, Consumer<String> target
      ) {
         if (source.get().length() > 192) {
            StringWidget label = new StringWidget(150, 20, Component.literal(source.get().substring(0, 128)), this.font).alignLeft();
            label.setTooltip(Tooltip.create(ConfigurationScreen.LONG_STRING));
            return new ConfigurationScreen.ConfigurationSectionScreen.Element(
               this.getTranslationComponent(key), this.getTooltipComponent(key, null), label, false
            );
         } else {
            EditBox box = new EditBox(this.font, 150, 20, this.getTranslationComponent(key));
            box.setEditable(true);
            box.setTooltip(Tooltip.create(this.getTooltipComponent(key, null)));
            box.setMaxLength(Mth.clamp(source.get().length() + 5, 128, 192));
            box.setValue(source.get());
            box.setResponder(newValue -> {
               if (newValue != null && tester.test(newValue)) {
                  if (!newValue.equals(source.get())) {
                     this.undoManager.add(v -> {
                        target.accept(v);
                        this.onChanged(key);
                     }, newValue, v -> {
                        target.accept(v);
                        this.onChanged(key);
                     }, source.get());
                  }

                  box.setTextColor(14737632);
               } else {
                  box.setTextColor(-65536);
               }
            });
            return new ConfigurationScreen.ConfigurationSectionScreen.Element(this.getTranslationComponent(key), this.getTooltipComponent(key, null), box);
         }
      }

      @Nullable
      protected ConfigurationScreen.ConfigurationSectionScreen.Element createOtherSection(String key, Object value) {
         return null;
      }

      @Nullable
      protected ConfigurationScreen.ConfigurationSectionScreen.Element createOtherValue(String key, ModConfigSpec.ConfigValue<?> value) {
         StringWidget label = new StringWidget(150, 20, Component.literal(Objects.toString(value.getRaw())), this.font).alignLeft();
         label.setTooltip(Tooltip.create(ConfigurationScreen.UNSUPPORTED_ELEMENT));
         return new ConfigurationScreen.ConfigurationSectionScreen.Element(this.getTranslationComponent(key), this.getTooltipComponent(key, null), label, false);
      }

      @Nullable
      protected ConfigurationScreen.ConfigurationSectionScreen.Element createBooleanValue(
         String key, ModConfigSpec.ValueSpec spec, Supplier<Boolean> source, Consumer<Boolean> target
      ) {
         return new ConfigurationScreen.ConfigurationSectionScreen.Element(
            this.getTranslationComponent(key),
            this.getTooltipComponent(key, null),
            new OptionInstance<>(
               this.getTranslationKey(key),
               this.getTooltip(key, null),
               OptionInstance.BOOLEAN_TO_STRING,
               ConfigurationScreen.ConfigurationSectionScreen.Custom.BOOLEAN_VALUES_NO_PREFIX,
               source.get(),
               newValue -> this.undoManager.add(v -> {
                  target.accept(v);
                  this.onChanged(key);
               }, newValue, v -> {
                  target.accept(v);
                  this.onChanged(key);
               }, source.get())
            )
         );
      }

      @Nullable
      protected <T extends Enum<T>> ConfigurationScreen.ConfigurationSectionScreen.Element createEnumValue(
         String key, ModConfigSpec.ValueSpec spec, Supplier<T> source, Consumer<T> target
      ) {
         Class<T> clazz = (Class<T>)spec.getClazz();

         assert clazz != null;

         List<T> list = Arrays.<T>stream(clazz.getEnumConstants()).filter(spec::test).toList();
         return new ConfigurationScreen.ConfigurationSectionScreen.Element(
            this.getTranslationComponent(key),
            this.getTooltipComponent(key, null),
            new OptionInstance<>(
               this.getTranslationKey(key),
               this.getTooltip(key, null),
               (caption, displayvalue) -> (Component)(displayvalue instanceof TranslatableEnum tenum
                  ? tenum.getTranslatedName()
                  : Component.literal(displayvalue.name())),
               new ConfigurationScreen.ConfigurationSectionScreen.Custom<>(list),
               source.get(),
               newValue -> this.undoManager.add(v -> {
                  target.accept(v);
                  this.onChanged(key);
               }, (T)newValue, v -> {
                  target.accept(v);
                  this.onChanged(key);
               }, source.get())
            )
         );
      }

      @Nullable
      protected ConfigurationScreen.ConfigurationSectionScreen.Element createIntegerValue(
         String key, ModConfigSpec.ValueSpec spec, Supplier<Integer> source, Consumer<Integer> target
      ) {
         ModConfigSpec.Range<Integer> range = spec.getRange();
         int min = range != null ? range.getMin() : 0;
         int max = range != null ? range.getMax() : Integer.MAX_VALUE;
         return (long)max - min < 256L
            ? this.createSlider(key, source, target, range)
            : this.createNumberBox(key, spec, source, target, null, Integer::decode, 0);
      }

      @Nullable
      protected ConfigurationScreen.ConfigurationSectionScreen.Element createSlider(
         String key, Supplier<Integer> source, Consumer<Integer> target, @Nullable ModConfigSpec.Range<Integer> range
      ) {
         return new ConfigurationScreen.ConfigurationSectionScreen.Element(
            this.getTranslationComponent(key),
            this.getTooltipComponent(key, null),
            new OptionInstance<>(
               this.getTranslationKey(key),
               this.getTooltip(key, range),
               (caption, displayvalue) -> Component.literal(displayvalue + ""),
               new OptionInstance.IntRange(range != null ? range.getMin() : 0, range != null ? range.getMax() : Integer.MAX_VALUE),
               null,
               source.get(),
               newValue -> {
                  if (!newValue.equals(source.get())) {
                     this.undoManager.add(v -> {
                        target.accept(v);
                        this.onChanged(key);
                     }, newValue, v -> {
                        target.accept(v);
                        this.onChanged(key);
                     }, source.get());
                  }
               }
            )
         );
      }

      @Nullable
      protected ConfigurationScreen.ConfigurationSectionScreen.Element createLongValue(
         String key, ModConfigSpec.ValueSpec spec, Supplier<Long> source, Consumer<Long> target
      ) {
         return this.createNumberBox(key, spec, source, target, null, Long::decode, 0L);
      }

      @Nullable
      protected <T extends Number & Comparable<? super T>> ConfigurationScreen.ConfigurationSectionScreen.Element createNumberBox(
         String key, ModConfigSpec.ValueSpec spec, Supplier<T> source, Consumer<T> target, @Nullable Predicate<T> tester, Function<String, T> parser, T zero
      ) {
         ModConfigSpec.Range<T> range = spec.getRange();
         EditBox box = new EditBox(this.font, 150, 20, this.getTranslationComponent(key));
         box.setEditable(true);
         box.setFilter(newValueString -> {
            try {
               parser.apply(newValueString);
               return true;
            } catch (NumberFormatException var6x) {
               return this.isPartialNumber(newValueString, range == null || range.getMin().compareTo(zero) < 0);
            }
         });
         box.setTooltip(Tooltip.create(this.getTooltipComponent(key, range)));
         box.setValue(source.get() + "");
         box.setResponder(newValueString -> {
            try {
               T newValue = parser.apply(newValueString);
               if (tester != null ? tester.test(newValue) : newValue != null && (range == null || range.test(newValue)) && spec.test(newValue)) {
                  if (!newValue.equals(source.get())) {
                     this.undoManager.add(v -> {
                        target.accept((T)v);
                        this.onChanged(key);
                     }, newValue, v -> {
                        target.accept((T)v);
                        this.onChanged(key);
                     }, source.get());
                  }

                  box.setTextColor(14737632);
                  return;
               }
            } catch (NumberFormatException var11) {
            }

            box.setTextColor(-65536);
         });
         return new ConfigurationScreen.ConfigurationSectionScreen.Element(this.getTranslationComponent(key), this.getTooltipComponent(key, null), box);
      }

      protected boolean isPartialNumber(String value, boolean allowNegative) {
         return switch (value) {
            case "" -> true;
            case "0" -> true;
            case "0x" -> true;
            case "0X" -> true;
            case "#" -> true;
            case "-" -> allowNegative;
            case "-0" -> allowNegative;
            case "-0x" -> allowNegative;
            case "-0X" -> allowNegative;
            default -> false;
         };
      }

      @Nullable
      protected ConfigurationScreen.ConfigurationSectionScreen.Element createDoubleValue(
         String key, ModConfigSpec.ValueSpec spec, Supplier<Double> source, Consumer<Double> target
      ) {
         return this.createNumberBox(key, spec, source, target, null, Double::parseDouble, 0.0);
      }

      @Nullable
      protected ConfigurationScreen.ConfigurationSectionScreen.Element createSection(String key, UnmodifiableConfig subconfig, UnmodifiableConfig subsection) {
         return subconfig.isEmpty()
            ? null
            : new ConfigurationScreen.ConfigurationSectionScreen.Element(
               Component.translatable("neoforge.configuration.uitext.section", this.getTranslationComponent(key)),
               this.getTooltipComponent(key, null),
               Button.builder(
                     Component.translatable(
                        "neoforge.configuration.uitext.section",
                        Component.translatable(
                           ConfigurationScreen.translationChecker.check(this.getTranslationKey(key) + ".button", "neoforge.configuration.uitext.sectiontext")
                        )
                     ),
                     button -> this.minecraft
                        .setScreen(
                           this.sectionCache
                              .computeIfAbsent(
                                 key,
                                 k -> new ConfigurationScreen.ConfigurationSectionScreen(
                                       this.context,
                                       this,
                                       subconfig.valueMap(),
                                       key,
                                       subsection.entrySet(),
                                       Component.translatable(this.getTranslationKey(key))
                                    )
                                    .rebuild()
                              )
                        )
                  )
                  .tooltip(Tooltip.create(this.getTooltipComponent(key, null)))
                  .width(150)
                  .build(),
               false
            );
      }

      @Nullable
      protected <T> ConfigurationScreen.ConfigurationSectionScreen.Element createList(
         String key, ModConfigSpec.ListValueSpec spec, ModConfigSpec.ConfigValue<List<T>> list
      ) {
         return new ConfigurationScreen.ConfigurationSectionScreen.Element(
            Component.translatable("neoforge.configuration.uitext.section", this.getTranslationComponent(key)),
            this.getTooltipComponent(key, null),
            Button.builder(
                  Component.translatable(
                     "neoforge.configuration.uitext.section",
                     Component.translatable(
                        ConfigurationScreen.translationChecker.check(this.getTranslationKey(key) + ".button", "neoforge.configuration.uitext.sectiontext")
                     )
                  ),
                  button -> this.minecraft
                     .setScreen(
                        this.sectionCache
                           .computeIfAbsent(
                              key,
                              k -> new ConfigurationScreen.ConfigurationListScreen<>(
                                 ConfigurationScreen.ConfigurationSectionScreen.Context.list(this.context, this),
                                 key,
                                 Component.translatable(
                                    "neoforge.configuration.uitext.breadcrumb.order",
                                    this.getTitle(),
                                    ConfigurationScreen.CRUMB_SEPARATOR,
                                    this.getTranslationComponent(key)
                                 ),
                                 spec,
                                 list
                              )
                           )
                           .rebuild()
                     )
               )
               .tooltip(Tooltip.create(this.getTooltipComponent(key, null)))
               .build(),
            false
         );
      }

      @Override
      public void render(GuiGraphics graphics, int p_281550_, int p_282878_, float p_282465_) {
         this.setUndoButtonstate(this.undoManager.canUndo());
         this.setResetButtonstate(this.isAnyNondefault());
         super.render(graphics, p_281550_, p_282878_, p_282465_);
      }

      @Override
      protected void addFooter() {
         if (this.undoButton == null && this.resetButton == null) {
            super.addFooter();
         } else {
            LinearLayout linearlayout = this.layout.addToFooter(LinearLayout.horizontal().spacing(8));
            if (this.undoButton != null) {
               linearlayout.addChild(this.undoButton);
            }

            if (this.resetButton != null) {
               linearlayout.addChild(this.resetButton);
            }

            linearlayout.addChild(this.doneButton);
         }
      }

      protected void createUndoButton() {
         this.undoButton = Button.builder(ConfigurationScreen.UNDO, button -> {
            this.undoManager.undo();
            this.rebuild();
         }).tooltip(Tooltip.create(ConfigurationScreen.UNDO_TOOLTIP)).width(120).build();
         this.undoButton.active = false;
      }

      protected void setUndoButtonstate(boolean state) {
         if (this.undoButton != null) {
            this.undoButton.active = state;
         }
      }

      protected void createResetButton() {
         this.resetButton = Button.builder(
               ConfigurationScreen.RESET,
               button -> {
                  List<ConfigurationScreen.UndoManager.Step<?>> list = new ArrayList<>();

                  for (Entry entry : this.context.entries) {
                     if (entry.getRawValue() instanceof ModConfigSpec.ConfigValue cv
                        && !(this.getValueSpec(entry.getKey()) instanceof ModConfigSpec.ListValueSpec)
                        && this.isNonDefault(cv)) {
                        String key = entry.getKey();
                        list.add(this.undoManager.step(v -> {
                           cv.set(v);
                           this.onChanged(key);
                        }, this.getValueSpec(key).correct(null), v -> {
                           cv.set(v);
                           this.onChanged(key);
                        }, cv.getRaw()));
                     }
                  }

                  this.undoManager.add(list);
                  this.rebuild();
               }
            )
            .tooltip(Tooltip.create(ConfigurationScreen.RESET_TOOLTIP))
            .width(120)
            .build();
      }

      protected void setResetButtonstate(boolean state) {
         if (this.resetButton != null) {
            this.resetButton.active = state;
         }
      }

      @Override
      public void onClose() {
         if (this.changed) {
            if (this.lastScreen instanceof ConfigurationScreen.ConfigurationSectionScreen parent) {
               parent.changed = true;
            } else {
               this.context.modSpec.save();
            }

            if (this.lastScreen instanceof ConfigurationScreen.ConfigurationSectionScreen parent) {
               parent.needsRestart = parent.needsRestart.with(this.needsRestart);
            } else if (this.lastScreen instanceof ConfigurationScreen parent) {
               parent.needsRestart = parent.needsRestart.with(this.needsRestart);
            }
         }

         super.onClose();
      }

      public record Context(
         String modId,
         Screen parent,
         ModConfig modConfig,
         ModConfigSpec modSpec,
         Set<? extends Entry> entries,
         Map<String, Object> valueSpecs,
         List<String> keylist,
         ConfigurationScreen.ConfigurationSectionScreen.Filter filter
      ) {
         @Internal
         public Context(
            String modId,
            Screen parent,
            ModConfig modConfig,
            ModConfigSpec modSpec,
            Set<? extends Entry> entries,
            Map<String, Object> valueSpecs,
            List<String> keylist,
            ConfigurationScreen.ConfigurationSectionScreen.Filter filter
         ) {
            this.modId = modId;
            this.parent = parent;
            this.modConfig = modConfig;
            this.modSpec = modSpec;
            this.entries = entries;
            this.valueSpecs = valueSpecs;
            this.keylist = keylist;
            this.filter = filter;
         }

         public static ConfigurationScreen.ConfigurationSectionScreen.Context top(
            String modId, Screen parent, ModConfig modConfig, ConfigurationScreen.ConfigurationSectionScreen.Filter filter
         ) {
            return new ConfigurationScreen.ConfigurationSectionScreen.Context(
               modId,
               parent,
               modConfig,
               (ModConfigSpec)modConfig.getSpec(),
               ((ModConfigSpec)modConfig.getSpec()).getValues().entrySet(),
               ((ModConfigSpec)modConfig.getSpec()).getSpec().valueMap(),
               List.of(),
               filter
            );
         }

         public static ConfigurationScreen.ConfigurationSectionScreen.Context section(
            ConfigurationScreen.ConfigurationSectionScreen.Context parentContext,
            Screen parent,
            Set<? extends Entry> entries,
            Map<String, Object> valueSpecs,
            String key
         ) {
            return new ConfigurationScreen.ConfigurationSectionScreen.Context(
               parentContext.modId,
               parent,
               parentContext.modConfig,
               parentContext.modSpec,
               entries,
               valueSpecs,
               parentContext.makeKeyList(key),
               parentContext.filter
            );
         }

         public static ConfigurationScreen.ConfigurationSectionScreen.Context list(
            ConfigurationScreen.ConfigurationSectionScreen.Context parentContext, Screen parent
         ) {
            return new ConfigurationScreen.ConfigurationSectionScreen.Context(
               parentContext.modId,
               parent,
               parentContext.modConfig,
               parentContext.modSpec,
               parentContext.entries,
               parentContext.valueSpecs,
               parentContext.keylist,
               null
            );
         }

         private ArrayList<String> makeKeyList(String key) {
            ArrayList<String> result = new ArrayList<>(this.keylist);
            result.add(key);
            return result;
         }
      }

      public record Custom<T>(List<T> values) implements OptionInstance.ValueSet<T> {
         public static final ConfigurationScreen.ConfigurationSectionScreen.Custom<Boolean> BOOLEAN_VALUES_NO_PREFIX = new ConfigurationScreen.ConfigurationSectionScreen.Custom<>(
            ImmutableList.of(Boolean.TRUE, Boolean.FALSE)
         );

         @Override
         public Function<OptionInstance<T>, AbstractWidget> createButton(
            OptionInstance.TooltipSupplier<T> tooltip, Options options, int x, int y, int width, Consumer<T> target
         ) {
            return optionsInstance -> CycleButton.builder(optionsInstance.toString)
               .withValues(CycleButton.ValueListSupplier.create(this.values))
               .withTooltip(tooltip)
               .displayOnlyValue()
               .withInitialValue(optionsInstance.get())
               .create(x, y, width, 20, optionsInstance.caption, (source, newValue) -> {
                  optionsInstance.set(newValue);
                  options.save();
                  target.accept(newValue);
               });
         }

         @Override
         public Optional<T> validateValue(T value) {
            return this.values.contains(value) ? Optional.of(value) : Optional.empty();
         }

         @Override
         public Codec<T> codec() {
            return null;
         }
      }

      public record Element(
         @Nullable Component name, @Nullable Component tooltip, @Nullable AbstractWidget widget, @Nullable OptionInstance<?> option, boolean undoable
      ) {
         @Internal
         public Element(
            @Nullable Component name, @Nullable Component tooltip, @Nullable AbstractWidget widget, @Nullable OptionInstance<?> option, boolean undoable
         ) {
            this.name = name;
            this.tooltip = tooltip;
            this.widget = widget;
            this.option = option;
            this.undoable = undoable;
         }

         public Element(@Nullable Component name, @Nullable Component tooltip, AbstractWidget widget) {
            this(name, tooltip, widget, null, true);
         }

         public Element(@Nullable Component name, @Nullable Component tooltip, AbstractWidget widget, boolean undoable) {
            this(name, tooltip, widget, null, undoable);
         }

         public Element(Component name, Component tooltip, OptionInstance<?> option) {
            this(name, tooltip, null, option, true);
         }

         public Element(Component name, Component tooltip, OptionInstance<?> option, boolean undoable) {
            this(name, tooltip, null, option, undoable);
         }

         public AbstractWidget getWidget(Options options) {
            return this.widget != null ? this.widget : this.option.createButton(options);
         }

         @Nullable
         public Object any() {
            return this.widget != null ? this.widget : this.option;
         }
      }

      public interface Filter {
         @Nullable
         ConfigurationScreen.ConfigurationSectionScreen.Element filterEntry(
            ConfigurationScreen.ConfigurationSectionScreen.Context var1, String var2, ConfigurationScreen.ConfigurationSectionScreen.Element var3
         );
      }
   }

   private static final class TooltipConfirmScreen extends ConfirmScreen {
      boolean seenYes = false;

      private TooltipConfirmScreen(BooleanConsumer callback, Component title, Component message, Component yesButton, Component noButton) {
         super(callback, title, message, yesButton, noButton);
      }

      @Override
      protected void init() {
         this.seenYes = false;
         super.init();
      }

      @Override
      protected void addExitButton(Button button) {
         if (this.seenYes) {
            button.setTooltip(Tooltip.create(ConfigurationScreen.RESTART_NO_TOOLTIP));
         } else {
            this.seenYes = true;
         }

         super.addExitButton(button);
      }
   }

   public static class TranslationChecker {
      private static final Logger LOGGER = LogManager.getLogger();
      private final Set<String> untranslatables = new HashSet<>();
      private final Set<String> untranslatablesWithFallback = new HashSet<>();

      public String check(String translationKey) {
         if (!I18n.exists(translationKey)) {
            this.untranslatables.add(translationKey);
         }

         return translationKey;
      }

      public String check(String translationKey, String fallback) {
         if (!I18n.exists(translationKey)) {
            this.untranslatablesWithFallback.add(translationKey);
            return this.check(fallback);
         } else {
            return translationKey;
         }
      }

      public boolean existsWithFallback(String translationKey) {
         if (!I18n.exists(translationKey)) {
            this.untranslatablesWithFallback.add(translationKey);
            return false;
         } else {
            return true;
         }
      }

      public Component optional(Component prefix, String translationKey, ChatFormatting... style) {
         return I18n.exists(translationKey)
            ? Component.empty().append(prefix).append(Component.translatable(translationKey).withStyle(style))
            : Component.empty();
      }

      public void finish() {
         if (NeoForgeConfig.CLIENT.logUntranslatedConfigurationWarnings.get()
            && !FMLLoader.isProduction()
            && (!this.untranslatables.isEmpty() || !this.untranslatablesWithFallback.isEmpty())) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(
               "\n\tDev warning - Untranslated configuration keys encountered. Please translate your configuration keys so users can properly configure your mod.\n"
            );
            if (!this.untranslatables.isEmpty()) {
               stringBuilder.append("\nUntranslated keys:");

               for (String key : this.untranslatables) {
                  stringBuilder.append("\n  \"").append(key).append("\": \"\",");
               }
            }

            if (!this.untranslatablesWithFallback.isEmpty()) {
               stringBuilder.append("\nThe following keys have fallbacks. Please check if those are suitable, and translate them if they're not.");

               for (String key : this.untranslatablesWithFallback) {
                  stringBuilder.append("\n  \"").append(key).append("\": \"\",");
               }
            }

            LOGGER.warn(stringBuilder);
         }

         this.untranslatables.clear();
      }
   }

   public static final class UndoManager {
      private final List<ConfigurationScreen.UndoManager.Step<?>> undos = new ArrayList<>();
      private final List<ConfigurationScreen.UndoManager.Step<?>> redos = new ArrayList<>();

      public void undo() {
         if (this.canUndo()) {
            ConfigurationScreen.UndoManager.Step<?> step = this.undos.removeLast();
            step.runUndo();
            this.redos.add(step);
         }
      }

      public void redo() {
         if (this.canRedo()) {
            ConfigurationScreen.UndoManager.Step<?> step = this.redos.removeLast();
            step.runRedo();
            this.undos.add(step);
         }
      }

      private void add(ConfigurationScreen.UndoManager.Step<?> step, boolean execute) {
         this.undos.add(step);
         this.redos.clear();
         if (execute) {
            step.runRedo();
         }
      }

      public <T> ConfigurationScreen.UndoManager.Step<T> step(Consumer<T> run, T newValue, Consumer<T> undo, T oldValue) {
         return new ConfigurationScreen.UndoManager.Step<>(run, newValue, undo, oldValue);
      }

      public <T> void add(Consumer<T> run, T newValue, Consumer<T> undo, T oldValue) {
         this.add(this.step(run, newValue, undo, oldValue), true);
      }

      public <T> void addNoExecute(Consumer<T> run, T newValue, Consumer<T> undo, T oldValue) {
         this.add(this.step(run, newValue, undo, oldValue), false);
      }

      public void add(ConfigurationScreen.UndoManager.Step<?>... steps) {
         this.add(ImmutableList.copyOf(steps));
      }

      public void add(List<ConfigurationScreen.UndoManager.Step<?>> steps) {
         this.add(
            new ConfigurationScreen.UndoManager.Step<>(
               n -> steps.forEach(ConfigurationScreen.UndoManager.Step::runRedo), null, n -> steps.forEach(ConfigurationScreen.UndoManager.Step::runUndo), null
            ),
            true
         );
      }

      public boolean canUndo() {
         return !this.undos.isEmpty();
      }

      public boolean canRedo() {
         return !this.redos.isEmpty();
      }

      public record Step<T>(Consumer<T> run, T newValue, Consumer<T> undo, T oldValue) {
         private void runUndo() {
            this.undo.accept(this.oldValue);
         }

         private void runRedo() {
            this.run.accept(this.newValue);
         }
      }
   }
}
