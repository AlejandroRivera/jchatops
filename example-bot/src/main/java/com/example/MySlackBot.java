package com.example;

import io.arivera.oss.jchatops.JChatOpsApplication;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;

@SpringBootApplication(
    scanBasePackageClasses = {JChatOpsApplication.class, MySlackBot.class}
)
@EnableGlobalMethodSecurity(securedEnabled = true, prePostEnabled = true)
public class MySlackBot {

  public static void main(String[] args) {
    SpringApplication.run(MySlackBot.class, args);
  }

}
