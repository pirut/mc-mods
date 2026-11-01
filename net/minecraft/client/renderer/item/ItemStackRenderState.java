package net.minecraft.client.renderer.item;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class ItemStackRenderState {
   ItemDisplayContext displayContext = ItemDisplayContext.NONE;
   private int activeLayerCount;
   private boolean animated;
   private boolean oversizedInGui;
   @Nullable
   private AABB cachedModelBoundingBox;
   private ItemStackRenderState.LayerRenderState[] layers = new ItemStackRenderState.LayerRenderState[]{new ItemStackRenderState.LayerRenderState()};

   public void ensureCapacity(int p_387339_) {
      int i = this.layers.length;
      int j = this.activeLayerCount + p_387339_;
      if (j > i) {
         this.layers = Arrays.copyOf(this.layers, j);

         for (int k = i; k < j; k++) {
            this.layers[k] = new ItemStackRenderState.LayerRenderState();
         }
      }
   }

   public ItemStackRenderState.LayerRenderState newLayer() {
      this.ensureCapacity(1);
      return this.layers[this.activeLayerCount++];
   }

   public void clear() {
      this.displayContext = ItemDisplayContext.NONE;

      for (int i = 0; i < this.activeLayerCount; i++) {
         this.layers[i].clear();
      }

      this.activeLayerCount = 0;
      this.animated = false;
      this.oversizedInGui = false;
      this.cachedModelBoundingBox = null;
   }

   public void setAnimated() {
      this.animated = true;
   }

   public boolean isAnimated() {
      return this.animated;
   }

   public void appendModelIdentityElement(Object p_416649_) {
   }

   private ItemStackRenderState.LayerRenderState firstLayer() {
      return this.layers[0];
   }

   public boolean isEmpty() {
      return this.activeLayerCount == 0;
   }

   public boolean usesBlockLight() {
      return this.firstLayer().usesBlockLight;
   }

   @Nullable
   public TextureAtlasSprite pickParticleIcon(RandomSource p_387539_) {
      return this.activeLayerCount == 0 ? null : this.layers[p_387539_.nextInt(this.activeLayerCount)].particleIcon;
   }

   public void visitExtents(Consumer<Vector3fc> p_405345_) {
      Vector3f vector3f = new Vector3f();
      PoseStack.Pose posestack$pose = new PoseStack.Pose();

      for (int i = 0; i < this.activeLayerCount; i++) {
         ItemStackRenderState.LayerRenderState itemstackrenderstate$layerrenderstate = this.layers[i];
         itemstackrenderstate$layerrenderstate.transform.apply(this.displayContext.leftHand(), posestack$pose);
         Matrix4f matrix4f = posestack$pose.pose();
         Vector3fc[] avector3fc = itemstackrenderstate$layerrenderstate.extents.get();

         for (Vector3fc vector3fc : avector3fc) {
            p_405345_.accept(vector3f.set(vector3fc).mulPosition(matrix4f));
         }

         posestack$pose.setIdentity();
      }
   }

   public void submit(PoseStack p_439009_, SubmitNodeCollector p_439820_, int p_439168_, int p_439735_, int p_440735_) {
      for (int i = 0; i < this.activeLayerCount; i++) {
         this.layers[i].submit(p_439009_, p_439820_, p_439168_, p_439735_, p_440735_);
      }
   }

   public AABB getModelBoundingBox() {
      if (this.cachedModelBoundingBox != null) {
         return this.cachedModelBoundingBox;
      } else {
         AABB.Builder aabb$builder = new AABB.Builder();
         this.visitExtents(aabb$builder::include);
         AABB aabb = aabb$builder.build();
         this.cachedModelBoundingBox = aabb;
         return aabb;
      }
   }

   public void setOversizedInGui(boolean p_428189_) {
      this.oversizedInGui = p_428189_;
   }

   public boolean isOversizedInGui() {
      return this.oversizedInGui;
   }

   @OnlyIn(Dist.CLIENT)
   public static enum FoilType {
      NONE,
      STANDARD,
      SPECIAL;
   }

   @OnlyIn(Dist.CLIENT)
   public class LayerRenderState {
      private static final Vector3fc[] NO_EXTENTS = new Vector3fc[0];
      public static final Supplier<Vector3fc[]> NO_EXTENTS_SUPPLIER = () -> NO_EXTENTS;
      private final List<BakedQuad> quads = new ArrayList<>();
      boolean usesBlockLight;
      @Nullable
      TextureAtlasSprite particleIcon;
      ItemTransform transform = ItemTransform.NO_TRANSFORM;
      @Nullable
      private RenderType renderType;
      private ItemStackRenderState.FoilType foilType = ItemStackRenderState.FoilType.NONE;
      private int[] tintLayers = new int[0];
      @Nullable
      private SpecialModelRenderer<Object> specialRenderer;
      @Nullable
      private Object argumentForSpecialRendering;
      Supplier<Vector3fc[]> extents = NO_EXTENTS_SUPPLIER;

      public void clear() {
         this.quads.clear();
         this.renderType = null;
         this.foilType = ItemStackRenderState.FoilType.NONE;
         this.specialRenderer = null;
         this.argumentForSpecialRendering = null;
         Arrays.fill(this.tintLayers, -1);
         this.usesBlockLight = false;
         this.particleIcon = null;
         this.transform = ItemTransform.NO_TRANSFORM;
         this.extents = NO_EXTENTS_SUPPLIER;
      }

      public List<BakedQuad> prepareQuadList() {
         return this.quads;
      }

      public void setRenderType(RenderType p_459027_) {
         this.renderType = p_459027_;
      }

      public void setUsesBlockLight(boolean p_405321_) {
         this.usesBlockLight = p_405321_;
      }

      public void setExtents(Supplier<Vector3fc[]> p_405802_) {
         this.extents = p_405802_;
      }

      public void setParticleIcon(TextureAtlasSprite p_404929_) {
         this.particleIcon = p_404929_;
      }

      public void setTransform(ItemTransform p_404949_) {
         this.transform = p_404949_;
      }

      public <T> void setupSpecialModel(SpecialModelRenderer<T> p_386884_, @Nullable T p_388093_) {
         this.specialRenderer = eraseSpecialRenderer(p_386884_);
         this.argumentForSpecialRendering = p_388093_;
      }

      private static SpecialModelRenderer<Object> eraseSpecialRenderer(SpecialModelRenderer<?> p_388852_) {
         return (SpecialModelRenderer<Object>)p_388852_;
      }

      public void setFoilType(ItemStackRenderState.FoilType p_386511_) {
         this.foilType = p_386511_;
      }

      public int[] prepareTintLayers(int p_387793_) {
         if (p_387793_ > this.tintLayers.length) {
            this.tintLayers = new int[p_387793_];
            Arrays.fill(this.tintLayers, -1);
         }

         return this.tintLayers;
      }

      void submit(PoseStack p_439934_, SubmitNodeCollector p_439690_, int p_439886_, int p_440533_, int p_440728_) {
         p_439934_.pushPose();
         this.transform.apply(ItemStackRenderState.this.displayContext.leftHand(), p_439934_.last());
         if (this.specialRenderer != null) {
            this.specialRenderer
               .submit(
                  this.argumentForSpecialRendering,
                  ItemStackRenderState.this.displayContext,
                  p_439934_,
                  p_439690_,
                  p_439886_,
                  p_440533_,
                  this.foilType != ItemStackRenderState.FoilType.NONE,
                  p_440728_
               );
         } else if (this.renderType != null) {
            p_439690_.submitItem(
               p_439934_,
               ItemStackRenderState.this.displayContext,
               p_439886_,
               p_440533_,
               p_440728_,
               this.tintLayers,
               this.quads,
               this.renderType,
               this.foilType
            );
         }

         p_439934_.popPose();
      }
   }
}
