package com.mrcd.mmat.analyzer.trace;

import java.lang.ref.PhantomReference;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;

import static com.mrcd.mmat.android.AndroidOS.MANUFACTURER;
import static com.mrcd.mmat.android.AndroidOS.SDK_INT;
import static com.mrcd.mmat.android.AndroidVersions.ICE_CREAM_SANDWICH;
import static com.mrcd.mmat.android.AndroidVersions.ICE_CREAM_SANDWICH_MR1;
import static com.mrcd.mmat.android.AndroidVersions.JELLY_BEAN;
import static com.mrcd.mmat.android.AndroidVersions.KITKAT;
import static com.mrcd.mmat.android.AndroidVersions.LOLLIPOP;
import static com.mrcd.mmat.android.AndroidVersions.LOLLIPOP_MR1;
import static com.mrcd.mmat.android.AndroidVersions.M;
import static com.mrcd.mmat.android.AndroidVersions.N;
import static com.mrcd.mmat.android.AndroidVersions.N_MR1;
import static com.mrcd.mmat.android.AndroidVersions.O;
import static com.mrcd.mmat.android.AndroidVersions.O_MR1;
import static com.mrcd.mmat.android.AndroidVersions.P;

/**
 * see : https://github.com/square/leakcanary/blob/v2.0-alpha-2/leakcanary-android-core/src/main/java/leakcanary/AndroidExcludedRefs.kt
 */
public class AndroidExcludeRefs {

    public static final String SAMSUNG = "samsung";
    public static final String MOTOROLA = "motorola";
    public static final String LENOVO = "LENOVO";
    public static final String LG = "LGE";
    public static final String NVIDIA = "NVIDIA";
    public static final String MEIZU = "Meizu";
    public static final String HUAWEI = "HUAWEI";
    public static final String VIVO = "vivo";

    private AndroidExcludeRefs() {
    }

