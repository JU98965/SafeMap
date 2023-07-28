package com.example.safemap;

import java.util.ArrayList;
import java.util.List;

public class PositionLayer {
    public List<String> nm;
    public List<Double> la;
    public List<Double> lo;

    //생성자
    public PositionLayer(){
        nm = new ArrayList<>();
        la = new ArrayList<>();
        lo = new ArrayList<>();
    }
}
