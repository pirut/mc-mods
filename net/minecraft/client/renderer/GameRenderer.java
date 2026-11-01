package net.minecraft.client.renderer;

import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.resource.CrossFrameResourcePool;
import com.mojang.blaze3d.shaders.ShaderSource;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.jtracy.TracyClient;
import com.mojang.logging.LogUtils;
import com.mojang.math.Axis;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.SharedConstants;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.Screenshot;
import net.minecraft.client.TextureFilteringMethod;
import net.minecraft.client.entity.ClientAvatarState;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.debug.DebugScreenEntries;
import net.minecraft.client.gui.font.ActiveArea;
import net.minecraft.client.gui.font.EmptyArea;
import net.minecraft.client.gui.font.TextRenderable;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.render.pip.GuiBannerResultRenderer;
import net.minecraft.client.gui.render.pip.GuiBookModelRenderer;
import net.minecraft.client.gui.render.pip.GuiEntityRenderer;
import net.minecraft.client.gui.render.pip.GuiProfilerChartRenderer;
import net.minecraft.client.gui.render.pip.GuiSignRenderer;
import net.minecraft.client.gui.render.pip.GuiSkinRenderer;
import net.minecraft.client.gui.render.state.ColoredRectangleRenderState;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.gui.screens.debug.DebugOptionsScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.fog.FogRenderer;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.state.LevelRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.AtlasManager;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceProvider;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.util.profiling.Zone;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.spider.Spider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.waypoints.TrackedWaypoint;
import org.apache.commons.io.IOUtils;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@Environment(EnvType.CLIENT)
public class GameRenderer implements TrackedWaypoint.Projector, AutoCloseable {
   private static final Identifier BLUR_POST_CHAIN_ID = Identifier.withDefaultNamespace("blur");
   public static final int MAX_BLUR_RADIUS = 10;
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final float PROJECTION_Z_NEAR = 0.05F;
   public static final float PROJECTION_3D_HUD_Z_FAR = 100.0F;
   private static final float PORTAL_SPINNING_SPEED = 20.0F;
   private static final float NAUSEA_SPINNING_SPEED = 7.0F;
   private final Minecraft minecraft;
   private final RandomSource random = RandomSource.create();
   private float renderDistance;
   public final ItemInHandRenderer itemInHandRenderer;
   private final ScreenEffectRenderer screenEffectRenderer;
   private final RenderBuffers renderBuffers;
   private float spinningEffectTime;
   private float spinningEffectSpeed;
   private float fovModifier;
   private float oldFovModifier;
   private float darkenWorldAmount;
   private float darkenWorldAmountO;
   private boolean renderBlockOutline = true;
   private long lastScreenshotAttempt;
   private boolean hasWorldScreenshot;
   private long lastActiveTime = Util.getMillis();
   private final LightTexture lightTexture;
   private final OverlayTexture overlayTexture = new OverlayTexture();
   @Nullable
   private PanoramicScreenshotParameters panoramicScreenshotParameters;
   protected final CubeMap cubeMap = new CubeMap(Identifier.withDefaultNamespace("textures/gui/title/background/panorama"));
   protected final PanoramaRenderer panorama = new PanoramaRenderer(this.cubeMap);
   private final CrossFrameResourcePool resourcePool = new CrossFrameResourcePool(3);
   private final FogRenderer fogRenderer = new FogRenderer();
   private final GuiRenderer guiRenderer;
   final GuiRenderState guiRenderState;
   private final LevelRenderState levelRenderState = new LevelRenderState();
   private final SubmitNodeStorage submitNodeStorage;
   private final FeatureRenderDispatcher featureRenderDispatcher;
   @Nullable
   private Identifier postEffectId;
   private boolean effectActive;
   private final Camera mainCamera = new Camera();
   private final Lighting lighting = new Lighting();
   private final GlobalSettingsUniform globalSettingsUniform = new GlobalSettingsUniform();
   private final PerspectiveProjectionMatrixBuffer levelProjectionMatrixBuffer = new PerspectiveProjectionMatrixBuffer("level");
   private final CachedPerspectiveProjectionMatrixBuffer hud3dProjectionMatrixBuffer = new CachedPerspectiveProjectionMatrixBuffer("3d hud", 0.05F, 100.0F);

