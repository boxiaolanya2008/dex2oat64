package com.vivo.applyindepthoptimization.ui.settings;

import androidx.annotation.Nullable;
import androidx.preference.PreferenceDataStore;

import java.util.Set;

public class EmptyPreferenceDataStore extends PreferenceDataStore {

    public void putString(String key, @Nullable String value) {
    }

    public void putStringSet(String key, @Nullable Set<String> values) {
    }

    public void putInt(String key, int value) {
    }

    public void putLong(String key, long value) {
    }

    public void putFloat(String key, float value) {
    }

    public void putBoolean(String key, boolean value) {
    }

    @Nullable
    public String getString(String key, @Nullable String defValue) {
        return defValue;
    }

    @Nullable
    public Set<String> getStringSet(String key, @Nullable Set<String> defValues) {
        return defValues;
    }

    public int getInt(String key, int defValue) {
        return defValue;
    }

    public long getLong(String key, long defValue) {
        return defValue;
    }

    public float getFloat(String key, float defValue) {
        return defValue;
    }

    public boolean getBoolean(String key, boolean defValue) {
        return defValue;
    }
}
