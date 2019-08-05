package com.example.mmat;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;

import com.bumptech.glide.Glide;

import java.util.LinkedList;
import java.util.List;

/**
 * 产生内存泄漏的页面
 */
public class MemoryLeakActivity extends AppCompatActivity {

    private static List<Activity> sActivityLeaked = new LinkedList<>() ;
    /**
     * 模拟Activity 持有2MB的内存占用
     */
    private byte[] mBigHeapRetained = new byte[2 * 1024 * 1024] ;

    public static void start(Context context) {
        Intent starter = new Intent(context, MemoryLeakActivity.class);
        context.startActivity(starter);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leak);

        // fixme: 这里会持有Activity 引用 (为报告简洁, 这里只模拟5个实例的泄漏), 在页面退出后会造成内存泄漏
        if ( sActivityLeaked.size() < 5 ) {
            sActivityLeaked.add(this) ;
        }

        // 点击关闭页面
        findViewById(R.id.sample_text).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        loadImages();
    }

    private void loadImages() {
        ImageView imageView = findViewById(R.id.imageview) ;
        Glide.with(getApplicationContext()).load("https://user-gold-cdn.xitu.io/2019/6/4/16b1fff4d46432a4?imageView2/0/w/1280/h/960/format/webp/ignore-error/1").into(imageView) ;
        Glide.with(getApplicationContext()).load("https://user-gold-cdn.xitu.io/2019/6/4/16b204afdb9bd1b3?imageView2/0/w/1280/h/960/format/webp/ignore-error/1").into(imageView) ;
        Glide.with(getApplicationContext()).load("https://user-gold-cdn.xitu.io/2019/6/4/16b204afdab91aa4?imageView2/0/w/1280/h/960/format/webp/ignore-error/1").into(imageView) ;
    }
}
