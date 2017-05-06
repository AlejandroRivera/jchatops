package com.example;

import io.arivera.oss.jchatops.internal.ConversationManager;
import io.arivera.oss.jchatops.internal.CustomMessageHandlersTestRegistrar;
import io.arivera.oss.jchatops.internal.GsonSupplier;
import io.arivera.oss.jchatops.internal.MessageHandlerAsync;
import io.arivera.oss.jchatops.internal.MessageHandlerConfiguration;
import io.arivera.oss.jchatops.internal.MessagesHandler;
import io.arivera.oss.jchatops.internal.SlackGlobalState;
import io.arivera.oss.jchatops.internal.SlackMessageState;
import io.arivera.oss.jchatops.internal.SlackRealTimeMessagingMock;
import io.arivera.oss.jchatops.responders.BasicResponder;
import io.arivera.oss.jchatops.responders.DefaultConversationHandlers;
import io.arivera.oss.jchatops.responders.NoOpResponder;
import io.arivera.oss.jchatops.responders.RespondersConfiguration;
import io.arivera.oss.jchatops.responders.Response;
import io.arivera.oss.jchatops.responders.SameChannelResponseProcessor;
import io.arivera.oss.jchatops.responders.TagUserResponseProcessor;

import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(
    classes = {
        MessageHandlerConfiguration.class,
        MessageHandlerAsync.class,
        SlackRealTimeMessagingMock.class,
        ConversationManager.class,
        Response.class,
        DefaultConversationHandlers.class,
        BasicResponder.class,
        NoOpResponder.class,
        RespondersConfiguration.class,
        SameChannelResponseProcessor.class,
        TagUserResponseProcessor.class,
        CustomMessageHandlersTestRegistrar.class,
        MessagesHandler.class,
        SlackGlobalState.class,
        SlackMessageState.class,
        GsonSupplier.class,
    }
)
public abstract class BaseTest {

}
