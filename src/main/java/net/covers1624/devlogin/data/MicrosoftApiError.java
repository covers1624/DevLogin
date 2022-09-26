package net.covers1624.devlogin.data;

import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.UUID;

/**
 * Created by covers1624 on 12/9/22.
 */
public class MicrosoftApiError {

    public String error;
    @SerializedName ("error_description")
    public String errorDescription;
    @SerializedName ("error_codes")
    public List<Integer> errorCodes;
    public String timestamp;
    @SerializedName ("trace_id")
    public UUID traceId;
    @SerializedName ("correlation_id")
    public UUID correlation_id;
    @SerializedName ("error_uri")
    public String errorUri;
}