   public GameRenderer(Minecraft minecraft, ItemInHandRenderer itemInHandRenderer, RenderBuffers renderBuffers, BlockRenderDispatcher blockRenderDispatcher) {
      this.minecraft = minecraft;
      this.itemInHandRenderer = itemInHandRenderer;
      this.lightTexture = new LightTexture(this, minecraft);
      this.renderBuffers = renderBuffers;
      this.guiRenderState = new GuiRenderState();
      MultiBufferSource.BufferSource bufferSource = renderBuffers.bufferSource();
      AtlasManager atlasManager = minecraft.getAtlasManager();
      this.submitNodeStorage = new SubmitNodeStorage();
      this.featureRenderDispatcher = new FeatureRenderDispatcher(
         this.submitNodeStorage,
         blockRenderDispatcher,
         bufferSource,
         atlasManager,
         renderBuffers.outlineBufferSource(),
         renderBuffers.crumblingBufferSource(),
         minecraft.font
      );
      this.guiRenderer = new GuiRenderer(
         this.guiRenderState,
         bufferSource,
         this.submitNodeStorage,
         this.featureRenderDispatcher,
         List.of(
            new GuiEntityRenderer(bufferSource, minecraft.getEntityRenderDispatcher()),
            new GuiSkinRenderer(bufferSource),
            new GuiBookModelRenderer(bufferSource),
            new GuiBannerResultRenderer(bufferSource, atlasManager),
            new GuiSignRenderer(bufferSource, atlasManager),
            new GuiProfilerChartRenderer(bufferSource)
         )
      );
      this.screenEffectRenderer = new ScreenEffectRenderer(minecraft, atlasManager, bufferSource);
   }

   @Override
   public void close() {
      this.globalSettingsUniform.close();
      this.lightTexture.close();
      this.overlayTexture.close();
      this.resourcePool.close();
      this.guiRenderer.close();
      this.levelProjectionMatrixBuffer.close();
      this.hud3dProjectionMatrixBuffer.close();
      this.lighting.close();
      this.cubeMap.close();
      this.fogRenderer.close();
      this.featureRenderDispatcher.close();
   }

   public SubmitNodeStorage getSubmitNodeStorage() {
      return this.submitNodeStorage;
   }

   public FeatureRenderDispatcher getFeatureRenderDispatcher() {
      return this.featureRenderDispatcher;
   }

   public LevelRenderState getLevelRenderState() {
      return this.levelRenderState;
   }

   public void setRenderBlockOutline(boolean renderBlockOutline) {
      this.renderBlockOutline = renderBlockOutline;
   }

   public void setPanoramicScreenshotParameters(@Nullable PanoramicScreenshotParameters panoramicScreenshotParameters) {
      this.panoramicScreenshotParameters = panoramicScreenshotParameters;
   }

   @Nullable
   public PanoramicScreenshotParameters getPanoramicScreenshotParameters() {
      return this.panoramicScreenshotParameters;
   }

   public boolean isPanoramicMode() {
      return this.panoramicScreenshotParameters != null;
   }

   public void clearPostEffect() {
      this.postEffectId = null;
      this.effectActive = false;
   }

   public void togglePostEffect() {
      this.effectActive = !this.effectActive;
   }

   public void checkEntityPostEffect(@Nullable Entity entity) {
      switch (entity) {
         case Creeper creeper:
            this.setPostEffect(Identifier.withDefaultNamespace("creeper"));
            break;
         case Spider spider:
            this.setPostEffect(Identifier.withDefaultNamespace("spider"));
            break;
         case EnderMan enderMan:
            this.setPostEffect(Identifier.withDefaultNamespace("invert"));
            break;
         case null:
         default:
            this.clearPostEffect();
      }
   }

   private void setPostEffect(Identifier postEffectId) {
      this.postEffectId = postEffectId;
      this.effectActive = true;
   }

   public void processBlurEffect() {
      PostChain postChain = this.minecraft.getShaderManager().getPostChain(BLUR_POST_CHAIN_ID, LevelTargetBundle.MAIN_TARGETS);
      if (postChain != null) {
         postChain.process(this.minecraft.getMainRenderTarget(), this.resourcePool);
      }
   }

   public void preloadUiShader(ResourceProvider resourceProvider) {
      GpuDevice gpuDevice = RenderSystem.getDevice();
      ShaderSource shaderSource = (identifier, shaderType) -> {
         Identifier identifier2 = shaderType.idConverter().idToFile(identifier);

         try {
            String var5;
            try (Reader reader = resourceProvider.getResourceOrThrow(identifier2).openAsReader()) {
               var5 = IOUtils.toString(reader);
            }

            return var5;
         } catch (IOException var9) {
            LOGGER.error("Coudln't preload {} shader {}: {}", new Object[]{shaderType, identifier, var9});
            return null;
         }
      };
      gpuDevice.precompilePipeline(RenderPipelines.GUI, shaderSource);
      gpuDevice.precompilePipeline(RenderPipelines.GUI_TEXTURED, shaderSource);
      if (TracyClient.isAvailable()) {
         gpuDevice.precompilePipeline(RenderPipelines.TRACY_BLIT, shaderSource);
      }
   }

