package com.crescendo.apps.wordpress;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.utils.RestClient;
import org.springframework.stereotype.Component;

@Component
public class WordPressUserHandlers {

    @ActionMapping(appKey = "wordpress", actionKey = "wordpress:user:getMany")
    public Object getUsers(ActionContext context) throws Exception {
        return RestClient.builder()
                .url(WordPressSupport.getBaseUrl(context) + "/wp-json/wp/v2/users")
                .header("Authorization", WordPressSupport.getAuth(context))
                .get()
                .execute();
    }

    @ActionMapping(appKey = "wordpress", actionKey = "wordpress:user:get")
    public Object getUser(ActionContext context) throws Exception {
        String userId = context.getString("userId");
        return RestClient.builder()
                .url(WordPressSupport.getBaseUrl(context) + "/wp-json/wp/v2/users/" + userId)
                .header("Authorization", WordPressSupport.getAuth(context))
                .get()
                .execute();
    }
}
