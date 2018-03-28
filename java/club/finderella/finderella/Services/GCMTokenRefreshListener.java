package club.finderella.finderella.Services;


import android.content.Intent;
import android.content.SharedPreferences;

import com.google.android.gms.iid.InstanceIDListenerService;


public class GCMTokenRefreshListener extends InstanceIDListenerService {
    private Intent mIntent;
    SharedPreferences mPref;
    SharedPreferences.Editor mEditor;

    @Override
    public void onTokenRefresh() {
        // GCM REGISTRATIONS
        // update shared pref
        mPref = getSharedPreferences("finderella_preferences", MODE_PRIVATE);   // filename
        mEditor = mPref.edit();

        mEditor.putInt("gcm_sync_required", 1);  // 1 means required
        mEditor.commit();

        mIntent = new Intent(this, GCMRegistrar.class);
        startService(mIntent);
    }


}
