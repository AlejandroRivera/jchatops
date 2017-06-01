package com.example;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.arivera.oss.jchatops.ConversationData;
import io.arivera.oss.jchatops.MessageHandler;
import io.arivera.oss.jchatops.MessageType;
import io.arivera.oss.jchatops.responders.CancelConversationHandlers;
import io.arivera.oss.jchatops.responders.Response;

import com.github.seratch.jslack.api.methods.response.rtm.RTMStartResponse;
import com.github.seratch.jslack.api.model.Message;
import com.github.seratch.jslack.api.rtm.RTMClient;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;

import java.util.regex.Matcher;

@ContextConfiguration(classes = {ConversationTest.HelloCommand.class})
public class ConversationTest extends BaseTest {

  private static final Logger LOGGER = LoggerFactory.getLogger(ConversationTest.class);

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
  public void conversationWithAlternativeFlows() {
    JsonObject msgToBot = new JsonObject();
    msgToBot.add("type", new JsonPrimitive("message"));
    msgToBot.add("text", new JsonPrimitive(String.format("<@%s>: hello", slackState.getSelf().getId())));
    msgToBot.add("channel", new JsonPrimitive(slackState.getChannels().get(0).getId()));
    msgToBot.add("user", new JsonPrimitive(slackState.getUsers().get(0).getId()));
    msgToBot.add("team", new JsonPrimitive(slackState.getTeam().getId()));

    rtmClient.onMessage(gson.toJson(msgToBot));
    verify(rtmClient).sendMessage(messageCaptor.capture());
    String response = messageCaptor.getValue();
    assertThat(response, containsString("What is your name?"));

    msgToBot.add("text", new JsonPrimitive(String.format("<@%s>: my name is Alejandro", slackState.getSelf().getId())));
    rtmClient.onMessage(gson.toJson(msgToBot));
    verify(rtmClient, times(2)).sendMessage(messageCaptor.capture());
    response = messageCaptor.getValue();
    assertThat(response, containsString("How old are you?"));

    msgToBot.add("text", new JsonPrimitive(String.format("<@%s>: 100", slackState.getSelf().getId())));
    rtmClient.onMessage(gson.toJson(msgToBot));
    verify(rtmClient, times(3)).sendMessage(messageCaptor.capture());
    response = messageCaptor.getValue();
    assertThat(response, containsString("Alejandro"));
    assertThat(response, containsString("100"));

    msgToBot.add("text", new JsonPrimitive(String.format("<@%s>: no", slackState.getSelf().getId())));
    rtmClient.onMessage(gson.toJson(msgToBot));
    verify(rtmClient, times(4)).sendMessage(messageCaptor.capture());
    response = messageCaptor.getValue();
    assertThat(response, containsString("Oops"));
    assertThat(response, containsString("What is your name?"));

    msgToBot.add("text", new JsonPrimitive(String.format("<@%s>: Rivera is what they call me", slackState.getSelf().getId())));
    rtmClient.onMessage(gson.toJson(msgToBot));
    verify(rtmClient, times(5)).sendMessage(messageCaptor.capture());
    response = messageCaptor.getValue();
    assertThat(response, containsString("How old are you?"));

    msgToBot.add("text", new JsonPrimitive(String.format("<@%s>: 50", slackState.getSelf().getId())));
    rtmClient.onMessage(gson.toJson(msgToBot));
    verify(rtmClient, times(6)).sendMessage(messageCaptor.capture());
    response = messageCaptor.getValue();
    assertThat(response, containsString("Rivera"));
    assertThat(response, containsString("50"));

    msgToBot.add("text", new JsonPrimitive(String.format("<@%s>: yes", slackState.getSelf().getId())));
    rtmClient.onMessage(gson.toJson(msgToBot));
    verify(rtmClient, times(7)).sendMessage(messageCaptor.capture());
    response = messageCaptor.getValue();
    assertThat(response, containsString("Confirmed"));
  }

