package io.arivera.oss.jchatops.annotations;

import io.arivera.oss.jchatops.MessageType;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;
import org.springframework.core.annotation.AliasFor;
import org.springframework.core.annotation.AnnotationAttributes;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

@Bean
@Scope(value = "prototype")
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MessageHandler {

  String NAME_FIELD_NAME = "name";
  String PATTERNS_FIELD_NAME = "patterns";
  String FLAGS_FIELD_NAME = "flags";
  String MESSAGE_TYPES_FIELD_NAME = "messageTypes";
  String REQUIRES_CONVERSATION_FIELD_NAME = "requiresConversation";

  /**
   * The value may indicate a suggestion for a logical component name,
   * to be turned into a Spring bean in case of an autodetected component.
   * @return the suggested component name, if any
   */
  @AliasFor(annotation = Bean.class, attribute = "name")
  String[] name() default {};

  /**
   * Regular Expression patterns.
   *
   * @see Pattern
   */
  String[] patterns() default {""};

  /**
   * Regular Expression pattern flags.
   *
   * @see Pattern
   */
  int flags() default Pattern.CASE_INSENSITIVE;

  /**
   * Defines the types of messages that this bean will submitResponse to.
   */
  MessageType[] messageTypes() default {MessageType.TAGGED, MessageType.PRIVATE};

  /**
   * If a bean is only meant to be invoked when in the context of a conversation, mark this as {@code true}
   */
  boolean requiresConversation() default false;


  class BaseMessageHandler implements MessageHandler {

    private final String[] name;
    private final String[] patterns;
    private final int flags;
    private final MessageType[] messageTypes;
    private final boolean requiresConversation;

    public BaseMessageHandler(AnnotationAttributes annotationAttributes) {
      this.name = annotationAttributes.getStringArray(MessageHandler.NAME_FIELD_NAME);
      this.patterns = annotationAttributes.getStringArray(MessageHandler.PATTERNS_FIELD_NAME);
      this.flags = annotationAttributes.getNumber(MessageHandler.FLAGS_FIELD_NAME);
      this.messageTypes = (MessageType[]) annotationAttributes.get(MessageHandler.MESSAGE_TYPES_FIELD_NAME);
      this.requiresConversation = annotationAttributes.getBoolean(MessageHandler.REQUIRES_CONVERSATION_FIELD_NAME);
    }

    public BaseMessageHandler(Map<String,Object> annotationAttributes) {
      this.name = (String[]) annotationAttributes.get(MessageHandler.NAME_FIELD_NAME);
      this.patterns = (String[]) annotationAttributes.get(MessageHandler.PATTERNS_FIELD_NAME);
      this.flags = (int) annotationAttributes.get(MessageHandler.FLAGS_FIELD_NAME);
      this.messageTypes = (MessageType[]) annotationAttributes.get(MessageHandler.MESSAGE_TYPES_FIELD_NAME);
      this.requiresConversation = (boolean) annotationAttributes.get(MessageHandler.REQUIRES_CONVERSATION_FIELD_NAME);
    }

    public BaseMessageHandler(String[] name, String[] patterns, int flags,
                              MessageType[] messageTypes, boolean requiresConversation) {
      this.name = name.clone();
      this.patterns = patterns.clone();
      this.flags = flags;
      this.messageTypes = messageTypes.clone();
      this.requiresConversation = requiresConversation;
    }

    @Override
    public Class<? extends Annotation> annotationType() {
      return MessageHandler.class;
    }

    @Override
    public String[] name() {
      return name.clone();
    }

    @Override
    public String[] patterns() {
      return patterns.clone();
    }

    @Override
    public int flags() {
      return flags;
    }

    @Override
    public MessageType[] messageTypes() {
      return messageTypes.clone();
    }

    @Override
    public boolean requiresConversation() {
      return requiresConversation;
    }

    @Override
    public String toString() {
      final StringBuilder sb = new StringBuilder("BaseMessageHandler{");
      sb.append("patterns=").append(Arrays.toString(patterns));
      sb.append(", flags=").append(flags);
      sb.append(", messageTypes=").append(Arrays.toString(messageTypes));
      sb.append(", requiresConversation=").append(requiresConversation);
      sb.append('}');
      return sb.toString();
    }
  }

  class FriendlyMessageHandler extends BaseMessageHandler {

    private final List<Pattern> compiledPatterns;

    public FriendlyMessageHandler(MessageHandler annotation) throws PatternSyntaxException {
      super(annotation.name(), annotation.patterns(), annotation.flags(),
            annotation.messageTypes(), annotation.requiresConversation());
      this.compiledPatterns = Arrays.stream(patterns())
          .map(pattern -> Pattern.compile(pattern, flags()))
          .collect(Collectors.toList());
    }

    public FriendlyMessageHandler(AnnotationAttributes annotationAttributes) throws PatternSyntaxException {
      super(annotationAttributes);

      this.compiledPatterns = Arrays.stream(patterns())
          .map(pattern -> Pattern.compile(pattern, flags()))
          .collect(Collectors.toList());
    }

    public FriendlyMessageHandler(Map<String,Object> annotationAttributes) throws PatternSyntaxException {
      super(annotationAttributes);

      this.compiledPatterns = Arrays.stream(patterns())
          .map(pattern -> Pattern.compile(pattern, flags()))
          .collect(Collectors.toList());
    }

    public List<Pattern> getCompiledPatterns() {
      return compiledPatterns;
    }

    public List<MessageType> getMessageTypes() {
      return Arrays.asList(messageTypes());
    }

  }
}
