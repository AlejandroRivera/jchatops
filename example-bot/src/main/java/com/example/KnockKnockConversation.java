package com.example;

import io.arivera.oss.jchatops.annotations.MessageHandler;
import io.arivera.oss.jchatops.responders.Response;

import com.github.seratch.jslack.api.model.Message;
import org.springframework.stereotype.Service;

import java.util.regex.MatchResult;

@Service
public class KnockKnockConversation {

    @MessageHandler(patterns = "knock knock")
    public Response someoneKnocking(Response response) {
      return response.message("who's there?\n_(Try responding in a thread!)_")
          .followingUpWith("name");
    }

    @MessageHandler(name = "name", patterns = "(.*)", requiresConversation = true)
    public Response whoIsThere(MatchResult matches, Response response) {
      return response.message(matches.group(0) + " who?\n_(Try starting a new conversation in the main chat in parallel to this!)_")
          .followingUpWith("punch_line");
    }

    @MessageHandler(name = "punch_line", patterns = ".*", requiresConversation = true)
    public Response whatJollyGoodFun(Message msg, Response response) {
      return response.message(msg.getText() + "! ha-ha!")
          .alsoPostToMainConversation()
          .resettingConversation();
    }

}
