package com.nmmart.retailos.models;

import com.google.gson.annotations.SerializedName;

public class AppError {
    @SerializedName("file_name")
    public String fileName;

    @SerializedName("function_name")
    public String functionName;

    @SerializedName("error_message")
    public String errorMessage;

    @SerializedName("timestamp")
    public String timestamp;

    @SerializedName("device_info")
    public String deviceInfo;

    public AppError() {
    }

    public AppError(String fileName, String functionName, String errorMessage, String timestamp, String deviceInfo) {
        this.fileName = fileName;
        this.functionName = functionName;
        this.errorMessage = errorMessage;
        this.timestamp = timestamp;
        this.deviceInfo = deviceInfo;
    }
}
