package io.arivera.oss.jchatops.internal;

import static io.arivera.oss.jchatops.misc.MoreCollectors.toLinkedMap;

import com.github.seratch.jslack.api.model.Im;
import com.github.seratch.jslack.api.model.Message;
import com.github.seratch.jslack.api.model.User;
import com.github.seratch.jslack.api.rtm.RTMClient;
import com.github.seratch.jslack.api.rtm.RTMMessageHandler;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.arivera.oss.jchatops.MessageHandler;
import io.arivera.oss.jchatops.MessageType;
import io.arivera.oss.jchatops.ResponseSupplier;
import io.arivera.oss.jchatops.responders.BasicResponder;
import io.arivera.oss.jchatops.responders.NoOpResponder;
import io.arivera.oss.jchatops.responders.Responder;
import io.arivera.oss.jchatops.responders.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.core.type.StandardMethodMetadata;
import org.springframework.stereotype.Component;
import org.springframework.web.context.support.GenericWebApplicationContext;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

@Component
@Scope("singleton")
public class SlackRtmMessagesHandler implements RTMMessageHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(SlackRtmMessagesHandler.class);

  private final RTMClient rtmClient;
  private final Gson gson;
  private final ApplicationContext applicationContext;
  private final Map<String, Im> ims;
  private final User bot;

  private final Map<String, FriendlyMessageHandler> messageHandlerSettings;
  private final Map<String, FriendlyMessageHandler> messageHandlerSettingsForNoConversations;

  private final ConversationManager conversationManager;

  private final NoOpResponder noOpResponder;
  private final BasicResponder basicResponder;

  @Autowired
  public SlackRtmMessagesHandler(RTMClient rtmClient,
                                 ApplicationContext applicationContext,
                                 GsonSupplier gsonSupplier,
                                 Map<String, Im> ims,
                                 User bot, ConversationManager conversationManager,
                                 BasicResponder basicResponder,
                                 NoOpResponder noOpResponder) {
    this.rtmClient = rtmClient;
    this.gson = gsonSupplier.get();
    this.applicationContext = applicationContext;
    this.ims = ims;
    this.bot = bot;
    this.basicResponder = basicResponder;
    this.noOpResponder = noOpResponder;
    this.messageHandlerSettings = new HashMap<>();
    this.messageHandlerSettingsForNoConversations = new HashMap<>();
    this.conversationManager = conversationManager;
  }

  @PostConstruct
  public void init() {
    registerMessageHandlers();
    rtmClient.addMessageHandler(this);
  }

  private void registerMessageHandlers() {
    Map<String, ResponseSupplier> beans = applicationContext.getBeansOfType(ResponseSupplier.class);

    beans.forEach((beanName, bean) -> {
//      BeanDefinition beanDefinition = ((AnnotationConfigEmbeddedWebApplicationContext) applicationContext)    // AnnotationConfigEmbeddedWebApplicationContext
//          .getBeanDefinition(beanName);

      BeanDefinition beanDefinition = ((GenericWebApplicationContext) applicationContext)    // AnnotationConfigEmbeddedWebApplicationContext
          .getBeanDefinition(beanName);

//      Map annotationAttributes = ((MethodMetadataReadingVisitor) beanDefinition.getSource())
//          .getAnnotationAttributes(MessageHandler.class.getName());

//      String[] patterns = (String[]) annotationAttributes.get(MessageHandler.PATTERNS_FIELD_NAME);
//      MessageType[] messageTypes = (MessageType[]) annotationAttributes.get(MessageHandler.MESSAGE_TYPES_FIELD_NAME);

      Annotation[] annotations = ((StandardMethodMetadata) beanDefinition.getSource()).getIntrospectedMethod().getDeclaredAnnotations();

      Arrays.stream(annotations)
          .filter(annotation -> annotation instanceof MessageHandler)
          .findFirst()
          .map(annotation -> (MessageHandler) annotation)
          .ifPresent(messageHandler -> {

            if (messageHandlerSettings.containsKey(beanName)) {
              throw new IllegalStateException("Bean with name '" + beanName + "' is already defined!");
            }

//            MessageHandler messageHandler = new MessageHandler(){
//              @Override
//              public Class<? extends Annotation> annotationType() {
//                return MessageHandler.class;
//              }
//
//              @Override
//              public String[] patterns() {
//                return patterns;
//              }
//
//              @Override
//              public MessageType[] messageTypes() {
//                return messageTypes;
//              }
//            };

            FriendlyMessageHandler messangeHandlerWrapper = new FriendlyMessageHandler(messageHandler);
            String[] patterns = messageHandler.patterns();

            Arrays.stream(patterns)
                .forEach(pattern -> {
                  try {
                    Pattern compiledPattern = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
                    messangeHandlerWrapper.getCompiledPatterns().add(compiledPattern);
                  } catch (PatternSyntaxException e) {
                    throw new IllegalArgumentException(
                        String.format("The patterns value '%s' found in '%s' of class '%s' is invalid.",
                            pattern, beanName, beanDefinition.getBeanClassName()), e
                    );
                  }
                });

            messageHandlerSettings.putIfAbsent(beanName, messangeHandlerWrapper);
            if (!messangeHandlerWrapper.requiresConversation()) {
              messageHandlerSettingsForNoConversations.putIfAbsent(beanName, messangeHandlerWrapper);
            }
            LOGGER.debug("Detected MessageHandler in bean named '{}': {}", beanName, messageHandler);
          });
    });
  }

  @PreDestroy
  public void close() {
    rtmClient.removeMessageHandler(this);
  }

  @Override
  public void handle(String jsonMessage) {
    try {
      doHandle(jsonMessage);
    } catch (RuntimeException e) {
      LOGGER.error("Uncaught exception processing message: {}", jsonMessage, e);
    }
  }

  private void doHandle(String jsonMessage) {
    JsonObject jsonObject = gson.fromJson(jsonMessage, JsonObject.class);
    if (jsonObject.get("type") == null) {
      LOGGER.debug("Message received without 'type': {}", jsonMessage);
      return;
    }

    Message rawMessage = gson.fromJson(jsonObject, Message.class);
    if (!rawMessage.getType().equalsIgnoreCase("message")) {
      LOGGER.debug("Message received but it's not an actual 'message': {}", jsonMessage);
      return;
    }

    MessageType currentMessageType = extractMessageType(rawMessage);
    LOGGER.debug("'{}' message received: {}", currentMessageType, rawMessage);

    Message message = preProcessMessage(rawMessage, currentMessageType);

    SlackMessageState.currentMessage.set(message);

    Optional<ConversationContext> conversation = conversationManager.getConversation(message.getUser(), message.getChannel());
    conversation.ifPresent(conversationContext -> SlackMessageState.currentConversation.set(conversationContext.getData()));

    Optional<ResponseSupplier> matchedResponder = Optional.empty();

    Map<String, FriendlyMessageHandler> map = getBeansAndSettingsToInspect(conversation);

    for (Map.Entry<String, FriendlyMessageHandler> entry : map.entrySet()) {
      String beanName = entry.getKey();

      FriendlyMessageHandler messageHandler = entry.getValue();
      LOGGER.debug("Inspecting bean '{}' to see if it can process the message...", beanName);

      if (shouldSkipCurrentBean(beanName, currentMessageType, conversation, messageHandler)) continue;

      Optional<Matcher> matchingMatcher = messageHandler.getCompiledPatterns().stream()
          .map(pattern -> pattern.matcher(message.getText()))
          .filter(Matcher::matches)
          .findFirst();

      if (matchingMatcher.isPresent()) {
        LOGGER.debug("Pattern '{}' matched in bean '{}'",
            matchingMatcher.get().pattern().pattern(), beanName);
        SlackMessageState.currentPatternMatch.set(matchingMatcher.get());

        ResponseSupplier bean = applicationContext.getBean(beanName, ResponseSupplier.class);
        matchedResponder = Optional.of(bean);
        break;
      } else {
        LOGGER.debug("Discarding bean '{}' because the patterns '{}' don't match the received message.",
            beanName, messageHandler.getCompiledPatterns());
      }
    }

    Responder responder = noOpResponder;
    Response response = null;

    if (matchedResponder.isPresent()) {
      response = matchedResponder.get().get();
      responder = basicResponder;
    } else {
      responder = noOpResponder;
    }
    responder.respondWith(response);
  }

  private boolean shouldSkipCurrentBean(String beanName,
                                        MessageType currentMessageType,
                                        Optional<ConversationContext> conversation,
                                        FriendlyMessageHandler messageHandler) {

    if (conversation.isPresent() && !conversation.get().getNextConversationBeanNames().contains(beanName)) {
      LOGGER.debug("Bean '{}' is not in the list of beans allowed for the current conversation: {}",
          beanName, conversation.get().getNextConversationBeanNames());
      return true;
    }

    if (!conversation.isPresent() && messageHandler.requiresConversation()) {
      LOGGER.debug("Bean '{}' requires a conversation to have already been started", beanName);
      return true;
    }

    if (!messageHandler.getMessageTypes().contains(currentMessageType)) {
      LOGGER.debug("Bean '{}' can only handle messages of type {}, not : {}",
          beanName, messageHandler.getMessageTypes(), currentMessageType);
      return true;
    }

    return false;
  }

  private Map<String, FriendlyMessageHandler> getBeansAndSettingsToInspect(Optional<ConversationContext> conversation) {
    return conversation
        .map(ConversationContext::getNextConversationBeanNames)
        .map(beanNames -> beanNames.stream().collect(toLinkedMap(Function.identity(), messageHandlerSettings::get)))
        .orElse(this.messageHandlerSettingsForNoConversations);
  }

  private Message preProcessMessage(Message message, MessageType currentMessageType) {
    if (currentMessageType == MessageType.TAGGED) {
      message.setText(message.getText().replaceAll("^\\s*<@" + bot.getId() + ">\\S*", "").trim());
    }
    return message;
  }

  private MessageType extractMessageType(Message message) {
    MessageType currentMessageType;
    if (ims.containsKey(message.getChannel())) {
      currentMessageType = MessageType.PRIVATE;
    } else if (message.getText().contains("<@" + bot.getId() + ">")) {
      currentMessageType = MessageType.TAGGED;
    } else {
      currentMessageType = MessageType.PUBLIC;
    }
    return currentMessageType;
  }

}
