package com.kc.filesync;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Kaustav on 27-Aug-16.
 */
public class Capsule {

    private Map<String, Integer> mapi = new HashMap<String, Integer>();
    private Map<String, String> maps = new HashMap<String, String>();

    public void set(String k, String v){
        maps.put(k, v);
    }

    public void set(String k, int v){
        mapi.put(k, new Integer(v));
    }

    public String get(String k){
        String v = null;
        v = maps.get(k);
        if (v == null){
            v = String.valueOf(mapi.get(k));
        }
        return v;
    }
}
