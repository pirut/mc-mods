package net.neoforged.neoforge.common.util;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;

public final class Size2i {
   public final int width;
   public final int height;

   public Size2i(int width, int height) {
      Preconditions.checkArgument(width >= 0, "width must be greater or equal 0");
      Preconditions.checkArgument(height >= 0, "height must be greater or equal 0");
      this.width = width;
      this.height = height;
   }

   @Override
   public boolean equals(Object obj) {
      return !(obj instanceof Size2i other) ? false : this.width == other.width && this.height == other.height;
   }

   @Override
   public int hashCode() {
      int hash = 17;
      hash = hash * 31 + this.width;
      return hash * 31 + this.height;
   }

   @Override
   public String toString() {
      return MoreObjects.toStringHelper(this).add("width", this.width).add("height", this.height).toString();
   }
}
