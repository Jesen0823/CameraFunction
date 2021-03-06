package com.jesen.cod.camerafunction.audio;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by wondertek on 2019/11/6
 * e-mail : xie_stacol@163.com
 * desc   : PCM录音文件格式转换为WAV格式
 * version: 1.0
 */
public class PCMConvWavUtil {
    //录音采样频率
    private int audioRate = 16000;
    //录音声道，单声道
    private int audioChannel = AudioFormat.CHANNEL_IN_MONO;
    //音频格式/量化深度
    private int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
    //缓存的大小
    private int bufferSize = AudioRecord.getMinBufferSize (audioRate,audioChannel,audioFormat) * 2;

    //PCM文件
    private File pcmFile;
    //WAV文件
    private File wavFile;
    private String basePath;
    //PCM文件目录
    private String inFileName; //测试文件暂存在assets里了
    //wav文件目录
    private String outFileName;

    public PCMConvWavUtil (Context context, int audioRate, int audioChannel, int audioFormat, int bufferSize) {
        this.audioRate = audioRate;
        this.audioChannel = audioChannel;
        this.audioFormat = audioFormat;
        this.bufferSize = bufferSize;
        basePath = context.getFilesDir().getAbsolutePath() + File.separator;
        //PCM文件目录
        inFileName = basePath + "test.pcm"; //测试文件暂存在assets里了
        //wav文件目录
        outFileName = basePath + "test.wav";

        File baseFile = new File (basePath);
        if (!baseFile.exists ()){
            baseFile.mkdirs ();
        }
        pcmFile = new File (inFileName);
        wavFile = new File (outFileName);

        try {
            if (!pcmFile.exists ())
                pcmFile.createNewFile ();
            if (!wavFile.exists ())
                wavFile.createNewFile ();
        } catch (IOException e) {
            e.printStackTrace ();
        }
    }

    public void PCMConvertWavFile(){
        Log.i("PCMConvertWavFile", "inFileName = "+inFileName);
        File twavFile = new File (outFileName);
        if (twavFile.exists ()){
            twavFile.delete ();
        }
        FileInputStream in = null;
        FileOutputStream out = null;
        long totalAudioLen = 0;
        long totalDataLen = totalAudioLen + 36;
        long longSampleRate = audioRate;
        int channels = 1;
        long byteRate = 16 * audioRate * channels / 8;
        if (audioFormat == AudioFormat.ENCODING_PCM_16BIT){
            byteRate = 16 * audioRate * channels / 8;
        }else if (audioFormat == AudioFormat.ENCODING_PCM_8BIT){
            byteRate = 8 * audioRate *channels /8;
        }

        byte[] data = new byte[bufferSize];
        try {
            in = new FileInputStream (inFileName);
            out = new FileOutputStream (outFileName);
            totalAudioLen = in.getChannel ().size ();
            //由于不包括前面的8个字节RIFF和WAV
            totalDataLen = totalAudioLen + 36;
            addWaveFileHeader(out,totalAudioLen,totalDataLen,longSampleRate,channels,byteRate);
            while (in.read (data) != -1){
                out.write (data);
            }
            in.close ();
            out.close ();
        } catch (FileNotFoundException e) {
            e.printStackTrace ();
        }catch (IOException e){
            e.printStackTrace ();
        }
    }

    private void addWaveFileHeader (FileOutputStream out, long totalAudioLen, long totalDataLen,
                                    long longSampleRate, int channels, long byteRate) throws IOException{
        byte[] header = new byte[44];
        //RIFF头
        header[0] = 'R';
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';
        //数据大小，真正大小是添加了8bit
        header[4] = (byte) (totalDataLen & 0xff);
        header[5] = (byte) ((totalDataLen >> 8) & 0xff);
        header[6] = (byte) ((totalDataLen >> 16) & 0xff);
        header[7] = (byte) ((totalDataLen >> 24) & 0xff);
        //wave格式
        header[8] = 'W';
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';
        //fmt Chunk
        header[12] = 'f';
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';
        //数据大小
        header[16] = 16; // 4 bytes: size of 'fmt ' chunk
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;
        //编码方式 10H为PCM编码格式
        header[20] = 1;
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
        int channelFlag = 1;
        if (audioChannel == AudioFormat.CHANNEL_IN_MONO){
            channelFlag = 1;
        }else {
            channelFlag = 2;
        }
        //确定系统一次要处理多少个这样字节的数据，确定缓冲区，通道数*采样位数
        if (audioFormat == AudioFormat.ENCODING_PCM_16BIT){
            header[32] = (byte) (channelFlag * 16 / 8);
        }else if (audioFormat == AudioFormat.ENCODING_PCM_8BIT){
            header[32] = (byte) (channelFlag * 8 / 8);
        }
        header[33] = 0;
        //每个样本的数据位数
        if (audioFormat == AudioFormat.ENCODING_PCM_16BIT){
            header[34] = 16;
        }else if (audioFormat == AudioFormat.ENCODING_PCM_8BIT){
            header[34] = 8;
        }
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
        out.write(header, 0, 44);

    }
}
