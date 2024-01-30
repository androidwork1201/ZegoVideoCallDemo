package com.example.zegovideocalldemo.data;

import com.google.gson.annotations.SerializedName;

public class ResAiEffectData {
    @SerializedName("Code")
    private int code;
    @SerializedName("Message")
    private String message;
    @SerializedName("Data")
    private DataDTO data;

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public DataDTO getData() {
        return data;
    }

    public void setData(DataDTO data) {
        this.data = data;
    }

    public static class DataDTO {
        @SerializedName("License")
        private String license;

        public String getLicense() {
            return license;
        }

        public void setLicense(String license) {
            this.license = license;
        }
    }
}
