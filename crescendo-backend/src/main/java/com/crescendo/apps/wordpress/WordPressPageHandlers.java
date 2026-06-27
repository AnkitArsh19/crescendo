package com.crescendo.apps.wordpress;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.utils.RestClient;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class WordPressPageHandlers {

    @ActionMapping(appKey = "wordpress", actionKey = "wordpress:page:create")
    public Object createPage(ActionContext context) throws Exception {
        Map<String, Object> body = new HashMap<>();
        
        String title = context.getString("title");
        if (title != null) body.put("title", title);
        
        String content = context.getString("content");
        if (content != null) body.put("content", content);
        
        String status = context.getString("status");
        if (status != null) body.put("status", status);

        return RestClient.builder()
                .url(WordPressSupport.getBaseUrl(context) + "/wp-json/wp/v2/pages")
                .header("Authorization", WordPressSupport.getAuth(context))
                .header("Content-Type", "application/json")
                .post(body)
                .execute();
    }

    @ActionMapping(appKey = "wordpress", actionKey = "wordpress:page:getMany")
    public Object getPages(ActionContext context) throws Exception {
        return RestClient.builder()
                .url(WordPressSupport.getBaseUrl(context) + "/wp-json/wp/v2/pages")
                .header("Authorization", WordPressSupport.getAuth(context))
                .get()
                .execute();
    }

    @ActionMapping(appKey = "wordpress", actionKey = "wordpress:page:get")
    public Object getPage(ActionContext context) throws Exception {
        String pageId = context.getString("pageId");
        return RestClient.builder()
                .url(WordPressSupport.getBaseUrl(context) + "/wp-json/wp/v2/pages/" + pageId)
                .header("Authorization", WordPressSupport.getAuth(context))
                .get()
                .execute();
    }

    @ActionMapping(appKey = "wordpress", actionKey = "wordpress:page:update")
    public Object updatePage(ActionContext context) throws Exception {
        String pageId = context.getString("pageId");
        Map<String, Object> body = new HashMap<>();
        
        String title = context.getString("title");
        if (title != null) body.put("title", title);
        
        String content = context.getString("content");
        if (content != null) body.put("content", content);
        
        String status = context.getString("status");
        if (status != null) body.put("status", status);

        return RestClient.builder()
                .url(WordPressSupport.getBaseUrl(context) + "/wp-json/wp/v2/pages/" + pageId)
                .header("Authorization", WordPressSupport.getAuth(context))
                .header("Content-Type", "application/json")
                .post(body)
                .execute();
    }

    @ActionMapping(appKey = "wordpress", actionKey = "wordpress:page:delete")
    public Object deletePage(ActionContext context) throws Exception {
        String pageId = context.getString("pageId");
        return RestClient.builder()
                .url(WordPressSupport.getBaseUrl(context) + "/wp-json/wp/v2/pages/" + pageId)
                .header("Authorization", WordPressSupport.getAuth(context))
                .delete()
                .execute();
    }
}
