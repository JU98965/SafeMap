package com.example.safemap;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        TextView txtView = (TextView) findViewById(R.id.txtView);
        Log.d("메인 텍스트 뷰", "onCreate: "+txtView);
        txtView.setText("타이틀");
        //지도화면 넘어가는 버튼
        Button btnGoMap = (Button) findViewById(R.id.btnGoMap);
        //지도화면 넘어가는 버튼 액션함수
        btnGoMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), GoogleMapActivity.class);
                startActivity(intent);
            }
        });

    }
}

