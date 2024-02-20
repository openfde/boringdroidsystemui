package com.boringdroid.systemui.data;

import androidx.annotation.NonNull;

import java.util.Map;

public class Compatible {
    private  int id ;
    private  boolean isSelect = false;
    private Map<String,Object> mp;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public boolean isSelect() {
        return isSelect;
    }

    public void setSelect(boolean select) {
        isSelect = select;
    }

    public Map<String, Object> getMp() {
        return mp;
    }

    public void setMp(Map<String, Object> mp) {
        this.mp = mp;
    }

    @Override
    public String toString() {
        return "Compatible{" +
                "id=" + id +
                ", isSelect=" + isSelect +
                ", mp=" + mp +
                '}';
    }
}
