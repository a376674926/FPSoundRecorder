
package com.stj.soundrecorder;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.MenuItem;
import com.stj.soundrecorder.StorageHelper.Storage;

import java.util.ArrayList;


public class SoundRecorderPreferenceActivity extends PreferenceActivity implements
        OnPreferenceClickListener, OnSharedPreferenceChangeListener {
    private static final String RECORD_TYPE = "pref_key_record_type";

    private static final String ENABLE_HIGH_QUALITY = "pref_key_enable_high_quality";

    private static final String ENABLE_SOUND_EFFECT = "pref_key_enable_sound_effect";

    private static final String FILE_STORAGE_PATH = "pref_key_file_storage_path";

    public static final String KEY_FILE_STORAGE_PATH = "key_file_storage_path";

    private ListPreference mRecordTypePref;
    private Preference mStoragePathPref;
    private Context mContext;
    private static StorageHelper mStorageHelper;

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.preferences);
        mContext = getApplicationContext();
        mStorageHelper = StorageHelper.getInstance(mContext);
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        mRecordTypePref = (ListPreference) findPreference(RECORD_TYPE);
        mStoragePathPref = (Preference) findPreference(FILE_STORAGE_PATH);
        mStoragePathPref.setOnPreferenceClickListener(this);
        mRecordTypePref.getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        initPrefrence();
    }

    private void initPrefrence() {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mContext);
        mStorageHelper.initStoragePaths();
        ArrayList<Storage> storagesList = mStorageHelper.getMountedStorageList();
        if (storagesList.size() == 0) {
            mStoragePathPref.setSummary(getString(R.string.storage_invalid));
            mStoragePathPref.setEnabled(false);
        } else if (storagesList.size() == 1) {
            settings.edit().putString(KEY_FILE_STORAGE_PATH, storagesList.get(0).mountPoint)
                    .commit();
            mStoragePathPref.setEnabled(false);
        }

        String preferPath = settings.getString(KEY_FILE_STORAGE_PATH,
                mStorageHelper.getInternalSdcardPath());
        for (Storage storage : storagesList) {
            if (TextUtils.equals(storage.mountPoint, preferPath)) {
                mStoragePathPref.setSummary(storage.descriptionId);
            }
        }

        String recordType = getRecordType(mContext);
        if (recordType != null) {
            CharSequence type = recordType
                    .subSequence(recordType.indexOf("/") + 1, recordType.length());
            mRecordTypePref.setSummary(type);
        }
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (preference == mStoragePathPref) {
            AlertDialog.Builder builder = new Builder(SoundRecorderPreferenceActivity.this,
                    AlertDialog.THEME_HOLO_DARK);
            builder.setTitle(mContext.getString(R.string.pref_dialog_title_storage_path));
            final ArrayList<Storage> storagesList = mStorageHelper.getStorageList();
            CharSequence[] storages = new CharSequence[storagesList.size()];
            for (int i = 0; i < storagesList.size(); i++) {
                storages[i] = mContext.getString(storagesList.get(i).descriptionId);
            }
            builder.setItems(storages, new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    SharedPreferences settings = PreferenceManager
                            .getDefaultSharedPreferences(mContext);
                    settings.edit()
                            .putString(KEY_FILE_STORAGE_PATH, storagesList.get(which).mountPoint)
                            .commit();
                }
            });
            builder.create().show();
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    public static String getRecordType(Context context) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        return settings.getString(RECORD_TYPE, context.getString(R.string.prefDefault_recordType));
    }

    public static boolean isHighQuality(Context context) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        return settings.getBoolean(ENABLE_HIGH_QUALITY, true);
    }

    public static boolean isEnabledSoundEffect(Context context) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        return settings.getBoolean(ENABLE_SOUND_EFFECT, true);
    }

    public static String getPrefStoragePath(Context context) {
        StorageHelper storageHelper = StorageHelper.getInstance(context);
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        String prefStoragePath = settings.getString(KEY_FILE_STORAGE_PATH,
                storageHelper.getInternalSdcardPath());
        if (!StorageHelper.isVolumeMounted(prefStoragePath)) {
            ArrayList<Storage> storagesList = storageHelper.getMountedStorageList();
            if (storagesList.size() > 0) {
                String stroagePath = storagesList.get(0).mountPoint;
                settings.edit().putString(KEY_FILE_STORAGE_PATH, stroagePath).commit();
                return stroagePath;
            }
        }
        return settings.getString(KEY_FILE_STORAGE_PATH, storageHelper.getInternalSdcardPath());
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

        if (TextUtils.equals(key, KEY_FILE_STORAGE_PATH) || TextUtils.equals(key, RECORD_TYPE)) {
            initPrefrence();
        }
    }
}
