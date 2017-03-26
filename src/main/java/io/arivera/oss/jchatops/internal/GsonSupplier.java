package io.arivera.oss.jchatops.internal;

import com.github.seratch.jslack.common.json.GsonFactory;
import com.google.gson.Gson;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

@Component
@Scope("singleton")
public class GsonSupplier implements Supplier<Gson> {

  private final Gson gson;

  public GsonSupplier() {
    gson = GsonFactory.createSnakeCase();
  }

  @Bean
  @Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
  public Gson get(){
    return gson;
  }

}
