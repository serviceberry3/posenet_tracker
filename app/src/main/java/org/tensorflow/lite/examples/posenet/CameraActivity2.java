package org.tensorflow.lite.examples.posenet;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.Toast;

public class CameraActivity2 extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        setContentView(R.layout.tfe_pn_activity_camera);
        Toast.makeText(getApplicationContext(), "Welcome to CameraActivity.kt", Toast.LENGTH_SHORT).show();


        //add the PoseNet submodule into the activity
        if (savedInstanceState==null) {
            /*FragmentManager*/getSupportFragmentManager()./*FragmentTransaction*/beginTransaction().
                    replace(R.id.container, PosenetActivity.this).commit();
        }
    }
}