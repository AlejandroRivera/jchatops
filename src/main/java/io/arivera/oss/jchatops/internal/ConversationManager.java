package io.arivera.oss.jchatops.internal;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Scope("singleton")
@Component
public class ConversationManager {

  private Map<ConversationKey, ConversationContext> conversations = new HashMap<>();

  public Optional<ConversationContext> getConversation(String userId, String channel) {
    return Optional.ofNullable(conversations.get(new ConversationKey(userId, channel)));
  }

  public void saveConversation(String userId, String channel, ConversationContext context) {
    conversations.put(new ConversationKey(userId, channel), context);
  }

  public void clearConversation(String userId, String channel) {
    conversations.remove(new ConversationKey(userId, channel));
  }

  private class ConversationKey {
    private String userId;
    private String channelId;

    public ConversationKey(String userId, String channelId) {
      this.userId = userId;
      this.channelId = channelId;
    }

    public String getUserId() {
      return userId;
    }

    public String getChannelId() {
      return channelId;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      ConversationKey that = (ConversationKey) o;
      return Objects.equals(userId, that.userId) &&
             Objects.equals(channelId, that.channelId);
    }

    @Override
    public int hashCode() {
      return Objects.hash(userId, channelId);
    }
  }
}
