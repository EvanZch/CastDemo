package com.example.castdemo;

import android.content.Context;
import android.hardware.display.DisplayManager;
import android.hardware.display.WifiDisplay;
import android.hardware.display.WifiDisplayStatus;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * @author : EvanZch
 * @date : 2020/8/10 10:34
 * description:
 **/
public class CastNeedPairDeviceAdapter extends BaseQuickAdapter<WifiDisplay, BaseViewHolder> {


    private static final String TAG = "CastNeedPairDeviceAdapt";
    private DisplayManager mDisplayManager;

    public CastNeedPairDeviceAdapter(int layoutResId, @Nullable List<WifiDisplay> data) {
        super(layoutResId, data);
        mDisplayManager = (DisplayManager) App.getInstance().getSystemService(Context.DISPLAY_SERVICE);
    }


    @Override
    protected void convert(@NotNull BaseViewHolder baseViewHolder, WifiDisplay display) {

        TextView tvDeviceName = baseViewHolder.findView(R.id.tv_cast_device_name);
        TextView tvDeviceState = baseViewHolder.findView(R.id.tv_cast_state);
        ImageView ivSettings = baseViewHolder.findView(R.id.iv_settings);

        LinearLayout llCast = baseViewHolder.findView(R.id.ll_cast);
        tvDeviceName.setText(display.getFriendlyDisplayName());
        tvDeviceState.setText(R.string.wireless_display_route_description);
        boolean canConnect = display.canConnect();
        llCast.setEnabled(canConnect);
        if (canConnect) {
            llCast.setAlpha(1f);
        } else {
            llCast.setAlpha(0.4f);
        }

        LogUtil.d(TAG + "--UnpairedWifiDisplayPreference  canConnect=" + display.canConnect());
        if (canConnect) {
            //  setOrder(ORDER_AVAILABLE);
        } else {
            // setOrder(ORDER_UNAVAILABLE);
            tvDeviceState.setText(R.string.wifi_display_status_in_use);
        }
        ivSettings.setVisibility(View.GONE);
    }

    private WifiDisplayStatus mWifiDisplayStatus;

    private WifiDisplay findWifiDisplay(String deviceAddress) {
        mWifiDisplayStatus = mDisplayManager.getWifiDisplayStatus();
        if (mWifiDisplayStatus != null && deviceAddress != null) {
            for (WifiDisplay display : mWifiDisplayStatus.getDisplays()) {
                if (display.getDeviceAddress().equals(deviceAddress)) {
                    return display;
                }
            }
        }
        return null;
    }
}
