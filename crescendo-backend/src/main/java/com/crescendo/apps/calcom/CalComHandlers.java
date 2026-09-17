package com.crescendo.apps.calcom;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.utils.RestClient;
import org.springframework.stereotype.Component;

@Component
public class CalComHandlers {

    private String getBaseUrl() {
        return "https://api.cal.com/v1";
    }

    private String getAuthToken(ActionContext context) {
        String token = context.getCredential("accessToken");
        if (token != null && !token.isBlank()) return token;
        return context.getCredential("apiKey");
    }

    @ActionMapping(appKey = "calcom", actionKey = "calcom:booking:get")
    public Object getBookings(ActionContext context) throws Exception {
        String token = getAuthToken(context);
        return RestClient.builder()
                .url(getBaseUrl() + "/bookings?apiKey=" + token)
                .header("Authorization", "Bearer " + token)
                .get()
                .execute();
    }

    @ActionMapping(appKey = "calcom", actionKey = "calcom:booking:cancel")
    public Object cancelBooking(ActionContext context) throws Exception {
        String bookingId = context.getString("bookingId");
        String token = getAuthToken(context);
        return RestClient.builder()
                .url(getBaseUrl() + "/bookings/" + bookingId + "/cancel?apiKey=" + token)
                .header("Authorization", "Bearer " + token)
                .delete()
                .execute();
    }
}
