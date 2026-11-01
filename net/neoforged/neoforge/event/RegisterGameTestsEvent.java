package net.neoforged.neoforge.event;

import java.util.function.Function;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.core.WritableRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestInstance;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;

public class RegisterGameTestsEvent extends Event implements IModBusEvent {
   private final WritableRegistry<TestEnvironmentDefinition> environmentsRegistry;
   private final WritableRegistry<GameTestInstance> testsRegistry;

   public RegisterGameTestsEvent(WritableRegistry<TestEnvironmentDefinition> environmentsRegistry, WritableRegistry<GameTestInstance> testsRegistry) {
      this.environmentsRegistry = environmentsRegistry;
      this.testsRegistry = testsRegistry;
   }

   public Holder<TestEnvironmentDefinition> registerEnvironment(Identifier name, TestEnvironmentDefinition... definitions) {
      return this.registerEnvironment(name, new TestEnvironmentDefinition.AllOf(definitions));
   }

   public Holder<TestEnvironmentDefinition> registerEnvironment(Identifier name, TestEnvironmentDefinition definition) {
      return this.environmentsRegistry.register(ResourceKey.create(Registries.TEST_ENVIRONMENT, name), definition, RegistrationInfo.BUILT_IN);
   }

   public void registerTest(
      Identifier name, Function<TestData<Holder<TestEnvironmentDefinition>>, GameTestInstance> factory, TestData<Holder<TestEnvironmentDefinition>> testData
   ) {
      this.registerTest(name, factory.apply(testData));
   }

   public void registerTest(Identifier name, GameTestInstance test) {
      this.testsRegistry.register(ResourceKey.create(Registries.TEST_INSTANCE, name), test, RegistrationInfo.BUILT_IN);
   }
}
