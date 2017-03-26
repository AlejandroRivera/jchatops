package io.arivera.oss.jchatops.responders;

import java.util.function.Function;

@FunctionalInterface
public interface ResponseProcessor extends Function<Response, Response> {

  @Override
  Response apply(Response message);
}
