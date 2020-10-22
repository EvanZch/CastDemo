package com.example.castdemo;

import android.view.View;

import com.kennyc.view.MultiStateView;


public class MultiStateUtils {

    public static void toLoading(MultiStateView view) {
        view.setViewState(MultiStateView.VIEW_STATE_LOADING);
    }

    public static void toEmpty(MultiStateView view) {
        view.setViewState(MultiStateView.VIEW_STATE_EMPTY);
    }

    public static void toError(MultiStateView view) {
        view.setViewState(MultiStateView.VIEW_STATE_ERROR);
    }

    public static void toContent(MultiStateView view) {
        view.setViewState(MultiStateView.VIEW_STATE_CONTENT);
    }


    public interface SimpleListener {
        void onResult();
    }

    public static void setEmptyAndErrorClick(MultiStateView view, SimpleListener listener) {
        setEmptyClick(view, listener);
        setErrorClick(view, listener);
    }

    public static void setEmptyClick(MultiStateView view, SimpleListener listener) {
        View empty = view.getView(MultiStateView.VIEW_STATE_EMPTY);
        if (empty != null) {
            empty.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listener.onResult();
                }
            });
        }
    }

    public static void setErrorClick(MultiStateView view, SimpleListener listener) {
        View error = view.getView(MultiStateView.VIEW_STATE_ERROR);
        if (error != null) {
            error.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listener.onResult();
                }
            });
        }
    }
}
