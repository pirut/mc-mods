package net.minecraft.client.gui.render.state;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.item.TrackingItemStackRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix3x2f;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public final class GuiItemRenderState implements ScreenArea {
   private final String name;
   private final Matrix3x2f pose;
   private final TrackingItemStackRenderState itemStackRenderState;
   private final int x;
   private final int y;
   @Nullable
   private final ScreenRectangle scissorArea;
   @Nullable
   private final ScreenRectangle oversizedItemBounds;
   @Nullable
   private final ScreenRectangle bounds;

   public GuiItemRenderState(
      String name, Matrix3x2f pose, TrackingItemStackRenderState itemStackRenderState, int x, int y, @Nullable ScreenRectangle scissorArea
   ) {
      this.name = name;
      this.pose = pose;
      this.itemStackRenderState = itemStackRenderState;
      this.x = x;
      this.y = y;
      this.scissorArea = scissorArea;
      this.oversizedItemBounds = this.itemStackRenderState().isOversizedInGui() ? this.calculateOversizedItemBounds() : null;
      this.bounds = this.calculateBounds(this.oversizedItemBounds != null ? this.oversizedItemBounds : new ScreenRectangle(this.x, this.y, 16, 16));
   }

   @Nullable
   private ScreenRectangle calculateOversizedItemBounds() {
      AABB aABB = this.itemStackRenderState.getModelBoundingBox();
      int i = Mth.ceil(aABB.getXsize() * 16.0);
      int j = Mth.ceil(aABB.getYsize() * 16.0);
      if (i <= 16 && j <= 16) {
         return null;
      } else {
         float f = (float)(aABB.minX * 16.0);
         float g = (float)(aABB.maxY * 16.0);
         int k = Mth.floor(f);
         int l = Mth.floor(g);
         int m = this.x + k + 8;
         int n = this.y - l + 8;
         return new ScreenRectangle(m, n, i, j);
      }
   }

   @Nullable
   private ScreenRectangle calculateBounds(ScreenRectangle rectangle) {
      ScreenRectangle screenRectangle = rectangle.transformMaxBounds(this.pose);
      return this.scissorArea != null ? this.scissorArea.intersection(screenRectangle) : screenRectangle;
   }

   public String name() {
      return this.name;
   }

   public Matrix3x2f pose() {
      return this.pose;
   }

   public TrackingItemStackRenderState itemStackRenderState() {
      return this.itemStackRenderState;
   }

   public int x() {
      return this.x;
   }

   public int y() {
      return this.y;
   }

   @Nullable
   public ScreenRectangle scissorArea() {
      return this.scissorArea;
   }

   @Nullable
   public ScreenRectangle oversizedItemBounds() {
      return this.oversizedItemBounds;
   }

   @Nullable
   @Override
   public ScreenRectangle bounds() {
      return this.bounds;
   }
}
