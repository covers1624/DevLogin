package net.covers1624.devlogin.data;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;

/**
 * Created by covers1624 on 12/9/22.
 */
public class XBLAuthenticationResponse {

    @SerializedName ("IssueInstant")
    public String issueInstant;
    @SerializedName ("NotAfter")
    public String notAfter;
    @SerializedName ("Token")
    public String token;
    @SerializedName ("DisplayClaims")
    public JsonObject displayClaims;

    public String getUserId() {
        // Ew, thanks Microsoft..
        return displayClaims.get("xui").getAsJsonArray().get(0).getAsJsonObject().get("uhs").getAsString();
    }
}
