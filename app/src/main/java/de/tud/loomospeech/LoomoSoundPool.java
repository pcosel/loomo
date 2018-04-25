package de.tud.loomospeech;

import android.content.Context;
import android.media.AudioManager;
import android.media.SoundPool;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;


public class LoomoSoundPool implements SoundPool.OnLoadCompleteListener {
    private SoundPool soundPool;
    private Context ctx;
    private HashMap<String, Integer> NamedSounds = new HashMap<>(10);
    private ArrayList<Integer> SoundIds = new ArrayList<>();

    public LoomoSoundPool(Context context) {
        ctx = context;
        soundPool = new SoundPool.Builder().setMaxStreams(10).build();
        soundPool.setOnLoadCompleteListener(this);

        NamedSounds.put("error", soundPool.load(ctx, R.raw.error, 2));
        NamedSounds.put("success", soundPool.load(ctx, R.raw.success, 1));
        NamedSounds.put("listening", soundPool.load(ctx, R.raw.listening, 1));
        NamedSounds.put("fart", soundPool.load(ctx, R.raw.fart, 1));
    }

    public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
        if (status == 0) { // success
            this.SoundIds.add(sampleId);
        }
    }

    int getVolume() {
        int volume = 50;
        AudioManager audioManager = (AudioManager) ctx.getSystemService(Context.AUDIO_SERVICE);

        if (audioManager != null) {
            // In Android 6 there would be an AudioManager.STREAM_NOTIFICATION, but it's not adjustable in Android 5.
            volume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        } else {
            Log.d("LoomoSoundPool", "Could not read system notification volume. Using 50%.");
        }

        return (volume * 10) % 100;
    }

    void addSound(String soundName, int resId) {
        addSound(soundName, resId, 1);
    }

    void addSound(String soundName, int resId, int priority) {
        NamedSounds.put(soundName, soundPool.load(ctx, resId, priority));
    }

    int play(String soundName) {
        return play(soundName, getVolume());
    }

    int play(String soundName, int volume) {
        int sampleID = NamedSounds.containsKey(soundName) ? NamedSounds.get(soundName) : -1;

        if (sampleID != -1 && SoundIds.contains(sampleID)) {
            return soundPool.play(sampleID, volume, volume, 1, 0, 1f);
        }

        return 0;
    }
}
