package net.covers1624.devlogin;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.covers1624.devlogin.data.Account;
import net.covers1624.devlogin.data.AuthenticationResponse;
import net.covers1624.devlogin.http.HttpEngine;
import net.covers1624.devlogin.util.IOUtils;
import net.covers1624.devlogin.util.JsonUtils;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Created by covers1624 on 11/9/22.
 */
public class DevLogin {

    public static final Gson GSON = new Gson();
    private static final Type ACCOUNT_MAP_TYPE = new TypeToken<Map<String, Account>>() { }.getType();

    private static final Path DEFAULT_STORAGE_DIR = Paths.get(System.getProperty("user.home"), ".devlogin")
            .normalize()
            .toAbsolutePath();
    private static final Path STORAGE_DIR;

    static {
        String val = System.getProperty("devlogin.storage");
        STORAGE_DIR = val != null ? Paths.get(val) : DEFAULT_STORAGE_DIR;
    }

    public static void main(String[] args) throws Throwable {
        List<String> newArgs = new LinkedList<>();
        Map<String, String> ourArgs = consumeArgs(args, newArgs, "--launch_profile", "--launch_target");
        String profile = ourArgs.getOrDefault("--launch_profile", "default");
        String launchTarget = ourArgs.get("--launch_target");
        if (launchTarget == null) {
            System.err.println("Expected '--launch_target' option to be set.");
            System.exit(1);
        }
        Class<?> targetClass = Class.forName(launchTarget);
        Method mainMethod = targetClass.getMethod("main", String[].class);

        boolean badArgs = false;
        String[] disallowedArgs = { "--accessToken", "--username", "--uuid", "--userType" };
        for (String disallowedArg : disallowedArgs) {
            if (newArgs.contains(disallowedArg)) {
                System.err.println("[DevLogin] Argument '" + disallowedArg + "' must be removed.");
                badArgs = true;
            }
        }
        if (badArgs) {
            System.exit(1);
        }

        Map<String, Account> accountMap = loadAccounts();
        Account account = accountMap.get(profile);

        HttpEngine engine = HttpEngine.selectEngine();

        if (account == null) {
            // New account!
            System.out.println("[DevLogin] Adding new profile: " + profile);
            AuthenticationResponse msAuth = MicrosoftOAuth.deviceAuth(engine);
            account = MicrosoftOAuth.loginToAccount(engine, msAuth);
            accountMap.put(profile, account);
            saveAccounts(accountMap);
        } else {
            System.out.println("[DevLogin] Validating profile: " + profile);
            MicrosoftOAuth.validateAccount(engine, account);
        }

        engine.shutdown();

        newArgs.add("--accessToken");
        newArgs.add(account.mcTokens.accessToken);
        newArgs.add("--uuid");
        newArgs.add(account.uuid.toString().replace("-", ""));
        newArgs.add("--username");
        newArgs.add(account.username);
        newArgs.add("--userType");
        newArgs.add("msa");

        mainMethod.invoke(null, (Object) newArgs.toArray(new String[0]));
    }

    private static Map<String, String> consumeArgs(String[] inputArgs, List<String> args, String... consumedArgs) {
        Set<String> filter = new HashSet<>();
        Collections.addAll(filter, consumedArgs);

        Map<String, String> consumedArgMap = new HashMap<>();
        for (int i = 0; i < inputArgs.length; i++) {
            String arg = inputArgs[i];
            if (filter.contains(arg)) {
                if (i++ >= inputArgs.length) {
                    System.err.println("Expected argument for '" + arg + "'.");
                    System.exit(1);
                }
                consumedArgMap.put(arg, inputArgs[i]);
            } else {
                args.add(arg);
            }
        }
        return consumedArgMap;
    }

    private static Map<String, Account> loadAccounts() throws IOException {
        Path accountsFile = STORAGE_DIR.resolve("accounts.json");

        if (Files.exists(accountsFile)) {
            // TODO try-catch around this and gracefully fail (reset file)
            return JsonUtils.parse(GSON, accountsFile, ACCOUNT_MAP_TYPE);
        }
        return new HashMap<>();
    }

    private static void saveAccounts(Map<String, Account> map) throws IOException {
        Path accountsFile = STORAGE_DIR.resolve("accounts.json");
        JsonUtils.write(GSON, IOUtils.makeParents(accountsFile), map);
    }
}
