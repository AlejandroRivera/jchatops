package io.arivera.oss.jchatops.internal;

import io.arivera.oss.jchatops.MessageHandler;
import io.arivera.oss.jchatops.MessageType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

class FriendlyMessageHandler {

  private final List<Pattern> compiledPatterns;
  private final List<MessageType> messageTypes;
  private final boolean requiresConversation;

  FriendlyMessageHandler(MessageHandler messageHandler) {
    this.compiledPatterns = new ArrayList<>(messageHandler.patterns().length);
    this.messageTypes = Arrays.asList(messageHandler.messageTypes());
    this.requiresConversation = messageHandler.requiresConversation();
  }

  public List<Pattern> getCompiledPatterns() {
    return compiledPatterns;
  }

  public List<MessageType> getMessageTypes() {
    return messageTypes;
  }

  public boolean requiresConversation() {
    return requiresConversation;
  }
}
