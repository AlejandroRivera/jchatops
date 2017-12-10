package io.arivera.oss.jchatops.internal;

import io.arivera.oss.jchatops.annotations.Bot;
import io.arivera.oss.jchatops.annotations.BotGraph;

import com.github.seratch.jslack.api.methods.response.rtm.RTMStartResponse;
import com.github.seratch.jslack.api.model.Channel;
import com.github.seratch.jslack.api.model.Group;
import com.github.seratch.jslack.api.model.Im;
import com.github.seratch.jslack.api.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import java.util.Map;
import java.util.stream.Collectors;

@Configuration
@Scope("singleton")
public class SlackBotState {

  protected RTMStartResponse rtmStartResponse;

  @Autowired
  public SlackBotState(RTMStartResponse rtmStartResponse) {
    this.rtmStartResponse = rtmStartResponse;
  }

  /**
   * @return Slack bot user information.
   */
  @Bean
  @Scope("singleton")
  @Bot
  public User getBot() {
    return rtmStartResponse.getSelf();
  }

  /**
   * @return Association of Channel identifiers and Channel objects known for the current Slack team.
   */
  @Bean
  @Scope("singleton")
  @BotGraph
  public Map<String, Channel> getChannelsMap() {
    return rtmStartResponse.getChannels().stream()
        .collect(Collectors.toMap(Channel::getId, channel -> channel));
  }

  /**
   * @return Association of User identifiers and User objects known for the current Slack team.
   */
  @Bean
  @Scope("singleton")
  @BotGraph
  public Map<String, User> getUserMap() {
    return rtmStartResponse.getUsers().stream()
        .collect(Collectors.toMap(User::getId, user -> user));
  }

  /**
   * @return Association of Instant/Direct Message identifiers and IM/DM objects known by the current Slack bot.
   */
  @Bean
  @Scope("singleton")
  @BotGraph
  public Map<String, Im> getInstantMessagesMap() {
    return rtmStartResponse.getIms().stream()
        .collect(Collectors.toMap(Im::getId, im -> im));
  }

  /**
   * @return Association of Group Chat identifiers and Group Chat objects known by the current Slack bot.
   */
  @Bean
  @Scope("singleton")
  @BotGraph
  public Map<String, Group> getGroupChatsMap() {
    return rtmStartResponse.getGroups().stream()
        .collect(Collectors.toMap(Group::getId, group -> group));
  }

}
