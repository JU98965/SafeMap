package com.example.safemap;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofenceStatusCodes;
import com.google.android.gms.location.GeofencingEvent;

public class GeofenceBroadcastReceiverCam extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        //인텐트에서 이벤트 정보 받아옴
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
        if (geofencingEvent.hasError()) {
            String errorMessage = GeofenceStatusCodes.getStatusCodeString(geofencingEvent.getErrorCode());
            Log.e("지오펜스 에러", errorMessage);
            return;
        }

        // Geofence 이벤트 처리
        int geofenceTransition = geofencingEvent.getGeofenceTransition();
        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            // Geofence 진입 이벤트 처리
            Log.d("지오펜스 지역에 진입", "지오펜스 지역에 진입, 지오펜스 지역에 진입, 지오펜스 지역에 진입");
//            // 데이터를 Activity로 전달하기 위해 Broadcast를 전송합니다.
            Intent broadcastIntent = new Intent();
            broadcastIntent.setAction("your_broadcast_action");
            broadcastIntent.putExtra("key", 1);
            context.sendBroadcast(broadcastIntent);

        } else if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {
            // Geofence 이탈 이벤트 처리
            Log.d("지오펜스 지역 이탈", "지오펜스 지역 이탈, 지오펜스 지역 이탈, 지오펜스 지역 이탈, 지오펜스 지역 이탈");
            Intent broadcastIntent = new Intent();
            broadcastIntent.setAction("your_broadcast_action");
            broadcastIntent.putExtra("key", 2);
            context.sendBroadcast(broadcastIntent);
        }

        Log.d("브로드캐스트 리시버", "지오펜스 브로드캐스트 리시버");
    }
}
