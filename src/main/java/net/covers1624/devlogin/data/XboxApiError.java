package net.covers1624.devlogin.data;

import com.google.gson.annotations.SerializedName;

/**
 * Created by covers1624 on 14/9/22.
 */
public class XboxApiError {

    @SerializedName ("Identity")
    public int identity;
    @SerializedName ("XErr")
    public long xErr;
    @SerializedName ("Message")
    public String message;
    @SerializedName ("Redirect")
    public String redirect;

    @Override
    public String toString() {
        return "XboxApiError{" +
                "identity=" + identity +
                ", xErr=" + xErr +
                ", message='" + message + '\'' +
                ", redirect='" + redirect + '\'' +
                '}';
    }
}
