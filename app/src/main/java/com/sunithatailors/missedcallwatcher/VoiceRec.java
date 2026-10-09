package com.sunithatailors.missedcallwatcher;

import android.content.Context;
import android.media.MediaRecorder;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/** Records one short voice note at a time (AAC/MP4, mono, low bitrate so it stays small). */
public class VoiceRec {

    public static final int MAX_SECONDS = 60;

    private static MediaRecorder recorder;
    private static String recordingId;
    private static File recordingFile;
    private static final Map<String, File> saved = new HashMap<String, File>();

    public static String recordingId() { return recordingId; }

    public static boolean isRecording() { return recordingId != null; }

    public static File fileFor(String id) { return saved.get(id); }

    public static boolean start(Context c, String id) {
        if (recordingId != null) return false;
        try {
            File old = saved.remove(id);
            if (old != null) old.delete();
            File f = new File(c.getCacheDir(), "voice_" + System.currentTimeMillis() + ".m4a");
            MediaRecorder r = new MediaRecorder();
            r.setAudioSource(MediaRecorder.AudioSource.MIC);
            r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            r.setAudioChannels(1);
            r.setAudioSamplingRate(16000);
            r.setAudioEncodingBitRate(32000);
            r.setOutputFile(f.getAbsolutePath());
            r.prepare();
            r.start();
            recorder = r;
            recordingId = id;
            recordingFile = f;
            return true;
        } catch (Exception e) {
            cleanup();
            return false;
        }
    }

    /** Stops the current recording and keeps the file for that row. Returns false if it was too short/failed. */
    public static boolean stop() {
        if (recordingId == null) return false;
        String id = recordingId;
        File f = recordingFile;
        boolean ok = true;
        try {
            recorder.stop();
        } catch (Exception e) {
            ok = false;
        }
        cleanup();
        if (ok && f != null && f.length() > 0) {
            saved.put(id, f);
            return true;
        }
        if (f != null) f.delete();
        return false;
    }

    public static void discard(String id) {
        File f = saved.remove(id);
        if (f != null) f.delete();
    }

    private static void cleanup() {
        try {
            if (recorder != null) recorder.release();
        } catch (Exception ignored) {
        }
        recorder = null;
        recordingId = null;
        recordingFile = null;
    }
}
