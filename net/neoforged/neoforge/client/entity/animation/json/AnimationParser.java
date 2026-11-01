package net.neoforged.neoforge.client.entity.animation.json;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.KeyDispatchCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.Pair;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.function.UnaryOperator;
import net.minecraft.client.animation.AnimationChannel;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.animation.Keyframe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import net.neoforged.neoforge.client.entity.animation.AnimationKeyframeTarget;
import net.neoforged.neoforge.client.entity.animation.AnimationTarget;
import org.joml.Vector3f;

public final class AnimationParser {
   private static final Codec<AnimationTarget> TARGET_CODEC = ResourceLocation.CODEC
      .flatXmap(
         name -> Optional.ofNullable(AnimationTypeManager.getTarget(name))
            .<DataResult>map(DataResult::success)
            .orElseGet(
               () -> DataResult.error(
                  () -> String.format(Locale.ENGLISH, "Animation target '%s' not found. Registered targets: %s", name, AnimationTypeManager.getTargetList())
               )
            ),
         target -> Optional.ofNullable(AnimationTypeManager.getTargetName(target))
            .<DataResult>map(DataResult::success)
            .orElseGet(
               () -> DataResult.error(
                  () -> String.format(
                     Locale.ENGLISH, "Unregistered animation target '%s'. Registered targets: %s", target, AnimationTypeManager.getTargetList()
                  )
               )
            )
      );
   private static final Codec<AnimationChannel.Interpolation> INTERPOLATION_CODEC = ResourceLocation.CODEC
      .flatXmap(
         name -> Optional.ofNullable(AnimationTypeManager.getInterpolation(name))
            .<DataResult>map(DataResult::success)
            .orElseGet(
               () -> DataResult.error(
                  () -> String.format(
                     Locale.ENGLISH, "Animation interpolation '%s' not found. Registered interpolations: %s", name, AnimationTypeManager.getInterpolationList()
                  )
               )
            ),
         target -> Optional.ofNullable(AnimationTypeManager.getInterpolationName(target))
            .<DataResult>map(DataResult::success)
            .orElseGet(
               () -> DataResult.error(
                  () -> String.format(
                     Locale.ENGLISH,
                     "Unregistered animation interpolation '%s'. Registered interpolations: %s",
                     target,
                     AnimationTypeManager.getInterpolationList()
                  )
               )
            )
      );
   public static final MapCodec<AnimationChannel> CHANNEL_CODEC = new KeyDispatchCodec(
      "target",
      TARGET_CODEC,
      channel -> Optional.ofNullable(AnimationTypeManager.getTargetFromChannelTarget(channel.target()))
         .<DataResult>map(DataResult::success)
         .orElseGet(
            () -> DataResult.error(
               () -> String.format(
                  Locale.ENGLISH, "Unregistered animation channel target '%s'. Registered targets: %s", channel.target(), AnimationTypeManager.getTargetList()
               )
            )
         ),
      target -> DataResult.success(
         Optional.ofNullable(AnimationTypeManager.getKeyframeCodec(target))
            .orElseGet(() -> keyframeCodec(target))
            .listOf()
            .xmap(keyframes -> new AnimationChannel(target.channelTarget(), keyframes.toArray(Keyframe[]::new)), channel -> Arrays.asList(channel.keyframes()))
            .fieldOf("keyframes")
      )
   );
   private static final Codec<Pair<String, AnimationChannel>> NAMED_CHANNEL_CODEC = RecordCodecBuilder.create(
      instance -> instance.group(Codec.STRING.fieldOf("bone").forGetter(Pair::key), CHANNEL_CODEC.forGetter(Pair::value)).apply(instance, Pair::of)
   );
   public static final Codec<AnimationDefinition> CODEC = RecordCodecBuilder.create(
      instance -> instance.group(
            Codec.FLOAT.fieldOf("length").forGetter(AnimationDefinition::lengthInSeconds),
            Codec.BOOL.optionalFieldOf("loop", false).forGetter(AnimationDefinition::looping),
            NAMED_CHANNEL_CODEC.listOf().xmap(list -> {
               HashMap<String, List<AnimationChannel>> result = new HashMap<>();

               for (Pair<String, AnimationChannel> animation : list) {
                  result.computeIfAbsent((String)animation.key(), k -> new ArrayList<>()).add((AnimationChannel)animation.value());
               }

               return result;
            }, map -> {
               ArrayList<Pair<String, AnimationChannel>> result = new ArrayList<>();

               for (Entry<String, List<AnimationChannel>> entry : map.entrySet()) {
                  for (AnimationChannel channel : entry.getValue()) {
                     result.add(Pair.of(entry.getKey(), channel));
                  }
               }

               return result;
            }).fieldOf("animations").forGetter(AnimationDefinition::boneAnimations)
         )
         .apply(instance, AnimationDefinition::new)
   );

   private AnimationParser() {
   }

   static Codec<Keyframe> keyframeCodec(AnimationTarget target) {
      return RecordCodecBuilder.create(
         instance -> instance.group(
               Codec.FLOAT.fieldOf("timestamp").forGetter(Keyframe::timestamp),
               ExtraCodecs.VECTOR3F
                  .xmap(keyframeTargetToUnaryOp(target.keyframeTarget()), keyframeTargetToUnaryOp(target.inverseKeyframeTarget()))
                  .fieldOf("target")
                  .forGetter(Keyframe::target),
               INTERPOLATION_CODEC.fieldOf("interpolation").forGetter(Keyframe::interpolation)
            )
            .apply(instance, Keyframe::new)
      );
   }

   private static UnaryOperator<Vector3f> keyframeTargetToUnaryOp(AnimationKeyframeTarget target) {
      return vec -> target.apply(vec.x, vec.y, vec.z);
   }
}
