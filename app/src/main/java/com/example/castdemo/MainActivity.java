package com.example.castdemo;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.hardware.display.DisplayManager;
import android.hardware.display.WifiDisplay;
import android.hardware.display.WifiDisplayStatus;
import android.media.MediaRouter;
import android.net.Uri;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.Menu;
import android.view.View;
import android.widget.CompoundButton;

import com.android.internal.app.MediaRouteDialogPresenter;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.listener.OnItemClickListener;
import com.kennyc.view.MultiStateView;
import com.kyleduo.switchbutton.SwitchButton;
import com.lxj.xpopup.XPopup;
import com.lxj.xpopup.core.BasePopupView;
import com.lxj.xpopup.enums.PopupAnimation;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {


    private static final String TAG = "MainActivity";

    private MediaRouter mRouter;
    private DisplayManager mDisplayManager;

    private boolean mStarted;
    private int mPendingChanges;

    private boolean mWifiDisplayOnSetting;
    private WifiDisplayStatus mWifiDisplayStatus;

    private WifiP2pManager mWifiP2pManager;
    private WifiP2pManager.Channel mWifiP2pChannel;


    private Handler mHandler;


    private static final int MENU_ID_ENABLE_WIFI_DISPLAY = Menu.FIRST;


    private static final int CHANGE_SETTINGS = 1 << 0;
    private static final int CHANGE_ROUTES = 1 << 1;
    private static final int CHANGE_WIFI_DISPLAY_STATUS = 1 << 2;
    private static final int CHANGE_ALL = -1;

    private static final int ORDER_CERTIFICATION = 1;
    private static final int ORDER_CONNECTED = 2;
    private static final int ORDER_AVAILABLE = 3;
    private static final int ORDER_UNAVAILABLE = 4;
    private RecyclerView mRv;
    private RecyclerView mRvNeedPair;
    private MultiStateView mMsv;
    private SwitchButton mSbCast;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mRv = findViewById(R.id.rv);
        mRvNeedPair = findViewById(R.id.rv_need_pair);
        mMsv = findViewById(R.id.msv);

        mSbCast = findViewById(R.id.switch_button);
        mHandler = new Handler();
        initRv();
        initCastOnCreate();
        onActivityCreate();
        checkSwitchState();

    }

    public void onActivityCreate() {
        mStarted = true;
        registerDisplayChangedStatusBroadcast();
        setGlobalParams();
        update(CHANGE_ALL);
    }

    private CastDeviceAdapter mCastDeviceAdapter;
    private CastNeedPairDeviceAdapter mCastNeedPairDeviceAdapter;

    public void initRv() {
        // 已配对过的设备
        mRv.setLayoutManager(new LinearLayoutManager(this, RecyclerView.VERTICAL, false));
        mCastDeviceAdapter = new CastDeviceAdapter(R.layout.item_cast_device, null);
        mRv.setAdapter(mCastDeviceAdapter);


        // 未配对的设备
        mRvNeedPair.setLayoutManager(new LinearLayoutManager(this, RecyclerView.VERTICAL, false));
        mCastNeedPairDeviceAdapter = new CastNeedPairDeviceAdapter(R.layout.item_cast_device, null);
        mRvNeedPair.setAdapter(mCastNeedPairDeviceAdapter);


        mCastNeedPairDeviceAdapter.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(@NonNull BaseQuickAdapter<?, ?> adapter, @NonNull View view, int position) {
                LogUtil.i(TAG + "--mCastNeedPairDeviceAdapter  mCastNeedPairDeviceAdapter  onItemClick");
                WifiDisplay wifiDisplay = mWifiDisplayList.get(position);
                pairWifiDisplay(wifiDisplay);
            }
        });


        mCastDeviceAdapter.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(@NonNull BaseQuickAdapter<?, ?> adapter, @NonNull View view, int position) {
                // 已配对过的设备点击
                LogUtil.i(TAG + "--mCastDeviceAdapter  onItemClick");
                MediaRouter.RouteInfo routeInfo = mRouteInfoList.get(position);
                toggleRoute(routeInfo);
            }
        });

        mCastDeviceAdapter.setSettingsClickListener(new CastDeviceAdapter.SettingsClickListener() {
            @Override
            public void onSettingsClick(int position) {
                // 修改名字或者忘记
                MediaRouter.RouteInfo routeInfo = mRouteInfoList.get(position);
                showWifiDisplayOptionsDialog(routeInfo);
            }
        });


    }

    public void initCastOnCreate() {
        // 这个broadcast会在WifiDisplayAdapter里面当wifi display的状态发生改变时发送
        // 包括扫描到新的设备、开始连接、连接成功、断开等消息都会被这个receiver接收到
        // 然后类似WifiDisplayController一样，注册一些对数据库改变的ContentObserver
        mRouter = (MediaRouter) this.getSystemService(Context.MEDIA_ROUTER_SERVICE);
        mDisplayManager = (DisplayManager) this.getSystemService(Context.DISPLAY_SERVICE);
        mWifiP2pManager = (WifiP2pManager) this.getSystemService(Context.WIFI_P2P_SERVICE);
        mWifiP2pChannel = mWifiP2pManager.initialize(this, Looper.getMainLooper(), null);
    }

    public void registerDisplayChangedStatusBroadcast() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(DisplayManager.ACTION_WIFI_DISPLAY_STATUS_CHANGED);
        this.registerReceiver(mReceiver, filter);
    }

    public void setGlobalParams() {
        this.getContentResolver().registerContentObserver(Settings.Global.getUriFor(
                Settings.Global.WIFI_DISPLAY_ON), false, mSettingsObserver);
        this.getContentResolver().registerContentObserver(Settings.Global.getUriFor(
                Settings.Global.WIFI_DISPLAY_CERTIFICATION_ON), false, mSettingsObserver);
        this.getContentResolver().registerContentObserver(Settings.Global.getUriFor(
                Settings.Global.WIFI_DISPLAY_WPS_CONFIG), false, mSettingsObserver);
        mRouter.addCallback(MediaRouter.ROUTE_TYPE_REMOTE_DISPLAY, mRouterCallback,
                MediaRouter.CALLBACK_FLAG_PERFORM_ACTIVE_SCAN);
    }

    /**
     * 返回值
     */
    private final MediaRouter.Callback mRouterCallback = new MediaRouter.SimpleCallback() {
        @Override
        public void onRouteAdded(MediaRouter router, MediaRouter.RouteInfo info) {
            LogUtil.i(TAG + "--mRouterCallback  onRouteAdded");
            scheduleUpdate(CHANGE_ROUTES);
        }

        @Override
        public void onRouteChanged(MediaRouter router, MediaRouter.RouteInfo info) {
            LogUtil.i(TAG + "--mRouterCallback  onRouteChanged");
            scheduleUpdate(CHANGE_ROUTES);
        }

        @Override
        public void onRouteRemoved(MediaRouter router, MediaRouter.RouteInfo info) {
            LogUtil.i(TAG + "--mRouterCallback  onRouteRemoved");
            scheduleUpdate(CHANGE_ROUTES);
        }

        @Override
        public void onRouteSelected(MediaRouter router, int type, MediaRouter.RouteInfo info) {
            LogUtil.i(TAG + "--mRouterCallback  onRouteSelected");
            scheduleUpdate(CHANGE_ROUTES);
        }

        @Override
        public void onRouteUnselected(MediaRouter router, int type, MediaRouter.RouteInfo info) {
            LogUtil.i(TAG + "--mRouterCallback  onRouteUnselected");
            scheduleUpdate(CHANGE_ROUTES);
        }
    };


    private final ContentObserver mSettingsObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange, Uri uri) {
            scheduleUpdate(CHANGE_SETTINGS);
        }
    };


    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(DisplayManager.ACTION_WIFI_DISPLAY_STATUS_CHANGED)) {
                LogUtil.i(TAG + "--BroadcastReceiver   mReceiver ");
                scheduleUpdate(CHANGE_WIFI_DISPLAY_STATUS);
            }
        }
    };

    private void scheduleUpdate(int changes) {
        if (mStarted) {
            if (mPendingChanges == 0) {
                mHandler.post(mUpdateRunnable);
            }
            mPendingChanges |= changes;
        }
    }

    private final Runnable mUpdateRunnable = new Runnable() {
        @Override
        public void run() {
            final int changes = mPendingChanges;
            mPendingChanges = 0;
            update(changes);
        }
    };

    private void unscheduleUpdate() {
        if (mPendingChanges != 0) {
            mPendingChanges = 0;
            mHandler.removeCallbacks(mUpdateRunnable);
        }
    }

    private boolean mWifiDisplayCertificationOn;
    private int mWpsConfig = WpsInfo.INVALID;


    List<MediaRouter.RouteInfo> mRouteInfoList = new ArrayList<>();
    List<WifiDisplay> mWifiDisplayList = new ArrayList<>();

    private void update(int changes) {
        LogUtil.i(TAG + "--update  changes=" + changes);
        boolean invalidateOptions = false;

        clearAdapter();

        // Update settings.
        if ((changes & CHANGE_SETTINGS) != 0) {
            mWifiDisplayOnSetting = Settings.Global.getInt(this.getContentResolver(),
                    Settings.Global.WIFI_DISPLAY_ON, 0) != 0;
            mWifiDisplayCertificationOn = Settings.Global.getInt(this.getContentResolver(),
                    Settings.Global.WIFI_DISPLAY_CERTIFICATION_ON, 0) != 0;
            mWpsConfig = Settings.Global.getInt(this.getContentResolver(),
                    Settings.Global.WIFI_DISPLAY_WPS_CONFIG, WpsInfo.INVALID);

            // The wifi display enabled setting may have changed.
            // 启用wifi显示的设置可能已更改。
            invalidateOptions = true;
        }

        // Update wifi display state.
        if ((changes & CHANGE_WIFI_DISPLAY_STATUS) != 0) {
            mWifiDisplayStatus = mDisplayManager.getWifiDisplayStatus();
            // The wifi display feature state may have changed.
            // wifi显示功能状态可能已更改。
            invalidateOptions = true;
        }

        // Rebuild the routes.

        // Add all known remote display routes.
        // 这里获取所有设备。
        final int routeCount = mRouter.getRouteCount();
        LogUtil.d(TAG + "--update  routeCount=" + routeCount);
        mCastDeviceAdapter.getData().clear();
        for (int i = 0; i < routeCount; i++) {
            MediaRouter.RouteInfo route = mRouter.getRouteAt(i);
            if (route.matchesTypes(MediaRouter.ROUTE_TYPE_REMOTE_DISPLAY)) {
                // 这里拿到搜索到的设备
                // preferenceScreen.addPreference();
                mRouteInfoList.add(route);
                // createRoutePreference(route);
                LogUtil.i(TAG + "--update  已配对设备  device=" + route.getName());
                MultiStateUtils.toContent(mMsv);

                mCastDeviceAdapter.addData(route);
                mCastDeviceAdapter.notifyDataSetChanged();
            }
        }

        // Additional features for wifi display routes.
        // wifi显示路线的其他功能。
        mCastNeedPairDeviceAdapter.getData().clear();
        if (mWifiDisplayStatus != null
                && mWifiDisplayStatus.getFeatureState() == WifiDisplayStatus.FEATURE_STATE_ON) {
            // Add all unpaired wifi displays.
            // 添加所有没有配对的设备
            for (WifiDisplay display : mWifiDisplayStatus.getDisplays()) {
                if (!display.isRemembered() && display.isAvailable()
                        && !display.equals(mWifiDisplayStatus.getActiveDisplay())) {
//                    new UnpairedWifiDisplayPreference(
//                            _mActivity, display);
                    mWifiDisplayList.add(display);
                    // 没有配对设备
                    MultiStateUtils.toContent(mMsv);
                    mCastNeedPairDeviceAdapter.addData(display);
                    mCastNeedPairDeviceAdapter.notifyDataSetChanged();
                    LogUtil.i(TAG + "--update  displayName=" + display.getDeviceName());
                }
            }

            if (mWifiDisplayList.size() == 0 && mRouteInfoList.size() == 0) {
                MultiStateUtils.toEmpty(mMsv);
            }

            // Add the certification menu if enabled in developer options.
            // 如果在开发人员选项中启用，则添加认证菜单。
            LogUtil.i(TAG + "--update  mWifiDisplayCertificationOn=" + mWifiDisplayCertificationOn);
            if (mWifiDisplayCertificationOn) {
                //buildCertificationMenu(preferenceScreen);
            }
        }
        LogUtil.d(TAG + "--update  invalidateOptions=" + invalidateOptions);
        // Invalidate menu options if needed.
        if (invalidateOptions) {
            this.invalidateOptionsMenu();
        }
    }


    public void clearAdapter() {
        mRouteInfoList.clear();
        mWifiDisplayList.clear();
        mCastDeviceAdapter.getData().clear();
        mCastNeedPairDeviceAdapter.getData().clear();
        mCastDeviceAdapter.notifyDataSetChanged();
        mCastNeedPairDeviceAdapter.notifyDataSetChanged();
    }


    public void checkSwitchState() {
        mSbCast.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isOpen) {
                LogUtil.i(TAG + "--onCheckedChanged  isOpen=" + isOpen);
                switchStateChanged(isOpen);
            }
        });

        if (mWifiDisplayStatus != null && mWifiDisplayStatus.getFeatureState()
                != WifiDisplayStatus.FEATURE_STATE_UNAVAILABLE) {
            mSbCast.setEnabled(true);
            LogUtil.i(TAG + "--checkSwitchState   mWifiDisplayOnSetting=" + mWifiDisplayOnSetting);
            mSbCast.setChecked(mWifiDisplayOnSetting);
            if (!mWifiDisplayOnSetting) {
                MultiStateUtils.toEmpty(mMsv);
            } else {
                MultiStateUtils.toContent(mMsv);
            }
        }

    }

    public void switchStateChanged(boolean isChecked) {
        LogUtil.d(TAG + "--switchStateChanged  isChecked=" + isChecked);
        // 通过开关触发设备扫描
        mWifiDisplayOnSetting = isChecked;
        if (mWifiDisplayOnSetting) {
            MultiStateUtils.toContent(mMsv);
        } else {
            MultiStateUtils.toEmpty(mMsv);
        }
        // 开始扫描：1，关闭扫描：0
        Settings.Global.putInt(getContentResolver(),
                Settings.Global.WIFI_DISPLAY_ON, mWifiDisplayOnSetting ? 1 : 0);
    }

    // 设备配对
    private void pairWifiDisplay(WifiDisplay display) {
        boolean canConnect = display.canConnect();
        LogUtil.i(TAG + "--pairWifiDisplay  canConnect=" + canConnect + ",deviceName" + display.getDeviceName());
        if (canConnect) {
            mDisplayManager.connectWifiDisplay(display.getDeviceAddress());
        }
    }


    private WifiDisplay findWifiDisplay(String deviceAddress) {
        if (mWifiDisplayStatus != null && deviceAddress != null) {
            for (WifiDisplay display : mWifiDisplayStatus.getDisplays()) {
                if (display.getDeviceAddress().equals(deviceAddress)) {
                    return display;
                }
            }
        }
        return null;
    }


    private void toggleRoute(MediaRouter.RouteInfo route) {
        boolean selected = route.isSelected();
        LogUtil.i(TAG + "--toggleRoute  selected=" + selected + ",routeName=" + route.getName());
        if (selected) {
            MediaRouteDialogPresenter.showDialogFragment(this,
                    MediaRouter.ROUTE_TYPE_REMOTE_DISPLAY, null);
        } else {
            // 这里直接取消连接
            route.select();
        }
    }


    // 修改名字以及忘记弹框
    private void showWifiDisplayOptionsDialog(final MediaRouter.RouteInfo routeInfo) {

        WifiDisplay display = findWifiDisplay(routeInfo.getDeviceAddress());
        showWifiDisplayOptionsPopup(display.getFriendlyDisplayName(), new CustomInputHintPopup.Callback() {
            @Override
            public void cancel() {

            }

            @Override
            public void forget() {
                mDisplayManager.forgetWifiDisplay(display.getDeviceAddress());

            }
        });
//        showWifiDisplayOptionsPopup( display.getFriendlyDisplayName()
//                , new ReturnPopupCallback() {
//                    @Override
//                    public void onClick(Object o) {
//                        String name = (String) o;
//                        if (name.isEmpty() || name.equals(display.getDeviceName())) {
//                            name = null;
//                        }
//                        mDisplayManager.renameWifiDisplay(display.getDeviceAddress(), name);
//                    }
//
//                    @Override
//                    public void onDismiss() {
//                        // forget
//                    }
//                });
    }


    public void showWifiDisplayOptionsPopup(String message, CustomInputHintPopup.Callback callback) {

        CustomInputHintPopup customInputHintPopup = new CustomInputHintPopup(this, message
        );
        customInputHintPopup.setOnClick(callback);
        showCustomPopup(customInputHintPopup, true);
    }

    public void showCustomPopup(BasePopupView basePopupView, boolean canDismiss) {
        BasePopupView mPopupView = new XPopup.Builder(this)
                .hasShadowBg(false)
                .autoOpenSoftInput(true)
                .popupAnimation(PopupAnimation.NoAnimation)
                .moveUpToKeyboard(true)
                .autoFocusEditText(true)
                .isRequestFocus(true)
                .dismissOnBackPressed(canDismiss)
                .dismissOnTouchOutside(canDismiss)
                .asCustom(basePopupView);
        mPopupView.show();
    }
}