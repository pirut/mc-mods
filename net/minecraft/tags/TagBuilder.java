package net.minecraft.tags;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.common.extensions.ITagBuilderExtension;

public class TagBuilder implements ITagBuilderExtension {
   private final List<TagEntry> removeEntries = new ArrayList<>();
   private boolean replace = false;
   private final List<TagEntry> entries = new ArrayList<>();

   public Stream<TagEntry> getRemoveEntries() {
      return this.removeEntries.stream();
   }

   public TagBuilder remove(TagEntry entry) {
      this.removeEntries.add(entry);
      return this;
   }

   public static TagBuilder create() {
      return new TagBuilder();
   }

   public List<TagEntry> build() {
      return List.copyOf(this.entries);
   }

   public TagBuilder add(TagEntry p_215903_) {
      this.entries.add(p_215903_);
      return this;
   }

   public TagBuilder addElement(Identifier p_468950_) {
      return this.add(TagEntry.element(p_468950_));
   }

   public TagBuilder addOptionalElement(Identifier p_469303_) {
      return this.add(TagEntry.optionalElement(p_469303_));
   }

   public TagBuilder addTag(Identifier p_468413_) {
      return this.add(TagEntry.tag(p_468413_));
   }

   public TagBuilder addOptionalTag(Identifier p_467982_) {
      return this.add(TagEntry.optionalTag(p_467982_));
   }

   public TagBuilder replace(boolean value) {
      this.replace = value;
      return this;
   }

   public TagBuilder replace() {
      return this.replace(true);
   }

   public boolean isReplace() {
      return this.replace;
   }
}
