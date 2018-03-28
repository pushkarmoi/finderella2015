package club.finderella.finderella.Coach;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import club.finderella.finderella.R;


public class StepOne extends Fragment {

    private TextView button1;
    private Common mInterface;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        mInterface = (Common) context;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.step_one, container, false);
        button1 = (TextView) v.findViewById(R.id.button1);

        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mInterface.next();
            }
        });
        return v;
    }
}
