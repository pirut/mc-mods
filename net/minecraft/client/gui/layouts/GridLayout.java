package net.minecraft.client.gui.layouts;

import com.mojang.math.Divisor;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.Util;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class GridLayout extends AbstractLayout {
   private final List<LayoutElement> children = new ArrayList<>();
   private final List<GridLayout.CellInhabitant> cellInhabitants = new ArrayList<>();
   private final LayoutSettings defaultCellSettings = LayoutSettings.defaults();
   private int rowSpacing = 0;
   private int columnSpacing = 0;

   public GridLayout() {
      this(0, 0);
   }

   public GridLayout(int p_265045_, int p_265035_) {
      super(p_265045_, p_265035_, 0, 0);
   }

   @Override
   public void arrangeElements() {
      super.arrangeElements();
      int $$0 = 0;
      int $$1 = 0;

      for (GridLayout.CellInhabitant $$2 : this.cellInhabitants) {
         $$0 = Math.max($$2.getLastOccupiedRow(), $$0);
         $$1 = Math.max($$2.getLastOccupiedColumn(), $$1);
      }

      int[] $$3 = new int[$$1 + 1];
      int[] $$4 = new int[$$0 + 1];

      for (GridLayout.CellInhabitant $$5 : this.cellInhabitants) {
         int $$6 = $$5.getHeight() - ($$5.occupiedRows - 1) * this.rowSpacing;
         Divisor $$7 = new Divisor($$6, $$5.occupiedRows);

         for (int $$8 = $$5.row; $$8 <= $$5.getLastOccupiedRow(); $$8++) {
            $$4[$$8] = Math.max($$4[$$8], $$7.nextInt());
         }

         int $$9 = $$5.getWidth() - ($$5.occupiedColumns - 1) * this.columnSpacing;
         Divisor $$10 = new Divisor($$9, $$5.occupiedColumns);

         for (int $$11 = $$5.column; $$11 <= $$5.getLastOccupiedColumn(); $$11++) {
            $$3[$$11] = Math.max($$3[$$11], $$10.nextInt());
         }
      }

      int[] $$12 = new int[$$1 + 1];
      int[] $$13 = new int[$$0 + 1];
      $$12[0] = 0;

      for (int $$14 = 1; $$14 <= $$1; $$14++) {
         $$12[$$14] = $$12[$$14 - 1] + $$3[$$14 - 1] + this.columnSpacing;
      }

      $$13[0] = 0;

      for (int $$15 = 1; $$15 <= $$0; $$15++) {
         $$13[$$15] = $$13[$$15 - 1] + $$4[$$15 - 1] + this.rowSpacing;
      }

      for (GridLayout.CellInhabitant $$16 : this.cellInhabitants) {
         int $$17 = 0;

         for (int $$18 = $$16.column; $$18 <= $$16.getLastOccupiedColumn(); $$18++) {
            $$17 += $$3[$$18];
         }

         $$17 += this.columnSpacing * ($$16.occupiedColumns - 1);
         $$16.setX(this.getX() + $$12[$$16.column], $$17);
         int $$19 = 0;

         for (int $$20 = $$16.row; $$20 <= $$16.getLastOccupiedRow(); $$20++) {
            $$19 += $$4[$$20];
         }

         $$19 += this.rowSpacing * ($$16.occupiedRows - 1);
         $$16.setY(this.getY() + $$13[$$16.row], $$19);
      }

      this.width = $$12[$$1] + $$3[$$1];
      this.height = $$13[$$0] + $$4[$$0];
   }

   public <T extends LayoutElement> T addChild(T p_265485_, int p_265720_, int p_265679_) {
      return this.addChild(p_265485_, p_265720_, p_265679_, this.newCellSettings());
   }

   public <T extends LayoutElement> T addChild(T p_265061_, int p_265080_, int p_265105_, LayoutSettings p_265057_) {
      return this.addChild(p_265061_, p_265080_, p_265105_, 1, 1, p_265057_);
   }

   public <T extends LayoutElement> T addChild(T p_294625_, int p_294408_, int p_294646_, Consumer<LayoutSettings> p_295693_) {
      return this.addChild(p_294625_, p_294408_, p_294646_, 1, 1, Util.make(this.newCellSettings(), p_295693_));
   }

   public <T extends LayoutElement> T addChild(T p_265590_, int p_265556_, int p_265323_, int p_265531_, int p_265352_) {
      return this.addChild(p_265590_, p_265556_, p_265323_, p_265531_, p_265352_, this.newCellSettings());
   }

   public <T extends LayoutElement> T addChild(T p_265031_, int p_265582_, int p_265782_, int p_265612_, int p_265448_, LayoutSettings p_265579_) {
      if (p_265612_ < 1) {
         throw new IllegalArgumentException("Occupied rows must be at least 1");
      } else if (p_265448_ < 1) {
         throw new IllegalArgumentException("Occupied columns must be at least 1");
      } else {
         this.cellInhabitants.add(new GridLayout.CellInhabitant(p_265031_, p_265582_, p_265782_, p_265612_, p_265448_, p_265579_));
         this.children.add(p_265031_);
         return p_265031_;
      }
   }

   public <T extends LayoutElement> T addChild(T p_295505_, int p_295555_, int p_295800_, int p_295919_, int p_295523_, Consumer<LayoutSettings> p_296424_) {
      return this.addChild(p_295505_, p_295555_, p_295800_, p_295919_, p_295523_, Util.make(this.newCellSettings(), p_296424_));
   }

   public GridLayout columnSpacing(int p_268135_) {
      this.columnSpacing = p_268135_;
      return this;
   }

   public GridLayout rowSpacing(int p_268237_) {
      this.rowSpacing = p_268237_;
      return this;
   }

   public GridLayout spacing(int p_268351_) {
      return this.columnSpacing(p_268351_).rowSpacing(p_268351_);
   }

   @Override
   public void visitChildren(Consumer<LayoutElement> p_265389_) {
      this.children.forEach(p_265389_);
   }

   public LayoutSettings newCellSettings() {
      return this.defaultCellSettings.copy();
   }

   public LayoutSettings defaultCellSetting() {
      return this.defaultCellSettings;
   }

   public GridLayout.RowHelper createRowHelper(int p_265327_) {
      return new GridLayout.RowHelper(p_265327_);
   }

   @OnlyIn(Dist.CLIENT)
   static class CellInhabitant extends AbstractLayout.AbstractChildWrapper {
      final int row;
      final int column;
      final int occupiedRows;
      final int occupiedColumns;

      CellInhabitant(LayoutElement p_265063_, int p_265675_, int p_265198_, int p_265625_, int p_265517_, LayoutSettings p_265036_) {
         super(p_265063_, p_265036_.getExposed());
         this.row = p_265675_;
         this.column = p_265198_;
         this.occupiedRows = p_265625_;
         this.occupiedColumns = p_265517_;
      }

      public int getLastOccupiedRow() {
         return this.row + this.occupiedRows - 1;
      }

      public int getLastOccupiedColumn() {
         return this.column + this.occupiedColumns - 1;
      }
   }

   @OnlyIn(Dist.CLIENT)
   public final class RowHelper {
      private final int columns;
      private int index;

      RowHelper(int p_265633_) {
         this.columns = p_265633_;
      }

      public <T extends LayoutElement> T addChild(T p_265455_) {
         return this.addChild(p_265455_, 1);
      }

      public <T extends LayoutElement> T addChild(T p_265413_, int p_265491_) {
         return this.addChild(p_265413_, p_265491_, this.defaultCellSetting());
      }

      public <T extends LayoutElement> T addChild(T p_265411_, LayoutSettings p_265755_) {
         return this.addChild(p_265411_, 1, p_265755_);
      }

      public <T extends LayoutElement> T addChild(T p_265200_, int p_265044_, LayoutSettings p_265797_) {
         int $$3 = this.index / this.columns;
         int $$4 = this.index % this.columns;
         if ($$4 + p_265044_ > this.columns) {
            $$3++;
            $$4 = 0;
            this.index = Mth.roundToward(this.index, this.columns);
         }

         this.index += p_265044_;
         return GridLayout.this.addChild(p_265200_, $$3, $$4, 1, p_265044_, p_265797_);
      }

      public GridLayout getGrid() {
         return GridLayout.this;
      }

      public LayoutSettings newCellSettings() {
         return GridLayout.this.newCellSettings();
      }

      public LayoutSettings defaultCellSetting() {
         return GridLayout.this.defaultCellSetting();
      }
   }
}
