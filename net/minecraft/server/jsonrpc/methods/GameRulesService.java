package net.minecraft.server.jsonrpc.methods;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.jsonrpc.internalapi.MinecraftApi;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRuleType;

public class GameRulesService {
   public static List<GameRulesService.GameRuleUpdate<?>> get(MinecraftApi p_443431_) {
      List<GameRulesService.GameRuleUpdate<?>> list = new ArrayList<>();
      p_443431_.gameRuleService().getAvailableGameRules().forEach(p_460839_ -> addGameRule(p_443431_, (GameRule<?>)p_460839_, list));
      return list;
   }

   private static <T> void addGameRule(MinecraftApi p_460714_, GameRule<T> p_461054_, List<GameRulesService.GameRuleUpdate<?>> p_461285_) {
      T t = p_460714_.gameRuleService().getRuleValue(p_461054_);
      p_461285_.add(getTypedRule(p_460714_, p_461054_, Objects.requireNonNull(t)));
   }

   public static <T> GameRulesService.GameRuleUpdate<T> getTypedRule(MinecraftApi p_442519_, GameRule<T> p_461108_, T p_460898_) {
      return p_442519_.gameRuleService().getTypedRule(p_461108_, p_460898_);
   }

   public static <T> GameRulesService.GameRuleUpdate<T> update(MinecraftApi p_443622_, GameRulesService.GameRuleUpdate<T> p_460954_, ClientInfo p_443278_) {
      return p_443622_.gameRuleService().updateGameRule(p_460954_, p_443278_);
   }

   public record GameRuleUpdate<T>(GameRule<T> gameRule, T value) {
      public static final Codec<GameRulesService.GameRuleUpdate<?>> TYPED_CODEC = BuiltInRegistries.GAME_RULE
         .byNameCodec()
         .dispatch("key", GameRulesService.GameRuleUpdate::gameRule, GameRulesService.GameRuleUpdate::getValueAndTypeCodec);
      public static final Codec<GameRulesService.GameRuleUpdate<?>> CODEC = BuiltInRegistries.GAME_RULE
         .byNameCodec()
         .dispatch("key", GameRulesService.GameRuleUpdate::gameRule, GameRulesService.GameRuleUpdate::getValueCodec);

      private static <T> MapCodec<? extends GameRulesService.GameRuleUpdate<T>> getValueCodec(GameRule<T> p_461217_) {
         return p_461217_.valueCodec()
            .fieldOf("value")
            .xmap(p_461035_ -> new GameRulesService.GameRuleUpdate<>(p_461217_, (T)p_461035_), GameRulesService.GameRuleUpdate::value);
      }

      private static <T> MapCodec<? extends GameRulesService.GameRuleUpdate<T>> getValueAndTypeCodec(GameRule<T> p_460976_) {
         return RecordCodecBuilder.mapCodec(
            p_460813_ -> p_460813_.group(
                  StringRepresentable.fromEnum(GameRuleType::values).fieldOf("type").forGetter(p_460997_ -> p_460997_.gameRule.gameRuleType()),
                  p_460976_.valueCodec().fieldOf("value").forGetter(GameRulesService.GameRuleUpdate::value)
               )
               .apply(p_460813_, (p_461004_, p_460794_) -> getUntypedRule(p_460976_, p_461004_, (T)p_460794_))
         );
      }

      private static <T> GameRulesService.GameRuleUpdate<T> getUntypedRule(GameRule<T> p_461209_, GameRuleType p_460747_, T p_461082_) {
         if (p_461209_.gameRuleType() != p_460747_) {
            throw new InvalidParameterJsonRpcException(
               "Stated type \"" + p_460747_ + "\" mismatches with actual type \"" + p_461209_.gameRuleType() + "\" of gamerule \"" + p_461209_.id() + "\""
            );
         } else {
            return new GameRulesService.GameRuleUpdate<>(p_461209_, p_461082_);
         }
      }
   }
}
