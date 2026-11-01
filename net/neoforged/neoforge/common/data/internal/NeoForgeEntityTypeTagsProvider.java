package net.neoforged.neoforge.common.data.internal;

import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.EntityTypeTagsProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public class NeoForgeEntityTypeTagsProvider extends EntityTypeTagsProvider {
   public NeoForgeEntityTypeTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, ExistingFileHelper existingFileHelper) {
      super(output, lookupProvider, "neoforge", existingFileHelper);
   }

   @Override
   public void addTags(HolderLookup.Provider lookupProvider) {
      this.tag(Tags.EntityTypes.BOSSES).add(EntityType.ENDER_DRAGON, EntityType.WITHER);
      this.tag(Tags.EntityTypes.MINECARTS)
         .add(
            EntityType.MINECART,
            EntityType.CHEST_MINECART,
            EntityType.FURNACE_MINECART,
            EntityType.HOPPER_MINECART,
            EntityType.SPAWNER_MINECART,
            EntityType.TNT_MINECART,
            EntityType.COMMAND_BLOCK_MINECART
         );
      this.tag(Tags.EntityTypes.BOATS).add(EntityType.BOAT, EntityType.CHEST_BOAT);
      this.tag(Tags.EntityTypes.CAPTURING_NOT_SUPPORTED);
      this.tag(Tags.EntityTypes.TELEPORTING_NOT_SUPPORTED);
      this.tag(Tags.EntityTypes.BOSSES).addOptionalTag(ResourceLocation.fromNamespaceAndPath("forge", "bosses"));
      this.tag(Tags.EntityTypes.MINECARTS).addOptionalTag(ResourceLocation.fromNamespaceAndPath("forge", "minecarts"));
      this.tag(Tags.EntityTypes.BOATS).addOptionalTag(ResourceLocation.fromNamespaceAndPath("forge", "boats"));
   }
}
