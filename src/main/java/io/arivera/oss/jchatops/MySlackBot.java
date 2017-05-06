package io.arivera.oss.jchatops;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(
    scanBasePackageClasses = {JChatOpsApplication.class}
)
public class MySlackBot {

  public static void main(String[] args) {
    SpringApplication.run(MySlackBot.class, args);
  }

}
