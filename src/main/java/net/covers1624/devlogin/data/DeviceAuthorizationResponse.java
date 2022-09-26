package net.covers1624.devlogin.data;

import com.google.gson.annotations.SerializedName;

/**
 * Created by covers1624 on 12/9/22.
 */
public class DeviceAuthorizationResponse {

    @SerializedName ("user_code")
    public String userCode;
    @SerializedName ("device_code")
    public String deviceCode;
    @SerializedName ("verification_url")
    public String verificationUrl;
    @SerializedName ("expires_in")
    public int expiresIn;
    public int interval;
    public String message;
}
