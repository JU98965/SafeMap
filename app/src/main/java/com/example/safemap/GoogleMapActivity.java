package com.example.safemap;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import android.Manifest;

public class GoogleMapActivity extends AppCompatActivity implements OnMapReadyCallback,
        ActivityCompat.OnRequestPermissionsResultCallback,
        GoogleMap.OnMyLocationButtonClickListener{

    //지오펜싱 클라이언트의 인스턴스를 만들기 위한 변수
    private GeofencingClient geofencingClient;
    //지오펜싱 갯수 리스트
    private List<Geofence> geofenceList = new ArrayList<>();
    private PendingIntent geofencePendingIntent;

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private GoogleMap map;
    RequestQueue queue;

    List<String> nm = new ArrayList<>();
    List<Double> la = new ArrayList<>();
    List<Double> lo = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_google_map);

        //액션바 없애는 코드 2줄
        ActionBar actionBar = getSupportActionBar();
        actionBar.hide();

        geofencingClient = LocationServices.getGeofencingClient(this);

        TextView textView = (TextView) findViewById(R.id.tvSafeLevel);

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
                        nm.add(obj.getString("polcsttn_nm"));
                        la.add(obj.getDouble("ydnts"));
                        lo.add(obj.getDouble("xcnts"));
                    }

                    for (int i = 0; i < nm.size(); i++) {
                        //지오펜스 빌더 객체 자체를 리스트에 추가
                        geofenceList.add(new Geofence.Builder()
                                .setRequestId("Geofence_" + i)// 이벤트 발생시 BroadcastReceiver에서 구분할 id
                                .setCircularRegion(la.get(i), lo.get(i), 300)// 위치 및 반경(m 단위)
                                .setExpirationDuration(Geofence.NEVER_EXPIRE)// Geofence 만료 시간(밀리세컨트 단위이지만 이 경우는 시간이 만료되지 않음)
                                // '|' 비트연산자 or
                                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT)
                                .build()
                        );
                        Log.d("지오펜스 객체 리스트 확인", geofenceList.get(i).toString());
                    }
                    //지오펜스 빌더 객체 자체를 리스트에 추가 우리 집 앞 지오펜스 테스트용 객체
                    geofenceList.add(new Geofence.Builder()
                            .setRequestId("GEOFENCE")// 이벤트 발생시 BroadcastReceiver에서 구분할 id
                            .setCircularRegion(36.774957, 127.132041, 30)// 위치 및 반경(m 단위)
                            .setExpirationDuration(Geofence.NEVER_EXPIRE)// Geofence 만료 시간(밀리세컨트 단위이지만 이 경우는 시간이 만료되지 않음)
                            // '|' 비트연산자 or
                            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT)
                            .build()
                    );
                    Log.d("지오펜스 객체 리스트 확인", geofenceList.get(nm.size()).toString());
                    Log.d("지오펜스 객체 사이즈 체크", Integer.toString(geofenceList.size()));

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
            }
        });
        //지도하고 웹통신 실행 코드
        queue.add(jsonObjectRequest);

        // BroadcastReceiver를 등록합니다.
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                // Broadcast를 수신하면 int 데이터를 추출하여 처리하는 로직을 구현합니다.
                int data = intent.getIntExtra("key", 0);
                // 데이터 처리 작업을 수행합니다.
                Log.d("세이프 레벨", "onReceive: "+data);

                if(data == 0){
                    textView.setText("안전지대 이탈");
                    textView.setTextColor(Color.argb(255,249,74,75));
                }else{
                    textView.setText("안전지대 진입");
                    textView.setTextColor(Color.argb(255,167,215,95));
                }
            }
        };
        IntentFilter filter = new IntentFilter("your_broadcast_action");
        registerReceiver(receiver, filter);
    }

    private GeofencingRequest getGeofencingRequest() {
        //지오펜싱 리퀘스트 빌더 객체 만들기
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
        //처음부터 Geofence 내에 있던 사용자에게도 진입 이벤트를 보냄
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);
        //모니터링 할 지오펜스 객체
        builder.addGeofences(geofenceList);
        return builder.build();
    }

    private PendingIntent getGeofencePendingIntent() {
        // Reuse the PendingIntent if we already have it.
        if (geofencePendingIntent != null) {
            return geofencePendingIntent;
        }
        // Geofence 이벤트를 수신할 PendingIntent 생성
        Intent intent = new Intent(this, GeofenceBroadcastReceiver.class);
        //안드로이드 12버전 이상을 타게팅한다면 FLAG_MUTABLE이나 FLAG_IMMUTABLE을 사용해야 함, 이 경우 펜딩인텐트가 계속 업데이트 되기 때문에 FLAG_MUTABLE를 사용함
        geofencePendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
        return geofencePendingIntent;
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        //위쪽에 클래스 변수로 이미 선언되어 있지만, 여기서 초기화 안해주면 널 오프젝트라고 에러뱉음
        map = googleMap;

        map.setOnMyLocationButtonClickListener(this);
        enableMyLocation();

        LatLng startPoint = new LatLng(36.318397, 127.366594);

        for(int i=0;i<nm.size();i++){
            // 1. 마커 옵션 설정 (만드는 과정)
            MarkerOptions makerOptions = new MarkerOptions();
            makerOptions // LatLng에 대한 어레이를 만들어서 이용할 수도 있다.
                    .position(new LatLng(la.get(i), lo.get(i)))
                    .title(nm.get(i)); // 타이틀.
//            makerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.pin));
            // 2. 마커 생성 (마커를 나타냄)
            map.addMarker(makerOptions);

            //원 영역 그리기
            CircleOptions circleOptions = new CircleOptions()
                    .center(new LatLng( la.get(i), lo.get(i)))
                    .radius(300)
                    .strokeWidth(3)
                    //argb값은  각255가 최대 0으로 갈수로 투명
                    .strokeColor(Color.argb(200, 167,215,95))
                    .fillColor(Color.argb(100, 167,215,95));
            map.addCircle(circleOptions);
        }
        //우리집앞에 영역그리기 테스트용
        CircleOptions circleOptions = new CircleOptions()
                .center(new LatLng(36.774957, 127.132041))
                .radius(30)
                .strokeWidth(3)
                //argb값은  각255가 최대 0으로 갈수로 투명
                .strokeColor(Color.argb(200, 167,215,95))
                .fillColor(Color.argb(100, 167,215,95));
        map.addCircle(circleOptions);

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

            Log.d("지오펜스 리퀘스트 값", "리퀘스트 값: "+getGeofencingRequest());
            Log.d("지오펜스 펜딩인텐트 값", "펜딩인텐트 값: "+getGeofencePendingIntent());
            //퍼미션 체크 있는 김에 지오펜스 추가 메서드를 여기에 두기로 함
            geofencingClient.addGeofences(getGeofencingRequest(), getGeofencePendingIntent())
                    .addOnSuccessListener(this, new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            // Geofences added
                            // ...
                            Log.d("지오펜스 추가 성공", "추가 성공!");
                        }
                    })
                    .addOnFailureListener(this, new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            // Failed to add geofences
                            // ...
                            Log.d("지오펜스 추가 실패", "추가 실패! : " +e);
                            TextView tvSafeLevel = (TextView) findViewById(R.id.tvSafeLevel);
                            tvSafeLevel.setText("지오펜스 추가 실패");
                        }
                    });
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