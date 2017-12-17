package io.arivera.oss.jchatops.filters;

import io.arivera.oss.jchatops.MessageFilter;
import io.arivera.oss.jchatops.responders.Response;

import com.github.seratch.jslack.api.methods.request.chat.ChatPostMessageRequest;
import com.github.seratch.jslack.api.model.Attachment;
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
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Component
@Scope("prototype")
public class UncaughtExceptionFilter extends MessageFilter {

  private static final Logger LOGGER = LoggerFactory.getLogger(UncaughtExceptionFilter.class);
  /**
   * This is required so the stacktrace is correctly shown as code, by using the triple back-tick characters.
   */
  private static final List<String> ATTACHMENT_HAS_MARKDOWN_IN = Collections.singletonList("text");

  private final String color;
  private final Response response;
  private final String messagePrefix;

  @Autowired
  public UncaughtExceptionFilter(@Value("${jchatops.filters.uncaught_exception.order:1000}") int order,
                                 @Value("${jchatops.filters.uncaught_exception.message:Uncaught exception processing message}")
                                     String messagePrefix,
                                 @Value("${jchatops.filters.uncaught_exception.color:#ff0000}") String color,
                                 Response response) {
    super(order);
    this.messagePrefix = messagePrefix;
    this.color = color;
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
      return Optional.of(response.messages(Stream.of(buildMessageWithExceptionInfo(exceptionToReport))));
    }

  }

  private ChatPostMessageRequest buildMessageWithExceptionInfo(Throwable exceptionToReport) {
    return ChatPostMessageRequest.builder()
        .asUser(true)
        .text(messagePrefix)
        .attachments(
            Collections.singletonList(
                Attachment.builder()
                    .title(exceptionToReport.getLocalizedMessage())
                    .text("```\n" + getStackTrace(exceptionToReport) + "```")
                    .color(color)
                    .mrkdwnIn(ATTACHMENT_HAS_MARKDOWN_IN)
                    .build()
            ))
        .build();
  }

  private String getStackTrace(Throwable throwable) {
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    throwable.printStackTrace(pw);
    return sw.toString();
  }

}