package com.example.cmc7.sensationble;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

public class DeviceControlActivity extends AppCompatActivity {
    TextView debug_txtView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_control);
        debug_txtView = (TextView) findViewById(R.id.textView);

        //TOOD: Under Construction
    }
}
