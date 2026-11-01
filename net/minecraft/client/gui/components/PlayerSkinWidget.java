package net.minecraft.client.gui.components;

import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.entity.player.PlayerSkin;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class PlayerSkinWidget extends AbstractWidget {
   private static final float MODEL_HEIGHT = 2.125F;
   private static final float FIT_SCALE = 0.97F;
   private static final float ROTATION_SENSITIVITY = 2.5F;
   private static final float DEFAULT_ROTATION_X = -5.0F;
   private static final float DEFAULT_ROTATION_Y = 30.0F;
   private static final float ROTATION_X_LIMIT = 50.0F;
   private final PlayerModel wideModel;
   private final PlayerModel slimModel;
   private final Supplier<PlayerSkin> skin;
   private float rotationX = -5.0F;
   private float rotationY = 30.0F;

   public PlayerSkinWidget(int width, int height, EntityModelSet model, Supplier<PlayerSkin> skin) {
      super(0, 0, width, height, CommonComponents.EMPTY);
      this.wideModel = new PlayerModel(model.bakeLayer(ModelLayers.PLAYER), false);
      this.slimModel = new PlayerModel(model.bakeLayer(ModelLayers.PLAYER_SLIM), true);
      this.skin = skin;
   }

   @Override
   protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
      float f = 0.97F * this.getHeight() / 2.125F;
      float g = -1.0625F;
      PlayerSkin playerSkin = this.skin.get();
      PlayerModel playerModel = playerSkin.model() == PlayerModelType.SLIM ? this.slimModel : this.wideModel;
      guiGraphics.submitSkinRenderState(
         playerModel, playerSkin.body().texturePath(), f, this.rotationX, this.rotationY, -1.0625F, this.getX(), this.getY(), this.getRight(), this.getBottom()
      );
   }

   @Override
   protected void onDrag(MouseButtonEvent event, double mouseX, double mouseY) {
      this.rotationX = Mth.clamp(this.rotationX - (float)mouseY * 2.5F, -50.0F, 50.0F);
      this.rotationY += (float)mouseX * 2.5F;
   }

   @Override
   public void playDownSound(SoundManager handler) {
   }

   @Override
   protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
   }

   @Nullable
   @Override
   public ComponentPath nextFocusPath(FocusNavigationEvent event) {
      return null;
   }
}
