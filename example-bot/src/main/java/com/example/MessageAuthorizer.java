package com.example;

import io.arivera.oss.jchatops.MessageFilter;
import io.arivera.oss.jchatops.responders.Response;

import com.github.seratch.jslack.api.model.Message;
import com.github.seratch.jslack.api.model.User;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.ApplicationContext;
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
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Component
@Scope("prototype")
public class MessageAuthorizer extends MessageFilter {

  private static final Logger LOGGER = LoggerFactory.getLogger(MessageAuthorizer.class);

  private final Map<String, User> users;
  private final BeanDefinitionRegistry beanDefinitionRegistry;
  private Set<String> adminEmails;

  @Autowired
  public MessageAuthorizer(@Value("${slackbot.example.auth_filter.order:1000}") int order,
                           ApplicationContext applicationContext,
                           BeanDefinitionRegistry beanDefinitionRegistry) {
    super(order);
    this.users = (Map<String, User>) applicationContext.getBean("getUserMap");
    this.beanDefinitionRegistry = beanDefinitionRegistry;
    this.adminEmails = new HashSet<>(Arrays.asList("alejandro@redmart.com"));
  }

  @Override
  public Optional<Response> apply(Message message) {
    UsernamePasswordAuthenticationToken user;

    User slackUser = users.get(message.getUser());
    if (adminEmails.contains(slackUser.getProfile().getEmail())) {
      List<GrantedAuthority> grantedAuthorities = AuthorityUtils.commaSeparatedStringToAuthorityList("ROLE_ADMIN");
      user = new UsernamePasswordAuthenticationToken(slackUser, "N/A", grantedAuthorities);
    } else {
      List<GrantedAuthority> grantedAuthorities = AuthorityUtils.commaSeparatedStringToAuthorityList("ROLE_NONE");
      user = new UsernamePasswordAuthenticationToken(slackUser, "N/A", grantedAuthorities);
    }

    LOGGER.info("User auth: {}", user);
    SecurityContextHolder.getContext().setAuthentication(user);
    try {
      return this.getNextFilter().apply(message);
    } catch (Exception e) {
      Throwable rootCause = ExceptionUtils.getRootCause(e);
      if (rootCause instanceof AccessDeniedException) {
        return Optional.of(
            new Response(message, beanDefinitionRegistry)
                .message("Nuh huh! You can't do that!")
        );
      }
      throw e;
    }
  }

}
