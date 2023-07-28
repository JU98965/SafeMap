package com.example.safemap;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.example.safemap.GeofenceLayerConfigure;
import com.example.safemap.PositionLayer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import android.Manifest;


public class GoogleMapActivity extends AppCompatActivity implements OnMapReadyCallback,
        ActivityCompat.OnRequestPermissionsResultCallback,
        GoogleMap.OnMyLocationButtonClickListener{
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private GoogleMap map;
    RequestQueue queue;

    //포지션 데이터들
    PositionLayer police = new PositionLayer();
    PositionLayer cam = new PositionLayer();

    GeofenceLayerConfigure geofenceLayerPolice;
    GeofenceLayerConfigure geofenceLayerCam;

    MakerCircleLayer policeMC = new MakerCircleLayer();
    MakerCircleLayer camMC = new MakerCircleLayer();

    Boolean key1 = false;
    Boolean key2 = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_google_map);

        //액션바 없애는 코드 2줄
        ActionBar actionBar = getSupportActionBar();
        actionBar.hide();

        TextView textView = (TextView) findViewById(R.id.tvSafeLevel);
        Button btnTest = (Button) findViewById(R.id.btnTest);

        //지오펜스 레이어들 초기화
        geofenceLayerPolice = new GeofenceLayerConfigure(this);
        geofenceLayerCam = new GeofenceLayerConfigure(this);

        //웹 통신 큐 초기화(null이면 값을 초기화)
        if (queue == null) {
            queue = Volley.newRequestQueue(this);
        }

        String url = "https://www.seogu.go.kr/seoguAPI/3660000/getPolcSttn";
        //인스턴스 초기값으로 들어갈 옵션, 콜백함수? 지정 그리고 add()로 큐에 집어넣고 웹통신 실행하는 방식
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET,
                url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    //왜 이렇게 한꺼풀씩 벗겨야하는지는 모르겠음
                    JSONObject header = response.getJSONObject("response");
                    JSONObject body = header.getJSONObject("body");
                    JSONArray arrItems = body.getJSONArray("items");
                    for (int i = 0; i < arrItems.length(); i++) {
                        JSONObject obj = arrItems.getJSONObject(i);
                        police.nm.add(obj.getString("polcsttn_nm"));
                        police.la.add(obj.getDouble("ydnts"));
                        police.lo.add(obj.getDouble("xcnts"));
                    }
                    for (int i = 0; i < police.nm.size(); i++) {
                        geofenceLayerPolice.buildGeofence("POLICE_" + i, police.la.get(i), police.lo.get(i), 300);
                    }
                    geofenceLayerPolice.buildGeofence("GEOFENCE1",36.774957, 127.132041, 300);
                    geofenceLayerPolice.addGeofence(GoogleMapActivity.this,
                            geofenceLayerPolice.getGeofencingRequest(),
                            geofenceLayerPolice.getGeofencePendingIntent(GoogleMapActivity.this, BroadcastReceiver.class)
                    );
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                //오류 발생 시 실행
                Log.d("웹 통신 오류", "onErrorResponse: "+error);
            }
        });
        String urlCam = "https://apis.data.go.kr/6300000/openapi2022/safeCCTV/getsafeCCTV?serviceKey=CwriQJZKSP6y2LQaHXDLSHPmEXZx5l05UssRhMEGYtwmLuihSXIxCOgf4k846%2FlnMV6sj6lcx29IS%2F0k6bbNpA%3D%3D&pageNo=1&numOfRows=10";
        //인스턴스 초기값으로 들어갈 옵션, 콜백함수? 지정 그리고 add()로 큐에 집어넣고 웹통신 실행하는 방식
        JsonObjectRequest jsonObjectRequestCam = new JsonObjectRequest(Request.Method.GET,
                urlCam, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    //왜 이렇게 한꺼풀씩 벗겨야하는지는 모르겠음
                    JSONObject header = response.getJSONObject("response");
                    JSONObject body = header.getJSONObject("body");
                    JSONArray arrItems = body.getJSONArray("items");
                    for (int i = 0; i < arrItems.length(); i++) {
                        JSONObject obj = arrItems.getJSONObject(i);
                        cam.nm.add(obj.getString("mgcNm"));
                        cam.la.add(obj.getDouble("crdntY"));
                        cam.lo.add(obj.getDouble("crdntX"));
                    }

                    for (int i = 0; i < cam.nm.size(); i++) {
                        geofenceLayerCam.buildGeofence("CAM_" + i, cam.la.get(i), cam.lo.get(i), 300);
                    }
                    geofenceLayerCam.buildGeofence("GEOFENCE2", 36.775236, 127.131880, 300);
                    geofenceLayerCam.addGeofence(GoogleMapActivity.this,
                            geofenceLayerCam.getGeofencingRequest(),
                            geofenceLayerCam.getGeofencePendingIntent(GoogleMapActivity.this, GeofenceBroadcastReceiverCam.class)
                    );
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                //오류 발생 시 실행
                Log.d("웹 통신 오류", "onErrorResponse: "+error);
            }
        });
        //지도하고 웹통신 실행 코드
        queue.add(jsonObjectRequest);
        queue.add(jsonObjectRequestCam);

        // BroadcastReceiver를 등록합니다.
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                TextView tvSafeLevel = (TextView) findViewById(R.id.tvSafeLevel);
                // Broadcast를 수신하면 int 데이터를 추출하여 처리하는 로직을 구현합니다.
                int data1 = intent.getIntExtra("key",0);
                int data2 = intent.getIntExtra("key2",0);
                Log.d("키 값 테스트 ", "onReceive: "+key1 + key2);
                if(data1 == 1){
                    key1 = true;
                    Log.d("지오펜스 지역 진입", "onReceive: 감지됨");
                }else if(data1 == 2){
                    key1 = false;
                }else if(data2 == 1){
                    key2 = true;
                } else if (data2 == 2) {
                    key2 = false;
                }

                if(key1 && key2){
                    tvSafeLevel.setText("2단계");
                }else if(key1 || key2){
                    tvSafeLevel.setText("1단계");
                }else{
                    tvSafeLevel.setText("0단계");
                }
            }
        };
        IntentFilter filter = new IntentFilter("your_broadcast_action");
        registerReceiver(receiver, filter);

        btnTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //지도실행 콜백 메서드, 네트워크 통신 끝나고 지도 표시하게 만듦
                SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.googleMap);
                mapFragment.getMapAsync(GoogleMapActivity.this);
            }
        });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        //위쪽에 클래스 변수로 이미 선언되어 있지만, 여기서 초기화 안해주면 널 오프젝트라고 에러뱉음
        map = googleMap;

        map.setOnMyLocationButtonClickListener(this);
        enableMyLocation();

        LatLng startPoint = new LatLng(36.318397, 127.366594);

        //경찰서 마커, 원형 폴리곤 그리기
        for(int i=0;i<police.nm.size();i++){
            //마커 생성 (마커를 나타냄)
            map.addMarker(policeMC.configureMaker(police.la.get(i), police.lo.get(i),police.nm.get(i)));
            //원 영역 그리기
            map.addCircle(policeMC.configureCircle(police.la.get(i), police.lo.get(i), 300, 167,215,95));
        }
        //우리집앞에 영역그리기 테스트용
        map.addCircle(policeMC.configureCircle(36.774957, 127.132041, 30, 167,215,95));

        //방범 카메라 마커, 원형 폴리곤 그리기
        for(int i=0;i<cam.nm.size();i++){
            //마커 생성 (마커를 나타냄)
            map.addMarker(camMC.configureMaker(cam.la.get(i), cam.lo.get(i), cam.nm.get(i)));
            //원 영역 그리기
            map.addCircle(camMC.configureCircle(cam.la.get(i), cam.lo.get(i), 300, 249,74,75));
        }

        map.moveCamera(CameraUpdateFactory.newLatLngZoom(startPoint, 16));
    }

    @Override
    public boolean onMyLocationButtonClick() {
        Toast.makeText(this, "현재위치로 이동합니다.", Toast.LENGTH_SHORT)
                .show();
        return false;
    }

    //말이 enableMyLocation이지 사실상 퍼미션 확인하는 함수
    @SuppressLint("MissingPermission")
    private void enableMyLocation() {
        // 1. 사용 권한이 부여되었는지 확인하고, 부여된 경우 내 위치 계층 사용
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            Log.d("권한체크", "권한있음, 위치레이어 실행");
            map.setMyLocationEnabled(true);
            return;
        }
        // 2. 그렇지 않으면 사용자에게 위치 권한을 요청합니다.
        Log.d("권한체크", "권한없음, 권한요청");
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                LOCATION_PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode != LOCATION_PERMISSION_REQUEST_CODE) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            return;
        }
        switch (requestCode) {
            case LOCATION_PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // 권한 허가
                    // 해당 권한을 사용해서 작업을 진행할 수 있습니다
                    Log.d("권한요청 결과", "권한허가 : enableMyLocation() 실행");
                    enableMyLocation();
                } else {
                    // 권한 거부
                    // 사용자가 해당권한을 거부했을때 해주어야 할 동작을 수행합니다
                    Log.d("권한요청 결과", "권한거부 : enableMyLocation() 실행 불가!");
                }
                return;
        }
    }
}