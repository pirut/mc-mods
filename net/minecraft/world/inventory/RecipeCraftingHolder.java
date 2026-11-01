package net.minecraft.world.inventory;

import java.util.Collections;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.gamerules.GameRules;
import org.jspecify.annotations.Nullable;

public interface RecipeCraftingHolder {
   void setRecipeUsed(@Nullable RecipeHolder<?> var1);

   @Nullable
   RecipeHolder<?> getRecipeUsed();

   default void awardUsedRecipes(Player p_300892_, List<ItemStack> p_301308_) {
      RecipeHolder<?> recipeholder = this.getRecipeUsed();
      if (recipeholder != null) {
         p_300892_.triggerRecipeCrafted(recipeholder, p_301308_);
         if (!recipeholder.value().isSpecial()) {
            p_300892_.awardRecipes(Collections.singleton(recipeholder));
            this.setRecipeUsed(null);
         }
      }
   }

   default boolean setRecipeUsed(ServerPlayer p_301242_, RecipeHolder<?> p_301296_) {
      if (!p_301296_.value().isSpecial()
         && p_301242_.level().getGameRules().get(GameRules.LIMITED_CRAFTING)
         && !p_301242_.getRecipeBook().contains(p_301296_.id())) {
         return false;
      } else {
         this.setRecipeUsed(p_301296_);
         return true;
      }
   }
}
