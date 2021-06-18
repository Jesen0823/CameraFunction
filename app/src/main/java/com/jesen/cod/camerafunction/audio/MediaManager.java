package com.jesen.cod.camerafunction.audio;

import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.core.content.FileProvider;

import com.jesen.cod.camerafunction.utils.Outil;

import java.io.File;
import java.io.IOException;

/**
 * Created by wondertek on 2019/11/6
 * e-mail : xie_stacol@163.com
 * desc   : 自定义录音播放工具类
 * version: 1.0
 */


public class MediaManager {
    private static MediaPlayer mMediaPlayer;
    private static boolean isPause;
    public final static String PCM_URL = "https://gitee.com/null_694_3232/ffmpeg-play-kot/tree/master/video_resource/out.pcm";

    private static final String AUTHORITY = "com.jesen.camerademo.fileProvider";

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public static void playSound(Context context, String soundPath, MediaPlayer.OnCompletionListener onCompletionListener){
        Log.i("playSound,","soundPath ="+soundPath);
        Uri fileUri = getLocalUri(context, soundPath);
        Log.i("playSound,","getLocalUri ="+ fileUri.getPath());

        try {
            mMediaPlayer = new MediaPlayer();
            //mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mMediaPlayer.setAudioAttributes(
                    new AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
            );
            mMediaPlayer.setDataSource(context, fileUri);
            //mMediaPlayer.setDataSource(PCM_URL);
            mMediaPlayer.prepareAsync();
            mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    Log.d("playSound", "==@==start==");
                    mMediaPlayer.start();
                }
            });
            mMediaPlayer.setOnCompletionListener(onCompletionListener);
            mMediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mp, int what, int extra) {
                    pause();
                    return false;
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }


/*        if (mMediaPlayer == null){
            mMediaPlayer = new MediaPlayer();
            mMediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener () {
                @Override
                public boolean onError (MediaPlayer mp, int what, int extra) {
                    mMediaPlayer.reset ();
                    return false;
                }
            });
        }else {
            mMediaPlayer.reset ();
        }
        mMediaPlayer.setAudioAttributes(
                new AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
        );

        mMediaPlayer.setOnCompletionListener (onCompletionListener);

        try {
            //mMediaPlayer.setDataSource(context, fileUri);
            mMediaPlayer.setDataSource(context, Uri.parse(PCM_URL));
            mMediaPlayer.prepareAsync();
        } catch (IOException e) {
            e.printStackTrace ();
        }

        mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                mMediaPlayer.start();

            }
        });*/

    }

    public static void pause(){
        if (mMediaPlayer != null && mMediaPlayer.isPlaying ()){
            mMediaPlayer.pause ();
            mMediaPlayer.prepareAsync ();
            isPause = true;
        }
    }

    public static void resume(){
        if (mMediaPlayer !=null && isPause){
            mMediaPlayer.start ();
            isPause = false;
        }
    }

    public static void release(){
        if (mMediaPlayer != null){
            mMediaPlayer.release ();
            mMediaPlayer = null;
        }
    }

    public static boolean isPlaying(){
        if (mMediaPlayer != null && mMediaPlayer.isPlaying ()){
            return true;
        }
        return false;
    }

    private static Uri getLocalUri(Context context, String filePath){
        File file = new File(filePath);
        Uri uri = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            uri = FileProvider.getUriForFile(context, AUTHORITY, file);
        }else {
            uri = Uri.fromFile(file);
        }
        Outil.log("getLocalUri, uri :"+uri);
        return uri;
    }
}
