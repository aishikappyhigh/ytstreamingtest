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

package com.google.android.apps.watchme;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import androidx.core.app.ActivityCompat;

/**
 * @author Ibrahim Ulukaya <ulukaya@google.com>
 * <p/>
 * AudioFrameGrabber class which records audio.
 */
public class AudioFrameGrabber {
    private Thread thread;
    private boolean cancel = false;
    private int frequency;
    private FrameCallback frameCallback;

    public void setFrameCallback(FrameCallback callback) {
        frameCallback = callback;
    }

    /**
     * Starts recording.
     *
     * @param frequency - Recording frequency.
     * @param context
     */
    public void start(int frequency, Context context) {
        Log.d("aishik", "start");

        this.frequency = frequency;

        cancel = false;
        thread = new Thread(new Runnable() {
            @Override
            public void run() {
                recordThread(context);
            }
        });
        thread.start();
    }

    /**
     * Records audio and pushes to buffer.
     *
     * @param context
     */
    public void recordThread(Context context) {
        Log.d("aishik", "recordThread");

        int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;
        int channelConfiguration = AudioFormat.CHANNEL_CONFIGURATION_MONO;
        int bufferSize = AudioRecord.getMinBufferSize(frequency, channelConfiguration, audioEncoding);
        Log.d("aishik", "AudioRecord buffer size: " + bufferSize);

        // 16 bit PCM stereo recording was chosen as example.

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        Log.d("aishik", "recordThread: a1");
        AudioRecord recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, frequency, channelConfiguration,
                audioEncoding, bufferSize);
        recorder.startRecording();
        Log.d("aishik", "recordThread: a");
        // Make bufferSize be in samples instead of bytes.
        bufferSize /= 2;
        short[] buffer = new short[bufferSize];
        Log.d("aishik", "recordThread: b");
        while (!cancel) {
            int bufferReadResult = recorder.read(buffer, 0, bufferSize);
            // Utils.Debug("bufferReadResult: " + bufferReadResult);
            Log.d("aishik", "recordThread: c");
            if (bufferReadResult > 0) {
                frameCallback.handleFrame(buffer, bufferReadResult);
            } else if (bufferReadResult < 0) {
                Log.d("aishik", "Error calling recorder.read: " + bufferReadResult);
            }
            Log.d("aishik", "recordThread: D");
        }
        recorder.stop();
        Log.d("aishik", "recordThread: E");

        Log.d("aishik", "exit recordThread");
    }

    /**
     * Stops recording.
     */
    public void stop() {
        Log.d("aishik", "stop");

        cancel = true;
        try {
            thread.join();
        } catch (InterruptedException e) {
            Log.d("aishik", "", e);
        }
    }

    public interface FrameCallback {
        void handleFrame(short[] audio_data, int length);
    }
}
