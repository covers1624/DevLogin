package net.covers1624.devlogin.data;

import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.UUID;

/**
 * Created by covers1624 on 14/9/22.
 */
public class MinecraftAuthResponse {

    public UUID username;
    public List<?> roles;
    @SerializedName ("access_token")
    public String accessToken;
    @SerializedName ("token_type")
    public String tokenType;
    @SerializedName ("expires_in")
    public int expiresIn;
}
