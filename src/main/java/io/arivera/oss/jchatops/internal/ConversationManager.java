package io.arivera.oss.jchatops.internal;

import io.arivera.oss.jchatops.responders.Response;

import com.github.seratch.jslack.api.model.Message;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Scope("singleton")
@Component
public class ConversationManager {

  private Map<ConversationKey, ConversationContext> conversations = new ConcurrentHashMap<>();

  public Optional<ConversationContext> getConversation(Message message) {

    String user = message.getUser();
    String channel = message.getChannel();
    Optional<String> parentThreadId = Optional.ofNullable(message.getThreadTs());
    ConversationKey threadedConversationKey = new ConversationKey(user, channel, parentThreadId);

    ConversationContext theadedConversationContext = conversations.get(threadedConversationKey);
    if (theadedConversationContext != null) {
      return Optional.of(theadedConversationContext);
    }

    ConversationKey rootConversationKey = new ConversationKey(message.getUser(), message.getChannel(), Optional.empty());
    ConversationContext conversationContext = conversations.get(rootConversationKey);

    return Optional.ofNullable(conversationContext);
  }

  public void saveConversation(Message incomingMessage, Response response, ConversationContext context) {
    String parentThreadId = null;
    Boolean forceThread = response.shouldRespondInThread().orElse(false);
    if (forceThread) {
      parentThreadId = Optional.ofNullable(incomingMessage.getThreadTs()).orElse(incomingMessage.getTs());
    } else {
      if (incomingMessage.getThreadTs() != null) {
        parentThreadId = incomingMessage.getThreadTs();
      }
    }

    ConversationKey previousKey = context.getConversationKey();
    previousKey.setParentThreadId(Optional.ofNullable(parentThreadId));
    conversations.put(previousKey, context);
  }

  public void clearConversation(String userId, String channel, Optional<String> parentThreadId) {
    conversations.remove(new ConversationKey(userId, channel, parentThreadId));
  }

  public static class ConversationKey {

    private final String userId;
    private final String channelId;

    private Optional<String> parentThreadId;

    public ConversationKey(String userId, String channelId, Optional<String> parentThreadId) {
      this.userId = userId;
      this.channelId = channelId;
      this.parentThreadId = parentThreadId;
    }

    public ConversationKey setParentThreadId(Optional<String> parentThreadId) {
      this.parentThreadId = parentThreadId;
      return this;
    }

    @Override
    public boolean equals(Object other) {
      if (this == other) {
        return true;
      }
      if (other == null || getClass() != other.getClass()) {
        return false;
      }
      ConversationKey that = (ConversationKey) other;
      return Objects.equals(userId, that.userId)
             && Objects.equals(channelId, that.channelId)
             && Objects.equals(parentThreadId, that.parentThreadId);
    }

    @Override
    public int hashCode() {
      return Objects.hash(userId, channelId);
    }
  }


}
