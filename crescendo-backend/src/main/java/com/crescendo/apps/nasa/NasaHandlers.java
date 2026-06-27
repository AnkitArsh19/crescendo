package com.crescendo.apps.nasa;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.utils.RestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class NasaHandlers {

    @Value("${crescendo.platform.nasa-api-key:}")
    private String platformApiKey;

    private String getApiKey(ActionContext context) {
        if (platformApiKey != null && !platformApiKey.isBlank()) {
            return platformApiKey;
        }
        return context.getCredential("apiKey");
    }

    @ActionMapping(appKey = "nasa", actionKey = "nasa:apod:get")
    public Object getApod(ActionContext context) throws Exception {
        String apiKey = getApiKey(context);
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException("NASA APOD requires an API key");
        }

        String date = context.getString("date");
        StringBuilder url = new StringBuilder("https://api.nasa.gov/planetary/apod?api_key=" + apiKey);
        if (date != null && !date.isBlank()) {
            url.append("&date=").append(date);
        }

        return RestClient.builder()
                .url(url.toString())
                .get()
                .execute();
    }

    @ActionMapping(appKey = "nasa", actionKey = "nasa:mars:getPhotos")
    public Object getMarsPhotos(ActionContext context) throws Exception {
        String apiKey = getApiKey(context);
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException("NASA Mars Photos requires an API key");
        }

        String rover = (String) context.configuration().getOrDefault("rover", "curiosity");
        String sol = (String) context.configuration().getOrDefault("sol", "1000");
        String camera = context.getString("camera");
        
        StringBuilder url = new StringBuilder("https://api.nasa.gov/mars-photos/api/v1/rovers/" + rover + "/photos?api_key=" + apiKey + "&sol=" + sol);
        if (camera != null && !camera.isBlank()) {
            url.append("&camera=").append(camera);
        }

        return RestClient.builder()
                .url(url.toString())
                .get()
                .execute();
    }
}
