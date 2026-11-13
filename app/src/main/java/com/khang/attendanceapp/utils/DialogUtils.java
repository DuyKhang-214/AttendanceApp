package com.khang.attendanceapp.utils;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;

import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.khang.attendanceapp.R;

/** Các hộp thoại dùng lại nhiều nơi. */
public final class DialogUtils {

    public interface OnTextEntered {
        void onEntered(String text);
    }

    private DialogUtils() {
    }

    /** Hộp thoại nhập 1 dòng văn bản (không cho phép để trống). */
    public static void showInput(Context ctx, @StringRes int title, @StringRes int hint,
                                 String current, final OnTextEntered callback) {
        View view = LayoutInflater.from(ctx).inflate(R.layout.dialog_input, null);
        final TextInputLayout til = view.findViewById(R.id.tilInput);
        final TextInputEditText et = view.findViewById(R.id.etInput);
        til.setHint(ctx.getString(hint));
        et.setText(current);
        if (current != null) et.setSelection(current.length());

        final AlertDialog dialog = new MaterialAlertDialogBuilder(ctx)
                .setTitle(title)
                .setView(view)
                .setNegativeButton(R.string.btn_cancel, null)
                .setPositiveButton(R.string.btn_ok, null)
                .create();
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        dialog.show();

        final Runnable submit = new Runnable() {
            @Override
            public void run() {
                String text = et.getText() == null ? "" : et.getText().toString().trim();
                if (text.isEmpty()) {
                    til.setError(til.getContext().getString(R.string.error_required));
                    return;
                }
                dialog.dismiss();
                callback.onEntered(text);
            }
        };
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> submit.run());
        et.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                submit.run();
                return true;
            }
            return false;
        });
    }

    /** Hộp thoại xác nhận (dùng cho thao tác xoá). */
    public static void confirm(Context ctx, @StringRes int title, String message,
                               @StringRes int positive, final Runnable onConfirm) {
        new MaterialAlertDialogBuilder(ctx)
                .setTitle(title)
                .setMessage(message)
                .setNegativeButton(R.string.btn_cancel, null)
                .setPositiveButton(positive, (d, w) -> onConfirm.run())
                .show();
    }
}
