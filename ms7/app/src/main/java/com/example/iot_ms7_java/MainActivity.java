package com.example.iot_ms7_java;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Fragment_1 firstFragment = new Fragment_1();
        Fragment_2 secondFragment = new Fragment_2();
        Button fragmentOneButton = findViewById(R.id.btnFrag1);
        Button fragmentTwoButton = findViewById(R.id.btnFrag2);

        // using fragment transaction
        // first fragment set as default
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.flFragment, firstFragment)
                    .commit();
        }

        fragmentOneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.flFragment, firstFragment)
                        .addToBackStack(null) // back arrow functions
                        .commit();
            }
        });

        fragmentTwoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.flFragment, secondFragment)
                        .addToBackStack(null)
                        .commit();
            }
        });

        // click on buttons and change fragment

    }
}