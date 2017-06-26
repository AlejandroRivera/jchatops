package com.example;

import io.arivera.oss.jchatops.MessageHandler;
import io.arivera.oss.jchatops.MessageType;
import io.arivera.oss.jchatops.responders.Response;

import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;

@Service
public class SecuredConversation {

  @MessageHandler(patterns = "(.*)password(.*)?", messageTypes = {MessageType.PRIVATE, MessageType.TAGGED})
  @Secured("ROLE_ADMIN")
  public Response revealPassword(Response response) {
    return response
        .message("The password is: `" + RandomStringUtils.randomAlphanumeric(10) + "`");
  }

}
