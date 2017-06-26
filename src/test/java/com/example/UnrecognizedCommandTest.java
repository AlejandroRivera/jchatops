package com.example;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;

import io.arivera.oss.jchatops.annotations.MessageHandler;
import io.arivera.oss.jchatops.filters.UnrecognizedCommandFilter;
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
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(classes = {UnrecognizedCommandTest.HelloCommand.class})
public class UnrecognizedCommandTest extends BaseTest{

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
  public void unrecognizedCommandIsAcknowledged() {
    JsonObject messageReceived = new JsonObject();
    messageReceived.add("type",     new JsonPrimitive("message"));
    messageReceived.add("text",     new JsonPrimitive("<@"+slackState.getSelf().getId()+"> hey"));
    messageReceived.add("channel",  new JsonPrimitive(slackState.getChannels().get(0).getId()));
    messageReceived.add("user",     new JsonPrimitive(slackState.getUsers().get(0).getId()));
    messageReceived.add("team",     new JsonPrimitive(slackState.getTeam().getId()));

    rtmClient.onMessage(gson.toJson(messageReceived));
    verify(rtmClient).sendMessage(messageCaptor.capture());

    Message message = gson.fromJson(messageCaptor.getValue(), Message.class);
    assertThat(message.getText(), containsString(UnrecognizedCommandFilter.UNRECOGNIZED_COMMAND_MESSAGE));
  }

  @Test
  public void unrecognizedCommandInConversationIsAcknowledged() {
    JsonObject messageReceived = new JsonObject();
    messageReceived.add("type",     new JsonPrimitive("message"));
    messageReceived.add("text",     new JsonPrimitive("<@"+slackState.getSelf().getId()+"> hello"));
    messageReceived.add("channel",  new JsonPrimitive(slackState.getChannels().get(0).getId()));
    messageReceived.add("user",     new JsonPrimitive(slackState.getUsers().get(0).getId()));
    messageReceived.add("team",     new JsonPrimitive(slackState.getTeam().getId()));

    rtmClient.onMessage(gson.toJson(messageReceived));
    verify(rtmClient).sendMessage(messageCaptor.capture());
    Mockito.reset(rtmClient);

    Message message = gson.fromJson(messageCaptor.getValue(), Message.class);
    assertThat(message.getText(), containsString("number"));
    assertThat(message.getText(), containsString("1 and 5"));

    messageReceived.add("text", new JsonPrimitive("<@"+slackState.getSelf().getId()+ "> Forty-two!"));

    rtmClient.onMessage(gson.toJson(messageReceived));
    verify(rtmClient).sendMessage(messageCaptor.capture());

    message = gson.fromJson(messageCaptor.getValue(), Message.class);
    assertThat(message.getText(), containsString(UnrecognizedCommandFilter.RESET_KEY_PHRASE));
  }

  @Configuration
  public static class HelloCommand {

    @MessageHandler(patterns = "hello")
    public Response hello(Message message, Response response) {
      return response
          .message("Guess what number, between 1 and 5, I'm thinking right now?")
          .followingUpWith("guessNumber");
    }

    @MessageHandler(patterns = "[1-5]", requiresConversation = true)
    public Response guessNumber(Message message, Response response) {
      return response
          .message("You got it!")
          .resettingConversation();
    }

  }

}
