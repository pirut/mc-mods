package net.minecraft.network.chat.contents.data;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.stream.Stream;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.coordinates.Coordinates;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;

public record BlockDataSource(String posPattern, @Nullable Coordinates compiledPos) implements DataSource {
   public static final MapCodec<BlockDataSource> MAP_CODEC = RecordCodecBuilder.mapCodec(
      p_443129_ -> p_443129_.group(Codec.STRING.fieldOf("block").forGetter(BlockDataSource::posPattern)).apply(p_443129_, BlockDataSource::new)
   );

   public BlockDataSource(String p_442777_) {
      this(p_442777_, compilePos(p_442777_));
   }

   @Nullable
   private static Coordinates compilePos(String p_442554_) {
      try {
         return BlockPosArgument.blockPos().parse(new StringReader(p_442554_));
      } catch (CommandSyntaxException var2) {
         return null;
      }
   }

   @Override
   public Stream<CompoundTag> getData(CommandSourceStack p_443087_) {
      if (this.compiledPos != null) {
         ServerLevel serverlevel = p_443087_.getLevel();
         BlockPos blockpos = this.compiledPos.getBlockPos(p_443087_);
         if (serverlevel.isLoaded(blockpos)) {
            BlockEntity blockentity = serverlevel.getBlockEntity(blockpos);
            if (blockentity != null) {
               return Stream.of(blockentity.saveWithFullMetadata(p_443087_.registryAccess()));
            }
         }
      }

      return Stream.empty();
   }

   @Override
   public MapCodec<BlockDataSource> codec() {
      return MAP_CODEC;
   }

   @Override
   public String toString() {
      return "block=" + this.posPattern;
   }

   @Override
   public boolean equals(Object p_443466_) {
      return this == p_443466_ ? true : p_443466_ instanceof BlockDataSource blockdatasource && this.posPattern.equals(blockdatasource.posPattern);
   }

   @Override
   public int hashCode() {
      return this.posPattern.hashCode();
   }
}
