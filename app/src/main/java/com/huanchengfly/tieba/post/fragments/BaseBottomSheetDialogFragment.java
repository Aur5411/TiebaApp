package com.huanchengfly.tieba.post.fragments;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.viewbinding.ViewBinding;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.huanchengfly.tieba.post.R;

public abstract class BaseBottomSheetDialogFragment extends BottomSheetDialogFragment {
    public static final String TAG = "BaseBottomSheetDialog";
    protected BottomSheetDialog dialog;
    protected BottomSheetBehavior mBehavior;
    View rootView;
    private ViewBinding binding;
    private Context attachContext;

    public BaseBottomSheetDialogFragment() {
    }

    @TargetApi(23)
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        onAttachToContext(context);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            onAttachToContext(activity);
        }
    }

    @CallSuper
    private void onAttachToContext(Context context) {
        attachContext = context;
    }

    @NonNull
    protected Context getAttachContext() {
        return attachContext;
    }

    protected int getScreenHeight() {
        return getAttachContext().getResources().getDisplayMetrics().heightPixels;
    }

    protected int getStatusBarHeight() {
        int statusBarHeight = 0;
        Resources resources = getAttachContext().getResources();
        int resourceId = resources.getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0)
            statusBarHeight = resources.getDimensionPixelSize(resourceId);
        return statusBarHeight;
    }

    protected boolean isFullScreen() {
        return false;
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            View bottomSheet = dialog.findViewById(R.id.design_bottom_sheet);
            bottomSheet.getLayoutParams().height = isFullScreen() ? ViewGroup.LayoutParams.MATCH_PARENT : ViewGroup.LayoutParams.WRAP_CONTENT;
        }
        final View view = getView();
        if (view != null) {
            view.post(() -> {
                View parent = (View) view.getParent();
                CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) parent.getLayoutParams();
                CoordinatorLayout.Behavior behavior = params.getBehavior();
                BottomSheetBehavior bottomSheetBehavior = (BottomSheetBehavior) behavior;
                if (bottomSheetBehavior != null)
                    bottomSheetBehavior.setPeekHeight(view.getMeasuredHeight());
            });
        }
    }

    protected abstract void initView();

    public void resetView() {
    }

    public boolean isShowing() {
        return dialog != null && dialog.isShowing();
    }

    /**
     * 使用关闭弹框 是否使用动画可选
     * 使用动画 同时切换界面Aty会卡顿 建议直接关闭
     */
    public void close() {
        dismiss();
    }

    protected void onCreatedBehavior(BottomSheetBehavior<?> behavior) {
    }

    /**
     * 子类如果需要使用 ViewBinding，应在此方法调用前（如 onCreate 中）调用 setBinding
     */
    protected void setBinding(@NonNull ViewBinding binding) {
        this.binding = binding;
    }

    /**
     * 当未通过 setBinding 设置 ViewBinding 时，子类需覆盖此方法返回内容视图
     * 默认实现返回 null，此时会抛出异常
     */
    @Nullable
    protected View onCreateContentView(@NonNull LayoutInflater inflater) {
        return null;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        dialog = new BottomSheetDialog(getAttachContext(), R.style.BottomSheetDialogStyle);
        mBehavior = dialog.getBehavior();
        mBehavior.setHideable(true);
        onCreatedBehavior(mBehavior);

        // 获取内容视图：优先使用 binding，否则调用 onCreateContentView
        View contentView = null;
        if (binding != null) {
            rootView = binding.getRoot();
            contentView = rootView;
        } else {
            LayoutInflater inflater = LayoutInflater.from(getAttachContext());
            View customView = onCreateContentView(inflater);
            if (customView != null) {
                rootView = customView;
                contentView = rootView;
            }
        }

        if (contentView == null) {
            throw new IllegalStateException("You must either call setBinding(ViewBinding) or override onCreateContentView() to provide the dialog content.");
        }
        dialog.setContentView(contentView);

        if (dialog.getWindow() != null) {
            if (needFixHeight())
                dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, getHeight());
            dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            ((View) rootView.getParent()).setBackgroundColor(Color.TRANSPARENT);
            dialog.getWindow().findViewById(R.id.design_bottom_sheet).setBackgroundColor(Color.TRANSPARENT);
        }
        initView();
        return dialog;
    }

    protected int getHeight() {
        int screenHeight = getScreenHeight();
        int statusBarHeight = getStatusBarHeight();
        int dialogHeight = screenHeight - statusBarHeight;
        return dialogHeight == 0 ? ViewGroup.LayoutParams.MATCH_PARENT : dialogHeight;
    }

    protected boolean needFixHeight() {
        return true;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        rootView = null;
    }

    // 方便子类获取 binding（可能为 null）
    @Nullable
    protected ViewBinding getBinding() {
        return binding;
    }
}