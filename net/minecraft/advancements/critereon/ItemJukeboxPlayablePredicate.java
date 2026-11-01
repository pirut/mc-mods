package net.minecraft.advancements.critereon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.JukeboxPlayable;
import net.minecraft.world.item.JukeboxSong;

public record ItemJukeboxPlayablePredicate(Optional<HolderSet<JukeboxSong>> song) implements SingleComponentItemPredicate<JukeboxPlayable> {
   public static final Codec<ItemJukeboxPlayablePredicate> CODEC = RecordCodecBuilder.create(
      p_350276_ -> p_350276_.group(
            RegistryCodecs.homogeneousList(Registries.JUKEBOX_SONG).optionalFieldOf("song").forGetter(ItemJukeboxPlayablePredicate::song)
         )
         .apply(p_350276_, ItemJukeboxPlayablePredicate::new)
   );

   @Override
   public DataComponentType<JukeboxPlayable> componentType() {
      return DataComponents.JUKEBOX_PLAYABLE;
   }

   public boolean matches(ItemStack p_350458_, JukeboxPlayable p_350743_) {
      if (!this.song.isPresent()) {
         return true;
      } else {
         boolean $$2 = false;

         for (Holder<JukeboxSong> $$3 : this.song.get()) {
            Optional<ResourceKey<JukeboxSong>> $$4 = $$3.unwrapKey();
            if (!$$4.isEmpty() && $$4.get() == p_350743_.song().key()) {
               $$2 = true;
               break;
            }
         }

         return $$2;
      }
   }

   public static ItemJukeboxPlayablePredicate any() {
      return new ItemJukeboxPlayablePredicate(Optional.empty());
   }
}
