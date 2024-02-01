package com.appcomponents.demo;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import com.appcomponents.demo.ui.ExoplayerActivity;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Find the button with the id 'btn_exoplayer' in the current layout and assign it to the variable 'btn_exoplayer'.
        Button btn_exoplayer = findViewById(R.id.btn_exoplayer);

        // Set an OnClickListener for the 'btn_exoplayer' button.
        btn_exoplayer.setOnClickListener(view -> {
            // Create an Intent to launch the ExoplayerActivity.
            Intent intent = new Intent(this, ExoplayerActivity.class);
            // Start the ExoplayerActivity.
            startActivity(intent);
        });
    }
}