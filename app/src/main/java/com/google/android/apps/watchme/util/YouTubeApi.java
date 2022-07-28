/*
 * Copyright (c) 2014 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.android.apps.watchme.util;

import android.util.Log;

import com.google.android.apps.watchme.MainActivity;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.util.DateTime;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.YouTube.LiveBroadcasts.Transition;
import com.google.api.services.youtube.model.CdnSettings;
import com.google.api.services.youtube.model.IngestionInfo;
import com.google.api.services.youtube.model.LiveBroadcast;
import com.google.api.services.youtube.model.LiveBroadcastContentDetails;
import com.google.api.services.youtube.model.LiveBroadcastListResponse;
import com.google.api.services.youtube.model.LiveBroadcastSnippet;
import com.google.api.services.youtube.model.LiveBroadcastStatus;
import com.google.api.services.youtube.model.LiveStream;
import com.google.api.services.youtube.model.LiveStreamListResponse;
import com.google.api.services.youtube.model.LiveStreamSnippet;
import com.google.api.services.youtube.model.MonitorStreamInfo;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

public class YouTubeApi {

    public static final String RTMP_URL_KEY = "rtmpUrl";
    public static final String BROADCAST_ID_KEY = "broadcastId";
    private static final int FUTURE_DATE_OFFSET_MILLIS = 5 * 1000;

    public static void createLiveEvent(YouTube youtube, String description,
                                       String name) {
        // We need a date that's in the proper ISO format and is in the future,
        // since the API won't
        // create events that start in the past.
        SimpleDateFormat dateFormat = new SimpleDateFormat(
                "yyyy-MM-dd'T'HH:mm:ss'Z'");
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        long futureDateMillis = System.currentTimeMillis()
                + FUTURE_DATE_OFFSET_MILLIS;
        Date futureDate = new Date();
        futureDate.setTime(futureDateMillis);
        String date = dateFormat.format(futureDate);

        Log.d("aishik", String.format(
                "Creating event: name='%s', description='%s', date='%s'.",
                name, description, date));

        try {
            Log.d("aishik", "createLiveEvent: a");
            LiveBroadcastSnippet broadcastSnippet = new LiveBroadcastSnippet();
            broadcastSnippet.setTitle(name);
            broadcastSnippet.setScheduledStartTime(new DateTime(futureDate));
            Log.d("aishik", "createLiveEvent: b");
            LiveBroadcastContentDetails contentDetails = new LiveBroadcastContentDetails();
            MonitorStreamInfo monitorStream = new MonitorStreamInfo();
            monitorStream.setEnableMonitorStream(false);
            Log.d("aishik", "createLiveEvent: c");
            contentDetails.setMonitorStream(monitorStream);
            // Create LiveBroadcastStatus with privacy status.
            LiveBroadcastStatus status = new LiveBroadcastStatus();
            Log.d("aishik", "createLiveEvent: d");
            status.setPrivacyStatus("unlisted");
            LiveBroadcast broadcast = new LiveBroadcast();
            broadcast.setKind("youtube#liveBroadcast");
            Log.d("aishik", "createLiveEvent: e");
            broadcast.setSnippet(broadcastSnippet);
            broadcast.setStatus(status);
            broadcast.setContentDetails(contentDetails);
            // Create the insert request
            Log.d("aishik", "createLiveEvent: f");
            YouTube.LiveBroadcasts.Insert liveBroadcastInsert = youtube
                    .liveBroadcasts().insert("snippet,status,contentDetails",
                            broadcast);

            // Request is executed and inserted broadcast is returned
            LiveBroadcast returnedBroadcast = liveBroadcastInsert.execute();
            Log.d("aishik", "createLiveEvent: g");

            // Create a snippet with title.
            LiveStreamSnippet streamSnippet = new LiveStreamSnippet();
            streamSnippet.setTitle(name);
            Log.d("aishik", "createLiveEvent: h");
            // Create content distribution network with format and ingestion
            // type.
            CdnSettings cdn = new CdnSettings();
            cdn.setResolution("variable");
            Log.d("aishik", "createLiveEvent: i");
            cdn.setFrameRate("variable");
            cdn.setIngestionType("rtmp");
            LiveStream stream = new LiveStream();
            stream.setKind("youtube#liveStream");
            Log.d("aishik", "createLiveEvent: j");
            stream.setSnippet(streamSnippet);
            stream.setCdn(cdn);

            Log.d("aishik", "createLiveEvent: k");
            // Create the insert request
            YouTube.LiveStreams.Insert liveStreamInsert = youtube.liveStreams()
                    .insert("snippet,cdn", stream);
            // Request is executed and inserted stream is returned
            Log.d("aishik", "createLiveEvent: l");
            LiveStream returnedStream = liveStreamInsert.execute();
            Log.d("aishik", "createLiveEvent: m");
            // Create the bind request
            YouTube.LiveBroadcasts.Bind liveBroadcastBind = youtube
                    .liveBroadcasts().bind(returnedBroadcast.getId(),
                            "id,contentDetails");
            // Set stream id to bind
            liveBroadcastBind.setStreamId(returnedStream.getId());
            Log.d("aishik", "createLiveEvent: n");
            // Request is executed and bound broadcast is returned
            liveBroadcastBind.execute();
            Log.d("aishik", "createLiveEvent: o");
        } catch (GoogleJsonResponseException e) {
            Log.d("aishik", "GoogleJsonResponseException code: " + e.getDetails().getCode() + " : " + e.getDetails().getMessage());
        } catch (IOException e) {
            Log.d("aishik","IOException: " + e.getMessage());
        } catch (Throwable t) {
            Log.d("aishik","Throwable: " + t.getStackTrace());
        }
    }

    // TODO: Catch those exceptions and handle them here.
    public static List<EventData> getLiveEvents(
            YouTube youtube) throws IOException {
        Log.d("aishik", "Requesting live events.");

        YouTube.LiveBroadcasts.List liveBroadcastRequest = youtube
                .liveBroadcasts().list("id,snippet,contentDetails");
        // liveBroadcastRequest.setMine(true);
        liveBroadcastRequest.setBroadcastStatus("upcoming");

        // List request is executed and list of broadcasts are returned
        LiveBroadcastListResponse returnedListResponse = liveBroadcastRequest.execute();

        // Get the list of broadcasts associated with the user.
        List<LiveBroadcast> returnedList = returnedListResponse.getItems();

        List<EventData> resultList = new ArrayList<>(returnedList.size());
        EventData event;

        for (LiveBroadcast broadcast : returnedList) {
            event = new EventData();
            event.setEvent(broadcast);
            String streamId = broadcast.getContentDetails().getBoundStreamId();
            Log.d("aishik", "getLiveEvents: "+event.getIngestionAddress());
            Log.d("aishik", "getLiveEvents: "+event.getThumbUri());
            Log.d("aishik", "getLiveEvents: "+event.getTitle());
            Log.d("aishik", "getLiveEvents: "+event.getWatchUri());
            Log.d("aishik", "getLiveEvents: "+event.getEvent());
            if (streamId != null) {
                String ingestionAddress = getIngestionAddress(youtube, streamId);
                event.setIngestionAddress(ingestionAddress);
            }
            resultList.add(event);
        }
        return resultList;
    }

    public static void startEvent(YouTube youtube, String broadcastId)
            throws IOException {

        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            Log.d("aishik", "", e);
        }

        Transition transitionRequest = youtube.liveBroadcasts().transition(
                "live", broadcastId, "status");
        transitionRequest.execute();
    }

    public static void endEvent(YouTube youtube, String broadcastId)
            throws IOException {
        Transition transitionRequest = youtube.liveBroadcasts().transition(
                "complete", broadcastId, "status");
        transitionRequest.execute();
    }

    public static String getIngestionAddress(YouTube youtube, String streamId)
            throws IOException {
        YouTube.LiveStreams.List liveStreamRequest = youtube.liveStreams()
                .list("cdn");
        liveStreamRequest.setId(streamId);
        LiveStreamListResponse returnedStream = liveStreamRequest.execute();

        List<LiveStream> streamList = returnedStream.getItems();
        if (streamList.isEmpty()) {
            return "";
        }
        IngestionInfo ingestionInfo = streamList.get(0).getCdn().getIngestionInfo();
        return ingestionInfo.getIngestionAddress() + "/"
                + ingestionInfo.getStreamName();
    }
}
