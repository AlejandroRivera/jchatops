package io.arivera.oss.jchatops;

import com.github.seratch.jslack.api.model.Attachment;
import com.github.seratch.jslack.api.model.Message;
import com.github.seratch.jslack.api.model.Reaction;

import java.util.List;
import java.util.Objects;

/**
 * An extension of the JSlack {@link Message} class.
 *
 * <p>It's particularly useful for adding support for fields that aren't available in JSlack yet.
 */
public class SlackMessage extends Message {

  boolean replyBroadcast;

  public boolean isReplyBroadcast() {
    return replyBroadcast;
  }

  public SlackMessage setReplyBroadcast(boolean replyBroadcast) {
    this.replyBroadcast = replyBroadcast;
    return this;
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof SlackMessage)) {
      return false;
    }
    if (!super.equals(other)) {
      return false;
    }
    SlackMessage that = (SlackMessage) other;
    return replyBroadcast == that.replyBroadcast;
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), replyBroadcast);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private String type = "message";
    private String channel;
    private String user;
    private String text;
    private List<Attachment> attachments;
    private String ts;
    private String threadTs;
    private boolean starred;
    private boolean wibblr;
    private List<String> pinnedTo;
    private List<Reaction> reactions;
    private boolean replyBroadcast;

    public Builder setType(String type) {
      this.type = type;
      return this;
    }

    public Builder setChannel(String channel) {
      this.channel = channel;
      return this;
    }

    public Builder setUser(String user) {
      this.user = user;
      return this;
    }

    public Builder setText(String text) {
      this.text = text;
      return this;
    }

    public Builder setAttachments(List<Attachment> attachments) {
      this.attachments = attachments;
      return this;
    }

    public Builder setTs(String ts) {
      this.ts = ts;
      return this;
    }

    public Builder setReplyBroadcast(boolean replyBroadcast) {
      this.replyBroadcast = replyBroadcast;
      return this;
    }

    public Builder setThreadTs(String threadTs) {
      this.threadTs = threadTs;
      return this;
    }

    public Builder setStarred(boolean starred) {
      this.starred = starred;
      return this;
    }

    public Builder setWibblr(boolean wibblr) {
      this.wibblr = wibblr;
      return this;
    }

    public Builder setPinnedTo(List<String> pinnedTo) {
      this.pinnedTo = pinnedTo;
      return this;
    }

    public Builder setReactions(List<Reaction> reactions) {
      this.reactions = reactions;
      return this;
    }

    public SlackMessage build() {
      SlackMessage message = new SlackMessage();
      message.setType(type);
      message.setChannel(channel);
      message.setUser(user);
      message.setText(text);
      message.setAttachments(attachments);
      message.setTs(ts);
      message.setThreadTs(threadTs);
      message.setStarred(starred);
      message.setWibblr(wibblr);
      message.setPinnedTo(pinnedTo);
      message.setReactions(reactions);
      message.setReplyBroadcast(replyBroadcast);
      return message;
    }
  }
}
