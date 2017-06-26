package com.example;

import io.arivera.oss.jchatops.filters.BotTaggedMessageRemoverFilter;
import io.arivera.oss.jchatops.filters.SameChannelResponseFilter;
import io.arivera.oss.jchatops.filters.TagUserResponseFilter;
import io.arivera.oss.jchatops.filters.UncaughtExceptionFilter;
import io.arivera.oss.jchatops.filters.UnrecognizedCommandFilter;
import io.arivera.oss.jchatops.internal.BeanRegistryConfiguration;
import io.arivera.oss.jchatops.internal.ConversationManager;
import io.arivera.oss.jchatops.internal.CustomMessageHandlersRegistrar;
import io.arivera.oss.jchatops.internal.GsonSupplier;
import io.arivera.oss.jchatops.internal.MessageHandlerAsync;
import io.arivera.oss.jchatops.internal.MessageHandlerConfiguration;
import io.arivera.oss.jchatops.internal.MessageRouter;
import io.arivera.oss.jchatops.internal.MessagesHandler;
import io.arivera.oss.jchatops.internal.SlackBotState;
import io.arivera.oss.jchatops.internal.SlackMessageState;
import io.arivera.oss.jchatops.internal.SlackRealTimeMessagingMock;
import io.arivera.oss.jchatops.responders.CancelConversationHandlers;
import io.arivera.oss.jchatops.responders.Responder;
import io.arivera.oss.jchatops.responders.Response;

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
        MessageRouter.class,
        CancelConversationHandlers.class,
        Responder.class,
        UncaughtExceptionFilter.class,
        BotTaggedMessageRemoverFilter.class,
        UnrecognizedCommandFilter.class,
        SameChannelResponseFilter.class,
        TagUserResponseFilter.class,
        BeanRegistryConfiguration.class,
        CustomMessageHandlersRegistrar.class,
        MessagesHandler.class,
        SlackBotState.class,
        SlackMessageState.class,
        GsonSupplier.class,
    }
)
public abstract class BaseTest {

}
