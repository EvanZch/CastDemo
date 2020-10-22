package com.example.castdemo;

import android.app.Application;

/**
 * @author : EvanZch
 * @date : 2020/10/22 11:14
 * description:
 **/
public class App extends Application {


    public static  App mContext;


    @Override
    public void onCreate() {
        super.onCreate();
        mContext = this;
    }


    public static App getInstance(){
        return mContext;
    }


}
