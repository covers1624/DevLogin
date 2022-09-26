package net.covers1624.devlogin.data;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Created by covers1624 on 11/9/22.
 */
public class Account {

    public String username;
    public UUID uuid;

    public MSTokens msTokens;
    public MCTokens mcTokens;

    public Account(){
    }

    public Account(MinecraftProfile mcProfile, AuthenticationResponse msAuth, MinecraftAuthResponse mcAuth) {
        username = mcProfile.name;
        uuid = mcProfile.uuid();
        msTokens = new MSTokens(msAuth);
        mcTokens = new MCTokens(mcAuth);
    }

    public static class MSTokens {

        public String accessToken;
        public String refreshToken;
        public long expiresAt;

        public MSTokens() {
        }

        public MSTokens(AuthenticationResponse auth) {
            accessToken = auth.accessToken;
            refreshToken = auth.refreshToken;
            expiresAt = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(auth.expiresIn);
        }
    }

    public static class MCTokens {

        public String accessToken;
        public long expiresAt;

        public MCTokens() {
        }

        public MCTokens(MinecraftAuthResponse auth) {
            accessToken = auth.accessToken;
            expiresAt = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(auth.expiresIn);
        }
    }
}
