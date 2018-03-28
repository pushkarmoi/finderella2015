package club.finderella.finderella;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import java.util.Arrays;


import club.finderella.finderella.Helpers.FragmentLifecycle;
import club.finderella.finderella.Helpers.Telephony;
import club.finderella.finderella.POJO.MyUrlData;
import club.finderella.finderella.POJO.SocialButton;
import club.finderella.finderella.Helpers.MyDBHandler;
import club.finderella.finderella.Helpers.MySingleton;
import club.finderella.finderella.InstagramOauth.InstagramApp;
import club.finderella.finderella.InstagramOauth.InstagramData;
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


public class MyIntroFrag extends Fragment implements FragmentLifecycle, MainActivity.MonitorTelView {


    private MyDBHandler db;
    private SQLiteDatabase dbObj;

    private static SocialButton mArray[] = new SocialButton[]{null, null, null, null};
    private static View mViewArray[] = new View[]{null, null, null, null};

    private static int exp_type = 0;
    private static FlowLayout mFlowLayout;
    private static String exp_source;
    private static View fragView;

    private static ImageView exp_img;

    // private static File f_profileimg = null;     not required as mProfieImageSet conveys info, and Photoviewer loads address separately
    private static File f_expimg = null;    // required, as no expImgSet variable exists

    private static TextView mName, mMetaData, mLocation, editCommand;
    private static EditText mStatus;

    private static int mResultCode = 0;

    private static boolean text_sync = false;
    private static boolean profile_pic_sync = false;
    private static boolean exp_sync = false;

    private static CircleImageView mProfileImg;
    private static boolean mProfileImgSet = false;


    private static int user_id;
    private static String password;
    private RequestQueue queue;

    private LoginManager mLoginManagerInstance;
    private CallbackManager fb_CallbackManager;
    private static Profile mProfile = null;

    private TwitterLoginButton tw_button;

    private static InstagramApp mInstagramApp;
    private InstagramApp.OAuthAuthenticationListener listener;

    private HttpClient mClient;
    private HttpPost mPost;
    private MultipartEntityBuilder mBuilder;
    private HttpEntity mEntity;

    private View telView;
    private static Telephony mTelHelper = new Telephony();


    private static final String TWITTER_KEY = "M8IG2YWR9PyWBAFii50ZOr7VD";
    private static final String TWITTER_SECRET = "trD499NJFFzCewOngu9NWO2Qxsl02eLrm2Gkqi0agXGraFNLoD";
    private static final int PICK_PHOTO_FROM_GALLERY = 20;
    private static final int PICK_PHOTO_FROM_CAMERA = 21;

