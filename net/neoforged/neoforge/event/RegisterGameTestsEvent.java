package net.neoforged.neoforge.event;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;

public class RegisterGameTestsEvent extends Event implements IModBusEvent {
   private final Set<Method> gameTestMethods;

   public RegisterGameTestsEvent(Set<Method> gameTestMethods) {
      this.gameTestMethods = gameTestMethods;
   }

   public void register(Class<?> testClass) {
      Arrays.stream(testClass.getDeclaredMethods()).forEach(this::register);
   }

   public void register(Method testMethod) {
      this.gameTestMethods.add(testMethod);
   }
}
