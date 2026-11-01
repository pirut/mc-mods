package net.minecraft.world.level.gamerules;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.Objects;
import java.util.function.ToIntFunction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import net.minecraft.world.flag.FeatureElement;
import net.minecraft.world.flag.FeatureFlagSet;

public final class GameRule<T> implements FeatureElement {
   private final GameRuleCategory category;
   private final GameRuleType gameRuleType;
   private final ArgumentType<T> argument;
   private final GameRules.VisitorCaller<T> visitorCaller;
   private final Codec<T> valueCodec;
   private final ToIntFunction<T> commandResultFunction;
   private final T defaultValue;
   private final FeatureFlagSet requiredFeatures;

   public GameRule(
      GameRuleCategory p_460778_,
      GameRuleType p_461116_,
      ArgumentType<T> p_461205_,
      GameRules.VisitorCaller<T> p_460902_,
      Codec<T> p_460950_,
      ToIntFunction<T> p_460678_,
      T p_460722_,
      FeatureFlagSet p_461099_
   ) {
      this.category = p_460778_;
      this.gameRuleType = p_461116_;
      this.argument = p_461205_;
      this.visitorCaller = p_460902_;
      this.valueCodec = p_460950_;
      this.commandResultFunction = p_460678_;
      this.defaultValue = p_460722_;
      this.requiredFeatures = p_461099_;
   }

   @Override
   public String toString() {
      return this.id();
   }

   public String id() {
      return this.getIdentifier().toShortString();
   }

   public Identifier getIdentifier() {
      return Objects.requireNonNull(BuiltInRegistries.GAME_RULE.getKey(this));
   }

   public String getDescriptionId() {
      return Util.makeDescriptionId("gamerule", this.getIdentifier());
   }

   public String serialize(T p_460889_) {
      return p_460889_.toString();
   }

   public DataResult<T> deserialize(String p_460650_) {
      try {
         StringReader stringreader = new StringReader(p_460650_);
         T t = (T)this.argument.parse(stringreader);
         return stringreader.canRead() ? DataResult.error(() -> "Failed to deserialize; trailing characters", t) : DataResult.success(t);
      } catch (CommandSyntaxException var4) {
         return DataResult.error(() -> "Failed to deserialize");
      }
   }

   public Class<T> valueClass() {
      return (Class<T>)this.defaultValue.getClass();
   }

   public void callVisitor(GameRuleTypeVisitor p_460674_) {
      this.visitorCaller.call(p_460674_, this);
   }

   public int getCommandResult(T p_460849_) {
      return this.commandResultFunction.applyAsInt(p_460849_);
   }

   public GameRuleCategory category() {
      return this.category;
   }

   public GameRuleType gameRuleType() {
      return this.gameRuleType;
   }

   public ArgumentType<T> argument() {
      return this.argument;
   }

   public Codec<T> valueCodec() {
      return this.valueCodec;
   }

   public T defaultValue() {
      return this.defaultValue;
   }

   @Override
   public FeatureFlagSet requiredFeatures() {
      return this.requiredFeatures;
   }
}
