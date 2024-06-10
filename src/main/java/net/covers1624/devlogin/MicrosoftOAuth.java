package net.covers1624.devlogin;

import net.covers1624.devlogin.data.*;
import net.covers1624.devlogin.http.HttpEngine;
import net.covers1624.devlogin.http.HttpResponse;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

import static java.util.Collections.singletonList;
import static net.covers1624.devlogin.DevLogin.GSON;
import static net.covers1624.devlogin.util.ColUtils.mapOf;

/**
 * Created by covers1624 on 12/9/22.
 */
public class MicrosoftOAuth {

    /**
     * Azure application client id. Must have 'No keyboard (Device Code Flow)' enabled.
     */
    public static final String CLIENT_ID = "170105bd-9573-4222-b09c-6f24c3b77cd8";
    public static final String TENANT = "consumers";

    public static final String DEVICE_CODE_URL = "https://login.microsoftonline.com/" + TENANT + "/oauth2/v2.0/devicecode";
    public static final String TOKEN_URL = "https://login.microsoftonline.com/" + TENANT + "/oauth2/v2.0/token";
    public static final String XBL_AUTH_URL = "https://user.auth.xboxlive.com/user/authenticate";
    public static final String XSTS_AUTH_URL = "https://xsts.auth.xboxlive.com/xsts/authorize";
    public static final String MINECRAFT_XBOX_LOGIN_URL = "https://api.minecraftservices.com/authentication/login_with_xbox";
    public static final String MINECRAFT_PROFILE_URL = "https://api.minecraftservices.com/minecraft/profile";

    // region Microsoft Auth
    public static AuthenticationResponse deviceAuth(HttpEngine engine) throws IOException {
        DeviceAuthorizationResponse deviceAuth = startDeviceAuth(engine);
        System.out.println("[DevLogin]: " + deviceAuth.message);
        long expires = (System.currentTimeMillis() / 1000) + deviceAuth.expiresIn;
        while (System.currentTimeMillis() / 1000 < expires) {
            try {
                Thread.sleep(deviceAuth.interval * 1000L);
            } catch (InterruptedException ignored) {
            }
            AuthenticationResponse resp = checkDeviceAuth(engine, deviceAuth);
            if (resp != null) {
                System.out.println("[DevLogin] Device code validated!");
                return resp;
            }
        }
        System.err.println("Device code flow failed, code flow not completed within " + deviceAuth.expiresIn + " seconds.");
        System.exit(1);
        return null;
    }

    private static DeviceAuthorizationResponse startDeviceAuth(HttpEngine engine) throws IOException {
        return engine.postForm(
                DEVICE_CODE_URL,
                mapOf(
                        "client_id", CLIENT_ID,
                        "scope", "XboxLive.signin offline_access"
                )
        ).fromJson(GSON, DeviceAuthorizationResponse.class);
    }

    @Nullable
    private static AuthenticationResponse checkDeviceAuth(HttpEngine engine, DeviceAuthorizationResponse deviceAuth) throws IOException {
        HttpResponse response = engine.postForm(
                TOKEN_URL,
                mapOf(
                        "grant_type", "urn:ietf:params:oauth:grant-type:device_code",
                        "client_id", CLIENT_ID,
                        "device_code", deviceAuth.deviceCode
                )
        );

        if (response.code != 200) {
            // We got an error!
            MicrosoftApiError error = response.fromJson(GSON, MicrosoftApiError.class);
            if (error.error.equals("authorization_pending")) {
                return null;
            }
            throw new RuntimeException("Device flow failed: " + error.errorDescription);
        }

        return response.fromJson(GSON, AuthenticationResponse.class);
    }

    public static AuthenticationResponse refreshMicrosoftAuth(HttpEngine engine, Account.MSTokens tokens) throws IOException {
        HttpResponse resp = engine.postForm(
                TOKEN_URL,
                mapOf(
                        "grant_type", "refresh_token",
                        "client_id", CLIENT_ID,
                        "scope", "XboxLive.signin offline_access",
                        "refresh_token", tokens.refreshToken
                )
        );

        if (resp.code != 200) {
            MicrosoftApiError error = resp.fromJson(GSON, MicrosoftApiError.class);
            if (error.error.equals("invalid_grant")) {
                throw new GrantExpiredException(error);
            }
            throw new RuntimeException("Failed to refresh Microsoft Token: " + error.errorDescription);
        }

        return resp.fromJson(GSON, AuthenticationResponse.class);
    }
    // endregion

    // region Account Login
    public static Account loginToAccount(HttpEngine engine, AuthenticationResponse msAuth) throws IOException {
        System.out.println("[DevLogin] Logging into account..");

        XBLAuthenticationResponse xblAuth = authenticateWithXBL(engine, msAuth.accessToken);
        XSTSAuthenticationResponse xstsAuth = authenticateWithXSTS(engine, xblAuth);

        MinecraftAuthResponse mcAuth = authenticateWithMinecraft(engine, xblAuth, xstsAuth);

        MinecraftProfile profile = getMinecraftProfile(engine, mcAuth);

        return new Account(profile, msAuth, mcAuth);
    }
    // endregion

    //region Account validate
    public static void validateAccount(HttpEngine engine, Account account) throws IOException {
        System.out.println("[DevLogin] Validating account..");
        if (System.currentTimeMillis() >= account.mcTokens.expiresAt) {
            System.out.println("[DevLogin] Minecraft token expired.");
            refreshMinecraftToken(engine, account);
        }

        System.out.println("[DevLogin] Account validated.");
    }

