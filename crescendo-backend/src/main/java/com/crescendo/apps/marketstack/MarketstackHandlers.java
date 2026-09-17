package com.crescendo.apps.marketstack;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.utils.RestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class MarketstackHandlers {

    @Value("${crescendo.platform.marketstack-api-key:}")
    private String defaultAccessKey;

    @ActionMapping(appKey = "marketstack", actionKey = "marketstack:eod:getLatest")
    public Object getLatestEod(ActionContext context) throws Exception {
        String symbols = context.getString("symbols");
        if (symbols == null || symbols.isBlank()) {
            throw new IllegalArgumentException("Marketstack symbols are required");
        }
        
        Integer limitObj = context.getInt("limit");
        int limit = limitObj != null ? limitObj : 10;

        String key = context.getCredential("accessKey");
        if (key == null || key.isBlank()) {
            key = defaultAccessKey;
        }
        
        return RestClient.builder()
                .url("https://api.marketstack.com/v1/eod/latest?access_key=" + key + "&symbols=" + symbols + "&limit=" + Math.max(1, limit))
                .header("User-Agent", "Mozilla/5.0")
                .get()
                .execute();
    }
}
