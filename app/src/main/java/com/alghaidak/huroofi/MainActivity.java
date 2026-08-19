package com.alghaidak.huroofi;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import java.util.Locale;

public class MainActivity extends Activity {
    private static final int FILE_CHOOSER_REQUEST = 1001;

    private WebView webView;
    private ValueCallback<Uri[]> filePathCallback;
    private TextToSpeech tts;
    private boolean ttsReady = false;
    private boolean ttsInitFinished = false;
    private String pendingSpeechText;
    private String pendingSpeechLang;
    private ToneGenerator toneGenerator;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        webView = new WebView(this);
        webView.setBackgroundColor(Color.rgb(248, 246, 251));
        setContentView(webView);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setTextZoom(100);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                applyAndroidWebPatch();
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView webView,
                                             ValueCallback<Uri[]> filePathCallback,
                                             FileChooserParams fileChooserParams) {
                if (MainActivity.this.filePathCallback != null) {
                    MainActivity.this.filePathCallback.onReceiveValue(null);
                }
                MainActivity.this.filePathCallback = filePathCallback;
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("image/*");
                startActivityForResult(Intent.createChooser(intent, "اختر صورة الطالب"), FILE_CHOOSER_REQUEST);
                return true;
            }
        });

        try {
            toneGenerator = new ToneGenerator(AudioManager.STREAM_MUSIC, 95);
        } catch (Exception ignored) {
            toneGenerator = null;
        }

        tts = new TextToSpeech(this, status -> {
            ttsInitFinished = true;
            ttsReady = status == TextToSpeech.SUCCESS;
            if (ttsReady && tts != null) {
                try {
                    tts.setAudioAttributes(new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build());
                } catch (Exception ignored) {}
                tts.setSpeechRate(0.88f);
                tts.setPitch(1.05f);

                if (pendingSpeechText != null && !pendingSpeechText.trim().isEmpty()) {
                    String text = pendingSpeechText;
                    String lang = pendingSpeechLang;
                    pendingSpeechText = null;
                    pendingSpeechLang = null;
                    nativeSpeak(text, lang);
                }
            } else if (pendingSpeechText != null) {
                pendingSpeechText = null;
                pendingSpeechLang = null;
                Toast.makeText(this,
                        "تعذّر تشغيل خدمة النطق في الهاتف. تأكد من تفعيل تحويل النص إلى كلام.",
                        Toast.LENGTH_LONG).show();
            }
        });

        webView.addJavascriptInterface(new AndroidBridge(), "AndroidBridge");
        webView.loadUrl("file:///android_asset/index.html");
    }

    private void applyAndroidWebPatch() {
        if (webView == null) return;

        String js = "(function(){try{" +
                "window.speak=function(text,lang,options){" +
                "if(!text)return false;" +
                "try{if(window.AndroidBridge&&typeof window.AndroidBridge.speak==='function'){" +
                "window.AndroidBridge.speak(String(text),String(lang||'ar-SA'));return true;}}catch(e){}" +
                "return false;};" +

                "window.success=function(){" +
                "try{if(window.AndroidBridge&&typeof window.AndroidBridge.playSuccess==='function'){" +
                "window.AndroidBridge.playSuccess();return true;}}catch(e){}return false;};" +

                "var style=document.getElementById('android-ui-fix');" +
                "if(!style){style=document.createElement('style');style.id='android-ui-fix';" +
                "style.textContent='" +
                ".toast{opacity:0!important;visibility:hidden!important;pointer-events:none!important;transform:translate(-50%,24px)!important}" +
                ".toast.show{opacity:1!important;visibility:visible!important;pointer-events:auto!important;transform:translate(-50%,0)!important}" +
                ".miniStep.done,.moduleItem.done{position:relative!important;top:auto!important;left:auto!important;width:auto!important;height:auto!important;border-radius:18px!important;place-items:normal!important}" +
                ".topbar .title img[src=\\\"\\\"],.heroCenter>img[src=\\\"\\\"]{display:none!important}" +
                "#wuduImg:not([src]),#wuduImg[src=\\\"\\\"],#prayerImg:not([src]),#prayerImg[src=\\\"\\\"]{display:none!important}" +
                ".androidBrandFallback{width:100%;display:flex;flex-direction:column;align-items:center;gap:7px;padding:4px 10px 2px;text-align:center}" +
                ".androidBrandSchool{font-size:19px;font-weight:900;color:#34205d;letter-spacing:.2px}" +
                ".androidBrandMark{display:grid;place-items:center;min-width:86px;height:62px;padding:0 17px;border-radius:24px;background:linear-gradient(135deg,#7a258f,#b51f76);color:#fff;font-size:22px;font-weight:900;box-shadow:0 12px 24px rgba(112,36,149,.2)}" +
                ".androidBrandTitle{font-size:29px;font-weight:900;color:#7b216f;line-height:1.25}" +
                ".wuduTheme .deluxeStepFrame:has(#wuduImg[src=\\\"\\\"]),.prayerTheme .deluxeStepFrame:has(#prayerImg[src=\\\"\\\"]){min-height:330px}" +
                ".wuduTheme .deluxeStepFrame:has(#wuduImg[src=\\\"\\\"]):before,.prayerTheme .deluxeStepFrame:has(#prayerImg[src=\\\"\\\"]):before{display:grid;place-items:center;width:100%;min-height:170px;border-radius:22px;background:linear-gradient(180deg,#f7fbff,#eef6ff);font-size:74px}" +
                ".wuduTheme .deluxeStepFrame:has(#wuduImg[src=\\\"\\\"]):before{content:\\\"💧\\\"}" +
                ".prayerTheme .deluxeStepFrame:has(#prayerImg[src=\\\"\\\"]):before{content:\\\"🕌\\\"}' +
                ";document.head.appendChild(style);}" +

                "var hero=document.querySelector('.heroCenter');" +
                "if(hero&&!document.getElementById('android-brand-fallback')){" +
                "var blank=hero.querySelectorAll('img[src=\\\"\\\"]');" +
                "if(blank.length){for(var i=0;i<blank.length;i++){blank[i].style.display='none';}" +
                "var b=document.createElement('div');b.id='android-brand-fallback';b.className='androidBrandFallback';" +
                "b.innerHTML='<div class=\\\"androidBrandSchool\\\">مدرسة الغيدق الدولية</div><div class=\\\"androidBrandMark\\\">الغيدق</div><div class=\\\"androidBrandTitle\\\">تطبيق حروفي</div>';" +
                "hero.appendChild(b);}}" +

                "var topBlank=document.querySelectorAll('.topbar .title img[src=\\\"\\\"]');" +
                "for(var j=0;j<topBlank.length;j++){topBlank[j].style.display='none';}" +
                "document.title='تطبيق الحروف - الغيدق V3.8.42';" +
                "}catch(e){}})();";

        webView.evaluateJavascript(js, null);
    }

    private boolean applyBestLanguage(String lang) {
        if (tts == null) return false;

        boolean english = lang != null && lang.toLowerCase(Locale.ROOT).startsWith("en");
        Locale[] candidates = english
                ? new Locale[]{Locale.US, Locale.UK, Locale.ENGLISH}
                : new Locale[]{new Locale("ar", "SA"), new Locale("ar", "SY"), new Locale("ar")};

        for (Locale candidate : candidates) {
            try {
                int available = tts.isLanguageAvailable(candidate);
                if (available != TextToSpeech.LANG_MISSING_DATA &&
                        available != TextToSpeech.LANG_NOT_SUPPORTED) {
                    int result = tts.setLanguage(candidate);
                    if (result != TextToSpeech.LANG_MISSING_DATA &&
                            result != TextToSpeech.LANG_NOT_SUPPORTED) {
                        return true;
                    }
                }
            } catch (Exception ignored) {}
        }
        return false;
    }

    private void nativeSpeak(String text, String lang) {
        if (tts == null || text == null || text.trim().isEmpty()) return;

        if (!ttsInitFinished) {
            pendingSpeechText = text;
            pendingSpeechLang = lang;
            Toast.makeText(this, "جاري تجهيز الصوت…", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!ttsReady) {
            Toast.makeText(this,
                    "خدمة النطق غير مفعلة في الهاتف. فعّل تحويل النص إلى كلام ثم أعد المحاولة.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        if (!applyBestLanguage(lang)) {
            Toast.makeText(this,
                    "لغة النطق المطلوبة غير مثبتة في الهاتف. ثبّت صوت العربية أو الإنجليزية من إعدادات تحويل النص إلى كلام.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        try {
            tts.stop();
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "huroofi-tts");
        } catch (Exception e) {
            Toast.makeText(this, "تعذّر تشغيل النطق على هذا الجهاز", Toast.LENGTH_SHORT).show();
        }
    }

    public class AndroidBridge {
        @JavascriptInterface
        public void speak(String text, String lang) {
            runOnUiThread(() -> nativeSpeak(text, lang));
        }

        @JavascriptInterface
        public void playSuccess() {
            runOnUiThread(() -> {
                if (toneGenerator == null) return;
                try {
                    toneGenerator.startTone(ToneGenerator.TONE_PROP_ACK, 120);
                    mainHandler.postDelayed(() -> {
                        try {
                            toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP2, 150);
                        } catch (Exception ignored) {}
                    }, 150);
                    mainHandler.postDelayed(() -> {
                        try {
                            toneGenerator.startTone(ToneGenerator.TONE_PROP_ACK, 180);
                        } catch (Exception ignored) {}
                    }, 330);
                } catch (Exception ignored) {}
            });
        }

        @JavascriptInterface
        public void openTtsSettings() {
            runOnUiThread(() -> {
                try {
                    Intent intent = new Intent("com.android.settings.TTS_SETTINGS");
                    startActivity(intent);
                } catch (Exception e) {
                    try {
                        Intent intent = new Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
                        startActivity(intent);
                    } catch (Exception ignored) {
                        Toast.makeText(MainActivity.this,
                                "افتح إعدادات الهاتف ثم ابحث عن: تحويل النص إلى كلام",
                                Toast.LENGTH_LONG).show();
                    }
                }
            });
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILE_CHOOSER_REQUEST && filePathCallback != null) {
            Uri[] results = null;
            if (resultCode == RESULT_OK && data != null && data.getData() != null) {
                results = new Uri[]{data.getData()};
            }
            filePathCallback.onReceiveValue(results);
            filePathCallback = null;
        }
    }

    @Override
    public void onBackPressed() {
        if (webView == null) {
            super.onBackPressed();
            return;
        }
        webView.evaluateJavascript(
                "(function(){try{if(window.st&&st.screen&&st.screen!=='home'){var b=document.getElementById('back');if(b){b.click();return 'handled';}}}catch(e){}return 'exit';})()",
                value -> {
                    if (value == null || value.contains("exit")) {
                        MainActivity.super.onBackPressed();
                    }
                });
    }

    @Override
    protected void onDestroy() {
        if (tts != null) {
            try {
                tts.stop();
                tts.shutdown();
            } catch (Exception ignored) {}
            tts = null;
        }

        if (toneGenerator != null) {
            try {
                toneGenerator.release();
            } catch (Exception ignored) {}
            toneGenerator = null;
        }

        mainHandler.removeCallbacksAndMessages(null);

        if (webView != null) {
            webView.removeJavascriptInterface("AndroidBridge");
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }
}
