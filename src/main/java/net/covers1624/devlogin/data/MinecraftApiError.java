package net.covers1624.devlogin.data;

/**
 * Created by covers1624 on 14/9/22.
 */
public class MinecraftApiError {

    public String path;
    public String errorType;
    public String error;
    public String errorMessage;
    public String developerMessage;

    @Override
    public String toString() {
        return "MinecraftApiError{" +
                "path='" + path + '\'' +
                ", errorType='" + errorType + '\'' +
                ", error='" + error + '\'' +
                ", errorMessage='" + errorMessage + '\'' +
                ", developerMessage='" + developerMessage + '\'' +
                '}';
    }
}
