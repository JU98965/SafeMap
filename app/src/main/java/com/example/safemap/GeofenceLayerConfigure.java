package com.example.safemap;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import java.util.ArrayList;
import java.util.List;

public class GeofenceLayerConfigure {
    //지오펜싱 클라이언트의 인스턴스를 만들기 위한 변수
    private GeofencingClient geofencingClient;
    //지오펜싱 갯수 리스트
    private List<Geofence> geofenceList;
    private PendingIntent geofencePendingIntent;

    //생성자
    public GeofenceLayerConfigure(Context context){
        geofencingClient = LocationServices.getGeofencingClient(context);
        geofenceList = new ArrayList<>();
    }


    public void  buildGeofence(String RequestId, double la, double lo, float radius){
        geofenceList.add(new Geofence.Builder()
                .setRequestId(RequestId)// 이벤트 발생시 BroadcastReceiver에서 구분할 id
                .setCircularRegion(la, lo, radius)// 위치 및 반경(m 단위)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)// Geofence 만료 시간(밀리세컨트 단위이지만 이 경우는 시간이 만료되지 않음)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT)// '|' 비트연산자 or
                .build()
        );
    }

    public GeofencingRequest getGeofencingRequest() {
        //지오펜싱 리퀘스트 빌더 객체 만들기
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
        //처음부터 Geofence 내에 있던 사용자에게도 진입 이벤트를 보냄
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);
        //모니터링 할 지오펜스 객체
        builder.addGeofences(geofenceList);
        return builder.build();
    }

    public PendingIntent getGeofencePendingIntent(Context context, Class cls) {
        // Reuse the PendingIntent if we already have it.
        if (geofencePendingIntent != null) {
            return geofencePendingIntent;
        }
        // Geofence 이벤트를 수신할 PendingIntent 생성
        Intent intent = new Intent(context, cls);
        //안드로이드 12버전 이상을 타게팅한다면 FLAG_MUTABLE이나 FLAG_IMMUTABLE을 사용해야 함, 이 경우 펜딩인텐트가 계속 업데이트 되기 때문에 FLAG_MUTABLE를 사용함
        geofencePendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
        return geofencePendingIntent;
    }

    public void addGeofence(Activity context, GeofencingRequest request, PendingIntent pendingIntent){
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            //퍼미션 체크 있는 김에 지오펜스 추가 메서드를 여기에 두기로 함
            geofencingClient.addGeofences(request, pendingIntent)
                    .addOnSuccessListener(context, new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            // Geofences added
                            // ...
                            Log.d("지오펜스 추가 성공", "추가 성공!");
                        }
                    })
                    .addOnFailureListener(context, new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            // Failed to add geofences
                            // ...
                            Log.d("지오펜스 추가 실패", "추가 실패! : " + e);
                        }
                    });
        }
    }




}
