package com.jesen.cod.camerafunction.activity;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.VideoView;

import com.jesen.cod.camerafunction.R;
import com.jesen.cod.camerafunction.utils.GetImagePath;
import com.jesen.cod.camerafunction.utils.Outil;
import com.jesen.cod.camerafunction.utils.CompressImgTask;
import com.jesen.cod.camerafunction.utils.FileUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class SystemCameraActivity extends AppCompatActivity {

    private static int REQ_THUMB_CAPTURE_1 = 1;
    private static int REQ_ORIGIN_CAPTURE_2 = 2;
    private static int REQ_CAPTURE_CLIP_3 = 3;
    private static int REQ_OPEN_ALBUM_4 = 4;
    private static int REQ_OPEN_ALBUM_N_5 = 5;
    private static int REQ_VIDEO_CAPTURE_6 = 6;
    private static int REQ_SYS_CLIP_IMAGE_7 = 7;

    private static String AUTHORITY = "com.jesen.camerademo.fileProvider";
    @Deprecated
    private static String ROOT_FOLDER_PATH_1 = Environment.getExternalStorageDirectory()
            .getAbsolutePath() + File.separator + "capture";
    private String ROOT_FOLDER_PATH;
    private File mImageFile;
    private File mCropFile;

    private Button thumbSysBtn, realSysBtn, captureClipBtn, albumClipBtn, captureVideoBtn;
    private ImageView resultShow;
    private VideoView videoView;
    private TextView tipText;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_system_camera);

        initView();
        ROOT_FOLDER_PATH = FileUtil.getStoragePath(this, Environment.DIRECTORY_PICTURES);
        Outil.log("onCreate, ROOT_FOLDER_PATH: " + ROOT_FOLDER_PATH);
        mCropFile = createImageFile(true);
    }

    private void initView() {
        thumbSysBtn = findViewById(R.id.thumbSysCameraBtn);
        realSysBtn = findViewById(R.id.realSysCameraBtn);
        captureClipBtn = findViewById(R.id.captureClipBtn);
        albumClipBtn = findViewById(R.id.albumClipBtn);
        captureVideoBtn = findViewById(R.id.captureVideoBtn);

        resultShow = findViewById(R.id.iv_result);
        videoView = findViewById(R.id.videoView);
        tipText = findViewById(R.id.tip_t);

        thumbSysBtn.setOnClickListener(view -> startSysCaptureThumb());

        realSysBtn.setOnClickListener(view -> startSysCaptureOrigin());

        captureClipBtn.setOnClickListener(view -> startSysCapThenCut());

        albumClipBtn.setOnClickListener(view -> startOpenAlbumThenOpen());

        captureVideoBtn.setOnClickListener(view -> startCapVideoThenPlay());
    }

    // 普通系统拍照，将返回缩略图
    private void startSysCaptureThumb() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        //startActivity(intent);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            intent.addFlags(Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);
        }
        startActivityForResult(intent, REQ_THUMB_CAPTURE_1);
    }

    // 系统拍照，指定存储位置 返回原图
    private void startSysCaptureOrigin() {
        mImageFile = createImageFile(false);
        Uri mPhotoUri;
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            intent.addFlags(Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);
            mPhotoUri = FileProvider.getUriForFile(SystemCameraActivity.this, AUTHORITY, mImageFile);
            // 指定存储路径
            intent.putExtra(MediaStore.EXTRA_OUTPUT, mPhotoUri);
        } else {
            intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(mImageFile));
        }
        // 指定保存格式
        intent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString());
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(intent, REQ_ORIGIN_CAPTURE_2);
        }
    }

    // 拍照并剪裁
    private void startSysCapThenCut() {
        mImageFile = createImageFile(false);
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            Uri mPhotoUri = FileProvider.getUriForFile(this, AUTHORITY, mImageFile);
            // 指定存储路径
            intent.putExtra(MediaStore.EXTRA_OUTPUT, mPhotoUri);
        } else {
            intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(mImageFile));
        }
        // 指定保存格式
        intent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString());
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(intent, REQ_CAPTURE_CLIP_3);
        }
    }

    // 打开相册并剪裁
    private void startOpenAlbumThenOpen() {
        mImageFile = createImageFile(true);
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {//如果大于等于7.0使用FileProvider
            Uri uriForFile = FileProvider.getUriForFile(this, AUTHORITY, mImageFile);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, uriForFile);
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivityForResult(intent, REQ_OPEN_ALBUM_N_5);
        } else {
            startActivityForResult(intent, REQ_OPEN_ALBUM_4);
        }
    }

    // 录屏并播放
    private void startCapVideoThenPlay() {
        Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        if (intent.resolveActivity(getPackageManager()) != null)
            startActivityForResult(intent, REQ_VIDEO_CAPTURE_6);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Outil.log("onActivityResult, requestCode = " + requestCode + "resultCode=" + resultCode);

        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQ_THUMB_CAPTURE_1) {
                showThumbImg(resultShow, data);
                tipText.setText("缩略图：");
            } else if (requestCode == REQ_ORIGIN_CAPTURE_2) {
                tipText.setText("真实图：");
                //resultShowOriginImg(resultShow);
                resultshowCompressImg(resultShow);
            } else if (requestCode == REQ_CAPTURE_CLIP_3) {
                tipText.setText("拍照剪裁：");
                Uri outImgUri = FileProvider.getUriForFile(this, AUTHORITY, mImageFile);
                startPhotoZoom(outImgUri);
            } else if (requestCode == REQ_OPEN_ALBUM_N_5) {
                tipText.setText("相册剪裁：");
                File imgUri = new File(mImageFile, String.valueOf(data.getData()));
                Uri dataUri = FileProvider.getUriForFile(this, AUTHORITY, imgUri);
                startPhotoZoom(dataUri);
            } else if (requestCode == REQ_OPEN_ALBUM_4) {
                tipText.setText("相册剪裁：");
                startPhotoZoom(data.getData());
            } else if (requestCode == REQ_SYS_CLIP_IMAGE_7) {
                resultShow.setImageBitmap(BitmapFactory.decodeFile(mCropFile.getAbsolutePath()));
            } else { // REQ_VIDEO_CAPTURE_6
                videoView.setVisibility(View.VISIBLE);
                videoView.setVideoURI(data.getData());
                videoView.start();
                Outil.log("video uri: " + data.getData());
            }
        }
    }


    // 创建照片文件路径
    private File createImageFile(boolean isCope) {
        File rootFile = new File(ROOT_FOLDER_PATH + File.separator + "capture");
        if (!rootFile.exists()) {
            rootFile.mkdirs();
        }
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String fileName = "IMG" + timeStamp + ".jpg";
        if (isCope) {
            fileName = "IMG" + timeStamp + "_cope.jpg";
        }
        ;
        return new File(rootFile.getAbsolutePath() + File.separator + fileName);
    }

    // 缩略图展示
    private void showThumbImg(ImageView view, Intent data) {
        Bundle bundle = data.getExtras();
        Bitmap bitmap = (Bitmap) bundle.get("data");
        Outil.log("thumb height = " + bitmap.getHeight());
        resultShow.setImageBitmap(bitmap);
    }

    // 原图展示
    private void resultShowOriginImg(ImageView view) {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(mImageFile);
            Bitmap bitmap = BitmapFactory.decodeStream(fis);
            view.setImageBitmap(bitmap);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            try {
                fis.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // 展示压缩图片
    private void resultshowCompressImg(ImageView view) {
        if (mImageFile.exists()) {
            new CompressImgTask(view, 2).execute(mImageFile.getAbsolutePath());
        }
    }

    @Override
    protected void onDestroy() {
        if (videoView.isPlaying()) {
            videoView.pause();
        }
        super.onDestroy();
    }

    /**
     * 裁剪图片方法实现
     *
     * @param inputUri
     */
    public void startPhotoZoom(Uri inputUri) {
        if (inputUri == null) {
            Outil.log("The uri is not exist.");
            return;
        }
        Outil.log("cutImg , uri = " + inputUri);

        Intent intent = new Intent("com.android.camera.action.CROP");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {

            Uri outPutUri = Uri.fromFile(mCropFile);
            intent.setDataAndType(inputUri, "image/*");
            intent.putExtra(MediaStore.EXTRA_OUTPUT, outPutUri);
            //去除默认的人脸识别，否则和剪裁匡重叠
            intent.putExtra("noFaceDetection", false);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

        } else {
            /*Uri outPutUri = Uri.fromFile(mCropFile);
            if (Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                //这个方法是处理4.4以上图片返回的Uri对象不同的处理方法
                String url = GetImagePath.getPath(this, inputUri);
                intent.setDataAndType(Uri.fromFile(new File(url)), "image/*");
            } else {
                intent.setDataAndType(inputUri, "image/*");
            }
            intent.putExtra(MediaStore.EXTRA_OUTPUT, outPutUri);*/
            intent.setDataAndType(Uri.fromFile(mImageFile), "image/*");
            intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(mCropFile));
        }

        // 设置裁剪
        intent.putExtra("crop", "true");
        // aspectX aspectY 是宽高的比例
        intent.putExtra("aspectX", 1);
        intent.putExtra("aspectY", 1);
        // outputX outputY 是裁剪图片宽高
        intent.putExtra("outputX", 200);
        intent.putExtra("outputY", 200);
        intent.putExtra("return-data", false);
        intent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString());// 图片格式
        startActivityForResult(intent, REQ_SYS_CLIP_IMAGE_7);//返回剪裁后
    }

}
