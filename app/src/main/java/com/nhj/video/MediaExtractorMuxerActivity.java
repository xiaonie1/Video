package com.nhj.video;

import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

public class MediaExtractorMuxerActivity extends AppCompatActivity {

    private String path ;
    private ExecutorService executorService;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media_extractor_muxer);
        path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath()
                + File.separator + "mv.mp4";
        executorService = Executors.newCachedThreadPool();
        findViewById(R.id.button1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //抽离视频MP4
                MediaExtractorTask<Integer> mediaExtractorTask = new MediaExtractorTask<>() ;
                FutureTask<Integer> futureTask = new FutureTask<Integer>(mediaExtractorTask);
                executorService.submit(futureTask) ;
//

            }
        });
        findViewById(R.id.buttoon2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
    }

    class MediaExtractorTask<T> implements Callable<T> {

        public MediaExtractorTask(){

        }
        @Override
        public T call() throws Exception {

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
                MediaExtractor mediaExtractor = new MediaExtractor() ;
                mediaExtractor.setDataSource(path);
                int trackCount = mediaExtractor.getTrackCount();
                Log.i("trackCount",trackCount+"") ;
                for(int i=0;i<trackCount;i++){
                    MediaFormat mediaFormat = mediaExtractor.getTrackFormat(i) ;
                    String mine = mediaFormat.getString(MediaFormat.KEY_MIME);
                    Log.i("@@","mine = "+mine) ;
                    if(!mine.startsWith("video/")){
                        continue;
                    }
                    int frameRate = mediaFormat.getInteger(MediaFormat.KEY_FRAME_RATE) ;
                    Log.i("@@","frameRate = "+frameRate) ;
                    mediaExtractor.selectTrack(i);
                }
                ByteBuffer inputBuffer = ByteBuffer.allocate(5*1024) ;
                int read = mediaExtractor.readSampleData(inputBuffer,0);
                while (!Thread.interrupted()&&read>0){

                }
            }

            return null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }
}
