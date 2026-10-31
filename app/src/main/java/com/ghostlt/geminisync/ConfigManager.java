package com.ghostlt.geminisync;

import android.content.Context;
import android.content.SharedPreferences;

public class ConfigManager {
    private static final String PREF_NAME = "gemini_sync_prefs";
    private static final String KEY_API_KEY = "gemini_api_key";
    private static final String KEY_GH_TOKEN = "github_token";
    private static final String KEY_GH_REPO = "github_repo";
    private static final String KEY_AUTO_SYNC = "auto_sync";

    private final SharedPreferences prefs;

    public ConfigManager(Context context) {
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public String getApiKey() {
        String key = prefs.getString(KEY_API_KEY, "");
        if (key.isEmpty()) {
            return BuildConfig.DEFAULT_GEMINI_API_KEY;
        }
        return key;
    }

    public void setApiKey(String key) {
        prefs.edit().putString(KEY_API_KEY, key).apply();
    }

    public String getGithubToken() {
        return prefs.getString(KEY_GH_TOKEN, "");
    }

    public void setGithubToken(String token) {
        prefs.edit().putString(KEY_GH_TOKEN, token).apply();
    }

    public String getGithubRepo() {
        return prefs.getString(KEY_GH_REPO, "GhostLT/cool-newton");
    }

    public void setGithubRepo(String repo) {
        prefs.edit().putString(KEY_GH_REPO, repo).apply();
    }

    public boolean isAutoSyncEnabled() {
        return prefs.getBoolean(KEY_AUTO_SYNC, true);
    }

    public void setAutoSyncEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_AUTO_SYNC, enabled).apply();
    }
}
