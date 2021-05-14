package com.jesen.cod.camerafunction.activity;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;

import com.jesen.cod.camerafunction.R;

public class SystemCameraActivity extends AppCompatActivity {

    private Button startSysBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_system_camera);

        initView();
    }

    private void initView(){
        startSysBtn = findViewById(R.id.startSysCameraBtn);

        startSysBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                startActivity(intent);
            }
        });
    }
}
