package com.example.safemap;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;

import androidx.core.content.ContextCompat;

import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class MakerCircleLayer {

    public MarkerOptions configureMaker(double la, double lo, String nm, int makerImg, Context context) {

        int height = 100;

        int width = 100;

        BitmapDrawable bitmapdraw=(BitmapDrawable) ContextCompat.getDrawable(context, makerImg);

        Bitmap b=bitmapdraw.getBitmap();

        Bitmap smallMarker = Bitmap.createScaledBitmap(b, width, height, false);


        //마커 옵션 설정 (만드는 과정)
        MarkerOptions makerOptions = new MarkerOptions();
        makerOptions // LatLng에 대한 어레이를 만들어서 이용할 수도 있다.
                .position(new LatLng(la, lo))
                .title(nm) // 타이틀.
//                .icon(BitmapDescriptorFactory.fromResource(makerImg));
                .icon(BitmapDescriptorFactory.fromBitmap(smallMarker));
        return makerOptions;
        //이 함수 리턴값을 addMaker()안에다가 넣어줌
//        map.addMarker(configureMaker());
    }

    public CircleOptions configureCircle(double la, double lo, double circleRadius, int r, int g, int b){
        //원 영역 그리기
        CircleOptions circleOptions = new CircleOptions()
                .center(new LatLng(la, lo))
                .radius(circleRadius)
                .strokeWidth(3)
                //argb값은  각255가 최대 0으로 갈수로 투명
                .strokeColor(Color.argb(200, 164,116,82))
                .fillColor(Color.argb(100, 152,209,254));
        return circleOptions;
        //이 함수 리턴값을 addCircle()에다가 넣어줌
//        map.addCircle(configureCircle());
    }
}
