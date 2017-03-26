package io.arivera.oss.jchatops;

import io.arivera.oss.jchatops.responders.Response;

import java.util.function.Supplier;

public interface ResponseSupplier extends Supplier<Response> {

  Response get();

}
