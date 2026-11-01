package net.minecraft.client.gui.screens.worldselection;

import com.mojang.datafixers.DataFixer;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import java.util.function.ToIntFunction;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.WorldStem;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.ServerPacksSource;
import net.minecraft.util.Mth;
import net.minecraft.util.worldupdate.WorldUpgrader;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.WorldData;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class OptimizeWorldScreen extends Screen {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final ToIntFunction<ResourceKey<Level>> DIMENSION_COLORS = Util.make(new Reference2IntOpenHashMap(), p_304055_ -> {
      p_304055_.put(Level.OVERWORLD, -13408734);
      p_304055_.put(Level.NETHER, -10075085);
      p_304055_.put(Level.END, -8943531);
      p_304055_.defaultReturnValue(-2236963);
   });
   private final BooleanConsumer callback;
   private final WorldUpgrader upgrader;

   @Nullable
   public static OptimizeWorldScreen create(
      Minecraft p_101316_, BooleanConsumer p_101317_, DataFixer p_101318_, LevelStorageSource.LevelStorageAccess p_101319_, boolean p_101320_
   ) {
      try {
         WorldOpenFlows $$5 = p_101316_.createWorldOpenFlows();
         PackRepository $$6 = ServerPacksSource.createPackRepository(p_101319_);

         OptimizeWorldScreen var10;
         try (WorldStem $$7 = $$5.loadWorldStem(p_101319_.getDataTag(), false, $$6)) {
            WorldData $$8 = $$7.worldData();
            RegistryAccess.Frozen $$9 = $$7.registries().compositeAccess();
            p_101319_.saveDataTag($$9, $$8);
            var10 = new OptimizeWorldScreen(p_101317_, p_101318_, p_101319_, $$8.getLevelSettings(), p_101320_, $$9);
         }

         return var10;
      } catch (Exception var13) {
         LOGGER.warn("Failed to load datapacks, can't optimize world", var13);
         return null;
      }
   }

   private OptimizeWorldScreen(
      BooleanConsumer p_251295_,
      DataFixer p_250489_,
      LevelStorageSource.LevelStorageAccess p_248781_,
      LevelSettings p_251180_,
      boolean p_250358_,
      RegistryAccess p_323621_
   ) {
      super(Component.translatable("optimizeWorld.title", p_251180_.levelName()));
      this.callback = p_251295_;
      this.upgrader = new WorldUpgrader(p_248781_, p_250489_, p_323621_, p_250358_, false);
   }

   @Override
   protected void init() {
      super.init();
      this.addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, p_101322_ -> {
         this.upgrader.cancel();
         this.callback.accept(false);
      }).bounds(this.width / 2 - 100, this.height / 4 + 150, 200, 20).build());
   }

   @Override
   public void tick() {
      if (this.upgrader.isFinished()) {
         this.callback.accept(true);
      }
   }

   @Override
   public void onClose() {
      this.callback.accept(false);
   }

   @Override
   public void removed() {
      this.upgrader.cancel();
   }

   @Override
   public void render(GuiGraphics p_281829_, int p_101312_, int p_101313_, float p_101314_) {
      super.render(p_281829_, p_101312_, p_101313_, p_101314_);
      p_281829_.drawCenteredString(this.font, this.title, this.width / 2, 20, 16777215);
      int $$4 = this.width / 2 - 150;
      int $$5 = this.width / 2 + 150;
      int $$6 = this.height / 4 + 100;
      int $$7 = $$6 + 10;
      p_281829_.drawCenteredString(this.font, this.upgrader.getStatus(), this.width / 2, $$6 - 9 - 2, 10526880);
      if (this.upgrader.getTotalChunks() > 0) {
         p_281829_.fill($$4 - 1, $$6 - 1, $$5 + 1, $$7 + 1, -16777216);
         p_281829_.drawString(this.font, Component.translatable("optimizeWorld.info.converted", this.upgrader.getConverted()), $$4, 40, 10526880);
         p_281829_.drawString(this.font, Component.translatable("optimizeWorld.info.skipped", this.upgrader.getSkipped()), $$4, 40 + 9 + 3, 10526880);
         p_281829_.drawString(this.font, Component.translatable("optimizeWorld.info.total", this.upgrader.getTotalChunks()), $$4, 40 + (9 + 3) * 2, 10526880);
         int $$8 = 0;

         for (ResourceKey<Level> $$9 : this.upgrader.levels()) {
            int $$10 = Mth.floor(this.upgrader.dimensionProgress($$9) * ($$5 - $$4));
            p_281829_.fill($$4 + $$8, $$6, $$4 + $$8 + $$10, $$7, DIMENSION_COLORS.applyAsInt($$9));
            $$8 += $$10;
         }

         int $$11 = this.upgrader.getConverted() + this.upgrader.getSkipped();
         Component $$12 = Component.translatable("optimizeWorld.progress.counter", $$11, this.upgrader.getTotalChunks());
         Component $$13 = Component.translatable("optimizeWorld.progress.percentage", Mth.floor(this.upgrader.getProgress() * 100.0F));
         p_281829_.drawCenteredString(this.font, $$12, this.width / 2, $$6 + 2 * 9 + 2, 10526880);
         p_281829_.drawCenteredString(this.font, $$13, this.width / 2, $$6 + ($$7 - $$6) / 2 - 9 / 2, 10526880);
      }
   }
}