   public void tick() {
      this.tickFov();
      this.lightTexture.tick();
      LocalPlayer localPlayer = this.minecraft.player;
      if (this.minecraft.getCameraEntity() == null) {
         this.minecraft.setCameraEntity(localPlayer);
      }

      this.mainCamera.tick();
      this.itemInHandRenderer.tick();
      float f = localPlayer.portalEffectIntensity;
      float g = localPlayer.getEffectBlendFactor(MobEffects.NAUSEA, 1.0F);
      if (!(f > 0.0F) && !(g > 0.0F)) {
         this.spinningEffectSpeed = 0.0F;
      } else {
         this.spinningEffectSpeed = (f * 20.0F + g * 7.0F) / (f + g);
         this.spinningEffectTime = this.spinningEffectTime + this.spinningEffectSpeed;
      }

      if (this.minecraft.level.tickRateManager().runsNormally()) {
         this.darkenWorldAmountO = this.darkenWorldAmount;
         if (this.minecraft.gui.getBossOverlay().shouldDarkenScreen()) {
            this.darkenWorldAmount += 0.05F;
            if (this.darkenWorldAmount > 1.0F) {
               this.darkenWorldAmount = 1.0F;
            }
         } else if (this.darkenWorldAmount > 0.0F) {
            this.darkenWorldAmount -= 0.0125F;
         }

         this.screenEffectRenderer.tick();
         ProfilerFiller profilerFiller = Profiler.get();
         profilerFiller.push("levelRenderer");
         this.minecraft.levelRenderer.tick(this.mainCamera);
         profilerFiller.pop();
      }
   }

   @Nullable
   public Identifier currentPostEffect() {
      return this.postEffectId;
   }

   public void resize(int width, int height) {
      this.resourcePool.clear();
      this.minecraft.levelRenderer.resize(width, height);
   }

   public void pick(float partialTick) {
      Entity entity = this.minecraft.getCameraEntity();
      if (entity != null) {
         if (this.minecraft.level != null && this.minecraft.player != null) {
            Profiler.get().push("pick");
            this.minecraft.hitResult = this.minecraft.player.raycastHitResult(partialTick, entity);
            this.minecraft.crosshairPickEntity = this.minecraft.hitResult instanceof EntityHitResult entityHitResult ? entityHitResult.getEntity() : null;
            Profiler.get().pop();
         }
      }
   }

   private void tickFov() {
      float g;
      if (this.minecraft.getCameraEntity() instanceof AbstractClientPlayer abstractClientPlayer) {
         Options options = this.minecraft.options;
         boolean bl = options.getCameraType().isFirstPerson();
         float f = options.fovEffectScale().get().floatValue();
         g = abstractClientPlayer.getFieldOfViewModifier(bl, f);
      } else {
         g = 1.0F;
      }

      this.oldFovModifier = this.fovModifier;
      this.fovModifier = this.fovModifier + (g - this.fovModifier) * 0.5F;
      this.fovModifier = Mth.clamp(this.fovModifier, 0.1F, 1.5F);
   }

   private float getFov(Camera camera, float partialTick, boolean useFovSetting) {
      if (this.isPanoramicMode()) {
         return 90.0F;
      } else {
         float f = 70.0F;
         if (useFovSetting) {
            f = this.minecraft.options.fov().get().intValue();
            f *= Mth.lerp(partialTick, this.oldFovModifier, this.fovModifier);
         }

         if (camera.entity() instanceof LivingEntity livingEntity && livingEntity.isDeadOrDying()) {
            float g = Math.min(livingEntity.deathTime + partialTick, 20.0F);
            f /= (1.0F - 500.0F / (g + 500.0F)) * 2.0F + 1.0F;
         }

         FogType fogType = camera.getFluidInCamera();
         if (fogType == FogType.LAVA || fogType == FogType.WATER) {
            float g = this.minecraft.options.fovEffectScale().get().floatValue();
            f *= Mth.lerp(g, 1.0F, 0.85714287F);
         }

         return f;
      }
   }

