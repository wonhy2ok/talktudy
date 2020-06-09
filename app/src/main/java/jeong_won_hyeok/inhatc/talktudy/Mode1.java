package jeong_won_hyeok.inhatc.talktudy;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
public class Mode1 extends Activity implements SensorEventListener {
    TextView myOutput;

    private SensorManager mSensorManager;
    private Sensor mProximity;

    final static int Init = 0;
    final static int Run = 1;
    final static int Pause = 2;

    int cur_Status = Init;
    int myCount = 1;
    long myBaseTime;
    long myPauseTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mode1);
        myOutput = (TextView) findViewById(R.id.time_out);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mProximity = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        loadData();
    }

    public final void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public final void onSensorChanged(SensorEvent event) {
        if (event.values[0] == 0) {
            switch (cur_Status) {
                case Init:
                    myBaseTime = SystemClock.elapsedRealtime();
                    System.out.println(myBaseTime);
                    myTimer.sendEmptyMessage(0);
                    cur_Status = Run; //현재상태를 런상태로 변경
                    myCount++;
                    break;
                case Pause:
                    long now = SystemClock.elapsedRealtime();
                    myTimer.sendEmptyMessage(0);
                    myBaseTime += (now - myPauseTime);
                    cur_Status = Run;
                    break;
            }

        } else {
            myTimer.removeMessages(0);
            myCount = 1;
            myPauseTime = SystemClock.elapsedRealtime();
            cur_Status = Pause;
        }
        saveState();
    }

    protected void saveState() {
        SharedPreferences pref = getSharedPreferences("pref", Activity.MODE_PRIVATE);
    }

    public void loadData() {
        SharedPreferences sharedPreferences = getSharedPreferences("pref", MODE_PRIVATE);
        myBaseTime = SystemClock.elapsedRealtime();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mProximity, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    Handler myTimer = new Handler() {
        public void handleMessage(Message msg) {
            myOutput.setText(getTimeOut());
            myTimer.sendEmptyMessage(0);
        }
    };

    String getTimeOut() {
        long now = SystemClock.elapsedRealtime();
        long outTime = now - myBaseTime;
        String easy_outTime = String.format("%02d:%02d:%02d", outTime / 1000 / 60, (outTime / 1000) % 60, (outTime % 1000) / 10);
        return easy_outTime;
    }

    public void Back(View view) {
        finish();
    }
}