    private final View.OnClickListener addExp = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            new BottomSheet.Builder(getActivity(), R.style.MyBottomSheetTheme).title("Import from").sheet(R.menu.gc_picker).listener(new DialogInterface.OnClickListener() {
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
            mResultCode = 7;
        }
    };
    private final View.OnClickListener editExpImage = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            new BottomSheet.Builder(getActivity(), R.style.MyBottomSheetTheme).title("Change picture").sheet(R.menu.addordelete).listener(new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    switch (which) {
                        case R.id.add_bottomsheet:
                            mResultCode = 7;
                            dialog.dismiss();
                            new BottomSheet.Builder(getActivity(), R.style.MyBottomSheetTheme).title("Import from").sheet(R.menu.gc_picker).listener(new DialogInterface.OnClickListener() {
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
                            new BottomSheet.Builder(getActivity(), R.style.MyBottomSheetTheme).title("Delete picture?").sheet(R.menu.yesorno).listener(
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
        }
    };
    private final View.OnClickListener editExpVideo = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
        }
    };      // not used
    private final View.OnClickListener importProfilePic = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (mProfileImgSet) {
                // Image is set
                // add or delete builder
                new BottomSheet.Builder(getActivity(), R.style.MyBottomSheetTheme).title("Import from").sheet(R.menu.addordelete).listener(new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case R.id.add_bottomsheet:
                                mResultCode = 6;
                                dialog.dismiss();
                                new BottomSheet.Builder(getActivity(), R.style.MyBottomSheetTheme).title("Import from").sheet(R.menu.gc_picker).listener(new DialogInterface.OnClickListener() {
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
                                new BottomSheet.Builder(getActivity(), R.style.MyBottomSheetTheme).title("Delete picture?").sheet(R.menu.yesorno).listener(
                                        new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog2, int which) {
                                                switch (which) {
                                                    case R.id.yes_delete:
                                                        new DeleteProfilePictureThread().execute();
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
                new BottomSheet.Builder(getActivity(), R.style.MyBottomSheetTheme).title("Import from").sheet(R.menu.gc_picker).listener(new DialogInterface.OnClickListener() {
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
    };
    private final View.OnClickListener profilePicIconClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (mProfileImgSet) {
                // LAUNCH IN FULL MODE

                Intent mIntent = new Intent(getContext(), ImageViewer.class);
                mIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                mIntent.putExtra("code", 1);    // code for exp pic
                startActivity(mIntent);
            } else {
                // Nothing
            }
        }
    };

    private final View.OnClickListener viewExpImage = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (f_expimg != null) {

                Intent mIntent = new Intent(getContext(), ImageViewer.class);
                mIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                mIntent.putExtra("code", 2);    //code for  exp pic
                startActivity(mIntent);
            }
        }
    };


    private final View.OnClickListener mClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case 10:
                    if (mArray[0].url == null) {
                        mLoginManagerInstance.logInWithReadPermissions(getActivity(), Arrays.asList("public_profile")); // open facebook
                        mResultCode = 1;
                    } else {
                        Intent mIntent = new Intent(getActivity(), Browser.class);
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
                        Intent mIntent = new Intent(getActivity(), Browser.class);
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
                        Intent mIntent = new Intent(getActivity(), Browser.class);
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
                        new BottomSheet.Builder(getActivity(), R.style.MyBottomSheetTheme).title("Call " + mArray[3].url + "?").sheet(R.menu.yesorno).listener(
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        if (which == R.id.yes_delete) {
                                            dialog.dismiss();
                                            Intent mInt = new Intent(Intent.ACTION_DIAL);
                                            mInt.setData(Uri.parse("tel:" + mArray[3].url));
                                            getContext().startActivity(mInt);
                                        } else {
                                            dialog.dismiss();

                                        }
                                    }
                                }).show();
                    }
                    break;
            }


        }
    };
    private final View.OnLongClickListener mLongClickListener = new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(final View v) {
            if (mArray[(v.getId() - 10)].url != null) {
                new BottomSheet.Builder(getActivity(), R.style.MyBottomSheetTheme).title("Delete?").sheet(R.menu.yesorno).listener(new DialogInterface.OnClickListener() {
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
                            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                            imm.hideSoftInputFromWindow(telView.getWindowToken(), 0);
                        }

                    } catch (Exception e) {
                        Log.i("mTag", "exception to get number --" + e);
                        Toast.makeText(getActivity(), "No SIM Card", Toast.LENGTH_LONG).show();
                        mTelHelper.code = null;
                        mTelHelper.is_verifying = false;
                    }
                }


            } else {
                // hide keyboard
                // hiding keyboard
                if (telView != null) {
                    InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
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
                    Toast.makeText(getActivity(), "Wrong Code Entered!", Toast.LENGTH_LONG).show();
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

            View view = fragView.findViewById(R.id.lay);
            if (view != null) {
                InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
            view.requestFocus();
        }
    };


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FacebookSdk.sdkInitialize(getActivity().getApplicationContext());

        final TwitterAuthConfig authConfig = new TwitterAuthConfig(TWITTER_KEY, TWITTER_SECRET);
        Fabric.with(getActivity().getApplicationContext(), new Twitter(authConfig));

        Log.i("mTag", "*********** ON CREATE EXECUTED FOR myintro frag ***************");

        db = new MyDBHandler(getActivity(), null, null, 1);
        dbObj = db.getWritableDatabase();

        queue = MySingleton.getInstance(getActivity()).getRequestQueue();
    }


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.layout_myintro, container, false);
        Log.i("workflow", "on create view called for MyIntro");
        fragView = v;

        mName = (TextView) v.findViewById(R.id.mName);
        mMetaData = (TextView) v.findViewById(R.id.mMetaData);
        mLocation = (TextView) v.findViewById(R.id.mLocation);
        mStatus = (EditText) v.findViewById(R.id.mStatus);
        editCommand = (TextView) v.findViewById(R.id.editCommand);
        mFlowLayout = (FlowLayout) fragView.findViewById(R.id.mFlowLayout);


        mStatus.setOnFocusChangeListener(new View.OnFocusChangeListener() {

            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    // move cursor to end
                    // show keyboard
                    mStatus.append("");
                    InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.showSoftInput(mStatus, InputMethodManager.SHOW_IMPLICIT);

                    editCommand.setText("Done");
                } else {
                    editCommand.setText("Edit");

                    // hiding keyboard
                    View view = fragView.findViewById(R.id.lay);
                    if (view != null) {
                        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                    }
                    view.requestFocus();


                }
            }
        });

        editCommand.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String statText;

                if (mStatus.hasFocus()) {

                    mStatus.clearFocus();
                    editCommand.setText("Edit");
                    text_sync = true;

                    if (mStatus.getText().length() == 0) {
                        statText = "";
                    } else {
                        statText = mStatus.getText().toString();
                    }

                    getActivity().startService(new Intent(getActivity(), TextSync.class));

                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            ContentValues cv = new ContentValues();
                            cv.put("status", statText);
                            dbObj.update("meta_data", cv, null, null);  // passing null  as where clause updates all rows
                        }
                    }).start(); // sql save

                } else {
                    mStatus.requestFocus();
                    editCommand.setText("Done");
                }
            }
        });

        mProfileImg = (CircleImageView) v.findViewById(R.id.mProfileImg);
        mProfileImg.setOnClickListener(profilePicIconClick);
        fragView.findViewById(R.id.textView2).setOnClickListener(importProfilePic);

        tw_button = (TwitterLoginButton) v.findViewById(R.id.tw_button);

        initSdkComponents();
        new LoadTextAndCredentials().execute(); // text
        new InitializeComponents().execute(); // exp images and social networks
        new LoadProfileImage().execute();

        return v;
    }

    @Override
    public void onPauseFragment() {
        Log.i("workflow", "On pause(custom) executed for MY_INTRO FRAG");
    }

    @Override
    public void onResumeFragment() {
        Log.i("workflow", "On resume(custom) executed for MY_INTRO FRAG");
    }

    public void pickImageGallery() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_PHOTO_FROM_GALLERY);
    }

    private class LoadProfileImage extends AsyncTask<Void, Void, Void> {
        private Bitmap b;

        @Override
        protected Void doInBackground(Void... params) {
            String query = "SELECT profile_image FROM user_media WHERE 1=1";
            Cursor c = dbObj.rawQuery(query, null);
            while (c.moveToNext()) {

                try {
                    if (c.getString(c.getColumnIndex("profile_image")) == null) {
                        b = null;

                    } else {
                        File f = new File(c.getString(c.getColumnIndex("profile_image")), "profile_image.jpg");
                        b = BitmapFactory.decodeStream(new FileInputStream(f));
                    }

                } catch (FileNotFoundException e) {
                    Log.i("mTag", "No profile picture at given path:" + c.getString(c.getColumnIndex("profile_image")));
                }
            }

            c.close();

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            if (b != null) {
                mProfileImg.setImageBitmap(b);
                mProfileImgSet = true;
            } else {
                mProfileImgSet = false;
            }
        }
    }

    private class LoadTextAndCredentials extends AsyncTask<Void, Void, Void> {

        String name, metaData, location, status;

        @Override
        protected Void doInBackground(Void... params) {
            Cursor c = dbObj.rawQuery("SELECT * FROM meta_data WHERE 1=1", null);
            while (c.moveToNext()) {
                name = c.getString(c.getColumnIndex("first_name")) + " " + c.getString(c.getColumnIndex("last_name"));

                String temp = "";
                if (c.getInt(c.getColumnIndex("age")) != 0)
                    temp += String.valueOf(c.getInt(c.getColumnIndex("age"))) + ", " + c.getString(c.getColumnIndex("gender"));       // gender will always be present
                else
                    temp = c.getString(c.getColumnIndex("gender"));


                metaData = temp;

                if (c.getString(c.getColumnIndex("location")) != null)
                    location = c.getString(c.getColumnIndex("location"));
                else
                    location = "";


                if (c.getString(c.getColumnIndex("status")) != null)
                    status = c.getString(c.getColumnIndex("status"));
                else
                    status = "Hey there! Nce to meet you";       // Default value will always be non-zero.
            }
            c.close();

            c = dbObj.rawQuery("SELECT user_id, password FROM user_data WHERE 1=1", null);
            while (c.moveToNext()) {
                user_id = c.getInt(c.getColumnIndex("user_id"));
                password = c.getString(c.getColumnIndex("password"));
            }
            c.close();


            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {

            if (name != null)
                mName.setText(name);
            if (metaData != null)
                mMetaData.setText(metaData);
            if (location != null)
                mLocation.setText(location);
            if (status != null)
                mStatus.setText(status);
        }
    }

    private class DeleteProfilePictureThread extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            dbObj.execSQL("UPDATE user_media SET profile_image= NULL WHERE 1=1");
            mProfileImgSet = false;
            dbObj.execSQL("UPDATE server_sync SET profile_pic_sync =1 WHERE 1=1");
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            mProfileImg.setImageResource(R.color.GreyColor);
            profile_pic_sync = true;
            getActivity().startService(new Intent(getActivity(), MediaSync.class));
        }
    }

    private class DeleteExpressionsThread extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            dbObj.execSQL("UPDATE user_media SET exp_image= NULL, exp_type=0 WHERE 1=1");
            dbObj.execSQL("UPDATE server_sync SET exp_sync =1 WHERE 1=1");
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            (fragView.findViewById(R.id.add_exp)).setVisibility(View.VISIBLE);
            (fragView.findViewById(R.id.exp_view_view)).setVisibility(View.GONE);
            exp_sync = true;
            f_expimg = null;
            exp_source = null;
            getActivity().startService(new Intent(getActivity(), MediaSync.class));
        }
    }

    private class InitializeComponents extends AsyncTask<Void, Void, Void> {
        private String query;
        File f;
        Bitmap b = null;

        @Override
        protected Void doInBackground(Void... params) {
            query = "SELECT * FROM user_media";
            Cursor c = dbObj.rawQuery(query, null);
            while (c.moveToNext()) {
                switch (c.getInt(c.getColumnIndex("exp_type"))) {
                    case 0:
                        exp_type = 0;
                        exp_source = null;
                        break;
                    case 1:
                        exp_type = 1;
                        exp_source = c.getString(c.getColumnIndex("exp_image"));
                        f = new File(exp_source, "exp_image.jpg");
                        f_expimg = f;
                        try {
                            b = BitmapFactory.decodeStream(new FileInputStream(f));
                        } catch (IOException i) {
                            b = null;
                        }

                        break;

                    case 2:
                        exp_type = 2;
                        exp_source = c.getString(c.getColumnIndex("exp_video"));
                        break;

                    default:
                        exp_type = 0;
                        exp_source = null;
                        break;
                }
            }
            query = "SELECT * FROM social_networks_data ORDER BY type ASC";
            c = dbObj.rawQuery(query, null);

            while (c.moveToNext()) {
                mArray[c.getInt(c.getColumnIndex("type")) - 1] = new SocialButton(c.getString(c.getColumnIndex("url")), c.getInt(c.getColumnIndex("type")));
            }
            c.close();


            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {

            exp_img = (ImageView) ((fragView.findViewById(R.id.exp_view_view))).findViewById(R.id.exp_view_image);
            exp_img.setOnClickListener(viewExpImage); //editExpImage
            ((fragView.findViewById(R.id.add_exp)).findViewById(R.id.blueButton)).setOnClickListener(addExp);
            ((fragView.findViewById(R.id.exp_view_view)).findViewById(R.id.exp_edit)).setOnClickListener(editExpImage);


            if (exp_type == 0) {

                f_expimg = null;

                (fragView.findViewById(R.id.add_exp)).setVisibility(View.VISIBLE);
                (fragView.findViewById(R.id.exp_view_view)).setVisibility(View.GONE);

            }

            if (exp_type == 1) {

                if (b != null)
                    exp_img.setImageBitmap(b);
                else
                    exp_img.setImageResource(ContextCompat.getColor(getContext(), R.color.GreyLight));


                (fragView.findViewById(R.id.add_exp)).setVisibility(View.GONE);
                (fragView.findViewById(R.id.exp_view_view)).setVisibility(View.VISIBLE);
            }

            if (exp_type == 2) {
                // set video
                (fragView.findViewById(R.id.add_exp)).setVisibility(View.GONE);
                (fragView.findViewById(R.id.exp_view_view)).setVisibility(View.VISIBLE);
            }


            LayoutInflater vi;
            View v;
            vi = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            for (int i = 0; i < mArray.length; i++) {
                if (mArray[i].url != null) {
                    // activated
                    v = vi.inflate(R.layout.badge_icon, null);
                    // by default red add button is not visible (visibility = View.INVISIBLE)
                    setBackResource(mArray[i].type, v);
                    v.setId(i + 10); // SET THE ID
                    mFlowLayout.addView(v, new FlowLayout.LayoutParams(FlowLayout.LayoutParams.WRAP_CONTENT, FlowLayout.LayoutParams.WRAP_CONTENT));
                    mViewArray[i] = v;

                    v.setOnClickListener(mClickListener);
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

                    v.setOnClickListener(mClickListener);
                    v.setOnLongClickListener(mLongClickListener);
                }
            }


        }
    }   // loads flow layout and expressions


    private final class SocialSync extends AsyncTask<SocialButton, Void, Void> {

        int operation;  // 0 for delete 1 for add
        boolean success = false;
        SocialButton mData;

        @Override
        protected Void doInBackground(final SocialButton... params) {

            Log.i("mTag", "Social button obj, type = " + params[0].type + ", url = " + params[0].url);

            try {

                mData = params[0];

                final JSONObject job = new JSONObject();
                // SENDS 0 AS VALUE IF WANTS TO DELETE
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
                Toast.makeText(getActivity(), "Try later..", Toast.LENGTH_LONG).show();
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


        // FACEBOOK

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
                    Toast.makeText(getActivity(), "Adding...", Toast.LENGTH_SHORT).show();
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
                Log.i("images", "fb exception" + exception.toString());
            }
        });


        // TWITTER

        tw_button.setCallback(new Callback<TwitterSession>() {
            @Override
            public void success(Result<TwitterSession> result) {
                mArray[1].url = "https://www.twitter.com/" + result.data.getUserName();
                mArray[1].type = 2;
                Toast.makeText(getActivity(), "Adding...", Toast.LENGTH_SHORT).show();
                new SocialSync().execute(new SocialButton(mArray[1].url, 2));
            }

            @Override
            public void failure(TwitterException exception) {
                Log.i("images", "twitter exception" + exception.toString());
            }
        });

        // INSTAGRAM
        mInstagramApp = new InstagramApp(getActivity(), InstagramData.CLIENT_ID, InstagramData.CLIENT_SECRET, InstagramData.CALLBACK_URL);
        listener = new InstagramApp.OAuthAuthenticationListener() {
            @Override
            public void onSuccess() {
                mArray[2].url = "https://www.instagram.com/" + mInstagramApp.getUserName();
                mArray[2].type = 3;
                Toast.makeText(getActivity(), "Adding...", Toast.LENGTH_SHORT).show();
                new SocialSync().execute(new SocialButton(mArray[2].url, 3));
            }

            @Override
            public void onFail(String error) {
            }
        };
        mInstagramApp.setListener(listener);


        telView = fragView.findViewById(R.id.telView);
        telView.setVisibility(View.GONE);
        telView.findViewById(R.id.mButton).setOnClickListener(telephonyMain);
        telView.findViewById(R.id.cancelText).setOnClickListener(telephonyCancel);

    }// end initSdkComponents

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_PHOTO_FROM_GALLERY && resultCode == Activity.RESULT_OK) {
            if (data == null) {
                Toast.makeText(getActivity(), "Try Again", Toast.LENGTH_LONG).show();
                return;
            }
            try {
                InputStream is = getContext().getContentResolver().openInputStream(data.getData());
                Bitmap b_image = BitmapFactory.decodeStream(is);

                ContextWrapper cw = new ContextWrapper(getContext().getApplicationContext());
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
                    profile_pic_sync = true;
                    mProfileImg.setImageBitmap(b_image);
                    getActivity().startService(new Intent(getActivity(), MediaSync.class));
                } else {
                    f_expimg = new File(directory, "exp_image.jpg");
                    (fragView.findViewById(R.id.add_exp)).setVisibility(View.GONE);
                    (fragView.findViewById(R.id.exp_view_view)).setVisibility(View.VISIBLE);
                    exp_img.setImageBitmap(b_image);
                    exp_type = 1;
                    exp_source = directory.getAbsolutePath();
                    exp_sync = true;
                    getActivity().startService(new Intent(getActivity(), MediaSync.class));
                }


            } catch (IOException e) {
            }


            return;
        }


        switch (mResultCode) {
            // mResultCode 1-fb 2-twitter 3-inst 4-phone 5-none 6-profile pic 7-exp pic
            case 1:
                fb_CallbackManager.onActivityResult(requestCode, resultCode, data);
                break;
            case 2:
                tw_button.onActivityResult(requestCode, resultCode, data);
                break;
            case 3:
                break;
            case 4:
                break;
        }
    }

    public void setBackResource(int type, View v) {
        switch (type) {
            case 1:
                ((ImageView) (v.findViewById(R.id.mIcon))).setImageDrawable(getActivity().getResources().getDrawable(R.drawable.facebook));
                break;
            case 2:
                ((ImageView) (v.findViewById(R.id.mIcon))).setImageDrawable(getActivity().getResources().getDrawable(R.drawable.twitter));
                break;
            case 3:
                ((ImageView) (v.findViewById(R.id.mIcon))).setImageDrawable(getActivity().getResources().getDrawable(R.drawable.instagram));
                break;
            case 4:
                ((ImageView) (v.findViewById(R.id.mIcon))).setImageDrawable(getActivity().getResources().getDrawable(R.drawable.phone));
                break;
        }
    }


    @Override
    public boolean getTelViewVisibility() {
        if (telView.getVisibility() == View.VISIBLE) {
            mTelHelper.is_verifying = false;
            mTelHelper.code = null;
            telView.setVisibility(View.GONE);
            resetTelView(telView);

            View view = fragView.findViewById(R.id.lay);
            if (view != null) {
                InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
            view.requestFocus();
            return true;
        } else {
            return false;
        }
    }


    public Bitmap getResizedBitmap(Bitmap image, int maxSize) {
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

    private String saveAsProfileImage(Bitmap b) {
        // deals only wirh memory
        Bitmap mTemp;

        // path to /data/data/yourapp/app_data/imageDir
        ContextWrapper cw = new ContextWrapper(getContext().getApplicationContext());
        File directory = cw.getDir("imageDir", Context.MODE_PRIVATE);
        File mypath = new File(directory, "profile_image.jpg");

        try {
            FileOutputStream fos = new FileOutputStream(mypath);
            b.compress(Bitmap.CompressFormat.JPEG, 99, fos);
            long originalSize = mypath.length();
            fos.close();

            if (originalSize > (150 * 1024)) {
                DisplayMetrics metrics = new DisplayMetrics();
                getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);
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
        ContextWrapper cw = new ContextWrapper(getContext().getApplicationContext());
        File directory = cw.getDir("imageDir", Context.MODE_PRIVATE);
        File mypath = new File(directory, "exp_image.jpg");

        try {
            FileOutputStream fos = new FileOutputStream(mypath);
            b.compress(Bitmap.CompressFormat.JPEG, 99, fos);
            long originalSize = mypath.length();
            fos.close();

            if (originalSize > (300 * 1024)) {
                DisplayMetrics metrics = new DisplayMetrics();
                getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);
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


}
