package net.minecraft.client.gui.render;

import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.Supplier;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.font.TextRenderable;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.pip.OversizedItemRenderer;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.gui.render.state.BlitRenderState;
import net.minecraft.client.gui.render.state.GlyphRenderState;
import net.minecraft.client.gui.render.state.GuiElementRenderState;
import net.minecraft.client.gui.render.state.GuiItemRenderState;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.gui.render.state.pip.OversizedItemRenderState;
import net.minecraft.client.gui.render.state.pip.PictureInPictureRenderState;
import net.minecraft.client.renderer.CachedOrthoProjectionMatrixBuffer;
import net.minecraft.client.renderer.MappableRingBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.item.TrackingItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.gui.PictureInPictureRendererPool;
import net.neoforged.neoforge.client.gui.PictureInPictureRendererRegistration;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.joml.Matrix3x2fc;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.jspecify.annotations.Nullable;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class GuiRenderer implements AutoCloseable {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final float MAX_GUI_Z = 10000.0F;
   public static final float MIN_GUI_Z = 0.0F;
   private static final float GUI_Z_NEAR = 1000.0F;
   public static final int GUI_3D_Z_FAR = 1000;
   public static final int GUI_3D_Z_NEAR = -1000;
   public static final int DEFAULT_ITEM_SIZE = 16;
   private static final int MINIMUM_ITEM_ATLAS_SIZE = 512;
   private static final int MAXIMUM_ITEM_ATLAS_SIZE = RenderSystem.getDevice().getMaxTextureSize();
   public static final int CLEAR_COLOR = 0;
   private static final Comparator<ScreenRectangle> SCISSOR_COMPARATOR = Comparator.nullsFirst(
      Comparator.comparing(ScreenRectangle::top)
         .thenComparing(ScreenRectangle::bottom)
         .thenComparing(ScreenRectangle::left)
         .thenComparing(ScreenRectangle::right)
   );
   private static final Comparator<TextureSetup> TEXTURE_COMPARATOR = Comparator.nullsFirst(Comparator.comparing(TextureSetup::getSortKey));
   private static final Comparator<GuiElementRenderState> ELEMENT_SORT_COMPARATOR = Comparator.comparing(GuiElementRenderState::scissorArea, SCISSOR_COMPARATOR)
      .thenComparing(GuiElementRenderState::pipeline, Comparator.comparing(RenderPipeline::getSortKey))
      .thenComparing(GuiElementRenderState::textureSetup, TEXTURE_COMPARATOR);
   private final Map<Object, GuiRenderer.AtlasPosition> atlasPositions = new Object2ObjectOpenHashMap();
   private final Map<Object, OversizedItemRenderer> oversizedItemRenderers = new Object2ObjectOpenHashMap();
   final GuiRenderState renderState;
   private final List<GuiRenderer.Draw> draws = new ArrayList<>();
   private final List<GuiRenderer.MeshToDraw> meshesToDraw = new ArrayList<>();
   private final ByteBufferBuilder byteBufferBuilder = new ByteBufferBuilder(786432);
   private final Map<VertexFormat, MappableRingBuffer> vertexBuffers = new Object2ObjectOpenHashMap();
   private int firstDrawIndexAfterBlur = Integer.MAX_VALUE;
   private final CachedOrthoProjectionMatrixBuffer guiProjectionMatrixBuffer = new CachedOrthoProjectionMatrixBuffer("gui", 1000.0F, 11000.0F, true);
   private final CachedOrthoProjectionMatrixBuffer itemsProjectionMatrixBuffer = new CachedOrthoProjectionMatrixBuffer("items", -1000.0F, 1000.0F, true);
   private final MultiBufferSource.BufferSource bufferSource;
   private final SubmitNodeCollector submitNodeCollector;
   private final FeatureRenderDispatcher featureRenderDispatcher;
   private final Map<Class<? extends PictureInPictureRenderState>, PictureInPictureRendererPool<?>> pictureInPictureRendererPools;
   private final Set<PictureInPictureRenderState> pictureInPictureRenderStatesScratch = new ReferenceOpenHashSet();
   @Nullable
   private GpuTexture itemsAtlas;
   @Nullable
   private GpuTextureView itemsAtlasView;
   @Nullable
   private GpuTexture itemsAtlasDepth;
   @Nullable
   private GpuTextureView itemsAtlasDepthView;
   private int itemAtlasX;
   private int itemAtlasY;
   private int cachedGuiScale;
   private int frameNumber;
   @Nullable
   private ScreenRectangle previousScissorArea = null;
   @Nullable
   private RenderPipeline previousPipeline = null;
   @Nullable
   private TextureSetup previousTextureSetup = null;
   @Nullable
   private BufferBuilder bufferBuilder = null;

   public GuiRenderer(
      GuiRenderState p_416418_,
      MultiBufferSource.BufferSource p_415673_,
      SubmitNodeCollector p_440036_,
      FeatureRenderDispatcher p_439972_,
      List<PictureInPictureRendererRegistration<?>> pipRendererFactories
   ) {
      this.renderState = p_416418_;
      this.bufferSource = p_415673_;
      this.submitNodeCollector = p_440036_;
      this.featureRenderDispatcher = p_439972_;
      this.pictureInPictureRendererPools = PictureInPictureRendererPool.createPools(p_415673_, pipRendererFactories);
   }

   public void incrementFrameNumber() {
      this.frameNumber++;
   }

   public void render(GpuBufferSlice p_418236_) {
      this.prepare();
      this.draw(p_418236_);

      for (MappableRingBuffer mappableringbuffer : this.vertexBuffers.values()) {
         mappableringbuffer.rotate();
      }

      this.draws.clear();
      this.meshesToDraw.clear();
      this.renderState.reset();
      this.firstDrawIndexAfterBlur = Integer.MAX_VALUE;
      this.clearUnusedOversizedItemRenderers();
      this.pictureInPictureRendererPools.values().forEach(PictureInPictureRendererPool::clearUnusedRenderers);
      if (SharedConstants.DEBUG_SHUFFLE_UI_RENDERING_ORDER) {
         RenderPipeline.updateSortKeySeed();
         TextureSetup.updateSortKeySeed();
      }
   }

   private void clearUnusedOversizedItemRenderers() {
      Iterator<Entry<Object, OversizedItemRenderer>> iterator = this.oversizedItemRenderers.entrySet().iterator();

      while (iterator.hasNext()) {
         Entry<Object, OversizedItemRenderer> entry = iterator.next();
         OversizedItemRenderer oversizeditemrenderer = entry.getValue();
         if (!oversizeditemrenderer.usedOnThisFrame()) {
            oversizeditemrenderer.close();
            iterator.remove();
         } else {
            oversizeditemrenderer.resetUsedOnThisFrame();
         }
      }
   }

   private void prepare() {
      this.bufferSource.endBatch();
      this.preparePictureInPicture();
      this.prepareItemElements();
      this.prepareText();
      this.renderState.sortElements(ELEMENT_SORT_COMPARATOR);
      this.addElementsToMeshes(GuiRenderState.TraverseRange.BEFORE_BLUR);
      this.firstDrawIndexAfterBlur = this.meshesToDraw.size();
      this.addElementsToMeshes(GuiRenderState.TraverseRange.AFTER_BLUR);
      this.recordDraws();
   }

   private void addElementsToMeshes(GuiRenderState.TraverseRange p_419768_) {
      this.previousScissorArea = null;
      this.previousPipeline = null;
      this.previousTextureSetup = null;
      this.bufferBuilder = null;
      this.renderState.forEachElement(this::addElementToMesh, p_419768_);
      if (this.bufferBuilder != null) {
         this.recordMesh(this.bufferBuilder, this.previousPipeline, this.previousTextureSetup, this.previousScissorArea);
      }
   }

   private void draw(GpuBufferSlice p_419894_) {
      if (!this.draws.isEmpty()) {
         Minecraft minecraft = Minecraft.getInstance();
         Window window = minecraft.getWindow();
         RenderSystem.setProjectionMatrix(
            this.guiProjectionMatrixBuffer.getBuffer((float)window.getWidth() / window.getGuiScale(), (float)window.getHeight() / window.getGuiScale()),
            ProjectionType.ORTHOGRAPHIC
         );
         RenderTarget rendertarget = minecraft.getMainRenderTarget();
         int i = 0;

         for (GuiRenderer.Draw guirenderer$draw : this.draws) {
            if (guirenderer$draw.indexCount > i) {
               i = guirenderer$draw.indexCount;
            }
         }

         RenderSystem.AutoStorageIndexBuffer rendersystem$autostorageindexbuffer = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);
         GpuBuffer gpubuffer = rendersystem$autostorageindexbuffer.getBuffer(i);
         VertexFormat.IndexType vertexformat$indextype = rendersystem$autostorageindexbuffer.type();
         GpuBufferSlice gpubufferslice = RenderSystem.getDynamicUniforms()
            .writeTransform(new Matrix4f().setTranslation(0.0F, 0.0F, -11000.0F), new Vector4f(1.0F, 1.0F, 1.0F, 1.0F), new Vector3f(), new Matrix4f());
         if (this.firstDrawIndexAfterBlur > 0) {
            this.executeDrawRange(
               () -> "GUI before blur",
               rendertarget,
               p_419894_,
               gpubufferslice,
               gpubuffer,
               vertexformat$indextype,
               0,
               Math.min(this.firstDrawIndexAfterBlur, this.draws.size())
            );
         }

         if (this.draws.size() > this.firstDrawIndexAfterBlur) {
            RenderSystem.getDevice().createCommandEncoder().clearDepthTexture(rendertarget.getDepthTexture(), 1.0);
            minecraft.gameRenderer.processBlurEffect();
            this.executeDrawRange(
               () -> "GUI after blur",
               rendertarget,
               p_419894_,
               gpubufferslice,
               gpubuffer,
               vertexformat$indextype,
               this.firstDrawIndexAfterBlur,
               this.draws.size()
            );
         }
      }
   }

   private void executeDrawRange(
      Supplier<String> p_420024_,
      RenderTarget p_419589_,
      GpuBufferSlice p_419814_,
      GpuBufferSlice p_419632_,
      GpuBuffer p_419743_,
      VertexFormat.IndexType p_419509_,
      int p_419798_,
      int p_419773_
   ) {
      try (RenderPass renderpass = RenderSystem.getDevice()
            .createCommandEncoder()
            .createRenderPass(
               p_420024_,
               p_419589_.getColorTextureView(),
               OptionalInt.empty(),
               p_419589_.useDepth ? p_419589_.getDepthTextureView() : null,
               OptionalDouble.empty()
            )) {
         RenderSystem.bindDefaultUniforms(renderpass);
         renderpass.setUniform("Fog", p_419814_);
         renderpass.setUniform("DynamicTransforms", p_419632_);

         for (int i = p_419798_; i < p_419773_; i++) {
            GuiRenderer.Draw guirenderer$draw = this.draws.get(i);
            this.executeDraw(guirenderer$draw, renderpass, p_419743_, p_419509_);
         }
      }
   }

   private void addElementToMesh(GuiElementRenderState p_419849_) {
      RenderPipeline renderpipeline = p_419849_.pipeline();
      TextureSetup texturesetup = p_419849_.textureSetup();
      ScreenRectangle screenrectangle = p_419849_.scissorArea();
      if (renderpipeline != this.previousPipeline
         || this.scissorChanged(screenrectangle, this.previousScissorArea)
         || renderpipeline.getVertexFormatMode().connectedPrimitives
         || !texturesetup.equals(this.previousTextureSetup)) {
         if (this.bufferBuilder != null) {
            this.recordMesh(this.bufferBuilder, this.previousPipeline, this.previousTextureSetup, this.previousScissorArea);
         }

         this.bufferBuilder = this.getBufferBuilder(renderpipeline);
         this.previousPipeline = renderpipeline;
         this.previousTextureSetup = texturesetup;
         this.previousScissorArea = screenrectangle;
      }

      p_419849_.buildVertices(this.bufferBuilder);
   }

   private void prepareText() {
      this.renderState.forEachText(p_457345_ -> {
         final Matrix3x2fc matrix3x2fc = p_457345_.pose;
         final ScreenRectangle screenrectangle = p_457345_.scissor;
         p_457345_.ensurePrepared().visit(new Font.GlyphVisitor() {
            @Override
            public void acceptGlyph(TextRenderable.Styled p_457512_) {
               this.accept(p_457512_);
            }

            @Override
            public void acceptEffect(TextRenderable p_438996_) {
               this.accept(p_438996_);
            }

            private void accept(TextRenderable p_439019_) {
               GuiRenderer.this.renderState.submitGlyphToCurrentLayer(new GlyphRenderState(matrix3x2fc, p_439019_, screenrectangle));
            }
         });
      });
   }

   private void prepareItemElements() {
      if (!this.renderState.getItemModelIdentities().isEmpty()) {
         int i = this.getGuiScaleInvalidatingItemAtlasIfChanged();
         int j = 16 * i;
         int k = this.calculateAtlasSizeInPixels(j);
         if (this.itemsAtlas == null) {
            this.createAtlasTextures(k);
         }

         RenderSystem.outputColorTextureOverride = this.itemsAtlasView;
         RenderSystem.outputDepthTextureOverride = this.itemsAtlasDepthView;
         RenderSystem.setProjectionMatrix(this.itemsProjectionMatrixBuffer.getBuffer(k, k), ProjectionType.ORTHOGRAPHIC);
         Minecraft.getInstance().gameRenderer.getLighting().setupFor(Lighting.Entry.ITEMS_3D);
         PoseStack posestack = new PoseStack();
         MutableBoolean mutableboolean = new MutableBoolean(false);
         MutableBoolean mutableboolean1 = new MutableBoolean(false);
         this.renderState
            .forEachItem(
               p_428048_ -> {
                  if (p_428048_.oversizedItemBounds() != null) {
                     mutableboolean1.setTrue();
                  } else {
                     TrackingItemStackRenderState trackingitemstackrenderstate = p_428048_.itemStackRenderState();
                     GuiRenderer.AtlasPosition guirenderer$atlasposition = this.atlasPositions.get(trackingitemstackrenderstate.getModelIdentity());
                     if (guirenderer$atlasposition == null
                        || trackingitemstackrenderstate.isAnimated() && guirenderer$atlasposition.lastAnimatedOnFrame != this.frameNumber) {
                        if (this.itemAtlasX + j > k) {
                           this.itemAtlasX = 0;
                           this.itemAtlasY += j;
                        }

                        boolean flag = trackingitemstackrenderstate.isAnimated() && guirenderer$atlasposition != null;
                        if (flag || this.itemAtlasY + j <= k) {
                           int l = flag ? guirenderer$atlasposition.x : this.itemAtlasX;
                           int i1 = flag ? guirenderer$atlasposition.y : this.itemAtlasY;
                           if (flag) {
                              RenderSystem.getDevice()
                                 .createCommandEncoder()
                                 .clearColorAndDepthTextures(this.itemsAtlas, 0, this.itemsAtlasDepth, 1.0, l, k - i1 - j, j, j);
                           }

                           this.renderItemToAtlas(trackingitemstackrenderstate, posestack, l, i1, j);
                           float f = (float)l / k;
                           float f1 = (float)(k - i1) / k;
                           this.submitBlitFromItemAtlas(p_428048_, f, f1, j, k);
                           if (flag) {
                              guirenderer$atlasposition.lastAnimatedOnFrame = this.frameNumber;
                           } else {
                              this.atlasPositions
                                 .put(
                                    p_428048_.itemStackRenderState().getModelIdentity(),
                                    new GuiRenderer.AtlasPosition(this.itemAtlasX, this.itemAtlasY, f, f1, this.frameNumber)
                                 );
                              this.itemAtlasX += j;
                           }
                        } else if (mutableboolean.isFalse()) {
                           LOGGER.warn("Trying to render too many items in GUI at the same time. Skipping some of them.");
                           mutableboolean.setTrue();
                        }
                     } else {
                        this.submitBlitFromItemAtlas(p_428048_, guirenderer$atlasposition.u, guirenderer$atlasposition.v, j, k);
                     }
                  }
               }
            );
         RenderSystem.outputColorTextureOverride = null;
         RenderSystem.outputDepthTextureOverride = null;
         if (mutableboolean1.booleanValue()) {
            this.renderState
               .forEachItem(
                  p_428050_ -> {
                     if (p_428050_.oversizedItemBounds() != null) {
                        TrackingItemStackRenderState trackingitemstackrenderstate = p_428050_.itemStackRenderState();
                        OversizedItemRenderer oversizeditemrenderer = this.oversizedItemRenderers
                           .computeIfAbsent(trackingitemstackrenderstate.getModelIdentity(), p_428051_ -> new OversizedItemRenderer(this.bufferSource));
                        ScreenRectangle screenrectangle = p_428050_.oversizedItemBounds();
                        OversizedItemRenderState oversizeditemrenderstate = new OversizedItemRenderState(
                           p_428050_, screenrectangle.left(), screenrectangle.top(), screenrectangle.right(), screenrectangle.bottom()
                        );
                        oversizeditemrenderer.prepare(oversizeditemrenderstate, this.renderState, i);
                     }
                  }
               );
         }
      }
   }

   private void preparePictureInPicture() {
      int i = Minecraft.getInstance().getWindow().getGuiScale();
      this.pictureInPictureRenderStatesScratch.clear();
      this.renderState.forEachPictureInPicture(state -> {
         if (this.preparePictureInPictureState(state, i, true)) {
            this.pictureInPictureRenderStatesScratch.add(state);
         }
      });
      this.renderState.forEachPictureInPicture(state -> {
         if (this.pictureInPictureRenderStatesScratch.add(state)) {
            this.preparePictureInPictureState(state, i, false);
         }
      });
      this.pictureInPictureRenderStatesScratch.clear();
   }

   private <T extends PictureInPictureRenderState> boolean preparePictureInPictureState(T p_415554_, int p_415565_, boolean firstPass) {
      PictureInPictureRendererPool<T> pool = (PictureInPictureRendererPool<T>)this.pictureInPictureRendererPools.get(p_415554_.getClass());
      if (pool == null) {
         return false;
      } else {
         PictureInPictureRenderer<T> pictureinpicturerenderer = pool.get(p_415554_, p_415565_, firstPass);
         if (pictureinpicturerenderer != null) {
            pictureinpicturerenderer.prepare(p_415554_, this.renderState, p_415565_);
            return true;
         } else {
            return false;
         }
      }
   }

   private void renderItemToAtlas(TrackingItemStackRenderState p_428843_, PoseStack p_416703_, int p_416443_, int p_416400_, int p_416052_) {
      p_416703_.pushPose();
      p_416703_.translate(p_416443_ + p_416052_ / 2.0F, p_416400_ + p_416052_ / 2.0F, 0.0F);
      p_416703_.scale(p_416052_, -p_416052_, p_416052_);
      boolean flag = !p_428843_.usesBlockLight();
      if (flag) {
         Minecraft.getInstance().gameRenderer.getLighting().setupFor(Lighting.Entry.ITEMS_FLAT);
      } else {
         Minecraft.getInstance().gameRenderer.getLighting().setupFor(Lighting.Entry.ITEMS_3D);
      }

      RenderSystem.enableScissorForRenderTypeDraws(p_416443_, this.itemsAtlas.getHeight(0) - p_416400_ - p_416052_, p_416052_, p_416052_);
      p_428843_.submit(p_416703_, this.submitNodeCollector, 15728880, OverlayTexture.NO_OVERLAY, 0);
      this.featureRenderDispatcher.renderAllFeatures();
      this.bufferSource.endBatch();
      RenderSystem.disableScissorForRenderTypeDraws();
      p_416703_.popPose();
   }

   private void submitBlitFromItemAtlas(GuiItemRenderState p_416534_, float p_416284_, float p_416546_, int p_415743_, int p_416437_) {
      float f = p_416284_ + (float)p_415743_ / p_416437_;
      float f1 = p_416546_ + (float)(-p_415743_) / p_416437_;
      this.renderState
         .submitBlitToCurrentLayer(
            new BlitRenderState(
               RenderPipelines.GUI_TEXTURED_PREMULTIPLIED_ALPHA,
               TextureSetup.singleTexture(this.itemsAtlasView, RenderSystem.getSamplerCache().getRepeat(FilterMode.NEAREST)),
               p_416534_.pose(),
               p_416534_.x(),
               p_416534_.y(),
               p_416534_.x() + 16,
               p_416534_.y() + 16,
               p_416284_,
               f,
               p_416546_,
               f1,
               -1,
               p_416534_.scissorArea(),
               null
            )
         );
   }

   private void createAtlasTextures(int p_416115_) {
      GpuDevice gpudevice = RenderSystem.getDevice();
      this.itemsAtlas = gpudevice.createTexture("UI items atlas", 12, TextureFormat.RGBA8, p_416115_, p_416115_, 1, 1);
      this.itemsAtlasView = gpudevice.createTextureView(this.itemsAtlas);
      this.itemsAtlasDepth = gpudevice.createTexture("UI items atlas depth", 8, TextureFormat.DEPTH32, p_416115_, p_416115_, 1, 1);
      this.itemsAtlasDepthView = gpudevice.createTextureView(this.itemsAtlasDepth);
      gpudevice.createCommandEncoder().clearColorAndDepthTextures(this.itemsAtlas, 0, this.itemsAtlasDepth, 1.0);
   }

   private int calculateAtlasSizeInPixels(int p_415560_) {
      Set<Object> set = this.renderState.getItemModelIdentities();
      int i;
      if (this.atlasPositions.isEmpty()) {
         i = set.size();
      } else {
         i = this.atlasPositions.size();

         for (Object object : set) {
            if (!this.atlasPositions.containsKey(object)) {
               i++;
            }
         }
      }

      if (this.itemsAtlas != null) {
         int j = this.itemsAtlas.getWidth(0) / p_415560_;
         int l = j * j;
         if (i < l) {
            return this.itemsAtlas.getWidth(0);
         }

         this.invalidateItemAtlas();
      }

      int k = set.size();
      int i1 = Mth.smallestSquareSide(k + k / 2);
      return Math.clamp((long)Mth.smallestEncompassingPowerOfTwo(i1 * p_415560_), 512, MAXIMUM_ITEM_ATLAS_SIZE);
   }

   private int getGuiScaleInvalidatingItemAtlasIfChanged() {
      int i = Minecraft.getInstance().getWindow().getGuiScale();
      if (i != this.cachedGuiScale) {
         this.invalidateItemAtlas();

         for (OversizedItemRenderer oversizeditemrenderer : this.oversizedItemRenderers.values()) {
            oversizeditemrenderer.invalidateTexture();
         }

         this.cachedGuiScale = i;
      }

      return i;
   }

   private void invalidateItemAtlas() {
      this.itemAtlasX = 0;
      this.itemAtlasY = 0;
      this.atlasPositions.clear();
      if (this.itemsAtlas != null) {
         this.itemsAtlas.close();
         this.itemsAtlas = null;
      }

      if (this.itemsAtlasView != null) {
         this.itemsAtlasView.close();
         this.itemsAtlasView = null;
      }

      if (this.itemsAtlasDepth != null) {
         this.itemsAtlasDepth.close();
         this.itemsAtlasDepth = null;
      }

      if (this.itemsAtlasDepthView != null) {
         this.itemsAtlasDepthView.close();
         this.itemsAtlasDepthView = null;
      }
   }

   private void recordMesh(BufferBuilder p_419584_, RenderPipeline p_419793_, TextureSetup p_419987_, @Nullable ScreenRectangle p_419796_) {
      MeshData meshdata = p_419584_.build();
      if (meshdata != null) {
         this.meshesToDraw.add(new GuiRenderer.MeshToDraw(meshdata, p_419793_, p_419987_, p_419796_));
      }
   }

   private void recordDraws() {
      this.ensureVertexBufferSizes();
      CommandEncoder commandencoder = RenderSystem.getDevice().createCommandEncoder();
      Object2IntMap<VertexFormat> object2intmap = new Object2IntOpenHashMap();

      for (GuiRenderer.MeshToDraw guirenderer$meshtodraw : this.meshesToDraw) {
         MeshData meshdata = guirenderer$meshtodraw.mesh;
         MeshData.DrawState meshdata$drawstate = meshdata.drawState();
         VertexFormat vertexformat = meshdata$drawstate.format();
         MappableRingBuffer mappableringbuffer = this.vertexBuffers.get(vertexformat);
         if (!object2intmap.containsKey(vertexformat)) {
            object2intmap.put(vertexformat, 0);
         }

         ByteBuffer bytebuffer = meshdata.vertexBuffer();
         int i = bytebuffer.remaining();
         int j = object2intmap.getInt(vertexformat);

         try (GpuBuffer.MappedView gpubuffer$mappedview = commandencoder.mapBuffer(mappableringbuffer.currentBuffer().slice(j, i), false, true)) {
            MemoryUtil.memCopy(bytebuffer, gpubuffer$mappedview.data());
         }

         object2intmap.put(vertexformat, j + i);
         this.draws
            .add(
               new GuiRenderer.Draw(
                  mappableringbuffer.currentBuffer(),
                  j / vertexformat.getVertexSize(),
                  meshdata$drawstate.mode(),
                  meshdata$drawstate.indexCount(),
                  guirenderer$meshtodraw.pipeline,
                  guirenderer$meshtodraw.textureSetup,
                  guirenderer$meshtodraw.scissorArea
               )
            );
         guirenderer$meshtodraw.close();
      }
   }

   private void ensureVertexBufferSizes() {
      Object2IntMap<VertexFormat> object2intmap = this.calculatedRequiredVertexBufferSizes();
      ObjectIterator var2 = object2intmap.object2IntEntrySet().iterator();

      while (var2.hasNext()) {
         it.unimi.dsi.fastutil.objects.Object2IntMap.Entry<VertexFormat> entry = (it.unimi.dsi.fastutil.objects.Object2IntMap.Entry<VertexFormat>)var2.next();
         VertexFormat vertexformat = (VertexFormat)entry.getKey();
         int i = entry.getIntValue();
         MappableRingBuffer mappableringbuffer = this.vertexBuffers.get(vertexformat);
         if (mappableringbuffer == null || mappableringbuffer.size() < i) {
            if (mappableringbuffer != null) {
               mappableringbuffer.close();
            }

            this.vertexBuffers.put(vertexformat, new MappableRingBuffer(() -> "GUI vertex buffer for " + vertexformat, 34, i));
         }
      }
   }

   private Object2IntMap<VertexFormat> calculatedRequiredVertexBufferSizes() {
      Object2IntMap<VertexFormat> object2intmap = new Object2IntOpenHashMap();

      for (GuiRenderer.MeshToDraw guirenderer$meshtodraw : this.meshesToDraw) {
         MeshData.DrawState meshdata$drawstate = guirenderer$meshtodraw.mesh.drawState();
         VertexFormat vertexformat = meshdata$drawstate.format();
         if (!object2intmap.containsKey(vertexformat)) {
            object2intmap.put(vertexformat, 0);
         }

         object2intmap.put(vertexformat, object2intmap.getInt(vertexformat) + meshdata$drawstate.vertexCount() * vertexformat.getVertexSize());
      }

      return object2intmap;
   }

   private void executeDraw(GuiRenderer.Draw p_416097_, RenderPass p_416119_, GpuBuffer p_416661_, VertexFormat.IndexType p_415757_) {
      RenderPipeline renderpipeline = p_416097_.pipeline();
      p_416119_.setPipeline(renderpipeline);
      p_416119_.setVertexBuffer(0, p_416097_.vertexBuffer);
      ScreenRectangle screenrectangle = p_416097_.scissorArea();
      if (screenrectangle != null) {
         this.enableScissor(screenrectangle, p_416119_);
      } else {
         p_416119_.disableScissor();
      }

      if (p_416097_.textureSetup.texure0() != null) {
         p_416119_.bindTexture("Sampler0", p_416097_.textureSetup.texure0(), p_416097_.textureSetup.sampler0());
      }

      if (p_416097_.textureSetup.texure1() != null) {
         p_416119_.bindTexture("Sampler1", p_416097_.textureSetup.texure1(), p_416097_.textureSetup.sampler1());
      }

      if (p_416097_.textureSetup.texure2() != null) {
         p_416119_.bindTexture("Sampler2", p_416097_.textureSetup.texure2(), p_416097_.textureSetup.sampler2());
      }

      VertexFormat.Mode formatMode = renderpipeline.getVertexFormatMode();
      if (formatMode != VertexFormat.Mode.QUADS) {
         RenderSystem.AutoStorageIndexBuffer autoStorageIndexBuffer = RenderSystem.getSequentialBuffer(formatMode);
         p_416661_ = autoStorageIndexBuffer.getBuffer(p_416097_.indexCount);
         p_415757_ = autoStorageIndexBuffer.type();
      }

      p_416119_.setIndexBuffer(p_416661_, p_415757_);
      p_416119_.drawIndexed(p_416097_.baseVertex, 0, p_416097_.indexCount, 1);
   }

   private BufferBuilder getBufferBuilder(RenderPipeline p_416066_) {
      return new BufferBuilder(this.byteBufferBuilder, p_416066_.getVertexFormatMode(), p_416066_.getVertexFormat());
   }

   private boolean scissorChanged(@Nullable ScreenRectangle p_416356_, @Nullable ScreenRectangle p_416077_) {
      if (p_416356_ == p_416077_) {
         return false;
      } else {
         return p_416356_ != null ? !p_416356_.equals(p_416077_) : true;
      }
   }

   private void enableScissor(ScreenRectangle p_416458_, RenderPass p_416455_) {
      Window window = Minecraft.getInstance().getWindow();
      int i = window.getHeight();
      int j = window.getGuiScale();
      double d0 = p_416458_.left() * j;
      double d1 = i - p_416458_.bottom() * j;
      double d2 = p_416458_.width() * j;
      double d3 = p_416458_.height() * j;
      p_416455_.enableScissor((int)d0, (int)d1, Math.max(0, (int)d2), Math.max(0, (int)d3));
   }

   @Override
   public void close() {
      this.byteBufferBuilder.close();
      if (this.itemsAtlas != null) {
         this.itemsAtlas.close();
      }

      if (this.itemsAtlasView != null) {
         this.itemsAtlasView.close();
      }

      if (this.itemsAtlasDepth != null) {
         this.itemsAtlasDepth.close();
      }

      if (this.itemsAtlasDepthView != null) {
         this.itemsAtlasDepthView.close();
      }

      this.pictureInPictureRendererPools.values().forEach(PictureInPictureRendererPool::close);
      this.guiProjectionMatrixBuffer.close();
      this.itemsProjectionMatrixBuffer.close();

      for (MappableRingBuffer mappableringbuffer : this.vertexBuffers.values()) {
         mappableringbuffer.close();
      }

      this.oversizedItemRenderers.values().forEach(PictureInPictureRenderer::close);
   }

   @OnlyIn(Dist.CLIENT)
   static final class AtlasPosition {
      final int x;
      final int y;
      final float u;
      final float v;
      int lastAnimatedOnFrame;

      AtlasPosition(int p_415957_, int p_416084_, float p_416595_, float p_416485_, int p_415830_) {
         this.x = p_415957_;
         this.y = p_416084_;
         this.u = p_416595_;
         this.v = p_416485_;
         this.lastAnimatedOnFrame = p_415830_;
      }
   }

   @OnlyIn(Dist.CLIENT)
   record Draw(
      GpuBuffer vertexBuffer,
      int baseVertex,
      VertexFormat.Mode mode,
      int indexCount,
      RenderPipeline pipeline,
      TextureSetup textureSetup,
      @Nullable ScreenRectangle scissorArea
   ) {
   }

   @OnlyIn(Dist.CLIENT)
   record MeshToDraw(MeshData mesh, RenderPipeline pipeline, TextureSetup textureSetup, @Nullable ScreenRectangle scissorArea) implements AutoCloseable {
      @Override
      public void close() {
         this.mesh.close();
      }
   }
}
