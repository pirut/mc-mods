package net.minecraft.client.renderer.state;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.CustomCloudsRenderer;
import net.neoforged.neoforge.client.CustomSkyboxRenderer;
import net.neoforged.neoforge.client.CustomWeatherEffectRenderer;
import net.neoforged.neoforge.client.renderstate.BaseRenderState;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class LevelRenderState extends BaseRenderState {
   public CameraRenderState cameraRenderState = new CameraRenderState();
   public final List<EntityRenderState> entityRenderStates = new ArrayList<>();
   public final List<BlockEntityRenderState> blockEntityRenderStates = new ArrayList<>();
   public boolean haveGlowingEntities;
   @Nullable
   public BlockOutlineRenderState blockOutlineRenderState;
   public final List<BlockBreakingRenderState> blockBreakingRenderStates = new ArrayList<>();
   public final WeatherRenderState weatherRenderState = new WeatherRenderState();
   public final WorldBorderRenderState worldBorderRenderState = new WorldBorderRenderState();
   public final SkyRenderState skyRenderState = new SkyRenderState();
   public long gameTime;
   @Nullable
   public CustomSkyboxRenderer customSkyboxRenderer;
   @Nullable
   public CustomCloudsRenderer customCloudsRenderer;
   @Nullable
   public CustomWeatherEffectRenderer customWeatherEffectRenderer;

   public void reset() {
      this.entityRenderStates.clear();
      this.blockEntityRenderStates.clear();
      this.blockBreakingRenderStates.clear();
      this.haveGlowingEntities = false;
      this.blockOutlineRenderState = null;
      this.weatherRenderState.reset();
      this.worldBorderRenderState.reset();
      this.skyRenderState.reset();
      this.gameTime = 0L;
      this.customSkyboxRenderer = null;
      this.customCloudsRenderer = null;
      this.customWeatherEffectRenderer = null;
      this.resetRenderData();
   }
}
