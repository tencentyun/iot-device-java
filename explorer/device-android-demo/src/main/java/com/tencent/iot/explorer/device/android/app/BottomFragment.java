package com.tencent.iot.explorer.device.android.app;

import android.annotation.SuppressLint;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.tencent.iot.explorer.device.android.app.R;

@SuppressLint("NewApi")
public class BottomFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_bottom, container, false);
    }
}