    private static void refreshMinecraftToken(HttpEngine engine, Account account) throws IOException {
        System.out.println("[DevLogin] Refreshing Minecraft Token..");
        if (System.currentTimeMillis() >= account.msTokens.expiresAt) {
            System.out.println("[DevLogin] Microsoft Token expired.");
            refreshMicrosoftToken(engine, account);
        }

        XBLAuthenticationResponse xblAuth = authenticateWithXBL(engine, account.msTokens.accessToken);
        XSTSAuthenticationResponse xstsAuth = authenticateWithXSTS(engine, xblAuth);

        MinecraftAuthResponse mcAuth = authenticateWithMinecraft(engine, xblAuth, xstsAuth);
        MinecraftProfile profile = getMinecraftProfile(engine, mcAuth);

        account.username = profile.name;
        account.uuid = profile.uuid();
        account.mcTokens = new Account.MCTokens(mcAuth);

        System.out.println("[DevLogin] Minecraft Token refreshed.");
    }

    private static void refreshMicrosoftToken(HttpEngine engine, Account account) throws IOException {
        System.out.println("[DevLogin] Refreshing Microsoft Token..");
        account.msTokens = new Account.MSTokens(refreshMicrosoftAuth(engine, account.msTokens));
        System.out.println("[DevLogin] Microsoft Token refreshed.");
    }
    //endregion

    // region Xbox Live
    private static XBLAuthenticationResponse authenticateWithXBL(HttpEngine engine, String authToken) throws IOException {
        HttpResponse resp = engine.postJson(
                XBL_AUTH_URL,
                GSON,
                mapOf("Properties", mapOf(
                                "AuthMethod", "RPS",
                                "SiteName", "user.auth.xboxlive.com",
                                "RpsTicket", "d=" + authToken),
                        "RelyingParty", "http://auth.xboxlive.com",
                        "TokenType", "JWT"
                )
        );

        if (resp.code != 200 || resp.body == null) {
            if (resp.body != null) {
                throw new RuntimeException("XBoxLive Authentication failed. Status code: " + resp.code + " Body: " + resp.fromJson(GSON, XboxApiError.class));
            }
            throw new RuntimeException("XBoxLive Authentication failed. Got status code: " + resp.code);
        }

        System.out.println("[DevLogin] Logged into XBoxLive!");
        return resp.fromJson(GSON, XBLAuthenticationResponse.class);
    }

    private static XSTSAuthenticationResponse authenticateWithXSTS(HttpEngine engine, XBLAuthenticationResponse auth) throws IOException {
        HttpResponse resp = engine.postJson(
                XSTS_AUTH_URL,
                GSON,
                mapOf("Properties", mapOf(
                                "SandboxId", "RETAIL",
                                "UserTokens", singletonList(auth.token)),
                        "RelyingParty", "rp://api.minecraftservices.com/",
                        "TokenType", "JWT"
                )
        );

        if (resp.code != 200) {
            XboxApiError error = resp.fromJson(GSON, XboxApiError.class);
            if (error.xErr == 2148916233L) {
                System.out.println("[DevLogin] You don't have an XBoxLive account. Please login to Minecraft at least once in the Vanilla launcher.");
            } else if (error.xErr == 2148916235L) {
                System.out.println("[DevLogin] XBoxLive is not available or banned in your country.");
            } else if (error.xErr == 2148916236L || error.xErr == 2148916237L) {
                System.out.println("[DevLogin] Your account needs adult verification. (South Korea)");
            } else if (error.xErr == 2148916238L) {
                System.out.println("[DevLogin] Your account is a child account and must be added to a Family by an adult.");
            } else if (error.message != null) {
                System.out.println("[DevLogin] Unknown XSTS Login error: " + error.message);
            } else {
                System.out.println("[DevLogin] Unknown XSTS Login error: " + error.xErr);
            }
            System.exit(1);
        }
        System.out.println("[DevLogin] Logged into XSTS!");
        return resp.fromJson(GSON, XSTSAuthenticationResponse.class);
    }
    // endregion

    // region Minecraft
    private static MinecraftAuthResponse authenticateWithMinecraft(HttpEngine engine, XBLAuthenticationResponse xblAuth, XSTSAuthenticationResponse xstsAuth) throws IOException {
        HttpResponse resp = engine.postJson(
                MINECRAFT_XBOX_LOGIN_URL,
                GSON,
                mapOf(
                        "identityToken", "XBL3.0 x=" + xblAuth.getUserId() + ";" + xstsAuth.token,
                        "ensureLegacyEnabled", "true"
                )
        );

        if (resp.code != 200) {
            MinecraftApiError error = resp.fromJson(GSON, MinecraftApiError.class);
            throw new RuntimeException(error.developerMessage != null ? error.developerMessage : "Minecraft api error: " + error);
        }

        System.out.println("[DevLogin] Logged into Minecraft!");
        return resp.fromJson(GSON, MinecraftAuthResponse.class);
    }

    private static MinecraftProfile getMinecraftProfile(HttpEngine engine, MinecraftAuthResponse auth) throws IOException {
        HttpResponse resp = engine.getJson(
                MINECRAFT_PROFILE_URL,
                mapOf("Authorization", "Bearer " + auth.accessToken)
        );

        if (resp.code != 200) {
            MinecraftApiError error = resp.fromJson(GSON, MinecraftApiError.class);
            throw new RuntimeException(error.developerMessage != null ? error.developerMessage : "Minecraft api error: " + error);
        }

        System.out.println("[DevLogin] Got Minecraft profile!");
        return resp.fromJson(GSON, MinecraftProfile.class);
    }
    // endregion

    public static class GrantExpiredException extends RuntimeException {

        public final MicrosoftApiError error;

        public GrantExpiredException(MicrosoftApiError error) {
            super(error.errorDescription);
            this.error = error;
        }
    }
}
