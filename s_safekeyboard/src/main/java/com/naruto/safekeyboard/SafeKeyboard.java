package com.naruto.safekeyboard;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.Editable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.fragment.app.Fragment;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Created by Administrator on 2018/3/7 0007.
 */

public class SafeKeyboard {
    private static final String TAG = "SafeKeyboard";
    private Context mContext;               //上下文
    private ViewGroup layout;

    private View keyContainer;              //自定义键盘的容器View
    private LinearLayout keyboardShower;
    private SafeKeyboardView keyboardView;  //键盘的View
    private Keyboard keyboardNumber;        //数字键盘

    private int keyboardType = 1;
    private EditText mEditText;

    @SuppressLint("ClickableViewAccessibility")
    public SafeKeyboard(Activity activity) {
        this.mContext = activity.getApplicationContext();
        this.layout = activity.getWindow().getDecorView().findViewById(android.R.id.content);
        keyContainer = LayoutInflater.from(mContext).inflate(R.layout.layout_keyboard_containor, layout, true);
        keyboardNumber = new Keyboard(mContext, R.xml.keyboard_num);            //实例化数字键盘
        keyboardShower = keyContainer.findViewById(R.id.keyboard_shower);
                // 由于符号键盘与字母键盘共用一个KeyBoardView, 所以不需要再为符号键盘单独实例化一个KeyBoardView
        keyboardView = keyContainer.findViewById(R.id.safeKeyboardLetter);
        keyboardView.setDelDrawable(mContext.getResources().getDrawable(R.drawable.icon_del));
        keyboardView.setEnabled(true);
        keyboardView.setPreviewEnabled(false);

        keyboardShower.setVisibility(View.GONE);
        initEvent();
    }
    public void regEditText(EditText edit){
        edit.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    SafeKeyboard.this.mEditText = (EditText) v;
                    hideSystemKeyBoard((EditText) v);
                    showKeyboard();
                }
                return false;
            }
        });
        edit.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (v instanceof EditText) {
                    if (hasFocus) {
//                        SafeKeyboard.this.mEditText = (EditText) v;
//                        hideSystemKeyBoard((EditText) v);
//                        showKeyboard();
                    }else{
                        hideSystemKeyBoard((EditText) v);
                        hideKeyboard();
                    }
                }
            }
        });
    }
    private void initEvent() {
        keyboardView.setOnKeyboardActionListener(listener);
        FrameLayout done = keyContainer.findViewById(R.id.keyboardDone);
        done.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isKeyboardShown()) {
                    hideKeyboard();
                }
            }
        });
        keyboardView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return event.getAction() == MotionEvent.ACTION_MOVE;
            }
        });
        keyContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e("KeyBoard","Click board");
            }
        });
    }

    // 设置键盘点击监听
    private KeyboardView.OnKeyboardActionListener listener = new KeyboardView.OnKeyboardActionListener() {

        @Override
        public void onPress(int primaryCode) {
            if (keyboardType == 3) {
                keyboardView.setPreviewEnabled(false);
            } else {
                keyboardView.setPreviewEnabled(true);
                if (primaryCode == -5 ||primaryCode == -2 || primaryCode == -35) {
                    keyboardView.setPreviewEnabled(false);
                } else {
                    keyboardView.setPreviewEnabled(true);
                }
            }
        }

        @Override
        public void onRelease(int primaryCode) {
        }

        @Override
        public void onKey(int primaryCode, int[] keyCodes) {
            try {
                Editable editable = mEditText.getText();
                int start = mEditText.getSelectionStart();
                int end = mEditText.getSelectionEnd();
                if (primaryCode == Keyboard.KEYCODE_CANCEL) {
                    // 隐藏键盘
                    hideKeyboard();
                } else if (primaryCode == -2) {//KEYCODE_MODE_CHANGE
                    // 回退键
                    if (editable != null && editable.length() > 0) {
                        if (start == end) { //光标开始和结束位置相同, 即没有选中内容
                            editable.delete(start - 1, start);
                        } else { //光标开始和结束位置不同, 即选中EditText中的内容
                            editable.delete(start, end);
                        }
                    }
                }else if (primaryCode == Keyboard.KEYCODE_DELETE || primaryCode == -35) {
                    // 回退键,删除字符,定义为全删
                    editable.clear();
                } else if (primaryCode == Keyboard.KEYCODE_SHIFT) {
                    // 大小写切换
//                    changeKeyboardLetterCase();
                    // 重新setKeyboard, 进而系统重新加载, 键盘内容才会变化(切换大小写)
                    keyboardType = 1;
                    switchKeyboard();
                } else if (primaryCode == Keyboard.KEYCODE_MODE_CHANGE) {
                    // 数字与字母键盘互换
                    if (keyboardType == 3) { //当前为数字键盘
                        keyboardType = 1;
                    } else {        //当前不是数字键盘
                        keyboardType = 3;
                    }
                    switchKeyboard();
                } else if (primaryCode == 100860) {
                    // 字母与符号切换
                    if (keyboardType == 2) { //当前是符号键盘
                        keyboardType = 1;
                    } else {        //当前不是符号键盘, 那么切换到符号键盘
                        keyboardType = 2;
                    }
                    switchKeyboard();
                } else {
                    // 输入键盘值
                    // editable.insert(start, Character.toString((char) primaryCode));
                    editable.replace(start, end, Character.toString((char) primaryCode));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onText(CharSequence text) {

        }

        @Override
        public void swipeLeft() {
        }

        @Override
        public void swipeRight() {
        }

        @Override
        public void swipeDown() {
        }

        @Override
        public void swipeUp() {
        }
    };
    private void switchKeyboard() {
        switch (keyboardType) {
            case 3:
                keyboardView.setKeyboard(keyboardNumber);
                break;
            default:
                Log.e(TAG, "ERROR keyboard type");
                break;
        }
    }


    //隐藏系统键盘关键代码
    private static void hideSystemKeyBoard(EditText edit) {
//        this.mEditText = edit;
        InputMethodManager imm = (InputMethodManager) edit.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm == null)
            return;
        boolean isOpen = imm.isActive();
        if (isOpen) {
            imm.hideSoftInputFromWindow(edit.getWindowToken(), 0);
        }

        int currentVersion = Build.VERSION.SDK_INT;
        String methodName = null;
        if (currentVersion >= 16) {
            methodName = "setShowSoftInputOnFocus";
        } else if (currentVersion >= 14) {
            methodName = "setSoftInputShownOnFocus";
        }

        if (methodName == null) {
            edit.setInputType(0);
        } else {
            try {
                Method setShowSoftInputOnFocus = EditText.class.getMethod(methodName, Boolean.TYPE);
                setShowSoftInputOnFocus.setAccessible(true);
                setShowSoftInputOnFocus.invoke(edit, Boolean.FALSE);
            } catch (NoSuchMethodException e) {
                edit.setInputType(0);
                e.printStackTrace();
            } catch (IllegalAccessException | InvocationTargetException | IllegalArgumentException e) {
                e.printStackTrace();
            }
        }
    }
//====================================clip=========================================
    private boolean isKeyboardShown() {
        return keyboardShower.getVisibility() == View.VISIBLE;
    }

    public void hideKeyboard() {
        keyboardShower.setVisibility(View.GONE);
    }
    private void showKeyboard() {
        keyboardView.setKeyboard(keyboardNumber);
//        layout.removeView(keyContainer);
//        layout.addView(keyContainer);
        if(keyboardShower.getVisibility()!=View.VISIBLE)keyboardShower.setVisibility(View.VISIBLE);
    }


}
