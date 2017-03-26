package io.arivera.oss.jchatops.internal;

import io.arivera.oss.jchatops.ConversationData;

import java.util.ArrayList;
import java.util.List;

public class ConversationContext {

  public ConversationData data = new ConversationData();

  public List<String> nextConversationBeanNames = new ArrayList<>();

  public ConversationContext() {
  }

  public ConversationData getData() {
    return data;
  }

  public ConversationContext setData(ConversationData data) {
    this.data = data;
    return this;
  }

  public List<String> getNextConversationBeanNames() {
    return nextConversationBeanNames;
  }

  public ConversationContext setNextConversationBeanNames(List<String> nextConversationBeanNames) {
    this.nextConversationBeanNames = nextConversationBeanNames;
    return this;
  }

}
