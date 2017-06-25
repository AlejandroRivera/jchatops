package io.arivera.oss.jchatops.filters;

import io.arivera.oss.jchatops.MessageFilter;
import io.arivera.oss.jchatops.SlackMessage;
import io.arivera.oss.jchatops.responders.Response;

import com.github.seratch.jslack.api.model.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanInstantiationException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Optional;
import java.util.stream.Stream;

@Component
@Scope("prototype")
public class UncaughtExceptionFilter extends MessageFilter {

  private static final Logger LOGGER = LoggerFactory.getLogger(UncaughtExceptionFilter.class);

  private final Response response;
  private String messagePrefix;

  @Autowired
  public UncaughtExceptionFilter(@Value("${jchatops.filters.uncaught_exception.order:1000}") int order,
                                 @Value("${jchatops.filters.uncaught_exception.message:Uncaught exception processing message}")
                                     String messagePrefix,
                                 Response response) {
    super(order);
    this.messagePrefix = messagePrefix;
    this.response = response;
  }

  @Override
  public Optional<Response> apply(Message message) {
    try {
      return getNextFilter().apply(message);
    } catch (Exception e) {
      Throwable exceptionToReport;
      if (e instanceof BeanCreationException) {
        exceptionToReport = e.getCause();
        if (exceptionToReport instanceof BeanInstantiationException) {
          exceptionToReport = exceptionToReport.getCause();
        }
      } else {
        exceptionToReport = e;
      }

      LOGGER.warn("Uncaught exception", exceptionToReport);
      return Optional.of(
          response.messages(Stream.of(
              buildMessageWithExceptionInfo(exceptionToReport)
          )));
    }


  }

  private SlackMessage buildMessageWithExceptionInfo(Throwable exceptionToReport) {

    return SlackMessage.builder()
        .setText(messagePrefix + "\n"
                 + "```\n" + getStackTrace(exceptionToReport) + "\n```")
        .build();
  }

  private String getStackTrace(Throwable e) {
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    e.printStackTrace(pw);
    return sw.toString();
  }

}