package com.mariostudio.rotatedrawabledemo;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;

import java.io.IOException;

public class MainActivity extends Activity implements View.OnClickListener, MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener{

    private final int Duration = 600;  // 动画时长

    private AnimatorState state = AnimatorState.State_Stop;  //动画状态
    private AudioState audioState = AudioState.STATE_STOP;   //音乐播放器状态

    private ImageView btnPre,btnPlay, btnNext;
    private ImageView cdBox, handerd;

    private MediaPlayer mediaPlayer;

    private boolean flag = false;  //标记，控制唱片旋转

    private String names[] = {"demo01.mp3", "demo02.mp3"};
    private int position = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //判断SDK版本是否大于等于19，大于就让他显示，小于就要隐藏，不然低版本会多出来一个
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            setTranslucentStatus(true);
            //还有设置View的高度，因为每个型号的手机状态栏高度都不相同
        }
        initAllViews();
    }

    private void prepareMusic() {
        //从Assets中获取音频资源
        cdBox.getDrawable().setLevel(0);
        AssetFileDescriptor fileDescriptor;
        try {
            fileDescriptor = this.getAssets().openFd(names[position]);
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setOnPreparedListener(this);
            mediaPlayer.setOnCompletionListener(this);
            mediaPlayer.setDataSource(fileDescriptor.getFileDescriptor(), fileDescriptor.getStartOffset(), fileDescriptor.getLength());
            mediaPlayer.prepareAsync();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @TargetApi(19)
    private void setTranslucentStatus(boolean on) {
        Window win = getWindow();
        WindowManager.LayoutParams winParams = win.getAttributes();
        final int bits = WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS;
        if (on) {
            winParams.flags |= bits;
        } else {
            winParams.flags &= ~bits;
        }
        win.setAttributes(winParams);
    }

    private void initAllViews() {
        cdBox  = (ImageView) findViewById(android.R.id.progress);
        handerd = (ImageView) findViewById(android.R.id.background);

        btnPre = (ImageView) findViewById(android.R.id.button1);
        btnPlay = (ImageView) findViewById(android.R.id.button2);
        btnNext = (ImageView) findViewById(android.R.id.button3);
        btnPre.setOnClickListener(this);
        btnPlay.setOnClickListener(this);
        btnNext.setOnClickListener(this);

        new MyThread().start();
    }

    @Override
    public void onClick(View view) {
        // 一切按键都必须在动画完成的状态下才能被触发
        if(state == AnimatorState.State_Stop) {
            switch (view.getId()) {
                case android.R.id.button1:
                    flag = false;
                    stop(-1);
                    break;
                case android.R.id.button2:
                    if(audioState != AudioState.STATE_PLAYING) {
                        if(audioState == AudioState.STATE_STOP) {
                            prepareMusic();
                        } else {
                            start();
                        }
                    } else {
                        pause();
                    }
                    break;
                case android.R.id.button3:
                    flag = false;
                    stop(1);
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * 开始动画
     * */
    private void start() {
        ValueAnimator animator = ValueAnimator.ofInt(0, 10000);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                int level = (int) animation.getAnimatedValue();
                handerd.getDrawable().setLevel(level);
            }
        });

        animator.addListener(new AnimatorListenerAdapter() {

            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                state = AnimatorState.State_Playing;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                state = AnimatorState.State_Stop;
                audioStart();
            }
        });
        animator.setDuration(Duration);
        animator.start();
    }

    private void setAudioState(AudioState audioState) {
        this.audioState = audioState;
        if(audioState == AudioState.STATE_PLAYING) {
            btnPlay.setImageResource(R.drawable.selector_pause);
        } else {
            btnPlay.setImageResource(R.drawable.selector_play);
        }
    }

    /**
     * 暂停动画
     * */
    private void pause() {
        ValueAnimator animator01 = ValueAnimator.ofInt(10000, 0);
        animator01.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                int level = (int) animation.getAnimatedValue();
                handerd.getDrawable().setLevel(level);
            }
        });

        animator01.addListener(new AnimatorListenerAdapter() {

            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                state = AnimatorState.State_Playing;
                audioPause();
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                state = AnimatorState.State_Stop;
            }
        });
        animator01.setDuration(Duration);
        animator01.start();
    }

    /**
     * 停止动画 ， 主要用于切歌
     * */
    private void stop(final int type) {
        if(audioState == AudioState.STATE_PLAYING) {
            ValueAnimator animator01 = ValueAnimator.ofInt(10000, 0);
            animator01.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    int level = (int) animation.getAnimatedValue();
                    handerd.getDrawable().setLevel(level);
                }
            });

            animator01.addListener(new AnimatorListenerAdapter() {

                @Override
                public void onAnimationStart(Animator animation) {
                    super.onAnimationStart(animation);
                    audioStop();
                    cdBox.getDrawable().setLevel(0);
                    state = AnimatorState.State_Playing;
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    state = AnimatorState.State_Stop;
                    switch (type) {
                        case -1:
                            audioPrevious();
                            break;
                        case 0:
                            audioStop();
                            break;
                        case 1:
                            audioNext();
                            break;
                    }
                }
            });
            animator01.setDuration(Duration);
            animator01.start();
        } else {
            audioStop();
            switch (type) {
                case -1:
                    audioPrevious();
                    break;
                case 0:
                    audioStop();
                    break;
                case 1:
                    audioNext();
                    break;
            }
        }
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        stop(1);
    }

    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
        setAudioState(AudioState.STATE_PAUSE);
        start();
    }

    /**
     * 动画状态
     * */
    public enum AnimatorState {
        State_Stop,
        State_Playing
    }

    /**
     * 音乐停止
     * */
    private void audioStop() {
        if(null != mediaPlayer && audioState != AudioState.STATE_STOP) {
            setAudioState(AudioState.STATE_STOP);
            mediaPlayer.stop();
            flag = false;
        }
    }

    /**
     * 音乐暂停
     * */
    private void audioPause() {
        if(mediaPlayer != null && audioState == AudioState.STATE_PLAYING) {
            setAudioState(AudioState.STATE_PAUSE);
            mediaPlayer.pause();
            flag = false;
        }
    }

    /**
     * 音乐播放
     * */
    private void audioStart() {
        if(mediaPlayer != null && (audioState == AudioState.STATE_PAUSE || audioState == AudioState.STATE_PREPARE)) {
            setAudioState(AudioState.STATE_PLAYING);
            mediaPlayer.start();
            flag = true;
        } else {
            if(mediaPlayer == null) {
                prepareMusic();
            }
        }
    }

    /**
     * 上一首
     * */
    private void audioPrevious() {
        if(audioState == AudioState.STATE_STOP) {
            position --;
            if(position < 0) {
                position = names.length - 1;
            }
            if(mediaPlayer != null) {
                mediaPlayer.release();
                mediaPlayer = null;
            }
            prepareMusic();
        }
    }

    /**
     * 下一首
     * */
    private void audioNext() {
        if(audioState == AudioState.STATE_STOP) {
            position ++;
            if(position >= names.length) {
                position = 0;
            }
            if(mediaPlayer != null) {
                mediaPlayer.release();
                mediaPlayer = null;
            }
            prepareMusic();
        }
    }

    public enum AudioState {
        STATE_STOP,
        STATE_PAUSE,
        STATE_PREPARE,
        STATE_PLAYING
    }

    class MyThread extends Thread {

        @Override
        public void run() {
            super.run();
            try {
                while(true) {
                    Thread.sleep(50);
                    if(flag) {
                        //只有在flag==true的情况下才会对唱片进行旋转操作
                        handler.sendMessage(new Message());
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    Handler handler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            int level = cdBox.getDrawable().getLevel();
            level = level + 200;
            if(level > 10000) {
                level = level - 10000;
            }
            cdBox.getDrawable().setLevel(level);
        }
    };
}
