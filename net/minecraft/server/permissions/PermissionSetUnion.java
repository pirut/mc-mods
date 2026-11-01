package net.minecraft.server.permissions;

import com.google.common.annotations.VisibleForTesting;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.ReferenceArraySet;
import it.unimi.dsi.fastutil.objects.ReferenceSet;

public class PermissionSetUnion implements PermissionSet {
   private final ReferenceSet<PermissionSet> permissions = new ReferenceArraySet();

   PermissionSetUnion(PermissionSet p_470685_, PermissionSet p_470546_) {
      this.permissions.add(p_470685_);
      this.permissions.add(p_470546_);
      this.ensureNoUnionsWithinUnions();
   }

   private PermissionSetUnion(ReferenceSet<PermissionSet> p_470564_, PermissionSet p_470678_) {
      this.permissions.addAll(p_470564_);
      this.permissions.add(p_470678_);
      this.ensureNoUnionsWithinUnions();
   }

   private PermissionSetUnion(ReferenceSet<PermissionSet> p_470728_, ReferenceSet<PermissionSet> p_470747_) {
      this.permissions.addAll(p_470728_);
      this.permissions.addAll(p_470747_);
      this.ensureNoUnionsWithinUnions();
   }

   @Override
   public boolean hasPermission(Permission p_470836_) {
      ObjectIterator var2 = this.permissions.iterator();

      while (var2.hasNext()) {
         PermissionSet permissionset = (PermissionSet)var2.next();
         if (permissionset.hasPermission(p_470836_)) {
            return true;
         }
      }

      return false;
   }

   @Override
   public PermissionSet union(PermissionSet p_470555_) {
      return p_470555_ instanceof PermissionSetUnion permissionsetunion
         ? new PermissionSetUnion(this.permissions, permissionsetunion.permissions)
         : new PermissionSetUnion(this.permissions, p_470555_);
   }

   @VisibleForTesting
   public ReferenceSet<PermissionSet> getPermissions() {
      return new ReferenceArraySet(this.permissions);
   }

   private void ensureNoUnionsWithinUnions() {
      ObjectIterator var1 = this.permissions.iterator();

      while (var1.hasNext()) {
         PermissionSet permissionset = (PermissionSet)var1.next();
         if (permissionset instanceof PermissionSetUnion) {
            throw new IllegalArgumentException("Cannot have PermissionSetUnion within another PermissionSetUnion");
         }
      }
   }
}
