package io.arivera.oss.jchatops;

import com.github.seratch.jslack.api.model.Attachment;
import com.github.seratch.jslack.api.model.Message;
import com.github.seratch.jslack.api.model.Reaction;

import java.util.List;

public class SlackMessage extends Message {

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
      return message;
    }
  }
}
