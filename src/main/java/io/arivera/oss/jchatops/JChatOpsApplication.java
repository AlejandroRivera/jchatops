package io.arivera.oss.jchatops;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication()
public class JChatOpsApplication {

  private static final Logger LOGGER = LoggerFactory.getLogger(JChatOpsApplication.class);

  public static void main(String[] args) {
    SpringApplication.run(JChatOpsApplication.class, args);
  }

}
