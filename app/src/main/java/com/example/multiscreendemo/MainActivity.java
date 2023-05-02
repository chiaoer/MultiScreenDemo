package com.example.multiscreendemo;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.display.DisplayManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.widget.Button;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private Display[] displays;
    private LinearLayout root;
    private List<MyPresentation> presentations = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        root = findViewById(R.id.root);
        checkNeedPermissions();
        //createPres();
    }

    private void createPres() {
        DisplayManager manager = (DisplayManager) getSystemService(Context.DISPLAY_SERVICE);
        displays = manager.getDisplays();
        Log.d("MultiScreen", "displays>>>" + displays.length);
        for(int i = 1; i < displays.length; i++) {
            //remove virtual dispaly that can't add view and cause crash.
            if(!(displays[i].getName().toLowerCase().contains(("Virtual").toLowerCase()))) {
                final MyPresentation presentation = new MyPresentation(this, displays[i], i % 2, i);
                presentation.show();
                presentations.add(presentation);
                Button button = new Button(this);
                button.setText("switch for screen " + i);
                button.setOnClickListener(v -> presentation.changeContent());
                root.addView(button);
            }
        }
    }

    private void testVideo() {
    }

    private void checkNeedPermissions(){
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            //多个权限一起申请
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE
            }, 1);
        }else {
            createPres();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case 1:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    createPres();
                } else {
                    //Toast.makeText(this, "You denied the permission", Toast.LENGTH_SHORT).show();
                    checkNeedPermissions();
                }
                break;
            default:
                break;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("test", "test_onDestroy");
        if(presentations.size() > 0){
            for(int i = 0; i < presentations.size(); i++) {
                presentations.get(i).
                        releaseVideo();
            }
        }
    }
}