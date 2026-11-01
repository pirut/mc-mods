package net.neoforged.neoforge.server.permission.nodes;

import java.util.Objects;

public final class PermissionType<T> {
   private final Class<T> typeToken;
   private final String typeName;

   PermissionType(Class<T> typeToken, String typeName) {
      this.typeToken = typeToken;
      this.typeName = typeName;
   }

   public Class<T> typeToken() {
      return this.typeToken;
   }

   public String typeName() {
      return this.typeName;
   }

   @Override
   public boolean equals(Object obj) {
      if (obj == this) {
         return true;
      } else {
         return !(obj instanceof PermissionType otherType)
            ? false
            : Objects.equals(this.typeToken, otherType.typeToken) && Objects.equals(this.typeName, otherType.typeName);
      }
   }

   @Override
   public int hashCode() {
      return Objects.hash(this.typeToken, this.typeName);
   }

   @Override
   public String toString() {
      return "PermissionType[typeToken=" + this.typeToken + ", typeName=" + this.typeName + "]";
   }
}
