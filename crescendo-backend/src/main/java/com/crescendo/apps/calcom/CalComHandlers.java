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

    private String getApiKey(ActionContext context) {
        return context.getCredential("apiKey");
    }

    @ActionMapping(appKey = "calcom", actionKey = "calcom:booking:get")
    public Object getBookings(ActionContext context) throws Exception {
        return RestClient.builder()
                .url(getBaseUrl() + "/bookings?apiKey=" + getApiKey(context))
                .get()
                .execute();
    }

    @ActionMapping(appKey = "calcom", actionKey = "calcom:booking:cancel")
    public Object cancelBooking(ActionContext context) throws Exception {
        String bookingId = context.getString("bookingId");
        return RestClient.builder()
                .url(getBaseUrl() + "/bookings/" + bookingId + "/cancel?apiKey=" + getApiKey(context))
                .delete()
                .execute();
    }
}
