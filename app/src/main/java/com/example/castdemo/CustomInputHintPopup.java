package com.example.castdemo;

import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.lxj.xpopup.core.CenterPopupView;

/**
 * @author : EvanZch
 * @date : 2020/10/22 11:31
 * description:
 **/
public class CustomInputHintPopup extends CenterPopupView {




    private String mMessage;
    public CustomInputHintPopup(@NonNull Context context,String message) {
        super(context);
        mMessage = message;
    }




    public interface Callback{
        void cancel();
        void forget();
    }



    private Callback mCallback;
    public void setOnClick(Callback callback){
        this.mCallback = callback;
    }
    @Override
    protected void onCreate() {
        super.onCreate();

        TextView tvContent = findViewById(R.id.tv_content);

        Button btCancel = findViewById(R.id.bt_cancel);
        Button btForget = findViewById(R.id.bt_forget);

        tvContent.setText(mMessage);

        btCancel.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mCallback != null){
                    mCallback.cancel();
                }
            }
        });


        btForget.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mCallback != null){
                    mCallback.forget();
                }
            }
        });
    }

    @Override
    protected int getImplLayoutId() {
        return R.layout.popup_input_dialog;
    }
}
