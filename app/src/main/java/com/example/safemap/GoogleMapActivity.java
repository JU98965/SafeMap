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
        GoogleMap.OnMyLocationButtonClickListener, GoogleMap.OnMyLocationClickListener{

    private BroadcastReceiver broadcastReceiver;

    //지오펜싱 클라이언트의 인스턴스를 만들기 위한 변수
    private GeofencingClient geofencingClient;

    //지오펜싱 갯수 리스트
    private List<Geofence> geofenceList = new ArrayList<>();

    private FusedLocationProviderClient fusedLocationClient;

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    private boolean permissionDenied = false;
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

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.googleMap);
        mapFragment.getMapAsync(this);

        TextView textView = (TextView) findViewById(R.id.tvSafeLevel);

        //초기화(null이면 값을 초기화)
        if (queue == null) {
            queue = Volley.newRequestQueue(this);
        }

        String url = "https://www.seogu.go.kr/seoguAPI/3660000/getPolcSttn";
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
//        Log.d("지오펜스 객체 사이즈 체크1", Integer.toString(geofenceList.size()));

                    //지오펜스 빌더 객체 자체를 리스트에 추가 우리 집 앞 지오펜스 테스트용 객체
                    geofenceList.add(new Geofence.Builder()
                            .setRequestId("geo1")// 이벤트 발생시 BroadcastReceiver에서 구분할 id
                            .setCircularRegion(36.774957, 127.132041, 300)// 위치 및 반경(m 단위)
                            .setExpirationDuration(Geofence.NEVER_EXPIRE)// Geofence 만료 시간(밀리세컨트 단위이지만 이 경우는 시간이 만료되지 않음)
                            // '|' 비트연산자 or
                            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT)
                            .build()
                    );
                    Log.d("지오펜스 객체 사이즈 체크2", Integer.toString(geofenceList.size()));

                    GeofencingRequest geofencingRequest = new GeofencingRequest.Builder()
                            //모니터링 할 지오펜스 객체
                            .addGeofences(geofenceList)
                            //처음부터 Geofence 내에 있던 사용자에게도 진입 이벤트를 보냄
                            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                            .build();

                    // Geofence 이벤트를 수신할 PendingIntent 생성
                    Intent intent = new Intent(GoogleMapActivity.this, GeofenceBroadcastReceiver.class);

                    Log.d("체크구간1", "여기까지는 실행 잘 되고 있는 중"+intent);
                    PendingIntent geofencePendingIntent = PendingIntent.getBroadcast(GoogleMapActivity.this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_MUTABLE);
                    Log.d("돌아가긴 하는거임??", "onResponse: "+geofencePendingIntent);

                    //일단 퍼미션 체크 만들어 놓긴 했지만 나중에 제대로 퍼미션 체크 구현해 둘 것
                    if (ActivityCompat.checkSelfPermission(GoogleMapActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        // TODO: Consider calling
                        //    ActivityCompat#requestPermissions
                        // here to request the missing permissions, and then overriding
                        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                        //                                          int[] grantResults)
                        // to handle the case where the user grants the permission. See the documentation
                        // for ActivityCompat#requestPermissions for more details.
                        return;
                    }
                    geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent)
                            .addOnSuccessListener(GoogleMapActivity.this, new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    // Geofences added
                                    // ...
                                    Log.d("지오펜스 추가 성공 ", "onSuccess: 지오펜스 추가됨!");
                                }
                            })
                            .addOnFailureListener(GoogleMapActivity.this, new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    // Failed to add geofences
                                    // ...
                                    Log.d("지오펜스 추가 실패", "onFailure: 지오펜스 추가 실패함...왜지" + e);
                                    textView.setText("지오펜스 추가 실패");
                                }
                            });

                    //지도관련, 비동기 같아서 그냥 네트워크 통신 끝나고 지도 표시하게 만듦
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
                System.out.print("에러가 난 거 같은 느낌");
            }
        });
        //지도하고 웹통신 실행 코드
        queue.add(jsonObjectRequest);


        //디스턴스매트릭스 요청 좌표(출발지, 도착지)
        String latLngOri = "36.319868,127.365301";
        String latLngDes = "36.322611,127.366542";

        //디스턴스 메트릭스 통신
        String urlGeo = "https://maps.googleapis.com/maps/api/distancematrix/json?units=metric&mode=transit&origins="
                + latLngOri + "&destinations=" + latLngDes + "&region=KR&key=AIzaSyA9tcEppdquzHnTDpoCvwtpn0KhYfNFiwE";
        JsonObjectRequest jsonObjectRequestGeo = new JsonObjectRequest(Request.Method.GET,
                urlGeo, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    //왜 이렇게 한꺼풀씩 벗겨야하는지는 모르겠음
//                    textOri.setText(response.toString());
                    JSONArray rows = response.getJSONArray("rows");
                    JSONObject rowsArr = rows.getJSONObject(0);
//                    textOri.setText(rows.toString());
                    JSONArray elements = rowsArr.getJSONArray("elements");
                    JSONObject elementsArr = elements.getJSONObject(0);
                    JSONObject distance = elementsArr.getJSONObject("distance");
                    String distanceValue = distance.getString("value");
//                    textOri.setText(distanceValue);


//                    textOri.setText(elements.toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                //오류 발생 시 실행
                System.out.print("에러가 난 거 같은 느낌");
//                textOri.setText("에러: " + error.toString());
            }
        });
        //디스턴스 메트릭스 통신 실행코드
        queue.add(jsonObjectRequestGeo);


        // BroadcastReceiver를 등록합니다.
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                // Broadcast를 수신하면 int 데이터를 추출하여 처리하는 로직을 구현합니다.
                int data = intent.getIntExtra("key", 0);
                // 데이터 처리 작업을 수행합니다.
                Log.d("세이프 레벨", "onReceive: "+data);
//                textView.setText(Integer.toString(data));
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


    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {

        map = googleMap;
        map.setOnMyLocationButtonClickListener(this);
        map.setOnMyLocationClickListener(this);
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
                .radius(300)
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

    @Override
    public void onMyLocationClick(@NonNull Location location) {
        Toast.makeText(this, "Current location:\n" + location, Toast.LENGTH_LONG)
                .show();
    }

    /**
     * Enables the My Location layer if the fine location permission has been granted.
     */
    @SuppressLint("MissingPermission")
    private void enableMyLocation() {
        // 1. 사용 권한이 부여되었는지 확인하고, 부여된 경우 내 위치 계층 사용
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            Log.d("뭐가 먼저 실행되는지 알아보자", "체크2체크2체크2체크2체크2체크2체크2체크2체크2체크2체크2체크2체크2");
            map.setMyLocationEnabled(true);
            return;
        }
        // 2. 그렇지 않으면 사용자에게 위치 권한을 요청합니다.
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
                    Log.d("뭐가 먼저 실행되는지 알아보자", "체크1체크1체크1체크1체크1체크1체크1체크1체크1체크1체크1체크1체크1체크1체크1체크1");
                    enableMyLocation();
                } else {
                    // 권한 거부
                    // 사용자가 해당권한을 거부했을때 해주어야 할 동작을 수행합니다
                }
                return;
        }
    }

}