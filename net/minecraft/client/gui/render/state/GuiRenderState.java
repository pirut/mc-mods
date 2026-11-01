package net.minecraft.client.gui.render.state;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.SharedConstants;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.render.state.pip.PictureInPictureRenderState;
import net.minecraft.client.renderer.RenderPipelines;
import org.joml.Matrix3x2f;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class GuiRenderState {
   private static final int DEBUG_RECTANGLE_COLOR = 2000962815;
   private final List<GuiRenderState.Node> strata = new ArrayList<>();
   private int firstStratumAfterBlur = Integer.MAX_VALUE;
   private GuiRenderState.Node current;
   private final Set<Object> itemModelIdentities = new HashSet<>();
   @Nullable
   private ScreenRectangle lastElementBounds;

   public GuiRenderState() {
      this.nextStratum();
   }

   public void nextStratum() {
      this.current = new GuiRenderState.Node(null);
      this.strata.add(this.current);
   }

   public void blurBeforeThisStratum() {
      if (this.firstStratumAfterBlur != Integer.MAX_VALUE) {
         throw new IllegalStateException("Can only blur once per frame");
      } else {
         this.firstStratumAfterBlur = this.strata.size() - 1;
      }
   }

   public void up() {
      if (this.current.up == null) {
         this.current.up = new GuiRenderState.Node(this.current);
      }

      this.current = this.current.up;
   }

   public void submitItem(GuiItemRenderState renderState) {
      if (this.findAppropriateNode(renderState)) {
         this.itemModelIdentities.add(renderState.itemStackRenderState().getModelIdentity());
         this.current.submitItem(renderState);
         this.sumbitDebugRectangleIfEnabled(renderState.bounds());
      }
   }

   public void submitText(GuiTextRenderState renderState) {
      if (this.findAppropriateNode(renderState)) {
         this.current.submitText(renderState);
         this.sumbitDebugRectangleIfEnabled(renderState.bounds());
      }
   }

   public void submitPicturesInPictureState(PictureInPictureRenderState renderState) {
      if (this.findAppropriateNode(renderState)) {
         this.current.submitPicturesInPictureState(renderState);
         this.sumbitDebugRectangleIfEnabled(renderState.bounds());
      }
   }

   public void submitGuiElement(GuiElementRenderState renderState) {
      if (this.findAppropriateNode(renderState)) {
         this.current.submitGuiElement(renderState);
         this.sumbitDebugRectangleIfEnabled(renderState.bounds());
      }
   }

   private void sumbitDebugRectangleIfEnabled(@Nullable ScreenRectangle debugRectangle) {
      if (SharedConstants.DEBUG_RENDER_UI_LAYERING_RECTANGLES && debugRectangle != null) {
         this.up();
         this.current
            .submitGuiElement(
               new ColoredRectangleRenderState(
                  RenderPipelines.GUI, TextureSetup.noTexture(), new Matrix3x2f(), 0, 0, 10000, 10000, 2000962815, 2000962815, debugRectangle
               )
            );
      }
   }

   private boolean findAppropriateNode(ScreenArea screenArea) {
      ScreenRectangle screenRectangle = screenArea.bounds();
      if (screenRectangle == null) {
         return false;
      } else {
         if (this.lastElementBounds != null && this.lastElementBounds.encompasses(screenRectangle)) {
            this.up();
         } else {
            this.navigateToAboveHighestElementWithIntersectingBounds(screenRectangle);
         }

         this.lastElementBounds = screenRectangle;
         return true;
      }
   }

   private void navigateToAboveHighestElementWithIntersectingBounds(ScreenRectangle rectangle) {
      GuiRenderState.Node node = this.strata.getLast();

      while (node.up != null) {
         node = node.up;
      }

      boolean bl = false;

      while (!bl) {
         bl = this.hasIntersection(rectangle, node.elementStates)
            || this.hasIntersection(rectangle, node.itemStates)
            || this.hasIntersection(rectangle, node.textStates)
            || this.hasIntersection(rectangle, node.picturesInPictureStates);
         if (node.parent == null) {
            break;
         }

         if (!bl) {
            node = node.parent;
         }
      }

      this.current = node;
      if (bl) {
         this.up();
      }
   }

   private boolean hasIntersection(ScreenRectangle rectangle, @Nullable List<? extends ScreenArea> screenAreas) {
      if (screenAreas != null) {
         for (ScreenArea screenArea : screenAreas) {
            ScreenRectangle screenRectangle = screenArea.bounds();
            if (screenRectangle != null && screenRectangle.intersects(rectangle)) {
               return true;
            }
         }
      }

      return false;
   }

   public void submitBlitToCurrentLayer(BlitRenderState renderState) {
      this.current.submitGuiElement(renderState);
   }

   public void submitGlyphToCurrentLayer(GuiElementRenderState renderState) {
      this.current.submitGlyph(renderState);
   }

   public Set<Object> getItemModelIdentities() {
      return this.itemModelIdentities;
   }

   public void forEachElement(Consumer<GuiElementRenderState> action, GuiRenderState.TraverseRange traverseRange) {
      this.traverse(node -> {
         if (node.elementStates != null || node.glyphStates != null) {
            if (node.elementStates != null) {
               for (GuiElementRenderState guiElementRenderState : node.elementStates) {
                  action.accept(guiElementRenderState);
               }
            }

            if (node.glyphStates != null) {
               for (GuiElementRenderState guiElementRenderState : node.glyphStates) {
                  action.accept(guiElementRenderState);
               }
            }
         }
      }, traverseRange);
   }

   public void forEachItem(Consumer<GuiItemRenderState> action) {
      GuiRenderState.Node node = this.current;
      this.traverse(nodex -> {
         if (nodex.itemStates != null) {
            this.current = nodex;

            for (GuiItemRenderState guiItemRenderState : nodex.itemStates) {
               action.accept(guiItemRenderState);
            }
         }
      }, GuiRenderState.TraverseRange.ALL);
      this.current = node;
   }

   public void forEachText(Consumer<GuiTextRenderState> action) {
      GuiRenderState.Node node = this.current;
      this.traverse(nodex -> {
         if (nodex.textStates != null) {
            for (GuiTextRenderState guiTextRenderState : nodex.textStates) {
               this.current = nodex;
               action.accept(guiTextRenderState);
            }
         }
      }, GuiRenderState.TraverseRange.ALL);
      this.current = node;
   }

   public void forEachPictureInPicture(Consumer<PictureInPictureRenderState> action) {
      GuiRenderState.Node node = this.current;
      this.traverse(nodex -> {
         if (nodex.picturesInPictureStates != null) {
            this.current = nodex;

            for (PictureInPictureRenderState pictureInPictureRenderState : nodex.picturesInPictureStates) {
               action.accept(pictureInPictureRenderState);
            }
         }
      }, GuiRenderState.TraverseRange.ALL);
      this.current = node;
   }

   public void sortElements(Comparator<GuiElementRenderState> comparator) {
      this.traverse(node -> {
         if (node.elementStates != null) {
            if (SharedConstants.DEBUG_SHUFFLE_UI_RENDERING_ORDER) {
               Collections.shuffle(node.elementStates);
            }

            node.elementStates.sort(comparator);
         }
      }, GuiRenderState.TraverseRange.ALL);
   }

   private void traverse(Consumer<GuiRenderState.Node> action, GuiRenderState.TraverseRange traverseRange) {
      int i = 0;
      int j = this.strata.size();
      if (traverseRange == GuiRenderState.TraverseRange.BEFORE_BLUR) {
         j = Math.min(this.firstStratumAfterBlur, this.strata.size());
      } else if (traverseRange == GuiRenderState.TraverseRange.AFTER_BLUR) {
         i = this.firstStratumAfterBlur;
      }

      for (int k = i; k < j; k++) {
         GuiRenderState.Node node = this.strata.get(k);
         this.traverse(node, action);
      }
   }

   private void traverse(GuiRenderState.Node node, Consumer<GuiRenderState.Node> action) {
      action.accept(node);
      if (node.up != null) {
         this.traverse(node.up, action);
      }
   }

   public void reset() {
      this.itemModelIdentities.clear();
      this.strata.clear();
      this.firstStratumAfterBlur = Integer.MAX_VALUE;
      this.nextStratum();
   }

   @Environment(EnvType.CLIENT)
   static class Node {
      @Nullable
      public final GuiRenderState.Node parent;
      @Nullable
      public GuiRenderState.Node up;
      @Nullable
      public List<GuiElementRenderState> elementStates;
      @Nullable
      public List<GuiElementRenderState> glyphStates;
      @Nullable
      public List<GuiItemRenderState> itemStates;
      @Nullable
      public List<GuiTextRenderState> textStates;
      @Nullable
      public List<PictureInPictureRenderState> picturesInPictureStates;

      Node(@Nullable GuiRenderState.Node parent) {
         this.parent = parent;
      }

      public void submitItem(GuiItemRenderState renderState) {
         if (this.itemStates == null) {
            this.itemStates = new ArrayList<>();
         }

         this.itemStates.add(renderState);
      }

      public void submitText(GuiTextRenderState renderState) {
         if (this.textStates == null) {
            this.textStates = new ArrayList<>();
         }

         this.textStates.add(renderState);
      }

      public void submitPicturesInPictureState(PictureInPictureRenderState renderState) {
         if (this.picturesInPictureStates == null) {
            this.picturesInPictureStates = new ArrayList<>();
         }

         this.picturesInPictureStates.add(renderState);
      }

      public void submitGuiElement(GuiElementRenderState renderState) {
         if (this.elementStates == null) {
            this.elementStates = new ArrayList<>();
         }

         this.elementStates.add(renderState);
      }

      public void submitGlyph(GuiElementRenderState renderState) {
         if (this.glyphStates == null) {
            this.glyphStates = new ArrayList<>();
         }

         this.glyphStates.add(renderState);
      }
   }

   @Environment(EnvType.CLIENT)
   public static enum TraverseRange {
      ALL,
      BEFORE_BLUR,
      AFTER_BLUR;
   }
}
