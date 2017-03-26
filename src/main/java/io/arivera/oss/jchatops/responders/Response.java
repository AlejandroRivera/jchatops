package io.arivera.oss.jchatops.responders;

import com.github.seratch.jslack.api.model.Message;
import io.arivera.oss.jchatops.ResponseSupplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Scope("prototype")
public class Response {

  private final ApplicationContext applicationContext;
  private final Message originalMessage;

  private Message responseMessage;
  private boolean resetConversation;
  private List<String> conversationBeansToFollowUpWith = new ArrayList<>(0);

  @Autowired
  public Response(Message originalMessage, ApplicationContext applicationContext) {
    this.originalMessage = originalMessage;
    this.applicationContext = applicationContext;
  }

  public Response message(String message) {
    Message response = createResponseMessage(message);
    return message(response);
  }

  public Message getResponseMessage() {
    return responseMessage;
  }

  private Message createResponseMessage(String message) {
    Message response = new Message();
    response.setType("message");
    response.setText(message);
    return response;
  }

  /**
   * Message will be processed by the registered list of {@link ResponseProcessor}s before being submitted.
   */
  public Response message(Message message) {
    responseMessage = message;
    return this;
  }

  public Response resettingConversation() {
    resetConversation = true;
    return this;
  }

  public Response followingUpWith(String... beanNames) {
    this.conversationBeansToFollowUpWith = Arrays.stream(beanNames)
        .map(bean -> {
          if (applicationContext.getBean(bean, ResponseSupplier.class) == null) {
            throw new IllegalArgumentException(
                "Bean '" + bean + "' of type " + ResponseSupplier.class.getSimpleName() + " not found.");
          }
          return bean;
        })
        .collect(Collectors.toList());
    return this;
  }

  public boolean shouldResetConversation() {
    return resetConversation;
  }

  public List<String> getConversationBeansToFollowUpWith() {
    return conversationBeansToFollowUpWith;
  }

  public Message getOriginalMessage() {
    return originalMessage;
  }
}