   private void bobHurt(PoseStack poseStack, float partialTicks) {
      if (this.minecraft.getCameraEntity() instanceof LivingEntity livingEntity) {
         float f = livingEntity.hurtTime - partialTicks;
         if (livingEntity.isDeadOrDying()) {
            float g = Math.min(livingEntity.deathTime + partialTicks, 20.0F);
            poseStack.mulPose(Axis.ZP.rotationDegrees(40.0F - 8000.0F / (g + 200.0F)));
         }

         if (f < 0.0F) {
            return;
         }

         f /= livingEntity.hurtDuration;
         f = Mth.sin(f * f * f * f * (float) Math.PI);
         float g = livingEntity.getHurtDir();
         poseStack.mulPose(Axis.YP.rotationDegrees(-g));
         float h = (float)(-f * 14.0 * this.minecraft.options.damageTiltStrength().get());
         poseStack.mulPose(Axis.ZP.rotationDegrees(h));
         poseStack.mulPose(Axis.YP.rotationDegrees(g));
      }
   }

   private void bobView(PoseStack poseStack, float partialTicks) {
      if (this.minecraft.getCameraEntity() instanceof AbstractClientPlayer abstractClientPlayer) {
         ClientAvatarState clientAvatarState = abstractClientPlayer.avatarState();
         float f = clientAvatarState.getBackwardsInterpolatedWalkDistance(partialTicks);
         float g = clientAvatarState.getInterpolatedBob(partialTicks);
         poseStack.translate(Mth.sin(f * (float) Math.PI) * g * 0.5F, -Math.abs(Mth.cos(f * (float) Math.PI) * g), 0.0F);
         poseStack.mulPose(Axis.ZP.rotationDegrees(Mth.sin(f * (float) Math.PI) * g * 3.0F));
         poseStack.mulPose(Axis.XP.rotationDegrees(Math.abs(Mth.cos(f * (float) Math.PI - 0.2F) * g) * 5.0F));
      }
   }

   private void renderItemInHand(float partialTick, boolean sleeping, Matrix4f projectionMatrix) {
      if (!this.isPanoramicMode()) {
         this.featureRenderDispatcher.renderAllFeatures();
         this.renderBuffers.bufferSource().endBatch();
         PoseStack poseStack = new PoseStack();
         poseStack.pushPose();
         poseStack.mulPose(projectionMatrix.invert(new Matrix4f()));
         Matrix4fStack matrix4fStack = RenderSystem.getModelViewStack();
         matrix4fStack.pushMatrix().mul(projectionMatrix);
         this.bobHurt(poseStack, partialTick);
         if (this.minecraft.options.bobView().get()) {
            this.bobView(poseStack, partialTick);
         }

         if (this.minecraft.options.getCameraType().isFirstPerson()
            && !sleeping
            && !this.minecraft.options.hideGui
            && this.minecraft.gameMode.getPlayerMode() != GameType.SPECTATOR) {
            this.itemInHandRenderer
               .renderHandsWithItems(
                  partialTick,
                  poseStack,
                  this.minecraft.gameRenderer.getSubmitNodeStorage(),
                  this.minecraft.player,
                  this.minecraft.getEntityRenderDispatcher().getPackedLightCoords(this.minecraft.player, partialTick)
               );
         }

         matrix4fStack.popMatrix();
         poseStack.popPose();
      }
   }

   public Matrix4f getProjectionMatrix(float fov) {
      Matrix4f matrix4f = new Matrix4f();
      return matrix4f.perspective(
         fov * (float) (Math.PI / 180.0), (float)this.minecraft.getWindow().getWidth() / this.minecraft.getWindow().getHeight(), 0.05F, this.getDepthFar()
      );
   }

   public float getDepthFar() {
      return Math.max(this.renderDistance * 4.0F, (float)(this.minecraft.options.cloudRange().get() * 16));
   }

   public static float getNightVisionScale(LivingEntity livingEntity, float nanoTime) {
      MobEffectInstance mobEffectInstance = livingEntity.getEffect(MobEffects.NIGHT_VISION);
      return !mobEffectInstance.endsWithin(200) ? 1.0F : 0.7F + Mth.sin((mobEffectInstance.getDuration() - nanoTime) * (float) Math.PI * 0.2F) * 0.3F;
   }

