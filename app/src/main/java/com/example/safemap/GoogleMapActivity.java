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
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
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
import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.Manifest;


public class GoogleMapActivity extends AppCompatActivity implements OnMapReadyCallback,
        ActivityCompat.OnRequestPermissionsResultCallback,
        GoogleMap.OnMyLocationButtonClickListener{
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private GoogleMap map;
    RequestQueue queue;

    //포지션 데이터들
    PositionLayer park = new PositionLayer();
    PositionLayer animalHospital = new PositionLayer();
    PositionLayer streetLight = new PositionLayer();
    PositionLayer cctv = new PositionLayer();

    GeofenceLayerConfigure geofenceLayerPark;
    GeofenceLayerConfigure geofenceLayerAnimalHospital;
    GeofenceLayerConfigure geofenceLayerStreetLight;
    GeofenceLayerConfigure geofenceLayerCCTV;

    MakerCircleLayer parkMC = new MakerCircleLayer();
    MakerCircleLayer animalHospitalMC = new MakerCircleLayer();
    MakerCircleLayer streetLightMC = new MakerCircleLayer();
    MakerCircleLayer cctvMC = new MakerCircleLayer();

    int keyPark = 0;
    int keyAnimalHospital = 0;
    int keyStreetLight = 0;
    int keyCCTV = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_google_map);

        //액션바 없애는 코드 2줄
        ActionBar actionBar = getSupportActionBar();
        actionBar.hide();

        TextView textView = (TextView) findViewById(R.id.tvSafeLevel);
        Button btnRetry = (Button) findViewById(R.id.btnRetry);

        //지오펜스 레이어들 초기화
        geofenceLayerPark = new GeofenceLayerConfigure(this);
        geofenceLayerAnimalHospital = new GeofenceLayerConfigure(this);
        geofenceLayerStreetLight = new GeofenceLayerConfigure(this);
        geofenceLayerCCTV = new GeofenceLayerConfigure(this);

        //웹 통신 큐 초기화(null이면 값을 초기화)
        if (queue == null) {
            queue = Volley.newRequestQueue(this);
        }

        String url = "https://safe-map-sever.run.goorm.site/main";
        //인스턴스 초기값으로 들어갈 옵션, 콜백함수? 지정 그리고 add()로 큐에 집어넣고 웹통신 실행하는 방식
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET,
                url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    //Json 파싱
                    JSONArray arrPark = response.getJSONArray("pagePark");
                    Log.d("일단 잘 파싱 됐는지 확인", "onResponse: "+arrPark);
                    for (int i = 0; i < arrPark.length(); i++) {
                        JSONObject obj = arrPark.getJSONObject(i);
                        park.nm.add(obj.getString("name"));
                        park.la.add(obj.getDouble("la"));
                        park.lo.add(obj.getDouble("lo"));
                    }
                    JSONArray arrAnimalHospital = response.getJSONArray("pageAnimalHospital");
                    for (int i = 0; i < arrAnimalHospital.length(); i++) {
                        JSONObject obj = arrAnimalHospital.getJSONObject(i);
                        animalHospital.nm.add(obj.getString("name"));
                        animalHospital.la.add(obj.getDouble("la"));
                        animalHospital.lo.add(obj.getDouble("lo"));
                    }
                    JSONArray arrStreetLight = response.getJSONArray("pageStreetLight");
                    for (int i = 0; i < arrStreetLight.length(); i++) {
                        JSONObject obj = arrStreetLight.getJSONObject(i);
                        streetLight.nm.add(obj.getString("name"));
                        streetLight.la.add(obj.getDouble("la"));
                        streetLight.lo.add(obj.getDouble("lo"));
                    }
                    JSONArray arrCCTV = response.getJSONArray("pageCCTV");
                    for (int i = 0; i < arrCCTV.length(); i++) {
                        JSONObject obj = arrCCTV.getJSONObject(i);
                        cctv.nm.add(obj.getString("name"));
                        cctv.la.add(obj.getDouble("la"));
                        cctv.lo.add(obj.getDouble("lo"));
                    }
                    Log.d("공원 어레이 크기 확인", "onResponse: "+ park.nm.size());
                    Log.d("동물병원 어레이 크기 확인", "onResponse: "+ animalHospital.nm.size());
                    Log.d("가로등 어레이 크기 확인", "onResponse: "+ streetLight.nm.size());
                    Log.d("카메라 어레이 크기 확인", "onResponse: "+ cctv.nm.size());

                    //지오펜스 구성
                    for (int i = 0; i < park.nm.size(); i++) {
                        geofenceLayerPark.buildGeofence("PARK_" + i, park.la.get(i), park.lo.get(i), 300);
                    }
                    for (int i = 0; i < animalHospital.nm.size(); i++) {
                        geofenceLayerAnimalHospital.buildGeofence("ANIMALHOSPITAL_" + i, animalHospital.la.get(i), animalHospital.lo.get(i), 300);
                    }
                    for (int i = 0; i < streetLight.nm.size(); i++) {
                        geofenceLayerStreetLight.buildGeofence("STREETLIGHT_" + i, streetLight.la.get(i), streetLight.lo.get(i), 10);
                    }
                    for (int i = 0; i < cctv.nm.size(); i++) {
                        geofenceLayerCCTV.buildGeofence("CCTV_" + i, cctv.la.get(i), cctv.lo.get(i), 100);
                    }
                    //테스트용 지오펜스 추가한 거 추후 삭제요망
                    geofenceLayerPark.buildGeofence("GEO1",36.322441, 127.370163, 600);
                    geofenceLayerAnimalHospital.buildGeofence("GEO2",36.322557, 127.369165,600);
                    geofenceLayerStreetLight.buildGeofence("GEO3",36.322471, 127.368806,600);
                    geofenceLayerStreetLight.buildGeofence("GEO4",36.322493, 127.368318,600);
                    geofenceLayerCCTV.buildGeofence("GEO5",36.322475, 127.368672,600);

                    //지오펜스 추가
                    geofenceLayerPark.addGeofence(GoogleMapActivity.this,
                            geofenceLayerPark.getGeofencingRequest(),
                            geofenceLayerPark.getGeofencePendingIntent(GoogleMapActivity.this, GeofenceBroadcastReceiverPark.class)
                    );
                    geofenceLayerAnimalHospital.addGeofence(GoogleMapActivity.this,
                            geofenceLayerAnimalHospital.getGeofencingRequest(),
                            geofenceLayerAnimalHospital.getGeofencePendingIntent(GoogleMapActivity.this, GeofenceBroadcastReceiverAnimalHospital.class)
                    );
                    geofenceLayerStreetLight.addGeofence(GoogleMapActivity.this,
                            geofenceLayerStreetLight.getGeofencingRequest(),
                            geofenceLayerStreetLight.getGeofencePendingIntent(GoogleMapActivity.this, GeofenceBroadcastReceiverStreetLight.class)
                    );
                    geofenceLayerCCTV.addGeofence(GoogleMapActivity.this,
                            geofenceLayerCCTV.getGeofencingRequest(),
                            geofenceLayerCCTV.getGeofencePendingIntent(GoogleMapActivity.this, GeofenceBroadcastReceiverCCTV.class)
                    );

                    textView.setText("0단계");

                    //지도실행 콜백 메서드, 네트워크 통신 끝나고 지도 표시하게 만듦
                    SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                            .findFragmentById(R.id.googleMap);
                    mapFragment.getMapAsync(GoogleMapActivity.this);

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                //오류 발생 시 실행
                Log.d("웹 통신 오류", "onErrorResponse: "+error);
                textView.setText("서버통신 오류!");
                btnRetry.setVisibility(View.VISIBLE);
            }
        });

        //volley 타임아웃 시간 제어, 원래는 2500마이크로초(2.5초)
        jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(
                7000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        //지도하고 웹통신 실행 코드
        queue.add(jsonObjectRequest);

        btnRetry.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //지도하고 웹통신 실행 코드
                queue.add(jsonObjectRequest);
                btnRetry.setVisibility(View.INVISIBLE);
                textView.setText("재요청중..");
            }
        });

        // BroadcastReceiver를 등록합니다.
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                TextView tvSafeLevel = (TextView) findViewById(R.id.tvSafeLevel);
                // Broadcast를 수신하면 int 데이터를 추출하여 처리하는 로직을 구현합니다.
                int dataPark = intent.getIntExtra("keyPark",3);
                int dataAnimalHospital = intent.getIntExtra("keyAnimalHospital",3);
                int dataStreetLight = intent.getIntExtra("keyStreetLight", 3);
                int dataCCTV = intent.getIntExtra("keyCCTV", 3);

                Log.i("기본값 어떻게 들어오는지 확인용", "onReceive: \n" + dataPark + "\n" + dataAnimalHospital + "\n" + dataStreetLight + "\n" + dataCCTV);

                //스위치케이스는 여기 못쓰겠더라..ㅠ
                if(dataPark == 1){
                    keyPark = 1;
                    Log.d("지오펜스 지역 진입", "onReceive: 감지됨");
                }else if(dataPark == 2){
                    keyPark = 0;
                }
                if(dataAnimalHospital == 1){
                    keyAnimalHospital = 1;
                }else if(dataAnimalHospital == 2) {
                    keyAnimalHospital = 0;
                }
                if(dataStreetLight == 1){
                    keyStreetLight = 1;
                }else if(dataStreetLight == 2){
                    keyStreetLight = 0;
                }
                if(dataCCTV == 1){
                    keyCCTV = 1;
                }else if(dataCCTV ==2){
                    keyCCTV = 0;
                }

                if(keyCCTV + keyPark + keyStreetLight + keyAnimalHospital == 4){
                    tvSafeLevel.setText("4단계");
                }else if(keyCCTV + keyPark + keyStreetLight + keyAnimalHospital == 3){
                    tvSafeLevel.setText("3단계");
                }else if(keyCCTV + keyPark + keyStreetLight + keyAnimalHospital == 2){
                    tvSafeLevel.setText("2단계");
                }else if(keyCCTV + keyPark + keyStreetLight + keyAnimalHospital == 1){
                    tvSafeLevel.setText("1단계");
                }else{
                    tvSafeLevel.setText("0단계");
                }
            }
        };
        IntentFilter filter = new IntentFilter("your_broadcast_action");
        registerReceiver(receiver, filter);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        //위쪽에 클래스 변수로 이미 선언되어 있지만, 여기서 초기화 안해주면 널 오프젝트라고 에러뱉음
        map = googleMap;

        map.setOnMyLocationButtonClickListener(this);
        enableMyLocation();

        LatLng startPoint = new LatLng(36.318397, 127.366594);

        //공원 마커, 원형 폴리곤 그리기
        for(int i = 0; i< park.nm.size(); i++){
            //마커 생성 (마커를 나타냄)
            map.addMarker(parkMC.configureMaker(park.la.get(i), park.lo.get(i), park.nm.get(i), R.drawable.maker_park, this));
            //원 영역 그리기
            map.addCircle(parkMC.configureCircle(park.la.get(i), park.lo.get(i), 300, 167,215,95));
        }

        //동물병원 마커, 원형 폴리곤 그리기
        for(int i=0;i<animalHospital.nm.size();i++){
            //마커 생성 (마커를 나타냄)
            map.addMarker(animalHospitalMC.configureMaker(animalHospital.la.get(i), animalHospital.lo.get(i), animalHospital.nm.get(i), R.drawable.maker_vet, this));
            //원 영역 그리기
            map.addCircle(animalHospitalMC.configureCircle(animalHospital.la.get(i), animalHospital.lo.get(i), 300, 249,74,75));
        }

        //가로등 마커, 원형 폴리곤 그리기
        for(int i=0;i<streetLight.nm.size();i++){
            //마커 생성 (마커를 나타냄)
            map.addMarker(streetLightMC.configureMaker(streetLight.la.get(i), streetLight.lo.get(i), streetLight.nm.get(i), R.drawable.maker_streetlight, this));
            //원 영역 그리기
            map.addCircle(streetLightMC.configureCircle(streetLight.la.get(i), streetLight.lo.get(i), 10, 250,239,92));
        }

        //CCTV 마커, 원형 폴리곤 그리기
        for(int i=0;i<cctv.nm.size();i++){
            //마커 생성 (마커를 나타냄)
            map.addMarker(cctvMC.configureMaker(cctv.la.get(i), cctv.lo.get(i), "방범 카메라", R.drawable.maker_cctv, this));
            //원 영역 그리기
            map.addCircle(cctvMC.configureCircle(cctv.la.get(i), cctv.lo.get(i), 100, 17,60,225));
        }

        //디버깅용 코드 추후 삭제요망
        map.addCircle(parkMC.configureCircle(36.322441, 127.370163, 600, 167,215,95));
        map.addCircle(animalHospitalMC.configureCircle(36.322557, 127.369165,600, 249,74,75));
        map.addCircle(streetLightMC.configureCircle(36.322471, 127.368806,600, 250,239,92));
        map.addCircle(streetLightMC.configureCircle(36.322493, 127.368318,15, 250,239,92));
        map.addCircle(cctvMC.configureCircle(36.322475, 127.368672,600, 17,60,225));

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