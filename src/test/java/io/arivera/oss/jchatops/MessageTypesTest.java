package io.arivera.oss.jchatops;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.github.seratch.jslack.api.methods.response.rtm.RTMStartResponse;
import com.github.seratch.jslack.api.model.Message;
import com.github.seratch.jslack.api.rtm.RTMClient;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import io.arivera.oss.jchatops.internal.ConversationManager;
import io.arivera.oss.jchatops.internal.CustomMessageHandlersRegistrar;
import io.arivera.oss.jchatops.internal.GsonSupplier;
import io.arivera.oss.jchatops.internal.SlackGlobalState;
import io.arivera.oss.jchatops.internal.SlackMessageState;
import io.arivera.oss.jchatops.internal.SlackRtmMessagesHandler;
import io.arivera.oss.jchatops.responders.BasicResponder;
import io.arivera.oss.jchatops.responders.DefaultConversationHandlers;
import io.arivera.oss.jchatops.responders.NoOpResponder;
import io.arivera.oss.jchatops.responders.RespondersConfiguration;
import io.arivera.oss.jchatops.responders.Response;
import io.arivera.oss.jchatops.responders.SameChannelResponseProcessor;
import io.arivera.oss.jchatops.responders.TagUserResponseProcessor;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(
    classes = {
        SlackRealTimeMessagingMock.class,
        MessageTypesTest.HelloCommand.class,
        ConversationManager.class,
        Response.class,
        DefaultConversationHandlers.class,
        BasicResponder.class,
        NoOpResponder.class,
        RespondersConfiguration.class,
        SameChannelResponseProcessor.class,
        TagUserResponseProcessor.class,
        CustomMessageHandlersRegistrar.class,
        SlackRtmMessagesHandler.class,
        SlackGlobalState.class,
        SlackMessageState.class,
        GsonSupplier.class,
    }
)
public class MessageTypesTest {

  private static final Logger LOGGER = LoggerFactory.getLogger(MessageTypesTest.class);

  @Autowired
  RTMClient rtmClient;
  @Autowired
  RTMStartResponse slackState;

  @Captor
  ArgumentCaptor<String> messageCaptor;

  @Autowired
  Gson gson;

  @Before
  public void setUp() {
    Mockito.reset(rtmClient);
    MockitoAnnotations.initMocks(this);
  }


  @Test
  public void testChatroomMessageMatchingPatternDoesNotGetProcessed() {
    JsonObject messageReceived = new JsonObject();
    messageReceived.add("type",     new JsonPrimitive("message"));
    messageReceived.add("text",     new JsonPrimitive("foo"));
    messageReceived.add("channel",  new JsonPrimitive(slackState.getChannels().get(0).getId()));
    messageReceived.add("user",     new JsonPrimitive(slackState.getUsers().get(0).getId()));
    messageReceived.add("team",     new JsonPrimitive(slackState.getTeam().getId()));

    rtmClient.onMessage(gson.toJson(messageReceived));
    verify(rtmClient, never()).sendMessage(anyString());
  }

  @Test
  public void testTaggedMessageMatchingPatternDoesNotGetProcessed() {
    JsonObject messageReceived = new JsonObject();
    messageReceived.add("type",     new JsonPrimitive("message"));
    messageReceived.add("text",     new JsonPrimitive("<@"+slackState.getSelf().getId()+">: foo"));
    messageReceived.add("channel",  new JsonPrimitive(slackState.getChannels().get(0).getId()));
    messageReceived.add("user",     new JsonPrimitive(slackState.getUsers().get(0).getId()));
    messageReceived.add("team",     new JsonPrimitive(slackState.getTeam().getId()));

    rtmClient.onMessage(gson.toJson(messageReceived));
    verify(rtmClient, never()).sendMessage(anyString());
  }

  @Test
  public void testTaggedMessageMatchingPatternDoesGetProcessed() {
    JsonObject messageReceived = new JsonObject();
    messageReceived.add("type",     new JsonPrimitive("message"));
    messageReceived.add("text",     new JsonPrimitive("<@"+slackState.getSelf().getId()+">: bar"));
    messageReceived.add("channel",  new JsonPrimitive(slackState.getChannels().get(0).getId()));
    messageReceived.add("user",     new JsonPrimitive(slackState.getUsers().get(0).getId()));
    messageReceived.add("team",     new JsonPrimitive(slackState.getTeam().getId()));

    rtmClient.onMessage(gson.toJson(messageReceived));
    verify(rtmClient).sendMessage(anyString());
  }

