package com.example;

import io.arivera.oss.jchatops.MessageFilter;
import io.arivera.oss.jchatops.annotations.MessageGraph;
import io.arivera.oss.jchatops.responders.Response;

import com.github.seratch.jslack.api.methods.request.chat.ChatPostMessageRequest;
import com.github.seratch.jslack.api.model.Message;
import com.github.seratch.jslack.api.model.User;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Component
@Scope("prototype")
public class MessageAuthorizerFilter extends MessageFilter {

  private static final Logger LOGGER = LoggerFactory.getLogger(MessageAuthorizerFilter.class);

  private final Set<String> adminEmails;
  private final String adminChannel;
  private final User sender;
  private final Response response;

  @Autowired
  public MessageAuthorizerFilter(@Value("${slackbot.example.auth_filter.order:2048}") int order,
                                 @Value("${adminEmail}") String adminEmail,
                                 @Value("${adminChannel}") String adminChannel,
                                 @MessageGraph User sender,
                                 Response response) {
    super(order);
    this.adminEmails = new HashSet<>(Arrays.asList(adminEmail));
    this.adminChannel = adminChannel;
    this.sender = sender;
    this.response = response;
  }

  @Override
  public Optional<Response> apply(Message message) {
    User userInContext = setAuthContext();

    try {
      return this.getNextFilter().apply(message);
    } catch (Exception e) {
      Throwable rootCause = ExceptionUtils.getRootCause(e);
      if (rootCause instanceof AccessDeniedException) {
        return handleAccessDenied(userInContext, message);
      }
      throw e;
    }
  }

  private User setAuthContext() {
    UsernamePasswordAuthenticationToken user;

    if (adminEmails.contains(sender.getProfile().getEmail())) {
      List<GrantedAuthority> grantedAuthorities = AuthorityUtils.commaSeparatedStringToAuthorityList("ROLE_ADMIN");
      user = new UsernamePasswordAuthenticationToken(sender, "N/A", grantedAuthorities);
    } else {
      List<GrantedAuthority> grantedAuthorities = AuthorityUtils.commaSeparatedStringToAuthorityList("ROLE_NONE");
      user = new UsernamePasswordAuthenticationToken(sender, "N/A", grantedAuthorities);
    }

    LOGGER.info("User auth: {}", user);
    SecurityContextHolder.getContext().setAuthentication(user);
    return sender;
  }

  private Optional<Response> handleAccessDenied(User userInContext, Message message) {
    return Optional.of(
        response.messages(
            ChatPostMessageRequest.builder()
                .asUser(true)
                .text("Nuh huh! You can't do that!")
                .build(),
            ChatPostMessageRequest.builder()
                .asUser(true)
                .text(String.format("Not to be a tattletale but '%s' just messaged me saying: '%s'",
                    userInContext.getName(), message.getText()))
                .channel(adminChannel)
                .build()));
  }

}
