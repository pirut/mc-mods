package net.minecraft.client.gui.components.toasts;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.color.ColorLerper;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.DyeColor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class NowPlayingToast implements Toast {
   private static final Identifier NOW_PLAYING_BACKGROUND_SPRITE = Identifier.withDefaultNamespace("toast/now_playing");
   private static final Identifier MUSIC_NOTES_SPRITE = Identifier.parse("icon/music_notes");
   private static final int PADDING = 7;
   private static final int MUSIC_NOTES_SIZE = 16;
   private static final int HEIGHT = 30;
   private static final int MUSIC_NOTES_SPACE = 30;
   private static final int VISIBILITY_DURATION = 5000;
   private static final int TEXT_COLOR = DyeColor.LIGHT_GRAY.getTextColor();
   private static final long MUSIC_COLOR_CHANGE_FREQUENCY_MS = 25L;
   private static int musicNoteColorTick;
   private static long lastMusicNoteColorChange;
   private static int musicNoteColor = -1;
   private boolean updateToast;
   private double notificationDisplayTimeMultiplier;
   private final Minecraft minecraft;
   private Toast.Visibility wantedVisibility = Toast.Visibility.HIDE;

   public NowPlayingToast() {
      this.minecraft = Minecraft.getInstance();
   }

   public static void renderToast(GuiGraphics p_425762_, Font p_426316_) {
      String s = getCurrentSongName();
      if (s != null) {
         p_425762_.blitSprite(RenderPipelines.GUI_TEXTURED, NOW_PLAYING_BACKGROUND_SPRITE, 0, 0, getWidth(s, p_426316_), 30);
         int i = 7;
         p_425762_.blitSprite(RenderPipelines.GUI_TEXTURED, MUSIC_NOTES_SPRITE, 7, 7, 16, 16, musicNoteColor);
         p_425762_.drawString(p_426316_, getNowPlayingString(s), 30, 11, TEXT_COLOR);
      }
   }

   @Nullable
   private static String getCurrentSongName() {
      return Minecraft.getInstance().getMusicManager().getCurrentMusicTranslationKey();
   }

   public static void tickMusicNotes() {
      if (getCurrentSongName() != null) {
         long i = System.currentTimeMillis();
         if (i > lastMusicNoteColorChange + 25L) {
            musicNoteColorTick++;
            lastMusicNoteColorChange = i;
            musicNoteColor = ColorLerper.getLerpedColor(ColorLerper.Type.MUSIC_NOTE, musicNoteColorTick);
         }
      }
   }

   private static Component getNowPlayingString(@Nullable String p_426151_) {
      return p_426151_ == null ? Component.empty() : Component.translatable(p_426151_.replace("/", "."));
   }

   public void showToast(Options p_427292_) {
      this.updateToast = true;
      this.notificationDisplayTimeMultiplier = p_427292_.notificationDisplayTime().get();
      this.setWantedVisibility(Toast.Visibility.SHOW);
   }

   @Override
   public void update(ToastManager p_426277_, long p_425837_) {
      if (this.updateToast) {
         this.wantedVisibility = p_425837_ < 5000.0 * this.notificationDisplayTimeMultiplier ? Toast.Visibility.SHOW : Toast.Visibility.HIDE;
         tickMusicNotes();
      }
   }

   @Override
   public void render(GuiGraphics p_425876_, Font p_425951_, long p_425666_) {
      renderToast(p_425876_, p_425951_);
   }

   @Override
   public void onFinishedRendering() {
      this.updateToast = false;
   }

   @Override
   public int width() {
      return getWidth(getCurrentSongName(), this.minecraft.font);
   }

   private static int getWidth(@Nullable String p_425810_, Font p_425668_) {
      return 30 + p_425668_.width(getNowPlayingString(p_425810_)) + 7;
   }

   @Override
   public int height() {
      return 30;
   }

   @Override
   public float xPos(int p_425795_, float p_426267_) {
      return this.width() * p_426267_ - this.width();
   }

   @Override
   public float yPos(int p_425944_) {
      return 0.0F;
   }

   @Override
   public Toast.Visibility getWantedVisibility() {
      return this.wantedVisibility;
   }

   public void setWantedVisibility(Toast.Visibility p_425787_) {
      this.wantedVisibility = p_425787_;
   }
}
