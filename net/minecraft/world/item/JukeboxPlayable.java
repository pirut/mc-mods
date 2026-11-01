package net.minecraft.world.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.stats.Stats;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.component.TooltipProvider;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.JukeboxBlock;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;

public record JukeboxPlayable(EitherHolder<JukeboxSong> song, boolean showInTooltip) implements TooltipProvider {
   public static final Codec<JukeboxPlayable> CODEC = RecordCodecBuilder.create(
      p_350579_ -> p_350579_.group(
            EitherHolder.codec(Registries.JUKEBOX_SONG, JukeboxSong.CODEC).fieldOf("song").forGetter(JukeboxPlayable::song),
            Codec.BOOL.optionalFieldOf("show_in_tooltip", true).forGetter(JukeboxPlayable::showInTooltip)
         )
         .apply(p_350579_, JukeboxPlayable::new)
   );
   public static final StreamCodec<RegistryFriendlyByteBuf, JukeboxPlayable> STREAM_CODEC = StreamCodec.composite(
      EitherHolder.streamCodec(Registries.JUKEBOX_SONG, JukeboxSong.STREAM_CODEC),
      JukeboxPlayable::song,
      ByteBufCodecs.BOOL,
      JukeboxPlayable::showInTooltip,
      JukeboxPlayable::new
   );

   @Override
   public void addToTooltip(Item.TooltipContext p_350489_, Consumer<Component> p_350818_, TooltipFlag p_350358_) {
      HolderLookup.Provider $$3 = p_350489_.registries();
      if (this.showInTooltip && $$3 != null) {
         this.song.unwrap($$3).ifPresent(p_350916_ -> {
            MutableComponent $$2 = p_350916_.value().description().copy();
            ComponentUtils.mergeStyles($$2, Style.EMPTY.withColor(ChatFormatting.GRAY));
            p_350818_.accept($$2);
         });
      }
   }

   public JukeboxPlayable withTooltip(boolean p_350922_) {
      return new JukeboxPlayable(this.song, p_350922_);
   }

   public static ItemInteractionResult tryInsertIntoJukebox(Level p_350560_, BlockPos p_350567_, ItemStack p_350531_, Player p_350807_) {
      JukeboxPlayable $$4 = p_350531_.get(DataComponents.JUKEBOX_PLAYABLE);
      if ($$4 == null) {
         return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
      } else {
         BlockState $$5 = p_350560_.getBlockState(p_350567_);
         if ($$5.is(Blocks.JUKEBOX) && !$$5.getValue(JukeboxBlock.HAS_RECORD)) {
            if (!p_350560_.isClientSide) {
               ItemStack $$6 = p_350531_.consumeAndReturn(1, p_350807_);
               if (p_350560_.getBlockEntity(p_350567_) instanceof JukeboxBlockEntity $$7) {
                  $$7.setTheItem($$6);
                  p_350560_.gameEvent(GameEvent.BLOCK_CHANGE, p_350567_, GameEvent.Context.of(p_350807_, $$5));
               }

               p_350807_.awardStat(Stats.PLAY_RECORD);
            }

            return ItemInteractionResult.sidedSuccess(p_350560_.isClientSide);
         } else {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
         }
      }
   }
}
