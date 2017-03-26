package com.example;

import com.github.seratch.jslack.api.model.Message;
import io.arivera.oss.jchatops.MessageHandler;
import io.arivera.oss.jchatops.ResponseSupplier;
import io.arivera.oss.jchatops.responders.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;

@Service
public class HelloWorldCommand {

  private static final Logger LOGGER = LoggerFactory.getLogger(HelloWorldCommand.class);

  @Autowired
  Response response;

  @MessageHandler(patterns = {"hi", "hey"})
  public ResponseSupplier hey(Message message, Response response) {
    return () -> response.message("yo!");
  }

  @MessageHandler(patterns = "hello (?<name>.*)$")
  public ResponseSupplier helloWold(Message message, Matcher matcher){
    return ()-> {
      String response = String.format("hello %s? hello yourself!", message.getUser(), matcher.group("name"));
      return this.response.message(response);
    };
  }

}
