package net.minecraft.world.item.trading;

import com.mojang.serialization.Codec;
import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

public class MerchantOffers extends ArrayList<MerchantOffer> {
   public static final Codec<MerchantOffers> CODEC = MerchantOffer.CODEC.listOf().fieldOf("Recipes").xmap(MerchantOffers::new, Function.identity()).codec();
   public static final StreamCodec<RegistryFriendlyByteBuf, MerchantOffers> STREAM_CODEC = MerchantOffer.STREAM_CODEC
      .apply(ByteBufCodecs.collection(MerchantOffers::new));

   public MerchantOffers() {
   }

   private MerchantOffers(int p_220323_) {
      super(p_220323_);
   }

   private MerchantOffers(Collection<MerchantOffer> p_323676_) {
      super(p_323676_);
   }

   @Nullable
   public MerchantOffer getRecipeFor(ItemStack p_45390_, ItemStack p_45391_, int p_45392_) {
      if (p_45392_ > 0 && p_45392_ < this.size()) {
         MerchantOffer $$3 = this.get(p_45392_);
         return $$3.satisfiedBy(p_45390_, p_45391_) ? $$3 : null;
      } else {
         for (int $$4 = 0; $$4 < this.size(); $$4++) {
            MerchantOffer $$5 = this.get($$4);
            if ($$5.satisfiedBy(p_45390_, p_45391_)) {
               return $$5;
            }
         }

         return null;
      }
   }

   public MerchantOffers copy() {
      MerchantOffers $$0 = new MerchantOffers(this.size());

      for (MerchantOffer $$1 : this) {
         $$0.add($$1.copy());
      }

      return $$0;
   }
}
