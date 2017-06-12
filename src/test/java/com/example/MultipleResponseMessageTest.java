package com.example;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.arivera.oss.jchatops.MessageHandler;
import io.arivera.oss.jchatops.MessageType;
import io.arivera.oss.jchatops.SlackMessage;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.test.context.ContextConfiguration;

import java.util.List;
import java.util.stream.Collectors;

@ContextConfiguration(classes = {MultipleResponseMessageTest.TestCommand.class})
public class MultipleResponseMessageTest extends BaseTest {

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
  public void testMultipleMessagesFromOneReceivedMessage() {
    JsonObject messageReceived = new JsonObject();
    messageReceived.add("type",     new JsonPrimitive("message"));
    messageReceived.add("text",     new JsonPrimitive("tequila"));
    messageReceived.add("channel",  new JsonPrimitive(slackState.getIms().get(0).getId()));
    messageReceived.add("user",     new JsonPrimitive(slackState.getUsers().get(0).getId()));
    messageReceived.add("team",     new JsonPrimitive(slackState.getTeam().getId()));

    rtmClient.onMessage(gson.toJson(messageReceived));
    verify(rtmClient, times(4)).sendMessage(messageCaptor.capture());

    List<String> messages = messageCaptor.getAllValues();
    List<JsonObject> responseMsgs = messages.stream()
        .map(msg -> gson.fromJson(msg, JsonObject.class))
        .collect(Collectors.toList());

    assertThat(responseMsgs.get(0).get("text").getAsString(), equalTo("One tequila..."));
    assertThat(responseMsgs.get(1).get("text").getAsString(), equalTo("Two tequilas..."));
    assertThat(responseMsgs.get(2).get("text").getAsString(), equalTo("Three tequilas..."));
    assertThat(responseMsgs.get(3).get("text").getAsString(), equalTo("Floor!"));
  }

  @Test
  public void testMultipleMessagesDeliveredToDifferentChannels() {
    JsonObject messageReceived = new JsonObject();
    messageReceived.add("type",     new JsonPrimitive("message"));
    messageReceived.add("text",     new JsonPrimitive("channels"));
    messageReceived.add("channel",  new JsonPrimitive(slackState.getIms().get(0).getId()));
    messageReceived.add("user",     new JsonPrimitive(slackState.getUsers().get(0).getId()));
    messageReceived.add("team",     new JsonPrimitive(slackState.getTeam().getId()));

    rtmClient.onMessage(gson.toJson(messageReceived));
    verify(rtmClient, times(2)).sendMessage(messageCaptor.capture());

    List<String> messages = messageCaptor.getAllValues();
    List<JsonObject> responseMsgs = messages.stream()
        .map(msg -> gson.fromJson(msg, JsonObject.class))
        .collect(Collectors.toList());

    assertThat(responseMsgs.get(0).get("text").getAsString(), equalTo("msg1"));
    assertThat(responseMsgs.get(0).get("channel").getAsString(), equalTo(messageReceived.get("channel").getAsString()));
    assertThat(responseMsgs.get(1).get("text").getAsString(), equalTo("msg2"));
    assertThat(responseMsgs.get(1).get("channel").getAsString(), equalTo("channel2"));
  }

  @Service
  public static class TestCommand {

    @MessageHandler(patterns = "tequila", messageTypes = MessageType.PRIVATE)
    public Response tequila(Message message, Response response) {
      return response
          .message(
              "One tequila...",
              "Two tequilas...",
              "Three tequilas...",
              "Floor!");
    }

    @MessageHandler(patterns = "channels", messageTypes = MessageType.PRIVATE)
    public Response foo(Message message, Response response) {
      return response
          .message(
              SlackMessage.builder()
                  .setText("msg1")
                  .build(),
              SlackMessage.builder()
                  .setText("msg2")
                  .setChannel("channel2")
                  .build());
    }

  }

}
