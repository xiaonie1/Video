package com.nhj.video;

import android.annotation.TargetApi;
import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static android.media.AudioFormat.ENCODING_PCM_16BIT;
import static com.nhj.video.AudioRecordManager.RecorderTask.SAMPLE_RATE_HERTZ;

/**
 * Created by nhj on 2018/11/6.
 */

public class AudioRecordManager {

    private static final String TAG = AudioRecordManager.class.getSimpleName();
    private volatile static AudioRecordManager mAudioRecordManager;
    private volatile boolean isRecoreder;
    private String audioFolderFile;
    private String DIR_NAME = "AUDIO";
    private ExecutorService executorService;
    private Future<?> resultRecord;
    private File wavFile;
    private Future<String> replayResult;
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 1:
                    Toast.makeText(context, "录制结束", Toast.LENGTH_SHORT).show();
                    break;
                case 2:
                    Toast.makeText(context, "播放结束", Toast.LENGTH_SHORT).show();
                    break;

            }
        }
    };

    private Context context;
    private File pcmFile;

    private AudioRecordManager(Context context) {
        this.context = context;
        //文件目录
        audioFolderFile = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath()
                + File.separator + DIR_NAME;
        Log.i("@@", "audioFolderFile = " + audioFolderFile);
        File wavDir = new File(audioFolderFile);
        if (!wavDir.exists()) {
            boolean flag = wavDir.mkdirs();
        } else {
        }
    }


    public static AudioRecordManager getInstanec(Context context) {
        if (mAudioRecordManager == null) {
            synchronized (AudioRecordManager.class) {
                if (mAudioRecordManager == null) {
                    mAudioRecordManager = new AudioRecordManager(context);
                }
            }
        }
        return mAudioRecordManager;
    }

    /**
     * 开始录制
     */
    public void startRecord() {
        if (isRecoreder) {
            return;
        }
        isRecoreder = true;
        SimpleDateFormat sdf = new SimpleDateFormat("yyMMdd_HHmmss", Locale.CHINA);
        String fName = sdf.format(new Date());
        pcmFile = new File(audioFolderFile + File.separator + fName + ".pcm");
        wavFile = new File(audioFolderFile + File.separator + fName + ".wav");
        RecorderTask recorderTask = new RecorderTask(pcmFile, wavFile);
        if (executorService == null) {
            executorService = Executors.newCachedThreadPool();
        }
        resultRecord = executorService.submit(recorderTask);
        Log.i("@@", "startRecord");
    }

    /**
     * 关闭录制
     */
    public void stopRecord() {

        if (resultRecord != null) {
            Log.i("@@", "stopRecord");
            resultRecord.cancel(true);
            //录制停止
            isRecoreder = false;
        }
    }

    public  void release() {
       executorService.shutdownNow() ;
    }

    class RecorderTask implements Runnable {
        private final File pcmFile;
        private final File wavFile;
        private int minBuffer = 10240;

        private AudioRecord audioRecord;
        /**
         * 采样率，现在能够保证在所有设备上使用的采样率是44100Hz, 但是其他的采样率（22050, 16000, 11025）在一些设备上也可以使用。
         */
        public static final int SAMPLE_RATE_HERTZ = 44100;
        /**
         * 声道数。CHANNEL_IN_MONO and CHANNEL_IN_STEREO. 其中CHANNEL_IN_MONO是可以保证在所有设备能够使用的。
         */
        public static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_STEREO;

        public RecorderTask(File pcmFile, File wavFile) {
            this.pcmFile = pcmFile;
            this.wavFile = wavFile;
            minBuffer = AudioRecord.getMinBufferSize(SAMPLE_RATE_HERTZ, AudioFormat.CHANNEL_IN_STEREO, ENCODING_PCM_16BIT);
            audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE_HERTZ, AudioFormat.CHANNEL_IN_STEREO,
                    ENCODING_PCM_16BIT, minBuffer);
        }

        @Override
        public void run() {
            Log.i("@@", "run RecorderTask");
            FileChannel fileChannel = null;
            FileOutputStream pcmFileOutputStream = null;
            try {
                pcmFileOutputStream = new FileOutputStream(pcmFile);
                fileChannel = pcmFileOutputStream.getChannel();
//                ByteBuffer buffer = ByteBuffer.allocateDirect(minBuffer);
                audioRecord.startRecording();
                while (!Thread.interrupted()) {
//                    int read = audioRecord.read(buffer, minBuffer);
                    byte[] bytes = new byte[minBuffer];
                    int read = audioRecord.read(bytes, 0, minBuffer);
                    if (read != audioRecord.ERROR_INVALID_OPERATION && read != AudioRecord.ERROR_BAD_VALUE) {
                        Log.i("@@", "byte size = " + read);
//                        buffer.flip();
//                        fileChannel.write(bytes);
//                        buffer.clear();
                        pcmFileOutputStream.write(bytes);
                    }
                }

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (fileChannel != null) {
                        fileChannel.close();
                    }
                    if (pcmFileOutputStream != null) {
                        pcmFileOutputStream.flush();
                        pcmFileOutputStream.close();
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }

            }

            Log.i("@@", "录制声音到文件完毕，开始添加header转换为wav可播放文件");
            //录制停止
            isRecoreder = false;
            //当录制完成就将Pcm编码数据转化为wav文件，也可以直接生成.wav
            synchronized (wavFile) {
                //防止播放线程在wavFile文件还没生成就开始访问
                pcmtoWav(pcmFile.getPath(), wavFile.getPath(), minBuffer);
                Log.i("@@", "转换完毕");
            }

            handler.obtainMessage(1).sendToTarget();
            Log.d(TAG, "录制结束");


        }
    }



    public void replay() {
        if (isRecoreder) {
            Log.i("@@", "无法播放");
            return;
        }
        if (executorService == null) {
            executorService = Executors.newCachedThreadPool();
        }
        ReplayTask replayTask = new ReplayTask();
        replayResult = executorService.submit(replayTask);

    }

    public void stopReplay() {
        if (replayResult != null) {
            replayResult.cancel(true);
            isRecoreder = false;
        }
    }


    class ReplayTask implements Callable<String> {
        private int bufferSize;
        AudioTrack audioTrack;

        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        public ReplayTask() {
            bufferSize = AudioTrack.getMinBufferSize(RecorderTask.SAMPLE_RATE_HERTZ, AudioFormat.CHANNEL_OUT_STEREO, ENCODING_PCM_16BIT);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                audioTrack = new AudioTrack.Builder().setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).
                        setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()).setAudioFormat(new AudioFormat.Builder().
                        setEncoding(ENCODING_PCM_16BIT).setSampleRate(RecorderTask.SAMPLE_RATE_HERTZ).setChannelMask(AudioFormat.CHANNEL_OUT_STEREO).build())
                        .build();
            }

        }

        @Override
        public String call() throws Exception {
            Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
            if (wavFile != null && wavFile.exists()) {
                synchronized (wavFile) {
                    FileChannel fileChannel = null;
                    FileInputStream fileInputStream = null;
                    try {
                        fileInputStream = new FileInputStream(wavFile);
                        fileChannel = fileInputStream.getChannel();
                        if (bufferSize < 1) {
                            bufferSize = 10240;
                        }
                        ByteBuffer buffer = ByteBuffer.allocate(bufferSize);
                        if (audioTrack != null) {
                            audioTrack.play();
                            while (!Thread.interrupted() && !isRecoreder) {
                                int read = fileChannel.read(buffer);
                                if (read == AudioTrack.ERROR_BAD_VALUE || read == AudioTrack.ERROR_INVALID_OPERATION) {
                                    continue;
                                } else if (read == -1) {
                                    break;
                                } else {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                        buffer.flip();
                                        audioTrack.write(buffer, read, audioTrack.WRITE_BLOCKING);
                                        buffer.clear();
                                    }
                                }

                            }
                        }

                    } catch (Exception o) {
                        o.printStackTrace();

                    } finally {
                        if (audioTrack != null) audioTrack.stop();
                        if (audioTrack != null) audioTrack.release();
                        if (fileChannel != null) fileChannel.close();
                        if (fileInputStream != null) fileInputStream.close();
                    }


                }
            }
            handler.obtainMessage(2).sendToTarget();
            return null;
        }
    }



    /**
     * 将pcm文件转化为可点击播放的wav文件
     *
     * @param inputPath pcm路径
     * @param outPath   wav存放路径
     * @param
     */
    private void pcmtoWav(String inputPath, String outPath, int minBuffe) {
        FileChannel in;
        FileChannel out;
        try {
            in = new FileInputStream(inputPath).getChannel();
            out = new FileOutputStream(outPath).getChannel();
            ByteBuffer buffer = ByteBuffer.allocate(minBuffe);
            //添加头部信息
            writeWavFileHeader(out, in.size(), SAMPLE_RATE_HERTZ, AudioFormat.CHANNEL_IN_STEREO);
            while (in.read(buffer) != -1) {
                buffer.flip();
                out.write(buffer);
                buffer.clear();
            }
            in.close();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * @param out            wav音频文件流
     * @param totalAudioLen  不包括header的音频数据总长度
     * @param longSampleRate 采样率,也就是录制时使用的频率
     * @param channels       audioRecord的频道数量
     * @throws IOException 写文件错误
     */
    private void writeWavFileHeader(FileChannel out, long totalAudioLen, long longSampleRate,
                                    int channels) throws IOException {
        byte[] header = generateWavFileHeader(totalAudioLen, longSampleRate, channels);
        //写头
        out.write(ByteBuffer.wrap(header, 0, header.length));

    }

    /**
     * 任何一种文件在头部添加相应的头文件才能够确定的表示这种文件的格式，
     * wave是RIFF文件结构，每一部分为一个chunk，其中有RIFF WAVE chunk，
     * FMT Chunk，Fact chunk,Data chunk,其中Fact chunk是可以选择的
     *
     * @param totalAudioLen  不包括header的音频数据总长度
     * @param longSampleRate 采样率,也就是录制时使用的频率
     * @param channels       audioRecord的频道数量
     */
    private byte[] generateWavFileHeader(long totalAudioLen, long longSampleRate, int channels) {
        long totalDataLen = totalAudioLen + 36;
        long byteRate = longSampleRate * 2 * channels;
        byte[] header = new byte[44];
        header[0] = 'R'; // RIFF
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';
        header[4] = (byte) (totalDataLen & 0xff);//数据大小
        header[5] = (byte) ((totalDataLen >> 8) & 0xff);
        header[6] = (byte) ((totalDataLen >> 16) & 0xff);
        header[7] = (byte) ((totalDataLen >> 24) & 0xff);
        header[8] = 'W';//WAVE
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';
        //FMT Chunk
        header[12] = 'f'; // 'fmt '
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';//过渡字节
        //数据大小
        header[16] = 16; // 4 bytes: size of 'fmt ' chunk
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;
        //编码方式 10H为PCM编码格式
        header[20] = 1; // format = 1
        header[21] = 0;
        //通道数
        header[22] = (byte) channels;
        header[23] = 0;
        //采样率，每个通道的播放速度
        header[24] = (byte) (longSampleRate & 0xff);
        header[25] = (byte) ((longSampleRate >> 8) & 0xff);
        header[26] = (byte) ((longSampleRate >> 16) & 0xff);
        header[27] = (byte) ((longSampleRate >> 24) & 0xff);
        //音频数据传送速率,采样率*通道数*采样深度/8
        header[28] = (byte) (byteRate & 0xff);
        header[29] = (byte) ((byteRate >> 8) & 0xff);
        header[30] = (byte) ((byteRate >> 16) & 0xff);
        header[31] = (byte) ((byteRate >> 24) & 0xff);
        // 确定系统一次要处理多少个这样字节的数据，确定缓冲区，通道数*采样位数
        header[32] = (byte) (2 * channels);
        header[33] = 0;
        //每个样本的数据位数
        header[34] = 16;
        header[35] = 0;
        //Data chunk
        header[36] = 'd';//data
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';
        header[40] = (byte) (totalAudioLen & 0xff);
        header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
        header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
        header[43] = (byte) ((totalAudioLen >> 24) & 0xff);
        return header;
    }


}
/**
 * 优化：
 * 播放线程可以等待录制线程。一旦录制线程被中断结束，就可以唤醒播放线程自动播放
 */