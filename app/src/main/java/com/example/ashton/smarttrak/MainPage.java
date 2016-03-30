package com.example.ashton.smarttrak;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class MainPage extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_page);
        setTitle("SmartTrak Home");

        Intent intent = getIntent();
        // getExtra methods go here
        String teststr = intent.getStringExtra("key");
        setTitle(teststr);

    }
}
