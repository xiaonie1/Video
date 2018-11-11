package com.nhj.video;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

public class AudioRecordReplay extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio_record_replay);
        findViewById(R.id.record).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AudioRecordManager.getInstanec(getApplicationContext()).startRecord();
            }
        });

        findViewById(R.id.stopRecord).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AudioRecordManager.getInstanec(getApplicationContext()).stopRecord();
            }
        });

        findViewById(R.id.play).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AudioRecordManager.getInstanec(getApplicationContext()).replay();
            }
        });

        findViewById(R.id.stopPlay).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AudioRecordManager.getInstanec(getApplicationContext()).stopReplay();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        AudioRecordManager.getInstanec(getApplicationContext()).release() ;
    }
}
