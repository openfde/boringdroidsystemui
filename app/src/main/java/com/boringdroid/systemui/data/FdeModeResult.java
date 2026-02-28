package com.boringdroid.systemui.data;

import java.io.Serializable;

public class FdeModeResult implements Serializable {
    public int Code;
    public String Message;
    public Data Data;

    public int getCode() {
        return Code;
    }

    public void setCode(int code) {
        Code = code;
    }

    public String getMessage() {
        return Message;
    }

    public void setMessage(String message) {
        Message = message;
    }

    public Data getData() {
        return Data;
    }

    public void setData(Data data) {
        Data = data;
    }

    @Override
    public String toString() {
        return "FdeModeResult{" +
                "Code=" + Code +
                ", Message='" + Message + '\'' +
                ", Data=" + Data +
                '}';
    }

    public static class Data implements Serializable{
        public String FDEMode;

        public String getFDEMode() {
            return FDEMode;
        }

        public void setFDEMode(String FDEMode) {
            this.FDEMode = FDEMode;
        }

        @Override
        public String toString() {
            return "Data{" +
                    "FDEMode='" + FDEMode + '\'' +
                    '}';
        }
    }
}
