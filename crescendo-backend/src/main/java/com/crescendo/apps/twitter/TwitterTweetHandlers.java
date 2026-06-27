package com.crescendo.apps.twitter;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.utils.RestClient;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class TwitterTweetHandlers {

    private String getBaseUrl() {
        return "https://api.twitter.com/2";
    }

    private String getAuth(ActionContext context) {
        return "Bearer " + context.getCredential("accessToken");
    }

    @ActionMapping(appKey = "twitter", actionKey = "post-tweet")
    public Object postTweet(ActionContext context) throws Exception {
        String text = context.getString("text");

        return RestClient.builder()
                .url(getBaseUrl() + "/tweets")
                .header("Authorization", getAuth(context))
                .header("Content-Type", "application/json")
                .post(Map.of("text", text))
                .execute();
    }

    @ActionMapping(appKey = "twitter", actionKey = "delete-tweet")
    public Object deleteTweet(ActionContext context) throws Exception {
        String tweetId = context.getString("tweetId");

        return RestClient.builder()
                .url(getBaseUrl() + "/tweets/" + tweetId)
                .header("Authorization", getAuth(context))
                .delete()
                .execute();
    }
}
