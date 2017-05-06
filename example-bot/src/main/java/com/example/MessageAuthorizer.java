package com.example;

import io.arivera.oss.jchatops.CustomMessagePreProcessor;
import io.arivera.oss.jchatops.MessageType;

import com.github.seratch.jslack.api.model.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MessageAuthorizer implements CustomMessagePreProcessor {

  private static final Logger LOGGER = LoggerFactory.getLogger(MessageAuthorizer.class);

  public Message process(Message message, MessageType messageType) {
    UsernamePasswordAuthenticationToken user;
    if (message.getUser().equalsIgnoreCase("U09SQE4RW")) {
      List<GrantedAuthority> grantedAuthorities = AuthorityUtils.commaSeparatedStringToAuthorityList("ROLE_ADMIN");
      user = new UsernamePasswordAuthenticationToken("user", "N/A", grantedAuthorities);
    } else {
      List<GrantedAuthority> grantedAuthorities = AuthorityUtils.commaSeparatedStringToAuthorityList("ROLE_NONE");
      user = new UsernamePasswordAuthenticationToken("unknown", "N/A", grantedAuthorities);
    }

    LOGGER.info("User auth: {}", user);
    SecurityContextHolder.getContext().setAuthentication(user);
    return message;
  }
}
