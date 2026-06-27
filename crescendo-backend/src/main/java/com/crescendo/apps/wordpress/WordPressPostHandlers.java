package com.crescendo.apps.wordpress;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.utils.RestClient;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class WordPressPostHandlers {

    @ActionMapping(appKey = "wordpress", actionKey = "wordpress:post:create")
    public Object createPost(ActionContext context) throws Exception {
        Map<String, Object> body = new HashMap<>();
        
        String title = context.getString("title");
        if (title != null) body.put("title", title);
        
        String content = context.getString("content");
        if (content != null) body.put("content", content);
        
        String status = context.getString("status");
        if (status != null) body.put("status", status);

        return RestClient.builder()
                .url(WordPressSupport.getBaseUrl(context) + "/wp-json/wp/v2/posts")
                .header("Authorization", WordPressSupport.getAuth(context))
                .header("Content-Type", "application/json")
                .post(body)
                .execute();
    }

    @ActionMapping(appKey = "wordpress", actionKey = "wordpress:post:getMany")
    public Object getPosts(ActionContext context) throws Exception {
        return RestClient.builder()
                .url(WordPressSupport.getBaseUrl(context) + "/wp-json/wp/v2/posts")
                .header("Authorization", WordPressSupport.getAuth(context))
                .get()
                .execute();
    }

    @ActionMapping(appKey = "wordpress", actionKey = "wordpress:post:get")
    public Object getPost(ActionContext context) throws Exception {
        String postId = context.getString("postId");
        return RestClient.builder()
                .url(WordPressSupport.getBaseUrl(context) + "/wp-json/wp/v2/posts/" + postId)
                .header("Authorization", WordPressSupport.getAuth(context))
                .get()
                .execute();
    }

    @ActionMapping(appKey = "wordpress", actionKey = "wordpress:post:update")
    public Object updatePost(ActionContext context) throws Exception {
        String postId = context.getString("postId");
        Map<String, Object> body = new HashMap<>();
        
        String title = context.getString("title");
        if (title != null) body.put("title", title);
        
        String content = context.getString("content");
        if (content != null) body.put("content", content);
        
        String status = context.getString("status");
        if (status != null) body.put("status", status);

        return RestClient.builder()
                .url(WordPressSupport.getBaseUrl(context) + "/wp-json/wp/v2/posts/" + postId)
                .header("Authorization", WordPressSupport.getAuth(context))
                .header("Content-Type", "application/json")
                .post(body)
                .execute();
    }

    @ActionMapping(appKey = "wordpress", actionKey = "wordpress:post:delete")
    public Object deletePost(ActionContext context) throws Exception {
        String postId = context.getString("postId");
        return RestClient.builder()
                .url(WordPressSupport.getBaseUrl(context) + "/wp-json/wp/v2/posts/" + postId)
                .header("Authorization", WordPressSupport.getAuth(context))
                .delete()
                .execute();
    }
}