  @Test
  public void testUknownResponseAndCancel() {
    JsonObject msgToBot = new JsonObject();
    msgToBot.add("type", new JsonPrimitive("message"));
    msgToBot.add("text", new JsonPrimitive("hello"));
    msgToBot.add("channel", new JsonPrimitive(slackState.getIms().get(0).getId()));
    msgToBot.add("user", new JsonPrimitive(slackState.getUsers().get(0).getId()));
    msgToBot.add("team", new JsonPrimitive(slackState.getTeam().getId()));

    rtmClient.onMessage(gson.toJson(msgToBot));
    verify(rtmClient).sendMessage(messageCaptor.capture());
    String response = messageCaptor.getValue();
    assertThat(response, containsString("What is your name?"));

    msgToBot.add("text", new JsonPrimitive("my name is Alejandro"));
    rtmClient.onMessage(gson.toJson(msgToBot));
    verify(rtmClient, times(2)).sendMessage(messageCaptor.capture());
    response = messageCaptor.getValue();
    assertThat(response, containsString("How old are you?"));

    msgToBot.add("text", new JsonPrimitive("hello"));
    rtmClient.onMessage(gson.toJson(msgToBot));
    verify(rtmClient, times(3)).sendMessage(messageCaptor.capture());
    response = messageCaptor.getValue();
    assertThat(response, containsString("I did not understand that"));
    assertThat(response, containsString(CancelConversationHandlers.RESET_KEY_PHRASE));

    msgToBot.add("text", new JsonPrimitive(CancelConversationHandlers.RESET_KEY_PHRASE));
    rtmClient.onMessage(gson.toJson(msgToBot));
    verify(rtmClient, times(4)).sendMessage(messageCaptor.capture());
    response = messageCaptor.getValue();
    assertThat(response, containsString("start over"));

    msgToBot.add("text", new JsonPrimitive("hello"));
    rtmClient.onMessage(gson.toJson(msgToBot));
    verify(rtmClient, times(5)).sendMessage(messageCaptor.capture());
    response = messageCaptor.getValue();
    assertThat(response, containsString("What is your name?"));
  }

  @Configuration
  public static class HelloCommand {

    @Bean
    @MessageHandler(patterns = "hello", messageTypes = {MessageType.PRIVATE, MessageType.TAGGED})
    public Response hello(Message message, Response response) {
      return response
          .message(message.getText() + " back! What is your name?")
          .followingUpWith("getNameAndAskAge");
    }

    @Bean
    @MessageHandler(
        patterns = {
            "(?<name>\\w+)",
            "my name is (?<name>\\w+)",
            "(?<name>\\w+) is what they call me"
        },
        requiresConversation = true)
    public Response getNameAndAskAge(Response response, Matcher matcher, ConversationData conversation) {
        conversation.put("name", matcher.group("name"));
        return response
            .message("How old are you?")
            .followingUpWith("getAgeAndConfirm");
    }

    @Bean
    @MessageHandler(patterns = "\\d+", requiresConversation = true)
    public Response getAgeAndConfirm(Message message, Response response, ConversationData conversation) {
        conversation.put("age", message.getText());
        return response
            .message(String.format("You are '%s' and you are %s years old. Did I get that right?",
                conversation.get("name"),
                conversation.get("age")))
            .followingUpWith("correct", "incorrect");
    }

    @Bean
    @MessageHandler(patterns = "(no|no way|nope|not at all|wrong|incorrect)", requiresConversation = true)
    public Response incorrect(Response response) {
      return response
          .message("Oops! Let's start over... What is your name?")
          .followingUpWith("getNameAndAskAge");
    }

    @Bean
    @MessageHandler(patterns = "(yes|yup|correct|perfect|indeed|great)", requiresConversation = true)
    public Response correct(Response response) {
      return response.message("Confirmed!");
    }

  }


}
