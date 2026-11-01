package net.minecraft.client.gui.render.state;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.SharedConstants;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.render.state.pip.PictureInPictureRenderState;
import net.minecraft.client.renderer.RenderPipelines;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix3x2f;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
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

   public void submitItem(GuiItemRenderState p_416573_) {
      if (this.findAppropriateNode(p_416573_)) {
         this.itemModelIdentities.add(p_416573_.itemStackRenderState().getModelIdentity());
         this.current.submitItem(p_416573_);
         this.sumbitDebugRectangleIfEnabled(p_416573_.bounds());
      }
   }

   public void submitText(GuiTextRenderState p_416631_) {
      if (this.findAppropriateNode(p_416631_)) {
         this.current.submitText(p_416631_);
         this.sumbitDebugRectangleIfEnabled(p_416631_.bounds());
      }
   }

   public void submitPicturesInPictureState(PictureInPictureRenderState p_416091_) {
      if (this.findAppropriateNode(p_416091_)) {
         this.current.submitPicturesInPictureState(p_416091_);
         this.sumbitDebugRectangleIfEnabled(p_416091_.bounds());
      }
   }

   public void submitGuiElement(GuiElementRenderState p_416144_) {
      if (this.findAppropriateNode(p_416144_)) {
         this.current.submitGuiElement(p_416144_);
         this.sumbitDebugRectangleIfEnabled(p_416144_.bounds());
      }
   }

   private void sumbitDebugRectangleIfEnabled(@Nullable ScreenRectangle p_422532_) {
      if (SharedConstants.DEBUG_RENDER_UI_LAYERING_RECTANGLES && p_422532_ != null) {
         this.up();
         this.current
            .submitGuiElement(
               new ColoredRectangleRenderState(
                  RenderPipelines.GUI, TextureSetup.noTexture(), new Matrix3x2f(), 0, 0, 10000, 10000, 2000962815, 2000962815, p_422532_
               )
            );
      }
   }

   private boolean findAppropriateNode(ScreenArea p_422644_) {
      ScreenRectangle screenrectangle = p_422644_.bounds();
      if (screenrectangle == null) {
         return false;
      } else {
         if (this.lastElementBounds != null && this.lastElementBounds.encompasses(screenrectangle)) {
            this.up();
         } else {
            this.navigateToAboveHighestElementWithIntersectingBounds(screenrectangle);
         }

         this.lastElementBounds = screenrectangle;
         return true;
      }
   }

   private void navigateToAboveHighestElementWithIntersectingBounds(ScreenRectangle p_421652_) {
      GuiRenderState.Node guirenderstate$node = this.strata.getLast();

      while (guirenderstate$node.up != null) {
         guirenderstate$node = guirenderstate$node.up;
      }

      boolean flag = false;

      while (!flag) {
         flag = this.hasIntersection(p_421652_, guirenderstate$node.elementStates)
            || this.hasIntersection(p_421652_, guirenderstate$node.itemStates)
            || this.hasIntersection(p_421652_, guirenderstate$node.textStates)
            || this.hasIntersection(p_421652_, guirenderstate$node.picturesInPictureStates);
         if (guirenderstate$node.parent == null) {
            break;
         }

         if (!flag) {
            guirenderstate$node = guirenderstate$node.parent;
         }
      }

      this.current = guirenderstate$node;
      if (flag) {
         this.up();
      }
   }

   private boolean hasIntersection(ScreenRectangle p_422321_, @Nullable List<? extends ScreenArea> p_422715_) {
      if (p_422715_ != null) {
         for (ScreenArea screenarea : p_422715_) {
            ScreenRectangle screenrectangle = screenarea.bounds();
            if (screenrectangle != null && screenrectangle.intersects(p_422321_)) {
               return true;
            }
         }
      }

      return false;
   }

   public void submitBlitToCurrentLayer(BlitRenderState p_427445_) {
      this.current.submitGuiElement(p_427445_);
   }

   public void submitGlyphToCurrentLayer(GuiElementRenderState p_427291_) {
      this.current.submitGlyph(p_427291_);
   }

   public Set<Object> getItemModelIdentities() {
      return this.itemModelIdentities;
   }

   public void forEachElement(Consumer<GuiElementRenderState> p_436752_, GuiRenderState.TraverseRange p_419645_) {
      this.traverse(p_436488_ -> {
         if (p_436488_.elementStates != null || p_436488_.glyphStates != null) {
            if (p_436488_.elementStates != null) {
               for (GuiElementRenderState guielementrenderstate : p_436488_.elementStates) {
                  p_436752_.accept(guielementrenderstate);
               }
            }

            if (p_436488_.glyphStates != null) {
               for (GuiElementRenderState guielementrenderstate1 : p_436488_.glyphStates) {
                  p_436752_.accept(guielementrenderstate1);
               }
            }
         }
      }, p_419645_);
   }

   public void forEachItem(Consumer<GuiItemRenderState> p_418276_) {
      GuiRenderState.Node guirenderstate$node = this.current;
      this.traverse(p_418061_ -> {
         if (p_418061_.itemStates != null) {
            this.current = p_418061_;

            for (GuiItemRenderState guiitemrenderstate : p_418061_.itemStates) {
               p_418276_.accept(guiitemrenderstate);
            }
         }
      }, GuiRenderState.TraverseRange.ALL);
      this.current = guirenderstate$node;
   }

   public void forEachText(Consumer<GuiTextRenderState> p_418229_) {
      GuiRenderState.Node guirenderstate$node = this.current;
      this.traverse(p_421279_ -> {
         if (p_421279_.textStates != null) {
            for (GuiTextRenderState guitextrenderstate : p_421279_.textStates) {
               this.current = p_421279_;
               p_418229_.accept(guitextrenderstate);
            }
         }
      }, GuiRenderState.TraverseRange.ALL);
      this.current = guirenderstate$node;
   }

   public void forEachPictureInPicture(Consumer<PictureInPictureRenderState> p_418352_) {
      GuiRenderState.Node guirenderstate$node = this.current;
      this.traverse(p_418372_ -> {
         if (p_418372_.picturesInPictureStates != null) {
            this.current = p_418372_;

            for (PictureInPictureRenderState pictureinpicturerenderstate : p_418372_.picturesInPictureStates) {
               p_418352_.accept(pictureinpicturerenderstate);
            }
         }
      }, GuiRenderState.TraverseRange.ALL);
      this.current = guirenderstate$node;
   }

   public void sortElements(Comparator<GuiElementRenderState> p_418003_) {
      this.traverse(p_448686_ -> {
         if (p_448686_.elementStates != null) {
            if (SharedConstants.DEBUG_SHUFFLE_UI_RENDERING_ORDER) {
               Collections.shuffle(p_448686_.elementStates);
            }

            p_448686_.elementStates.sort(p_418003_);
         }
      }, GuiRenderState.TraverseRange.ALL);
   }

   private void traverse(Consumer<GuiRenderState.Node> p_418051_, GuiRenderState.TraverseRange p_419641_) {
      int i = 0;
      int j = this.strata.size();
      if (p_419641_ == GuiRenderState.TraverseRange.BEFORE_BLUR) {
         j = Math.min(this.firstStratumAfterBlur, this.strata.size());
      } else if (p_419641_ == GuiRenderState.TraverseRange.AFTER_BLUR) {
         i = this.firstStratumAfterBlur;
      }

      for (int k = i; k < j; k++) {
         GuiRenderState.Node guirenderstate$node = this.strata.get(k);
         this.traverse(guirenderstate$node, p_418051_);
      }
   }

   private void traverse(GuiRenderState.Node p_418468_, Consumer<GuiRenderState.Node> p_418350_) {
      p_418350_.accept(p_418468_);
      if (p_418468_.up != null) {
         this.traverse(p_418468_.up, p_418350_);
      }
   }

   public void reset() {
      this.itemModelIdentities.clear();
      this.strata.clear();
      this.firstStratumAfterBlur = Integer.MAX_VALUE;
      this.nextStratum();
   }

   @OnlyIn(Dist.CLIENT)
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

      Node(@Nullable GuiRenderState.Node p_418301_) {
         this.parent = p_418301_;
      }

      public void submitItem(GuiItemRenderState p_418222_) {
         if (this.itemStates == null) {
            this.itemStates = new ArrayList<>();
         }

         this.itemStates.add(p_418222_);
      }

      public void submitText(GuiTextRenderState p_418089_) {
         if (this.textStates == null) {
            this.textStates = new ArrayList<>();
         }

         this.textStates.add(p_418089_);
      }

      public void submitPicturesInPictureState(PictureInPictureRenderState p_418088_) {
         if (this.picturesInPictureStates == null) {
            this.picturesInPictureStates = new ArrayList<>();
         }

         this.picturesInPictureStates.add(p_418088_);
      }

      public void submitGuiElement(GuiElementRenderState p_418022_) {
         if (this.elementStates == null) {
            this.elementStates = new ArrayList<>();
         }

         this.elementStates.add(p_418022_);
      }

      public void submitGlyph(GuiElementRenderState p_427510_) {
         if (this.glyphStates == null) {
            this.glyphStates = new ArrayList<>();
         }

         this.glyphStates.add(p_427510_);
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static enum TraverseRange {
      ALL,
      BEFORE_BLUR,
      AFTER_BLUR;
   }
}