   public void render(DeltaTracker deltaTracker, boolean renderLevel) {
      if (!this.minecraft.isWindowActive()
         && this.minecraft.options.pauseOnLostFocus
         && (!this.minecraft.options.touchscreen().get() || !this.minecraft.mouseHandler.isRightPressed())) {
         if (Util.getMillis() - this.lastActiveTime > 500L) {
            this.minecraft.pauseGame(false);
         }
      } else {
         this.lastActiveTime = Util.getMillis();
      }

      if (!this.minecraft.noRender) {
         ProfilerFiller profilerFiller = Profiler.get();
         profilerFiller.push("camera");
         this.updateCamera(deltaTracker);
         profilerFiller.pop();
         this.globalSettingsUniform
            .update(
               this.minecraft.getWindow().getWidth(),
               this.minecraft.getWindow().getHeight(),
               this.minecraft.options.glintStrength().get(),
               this.minecraft.level == null ? 0L : this.minecraft.level.getGameTime(),
               deltaTracker,
               this.minecraft.options.getMenuBackgroundBlurriness(),
               this.mainCamera,
               this.minecraft.options.textureFiltering().get() == TextureFilteringMethod.RGSS
            );
         boolean bl = this.minecraft.isGameLoadFinished();
         int i = (int)this.minecraft.mouseHandler.getScaledXPos(this.minecraft.getWindow());
         int j = (int)this.minecraft.mouseHandler.getScaledYPos(this.minecraft.getWindow());
         if (bl && renderLevel && this.minecraft.level != null) {
            profilerFiller.push("world");
            this.renderLevel(deltaTracker);
            this.tryTakeScreenshotIfNeeded();
            this.minecraft.levelRenderer.doEntityOutline();
            if (this.postEffectId != null && this.effectActive) {
               PostChain postChain = this.minecraft.getShaderManager().getPostChain(this.postEffectId, LevelTargetBundle.MAIN_TARGETS);
               if (postChain != null) {
                  postChain.process(this.minecraft.getMainRenderTarget(), this.resourcePool);
               }
            }

            profilerFiller.pop();
         }

         this.fogRenderer.endFrame();
         RenderTarget renderTarget = this.minecraft.getMainRenderTarget();
         RenderSystem.getDevice().createCommandEncoder().clearDepthTexture(renderTarget.getDepthTexture(), 1.0);
         this.minecraft.gameRenderer.getLighting().setupFor(Lighting.Entry.ITEMS_3D);
         this.guiRenderState.reset();
         profilerFiller.push("guiExtraction");
         GuiGraphics guiGraphics = new GuiGraphics(this.minecraft, this.guiRenderState, i, j);
         if (bl && renderLevel && this.minecraft.level != null) {
            this.minecraft.gui.render(guiGraphics, deltaTracker);
         }

         if (this.minecraft.getOverlay() != null) {
            try {
               this.minecraft.getOverlay().render(guiGraphics, i, j, deltaTracker.getGameTimeDeltaTicks());
            } catch (Throwable var15) {
               CrashReport crashReport = CrashReport.forThrowable(var15, "Rendering overlay");
               CrashReportCategory crashReportCategory = crashReport.addCategory("Overlay render details");
               crashReportCategory.setDetail("Overlay name", () -> this.minecraft.getOverlay().getClass().getCanonicalName());
               throw new ReportedException(crashReport);
            }
         } else if (bl && this.minecraft.screen != null) {
            try {
               this.minecraft.screen.renderWithTooltipAndSubtitles(guiGraphics, i, j, deltaTracker.getGameTimeDeltaTicks());
            } catch (Throwable var14) {
               CrashReport crashReport = CrashReport.forThrowable(var14, "Rendering screen");
               CrashReportCategory crashReportCategory = crashReport.addCategory("Screen render details");
               crashReportCategory.setDetail("Screen name", () -> this.minecraft.screen.getClass().getCanonicalName());
               this.minecraft.mouseHandler.fillMousePositionDetails(crashReportCategory, this.minecraft.getWindow());
               throw new ReportedException(crashReport);
            }

            if (SharedConstants.DEBUG_CURSOR_POS) {
               this.minecraft.mouseHandler.drawDebugMouseInfo(this.minecraft.font, guiGraphics);
            }

            try {
               if (this.minecraft.screen != null) {
                  this.minecraft.screen.handleDelayedNarration();
               }
            } catch (Throwable var13) {
               CrashReport crashReport = CrashReport.forThrowable(var13, "Narrating screen");
               CrashReportCategory crashReportCategory = crashReport.addCategory("Screen details");
               crashReportCategory.setDetail("Screen name", () -> this.minecraft.screen.getClass().getCanonicalName());
               throw new ReportedException(crashReport);
            }
         }

         if (bl && renderLevel && this.minecraft.level != null) {
            this.minecraft.gui.renderSavingIndicator(guiGraphics, deltaTracker);
         }

         if (bl) {
            try (Zone zone = profilerFiller.zone("toasts")) {
               this.minecraft.getToastManager().render(guiGraphics);
            }
         }

         if (!(this.minecraft.screen instanceof DebugOptionsScreen)) {
            this.minecraft.gui.renderDebugOverlay(guiGraphics);
         }

         this.minecraft.gui.renderDeferredSubtitles();
         if (SharedConstants.DEBUG_ACTIVE_TEXT_AREAS) {
            this.renderActiveTextDebug();
         }

         profilerFiller.popPush("guiRendering");
         this.guiRenderer.render(this.fogRenderer.getBuffer(FogRenderer.FogMode.NONE));
         this.guiRenderer.incrementFrameNumber();
         profilerFiller.pop();
         guiGraphics.applyCursor(this.minecraft.getWindow());
         this.submitNodeStorage.endFrame();
         this.featureRenderDispatcher.endFrame();
         this.resourcePool.endFrame();
      }
   }

