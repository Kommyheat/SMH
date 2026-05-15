package com.app.smh.alarm;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;

import java.util.Locale;

public class TtsHelper {

    private TextToSpeech tts;

    public void speak(Context context, String message) {  // static 제거
        tts = new TextToSpeech(context.getApplicationContext(), status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(Locale.KOREAN);
                tts.setSpeechRate(0.95f);

                tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                    @Override
                    public void onStart(String utteranceId) {}

                    @Override
                    public void onDone(String utteranceId) {
                        // 두 번째 발화가 끝난 후에만 shutdown
                        if ("medication_tts_2".equals(utteranceId)) {
                            tts.shutdown();
                        }
                    }

                    @Override
                    public void onError(String utteranceId) {
                        tts.shutdown();
                    }
                });

                tts.speak(message, TextToSpeech.QUEUE_FLUSH, null, "medication_tts_1");

                // 두 번째 발화: QUEUE_ADD (첫 번째 끝난 후 이어서 재생)
                tts.speak(message, TextToSpeech.QUEUE_ADD, null, "medication_tts_2");
            }
        });
    }
}
