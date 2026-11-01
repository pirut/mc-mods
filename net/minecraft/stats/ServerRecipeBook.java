package net.minecraft.stats;

import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.ResourceLocationException;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.protocol.game.ClientboundRecipePacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import org.slf4j.Logger;

public class ServerRecipeBook extends RecipeBook {
   public static final String RECIPE_BOOK_TAG = "recipeBook";
   private static final Logger LOGGER = LogUtils.getLogger();

   public int addRecipes(Collection<RecipeHolder<?>> p_12792_, ServerPlayer p_12793_) {
      List<ResourceLocation> $$2 = Lists.newArrayList();
      int $$3 = 0;

      for (RecipeHolder<?> $$4 : p_12792_) {
         ResourceLocation $$5 = $$4.id();
         if (!this.known.contains($$5) && !$$4.value().isSpecial()) {
            this.add($$5);
            this.addHighlight($$5);
            $$2.add($$5);
            CriteriaTriggers.RECIPE_UNLOCKED.trigger(p_12793_, $$4);
            $$3++;
         }
      }

      if ($$2.size() > 0) {
         this.sendRecipes(ClientboundRecipePacket.State.ADD, p_12793_, $$2);
      }

      return $$3;
   }

   public int removeRecipes(Collection<RecipeHolder<?>> p_12807_, ServerPlayer p_12808_) {
      List<ResourceLocation> $$2 = Lists.newArrayList();
      int $$3 = 0;

      for (RecipeHolder<?> $$4 : p_12807_) {
         ResourceLocation $$5 = $$4.id();
         if (this.known.contains($$5)) {
            this.remove($$5);
            $$2.add($$5);
            $$3++;
         }
      }

      this.sendRecipes(ClientboundRecipePacket.State.REMOVE, p_12808_, $$2);
      return $$3;
   }

   private void sendRecipes(ClientboundRecipePacket.State p_12802_, ServerPlayer p_12803_, List<ResourceLocation> p_12804_) {
      p_12803_.connection.send(new ClientboundRecipePacket(p_12802_, p_12804_, Collections.emptyList(), this.getBookSettings()));
   }

   public CompoundTag toNbt() {
      CompoundTag $$0 = new CompoundTag();
      this.getBookSettings().write($$0);
      ListTag $$1 = new ListTag();

      for (ResourceLocation $$2 : this.known) {
         $$1.add(StringTag.valueOf($$2.toString()));
      }

      $$0.put("recipes", $$1);
      ListTag $$3 = new ListTag();

      for (ResourceLocation $$4 : this.highlight) {
         $$3.add(StringTag.valueOf($$4.toString()));
      }

      $$0.put("toBeDisplayed", $$3);
      return $$0;
   }

   public void fromNbt(CompoundTag p_12795_, RecipeManager p_12796_) {
      this.setBookSettings(RecipeBookSettings.read(p_12795_));
      ListTag $$2 = p_12795_.getList("recipes", 8);
      this.loadRecipes($$2, this::add, p_12796_);
      ListTag $$3 = p_12795_.getList("toBeDisplayed", 8);
      this.loadRecipes($$3, this::addHighlight, p_12796_);
   }

   private void loadRecipes(ListTag p_12798_, Consumer<RecipeHolder<?>> p_12799_, RecipeManager p_12800_) {
      for (int $$3 = 0; $$3 < p_12798_.size(); $$3++) {
         String $$4 = p_12798_.getString($$3);

         try {
            ResourceLocation $$5 = ResourceLocation.parse($$4);
            Optional<RecipeHolder<?>> $$6 = p_12800_.byKey($$5);
            if ($$6.isEmpty()) {
               LOGGER.error("Tried to load unrecognized recipe: {} removed now.", $$5);
            } else {
               p_12799_.accept($$6.get());
            }
         } catch (ResourceLocationException var8) {
            LOGGER.error("Tried to load improperly formatted recipe: {} removed now.", $$4);
         }
      }
   }

   public void sendInitialRecipeBook(ServerPlayer p_12790_) {
      p_12790_.connection.send(new ClientboundRecipePacket(ClientboundRecipePacket.State.INIT, this.known, this.highlight, this.getBookSettings()));
   }
}