   private void renderActiveTextDebug() {
      this.guiRenderState.nextStratum();
      this.guiRenderState
         .forEachText(
            guiTextRenderState -> guiTextRenderState.ensurePrepared()
               .visit(
                  new Font.GlyphVisitor() {
                     private int index;

                     @Override
                     public void acceptGlyph(TextRenderable.Styled glyph) {
                        this.renderDebugMarkers(glyph, false);
                     }

                     @Override
                     public void acceptEmptyArea(EmptyArea emptyArea) {
                        this.renderDebugMarkers(emptyArea, true);
                     }

                     private void renderDebugMarkers(ActiveArea activeArea, boolean bl) {
                        int i = (bl ? 128 : 255) - (this.index++ & 1) * 64;
                        Style style = activeArea.style();
                        int j = style.getClickEvent() != null ? i : 0;
                        int k = style.getHoverEvent() != null ? i : 0;
                        int l = j != 0 && k != 0 ? 0 : i;
                        int m = ARGB.color(128, j, k, l);
                        GameRenderer.this.guiRenderState
                           .submitGuiElement(
                              new ColoredRectangleRenderState(
                                 RenderPipelines.GUI,
                                 TextureSetup.noTexture(),
                                 guiTextRenderState.pose,
                                 (int)activeArea.activeLeft(),
                                 (int)activeArea.activeTop(),
                                 (int)activeArea.activeRight(),
                                 (int)activeArea.activeBottom(),
                                 m,
                                 m,
                                 guiTextRenderState.scissor
                              )
                           );
                     }
                  }
               )
         );
   }

   private void tryTakeScreenshotIfNeeded() {
      if (!this.hasWorldScreenshot && this.minecraft.isLocalServer()) {
         long l = Util.getMillis();
         if (l - this.lastScreenshotAttempt >= 1000L) {
            this.lastScreenshotAttempt = l;
            IntegratedServer integratedServer = this.minecraft.getSingleplayerServer();
            if (integratedServer != null && !integratedServer.isStopped()) {
               integratedServer.getWorldScreenshotFile().ifPresent(path -> {
                  if (Files.isRegularFile(path)) {
                     this.hasWorldScreenshot = true;
                  } else {
                     this.takeAutoScreenshot(path);
                  }
               });
            }
         }
      }
   }

   private void takeAutoScreenshot(Path path) {
      if (this.minecraft.levelRenderer.countRenderedSections() > 10 && this.minecraft.levelRenderer.hasRenderedAllSections()) {
         Screenshot.takeScreenshot(this.minecraft.getMainRenderTarget(), nativeImage -> Util.ioPool().execute(() -> {
            int i = nativeImage.getWidth();
            int j = nativeImage.getHeight();
            int k = 0;
            int l = 0;
            if (i > j) {
               k = (i - j) / 2;
               i = j;
            } else {
               l = (j - i) / 2;
               j = i;
            }

            try (NativeImage nativeImage2 = new NativeImage(64, 64, false)) {
               nativeImage.resizeSubRectTo(k, l, i, j, nativeImage2);
               nativeImage2.writeToFile(path);
            } catch (IOException var16) {
               LOGGER.warn("Couldn't save auto screenshot", var16);
            } finally {
               nativeImage.close();
            }
         }));
      }
   }

   private boolean shouldRenderBlockOutline() {
      if (!this.renderBlockOutline) {
         return false;
      } else {
         Entity entity = this.minecraft.getCameraEntity();
         boolean bl = entity instanceof Player && !this.minecraft.options.hideGui;
         if (bl && !((Player)entity).getAbilities().mayBuild) {
            ItemStack itemStack = ((LivingEntity)entity).getMainHandItem();
            HitResult hitResult = this.minecraft.hitResult;
            if (hitResult != null && hitResult.getType() == HitResult.Type.BLOCK) {
               BlockPos blockPos = ((BlockHitResult)hitResult).getBlockPos();
               BlockState blockState = this.minecraft.level.getBlockState(blockPos);
               if (this.minecraft.gameMode.getPlayerMode() == GameType.SPECTATOR) {
                  bl = blockState.getMenuProvider(this.minecraft.level, blockPos) != null;
               } else {
                  BlockInWorld blockInWorld = new BlockInWorld(this.minecraft.level, blockPos, false);
                  Registry<Block> registry = this.minecraft.level.registryAccess().lookupOrThrow(Registries.BLOCK);
                  bl = !itemStack.isEmpty() && (itemStack.canBreakBlockInAdventureMode(blockInWorld) || itemStack.canPlaceOnBlockInAdventureMode(blockInWorld));
               }
            }
         }

         return bl;
      }
   }