  @Test
  public void testTaggedMessageMatchingPattern2DoesGetProcessed() {
    JsonObject messageReceived = new JsonObject();
    messageReceived.add("type",     new JsonPrimitive("message"));
    messageReceived.add("text",     new JsonPrimitive(" <@"+slackState.getSelf().getId()+">, bar"));
    messageReceived.add("channel",  new JsonPrimitive(slackState.getChannels().get(0).getId()));
    messageReceived.add("user",     new JsonPrimitive(slackState.getUsers().get(0).getId()));
    messageReceived.add("team",     new JsonPrimitive(slackState.getTeam().getId()));

    rtmClient.onMessage(gson.toJson(messageReceived));
    verify(rtmClient).sendMessage(anyString());
  }

  @Test
  public void testTaggedMessageMatchingPattern3DoesGetProcessed() {
    JsonObject messageReceived = new JsonObject();
    messageReceived.add("type",     new JsonPrimitive("message"));
    messageReceived.add("text",     new JsonPrimitive("<@"+slackState.getSelf().getId()+"> bar"));
    messageReceived.add("channel",  new JsonPrimitive(slackState.getChannels().get(0).getId()));
    messageReceived.add("user",     new JsonPrimitive(slackState.getUsers().get(0).getId()));
    messageReceived.add("team",     new JsonPrimitive(slackState.getTeam().getId()));

    rtmClient.onMessage(gson.toJson(messageReceived));
    verify(rtmClient).sendMessage(anyString());
  }

  @Test
  public void testTaggedMessageMatchingPattern4DoesNotGetProcessed() {
    JsonObject messageReceived = new JsonObject();
    messageReceived.add("type",     new JsonPrimitive("message"));
    messageReceived.add("text",     new JsonPrimitive("bar, <@"+slackState.getSelf().getId()+">"));
    messageReceived.add("channel",  new JsonPrimitive(slackState.getChannels().get(0).getId()));
    messageReceived.add("user",     new JsonPrimitive(slackState.getUsers().get(0).getId()));
    messageReceived.add("team",     new JsonPrimitive(slackState.getTeam().getId()));

    rtmClient.onMessage(gson.toJson(messageReceived));
    verify(rtmClient, never()).sendMessage(anyString());
  }

  @Test
  public void testPrivateMessageIsProcessed() {
    JsonObject messageReceived = new JsonObject();
    messageReceived.add("type",     new JsonPrimitive("message"));
    messageReceived.add("text",     new JsonPrimitive("foo"));
    messageReceived.add("channel",  new JsonPrimitive(slackState.getIms().get(0).getId()));
    messageReceived.add("user",     new JsonPrimitive(slackState.getUsers().get(0).getId()));
    messageReceived.add("team",     new JsonPrimitive(slackState.getTeam().getId()));

    rtmClient.onMessage(gson.toJson(messageReceived));
    verify(rtmClient).sendMessage(messageCaptor.capture());

    String message = messageCaptor.getValue();
    assertThat(message, notNullValue());

    JsonObject json = gson.fromJson(message, JsonObject.class);

    assertThat(json.get("text").getAsString(), containsString("foo back"));
  }

  @Service
  public static class HelloCommand {

    @Bean
    @MessageHandler(patterns = "foo", messageTypes = MessageType.PRIVATE)
    public ResponseSupplier foo(Message message, Response response) {
      return () -> response
          .message(message.getText() + " back!");
    }

    @Bean
    @MessageHandler(patterns = "bar", messageTypes = MessageType.TAGGED)
    public ResponseSupplier bar(Message message, Response response) {
      return () -> response
          .message(message.getText() + " back!");
    }

    @Bean
    @MessageHandler(patterns = "baz", messageTypes = MessageType.PUBLIC)
    public ResponseSupplier baz(Message message, Response response) {
      return () -> response
          .message(message.getText() + " back!");
    }
  }

}
