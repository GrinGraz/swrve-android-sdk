package com.swrve.sdk;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.swrve.sdk.ISwrveCommon.EVENT_ID_KEY;
import static com.swrve.sdk.ISwrveCommon.GENERIC_EVENT_ACTION_TYPE_INFLUENCED;
import static com.swrve.sdk.ISwrveCommon.GENERIC_EVENT_ACTION_TYPE_KEY;
import static com.swrve.sdk.ISwrveCommon.GENERIC_EVENT_CAMPAIGN_TYPE_KEY;
import static com.swrve.sdk.ISwrveCommon.EVENT_TYPE_GENERIC_CAMPAIGN;
import static com.swrve.sdk.ISwrveCommon.GENERIC_EVENT_CAMPAIGN_TYPE_PUSH;
import static com.swrve.sdk.SwrveNotificationConstants.SWRVE_INFLUENCED_WINDOW_MINS_KEY;

public class SwrveCampaignInfluence {

    public static final String INFLUENCED_PREFS = "swrve.influenced_data";

    public List<InfluenceData> getSavedInfluencedData(SharedPreferences prefs) {
        Set<String> keys = prefs.getAll().keySet();
        ArrayList<InfluenceData> influencedData = new ArrayList<>();
        for (String trackingId : keys) {
            long maxInfluenceMillis = prefs.getLong(trackingId, 0);
            if (maxInfluenceMillis > 0) {
                influencedData.add(new InfluenceData(trackingId, maxInfluenceMillis));
            }
        }
        return influencedData;
    }

    public void saveInfluencedCampaign(Context context, String trackingId, Bundle msg, Date date) {
        if(msg == null || !msg.containsKey(SWRVE_INFLUENCED_WINDOW_MINS_KEY)) {
            SwrveLogger.d("Cannot save influence data because there's no influenced window set.");
            return;
        }

        if(SwrveHelper.isNullOrEmpty(trackingId)) {
            SwrveLogger.d("Cannot save influence data because cannot no tracking id.");
            return;
        }

        String influencedWindowMinsStr = msg.getString(SWRVE_INFLUENCED_WINDOW_MINS_KEY);
        int influencedWindowMins = Integer.parseInt(influencedWindowMinsStr);

        // Calculate the max time when this push will be considered influenced
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(Calendar.MINUTE, influencedWindowMins);
        Date influencedDate = cal.getTime();

        // Add the new push influenced data to the list
        InfluenceData newInfluenceData = new InfluenceData(trackingId, influencedDate.getTime());
        SharedPreferences sharedPreferences = context.getSharedPreferences(INFLUENCED_PREFS, Context.MODE_PRIVATE);
        List<InfluenceData> influencedData = getSavedInfluencedData(sharedPreferences);
        influencedData.add(newInfluenceData);

        // Save the list
        SharedPreferences.Editor edit = sharedPreferences.edit();
        for (InfluenceData influenceData : influencedData) {
            edit.putLong(influenceData.trackingId, influenceData.maxInfluencedMillis);
        }
        edit.commit();
    }

    public void removeInfluenceCampaign(Context context, String trackingId) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(INFLUENCED_PREFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor edit = sharedPreferences.edit();
        edit.remove(trackingId).commit();
    }

    public void processInfluenceData(Context context, ISwrveCommon swrveCommon) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(INFLUENCED_PREFS, Context.MODE_PRIVATE);
        List<InfluenceData> influencedArray = getSavedInfluencedData(sharedPreferences);
        if (!influencedArray.isEmpty()) {
            ArrayList<String> influencedEvents = new ArrayList<>();
            long nowMillis = getNow().getTime();
            for (InfluenceData influenceData : influencedArray) {
                try {
                    long deltaMillis = influenceData.maxInfluencedMillis - nowMillis;
                    if (deltaMillis >= 0 && influenceData.maxInfluencedMillis > 0) {
                        // We are still inside the influence window
                        Map<String, Object> parameters = new HashMap<>();
                        parameters.put(EVENT_ID_KEY, influenceData.getIntTrackingId());
                        parameters.put(GENERIC_EVENT_CAMPAIGN_TYPE_KEY, GENERIC_EVENT_CAMPAIGN_TYPE_PUSH);
                        parameters.put(GENERIC_EVENT_ACTION_TYPE_KEY, GENERIC_EVENT_ACTION_TYPE_INFLUENCED);
                        Map<String, String> payload = new HashMap<>();
                        // Add delta time in minutes
                        payload.put("delta", String.valueOf(deltaMillis / (1000 * 60)));

                        String eventAsJSON = EventHelper.eventAsJSON(EVENT_TYPE_GENERIC_CAMPAIGN, parameters, payload, swrveCommon.getNextSequenceNumber(), System.currentTimeMillis());
                        influencedEvents.add(eventAsJSON);
                    }
                } catch (JSONException e) {
                    SwrveLogger.e("Could not obtain push influenced data:", e);
                }
            }
            if (!influencedEvents.isEmpty()) {
                swrveCommon.sendEventsInBackground(context, swrveCommon.getUserId(), influencedEvents);
            }

            // Remove the influence data
            sharedPreferences.edit().clear().commit();
        }
    }

    protected Date getNow() {
        return new Date();
    }

    public class InfluenceData {
        String trackingId;
        long maxInfluencedMillis;

        public InfluenceData(String trackingId, long maxInfluenceMillis) {
            this.trackingId = trackingId;
            this.maxInfluencedMillis = maxInfluenceMillis;
        }

        public long getIntTrackingId() {
            return Long.parseLong(trackingId);
        }

        public JSONObject toJson() {
            try {
                JSONObject result = new JSONObject();
                result.put("trackingId", trackingId);
                result.put("maxInfluencedMillis", maxInfluencedMillis);
                return result;
            } catch (Exception e) {
                SwrveLogger.e("Could not serialize influence data:", e);
            }
            return null;
        }
    }
}
