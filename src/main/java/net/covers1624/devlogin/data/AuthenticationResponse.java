package net.covers1624.devlogin.data;

import com.google.gson.annotations.SerializedName;

/**
 * Created by covers1624 on 12/9/22.
 */
public class AuthenticationResponse {

    @SerializedName ("token_type")
    public String tokenType;
    public String scope;
    @SerializedName ("expires_in")
    public int expiresIn;
    @SerializedName ("access_token")
    public String accessToken;
    @SerializedName ("id_token")
    public String idToken;
    @SerializedName ("refresh_token")
    public String refreshToken;
}
