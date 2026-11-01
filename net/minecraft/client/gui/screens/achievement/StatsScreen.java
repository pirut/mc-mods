package net.minecraft.client.gui.screens.achievement;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.LoadingDotsWidget;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.stats.Stat;
import net.minecraft.stats.StatType;
import net.minecraft.stats.Stats;
import net.minecraft.stats.StatsCounter;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class StatsScreen extends Screen {
   private static final Component TITLE = Component.translatable("gui.stats");
   static final ResourceLocation SLOT_SPRITE = ResourceLocation.withDefaultNamespace("container/slot");
   static final ResourceLocation HEADER_SPRITE = ResourceLocation.withDefaultNamespace("statistics/header");
   static final ResourceLocation SORT_UP_SPRITE = ResourceLocation.withDefaultNamespace("statistics/sort_up");
   static final ResourceLocation SORT_DOWN_SPRITE = ResourceLocation.withDefaultNamespace("statistics/sort_down");
   private static final Component PENDING_TEXT = Component.translatable("multiplayer.downloadingStats");
   static final Component NO_VALUE_DISPLAY = Component.translatable("stats.none");
   private static final Component GENERAL_BUTTON = Component.translatable("stat.generalButton");
   private static final Component ITEMS_BUTTON = Component.translatable("stat.itemsButton");
   private static final Component MOBS_BUTTON = Component.translatable("stat.mobsButton");
   protected final Screen lastScreen;
   private static final int LIST_WIDTH = 280;
   private static final int PADDING = 5;
   private static final int FOOTER_HEIGHT = 58;
   private HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this, 33, 58);
   @Nullable
   private StatsScreen.GeneralStatisticsList statsList;
   @Nullable
   StatsScreen.ItemStatisticsList itemStatsList;
   @Nullable
   private StatsScreen.MobsStatisticsList mobsStatsList;
   final StatsCounter stats;
   @Nullable
   private ObjectSelectionList<?> activeList;
   private boolean isLoading = true;

   public StatsScreen(Screen p_96906_, StatsCounter p_96907_) {
      super(TITLE);
      this.lastScreen = p_96906_;
      this.stats = p_96907_;
   }

   @Override
   protected void init() {
      this.layout.addToContents(new LoadingDotsWidget(this.font, PENDING_TEXT));
      this.minecraft.getConnection().send(new ServerboundClientCommandPacket(ServerboundClientCommandPacket.Action.REQUEST_STATS));
   }

   public void initLists() {
      this.statsList = new StatsScreen.GeneralStatisticsList(this.minecraft);
      this.itemStatsList = new StatsScreen.ItemStatisticsList(this.minecraft);
      this.mobsStatsList = new StatsScreen.MobsStatisticsList(this.minecraft);
   }

   public void initButtons() {
      HeaderAndFooterLayout $$0 = new HeaderAndFooterLayout(this, 33, 58);
      $$0.addTitleHeader(TITLE, this.font);
      LinearLayout $$1 = $$0.addToFooter(LinearLayout.vertical()).spacing(5);
      $$1.defaultCellSetting().alignHorizontallyCenter();
      LinearLayout $$2 = $$1.addChild(LinearLayout.horizontal()).spacing(5);
      $$2.addChild(Button.builder(GENERAL_BUTTON, p_96963_ -> this.setActiveList(this.statsList)).width(120).build());
      Button $$3 = $$2.addChild(Button.builder(ITEMS_BUTTON, p_96959_ -> this.setActiveList(this.itemStatsList)).width(120).build());
      Button $$4 = $$2.addChild(Button.builder(MOBS_BUTTON, p_96949_ -> this.setActiveList(this.mobsStatsList)).width(120).build());
      $$1.addChild(Button.builder(CommonComponents.GUI_DONE, p_329727_ -> this.onClose()).width(200).build());
      if (this.itemStatsList != null && this.itemStatsList.children().isEmpty()) {
         $$3.active = false;
      }

      if (this.mobsStatsList != null && this.mobsStatsList.children().isEmpty()) {
         $$4.active = false;
      }

      this.layout = $$0;
      this.layout.visitWidgets(p_329726_ -> {
         AbstractWidget var10000 = this.addRenderableWidget(p_329726_);
      });
      this.repositionElements();
   }

   @Override
   protected void repositionElements() {
      this.layout.arrangeElements();
      if (this.activeList != null) {
         this.activeList.updateSize(this.width, this.layout);
      }
   }

   @Override
   public void onClose() {
      this.minecraft.setScreen(this.lastScreen);
   }

   public void onStatsUpdated() {
      if (this.isLoading) {
         this.initLists();
         this.setActiveList(this.statsList);
         this.initButtons();
         this.setInitialFocus();
         this.isLoading = false;
      }
   }

   @Override
   public boolean isPauseScreen() {
      return !this.isLoading;
   }

   public void setActiveList(@Nullable ObjectSelectionList<?> p_96925_) {
      if (this.activeList != null) {
         this.removeWidget(this.activeList);
      }

      if (p_96925_ != null) {
         this.addRenderableWidget(p_96925_);
         this.activeList = p_96925_;
         this.repositionElements();
      }
   }

   static String getTranslationKey(Stat<ResourceLocation> p_96947_) {
      return "stat." + p_96947_.getValue().toString().replace(':', '.');
   }

   @OnlyIn(Dist.CLIENT)
   class GeneralStatisticsList extends ObjectSelectionList<StatsScreen.GeneralStatisticsList.Entry> {
      public GeneralStatisticsList(Minecraft p_96995_) {
         super(p_96995_, StatsScreen.this.width, StatsScreen.this.height - 33 - 58, 33, 14);
         ObjectArrayList<Stat<ResourceLocation>> $$1 = new ObjectArrayList(Stats.CUSTOM.iterator());
         $$1.sort(Comparator.comparing(p_96997_ -> I18n.get(StatsScreen.getTranslationKey((Stat<ResourceLocation>)p_96997_))));
         ObjectListIterator var4 = $$1.iterator();

         while (var4.hasNext()) {
            Stat<ResourceLocation> $$2 = (Stat<ResourceLocation>)var4.next();
            this.addEntry(new StatsScreen.GeneralStatisticsList.Entry($$2));
         }
      }

      @Override
      public int getRowWidth() {
         return 280;
      }

      @OnlyIn(Dist.CLIENT)
      class Entry extends ObjectSelectionList.Entry<StatsScreen.GeneralStatisticsList.Entry> {
         private final Stat<ResourceLocation> stat;
         private final Component statDisplay;

         Entry(Stat<ResourceLocation> p_97005_) {
            this.stat = p_97005_;
            this.statDisplay = Component.translatable(StatsScreen.getTranslationKey(p_97005_));
         }

         private String getValueText() {
            return this.stat.format(StatsScreen.this.stats.getValue(this.stat));
         }

         @Override
         public void render(
            GuiGraphics p_283043_,
            int p_97012_,
            int p_97013_,
            int p_97014_,
            int p_97015_,
            int p_97016_,
            int p_97017_,
            int p_97018_,
            boolean p_97019_,
            float p_97020_
         ) {
            int $$10 = p_97013_ + p_97016_ / 2 - 9 / 2;
            int $$11 = p_97012_ % 2 == 0 ? -1 : -4539718;
            p_283043_.drawString(StatsScreen.this.font, this.statDisplay, p_97014_ + 2, $$10, $$11);
            String $$12 = this.getValueText();
            p_283043_.drawString(StatsScreen.this.font, $$12, p_97014_ + p_97015_ - StatsScreen.this.font.width($$12) - 4, $$10, $$11);
         }

         @Override
         public Component getNarration() {
            return Component.translatable(
               "narrator.select", Component.empty().append(this.statDisplay).append(CommonComponents.SPACE).append(this.getValueText())
            );
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   class ItemStatisticsList extends ObjectSelectionList<StatsScreen.ItemStatisticsList.ItemRow> {
      private static final int SLOT_BG_SIZE = 18;
      private static final int SLOT_STAT_HEIGHT = 22;
      private static final int SLOT_BG_Y = 1;
      private static final int SORT_NONE = 0;
      private static final int SORT_DOWN = -1;
      private static final int SORT_UP = 1;
      private final ResourceLocation[] iconSprites = new ResourceLocation[]{
         ResourceLocation.withDefaultNamespace("statistics/block_mined"),
         ResourceLocation.withDefaultNamespace("statistics/item_broken"),
         ResourceLocation.withDefaultNamespace("statistics/item_crafted"),
         ResourceLocation.withDefaultNamespace("statistics/item_used"),
         ResourceLocation.withDefaultNamespace("statistics/item_picked_up"),
         ResourceLocation.withDefaultNamespace("statistics/item_dropped")
      };
      protected final List<StatType<Block>> blockColumns;
      protected final List<StatType<Item>> itemColumns;
      protected final Comparator<StatsScreen.ItemStatisticsList.ItemRow> itemStatSorter = new StatsScreen.ItemStatisticsList.ItemRowComparator();
      @Nullable
      protected StatType<?> sortColumn;
      protected int headerPressed = -1;
      protected int sortOrder;

      public ItemStatisticsList(Minecraft p_97032_) {
         super(p_97032_, StatsScreen.this.width, StatsScreen.this.height - 33 - 58, 33, 22);
         this.blockColumns = Lists.newArrayList();
         this.blockColumns.add(Stats.BLOCK_MINED);
         this.itemColumns = Lists.newArrayList(new StatType[]{Stats.ITEM_BROKEN, Stats.ITEM_CRAFTED, Stats.ITEM_USED, Stats.ITEM_PICKED_UP, Stats.ITEM_DROPPED});
         this.setRenderHeader(true, 22);
         Set<Item> $$1 = Sets.newIdentityHashSet();

         for (Item $$2 : BuiltInRegistries.ITEM) {
            boolean $$3 = false;

            for (StatType<Item> $$4 : this.itemColumns) {
               if ($$4.contains($$2) && StatsScreen.this.stats.getValue($$4.get($$2)) > 0) {
                  $$3 = true;
               }
            }

            if ($$3) {
               $$1.add($$2);
            }
         }

         for (Block $$5 : BuiltInRegistries.BLOCK) {
            boolean $$6 = false;

            for (StatType<Block> $$7 : this.blockColumns) {
               if ($$7.contains($$5) && StatsScreen.this.stats.getValue($$7.get($$5)) > 0) {
                  $$6 = true;
               }
            }

            if ($$6) {
               $$1.add($$5.asItem());
            }
         }

         $$1.remove(Items.AIR);

         for (Item $$8 : $$1) {
            this.addEntry(new StatsScreen.ItemStatisticsList.ItemRow($$8));
         }
      }

      int getColumnX(int p_331965_) {
         return 75 + 40 * p_331965_;
      }

      @Override
      protected void renderHeader(GuiGraphics p_282214_, int p_97050_, int p_97051_) {
         if (!this.minecraft.mouseHandler.isLeftPressed()) {
            this.headerPressed = -1;
         }

         for (int $$3 = 0; $$3 < this.iconSprites.length; $$3++) {
            ResourceLocation $$4 = this.headerPressed == $$3 ? StatsScreen.SLOT_SPRITE : StatsScreen.HEADER_SPRITE;
            p_282214_.blitSprite($$4, p_97050_ + this.getColumnX($$3) - 18, p_97051_ + 1, 0, 18, 18);
         }

         if (this.sortColumn != null) {
            int $$5 = this.getColumnX(this.getColumnIndex(this.sortColumn)) - 36;
            ResourceLocation $$6 = this.sortOrder == 1 ? StatsScreen.SORT_UP_SPRITE : StatsScreen.SORT_DOWN_SPRITE;
            p_282214_.blitSprite($$6, p_97050_ + $$5, p_97051_ + 1, 0, 18, 18);
         }

         for (int $$7 = 0; $$7 < this.iconSprites.length; $$7++) {
            int $$8 = this.headerPressed == $$7 ? 1 : 0;
            p_282214_.blitSprite(this.iconSprites[$$7], p_97050_ + this.getColumnX($$7) - 18 + $$8, p_97051_ + 1 + $$8, 0, 18, 18);
         }
      }

      @Override
      public int getRowWidth() {
         return 280;
      }

      @Override
      protected boolean clickedHeader(int p_97036_, int p_97037_) {
         this.headerPressed = -1;

         for (int $$2 = 0; $$2 < this.iconSprites.length; $$2++) {
            int $$3 = p_97036_ - this.getColumnX($$2);
            if ($$3 >= -36 && $$3 <= 0) {
               this.headerPressed = $$2;
               break;
            }
         }

         if (this.headerPressed >= 0) {
            this.sortByColumn(this.getColumn(this.headerPressed));
            this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            return true;
         } else {
            return super.clickedHeader(p_97036_, p_97037_);
         }
      }

      private StatType<?> getColumn(int p_97034_) {
         return p_97034_ < this.blockColumns.size() ? this.blockColumns.get(p_97034_) : this.itemColumns.get(p_97034_ - this.blockColumns.size());
      }

      private int getColumnIndex(StatType<?> p_97059_) {
         int $$1 = this.blockColumns.indexOf(p_97059_);
         if ($$1 >= 0) {
            return $$1;
         } else {
            int $$2 = this.itemColumns.indexOf(p_97059_);
            return $$2 >= 0 ? $$2 + this.blockColumns.size() : -1;
         }
      }

      @Override
      protected void renderDecorations(GuiGraphics p_283203_, int p_97046_, int p_97047_) {
         if (p_97047_ >= this.getY() && p_97047_ <= this.getBottom()) {
            StatsScreen.ItemStatisticsList.ItemRow $$3 = this.getHovered();
            int $$4 = this.getRowLeft();
            if ($$3 != null) {
               if (p_97046_ < $$4 || p_97046_ > $$4 + 18) {
                  return;
               }

               Item $$5 = $$3.getItem();
               p_283203_.renderTooltip(StatsScreen.this.font, $$5.getDescription(), p_97046_, p_97047_);
            } else {
               Component $$6 = null;
               int $$7 = p_97046_ - $$4;

               for (int $$8 = 0; $$8 < this.iconSprites.length; $$8++) {
                  int $$9 = this.getColumnX($$8);
                  if ($$7 >= $$9 - 18 && $$7 <= $$9) {
                     $$6 = this.getColumn($$8).getDisplayName();
                     break;
                  }
               }

               if ($$6 != null) {
                  p_283203_.renderTooltip(StatsScreen.this.font, $$6, p_97046_, p_97047_);
               }
            }
         }
      }

      protected void sortByColumn(StatType<?> p_97039_) {
         if (p_97039_ != this.sortColumn) {
            this.sortColumn = p_97039_;
            this.sortOrder = -1;
         } else if (this.sortOrder == -1) {
            this.sortOrder = 1;
         } else {
            this.sortColumn = null;
            this.sortOrder = 0;
         }

         this.children().sort(this.itemStatSorter);
      }

      @OnlyIn(Dist.CLIENT)
      class ItemRow extends ObjectSelectionList.Entry<StatsScreen.ItemStatisticsList.ItemRow> {
         private final Item item;

         ItemRow(Item p_169517_) {
            this.item = p_169517_;
         }

         public Item getItem() {
            return this.item;
         }

         @Override
         public void render(
            GuiGraphics p_283614_,
            int p_97082_,
            int p_97083_,
            int p_97084_,
            int p_97085_,
            int p_97086_,
            int p_97087_,
            int p_97088_,
            boolean p_97089_,
            float p_97090_
         ) {
            p_283614_.blitSprite(StatsScreen.SLOT_SPRITE, p_97084_, p_97083_, 0, 18, 18);
            p_283614_.renderFakeItem(this.item.getDefaultInstance(), p_97084_ + 1, p_97083_ + 1);
            if (StatsScreen.this.itemStatsList != null) {
               for (int $$10 = 0; $$10 < StatsScreen.this.itemStatsList.blockColumns.size(); $$10++) {
                  Stat<Block> $$12;
                  if (this.item instanceof BlockItem $$11) {
                     $$12 = StatsScreen.this.itemStatsList.blockColumns.get($$10).get($$11.getBlock());
                  } else {
                     $$12 = null;
                  }

                  this.renderStat(p_283614_, $$12, p_97084_ + ItemStatisticsList.this.getColumnX($$10), p_97083_ + p_97086_ / 2 - 9 / 2, p_97082_ % 2 == 0);
               }

               for (int $$14 = 0; $$14 < StatsScreen.this.itemStatsList.itemColumns.size(); $$14++) {
                  this.renderStat(
                     p_283614_,
                     StatsScreen.this.itemStatsList.itemColumns.get($$14).get(this.item),
                     p_97084_ + ItemStatisticsList.this.getColumnX($$14 + StatsScreen.this.itemStatsList.blockColumns.size()),
                     p_97083_ + p_97086_ / 2 - 9 / 2,
                     p_97082_ % 2 == 0
                  );
               }
            }
         }

         protected void renderStat(GuiGraphics p_282544_, @Nullable Stat<?> p_97093_, int p_97094_, int p_97095_, boolean p_97096_) {
            Component $$5 = (Component)(p_97093_ == null
               ? StatsScreen.NO_VALUE_DISPLAY
               : Component.literal(p_97093_.format(StatsScreen.this.stats.getValue(p_97093_))));
            p_282544_.drawString(StatsScreen.this.font, $$5, p_97094_ - StatsScreen.this.font.width($$5), p_97095_, p_97096_ ? -1 : -4539718);
         }

         @Override
         public Component getNarration() {
            return Component.translatable("narrator.select", this.item.getDescription());
         }
      }

      @OnlyIn(Dist.CLIENT)
      class ItemRowComparator implements Comparator<StatsScreen.ItemStatisticsList.ItemRow> {
         public int compare(StatsScreen.ItemStatisticsList.ItemRow p_169524_, StatsScreen.ItemStatisticsList.ItemRow p_169525_) {
            Item $$2 = p_169524_.getItem();
            Item $$3 = p_169525_.getItem();
            int $$4;
            int $$5;
            if (ItemStatisticsList.this.sortColumn == null) {
               $$4 = 0;
               $$5 = 0;
            } else if (ItemStatisticsList.this.blockColumns.contains(ItemStatisticsList.this.sortColumn)) {
               StatType<Block> $$6 = (StatType<Block>)ItemStatisticsList.this.sortColumn;
               $$4 = $$2 instanceof BlockItem ? StatsScreen.this.stats.getValue($$6, ((BlockItem)$$2).getBlock()) : -1;
               $$5 = $$3 instanceof BlockItem ? StatsScreen.this.stats.getValue($$6, ((BlockItem)$$3).getBlock()) : -1;
            } else {
               StatType<Item> $$9 = (StatType<Item>)ItemStatisticsList.this.sortColumn;
               $$4 = StatsScreen.this.stats.getValue($$9, $$2);
               $$5 = StatsScreen.this.stats.getValue($$9, $$3);
            }

            return $$4 == $$5
               ? ItemStatisticsList.this.sortOrder * Integer.compare(Item.getId($$2), Item.getId($$3))
               : ItemStatisticsList.this.sortOrder * Integer.compare($$4, $$5);
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   class MobsStatisticsList extends ObjectSelectionList<StatsScreen.MobsStatisticsList.MobRow> {
      public MobsStatisticsList(Minecraft p_97100_) {
         super(p_97100_, StatsScreen.this.width, StatsScreen.this.height - 33 - 58, 33, 9 * 4);

         for (EntityType<?> $$1 : BuiltInRegistries.ENTITY_TYPE) {
            if (StatsScreen.this.stats.getValue(Stats.ENTITY_KILLED.get($$1)) > 0 || StatsScreen.this.stats.getValue(Stats.ENTITY_KILLED_BY.get($$1)) > 0) {
               this.addEntry(new StatsScreen.MobsStatisticsList.MobRow($$1));
            }
         }
      }

      @Override
      public int getRowWidth() {
         return 280;
      }

      @OnlyIn(Dist.CLIENT)
      class MobRow extends ObjectSelectionList.Entry<StatsScreen.MobsStatisticsList.MobRow> {
         private final Component mobName;
         private final Component kills;
         private final Component killedBy;
         private final boolean hasKills;
         private final boolean wasKilledBy;

         public MobRow(EntityType<?> p_97112_) {
            this.mobName = p_97112_.getDescription();
            int $$1 = StatsScreen.this.stats.getValue(Stats.ENTITY_KILLED.get(p_97112_));
            if ($$1 == 0) {
               this.kills = Component.translatable("stat_type.minecraft.killed.none", this.mobName);
               this.hasKills = false;
            } else {
               this.kills = Component.translatable("stat_type.minecraft.killed", $$1, this.mobName);
               this.hasKills = true;
            }

            int $$2 = StatsScreen.this.stats.getValue(Stats.ENTITY_KILLED_BY.get(p_97112_));
            if ($$2 == 0) {
               this.killedBy = Component.translatable("stat_type.minecraft.killed_by.none", this.mobName);
               this.wasKilledBy = false;
            } else {
               this.killedBy = Component.translatable("stat_type.minecraft.killed_by", this.mobName, $$2);
               this.wasKilledBy = true;
            }
         }

         @Override
         public void render(
            GuiGraphics p_283265_,
            int p_97115_,
            int p_97116_,
            int p_97117_,
            int p_97118_,
            int p_97119_,
            int p_97120_,
            int p_97121_,
            boolean p_97122_,
            float p_97123_
         ) {
            p_283265_.drawString(StatsScreen.this.font, this.mobName, p_97117_ + 2, p_97116_ + 1, -1);
            p_283265_.drawString(StatsScreen.this.font, this.kills, p_97117_ + 2 + 10, p_97116_ + 1 + 9, this.hasKills ? -4539718 : -8355712);
            p_283265_.drawString(StatsScreen.this.font, this.killedBy, p_97117_ + 2 + 10, p_97116_ + 1 + 9 * 2, this.wasKilledBy ? -4539718 : -8355712);
         }

         @Override
         public Component getNarration() {
            return Component.translatable("narrator.select", CommonComponents.joinForNarration(this.kills, this.killedBy));
         }
      }
   }
}
