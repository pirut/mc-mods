package net.minecraft.gametest.framework;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Rotation;

public abstract class GameTestInstance {
   public static final Codec<GameTestInstance> DIRECT_CODEC = BuiltInRegistries.TEST_INSTANCE_TYPE
      .byNameCodec()
      .dispatch(GameTestInstance::codec, p_397208_ -> p_397208_);
   private final TestData<Holder<TestEnvironmentDefinition>> info;

   public static MapCodec<? extends GameTestInstance> bootstrap(Registry<MapCodec<? extends GameTestInstance>> p_397652_) {
      register(p_397652_, "block_based", BlockBasedTestInstance.CODEC);
      return register(p_397652_, "function", FunctionGameTestInstance.CODEC);
   }

   private static MapCodec<? extends GameTestInstance> register(
      Registry<MapCodec<? extends GameTestInstance>> p_397158_, String p_397123_, MapCodec<? extends GameTestInstance> p_397461_
   ) {
      return Registry.register(p_397158_, ResourceKey.create(Registries.TEST_INSTANCE_TYPE, Identifier.withDefaultNamespace(p_397123_)), p_397461_);
   }

   protected GameTestInstance(TestData<Holder<TestEnvironmentDefinition>> p_397684_) {
      this.info = p_397684_;
   }

   public abstract void run(GameTestHelper var1);

   public abstract MapCodec<? extends GameTestInstance> codec();

   public Holder<TestEnvironmentDefinition> batch() {
      return this.info.environment();
   }

   public Identifier structure() {
      return this.info.structure();
   }

   public int maxTicks() {
      return this.info.maxTicks();
   }

   public int setupTicks() {
      return this.info.setupTicks();
   }

   public boolean required() {
      return this.info.required();
   }

   public boolean manualOnly() {
      return this.info.manualOnly();
   }

   public int maxAttempts() {
      return this.info.maxAttempts();
   }

   public int requiredSuccesses() {
      return this.info.requiredSuccesses();
   }

   public boolean skyAccess() {
      return this.info.skyAccess();
   }

   public Rotation rotation() {
      return this.info.rotation();
   }

   protected TestData<Holder<TestEnvironmentDefinition>> info() {
      return this.info;
   }

   protected abstract MutableComponent typeDescription();

   public Component describe() {
      return this.describeType().append(this.describeInfo());
   }

   protected MutableComponent describeType() {
      return this.descriptionRow("test_instance.description.type", this.typeDescription());
   }

   protected Component describeInfo() {
      return this.descriptionRow("test_instance.description.structure", this.info.structure().toString())
         .append(this.descriptionRow("test_instance.description.batch", this.info.environment().getRegisteredName()));
   }

   protected MutableComponent descriptionRow(String p_397304_, String p_397169_) {
      return this.descriptionRow(p_397304_, Component.literal(p_397169_));
   }

   protected MutableComponent descriptionRow(String p_397979_, MutableComponent p_397599_) {
      return Component.translatable(p_397979_, p_397599_.withStyle(ChatFormatting.BLUE)).append(Component.literal("\n"));
   }
}
