package io.arivera.oss.jchatops;

import org.springframework.context.annotation.Scope;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Scope(value = "prototype")
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MessageHandler {

  String PATTERNS_FIELD_NAME = "patterns";
  String MESSAGE_TYPES_FIELD_NAME = "messageTypes";

  /**
   * Regular Expression patterns.
   */
  String[] patterns() default {""};

  /**
   * Defines the types of messages that this bean will respondWith to.
   */
  MessageType[] messageTypes() default {MessageType.TAGGED, MessageType.PRIVATE};

  /**
   * If a bean is only meant to be invoked when in the context of a conversation, mark this as {@code true}
   */
  boolean requiresConversation() default false;
}
