package club.finderella.finderella;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import club.finderella.finderella.Coach.Coach;

public class Router extends Activity {

    private SharedPreferences mPref;
    private SharedPreferences.Editor mEditor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_router);

        mPref = getSharedPreferences("finderella_preferences", MODE_PRIVATE);   // filename
        mEditor = mPref.edit();

        if (mPref.getInt("code", 0) != 0) {
            Log.i("mTag", "'code' is present in mPref:" + mPref.getInt("code", 0));
            switch (mPref.getInt("code", 0)) {
                case 1:
                    startActivity(new Intent(this, Welcome.class));
                    finish();
                    break;
                case 2:
                    startActivity(new Intent(this, Coach.class));
                    finish();
                    break;
                case 3:
                    startActivity(new Intent(this, MainActivity.class));
                    finish();
                    break;
                default:
                    mEditor.putInt("code", 1);
                    mEditor.commit();
                    startActivity(new Intent(this, Welcome.class));
                    finish();
                    break;
            }
        } else {
            Log.i("mTag", "No mPref, so creating new one");
            mEditor.putInt("code", 1);
            mEditor.commit();
            startActivity(new Intent(this, Welcome.class));
            finish();
        }

    }


}
