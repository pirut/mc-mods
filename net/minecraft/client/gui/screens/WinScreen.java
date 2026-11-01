package net.minecraft.client.gui.screens;

import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.LogoRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.Music;
import net.minecraft.sounds.Musics;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.RandomSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class WinScreen extends Screen {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final ResourceLocation VIGNETTE_LOCATION = ResourceLocation.withDefaultNamespace("textures/misc/credits_vignette.png");
   private static final Component SECTION_HEADING = Component.literal("============").withStyle(ChatFormatting.WHITE);
   private static final String NAME_PREFIX = "           ";
   private static final String OBFUSCATE_TOKEN = "" + ChatFormatting.WHITE + ChatFormatting.OBFUSCATED + ChatFormatting.GREEN + ChatFormatting.AQUA;
   private static final float SPEEDUP_FACTOR = 5.0F;
   private static final float SPEEDUP_FACTOR_FAST = 15.0F;
   private static final ResourceLocation END_POEM_LOCATION = ResourceLocation.withDefaultNamespace("texts/end.txt");
   private static final ResourceLocation CREDITS_LOCATION = ResourceLocation.withDefaultNamespace("texts/credits.json");
   private static final ResourceLocation POSTCREDITS_LOCATION = ResourceLocation.withDefaultNamespace("texts/postcredits.txt");
   private final boolean poem;
   private final Runnable onFinished;
   private float scroll;
   private List<FormattedCharSequence> lines;
   private IntSet centeredLines;
   private int totalScrollLength;
   private boolean speedupActive;
   private final IntSet speedupModifiers = new IntOpenHashSet();
   private float scrollSpeed;
   private final float unmodifiedScrollSpeed;
   private int direction;
   private final LogoRenderer logoRenderer = new LogoRenderer(false);

   public WinScreen(boolean p_276286_, Runnable p_276294_) {
      super(GameNarrator.NO_TITLE);
      this.poem = p_276286_;
      this.onFinished = p_276294_;
      if (!p_276286_) {
         this.unmodifiedScrollSpeed = 0.75F;
      } else {
         this.unmodifiedScrollSpeed = 0.5F;
      }

      this.direction = 1;
      this.scrollSpeed = this.unmodifiedScrollSpeed;
   }

   private float calculateScrollSpeed() {
      return this.speedupActive
         ? this.unmodifiedScrollSpeed * (5.0F + this.speedupModifiers.size() * 15.0F) * this.direction
         : this.unmodifiedScrollSpeed * this.direction;
   }

   @Override
   public void tick() {
      this.minecraft.getMusicManager().tick();
      this.minecraft.getSoundManager().tick(false);
      float $$0 = this.totalScrollLength + this.height + this.height + 24;
      if (this.scroll > $$0) {
         this.respawn();
      }
   }

   @Override
   public boolean keyPressed(int p_169469_, int p_169470_, int p_169471_) {
      if (p_169469_ == 265) {
         this.direction = -1;
      } else if (p_169469_ == 341 || p_169469_ == 345) {
         this.speedupModifiers.add(p_169469_);
      } else if (p_169469_ == 32) {
         this.speedupActive = true;
      }

      this.scrollSpeed = this.calculateScrollSpeed();
      return super.keyPressed(p_169469_, p_169470_, p_169471_);
   }

   @Override
   public boolean keyReleased(int p_169476_, int p_169477_, int p_169478_) {
      if (p_169476_ == 265) {
         this.direction = 1;
      }

      if (p_169476_ == 32) {
         this.speedupActive = false;
      } else if (p_169476_ == 341 || p_169476_ == 345) {
         this.speedupModifiers.remove(p_169476_);
      }

      this.scrollSpeed = this.calculateScrollSpeed();
      return super.keyReleased(p_169476_, p_169477_, p_169478_);
   }

   @Override
   public void onClose() {
      this.respawn();
   }

   private void respawn() {
      this.onFinished.run();
   }

   @Override
   protected void init() {
      if (this.lines == null) {
         this.lines = Lists.newArrayList();
         this.centeredLines = new IntOpenHashSet();
         if (this.poem) {
            this.wrapCreditsIO(END_POEM_LOCATION, this::addPoemFile);
         }

         this.wrapCreditsIO(CREDITS_LOCATION, this::addCreditsFile);
         if (this.poem) {
            this.wrapCreditsIO(POSTCREDITS_LOCATION, this::addPoemFile);
         }

         this.totalScrollLength = this.lines.size() * 12;
      }
   }

   private void wrapCreditsIO(ResourceLocation p_350854_, WinScreen.CreditsReader p_197400_) {
      try (Reader $$2 = this.minecraft.getResourceManager().openAsReader(p_350854_)) {
         p_197400_.read($$2);
      } catch (Exception var8) {
         LOGGER.error("Couldn't load credits from file {}", p_350854_, var8);
      }
   }

   private void addPoemFile(Reader p_232818_) throws IOException {
      BufferedReader $$1 = new BufferedReader(p_232818_);
      RandomSource $$2 = RandomSource.create(8124371L);

      String $$3;
      while (($$3 = $$1.readLine()) != null) {
         $$3 = $$3.replaceAll("PLAYERNAME", this.minecraft.getUser().getName());

         int $$4;
         while (($$4 = $$3.indexOf(OBFUSCATE_TOKEN)) != -1) {
            String $$5 = $$3.substring(0, $$4);
            String $$6 = $$3.substring($$4 + OBFUSCATE_TOKEN.length());
            $$3 = $$5 + ChatFormatting.WHITE + ChatFormatting.OBFUSCATED + "XXXXXXXX".substring(0, $$2.nextInt(4) + 3) + $$6;
         }

         this.addPoemLines($$3);
         this.addEmptyLine();
      }

      for (int $$7 = 0; $$7 < 8; $$7++) {
         this.addEmptyLine();
      }
   }

   private void addCreditsFile(Reader p_232820_) {
      for (JsonElement $$2 : GsonHelper.parseArray(p_232820_)) {
         JsonObject $$3 = $$2.getAsJsonObject();
         String $$4 = $$3.get("section").getAsString();
         this.addCreditsLine(SECTION_HEADING, true);
         this.addCreditsLine(Component.literal($$4).withStyle(ChatFormatting.YELLOW), true);
         this.addCreditsLine(SECTION_HEADING, true);
         this.addEmptyLine();
         this.addEmptyLine();

         for (JsonElement $$6 : $$3.getAsJsonArray("disciplines")) {
            JsonObject $$7 = $$6.getAsJsonObject();
            String $$8 = $$7.get("discipline").getAsString();
            if (StringUtils.isNotEmpty($$8)) {
               this.addCreditsLine(Component.literal($$8).withStyle(ChatFormatting.YELLOW), true);
               this.addEmptyLine();
               this.addEmptyLine();
            }

            for (JsonElement $$10 : $$7.getAsJsonArray("titles")) {
               JsonObject $$11 = $$10.getAsJsonObject();
               String $$12 = $$11.get("title").getAsString();
               JsonArray $$13 = $$11.getAsJsonArray("names");
               this.addCreditsLine(Component.literal($$12).withStyle(ChatFormatting.GRAY), false);

               for (JsonElement $$14 : $$13) {
                  String $$15 = $$14.getAsString();
                  this.addCreditsLine(Component.literal("           ").append($$15).withStyle(ChatFormatting.WHITE), false);
               }

               this.addEmptyLine();
               this.addEmptyLine();
            }
         }
      }
   }

   private void addEmptyLine() {
      this.lines.add(FormattedCharSequence.EMPTY);
   }

   private void addPoemLines(String p_181398_) {
      this.lines.addAll(this.minecraft.font.split(Component.literal(p_181398_), 256));
   }

   private void addCreditsLine(Component p_169473_, boolean p_169474_) {
      if (p_169474_) {
         this.centeredLines.add(this.lines.size());
      }

      this.lines.add(p_169473_.getVisualOrderText());
   }

   @Override
   public void render(GuiGraphics p_281907_, int p_282364_, int p_282696_, float p_281316_) {
      super.render(p_281907_, p_282364_, p_282696_, p_281316_);
      this.renderVignette(p_281907_);
      this.scroll = Math.max(0.0F, this.scroll + p_281316_ * this.scrollSpeed);
      int $$4 = this.width / 2 - 128;
      int $$5 = this.height + 50;
      float $$6 = -this.scroll;
      p_281907_.pose().pushPose();
      p_281907_.pose().translate(0.0F, $$6, 0.0F);
      this.logoRenderer.renderLogo(p_281907_, this.width, 1.0F, $$5);
      int $$7 = $$5 + 100;

      for (int $$8 = 0; $$8 < this.lines.size(); $$8++) {
         if ($$8 == this.lines.size() - 1) {
            float $$9 = $$7 + $$6 - (this.height / 2 - 6);
            if ($$9 < 0.0F) {
               p_281907_.pose().translate(0.0F, -$$9, 0.0F);
            }
         }

         if ($$7 + $$6 + 12.0F + 8.0F > 0.0F && $$7 + $$6 < this.height) {
            FormattedCharSequence $$10 = this.lines.get($$8);
            if (this.centeredLines.contains($$8)) {
               p_281907_.drawCenteredString(this.font, $$10, $$4 + 128, $$7, -1);
            } else {
               p_281907_.drawString(this.font, $$10, $$4, $$7, -1);
            }
         }

         $$7 += 12;
      }

      p_281907_.pose().popPose();
   }

   private void renderVignette(GuiGraphics p_331973_) {
      RenderSystem.enableBlend();
      RenderSystem.blendFunc(GlStateManager.SourceFactor.ZERO, GlStateManager.DestFactor.ONE_MINUS_SRC_COLOR);
      p_331973_.blit(VIGNETTE_LOCATION, 0, 0, 0, 0.0F, 0.0F, this.width, this.height, this.width, this.height);
      RenderSystem.disableBlend();
      RenderSystem.defaultBlendFunc();
   }

   @Override
   public void renderBackground(GuiGraphics p_282239_, int p_294762_, int p_295473_, float p_296441_) {
      if (this.poem) {
         p_282239_.fillRenderType(RenderType.endPortal(), 0, 0, this.width, this.height, 0);
      } else {
         super.renderBackground(p_282239_, p_294762_, p_295473_, p_296441_);
      }
   }

   @Override
   protected void renderMenuBackground(GuiGraphics p_333914_, int p_333869_, int p_334014_, int p_333930_, int p_333890_) {
      float $$5 = this.scroll * 0.5F;
      Screen.renderMenuBackgroundTexture(p_333914_, Screen.MENU_BACKGROUND, 0, 0, 0.0F, $$5, p_333930_, p_333890_);
   }

   @Override
   public boolean isPauseScreen() {
      return !this.poem;
   }

   @Override
   public void removed() {
      this.minecraft.getMusicManager().stopPlaying(Musics.CREDITS);
   }

   @Override
   public Music getBackgroundMusic() {
      return Musics.CREDITS;
   }

   @FunctionalInterface
   @OnlyIn(Dist.CLIENT)
   interface CreditsReader {
      void read(Reader var1) throws IOException;
   }
}
