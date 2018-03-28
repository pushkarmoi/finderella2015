package club.finderella.finderella.Helpers;


import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.util.Random;

import club.finderella.finderella.R;

public class Telephony {

    public String code;
    public boolean is_verifying;
    public String mPhoneNumber;

    public Telephony() {
        code = null;
        is_verifying = false;
        mPhoneNumber = null;

    }


    public static String getRandomCode() {
        Random rand = new Random();

        String res = "";

        for (int i = 0; i < 4; i++) {
            res += String.valueOf(rand.nextInt((9 - 1) + 1) + 1);        //((max - min) + 1) + min
        }

        return res;
    }


}
