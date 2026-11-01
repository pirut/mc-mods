package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.Dynamic3CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import java.util.stream.Stream;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

public class AttributeCommand {
   private static final DynamicCommandExceptionType ERROR_NOT_LIVING_ENTITY = new DynamicCommandExceptionType(
      object -> Component.translatableEscape("commands.attribute.failed.entity", object)
   );
   private static final Dynamic2CommandExceptionType ERROR_NO_SUCH_ATTRIBUTE = new Dynamic2CommandExceptionType(
      (object, object2) -> Component.translatableEscape("commands.attribute.failed.no_attribute", object, object2)
   );
   private static final Dynamic3CommandExceptionType ERROR_NO_SUCH_MODIFIER = new Dynamic3CommandExceptionType(
      (object, object2, object3) -> Component.translatableEscape("commands.attribute.failed.no_modifier", object2, object, object3)
   );
   private static final Dynamic3CommandExceptionType ERROR_MODIFIER_ALREADY_PRESENT = new Dynamic3CommandExceptionType(
      (object, object2, object3) -> Component.translatableEscape("commands.attribute.failed.modifier_already_present", object3, object2, object)
   );

   public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context) {
      dispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("attribute").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)))
            .then(
               Commands.argument("target", EntityArgument.entity())
                  .then(
                     ((RequiredArgumentBuilder)((RequiredArgumentBuilder)Commands.argument(
                                 "attribute", ResourceArgument.resource(context, Registries.ATTRIBUTE)
                              )
                              .then(
                                 ((LiteralArgumentBuilder)Commands.literal("get")
                                       .executes(
                                          commandContext -> getAttributeValue(
                                             (CommandSourceStack)commandContext.getSource(),
                                             EntityArgument.getEntity(commandContext, "target"),
                                             ResourceArgument.getAttribute(commandContext, "attribute"),
                                             1.0
                                          )
                                       ))
                                    .then(
                                       Commands.argument("scale", DoubleArgumentType.doubleArg())
                                          .executes(
                                             commandContext -> getAttributeValue(
                                                (CommandSourceStack)commandContext.getSource(),
                                                EntityArgument.getEntity(commandContext, "target"),
                                                ResourceArgument.getAttribute(commandContext, "attribute"),
                                                DoubleArgumentType.getDouble(commandContext, "scale")
                                             )
                                          )
                                    )
                              ))
                           .then(
                              ((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("base")
                                       .then(
                                          Commands.literal("set")
                                             .then(
                                                Commands.argument("value", DoubleArgumentType.doubleArg())
                                                   .executes(
                                                      commandContext -> setAttributeBase(
                                                         (CommandSourceStack)commandContext.getSource(),
                                                         EntityArgument.getEntity(commandContext, "target"),
                                                         ResourceArgument.getAttribute(commandContext, "attribute"),
                                                         DoubleArgumentType.getDouble(commandContext, "value")
                                                      )
                                                   )
                                             )
                                       ))
                                    .then(
                                       ((LiteralArgumentBuilder)Commands.literal("get")
                                             .executes(
                                                commandContext -> getAttributeBase(
                                                   (CommandSourceStack)commandContext.getSource(),
                                                   EntityArgument.getEntity(commandContext, "target"),
                                                   ResourceArgument.getAttribute(commandContext, "attribute"),
                                                   1.0
                                                )
                                             ))
                                          .then(
                                             Commands.argument("scale", DoubleArgumentType.doubleArg())
                                                .executes(
                                                   commandContext -> getAttributeBase(
                                                      (CommandSourceStack)commandContext.getSource(),
                                                      EntityArgument.getEntity(commandContext, "target"),
                                                      ResourceArgument.getAttribute(commandContext, "attribute"),
                                                      DoubleArgumentType.getDouble(commandContext, "scale")
                                                   )
                                                )
                                          )
                                    ))
                                 .then(
                                    Commands.literal("reset")
                                       .executes(
                                          commandContext -> resetAttributeBase(
                                             (CommandSourceStack)commandContext.getSource(),
                                             EntityArgument.getEntity(commandContext, "target"),
                                             ResourceArgument.getAttribute(commandContext, "attribute")
                                          )
                                       )
                                 )
                           ))
                        .then(
                           ((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("modifier")
                                    .then(
                                       Commands.literal("add")
                                          .then(
                                             Commands.argument("id", IdentifierArgument.id())
                                                .then(
                                                   ((RequiredArgumentBuilder)((RequiredArgumentBuilder)Commands.argument(
                                                               "value", DoubleArgumentType.doubleArg()
                                                            )
                                                            .then(
                                                               Commands.literal("add_value")
                                                                  .executes(
                                                                     commandContext -> addModifier(
                                                                        (CommandSourceStack)commandContext.getSource(),
                                                                        EntityArgument.getEntity(commandContext, "target"),
                                                                        ResourceArgument.getAttribute(commandContext, "attribute"),
                                                                        IdentifierArgument.getId(commandContext, "id"),
                                                                        DoubleArgumentType.getDouble(commandContext, "value"),
                                                                        AttributeModifier.Operation.ADD_VALUE
                                                                     )
                                                                  )
                                                            ))
                                                         .then(
                                                            Commands.literal("add_multiplied_base")
                                                               .executes(
                                                                  commandContext -> addModifier(
                                                                     (CommandSourceStack)commandContext.getSource(),
                                                                     EntityArgument.getEntity(commandContext, "target"),
                                                                     ResourceArgument.getAttribute(commandContext, "attribute"),
                                                                     IdentifierArgument.getId(commandContext, "id"),
                                                                     DoubleArgumentType.getDouble(commandContext, "value"),
                                                                     AttributeModifier.Operation.ADD_MULTIPLIED_BASE
                                                                  )
                                                               )
                                                         ))
                                                      .then(
                                                         Commands.literal("add_multiplied_total")
                                                            .executes(
                                                               commandContext -> addModifier(
                                                                  (CommandSourceStack)commandContext.getSource(),
                                                                  EntityArgument.getEntity(commandContext, "target"),
                                                                  ResourceArgument.getAttribute(commandContext, "attribute"),
                                                                  IdentifierArgument.getId(commandContext, "id"),
                                                                  DoubleArgumentType.getDouble(commandContext, "value"),
                                                                  AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
                                                               )
                                                            )
                                                      )
                                                )
                                          )
                                    ))
                                 .then(
                                    Commands.literal("remove")
                                       .then(
                                          Commands.argument("id", IdentifierArgument.id())
                                             .suggests(
                                                (commandContext, suggestionsBuilder) -> SharedSuggestionProvider.suggestResource(
                                                   getAttributeModifiers(
                                                      EntityArgument.getEntity(commandContext, "target"),
                                                      ResourceArgument.getAttribute(commandContext, "attribute")
                                                   ),
                                                   suggestionsBuilder
                                                )
                                             )
                                             .executes(
                                                commandContext -> removeModifier(
                                                   (CommandSourceStack)commandContext.getSource(),
                                                   EntityArgument.getEntity(commandContext, "target"),
                                                   ResourceArgument.getAttribute(commandContext, "attribute"),
                                                   IdentifierArgument.getId(commandContext, "id")
                                                )
                                             )
                                       )
                                 ))
                              .then(
                                 Commands.literal("value")
                                    .then(
                                       Commands.literal("get")
                                          .then(
                                             ((RequiredArgumentBuilder)Commands.argument("id", IdentifierArgument.id())
                                                   .suggests(
                                                      (commandContext, suggestionsBuilder) -> SharedSuggestionProvider.suggestResource(
                                                         getAttributeModifiers(
                                                            EntityArgument.getEntity(commandContext, "target"),
                                                            ResourceArgument.getAttribute(commandContext, "attribute")
                                                         ),
                                                         suggestionsBuilder
                                                      )
                                                   )
                                                   .executes(
                                                      commandContext -> getAttributeModifier(
                                                         (CommandSourceStack)commandContext.getSource(),
                                                         EntityArgument.getEntity(commandContext, "target"),
                                                         ResourceArgument.getAttribute(commandContext, "attribute"),
                                                         IdentifierArgument.getId(commandContext, "id"),
                                                         1.0
                                                      )
                                                   ))
                                                .then(
                                                   Commands.argument("scale", DoubleArgumentType.doubleArg())
                                                      .executes(
                                                         commandContext -> getAttributeModifier(
                                                            (CommandSourceStack)commandContext.getSource(),
                                                            EntityArgument.getEntity(commandContext, "target"),
                                                            ResourceArgument.getAttribute(commandContext, "attribute"),
                                                            IdentifierArgument.getId(commandContext, "id"),
                                                            DoubleArgumentType.getDouble(commandContext, "scale")
                                                         )
                                                      )
                                                )
                                          )
                                    )
                              )
                        )
                  )
            )
      );
   }

   private static AttributeInstance getAttributeInstance(Entity entity, Holder<Attribute> attribute) throws CommandSyntaxException {
      AttributeInstance attributeInstance = getLivingEntity(entity).getAttributes().getInstance(attribute);
      if (attributeInstance == null) {
         throw ERROR_NO_SUCH_ATTRIBUTE.create(entity.getName(), getAttributeDescription(attribute));
      } else {
         return attributeInstance;
      }
   }

   private static LivingEntity getLivingEntity(Entity target) throws CommandSyntaxException {
      if (!(target instanceof LivingEntity)) {
         throw ERROR_NOT_LIVING_ENTITY.create(target.getName());
      } else {
         return (LivingEntity)target;
      }
   }

   private static LivingEntity getEntityWithAttribute(Entity entity, Holder<Attribute> attribute) throws CommandSyntaxException {
      LivingEntity livingEntity = getLivingEntity(entity);
      if (!livingEntity.getAttributes().hasAttribute(attribute)) {
         throw ERROR_NO_SUCH_ATTRIBUTE.create(entity.getName(), getAttributeDescription(attribute));
      } else {
         return livingEntity;
      }
   }

   private static int getAttributeValue(CommandSourceStack source, Entity entity, Holder<Attribute> attribute, double scale) throws CommandSyntaxException {
      LivingEntity livingEntity = getEntityWithAttribute(entity, attribute);
      double d = livingEntity.getAttributeValue(attribute);
      source.sendSuccess(() -> Component.translatable("commands.attribute.value.get.success", getAttributeDescription(attribute), entity.getName(), d), false);
      return (int)(d * scale);
   }

   private static int getAttributeBase(CommandSourceStack source, Entity entity, Holder<Attribute> attribute, double scale) throws CommandSyntaxException {
      LivingEntity livingEntity = getEntityWithAttribute(entity, attribute);
      double d = livingEntity.getAttributeBaseValue(attribute);
      source.sendSuccess(
         () -> Component.translatable("commands.attribute.base_value.get.success", getAttributeDescription(attribute), entity.getName(), d), false
      );
      return (int)(d * scale);
   }

   private static int getAttributeModifier(CommandSourceStack source, Entity entity, Holder<Attribute> attribute, Identifier id, double scale) throws CommandSyntaxException {
      LivingEntity livingEntity = getEntityWithAttribute(entity, attribute);
      AttributeMap attributeMap = livingEntity.getAttributes();
      if (!attributeMap.hasModifier(attribute, id)) {
         throw ERROR_NO_SUCH_MODIFIER.create(entity.getName(), getAttributeDescription(attribute), id);
      } else {
         double d = attributeMap.getModifierValue(attribute, id);
         source.sendSuccess(
            () -> Component.translatable(
               "commands.attribute.modifier.value.get.success", Component.translationArg(id), getAttributeDescription(attribute), entity.getName(), d
            ),
            false
         );
         return (int)(d * scale);
      }
   }

   private static Stream<Identifier> getAttributeModifiers(Entity entity, Holder<Attribute> attribute) throws CommandSyntaxException {
      AttributeInstance attributeInstance = getAttributeInstance(entity, attribute);
      return attributeInstance.getModifiers().stream().map(AttributeModifier::id);
   }

   private static int setAttributeBase(CommandSourceStack source, Entity entity, Holder<Attribute> attribute, double value) throws CommandSyntaxException {
      getAttributeInstance(entity, attribute).setBaseValue(value);
      source.sendSuccess(
         () -> Component.translatable("commands.attribute.base_value.set.success", getAttributeDescription(attribute), entity.getName(), value), false
      );
      return 1;
   }

   private static int resetAttributeBase(CommandSourceStack source, Entity entity, Holder<Attribute> attribute) throws CommandSyntaxException {
      LivingEntity livingEntity = getLivingEntity(entity);
      if (!livingEntity.getAttributes().resetBaseValue(attribute)) {
         throw ERROR_NO_SUCH_ATTRIBUTE.create(entity.getName(), getAttributeDescription(attribute));
      } else {
         double d = livingEntity.getAttributeBaseValue(attribute);
         source.sendSuccess(
            () -> Component.translatable("commands.attribute.base_value.reset.success", getAttributeDescription(attribute), entity.getName(), d), false
         );
         return 1;
      }
   }

   private static int addModifier(
      CommandSourceStack source, Entity entity, Holder<Attribute> attribute, Identifier id, double amount, AttributeModifier.Operation operation
   ) throws CommandSyntaxException {
      AttributeInstance attributeInstance = getAttributeInstance(entity, attribute);
      AttributeModifier attributeModifier = new AttributeModifier(id, amount, operation);
      if (attributeInstance.hasModifier(id)) {
         throw ERROR_MODIFIER_ALREADY_PRESENT.create(entity.getName(), getAttributeDescription(attribute), id);
      } else {
         attributeInstance.addPermanentModifier(attributeModifier);
         source.sendSuccess(
            () -> Component.translatable(
               "commands.attribute.modifier.add.success", Component.translationArg(id), getAttributeDescription(attribute), entity.getName()
            ),
            false
         );
         return 1;
      }
   }

   private static int removeModifier(CommandSourceStack source, Entity entity, Holder<Attribute> attribute, Identifier id) throws CommandSyntaxException {
      AttributeInstance attributeInstance = getAttributeInstance(entity, attribute);
      if (attributeInstance.removeModifier(id)) {
         source.sendSuccess(
            () -> Component.translatable(
               "commands.attribute.modifier.remove.success", Component.translationArg(id), getAttributeDescription(attribute), entity.getName()
            ),
            false
         );
         return 1;
      } else {
         throw ERROR_NO_SUCH_MODIFIER.create(entity.getName(), getAttributeDescription(attribute), id);
      }
   }

   private static Component getAttributeDescription(Holder<Attribute> attribute) {
      return Component.translatable(attribute.value().getDescriptionId());
   }
}
