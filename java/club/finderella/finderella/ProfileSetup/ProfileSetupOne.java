package club.finderella.finderella.ProfileSetup;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.telephony.SmsManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.RequestQueue;
import com.cocosw.bottomsheet.BottomSheet;
import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.Profile;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;

import com.twitter.sdk.android.Twitter;
import com.twitter.sdk.android.core.Callback;
import com.twitter.sdk.android.core.Result;
import com.twitter.sdk.android.core.TwitterAuthConfig;
import com.twitter.sdk.android.core.TwitterException;
import com.twitter.sdk.android.core.TwitterSession;
import com.twitter.sdk.android.core.identity.TwitterLoginButton;

import org.apmem.tools.layouts.FlowLayout;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;


import club.finderella.finderella.Helpers.Telephony;
import club.finderella.finderella.POJO.MyUrlData;
import club.finderella.finderella.POJO.SocialButton;
import club.finderella.finderella.Helpers.MyDBHandler;
import club.finderella.finderella.Helpers.MySingleton;
import club.finderella.finderella.InstagramOauth.InstagramApp;
import club.finderella.finderella.InstagramOauth.InstagramData;
import club.finderella.finderella.MainActivity;
import club.finderella.finderella.R;
import club.finderella.finderella.Services.MediaSync;
import club.finderella.finderella.Services.TextSync;
import club.finderella.finderella.Utilities.Browser;
import club.finderella.finderella.Utilities.ImageViewer;
import cz.msebera.android.httpclient.HttpEntity;
import cz.msebera.android.httpclient.HttpResponse;
import cz.msebera.android.httpclient.client.HttpClient;
import cz.msebera.android.httpclient.client.methods.HttpPost;
import cz.msebera.android.httpclient.entity.mime.HttpMultipartMode;
import cz.msebera.android.httpclient.entity.mime.MultipartEntityBuilder;
import cz.msebera.android.httpclient.impl.client.DefaultHttpClient;
import cz.msebera.android.httpclient.util.EntityUtils;
import de.hdodenhof.circleimageview.CircleImageView;
import io.fabric.sdk.android.Fabric;


public class ProfileSetupOne extends AppCompatActivity {

    private EditText firstName, lastName;
    private EditText status;
    private Button doneButton;
    private TextView editCommand;
    private ImageView blueButton;

    private FlowLayout mFlowLayout;

    private static CircleImageView mProfileImg;
    private static boolean mProfileImgSet = false;

