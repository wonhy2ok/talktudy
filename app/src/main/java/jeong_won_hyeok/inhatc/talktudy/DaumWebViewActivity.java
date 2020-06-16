package jeong_won_hyeok.inhatc.talktudy;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;

public class DaumWebViewActivity extends AppCompatActivity {

    private WebView webView;
    private Handler handler;
    private EditText searchBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_daum_web_view);

        webView   = (WebView)findViewById(R.id.webView);

        // 핸들러를 통한 JavaScript 이벤트 반응
        handler = new Handler();

        init_webView();
    }


    @JavascriptInterface
    public void init_webView() {
        webView.getSettings().setJavaScriptEnabled(true);

        // JavaScript의 window.open 허용
        webView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);

        // JavaScript이벤트에 대응할 함수를 정의 한 클래스를 붙여줌
        webView.addJavascriptInterface(new AndroidBridge(), "TestApp");

        // web client 를 chrome 으로 설정
        webView.setWebChromeClient(new WebChromeClient());

        // webview url load
        webView.loadUrl("http://112.154.166.107:1234/qwer.php");
    }

    private class AndroidBridge {
        @JavascriptInterface
        public void setAddress(final String arg1, final String arg2, final String arg3) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    init_webView();

                    String address = String.format("(%s) %s %s", arg1, arg2, arg3);
                    Intent intent = new Intent();
                    intent.putExtra("address", address);
                    setResult(RESULT_OK, intent);
                    finish();
                }
            });
        }
    }
}
