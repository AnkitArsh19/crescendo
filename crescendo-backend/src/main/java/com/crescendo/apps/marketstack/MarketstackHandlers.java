package com.crescendo.apps.marketstack;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.utils.RestClient;
import org.springframework.stereotype.Component;

@Component
public class MarketstackHandlers {

    @ActionMapping(appKey = "marketstack", actionKey = "marketstack:eod:getLatest")
    public Object getLatestEod(ActionContext context) throws Exception {
        String symbols = context.getString("symbols");
        if (symbols == null || symbols.isBlank()) {
            throw new IllegalArgumentException("Marketstack symbols are required");
        }
        
        Integer limitObj = context.getInt("limit");
        int limit = limitObj != null ? limitObj : 10;
        
        return RestClient.builder()
                .url("https://api.marketstack.com/v1/eod/latest?access_key=" + context.getCredential("accessKey") + "&symbols=" + symbols + "&limit=" + Math.max(1, limit))
                .get()
                .execute();
    }
}
