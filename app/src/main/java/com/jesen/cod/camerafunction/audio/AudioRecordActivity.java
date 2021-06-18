package com.jesen.cod.camerafunction.audio;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.jesen.cod.camerafunction.R;
import com.jesen.cod.camerafunction.databinding.ActivityAudioRecordBinding;
import com.jesen.cod.camerafunction.utils.Outil;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;


public class AudioRecordActivity extends AppCompatActivity {
    
    private ActivityAudioRecordBinding mBinding;

    private static final String TAG = "AudioRecordActivity";
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {"android.permission.READ_EXTERNAL_STORAGE"
            , "android.permission.WRITE_EXTERNAL_STORAGE", "android.permission.RECORD_AUDIO"};

    private AudioRecord mAudioRecord;
    private String voicePath;
    private String voiceName = "test.pcm";
    private boolean isRecording = false;

    private int audioSource = MediaRecorder.AudioSource.MIC;//声音来源
    private int sampleRateInHz = 16000;//采样频率
    private int channelConfig = AudioFormat.CHANNEL_IN_MONO;//声道
    private int ahdioFormat = AudioFormat.ENCODING_PCM_16BIT;//音频格式
    private int bufferSize = 2 * AudioRecord.getMinBufferSize(sampleRateInHz, channelConfig, ahdioFormat);//缓冲区大小

    ProgressBar mProgressBar;
    Button startRecord;
    Button stopRecord;
    Button startPlay;
    Button modeStatick;
    Button modeStream;
    Button pcm2wav;
    private byte[] audioData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = ActivityAudioRecordBinding.inflate(getLayoutInflater());
        
        setContentView(R.layout.activity_audio_record);

        voicePath = getFilesDir().getAbsolutePath() + File.separator+"audio"+File.separator;

        //ButterKnife.bind (this);
        init();
    }

    private void init() {
        mProgressBar = findViewById(R.id.progress_bar);
        startRecord = findViewById(R.id.start_record);
        startPlay = findViewById(R.id.start_play);
        stopRecord = findViewById(R.id.stop_record);
        modeStatick = findViewById(R.id.mode_statick);
        modeStream = findViewById(R.id.mode_stream);
        pcm2wav = findViewById(R.id.pcm_2_wav);

        setClickEvent();

        verifyStoragePermissions(this);
        mProgressBar.setVisibility(View.GONE);
    }


    public static void verifyStoragePermissions(Activity activity) {
        try {
            int permission_STORGE = ActivityCompat.checkSelfPermission(activity, "android.permission.WRITE_EXTERNAL_STORAGE");
            int permission_AUDIO = ActivityCompat.checkSelfPermission(activity, "android.permission_RECORD_AUDIO");
            if (permission_STORGE != PackageManager.PERMISSION_GRANTED || permission_AUDIO != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(activity, PERMISSIONS_STORAGE, REQUEST_EXTERNAL_STORAGE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void stopRecord() {
        isRecording = false;
        if (mAudioRecord != null) {
            mProgressBar.setVisibility(View.GONE);
            mAudioRecord.stop();
            mAudioRecord.release();
            //调用release之后必须置null
            mAudioRecord = null;
        }
    }

    private void startRecordAudio() {
        Outil.log("AudioRecordActivity ,startRecord");

        mAudioRecord = new AudioRecord(audioSource, sampleRateInHz, channelConfig, ahdioFormat, bufferSize);
        final byte data[] = new byte[bufferSize];
        final File file = new File(voicePath);
        final File fileaudio = new File(voicePath + voiceName);
        Outil.log("AudioRecordActivity ,startRecord fileaudio=" + fileaudio);
        if (fileaudio.exists()) {
            fileaudio.delete();
        }
        if (!file.exists()) {
            Outil.log("AudioRecordActivity ,file.mkdir");
            file.mkdir();
        }
        mAudioRecord.startRecording();
        isRecording = true;
        mProgressBar.setVisibility(View.VISIBLE);

        new Thread(new Runnable() {
            @Override
            public void run() {
                FileOutputStream fos = null;
                try {
                    fos = new FileOutputStream(fileaudio);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }

                if (fos != null) {
                    while (isRecording) {
                        int read = mAudioRecord.read(data, 0, bufferSize);
                        if (AudioRecord.ERROR_INVALID_OPERATION != read) {
                            try {
                                fos.write(data);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    try {
                        fos.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                audioData = File2byte(fileaudio.getPath());
                Outil.log("audioData length = " + audioData.length);
            }
        }).start();
    }

    public static byte[] File2byte(String filePath) {
        byte[] buffer = null;
        try {
            File file = new File(filePath);
            FileInputStream fis = new FileInputStream(file);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] b = new byte[1024];
            int n;
            while ((n = fis.read(b)) != -1) {
                bos.write(b, 0, n);
            }
            fis.close();
            bos.close();
            buffer = bos.toByteArray();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Outil.log("audioData length = " + buffer.length);
        return buffer;
    }


    private void playSoundByAudioTrac_MODE_STATICk() {
        AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRateInHz,
                AudioFormat.CHANNEL_OUT_MONO
                , ahdioFormat, audioData.length, AudioTrack.MODE_STATIC);
        audioTrack.write(audioData, 0, audioData.length);
        audioTrack.play();
    }

    public void playsoundByAudioTrack_MODE_STREAM(final String path) {
        final AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRateInHz, AudioFormat.CHANNEL_OUT_MONO
                , ahdioFormat, bufferSize, AudioTrack.MODE_STREAM);
        audioTrack.play();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    InputStream in = new FileInputStream(path);
                    try {
                        byte[] tempBuffer = new byte[bufferSize];
                        int readCount = 0;
                        while (in.available() > 0) {
                            readCount = in.read(tempBuffer);
                            if (readCount == AudioTrack.ERROR_INVALID_OPERATION || readCount == AudioTrack.ERROR_BAD_VALUE) {
                                continue;
                            }
                            if (readCount != 0 && readCount != -1) {
                                audioTrack.write(tempBuffer, 0, readCount);
                            }
                        }
                    } finally {
                        in.close();
                    }
                } catch (IOException e) {

                }
            }
        }).start();
    }

    private void setClickEvent() {
        startRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //if (mAudioRecord != null) {
                    Outil.log("AudioRecordActivity mAudioRecord != null");
                    startRecordAudio();
               // }
            }
        });
        stopRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mAudioRecord != null) {
                    stopRecord();
                }
            }
        });

        startPlay.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onClick(View v) {
                Outil.log("==play ,voicePath" + voicePath);
                Outil.log("==play ，voiceName" + voiceName);
                MediaManager.playSound(AudioRecordActivity.this,
                        voicePath + voiceName, new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        Outil.log("==onCompletion" + mp);
                    }
                });
            }
        });
        modeStatick.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playSoundByAudioTrac_MODE_STATICk();//justPlay
            }
        });
        modeStream.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playsoundByAudioTrack_MODE_STREAM(voicePath);

            }
        });
        pcm2wav.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //录音采样频率
                int audioRate = 16000;
                //录音声道，单声道
                int audioChannel = AudioFormat.CHANNEL_IN_MONO;
                //音频格式/量化深度
                int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
                //缓存的大小
                int bufferSize = AudioRecord.getMinBufferSize(audioRate, audioChannel, audioFormat) * 2;
                PCMConvWavUtil util = new PCMConvWavUtil(AudioRecordActivity.this, audioRate, audioChannel, audioFormat, bufferSize);
                util.PCMConvertWavFile();
            }
        });
    }
}
