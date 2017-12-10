package com.example;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;

import io.arivera.oss.jchatops.annotations.MessageHandler;
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
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(classes = {ConversationThreadTest.HelloCommand.class})
public class ConversationThreadTest extends BaseTest {

  private static final Logger LOGGER = LoggerFactory.getLogger(ConversationThreadTest.class);

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
  public void responseAsThread() {
    JsonObject msgToBot = new JsonObject();
    msgToBot.add("type", new JsonPrimitive("message"));
    msgToBot.add("text", new JsonPrimitive(String.format("<@%s>: thread the needle", slackState.getSelf().getId())));
    msgToBot.add("channel", new JsonPrimitive(slackState.getChannels().get(0).getId()));
    msgToBot.add("user", new JsonPrimitive(slackState.getUsers().get(0).getId()));
    msgToBot.add("team", new JsonPrimitive(slackState.getTeam().getId()));
    msgToBot.add("ts", new JsonPrimitive("parent_thread"));

    rtmClient.onMessage(gson.toJson(msgToBot));
    verify(rtmClient).sendMessage(messageCaptor.capture());

    Message responseFromBot = gson.fromJson(messageCaptor.getValue(), Message.class);

    assertThat(responseFromBot.getText(), containsString("needle's threaded."));
    assertThat(responseFromBot.getThreadTs(), equalTo("parent_thread"));
  }

  @Test
  public void conversationMovesToThread() {
    JsonObject msgToBot = new JsonObject();
    msgToBot.add("type", new JsonPrimitive("message"));
    msgToBot.add("text", new JsonPrimitive(String.format("<@%s>: knock knock", slackState.getSelf().getId())));
    msgToBot.add("channel", new JsonPrimitive(slackState.getChannels().get(0).getId()));
    msgToBot.add("user", new JsonPrimitive(slackState.getUsers().get(0).getId()));
    msgToBot.add("team", new JsonPrimitive(slackState.getTeam().getId()));
    msgToBot.add("ts", new JsonPrimitive(String.valueOf(System.currentTimeMillis())));

    rtmClient.onMessage(gson.toJson(msgToBot));
    verify(rtmClient).sendMessage(messageCaptor.capture());

    Message responseFromBot = gson.fromJson(messageCaptor.getValue(), Message.class);

    responseFromBot.setTs("parent_thread");
    assertThat(responseFromBot.getText(), containsString("who's there?"));
    Mockito.reset(rtmClient);

    msgToBot.add("text", new JsonPrimitive(String.format("<@%s>: it's me", slackState.getSelf().getId())));
    msgToBot.add("ts", new JsonPrimitive(String.valueOf(System.currentTimeMillis())));
    msgToBot.add("thread_ts", new JsonPrimitive(responseFromBot.getTs()));
    rtmClient.onMessage(gson.toJson(msgToBot));

    verify(rtmClient).sendMessage(messageCaptor.capture());
    responseFromBot = gson.fromJson(messageCaptor.getValue(), Message.class);
    assertThat(responseFromBot.getText(), containsString("it's me who?"));
    assertThat(responseFromBot.getThreadTs(), equalTo("parent_thread"));
    Mockito.reset(rtmClient);

    msgToBot.add("text", new JsonPrimitive(String.format("<@%s>: Mario!", slackState.getSelf().getId())));
    msgToBot.add("ts", new JsonPrimitive(String.valueOf(System.currentTimeMillis())));
    msgToBot.add("thread_ts", new JsonPrimitive(responseFromBot.getThreadTs()));
    rtmClient.onMessage(gson.toJson(msgToBot));

    verify(rtmClient).sendMessage(messageCaptor.capture());
    responseFromBot = gson.fromJson(messageCaptor.getValue(), Message.class);
    assertThat(responseFromBot.getText(), containsString("ha-ha"));
  }

  @Test
  public void threadedConversationIsIndependent() {
    JsonObject msgToBot = new JsonObject();
    msgToBot.add("type", new JsonPrimitive("message"));
    msgToBot.add("text", new JsonPrimitive(String.format("<@%s>: knock knock", slackState.getSelf().getId())));
    msgToBot.add("channel", new JsonPrimitive(slackState.getChannels().get(0).getId()));
    msgToBot.add("user", new JsonPrimitive(slackState.getUsers().get(0).getId()));
    msgToBot.add("team", new JsonPrimitive(slackState.getTeam().getId()));
    msgToBot.add("ts", new JsonPrimitive(String.valueOf(System.currentTimeMillis())));

    rtmClient.onMessage(gson.toJson(msgToBot));
    verify(rtmClient).sendMessage(messageCaptor.capture());

    Message responseFromBot = gson.fromJson(messageCaptor.getValue(), Message.class);

    responseFromBot.setTs("parent_thread");
    assertThat(responseFromBot.getText(), containsString("who's there?"));
    Mockito.reset(rtmClient);

    msgToBot.add("text", new JsonPrimitive(String.format("<@%s>: it's me", slackState.getSelf().getId())));
    msgToBot.add("ts", new JsonPrimitive(String.valueOf(System.currentTimeMillis())));
    msgToBot.add("thread_ts", new JsonPrimitive(responseFromBot.getTs()));
    rtmClient.onMessage(gson.toJson(msgToBot));

    verify(rtmClient).sendMessage(messageCaptor.capture());
    responseFromBot = gson.fromJson(messageCaptor.getValue(), Message.class);
    assertThat(responseFromBot.getText(), containsString("it's me who?"));
    assertThat(responseFromBot.getThreadTs(), equalTo("parent_thread"));
    Mockito.reset(rtmClient);

    // Conversation is now in a Thread. Let's start another knock-knock joke

    JsonObject secondConversation = new JsonObject();
    secondConversation.add("type", new JsonPrimitive("message"));
    secondConversation.add("text", new JsonPrimitive(String.format("<@%s>: knock knock", slackState.getSelf().getId())));
    secondConversation.add("channel", new JsonPrimitive(slackState.getChannels().get(0).getId()));
    secondConversation.add("user", new JsonPrimitive(slackState.getUsers().get(0).getId()));
    secondConversation.add("team", new JsonPrimitive(slackState.getTeam().getId()));
    secondConversation.add("ts", new JsonPrimitive(String.valueOf(System.currentTimeMillis())));

    rtmClient.onMessage(gson.toJson(secondConversation));
    verify(rtmClient).sendMessage(messageCaptor.capture());

    responseFromBot = gson.fromJson(messageCaptor.getValue(), Message.class);
    assertThat(responseFromBot.getText(), containsString("who's there?"));
    Mockito.reset(rtmClient);
  }

  @Configuration
  public static class HelloCommand {

    @MessageHandler(patterns = "thread the needle")
    public Response respondInThread(Message message, Response response) {
      return response.message("needle's threaded.")
          .inThread();
    }

    @MessageHandler(name = "knock", patterns = "knock knock")
    public Response hearKnock(Message message, Response response) {
      return response.message("who's there?")
          .followingUpWith("name");
    }

    @MessageHandler(name = "name", patterns = ".*", requiresConversation = true)
    public Response hearName(Message message, Response response) {
      return response.message(message.getText() + " who?")
          .followingUpWith("punch_line");
    }

    @MessageHandler(name = "punch_line", patterns = ".*", requiresConversation = true)
    public Response hearJoke(Response response) {
      return response.message("ha-ha!")
          .resettingConversation();
    }

  }

}
