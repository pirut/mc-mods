package net.neoforged.neoforge.common.extensions;

import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagBuilder;
import net.minecraft.tags.TagEntry;

public interface ITagBuilderExtension {
   private TagBuilder self() {
      return (TagBuilder)this;
   }

   default TagBuilder removeElement(Identifier elementID) {
      return this.self().remove(TagEntry.element(elementID));
   }

   default TagBuilder removeTag(Identifier tagID) {
      return this.self().remove(TagEntry.tag(tagID));
   }
}
