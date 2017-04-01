package io.arivera.oss.jchatops.internal;

import com.github.seratch.jslack.common.json.GsonFactory;
import com.google.gson.Gson;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import java.util.function.Supplier;

@Configuration
public class GsonSupplier implements Supplier<Gson> {

  private final Gson gson;

  public GsonSupplier() {
    gson = GsonFactory.createSnakeCase();
  }

  @Bean
  @Scope("singleton")
  public Gson get() {
    return gson;
  }

}
