package net.minecraft.network.chat.contents;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.commands.arguments.selector.EntitySelectorParser;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.numbers.StyledFormat;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ReadOnlyScoreInfo;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.Scoreboard;

public class ScoreContents implements ComponentContents {
   public static final MapCodec<ScoreContents> INNER_CODEC = RecordCodecBuilder.mapCodec(
      p_304795_ -> p_304795_.group(
            Codec.STRING.fieldOf("name").forGetter(ScoreContents::getName), Codec.STRING.fieldOf("objective").forGetter(ScoreContents::getObjective)
         )
         .apply(p_304795_, ScoreContents::new)
   );
   public static final MapCodec<ScoreContents> CODEC = INNER_CODEC.fieldOf("score");
   public static final ComponentContents.Type<ScoreContents> TYPE = new ComponentContents.Type<>(CODEC, "score");
   private final String name;
   @Nullable
   private final EntitySelector selector;
   private final String objective;

   @Nullable
   private static EntitySelector parseSelector(String p_237448_) {
      try {
         return new EntitySelectorParser(new StringReader(p_237448_), true).parse();
      } catch (CommandSyntaxException var2) {
         return null;
      }
   }

   public ScoreContents(String p_237438_, String p_237439_) {
      this.name = p_237438_;
      this.selector = parseSelector(p_237438_);
      this.objective = p_237439_;
   }

   @Override
   public ComponentContents.Type<?> type() {
      return TYPE;
   }

   public String getName() {
      return this.name;
   }

   @Nullable
   public EntitySelector getSelector() {
      return this.selector;
   }

   public String getObjective() {
      return this.objective;
   }

   private ScoreHolder findTargetName(CommandSourceStack p_237442_) throws CommandSyntaxException {
      if (this.selector != null) {
         List<? extends Entity> $$1 = this.selector.findEntities(p_237442_);
         if (!$$1.isEmpty()) {
            if ($$1.size() != 1) {
               throw EntityArgument.ERROR_NOT_SINGLE_ENTITY.create();
            }

            return $$1.get(0);
         }
      }

      return ScoreHolder.forNameOnly(this.name);
   }

   private MutableComponent getScore(ScoreHolder p_313863_, CommandSourceStack p_237451_) {
      MinecraftServer $$2 = p_237451_.getServer();
      if ($$2 != null) {
         Scoreboard $$3 = $$2.getScoreboard();
         Objective $$4 = $$3.getObjective(this.objective);
         if ($$4 != null) {
            ReadOnlyScoreInfo $$5 = $$3.getPlayerScoreInfo(p_313863_, $$4);
            if ($$5 != null) {
               return $$5.formatValue($$4.numberFormatOrDefault(StyledFormat.NO_STYLE));
            }
         }
      }

      return Component.empty();
   }

   @Override
   public MutableComponent resolve(@Nullable CommandSourceStack p_237444_, @Nullable Entity p_237445_, int p_237446_) throws CommandSyntaxException {
      if (p_237444_ == null) {
         return Component.empty();
      } else {
         ScoreHolder $$3 = this.findTargetName(p_237444_);
         ScoreHolder $$4 = (ScoreHolder)(p_237445_ != null && $$3.equals(ScoreHolder.WILDCARD) ? p_237445_ : $$3);
         return this.getScore($$4, p_237444_);
      }
   }

   @Override
   public boolean equals(Object p_237455_) {
      return this == p_237455_ ? true : p_237455_ instanceof ScoreContents $$1 && this.name.equals($$1.name) && this.objective.equals($$1.objective);
   }

   @Override
   public int hashCode() {
      int $$0 = this.name.hashCode();
      return 31 * $$0 + this.objective.hashCode();
   }

   @Override
   public String toString() {
      return "score{name='" + this.name + "', objective='" + this.objective + "'}";
   }
}
