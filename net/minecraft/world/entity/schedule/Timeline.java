package net.minecraft.world.entity.schedule;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.ints.Int2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectSortedMap;
import java.util.Collection;
import java.util.List;

public class Timeline {
   private final List<Keyframe> keyframes = Lists.newArrayList();
   private int previousIndex;

   public ImmutableList<Keyframe> getKeyframes() {
      return ImmutableList.copyOf(this.keyframes);
   }

   public Timeline addKeyframe(int p_38061_, float p_38062_) {
      this.keyframes.add(new Keyframe(p_38061_, p_38062_));
      this.sortAndDeduplicateKeyframes();
      return this;
   }

   public Timeline addKeyframes(Collection<Keyframe> p_150248_) {
      this.keyframes.addAll(p_150248_);
      this.sortAndDeduplicateKeyframes();
      return this;
   }

   private void sortAndDeduplicateKeyframes() {
      Int2ObjectSortedMap<Keyframe> $$0 = new Int2ObjectAVLTreeMap();
      this.keyframes.forEach(p_38065_ -> $$0.put(p_38065_.getTimeStamp(), p_38065_));
      this.keyframes.clear();
      this.keyframes.addAll($$0.values());
      this.previousIndex = 0;
   }

   public float getValueAt(int p_38059_) {
      if (this.keyframes.size() <= 0) {
         return 0.0F;
      } else {
         Keyframe $$1 = this.keyframes.get(this.previousIndex);
         Keyframe $$2 = this.keyframes.get(this.keyframes.size() - 1);
         boolean $$3 = p_38059_ < $$1.getTimeStamp();
         int $$4 = $$3 ? 0 : this.previousIndex;
         float $$5 = $$3 ? $$2.getValue() : $$1.getValue();

         for (int $$6 = $$4; $$6 < this.keyframes.size(); $$6++) {
            Keyframe $$7 = this.keyframes.get($$6);
            if ($$7.getTimeStamp() > p_38059_) {
               break;
            }

            this.previousIndex = $$6;
            $$5 = $$7.getValue();
         }

         return $$5;
      }
   }
}
