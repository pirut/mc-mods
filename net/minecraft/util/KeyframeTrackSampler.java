package net.minecraft.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.world.attribute.LerpFunction;

public class KeyframeTrackSampler<T> {
   private final Optional<Integer> periodTicks;
   private final LerpFunction<T> lerp;
   private final List<KeyframeTrackSampler.Segment<T>> segments;

   KeyframeTrackSampler(KeyframeTrack<T> track, Optional<Integer> periodTicks, LerpFunction<T> lerp) {
      this.periodTicks = periodTicks;
      this.lerp = lerp;
      this.segments = bakeSegments(track, periodTicks);
   }

   private static <T> List<KeyframeTrackSampler.Segment<T>> bakeSegments(KeyframeTrack<T> track, Optional<Integer> periodTicks) {
      List<Keyframe<T>> list = track.keyframes();
      if (list.size() == 1) {
         T object = list.getFirst().value();
         return List.of(new KeyframeTrackSampler.Segment<>(EasingType.CONSTANT, object, 0, object, 0));
      } else {
         List<KeyframeTrackSampler.Segment<T>> list2 = new ArrayList<>();
         if (periodTicks.isPresent()) {
            Keyframe<T> keyframe = list.getFirst();
            Keyframe<T> keyframe2 = list.getLast();
            list2.add(new KeyframeTrackSampler.Segment<>(track, keyframe2, keyframe2.ticks() - periodTicks.get(), keyframe, keyframe.ticks()));
            addSegmentsFromKeyframes(track, list, list2);
            list2.add(new KeyframeTrackSampler.Segment<>(track, keyframe2, keyframe2.ticks(), keyframe, keyframe.ticks() + periodTicks.get()));
         } else {
            addSegmentsFromKeyframes(track, list, list2);
         }

         return List.copyOf(list2);
      }
   }

   private static <T> void addSegmentsFromKeyframes(KeyframeTrack<T> track, List<Keyframe<T>> keyframes, List<KeyframeTrackSampler.Segment<T>> output) {
      for (int i = 0; i < keyframes.size() - 1; i++) {
         Keyframe<T> keyframe = keyframes.get(i);
         Keyframe<T> keyframe2 = keyframes.get(i + 1);
         output.add(new KeyframeTrackSampler.Segment<>(track, keyframe, keyframe.ticks(), keyframe2, keyframe2.ticks()));
      }
   }

   public T sample(long ticks) {
      long l = this.loopTicks(ticks);
      KeyframeTrackSampler.Segment<T> segment = this.getSegmentAt(l);
      if (l <= segment.fromTicks) {
         return segment.fromValue;
      } else if (l >= segment.toTicks) {
         return segment.toValue;
      } else {
         float f = (float)(l - segment.fromTicks) / (segment.toTicks - segment.fromTicks);
         float g = segment.easing.apply(f);
         return this.lerp.apply(g, segment.fromValue, segment.toValue);
      }
   }

   private KeyframeTrackSampler.Segment<T> getSegmentAt(long ticks) {
      for (KeyframeTrackSampler.Segment<T> segment : this.segments) {
         if (ticks < segment.toTicks) {
            return segment;
         }
      }

      return this.segments.getLast();
   }

   private long loopTicks(long ticks) {
      return this.periodTicks.isPresent() ? Math.floorMod(ticks, this.periodTicks.get()) : ticks;
   }

   record Segment<T>(EasingType easing, T fromValue, int fromTicks, T toValue, int toTicks) {

      public Segment(KeyframeTrack<T> track, Keyframe<T> from, int fromTicks, Keyframe<T> to, int toTicks) {
         this(track.easingType(), from.value(), fromTicks, to.value(), toTicks);
      }
   }
}
