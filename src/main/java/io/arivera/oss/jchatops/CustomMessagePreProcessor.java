package io.arivera.oss.jchatops;

import com.github.seratch.jslack.api.model.Message;

public interface CustomMessagePreProcessor {

  Message process(Message message, MessageType messageType);

}
