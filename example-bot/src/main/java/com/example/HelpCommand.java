package com.example;

import io.arivera.oss.jchatops.annotations.MessageHandler;
import io.arivera.oss.jchatops.responders.Response;

import com.github.seratch.jslack.api.model.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class HelpCommand {

  private static final Logger LOGGER = LoggerFactory.getLogger(HelpCommand.class);

  @MessageHandler(patterns = "help")
  public Response help(Message message, Response response) {
      return response.message("```\n"
                   + "* Say: 'hello'\n"
                   + "* Say: 'help'\n"
                   + "* Say: 'knock knock'\n"
                   + "```");
  }

  @MessageHandler(patterns = "help hello")
  public Response helpHello(Message message, Response response) {
    return response
        .message("> hello\n"
                 + "Starts a conversation about your name age.");
  }

  @MessageHandler(patterns = "help knock knock")
  public Response helpKnockKnock(Message message, Response response) {
    return response
        .message("> knock knock\n"
                 + "Starts a knock-knock joke dialog.");
  }
}
