package com.example;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import io.arivera.oss.jchatops.MessageHandler;
import io.arivera.oss.jchatops.MessageType;
import io.arivera.oss.jchatops.responders.Response;

import com.github.seratch.jslack.api.methods.response.rtm.RTMStartResponse;
import com.github.seratch.jslack.api.model.Message;
import com.github.seratch.jslack.api.rtm.RTMClient;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.junit.After;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(classes = {PatternsTest.HelloCommand.class})
public class PatternsTest extends BaseTest {

  private static final Logger LOGGER = LoggerFactory.getLogger(PatternsTest.class);

  @Autowired
  Gson gson;
  @Autowired
  RTMClient rtmClient;
  @Autowired
  RTMStartResponse slackState;
  @Captor
  ArgumentCaptor<String> messageCaptor;

  @After
  public void afterEachTest() {
    Mockito.reset(rtmClient);
  }

  @Test
  public void testNoMessage() {
    rtmClient.onMessage("{\"random\": true}");
    verify(rtmClient, never()).sendMessage(anyString());
  }

  @Test
  public void testChatroomMessageDoesNotMatchPattern() {
    JsonObject messageReceived = new JsonObject();
    messageReceived.add("type",     new JsonPrimitive("message"));
    messageReceived.add("text",     new JsonPrimitive("hey"));
    messageReceived.add("channel",  new JsonPrimitive(slackState.getChannels().get(0).getId()));
    messageReceived.add("user",     new JsonPrimitive(slackState.getUsers().get(0).getId()));
    messageReceived.add("team",     new JsonPrimitive(slackState.getTeam().getId()));

    rtmClient.onMessage(gson.toJson(messageReceived));
    verify(rtmClient, never()).sendMessage(anyString());
  }

  @Test
  public void testChatroomMessage() {
    JsonObject messageReceived = new JsonObject();
    messageReceived.add("type",     new JsonPrimitive("message"));
    messageReceived.add("text",     new JsonPrimitive("hello"));
    messageReceived.add("channel",  new JsonPrimitive(slackState.getChannels().get(0).getId()));
    messageReceived.add("user",     new JsonPrimitive(slackState.getUsers().get(0).getId()));
    messageReceived.add("team",     new JsonPrimitive(slackState.getTeam().getId()));

    rtmClient.onMessage(gson.toJson(messageReceived));
    verify(rtmClient).sendMessage(messageCaptor.capture());

    String message = messageCaptor.getValue();
    assertThat(message, notNullValue());

    JsonObject json = gson.fromJson(message, JsonObject.class);

    assertThat(json.get("text").getAsString(), containsString("hello back"));
    assertThat(json.get("text").getAsString(), containsString("<@" + messageReceived.get("user").getAsString() + ">"));
    assertThat(json.get("channel").getAsString(), containsString(messageReceived.get("channel").getAsString()));
  }

  @Test
  public void testPrivateMessage() {
    JsonObject messageReceived = new JsonObject();
    messageReceived.add("type",     new JsonPrimitive("message"));
    messageReceived.add("text",     new JsonPrimitive("hello"));
    messageReceived.add("channel",  new JsonPrimitive(slackState.getIms().get(0).getId()));
    messageReceived.add("user",     new JsonPrimitive(slackState.getUsers().get(0).getId()));
    messageReceived.add("team",     new JsonPrimitive(slackState.getTeam().getId()));

    rtmClient.onMessage(gson.toJson(messageReceived));
    verify(rtmClient).sendMessage(messageCaptor.capture());

    String message = messageCaptor.getValue();
    assertThat(message, notNullValue());

    JsonObject json = gson.fromJson(message, JsonObject.class);

    assertThat(json.get("text").getAsString(), containsString("hello back"));
    assertThat(json.get("text").getAsString(), not(containsString("<@" + messageReceived.get("user").getAsString() + ">")));
  }

  @Service
  public static class HelloCommand {

    @Bean
    @MessageHandler(patterns = "hello", messageTypes = {MessageType.PRIVATE, MessageType.PUBLIC, MessageType.TAGGED})
    public Response hey(Message message, Response response) {
      return response.message(message.getText() + " back!");
    }
  }

}