   public void updateCamera(DeltaTracker deltaTracker) {
      float f = deltaTracker.getGameTimeDeltaPartialTick(true);
      LocalPlayer localPlayer = this.minecraft.player;
      if (localPlayer != null && this.minecraft.level != null) {
         if (this.minecraft.getCameraEntity() == null) {
            this.minecraft.setCameraEntity(localPlayer);
         }

         Entity entity = (Entity)(this.minecraft.getCameraEntity() == null ? localPlayer : this.minecraft.getCameraEntity());
         float g = this.minecraft.level.tickRateManager().isEntityFrozen(entity) ? 1.0F : f;
         this.mainCamera
            .setup(
               this.minecraft.level, entity, !this.minecraft.options.getCameraType().isFirstPerson(), this.minecraft.options.getCameraType().isMirrored(), g
            );
      }
   }

   public void renderLevel(DeltaTracker deltaTracker) {
      float f = deltaTracker.getGameTimeDeltaPartialTick(true);
      LocalPlayer localPlayer = this.minecraft.player;
      this.lightTexture.updateLightTexture(1.0F);
      this.pick(f);
      ProfilerFiller profilerFiller = Profiler.get();
      boolean bl = this.shouldRenderBlockOutline();
      this.extractCamera(f);
      this.renderDistance = this.minecraft.options.getEffectiveRenderDistance() * 16;
      profilerFiller.push("matrices");
      float g = this.getFov(this.mainCamera, f, true);
      Matrix4f matrix4f = this.getProjectionMatrix(g);
      PoseStack poseStack = new PoseStack();
      this.bobHurt(poseStack, this.mainCamera.getPartialTickTime());
      if (this.minecraft.options.bobView().get()) {
         this.bobView(poseStack, this.mainCamera.getPartialTickTime());
      }

      matrix4f.mul(poseStack.last().pose());
      float h = this.minecraft.options.screenEffectScale().get().floatValue();
      float i = Mth.lerp(f, localPlayer.oPortalEffectIntensity, localPlayer.portalEffectIntensity);
      float j = localPlayer.getEffectBlendFactor(MobEffects.NAUSEA, f);
      float k = Math.max(i, j) * (h * h);
      if (k > 0.0F) {
         float l = 5.0F / (k * k + 5.0F) - k * 0.04F;
         l *= l;
         Vector3f vector3f = new Vector3f(0.0F, Mth.SQRT_OF_TWO / 2.0F, Mth.SQRT_OF_TWO / 2.0F);
         float m = (this.spinningEffectTime + f * this.spinningEffectSpeed) * (float) (Math.PI / 180.0);
         matrix4f.rotate(m, vector3f);
         matrix4f.scale(1.0F / l, 1.0F, 1.0F);
         matrix4f.rotate(-m, vector3f);
      }

      RenderSystem.setProjectionMatrix(this.levelProjectionMatrixBuffer.getBuffer(matrix4f), ProjectionType.PERSPECTIVE);
      Quaternionf quaternionf = this.mainCamera.rotation().conjugate(new Quaternionf());
      Matrix4f matrix4f2 = new Matrix4f().rotation(quaternionf);
      profilerFiller.popPush("fog");
      Vector4f vector4f = this.fogRenderer
         .setupFog(this.mainCamera, this.minecraft.options.getEffectiveRenderDistance(), deltaTracker, this.getDarkenWorldAmount(f), this.minecraft.level);
      GpuBufferSlice gpuBufferSlice = this.fogRenderer.getBuffer(FogRenderer.FogMode.WORLD);
      profilerFiller.popPush("level");
      boolean bl2 = this.minecraft.gui.getBossOverlay().shouldCreateWorldFog();
      this.minecraft
         .levelRenderer
         .renderLevel(
            this.resourcePool, deltaTracker, bl, this.mainCamera, matrix4f2, matrix4f, this.getProjectionMatrixForCulling(g), gpuBufferSlice, vector4f, !bl2
         );
      profilerFiller.popPush("hand");
      boolean bl3 = this.minecraft.getCameraEntity() instanceof LivingEntity && ((LivingEntity)this.minecraft.getCameraEntity()).isSleeping();
      RenderSystem.setProjectionMatrix(
         this.hud3dProjectionMatrixBuffer
            .getBuffer(this.minecraft.getWindow().getWidth(), this.minecraft.getWindow().getHeight(), this.getFov(this.mainCamera, f, false)),
         ProjectionType.PERSPECTIVE
      );
      RenderSystem.getDevice().createCommandEncoder().clearDepthTexture(this.minecraft.getMainRenderTarget().getDepthTexture(), 1.0);
      this.renderItemInHand(f, bl3, matrix4f2);
      profilerFiller.popPush("screenEffects");
      MultiBufferSource.BufferSource bufferSource = this.renderBuffers.bufferSource();
      this.screenEffectRenderer.renderScreenEffect(bl3, f, this.submitNodeStorage);
      this.featureRenderDispatcher.renderAllFeatures();
      bufferSource.endBatch();
      profilerFiller.pop();
      RenderSystem.setShaderFog(this.fogRenderer.getBuffer(FogRenderer.FogMode.NONE));
      if (this.minecraft.debugEntries.isCurrentlyEnabled(DebugScreenEntries.THREE_DIMENSIONAL_CROSSHAIR)
         && this.minecraft.options.getCameraType().isFirstPerson()
         && !this.minecraft.options.hideGui) {
         this.minecraft.getDebugOverlay().render3dCrosshair(this.mainCamera);
      }
   }

