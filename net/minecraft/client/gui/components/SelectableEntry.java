package net.minecraft.client.gui.components;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface SelectableEntry {
   default boolean mouseOverIcon(int p_461193_, int p_460784_, int p_460715_) {
      return p_461193_ >= 0 && p_461193_ < p_460715_ && p_460784_ >= 0 && p_460784_ < p_460715_;
   }

   default boolean mouseOverLeftHalf(int p_460659_, int p_460837_, int p_461087_) {
      return p_460659_ >= 0 && p_460659_ < p_461087_ / 2 && p_460837_ >= 0 && p_460837_ < p_461087_;
   }

   default boolean mouseOverRightHalf(int p_461282_, int p_460798_, int p_461221_) {
      return p_461282_ >= p_461221_ / 2 && p_461282_ < p_461221_ && p_460798_ >= 0 && p_460798_ < p_461221_;
   }

   default boolean mouseOverTopRightQuarter(int p_461173_, int p_461056_, int p_460774_) {
      return p_461173_ >= p_460774_ / 2 && p_461173_ < p_460774_ && p_461056_ >= 0 && p_461056_ < p_460774_ / 2;
   }

   default boolean mouseOverBottomRightQuarter(int p_461027_, int p_461049_, int p_461258_) {
      return p_461027_ >= p_461258_ / 2 && p_461027_ < p_461258_ && p_461049_ >= p_461258_ / 2 && p_461049_ < p_461258_;
   }

   default boolean mouseOverTopLeftQuarter(int p_460803_, int p_461212_, int p_460741_) {
      return p_460803_ >= 0 && p_460803_ < p_460741_ / 2 && p_461212_ >= 0 && p_461212_ < p_460741_ / 2;
   }

   default boolean mouseOverBottomLeftQuarter(int p_461222_, int p_461017_, int p_460693_) {
      return p_461222_ >= 0 && p_461222_ < p_460693_ / 2 && p_461017_ >= p_460693_ / 2 && p_461017_ < p_460693_;
   }
}
