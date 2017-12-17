package com.example;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.internal.verification.VerificationModeFactory.times;

import io.arivera.oss.jchatops.MessageType;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.test.context.ContextConfiguration;

import java.util.stream.Stream;

@ContextConfiguration(classes = {UncaughtExceptionsTest.TestCommand.class})
public class UncaughtExceptionsTest extends BaseTest {

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
  public void exceptionInHandlerMethodGetsReported() {
    JsonObject messageReceived = new JsonObject();
    messageReceived.add("type", new JsonPrimitive("message"));
    messageReceived.add("text", new JsonPrimitive("bean error"));
    messageReceived.add("channel", new JsonPrimitive(slackState.getIms().get(0).getId()));
    messageReceived.add("user", new JsonPrimitive(slackState.getUsers().get(0).getId()));
    messageReceived.add("team", new JsonPrimitive(slackState.getTeam().getId()));
    rtmClient.onMessage(gson.toJson(messageReceived));

    verify(rtmClient).sendMessage(messageCaptor.capture());

    Message message = gson.fromJson(messageCaptor.getValue(), Message.class);
    assertThat(message, notNullValue());
    assertThat(message.getText().toLowerCase(), containsString("uncaught exception"));
    assertThat(message.getAttachments().get(0).getText(), containsString(TestCommand.BEAN_EXCEPTION_MSG));
  }

  @Test
  public void exceptionInStreamDoesNotGetReported() {
    JsonObject messageReceived = new JsonObject();
    messageReceived.add("type", new JsonPrimitive("message"));
    messageReceived.add("text", new JsonPrimitive("lazy stream error"));
    messageReceived.add("channel", new JsonPrimitive(slackState.getIms().get(0).getId()));
    messageReceived.add("user", new JsonPrimitive(slackState.getUsers().get(0).getId()));
    messageReceived.add("team", new JsonPrimitive(slackState.getTeam().getId()));
    rtmClient.onMessage(gson.toJson(messageReceived));

    verify(rtmClient, times(1)).sendMessage(messageCaptor.capture());

    Message message = gson.fromJson(messageCaptor.getValue(), Message.class);
    assertThat(message, notNullValue());
    assertThat(message.getText().toLowerCase(), containsString("1"));
  }

  @Service
  public static class TestCommand {

    private static final String BEAN_EXCEPTION_MSG = "Sorry, my bad!";
    private static final String STREAM_EXCEPTION_MSG = "Yawn... are we there yet?";

    @MessageHandler(patterns = "bean error", messageTypes = {MessageType.PRIVATE})
    public Response throwError(Response response) {
      throw new RuntimeException(BEAN_EXCEPTION_MSG);
    }

    @MessageHandler(patterns = "lazy stream error", messageTypes = {MessageType.PRIVATE})
    public Response throwErrorInStream(Response response) {
      return response.message(
          Stream.of(1, 2)
              .map((n) -> {
                    if (n == 2) {
                      throw new IllegalStateException(STREAM_EXCEPTION_MSG);
                    }
                    return String.valueOf(n);
                  }
              )
      );
    }
  }

}
