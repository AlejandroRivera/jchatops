package com.example;

import io.arivera.oss.jchatops.JChatOpsApplication;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackageClasses = {JChatOpsApplication.class, MySlackBot.class})
public class MySlackBot {

  public static void main(String[] args) {
    SpringApplication.run(MySlackBot.class, args);
  }

}
