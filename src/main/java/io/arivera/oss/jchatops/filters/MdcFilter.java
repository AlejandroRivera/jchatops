package io.arivera.oss.jchatops.filters;

import io.arivera.oss.jchatops.MessageFilter;
import io.arivera.oss.jchatops.annotations.BotGraph;
import io.arivera.oss.jchatops.responders.Response;

import com.github.seratch.jslack.api.model.Channel;
import com.github.seratch.jslack.api.model.Group;
import com.github.seratch.jslack.api.model.Im;
import com.github.seratch.jslack.api.model.Message;
import com.github.seratch.jslack.api.model.User;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
@Scope("prototype")
public class MdcFilter extends MessageFilter {

  private final Map<String, Channel> channels;
  private final Map<String, Group> groups;
  private final Map<String, User> users;
  private final Map<String, Im> instantMessages;

  @Autowired
  public MdcFilter(@Value("${jchatops.filters.unrecognized_command:50}") int order,
                   @BotGraph Map<String, Channel> channels,
                   @BotGraph Map<String, Group> groups,
                   @BotGraph Map<String, User> users,
                   @BotGraph Map<String, Im> instantMessages) {
    super(order);
    this.channels = channels;
    this.groups = groups;
    this.users = users;
    this.instantMessages = instantMessages;
  }

  @Override
  public Optional<Response> apply(Message message) {
    String userId = message.getUser();
    String username = Optional.ofNullable(users.get(userId)).map(u -> "@" + u.getName()).orElse(userId);
    MDC.put("msg_user", username);

    String channel;
    switch (message.getChannel().charAt(0)) {
      case 'G':
        channel = "#" + Optional.ofNullable(groups.get(message.getChannel())).map(Group::getName).orElse("UNKNOWN");
        break;
      case 'C':
        channel = "#" + Optional.ofNullable(channels.get(message.getChannel())).map(Channel::getName).orElse("UNKNOWN");
        break;
      case 'D':
        channel = "@" + Optional.ofNullable(instantMessages.get(message.getChannel()))
            .map(Im::getUser)
            .map(u -> Optional.ofNullable(users.get(u)).map(User::getName).orElse("UNKNOWN"))
            .orElse("UNKNOWN");
        break;
      default:
        channel = "?UNKNOWN";
    }

    MDC.put("msg_channel", channel);

    return getNextFilter().apply(message);
  }
}
