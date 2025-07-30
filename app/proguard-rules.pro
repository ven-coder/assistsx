
-keep class com.ven.assists.** { *; }
-keep class com.ven.assists.mp.** { *; }
-keep class com.ven.assists.web.** { *; }
-keep class com.ven.assistsx.model.** { *; }

# 保持项目模块的类
-keep class com.youth.banner.** { *; }
-keep class com.weikaiyun.fragmentation.** { *; }
-keep class com.weikaiyun.fragmentationx_swipeback.** { *; }

# 保持AndroidX库的类
-keep class androidx.** { *; }
-dontwarn androidx.**

# 保持Gson解析的类
-keep class com.google.gson.** { *; }
-keepattributes Signature
-keepattributes *Annotation

# 保持Room库的类
-keep class androidx.room.** { *; }
-keep class com.aip.otx.android.room.** { *; }

# 保持Lifecycle库的类
-keep class androidx.lifecycle.** { *; }

# 保持OkHttp的类
-keep class okhttp3.** { *; }
-dontwarn okhttp3.**
-keep class com.squareup.okhttp3.** { *; }

# 保持Socket.IO的类
-keep class io.socket.** { *; }

# 保持Coil库的类
-keep class coil.** { *; }
-dontwarn coil.**

# 保持Agora SDK的类
-keep class io.agora.**{*;}
-dontwarn io.agora.**

# 保持SVGAPlayer的类
-keep class com.opensource.svgaplayer.** { *; }

# 保持PhotoView的类
-keep class com.github.chrisbanes.** { *; }

# 保持Material库的类
-keep class com.google.android.material.** { *; }

# 其他库的类
-keep class com.blankj.utilcode.** { *; }
-keep class com.github.mrmike.** { *; }
-keep class com.vanniktech.** { *; }

# 防止在混淆过程中移除主活动
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application

-keepattributes Exceptions,InnerClasses

-keepattributes Signature

-keep class io.rong.** {*;}
-keep class cn.rongcloud.** {*;}
-dontwarn io.rong.push.**
-dontnote com.xiaomi.**
-dontnote com.google.android.gms.gcm.**
-dontnote io.rong.**

-keep class com.umeng.** {*;}

-keep class org.repackage.** {*;}

-keep class com.uyumao.** { *; }

-keepclassmembers class * {
   public <init> (org.json.JSONObject);
}

-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

-dontwarn com.tencent.bugly.**
-keep public class com.tencent.bugly.**{*;}

-ignorewarnings

-dontwarn com.google.android.gms.common.GoogleApiAvailability
-dontwarn com.google.android.gms.gcm.GcmListenerService
-dontwarn com.google.android.gms.iid.InstanceID
-dontwarn com.google.android.gms.iid.InstanceIDListenerService
-dontwarn com.google.android.gms.tasks.OnCompleteListener
-dontwarn com.google.android.gms.tasks.Task
-dontwarn com.google.firebase.FirebaseApp
-dontwarn com.google.firebase.FirebaseOptions
-dontwarn com.google.firebase.messaging.FirebaseMessaging
-dontwarn com.google.firebase.messaging.FirebaseMessagingService
-dontwarn com.google.firebase.messaging.RemoteMessage
-dontwarn com.heytap.msp.push.HeytapPushManager
-dontwarn com.heytap.msp.push.callback.ICallBackResultService
-dontwarn com.hihonor.push.sdk.HonorMessageService
-dontwarn com.hihonor.push.sdk.HonorPushCallback
-dontwarn com.hihonor.push.sdk.HonorPushClient
-dontwarn com.hihonor.push.sdk.HonorPushDataMsg
-dontwarn com.huawei.agconnect.config.AGConnectServicesConfig
-dontwarn com.huawei.hms.aaid.HmsInstanceId
-dontwarn com.huawei.hms.common.ApiException
-dontwarn com.huawei.hms.push.HmsMessageService
-dontwarn com.huawei.hms.push.RemoteMessage
-dontwarn com.meizu.cloud.pushsdk.MzPushMessageReceiver
-dontwarn com.meizu.cloud.pushsdk.PushManager
-dontwarn com.meizu.cloud.pushsdk.handler.MzPushMessage
-dontwarn com.meizu.cloud.pushsdk.platform.message.PushSwitchStatus
-dontwarn com.meizu.cloud.pushsdk.platform.message.RegisterStatus
-dontwarn com.meizu.cloud.pushsdk.platform.message.SubAliasStatus
-dontwarn com.meizu.cloud.pushsdk.platform.message.SubTagsStatus
-dontwarn com.meizu.cloud.pushsdk.platform.message.UnRegisterStatus
-dontwarn com.vivo.push.IPushActionListener
-dontwarn com.vivo.push.PushClient
-dontwarn com.vivo.push.PushConfig$Builder
-dontwarn com.vivo.push.PushConfig
-dontwarn com.vivo.push.listener.IPushQueryActionListener
-dontwarn com.vivo.push.model.UPSNotificationMessage
-dontwarn com.vivo.push.sdk.OpenClientPushMessageReceiver
-dontwarn com.vivo.push.util.VivoPushException
-dontwarn com.xiaomi.mipush.sdk.MiPushClient
-dontwarn com.xiaomi.mipush.sdk.MiPushCommandMessage
-dontwarn com.xiaomi.mipush.sdk.MiPushMessage
-dontwarn com.xiaomi.mipush.sdk.PushMessageReceiver
-dontwarn io.rong.imlib.translation.TranslationClient
-dontwarn io.rong.imlib.translation.TranslationResultListener