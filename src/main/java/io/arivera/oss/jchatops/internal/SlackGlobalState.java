package io.arivera.oss.jchatops.internal;

import com.github.seratch.jslack.api.methods.response.rtm.RTMStartResponse;
import com.github.seratch.jslack.api.model.Channel;
import com.github.seratch.jslack.api.model.Group;
import com.github.seratch.jslack.api.model.Im;
import com.github.seratch.jslack.api.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.stream.Collectors;

@Component
@Scope("singleton")
public class SlackGlobalState {

  protected RTMStartResponse rtmStartResponse;

  @Autowired
  public SlackGlobalState(RTMStartResponse rtmStartResponse) {
    this.rtmStartResponse = rtmStartResponse;
  }

  @Bean
  @Scope("singleton")
  public User getBot() {
    return rtmStartResponse.getSelf();
  }

  @Bean
  @Scope("singleton")
  public Map<String, Channel> getChannelsMap() {
    return rtmStartResponse.getChannels().stream()
        .collect(Collectors.toMap(Channel::getId, channel -> channel));
  }

  @Bean
  @Scope("singleton")
  public Map<String, User> getUserMap() {
    return rtmStartResponse.getUsers().stream()
        .collect(Collectors.toMap(User::getId, user -> user));
  }

  @Bean
  @Scope("singleton")
  public Map<String, Im> getInstantMessagesMap() {
    return rtmStartResponse.getIms().stream()
        .collect(Collectors.toMap(Im::getId, im -> im));
  }

  @Bean
  @Scope("singleton")
  public Map<String, Group> getGroupChatsMap() {
    return rtmStartResponse.getGroups().stream()
        .collect(Collectors.toMap(Group::getId, group -> group));
  }

}