   private void extractCamera(float partialTick) {
      CameraRenderState cameraRenderState = this.levelRenderState.cameraRenderState;
      cameraRenderState.initialized = this.mainCamera.isInitialized();
      cameraRenderState.pos = this.mainCamera.position();
      cameraRenderState.blockPos = this.mainCamera.blockPosition();
      cameraRenderState.entityPos = this.mainCamera.entity().getPosition(partialTick);
      cameraRenderState.orientation = new Quaternionf(this.mainCamera.rotation());
   }

   private Matrix4f getProjectionMatrixForCulling(float fov) {
      float f = Math.max(fov, (float)this.minecraft.options.fov().get().intValue());
      return this.getProjectionMatrix(f);
   }

   public void resetData() {
      this.screenEffectRenderer.resetItemActivation();
      this.minecraft.getMapTextureManager().resetData();
      this.mainCamera.reset();
      this.hasWorldScreenshot = false;
   }

   public void displayItemActivation(ItemStack stack) {
      this.screenEffectRenderer.displayItemActivation(stack, this.random);
   }

   public Minecraft getMinecraft() {
      return this.minecraft;
   }

   public float getDarkenWorldAmount(float partialTicks) {
      return Mth.lerp(partialTicks, this.darkenWorldAmountO, this.darkenWorldAmount);
   }

   public float getRenderDistance() {
      return this.renderDistance;
   }

   public Camera getMainCamera() {
      return this.mainCamera;
   }

   public LightTexture lightTexture() {
      return this.lightTexture;
   }

   public OverlayTexture overlayTexture() {
      return this.overlayTexture;
   }

   @Override
   public Vec3 projectPointToScreen(Vec3 point) {
      Matrix4f matrix4f = this.getProjectionMatrix(this.getFov(this.mainCamera, 0.0F, true));
      Quaternionf quaternionf = this.mainCamera.rotation().conjugate(new Quaternionf());
      Matrix4f matrix4f2 = new Matrix4f().rotation(quaternionf);
      Matrix4f matrix4f3 = matrix4f.mul(matrix4f2);
      Vec3 vec3 = this.mainCamera.position();
      Vec3 vec32 = point.subtract(vec3);
      Vector3f vector3f = matrix4f3.transformProject(vec32.toVector3f());
      return new Vec3(vector3f);
   }

   @Override
   public double projectHorizonToScreen() {
      float f = this.mainCamera.xRot();
      if (f <= -90.0F) {
         return Double.NEGATIVE_INFINITY;
      } else if (f >= 90.0F) {
         return Double.POSITIVE_INFINITY;
      } else {
         float g = this.getFov(this.mainCamera, 0.0F, true);
         return Math.tan(f * (float) (Math.PI / 180.0)) / Math.tan(g / 2.0F * (float) (Math.PI / 180.0));
      }
   }

   public GlobalSettingsUniform getGlobalSettingsUniform() {
      return this.globalSettingsUniform;
   }

   public Lighting getLighting() {
      return this.lighting;
   }

   public void setLevel(@Nullable ClientLevel level) {
      if (level != null) {
         this.lighting.updateLevel(level.dimensionType().cardinalLightType());
      }
   }

   public PanoramaRenderer getPanorama() {
      return this.panorama;
   }
}