    public static void buildSystemExcludeRefs(ExcludedRefs.Builder excluded) {
        //  exclude invalid references
        excludeInvalidReference(excluded);

        if (SDK_INT >= KITKAT && SDK_INT <= LOLLIPOP) {
            // Android AOSP sometimes keeps a reference to a destroyed activity as a "nextIdle" client
            // record in the android.app.ActivityThread.mActivities map.
            // Not sure what's going on there, input welcome.
            excluded.instanceField("android.app.ActivityThread$ActivityClientRecord", "nextIdle");
        }

        if (SDK_INT <= KITKAT) {
            // Editor inserts a special span, which has a reference to the EditText. That span is a
            // NoCopySpan, which makes sure it gets dropped when creating a new SpannableStringBuilder
            // from a given CharSequence.
            // TextView.onSaveInstanceState() does a copy of its mText before saving it in the bundle.
            // Prior to KitKat, that copy was done using the SpannableString constructor, instead of
            // SpannableStringBuilder. The SpannableString constructor does not drop NoCopySpan spans.
            // So we end up with a saved state that holds a reference to the textview and therefore the
            // entire view hierarchy & activity context.
            // Fix: https://github.com/android/platform_frameworks_base/commit
            // /af7dcdf35a37d7a7dbaad7d9869c1c91bce2272b

            // Hack: to fix this, you could override TextView.onSaveInstanceState(), and then use
            // reflection to access TextView.SavedState.mText and clear the NoCopySpan spans.
            excluded.instanceField("android.widget.Editor$EasyEditSpanController", "this$0");
            excluded.instanceField("android.widget.Editor$SpanController", "this$0");
        }

        if (SDK_INT == LOLLIPOP) {
            // MediaSessionLegacyHelper is a static singleton that is lazily instantiated and keeps a
            // reference to the context it's given the first time MediaSessionLegacyHelper.getHelper()
            // is called.
            // This leak was introduced in android-5.0.1_r1 and fixed in Android 5.1.0_r1 by calling
            // context.getApplicationContext().
            // Fix: https://github.com/android/platform_frameworks_base/commit
            // /9b5257c9c99c4cb541d8e8e78fb04f008b1a9091

            // Hack: to fix this, you could call MediaSessionLegacyHelper.getHelper() early in
            // Application.onCreate() and pass it the application context.
            excluded.staticField("android.media.session.MediaSessionLegacyHelper", "sInstance");
        }

        if (SDK_INT < LOLLIPOP_MR1) {
            // TextLine.sCached is a pool of 3 TextLine instances. TextLine.recycle() has had at least two
            // bugs that created memory leaks by not correctly clearing the recycled TextLine instances.
            // The first was fixed in android-5.1.0_r1:
            // https://github.com/android/platform_frameworks_base/commit
            // /893d6fe48d37f71e683f722457bea646994a10bf

            // The second was fixed, not released yet:
            // https://github.com/android/platform_frameworks_base/commit
            // /b3a9bc038d3a218b1dbdf7b5668e3d6c12be5ee4

            // Hack: to fix this, you could access TextLine.sCached and clear the pool every now and then
            // (e.g. on activity destroy).
            excluded.staticField("android.text.TextLine", "sCached");
        }

        if (SDK_INT < LOLLIPOP) {
            // Prior to ART, a thread waiting on a blocking queue will leak the last dequeued object
            // as a stack local reference.
            // So when a HandlerThread becomes idle, it keeps a local reference to the last message it
            // received. That message then gets recycled and can be used again.
            // As long as all messages are recycled after being used, this won't be a problem, because
            // there references are cleared when being recycled.
            // However, dialogs create template Message instances to be copied when a message needs to be
            // sent. These Message templates holds references to the dialog listeners, which most likely
            // leads to holding a reference onto the activity in some way. Dialogs never recycle their
            // template Message, assuming these Message instances will get GCed when the dialog is GCed.
            // The combination of these two things creates a high potential for memory leaks as soon
            // as you use dialogs. These memory leaks might be temporary, but some handler threads sleep
            // for a long time.

            // Hack: to fix this, you could post empty messages to the idle handler threads from time to
            // time. This won't be easy because you cannot access all handler threads, but a library
            // that is widely used should consider doing this for its own handler threads.
            excluded.instanceField("android.os.Message", "obj");
            excluded.instanceField("android.os.Message", "next");
            excluded.instanceField("android.os.Message", "target");
        }

        if (SDK_INT >= KITKAT && SDK_INT <= LOLLIPOP_MR1) {
            // When we detach a view that receives keyboard input, the InputMethodManager leaks a
            // reference to it until a new view asks for keyboard input.
            // Tracked here: https://code.google.com/p/android/issues/detail?id=171190
            // Hack: https://gist.github.com/pyricau/4df64341cc978a7de414
            excluded.instanceField("android.view.inputmethod.InputMethodManager", "mNextServedView");
            excluded.instanceField("android.view.inputmethod.InputMethodManager", "mServedView");
            excluded.instanceField("android.view.inputmethod.InputMethodManager",
                    "mServedInputConnection");
        }

        if (SDK_INT >= ICE_CREAM_SANDWICH_MR1 && SDK_INT <= LOLLIPOP_MR1) {
            // The singleton InputMethodManager is holding a reference to mCurRootView long after the
            // activity has been destroyed.
            // Observed on ICS MR1: https://github.com/square/leakcanary/issues/1#issuecomment-100579429
            // Hack: https://gist.github.com/pyricau/4df64341cc978a7de414
            excluded.instanceField("android.view.inputmethod.InputMethodManager", "mCurRootView");
        }

        if (SDK_INT >= ICE_CREAM_SANDWICH && SDK_INT <= LOLLIPOP_MR1) {
            // LayoutTransition leaks parent ViewGroup through ViewTreeObserver.OnPreDrawListener
            // When triggered, this leaks stays until the window is destroyed.
            // Tracked here: https://code.google.com/p/android/issues/detail?id=171830
            excluded.instanceField("android.animation.LayoutTransition$1", "val$parent");
        }

        if (SDK_INT >= JELLY_BEAN || SDK_INT <= LOLLIPOP_MR1) {
            // SpellCheckerSessionListenerImpl.mHandler is leaking destroyed Activity when the
            // SpellCheckerSession is closed before the service is connected.
            // Tracked here: https://code.google.com/p/android/issues/detail?id=172542
            excluded.instanceField("android.view.textservice.SpellCheckerSession$1", "this$0");
        }

        if (MOTOROLA.equals(MANUFACTURER) && SDK_INT == KITKAT) {
            // DevicePolicyManager keeps a reference to the context it has been created with instead of
            // extracting the application context. In this Motorola build, DevicePolicyManager has an
            // inner SettingsObserver class that is a content observer, which is held into memory
            // by a binder transport object.
            excluded.instanceField("android.app.admin.DevicePolicyManager$SettingsObserver", "this$0");
        }

        if (SAMSUNG.equals(MANUFACTURER) && SDK_INT == KITKAT) {
            // SpenGestureManager has a static mContext field that leaks a reference to the activity.
            // Yes, a STATIC "mContext" field.
            excluded.staticField("com.samsung.android.smartclip.SpenGestureManager", "mContext");
        }

        if (SAMSUNG.equals(MANUFACTURER) && SDK_INT >= KITKAT && SDK_INT <= LOLLIPOP) {
            // ClipboardUIManager is a static singleton that leaks an activity context.
            excluded.staticField("android.sec.clipboard.ClipboardUIManager", "sInstance");
        }

        if (LG.equals(MANUFACTURER) && SDK_INT >= KITKAT && SDK_INT <= LOLLIPOP) {
            // A static helper for EditText "bubble popups" leaks a reference to the latest focused view.
            excluded.staticField("android.widget.BubblePopupHelper", "sHelper");
        }

        if (SAMSUNG.equals(MANUFACTURER) && SDK_INT == KITKAT) {
            // AwResource#setResources() is called with resources that hold a reference to the
            // activity context (instead of the application context) and doesn't clear it.
            // Not sure what's going on there, input welcome.
            excluded.staticField("com.android.org.chromium.android_webview.AwResource", "sResources");
        }

        if (NVIDIA.equals(MANUFACTURER) && SDK_INT == KITKAT) {
            // Not sure exactly what ControllerMapper is about, but there is an anonymous Handler in
            // ControllerMapper.MapperClient.ServiceClient, which leaks ControllerMapper.MapperClient
            // which leaks the activity context.
            excluded.instanceField("com.nvidia.ControllerMapper.MapperClient$ServiceClient", "this$0");
        }

        if (SAMSUNG.equals(MANUFACTURER) && SDK_INT == KITKAT) {
            // mLastHoveredView is a static field in TextView that leaks the last hovered view.
            excluded.staticField("android.widget.TextView", "mLastHoveredView");
        }

        if (SAMSUNG.equals(MANUFACTURER) && SDK_INT == KITKAT) {
            // android.app.LoadedApk.mResources has a reference to
            // android.content.res.Resources.mPersonaManager which has a reference to
            // android.os.PersonaManager.mContext which is an activity.
            excluded.instanceField("android.os.PersonaManager", "mContext");
        }

        if (SAMSUNG.equals(MANUFACTURER) && SDK_INT == KITKAT) {
            // In AOSP the Resources class does not have a context.
            // Here we have ZygoteInit.mResources (static field) holding on to a Resources instance that
            // has a context that is the activity.
            // Observed here: https://github.com/square/leakcanary/issues/1#issue-74450184
            excluded.instanceField("android.content.res.Resources", "mContext");
        }

        if (SAMSUNG.equals(MANUFACTURER) && SDK_INT == KITKAT) {
            // In AOSP the ViewConfiguration class does not have a context.
            // Here we have ViewConfiguration.sConfigurations (static field) holding on to a
            // ViewConfiguration instance that has a context that is the activity.
            // Observed here: https://github.com/square/leakcanary/issues/1#issuecomment-100324683
            excluded.instanceField("android.view.ViewConfiguration", "mContext");
        }

        if (SAMSUNG.equals(MANUFACTURER) && SDK_INT == KITKAT) {
            // Samsung added a static mContext_static field to AudioManager, holds a reference to the
            // activity.
            // Observed here: https://github.com/square/leakcanary/issues/32
            excluded.staticField("android.media.AudioManager", "mContext_static");
        }

        if (SDK_INT <= LOLLIPOP_MR1) {
            excluded.instanceField("android.media.AudioManager$1", "this$0") ;
        }

        if (SDK_INT <= LOLLIPOP_MR1) {
            excluded.instanceField("android.widget.Editor$Blink", "this$0");
        }

        if (SDK_INT <= M) {
            excluded.instanceField("android.net.ConnectivityManager", "sInstance") ;
        }

        if (SDK_INT >= O && SDK_INT <= O_MR1) {
            excluded.instanceField("android.view.accessibility.AccessibilityNodeInfo", "mOriginalText") ;
        }

        if (SDK_INT >= O && SDK_INT <= N) {
            excluded.instanceField("com.android.internal.policy.BackdropFrameRenderer", "mDecorView") ;
        }

        if ( SDK_INT == N) {
            excluded.instanceField("android.speech.tts.TextToSpeech", "mContext") ;
            excluded.instanceField("android.speech.tts.TtsEngines", "mContext") ;
        }

        if ( MANUFACTURER.equalsIgnoreCase(HUAWEI) && SDK_INT >= N && SDK_INT <= N_MR1) {
            excluded.instanceField("android.gestureboost.GestureBoostManager", "mContext") ;
        }

        if ( SDK_INT == P ) {
            excluded.instanceField("android.view.ViewGroup$ViewLocationHolder", "mRoot") ;
            excluded.staticField("android.view.ViewGroup$ViewLocationHolder", "sPool") ;
        }
    }

    private static void excludeInvalidReference(ExcludedRefs.Builder excluded) {
        // 默认去除软引用、弱引用、虚引用
        excluded.instanceField(SoftReference.class.getName(), "referent") ;
        excluded.instanceField(WeakReference.class.getName(), "referent") ;
        excluded.instanceField(PhantomReference.class.getName(), "referent") ;

        excluded.instanceField("java.lang.ref.FinalizerReference", "referent") ;
        excluded.instanceField("java.lang.ref.Finalizer", "prev");
        excluded.instanceField("java.lang.ref.Finalizer", "element");
        excluded.instanceField("java.lang.ref.Finalizer", "next") ;
        excluded.instanceField("java.lang.ref.FinalizerReference", "prev");
        excluded.instanceField("java.lang.ref.FinalizerReference", "element");
        excluded.instanceField("java.lang.ref.FinalizerReference", "next") ;
        excluded.instanceField("sun.misc.Cleaner", "prev") ;
        excluded.instanceField("sun.misc.Cleaner", "next") ;
    }
}
