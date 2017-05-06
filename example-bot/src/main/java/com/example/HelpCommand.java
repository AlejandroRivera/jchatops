package com.example;

import io.arivera.oss.jchatops.MessageHandler;
import io.arivera.oss.jchatops.ResponseSupplier;
import io.arivera.oss.jchatops.responders.Response;

import com.github.seratch.jslack.api.model.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

@Service
public class HelpCommand {

  private static final Logger LOGGER = LoggerFactory.getLogger(HelpCommand.class);

  @Bean
  @MessageHandler(patterns = "help")
  public ResponseSupplier help(Message message, Response response) {
    return () -> {
      try {
        Thread.sleep(5000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      return response.message("```\n"
                   + "* Say: 'hello'\n"
                   + "* Say: 'help\n"
                   + "```");
    };
  }

  @Bean
  @MessageHandler(patterns = "help hello")
  public ResponseSupplier helpHello(Message message, Response response) {
    return () -> response
        .message("> Hello\n"
                 + "Starts a conversation about your name age.");
  }
}
