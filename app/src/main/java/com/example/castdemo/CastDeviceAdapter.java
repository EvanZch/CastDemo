package com.example.castdemo;

import android.content.Context;
import android.hardware.display.DisplayManager;
import android.hardware.display.WifiDisplay;
import android.hardware.display.WifiDisplayStatus;
import android.media.MediaRouter;
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
public class CastDeviceAdapter extends BaseQuickAdapter<MediaRouter.RouteInfo, BaseViewHolder> {

    private DisplayManager mDisplayManager;

    public CastDeviceAdapter(int layoutResId, @Nullable List<MediaRouter.RouteInfo> data) {
        super(layoutResId, data);
        mDisplayManager = (DisplayManager) App.getInstance().getSystemService(Context.DISPLAY_SERVICE);
    }


    @Override
    protected void convert(@NotNull BaseViewHolder baseViewHolder, MediaRouter.RouteInfo routeInfo) {

        TextView tvDeviceName = baseViewHolder.findView(R.id.tv_cast_device_name);
        TextView tvDeviceState = baseViewHolder.findView(R.id.tv_cast_state);
        ImageView ivSettings = baseViewHolder.findView(R.id.iv_settings);
        addChildClickViewIds(R.id.iv_settings);
        LinearLayout llCast = baseViewHolder.findView(R.id.ll_cast);


        CharSequence routeName = routeInfo.getName();
        CharSequence routeDesc = routeInfo.getDescription();
        boolean enabled = routeInfo.isEnabled();


        tvDeviceName.setText(routeName);
        tvDeviceState.setText(routeDesc);


        llCast.setEnabled(enabled);
        ivSettings.setEnabled(enabled);
        if (enabled) {
            llCast.setAlpha(1f);
        } else {
            llCast.setAlpha(0.4f);
        }

        if (routeInfo.isSelected()) {
            if (routeInfo.isConnecting()) {
                tvDeviceState.setText(R.string.wifi_display_status_connecting);
            } else {
                tvDeviceState.setText(R.string.wifi_display_status_connected);
            }
        } else {
            if (routeInfo.isEnabled()) {
            } else {
                if (routeInfo.getStatusCode() == MediaRouter.RouteInfo.STATUS_IN_USE) {
                    tvDeviceState.setText(R.string.wifi_display_status_in_use);
                } else {
                    tvDeviceState.setText(R.string.wifi_display_status_not_available);
                }
            }
        }

        WifiDisplay wifiDisplay = findWifiDisplay(routeInfo.getDeviceAddress());

        if (wifiDisplay != null) {
            ivSettings.setVisibility(View.VISIBLE);
        } else {
            ivSettings.setVisibility(View.GONE);
        }

        if (mSettingsClickListener != null){
            ivSettings.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mSettingsClickListener.onSettingsClick(getItemPosition(routeInfo));
                }
            });
        }


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


    public interface SettingsClickListener {
        void onSettingsClick(int position);
    }

    private  SettingsClickListener mSettingsClickListener;
    public void setSettingsClickListener(SettingsClickListener settingsClickListener){
        mSettingsClickListener = settingsClickListener;
    }
}
