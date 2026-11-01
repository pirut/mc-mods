package net.neoforged.neoforge.event;

import it.unimi.dsi.fastutil.objects.ObjectSortedSet;
import it.unimi.dsi.fastutil.objects.ObjectSortedSets;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;
import net.neoforged.neoforge.common.util.InsertableLinkedOpenCustomHashSet;
import org.jetbrains.annotations.ApiStatus.Internal;

public final class BuildCreativeModeTabContentsEvent extends Event implements IModBusEvent, CreativeModeTab.Output {
   private final CreativeModeTab tab;
   private final CreativeModeTab.ItemDisplayParameters parameters;
   private final InsertableLinkedOpenCustomHashSet<ItemStack> parentEntries;
   private final InsertableLinkedOpenCustomHashSet<ItemStack> searchEntries;
   private final ResourceKey<CreativeModeTab> tabKey;

   @Internal
   public BuildCreativeModeTabContentsEvent(
      CreativeModeTab tab,
      ResourceKey<CreativeModeTab> tabKey,
      CreativeModeTab.ItemDisplayParameters parameters,
      InsertableLinkedOpenCustomHashSet<ItemStack> parentEntries,
      InsertableLinkedOpenCustomHashSet<ItemStack> searchEntries
   ) {
      this.tab = tab;
      this.tabKey = tabKey;
      this.parameters = parameters;
      this.parentEntries = parentEntries;
      this.searchEntries = searchEntries;
   }

   public CreativeModeTab getTab() {
      return this.tab;
   }

   public ResourceKey<CreativeModeTab> getTabKey() {
      return this.tabKey;
   }

   public FeatureFlagSet getFlags() {
      return this.parameters.enabledFeatures();
   }

   public CreativeModeTab.ItemDisplayParameters getParameters() {
      return this.parameters;
   }

   public boolean hasPermissions() {
      return this.parameters.hasPermissions();
   }

   public ObjectSortedSet<ItemStack> getParentEntries() {
      return ObjectSortedSets.unmodifiable(this.parentEntries);
   }

   public ObjectSortedSet<ItemStack> getSearchEntries() {
      return ObjectSortedSets.unmodifiable(this.searchEntries);
   }

   @Override
   public void accept(ItemStack newEntry, CreativeModeTab.TabVisibility visibility) {
      assertStackCount(newEntry);
      if (isParentTab(visibility)) {
         this.assertNewEntryDoesNotAlreadyExists(this.parentEntries, newEntry);
         this.parentEntries.add(newEntry);
      }

      if (isSearchTab(visibility)) {
         this.assertNewEntryDoesNotAlreadyExists(this.searchEntries, newEntry);
         this.searchEntries.add(newEntry);
      }
   }

   public void insertAfter(ItemStack existingEntry, ItemStack newEntry, CreativeModeTab.TabVisibility visibility) {
      assertStackCount(newEntry);
      if (isParentTab(visibility)) {
         this.assertTargetExists(this.parentEntries, existingEntry);
         this.assertNewEntryDoesNotAlreadyExists(this.parentEntries, newEntry);
         this.parentEntries.addAfter(existingEntry, newEntry);
      }

      if (isSearchTab(visibility)) {
         this.assertTargetExists(this.searchEntries, existingEntry);
         this.assertNewEntryDoesNotAlreadyExists(this.searchEntries, newEntry);
         this.searchEntries.addAfter(existingEntry, newEntry);
      }
   }

   public void insertBefore(ItemStack existingEntry, ItemStack newEntry, CreativeModeTab.TabVisibility visibility) {
      assertStackCount(newEntry);
      if (isParentTab(visibility)) {
         this.assertTargetExists(this.parentEntries, existingEntry);
         this.assertNewEntryDoesNotAlreadyExists(this.parentEntries, newEntry);
         this.parentEntries.addBefore(existingEntry, newEntry);
      }

      if (isSearchTab(visibility)) {
         this.assertTargetExists(this.searchEntries, existingEntry);
         this.assertNewEntryDoesNotAlreadyExists(this.searchEntries, newEntry);
         this.searchEntries.addBefore(existingEntry, newEntry);
      }
   }

   public void insertFirst(ItemStack newEntry, CreativeModeTab.TabVisibility visibility) {
      assertStackCount(newEntry);
      if (isParentTab(visibility)) {
         this.assertNewEntryDoesNotAlreadyExists(this.parentEntries, newEntry);
         this.parentEntries.addFirst(newEntry);
      }

      if (isSearchTab(visibility)) {
         this.assertNewEntryDoesNotAlreadyExists(this.searchEntries, newEntry);
         this.searchEntries.addFirst(newEntry);
      }
   }

   public void remove(ItemStack existingEntry, CreativeModeTab.TabVisibility visibility) {
      if (isParentTab(visibility)) {
         this.parentEntries.remove(existingEntry);
      }

      if (isSearchTab(visibility)) {
         this.searchEntries.remove(existingEntry);
      }
   }

   static boolean isParentTab(CreativeModeTab.TabVisibility visibility) {
      return visibility == CreativeModeTab.TabVisibility.PARENT_TAB_ONLY || visibility == CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS;
   }

   static boolean isSearchTab(CreativeModeTab.TabVisibility visibility) {
      return visibility == CreativeModeTab.TabVisibility.SEARCH_TAB_ONLY || visibility == CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS;
   }

   private void assertTargetExists(InsertableLinkedOpenCustomHashSet<ItemStack> setToCheck, ItemStack existingEntry) {
      if (!setToCheck.contains(existingEntry)) {
         throw new IllegalArgumentException("Itemstack " + existingEntry + " does not exist in tab's list");
      }
   }

   private void assertNewEntryDoesNotAlreadyExists(InsertableLinkedOpenCustomHashSet<ItemStack> setToCheck, ItemStack newEntry) {
      if (setToCheck.contains(newEntry)) {
         throw new IllegalArgumentException("Itemstack " + newEntry + " already exists in the tab's list");
      }
   }

   private static void assertStackCount(ItemStack newEntry) {
      if (newEntry.getCount() != 1) {
         throw new IllegalArgumentException("The stack count must be 1 for " + newEntry);
      }
   }
}