    private final View.OnClickListener profilePicIconClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (mProfileImgSet) {
                // LAUNCH IN FULL MODE
                Intent mIntent = new Intent(ProfileSetupOne.this, ImageViewer.class);
                mIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                mIntent.putExtra("code", 1);    // code for profile pic
                startActivity(mIntent);
            } else {
                // nothing
            }
        }
    };

    private static File f_expimg = null;

    private static View add_exp;
    private static View exp_view_view;

    private MyDBHandler db;
    private SQLiteDatabase dbObj;
    private static SocialButton mArray[] = new SocialButton[]{null, null, null, null};
    private static View mViewArray[] = new View[]{null, null, null, null};

    private static RequestQueue queue;
    private static int user_id;
    private static String password;

    private HttpClient mClient;
    private HttpPost mPost;
    private MultipartEntityBuilder mBuilder;
    private HttpEntity mEntity;

    private LoginManager mLoginManagerInstance;
    private CallbackManager fb_CallbackManager;
    private static Profile mProfile = null;

    private static int mResultCode;

    private TwitterLoginButton tw_button;

    private static InstagramApp mInstagramApp;
    InstagramApp.OAuthAuthenticationListener listener;


    private static final String TWITTER_KEY = "M8IG2YWR9PyWBAFii50ZOr7VD";
    private static final String TWITTER_SECRET = "trD499NJFFzCewOngu9NWO2Qxsl02eLrm2Gkqi0agXGraFNLoD";

    private static final int PICK_PHOTO_FROM_GALLERY = 21;
    private static final int PICK_PHOTO_FROM_CAMERA = 22;


    private SharedPreferences mPref;
    private SharedPreferences.Editor mEditor;


    private View telView;
    private static Telephony mTelHelper = new Telephony();


    private final View.OnLongClickListener mLongClickListener = new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(final View v) {
            if (mArray[(v.getId() - 10)].url != null) {
                new BottomSheet.Builder(ProfileSetupOne.this, R.style.MyBottomSheetTheme).title("Delete?").sheet(R.menu.yesorno).listener(new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case R.id.yes_delete:
                                if (mArray[(v.getId() - 10)].url != null) { // id's - 10,11,12,13,14
                                    mArray[(v.getId() - 10)].url = null;
                                    mArray[(v.getId() - 10)].type = 1;
                                    new SocialSync().execute(new SocialButton(null, (v.getId() - 9)));
                                }
                                break;
                            case R.id.no_delete:
                                dialog.dismiss();
                                break;
                        }
                    }
                }).show();
                return true;
            } else
                return false;
        }
    };
    private final View.OnClickListener mGeneralSocialClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case 10:
                    if (mArray[0].url == null) {
                        mLoginManagerInstance.logInWithReadPermissions(ProfileSetupOne.this, Arrays.asList("public_profile")); // open facebook
                        mResultCode = 1;
                    } else {
                        Intent mIntent = new Intent(ProfileSetupOne.this, Browser.class);
                        mIntent.putExtra("url", mArray[0].url);
                        mIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                        startActivity(mIntent);
                    }
                    break;
                case 11:
                    if (mArray[1].url == null) {
                        tw_button.performClick(); // open twitter
                        mResultCode = 2;
                    } else {
                        Intent mIntent = new Intent(ProfileSetupOne.this, Browser.class);
                        mIntent.putExtra("url", mArray[1].url);
                        mIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                        startActivity(mIntent);
                    }
                    break;
                case 12:
                    if (mArray[2].url == null) {
                        if (mInstagramApp.hasAccessToken())
                            mInstagramApp.resetAccessToken();

                        mInstagramApp.authorize();
                        mResultCode = 3;
                    } else {
                        Intent mIntent = new Intent(ProfileSetupOne.this, Browser.class);
                        mIntent.putExtra("url", mArray[2].url);
                        mIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                        startActivity(mIntent);
                    }
                    break;
                case 13:
                    if (mArray[3].url == null) {
                        telView.setVisibility(View.VISIBLE);
                        mResultCode = 4;
                    } else {

                        new BottomSheet.Builder(ProfileSetupOne.this, R.style.MyBottomSheetTheme).title("Call " + mArray[3].url + "?").sheet(R.menu.yesorno).listener(
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        if (which == R.id.yes_delete) {
                                            dialog.dismiss();
                                            Intent mInt = new Intent(Intent.ACTION_DIAL);
                                            mInt.setData(Uri.parse("tel:" + mArray[3].url));
                                            ProfileSetupOne.this.startActivity(mInt);
                                        } else {
                                            dialog.dismiss();

                                        }
                                    }
                                }).show();
                    }
                    break;
            }


        }
    };          // for social button clicks

    private final View.OnClickListener telephonyMain = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (!mTelHelper.is_verifying) {
                // check input
                if (((EditText) telView.findViewById(R.id.mEditText)).getText().length() != 0) {
                    // send sms
                    mTelHelper.mPhoneNumber = ((EditText) telView.findViewById(R.id.mEditText)).getText().toString();
                    mTelHelper.code = mTelHelper.getRandomCode();

                    try {
                        SmsManager smsManager = SmsManager.getDefault();
                        smsManager.sendTextMessage(mTelHelper.mPhoneNumber, null, "Finderella phone verification Code:" + mTelHelper.code, null, null);
                        mTelHelper.is_verifying = true;

                        // change text on button and edit text
                        ((Button) telView.findViewById(R.id.mButton)).setText("Verify");
                        ((EditText) telView.findViewById(R.id.mEditText)).setText("");
                        ((EditText) telView.findViewById(R.id.mEditText)).setHint("Enter Verification Code");

                        if (telView != null) {
                            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                            imm.hideSoftInputFromWindow(telView.getWindowToken(), 0);
                        }

                    } catch (Exception e) {
                        Log.i("mTag", "exception to get number --" + e);
                        Toast.makeText(ProfileSetupOne.this, "No SIM Card", Toast.LENGTH_LONG).show();
                        mTelHelper.code = null;
                        mTelHelper.is_verifying = false;
                    }
                }


            } else {
                // hide keyboard
                // hiding keyboard
                if (telView != null) {
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(telView.getWindowToken(), 0);
                }

                // check verification code
                if (((EditText) telView.findViewById(R.id.mEditText)).getText().toString().equals(mTelHelper.code)) {
                    // success
                    mTelHelper.code = null;
                    mTelHelper.is_verifying = false;
                    telView.setVisibility(View.GONE);
                    resetTelView(telView);
                    mArray[3].url = mTelHelper.mPhoneNumber;
                    new SocialSync().execute(new SocialButton(mArray[3].url, 4));
                } else {
                    // error
                    Toast.makeText(ProfileSetupOne.this, "Wrong Code Entered!", Toast.LENGTH_LONG).show();
                }

            }

        }
    };
    private final View.OnClickListener telephonyCancel = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            mTelHelper.is_verifying = false;
            mTelHelper.code = null;
            telView.setVisibility(View.GONE);
            resetTelView(telView);

            View view = findViewById(R.id.relLayout);
            if (view != null) {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
            view.requestFocus();


        }
    };

    private static boolean loadedFromGal = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FacebookSdk.sdkInitialize(getApplicationContext());
        final TwitterAuthConfig authConfig = new TwitterAuthConfig(TWITTER_KEY, TWITTER_SECRET);
        Fabric.with(this, new Twitter(authConfig));

        setContentView(R.layout.layout_profilesetup_one);

        db = new MyDBHandler(this, null, null, 1);
        dbObj = db.getWritableDatabase();
        mPref = getSharedPreferences("finderella_preferences", MODE_PRIVATE);
        mEditor = mPref.edit();

        registerReceiver(mFbReceiver, new IntentFilter("fbDownload"));

        mFlowLayout = (FlowLayout) findViewById(R.id.mFlowLayout);
        editCommand = (TextView) findViewById(R.id.editCommand);
        queue = MySingleton.getInstance(this).getRequestQueue();

        tw_button = (TwitterLoginButton) findViewById(R.id.tw_button);
        initSdkComponents();

        firstName = (EditText) findViewById(R.id.firstName);
        lastName = (EditText) findViewById(R.id.lastName);

        status = (EditText) findViewById(R.id.status);
        status.setOnFocusChangeListener(new View.OnFocusChangeListener() {

            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    // move cursor to end
                    // show keyboard
                    status.append("");
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.showSoftInput(status, InputMethodManager.SHOW_IMPLICIT);

                    editCommand.setText("Done");
                } else {
                    editCommand.setText("Edit");
                }
            }
        });


        mProfileImg = (CircleImageView) findViewById(R.id.mProfileImg);
        mProfileImg.setOnClickListener(profilePicIconClick);


        add_exp = findViewById(R.id.add_exp); // view containing blue button
        exp_view_view = findViewById(R.id.exp_view_view); // view showing the expression image


        doneButton = (Button) findViewById(R.id.doneButton);
        doneButton.setEnabled(false);


        firstName.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable arg0) {
                if (firstName.getText().length() > 0) {
                    doneButton.setEnabled(true);
                } else {
                    doneButton.setEnabled(false);
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });


        // adding exp_photo Exp picture ADD only
        blueButton = (ImageView) (findViewById(R.id.add_exp)).findViewById(R.id.blueButton);
        blueButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // selection only for images
                        mResultCode = 7;
                        new BottomSheet.Builder(ProfileSetupOne.this, R.style.MyBottomSheetTheme).title("Import from").sheet(R.menu.gc_picker).listener(new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                switch (which) {
                                    case R.id.gal_bottomsheet:
                                        dialog.dismiss();
                                        pickImageGallery();
                                        break;

                                }
                            }
                        }).show();
                    }
                }
        );


        // Exp editing (Replacing or Deleting both photos and videos)
        ((findViewById(R.id.exp_view_view)).findViewById(R.id.exp_edit)).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        new BottomSheet.Builder(ProfileSetupOne.this, R.style.MyBottomSheetTheme).title("Import from").sheet(R.menu.addordelete).listener(new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                switch (which) {
                                    case R.id.add_bottomsheet:
                                        mResultCode = 7;
                                        dialog.dismiss();
                                        new BottomSheet.Builder(ProfileSetupOne.this, R.style.MyBottomSheetTheme).title("Import from").sheet(R.menu.gc_picker).listener(new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                switch (which) {
                                                    case R.id.gal_bottomsheet:
                                                        dialog.dismiss();
                                                        pickImageGallery();
                                                        break;

                                                }
                                            }
                                        }).show();
                                        break;

                                    case R.id.delete_bottomsheet:
                                        new BottomSheet.Builder(ProfileSetupOne.this, R.style.MyBottomSheetTheme).title("Delete picture?").sheet(R.menu.yesorno).listener(
                                                new DialogInterface.OnClickListener() {
                                                    @Override
                                                    public void onClick(DialogInterface dialog2, int which) {
                                                        switch (which) {
                                                            case R.id.yes_delete:
                                                                new DeleteExpressionsThread().execute();
                                                                dialog2.dismiss();
                                                                break;
                                                            case R.id.no_delete:
                                                                dialog2.dismiss();
                                                                break;
                                                        }
                                                    }
                                                }

                                        ).show();
                                        dialog.dismiss();
                                        break;
                                }
                            }
                        }).show();
                    } // end onClick main dialog
                }
        );

        (findViewById(R.id.exp_view_view).findViewById(R.id.exp_view_image)).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (f_expimg != null) {
                            // Launch full screen image view for exp

                            Intent mIntent = new Intent(ProfileSetupOne.this, ImageViewer.class);
                            mIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                            mIntent.putExtra("code", 2);    // for exp image
                            startActivity(mIntent);

                        }
                    }
                }
        );


        new LoadTextAndCredentials().execute();
        new LoadProfilePicture().execute();
        new LoadExpressions().execute();
        new LoadFlowLayout().execute();


    }// end of onCreate


    public void importProfilePic(View v) { // click listener for Import text
        if (mProfileImgSet) {
            // Image is set
            // add or delete builder
            new BottomSheet.Builder(this, R.style.MyBottomSheetTheme).title("Import from").sheet(R.menu.addordelete).listener(new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    switch (which) {
                        case R.id.add_bottomsheet:
                            mResultCode = 6;
                            dialog.dismiss();
                            new BottomSheet.Builder(ProfileSetupOne.this, R.style.MyBottomSheetTheme).title("Import from").sheet(R.menu.gc_picker).listener(new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    switch (which) {
                                        case R.id.gal_bottomsheet:
                                            dialog.dismiss();
                                            pickImageGallery();
                                            break;

                                    }
                                }
                            }).show();
                            break;

                        case R.id.delete_bottomsheet:
                            // YES AND NO
                            dialog.dismiss();
                            new BottomSheet.Builder(ProfileSetupOne.this, R.style.MyBottomSheetTheme).title("Delete picture?").sheet(R.menu.yesorno).listener(
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog2, int which) {
                                            switch (which) {
                                                case R.id.yes_delete:

                                                    new Thread(new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            new DeleteProfilePictureThread().execute();
                                                        }
                                                    }).start();

                                                    dialog2.dismiss();
                                                    break;
                                                case R.id.no_delete:
                                                    dialog2.dismiss();
                                                    break;
                                            }
                                        }
                                    }

                            ).show();
                            break;
                    }
                }
            }).show();
        } else {
            // image not set
            mResultCode = 6;
            new BottomSheet.Builder(ProfileSetupOne.this, R.style.MyBottomSheetTheme).title("Import from").sheet(R.menu.gc_picker).listener(new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    switch (which) {
                        case R.id.gal_bottomsheet:
                            dialog.dismiss();
                            pickImageGallery();
                            break;

                    }
                }
            }).show();
        }
    }


    public void editStatus(View v) {
        if (status.hasFocus()) {

            editCommand.setText("Edit");
            View view = findViewById(R.id.relLayout);

            // hiding keyboard
            if (view != null) {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }

            status.clearFocus();


        } else {
            status.requestFocus();
            editCommand.setText("Done");
        }

    }

    public void done(View v) {
        // pass in first name last name status
        String fname, lname, statText;

        fname = firstName.getText().toString();
        if (fname.charAt(0) >= 97) {
            new StringBuilder(fname).setCharAt(0, (char) (fname.charAt(0) - 32));
        }
        lname = lastName.getText().toString();
        if (fname.charAt(0) >= 97) {
            new StringBuilder(lname).setCharAt(0, (char) (lname.charAt(0) - 32));
        }

        if (status.getText().toString().length() == 0) {
            statText = "Hey there! Nice to meet you";
        } else {
            statText = status.getText().toString();
        }

        new ServerOperations().execute(fname, lname, statText);
    }


    private class ServerOperations extends AsyncTask<String, Void, Integer> {
        @Override
        protected Integer doInBackground(String... params) {

            ContentValues cv = new ContentValues();
            cv.put("first_name", params[0]);
            cv.put("last_name", params[1]);
            cv.put("status", params[2]);

            dbObj.update("meta_data", cv, null, null);      // passing null as where clause updates all rows

            String query = "UPDATE server_sync SET text_sync =1,exp_sync=1,profile_pic_sync=1 WHERE 1=1";
            dbObj.execSQL(query);

            startService(new Intent(ProfileSetupOne.this, TextSync.class));
            startService(new Intent(ProfileSetupOne.this, MediaSync.class));

            return null;
        }

        @Override
        protected void onPostExecute(Integer integer) {
            mEditor.putInt("code", 3);
            mEditor.commit();
            startActivity(new Intent(ProfileSetupOne.this, MainActivity.class));
            finish();
        }
    }


    private class LoadExpressions extends AsyncTask<Void, Void, Void> {
        private Bitmap b;
        private int exp_type;

        @Override
        protected Void doInBackground(Void... params) {
            String query = "SELECT * FROM user_media WHERE 1=1";
            Cursor c = dbObj.rawQuery(query, null);

            while (c.moveToNext()) {
                exp_type = c.getInt(c.getColumnIndex("exp_type"));

                if (exp_type == 1) {
                    try {
                        if (c.getString(c.getColumnIndex("exp_image")) == null) {
                            b = null;
                        } else {
                            File f = new File(c.getString(c.getColumnIndex("exp_image")), "exp_image.jpg");
                            b = BitmapFactory.decodeStream(new FileInputStream(f));
                            f_expimg = f;
                        }
                    } catch (FileNotFoundException e) {
                        Log.i("mTag", "No exp picture at given path:" + c.getString(c.getColumnIndex("exp_image")));
                    }
                } else {
                    b = null;
                }

                break;
            }

            c.close();
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            switch (exp_type) {
                case 0:
                    Log.i("mTag", "exp_type = 0");
                    add_exp.setVisibility(View.VISIBLE);
                    exp_view_view.setVisibility(View.GONE);
                    break;
                case 1:
                    Log.i("mTag", "exp_type = 1");
                    add_exp.setVisibility(View.GONE);
                    exp_view_view.setVisibility(View.VISIBLE);
                    break;
                case 2:
                    Log.i("mTag", "exp_type = 2");
                    add_exp.setVisibility(View.GONE);
                    exp_view_view.setVisibility(View.VISIBLE);
                    break;

            }

            if (b != null) {
                add_exp.setVisibility(View.GONE);
                exp_view_view.setVisibility(View.VISIBLE);
                ((ImageView) ((findViewById(R.id.exp_view_view)).findViewById(R.id.exp_view_image))).setImageBitmap(b);
            }
        }
    }


    private class LoadProfilePicture extends AsyncTask<Void, Void, Void> {
        private Bitmap b;

        @Override
        protected Void doInBackground(Void... params) {

            String query = "SELECT profile_image FROM user_media WHERE 1=1";
            Cursor c = dbObj.rawQuery(query, null);
            try {
                while (c.moveToNext()) {
                    if (c.getString(c.getColumnIndex("profile_image")) == null) {
                        Log.i("mTag", "profile pic stored in databse is null");
                        b = null;
                        // check for temp_image
                        ContextWrapper cw = new ContextWrapper(getApplicationContext());
                        // path to /data/data/yourapp/app_data/imageDir
                        File directory = cw.getDir("imageDir", Context.MODE_PRIVATE);
                        File mypath = new File(directory, "fb_temp.jpg");
                        b = BitmapFactory.decodeStream(new FileInputStream(mypath));
                        // save it as profle picture
                        saveAsProfileImage(b);
                    } else {
                        Log.i("mTag", "profile pic is present in databae, fetching");
                        File f = new File(c.getString(c.getColumnIndex("profile_image")), "profile_image.jpg");
                        Log.i("mTag", "Path:" + c.getString(c.getColumnIndex("profile_image")));
                        b = BitmapFactory.decodeStream(new FileInputStream(f));
                    }
                    break;
                }

            } catch (FileNotFoundException e) {
                Log.i("mTag", "No profile picture at given path:");
                b = null;
            }
            c.close();
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            if (b != null && !loadedFromGal) {
                mProfileImg.setImageBitmap(b);
                mProfileImgSet = true;
            } else {
                mProfileImgSet = false;
            }
        }
    }


    private BroadcastReceiver mFbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                if (intent.getBooleanExtra("success", false)) {
                    if (!loadedFromGal) {
                        // set it as profile picture
                        Bitmap b;
                        ContextWrapper cw = new ContextWrapper(getApplicationContext());
                        File directory = cw.getDir("imageDir", Context.MODE_PRIVATE);
                        File mypath = new File(directory, "fb_temp.jpg");
                        try {
                            b = BitmapFactory.decodeStream(new FileInputStream(mypath));
                        } catch (FileNotFoundException ff) {
                            b = null;
                        }
                        if (b != null) {
                            saveAsProfileImage(b);
                            mProfileImg.setImageBitmap(b);
                            mProfileImgSet = true;
                        }
                    }
                }
            }
        }
    };


    private class LoadTextAndCredentials extends AsyncTask<Void, Void, Void> {
        private String fname, lname;

        @Override
        protected Void doInBackground(Void... params) {
            Log.i("mTag", "Loading first name last name");
            String query = "SELECT first_name,last_name FROM meta_data WHERE 1=1";
            Cursor c = dbObj.rawQuery(query, null);
            while (c.moveToNext()) {
                fname = c.getString(c.getColumnIndex("first_name"));
                lname = c.getString(c.getColumnIndex("last_name"));
            }

            query = "SELECT user_id,password FROM user_data WHERE 1=1";
            c = dbObj.rawQuery(query, null);
            while (c.moveToNext()) {
                user_id = c.getInt(c.getColumnIndex("user_id"));
                password = c.getString(c.getColumnIndex("password"));
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            if (firstName != null)
                firstName.setText(fname);

            if (lastName != null)
                lastName.setText(lname);
        }
    }


    private class LoadFlowLayout extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            String query;
            Cursor c;

            query = "SELECT * FROM social_networks_data ORDER BY type ASC";
            c = dbObj.rawQuery(query, null);

            while (c.moveToNext()) {
                mArray[c.getInt(c.getColumnIndex("type")) - 1] = new SocialButton(c.getString(c.getColumnIndex("url")), c.getInt(c.getColumnIndex("type")));
                // Server ops for facebook
                if (c.getInt(c.getColumnIndex("type")) == 1) {
                    new SocialSync().execute(new SocialButton(c.getString(c.getColumnIndex("url")), 1));
                }


            }


            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            LayoutInflater vi;
            View v;
            vi = (LayoutInflater) getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            for (int i = 0; i < mArray.length; i++) {
                if (mArray[i] != null && mArray[i].url != null) {
                    // activated
                    v = vi.inflate(R.layout.badge_icon, null);
                    // by default red add button is not visible (visibility = View.INVISIBLE)
                    setBackResource(mArray[i].type, v);
                    v.setId(i + 10); // SET THE ID
                    mFlowLayout.addView(v, new FlowLayout.LayoutParams(FlowLayout.LayoutParams.WRAP_CONTENT, FlowLayout.LayoutParams.WRAP_CONTENT));
                    mViewArray[i] = v;

                    v.setOnClickListener(mGeneralSocialClickListener);
                    v.setOnLongClickListener(mLongClickListener);
                } else {
                    // deactivated
                    v = vi.inflate(R.layout.badge_icon, null);
                    // by default red add button is not visible
                    (v.findViewById(R.id.addIcon)).setVisibility(View.VISIBLE);
                    setBackResource((i + 1), v); // mArray[i] might be null
                    v.setId(i + 10); // SET THE ID
                    mFlowLayout.addView(v, new FlowLayout.LayoutParams(FlowLayout.LayoutParams.WRAP_CONTENT, FlowLayout.LayoutParams.WRAP_CONTENT));
                    mViewArray[i] = v;

                    v.setOnClickListener(mGeneralSocialClickListener);
                    v.setOnLongClickListener(mLongClickListener);
                }

            } // end of for


        }
    }// end LoadFlowLayout  (conducts first time socialsync for facebook)

    private class DeleteProfilePictureThread extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            dbObj.execSQL("UPDATE user_media SET profile_image= NULL WHERE 1=1");
            mProfileImgSet = false;
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            mProfileImg.setImageResource(R.color.GreyColor);
        }
    }

    private class DeleteExpressionsThread extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            dbObj.execSQL("UPDATE user_media SET exp_image= NULL, exp_type=0 WHERE 1=1");
            f_expimg = null;
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            add_exp.setVisibility(View.VISIBLE);
            exp_view_view.setVisibility(View.GONE);

        }
    }


    public void pickImageGallery() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_PHOTO_FROM_GALLERY);
    }

    public void setBackResource(int type, View v) {
        switch (type) {
            case 1:
                ((ImageView) (v.findViewById(R.id.mIcon))).setImageDrawable(getResources().getDrawable(R.drawable.facebook));
                break;
            case 2:
                ((ImageView) (v.findViewById(R.id.mIcon))).setImageDrawable(getResources().getDrawable(R.drawable.twitter));
                break;
            case 3:
                ((ImageView) (v.findViewById(R.id.mIcon))).setImageDrawable(getResources().getDrawable(R.drawable.instagram));
                break;
            case 4:
                ((ImageView) (v.findViewById(R.id.mIcon))).setImageDrawable(getResources().getDrawable(R.drawable.phone));
                break;
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_PHOTO_FROM_GALLERY && resultCode == Activity.RESULT_OK) {
            if (data == null) {
                Toast.makeText(this, "Try Again", Toast.LENGTH_LONG).show();
                return;
            }
            try {
                InputStream is = this.getContentResolver().openInputStream(data.getData());
                Bitmap b_image = BitmapFactory.decodeStream(is);

                ContextWrapper cw = new ContextWrapper(getApplicationContext());
                File directory = cw.getDir("imageDir", Context.MODE_PRIVATE);

                if (mResultCode == 6) {
                    // save as profile
                    saveAsProfileImage(b_image);
                } else {
                    // save as exp_image
                    saveAsExpImage(b_image);
                }
                if (mResultCode == 6) {
                    mProfileImgSet = true;
                    mProfileImg.setImageBitmap(b_image);
                    loadedFromGal = true;
                } else {
                    f_expimg = new File(directory, "exp_image.jpg");
                    add_exp.setVisibility(View.GONE);
                    exp_view_view.setVisibility(View.VISIBLE);
                    ((ImageView) ((findViewById(R.id.exp_view_view)).findViewById(R.id.exp_view_image))).setImageBitmap(b_image);
                }


            } catch (IOException e) {
            }
            return;
        }



        switch (mResultCode) {
            // mResultCode 1-fb 2-twitter 3-inst 4-google+ 5-phone 6-profile pic 7-exp pic
            case 1:
                fb_CallbackManager.onActivityResult(requestCode, resultCode, data);
                break;
            case 2:
                tw_button.onActivityResult(requestCode, resultCode, data);
                break;
            case 3: // instagram doesnt require handling in result code
                break;
            case 4:
                break;

        }


    }


    private final class SocialSync extends AsyncTask<SocialButton, Void, Void> {

        int operation;  // 0 for delete 1 for add
        boolean success = false;
        SocialButton mData;

        @Override
        protected Void doInBackground(final SocialButton... params) {

            try {

                mData = params[0];

                final JSONObject job = new JSONObject();
                if (params[0].url == null) {
                    job.put("url", "0");
                    operation = 0;
                } else {
                    job.put("url", params[0].url);
                    operation = 1;
                }

                job.put("user_id", user_id);
                job.put("password", password);
                job.put("type", params[0].type);
                job.put("operation", operation);

                Log.i("mTag", "Social object being sent: " + job.toString());


                mClient = new DefaultHttpClient();
                mPost = new HttpPost(MyUrlData.social_sync);
                mBuilder = MultipartEntityBuilder.create();
                mBuilder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
                mBuilder.addTextBody("social_data", job.toString());

                mEntity = mBuilder.build();
                mPost.setEntity(mEntity);


                HttpResponse response = mClient.execute(mPost);
                HttpEntity httpEntity = response.getEntity();

                int statusCode = response.getStatusLine().getStatusCode();


                if (statusCode == 200) {
                    Log.i("mTag", "Server 200 response social sync! ->" + EntityUtils.toString(httpEntity));

                    if (mData.url == null)
                        dbObj.execSQL("UPDATE social_networks_data SET url= NULL WHERE type=" + mData.type);
                    else
                        dbObj.execSQL("UPDATE social_networks_data SET url=\"" + mData.url + "\" WHERE type=" + mData.type);

                    success = true;
                } else {
                    Log.i("mTag", "Eroor response social sync!");
                    success = false;
                }

                EntityUtils.consumeQuietly(httpEntity);

            } catch (Exception e) {
                Log.i("mTag", "Exception in social sync, myintrofrag" + e.toString());
                success = false;
            }

            return null;
        }


        @Override
        protected void onPostExecute(Void sb) {
            if (success)
                success();
            else
                error();
        }

        private void success() {
            Log.i("mTag", "Inside success routine, social sync");

            switch (mData.type) {
                case 1: // fb
                    if (operation == 0)
                        (mViewArray[0].findViewById(R.id.addIcon)).setVisibility(View.VISIBLE);
                    else
                        (mViewArray[0].findViewById(R.id.addIcon)).setVisibility(View.INVISIBLE);

                    LoginManager.getInstance().logOut();
                    break;
                case 2: // twitter
                    if (operation == 0)
                        (mViewArray[1].findViewById(R.id.addIcon)).setVisibility(View.VISIBLE);
                    else
                        (mViewArray[1].findViewById(R.id.addIcon)).setVisibility(View.INVISIBLE);
                    break;
                case 3: // instagram
                    if (operation == 0)
                        (mViewArray[2].findViewById(R.id.addIcon)).setVisibility(View.VISIBLE);
                    else
                        (mViewArray[2].findViewById(R.id.addIcon)).setVisibility(View.INVISIBLE);

                    mInstagramApp.resetAccessToken();
                    break;

                case 4: // phone
                    Log.i("mTag", "hit her!");
                    if (operation == 0)
                        (mViewArray[3].findViewById(R.id.addIcon)).setVisibility(View.VISIBLE);
                    else
                        (mViewArray[3].findViewById(R.id.addIcon)).setVisibility(View.INVISIBLE);
                    break;


            }

        }

        private void error() {
            if (mData.type != 4) {
                Toast.makeText(ProfileSetupOne.this, "Try later..", Toast.LENGTH_LONG).show();
            }
            switch (mData.type) {
                case 1:
                    LoginManager.getInstance().logOut();
                    break;
                case 2:
                    break;
                case 3:
                    mInstagramApp.resetAccessToken();
                    break;
                case 4:
                    break;

            }

        }


    }


    private void initSdkComponents() {
        // sdks's loaded in onCreate for twitter and facebook

        fb_CallbackManager = CallbackManager.Factory.create();
        mLoginManagerInstance = LoginManager.getInstance();

        mLoginManagerInstance.registerCallback(fb_CallbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                if (Profile.getCurrentProfile() != null && AccessToken.getCurrentAccessToken() != null) {
                    mProfile = Profile.getCurrentProfile();
                } else {
                    Profile.fetchProfileForCurrentAccessToken();
                    mProfile = Profile.getCurrentProfile();
                }

                if (mProfile.getLinkUri().toString() != null) {
                    mArray[0].url = mProfile.getLinkUri().toString();
                    mArray[0].type = 1;
                    Toast.makeText(ProfileSetupOne.this, "Adding...", Toast.LENGTH_SHORT).show();
                    new SocialSync().execute(new SocialButton(mArray[0].url, 1));
                } else {
                    Log.i("mTag", "inside onSuccess facebook, link url fetched is null");
                }
            }

            @Override
            public void onCancel() {
            }

            @Override
            public void onError(FacebookException exception) {
            }
        });


        tw_button.setCallback(new Callback<TwitterSession>() {
            @Override
            public void success(Result<TwitterSession> result) {
                mArray[1].url = "https://www.twitter.com/" + result.data.getUserName();
                mArray[1].type = 2;
                Toast.makeText(ProfileSetupOne.this, "Adding...", Toast.LENGTH_SHORT).show();
                new SocialSync().execute(new SocialButton(mArray[1].url, 2));
            }

            @Override
            public void failure(TwitterException exception) {
            }
        });

        mInstagramApp = new InstagramApp(this, InstagramData.CLIENT_ID, InstagramData.CLIENT_SECRET, InstagramData.CALLBACK_URL);
        listener = new InstagramApp.OAuthAuthenticationListener() {
            @Override
            public void onSuccess() {
                mArray[2].url = "https://www.instagram.com/" + mInstagramApp.getUserName();
                mArray[2].type = 3;
                Toast.makeText(ProfileSetupOne.this, "Adding...", Toast.LENGTH_SHORT).show();
                new SocialSync().execute(new SocialButton(mArray[2].url, 3));
            }

            @Override
            public void onFail(String error) {
            }
        };
        mInstagramApp.setListener(listener);

        telView = findViewById(R.id.telView);
        telView.setVisibility(View.GONE);


        telView.findViewById(R.id.mButton).setOnClickListener(telephonyMain);
        telView.findViewById(R.id.cancelText).setOnClickListener(telephonyCancel);


    }// end initSdkComponents

    @Override
    public void onBackPressed() {
        if (telView.getVisibility() == View.VISIBLE) {
            mTelHelper.is_verifying = false;
            mTelHelper.code = null;
            telView.setVisibility(View.GONE);
            resetTelView(telView);


            View view = findViewById(R.id.relLayout);
            if (view != null) {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
            view.requestFocus();
        } else
            super.onBackPressed();
    }

    private Bitmap getResizedBitmap(Bitmap image, int maxSize) {
        int width = image.getWidth();
        int height = image.getHeight();

        float bitmapRatio = (float) width / (float) height;
        if (bitmapRatio > 0) {
            width = maxSize;
            height = (int) (width / bitmapRatio);
        } else {
            height = maxSize;
            width = (int) (height * bitmapRatio);
        }
        return Bitmap.createScaledBitmap(image, width, height, true);
    }

    private void resetTelView(View mView) {
        ((Button) mView.findViewById(R.id.mButton)).setText("Send Verification Code");
        ((EditText) mView.findViewById(R.id.mEditText)).setText("");
        ((EditText) mView.findViewById(R.id.mEditText)).setHint("Include country code (Ex. +91)");
    }


    //  following methods only deal with memory and sql, DO  and image setting and variable set elsewhere

    private String saveAsProfileImage(Bitmap b) {
        // deals only wirh memory
        Bitmap mTemp;

        // path to /data/data/yourapp/app_data/imageDir
        ContextWrapper cw = new ContextWrapper(getApplicationContext());
        File directory = cw.getDir("imageDir", Context.MODE_PRIVATE);
        File mypath = new File(directory, "profile_image.jpg");

        try {
            FileOutputStream fos = new FileOutputStream(mypath);
            b.compress(Bitmap.CompressFormat.JPEG, 99, fos);
            long originalSize = mypath.length();
            fos.close();

            if (originalSize > (150 * 1024)) {
                DisplayMetrics metrics = new DisplayMetrics();
                getWindowManager().getDefaultDisplay().getMetrics(metrics);
                int mWidth = (int) (metrics.widthPixels);
                mTemp = getResizedBitmap(b, mWidth);

                fos = new FileOutputStream(mypath);
                mTemp.compress(Bitmap.CompressFormat.JPEG, 99, fos);

                if (mypath.length() > originalSize) {
                    fos.close();
                    fos = new FileOutputStream(mypath);
                    b.compress(Bitmap.CompressFormat.JPEG, 90, fos);
                    fos.close();

                }
            }

            dbObj.execSQL("UPDATE user_media SET profile_image =\"" + directory.getAbsolutePath() + "\" WHERE 1=1");
            dbObj.execSQL("UPDATE server_sync SET profile_pic_sync =1 WHERE 1=1");


        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return directory.getAbsolutePath();

    }
    private String saveAsExpImage(Bitmap b) {
        // deals only wirh memory
        Bitmap mTemp;

        // path to /data/data/yourapp/app_data/imageDir
        ContextWrapper cw = new ContextWrapper(getApplicationContext());
        File directory = cw.getDir("imageDir", Context.MODE_PRIVATE);
        File mypath = new File(directory, "exp_image.jpg");

        try {
            FileOutputStream fos = new FileOutputStream(mypath);
            b.compress(Bitmap.CompressFormat.JPEG, 99, fos);
            long originalSize = mypath.length();
            fos.close();

            if (originalSize > (300 * 1024)) {
                DisplayMetrics metrics = new DisplayMetrics();
                getWindowManager().getDefaultDisplay().getMetrics(metrics);
                int mWidth = (int) (metrics.widthPixels);
                mTemp = getResizedBitmap(b, mWidth);

                fos = new FileOutputStream(mypath);
                mTemp.compress(Bitmap.CompressFormat.JPEG, 99, fos);

                if (mypath.length() > originalSize) {
                    fos.close();
                    fos = new FileOutputStream(mypath);
                    b.compress(Bitmap.CompressFormat.JPEG, 90, fos);
                    fos.close();

                }
            }

            dbObj.execSQL("UPDATE user_media SET exp_image =\"" + directory.getAbsolutePath() + "\" WHERE 1=1");
            dbObj.execSQL("UPDATE user_media SET exp_type =1 WHERE 1=1");
            dbObj.execSQL("UPDATE server_sync SET exp_sync =1 WHERE 1=1");


        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return directory.getAbsolutePath();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mFbReceiver);
    }
}
