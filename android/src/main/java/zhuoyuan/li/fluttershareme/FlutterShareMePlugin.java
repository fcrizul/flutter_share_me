package zhuoyuan.li.fluttershareme;


import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;

import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.share.Sharer;
import com.facebook.share.model.ShareLinkContent;
import com.facebook.share.model.ShareStoryContent;
import com.facebook.share.model.SharePhoto;
import com.facebook.share.model.ShareVideo;
import com.facebook.share.model.ShareContent;
import com.facebook.share.model.ShareMediaContent;
import com.facebook.share.model.ShareMedia;
import com.facebook.share.widget.ShareDialog;
import com.twitter.sdk.android.tweetcomposer.TweetComposer;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;
import java.util.ArrayList;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;

/**
 * FlutterShareMePlugin
 */
public class FlutterShareMePlugin implements MethodCallHandler, FlutterPlugin, ActivityAware {

    final private static String _methodWhatsApp = "whatsapp_share";
    final private static String _methodWhatsAppPersonal = "whatsapp_personal";
    final private static String _methodWhatsAppBusiness = "whatsapp_business_share";
    final private static String _methodFaceBook = "facebook_share";
    final private static String _methodTwitter = "twitter_share";
    final private static String _methodSystemShare = "system_share";
    final private static String _methodInstagramShare = "instagram_share";


    private Activity activity;
    private static CallbackManager callbackManager;
    private MethodChannel methodChannel;

    /**
     * Plugin registration.
     */
    public static void registerWith(Registrar registrar) {
        final FlutterShareMePlugin instance = new FlutterShareMePlugin();
        instance.onAttachedToEngine(registrar.messenger());
        instance.activity = registrar.activity();
    }

    @Override
    public void onAttachedToEngine(FlutterPluginBinding binding) {
        onAttachedToEngine(binding.getBinaryMessenger());
    }

    @Override
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
        methodChannel.setMethodCallHandler(null);
        methodChannel = null;
        activity = null;
    }

    private void onAttachedToEngine(BinaryMessenger messenger) {
        methodChannel = new MethodChannel(messenger, "flutter_share_me");
        methodChannel.setMethodCallHandler(this);
        callbackManager = CallbackManager.Factory.create();
    }

    /**
     * method
     *
     * @param call   methodCall
     * @param result Result
     */
    @Override
    public void onMethodCall(MethodCall call, @NonNull Result result) {
        String url, msg, fileType, postType;
        List<String> urls;
        switch (call.method) {
            case _methodFaceBook:
                urls = call.argument("urls");
                msg = call.argument("msg");
                fileType = call.argument("fileType");
                postType = call.argument("postType");
                shareToFacebook(urls, msg, fileType, postType, result);
                break;
            case _methodTwitter:
                url = call.argument("url");
                msg = call.argument("msg");
                shareToTwitter(url, msg, result);
                break;
            case _methodWhatsApp:
                msg = call.argument("msg");
                urls = call.argument("urls");
                shareWhatsApp(urls, msg, result, false);
                break;
            case _methodWhatsAppBusiness:
                msg = call.argument("msg");
                url = call.argument("url");
                //shareWhatsApp(url, msg, result, true);
                break;
            case _methodWhatsAppPersonal:
                msg = call.argument("msg");
                String phoneNumber = call.argument("phoneNumber");
                shareWhatsAppPersonal(msg, phoneNumber, result);
                break;
            case _methodSystemShare:
                msg = call.argument("msg");
                shareSystem(result, msg);
                break;
            case _methodInstagramShare:
                urls = call.argument("urls");
                msg = call.argument("msg");
                fileType = call.argument("fileType");
                postType = call.argument("postType");
                shareInstagram(urls, fileType, postType, msg, result);
                break;
            default:
                result.notImplemented();
                break;
        }
    }

    /**
     * system share
     *
     * @param msg    String
     * @param result Result
     */
    private void shareSystem(Result result, String msg) {
        try {
            Intent textIntent = new Intent("android.intent.action.SEND");
            textIntent.setType("text/plain");
            textIntent.putExtra("android.intent.extra.TEXT", msg);
            activity.startActivity(Intent.createChooser(textIntent, "Share to"));
            result.success("success");
        } catch (Exception var7) {
            result.error("error", var7.toString(), "");
        }
    }

    /**
     * share to twitter
     *
     * @param url    String
     * @param msg    String
     * @param result Result
     */

    private void shareToTwitter(String url, String msg, Result result) {
        try {
            TweetComposer.Builder builder = new TweetComposer.Builder(activity)
                    .text(msg);
            if (url != null && url.length() > 0) {
                builder.url(new URL(url));
            }

            builder.show();
            result.success("success");
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    /**
     * share to Facebook
     *
     * @param url    String
     * @param msg    String
     * @param result Result
     */
    private void shareToFacebook(List<String> imagesPath, String msg, String fileType, String postType, Result result) {

        ShareDialog shareDialog = new ShareDialog(activity);
        // this part is optional
        shareDialog.registerCallback(callbackManager, new FacebookCallback<Sharer.Result>() {
            @Override
            public void onSuccess(Sharer.Result result) {
                System.out.println("--------------------success");
            }

            @Override
            public void onCancel() {
                System.out.println("-----------------onCancel");
            }

            @Override
            public void onError(FacebookException error) {
                System.out.println("---------------onError");
            }
        });
        List<ShareMedia> medias = new ArrayList<ShareMedia>();
        for (String imagePath : imagesPath) 
        {
            File file = new File(imagePath);
            Uri fileUri = FileProvider.getUriForFile(activity, activity.getApplicationContext().getPackageName() + ".provider", file);
            if(fileType.equals("image")){
                SharePhoto sharePhoto = new SharePhoto.Builder()
                    .setImageUrl(fileUri)
                    .build();
                medias.add(sharePhoto);
            }else if(fileType.equals("video")){
                ShareVideo sharePhoto = new ShareVideo.Builder()
                    .setLocalUrl(fileUri)
                    .build();
                medias.add(sharePhoto);
            }
        } 
        if(postType.equals("feed")){
            ShareMediaContent shareContent = new ShareMediaContent.Builder()
                .addMedia(medias)
                .build();
            if (ShareDialog.canShow(ShareMediaContent.class)) {
                shareDialog.show(shareContent);
                result.success("success");
            }
        }else{
            ShareStoryContent shareContent = new ShareStoryContent.Builder()
                    .setBackgroundAsset(medias.get(0))
                    .build();
            if (ShareDialog.canShow(ShareMediaContent.class)) {
                shareDialog.show(shareContent);
                result.success("success");
            }
        }
    }

    /**
     * share to whatsapp
     *
     * @param msg                String
     * @param result             Result
     * @param shareToWhatsAppBiz boolean
     */
    private void shareWhatsApp(List<String> imagesPath, String msg, Result result, boolean shareToWhatsAppBiz) {
        try {
            Intent whatsappIntent = new Intent(Intent.ACTION_SEND_MULTIPLE);
            ArrayList<Uri> files = new ArrayList<Uri>();
            
            whatsappIntent.setPackage(shareToWhatsAppBiz ? "com.whatsapp.w4b" : "com.whatsapp");
            whatsappIntent.putExtra(Intent.EXTRA_TEXT, msg);
            // if the url is the not empty then get url of the file and share
            if (imagesPath.size() > 0 ) {
                whatsappIntent.setType("*/*");
                for (String imagePath : imagesPath) 
                {
                    File file = new File(imagePath);
                    Uri fileUri = FileProvider.getUriForFile(activity, activity.getApplicationContext().getPackageName() + ".provider", file);
                    files.add(fileUri); 
                } 
                whatsappIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                whatsappIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, files);
                whatsappIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }
            else {
                whatsappIntent.setType("text/plain");
            }
            activity.startActivity(whatsappIntent);
            result.success("success");
        } catch (Exception var9) {
            result.error("error", var9.toString(), "");
        }
    }

    /**
     * share whatsapp message to personal number
     *
     * @param msg         String
     * @param phoneNumber String with country code
     * @param result
     */
    private void shareWhatsAppPersonal(String msg, String phoneNumber, Result result) {
        String url = null;
        try {
            url = "https://api.whatsapp.com/send?phone=" + phoneNumber + "&text=" + URLEncoder.encode(msg, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setPackage("com.whatsapp");
        i.setData(Uri.parse(url));
        activity.startActivity(i);
        result.success("success");
    }

    /**
     * share to instagram
     *
     * @param url    local image path
     * @param result flutterResult
     */
    private void shareInstagram(List<String> imagesPath,String fileType, String postType, String msg, Result result) {
        if (instagramInstalled()) {
            ArrayList<Uri> files = new ArrayList<Uri>();
            for (String imagePath : imagesPath) 
            {
                File file = new File(imagePath);
                Uri fileUri = FileProvider.getUriForFile(activity, activity.getApplicationContext().getPackageName() + ".provider", file);
                files.add(fileUri); 
            } 
            Intent instagramIntent;
            if(postType.equals("story")){
                instagramIntent = new Intent(Intent.ACTION_SEND_MULTIPLE);
                instagramIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, files);
            }else{
                instagramIntent = new Intent("com.instagram.share.ADD_TO_STORY");
                instagramIntent.putExtra(Intent.EXTRA_STREAM, files.get(0));
            }
             if(fileType.equals("image")){
                instagramIntent.setType("image/*");
            }else{
                instagramIntent.setType("video/*");
            }
            //instagramIntent.putExtra(Intent.EXTRA_TITLE, msg);            
            instagramIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            instagramIntent.setPackage("com.instagram.android");
            try {
                activity.startActivity(instagramIntent);
                result.success("Success");
            } catch (ActivityNotFoundException e) {
                e.printStackTrace();
                result.success("Failure");
            }
        } else {
            result.error("Instagram not found", "Instagram is not installed on device.", "");
        }
    }

    @Override
    public void onAttachedToActivity(ActivityPluginBinding binding) {
        activity = binding.getActivity();
    }

    @Override
    public void onDetachedFromActivityForConfigChanges() {

    }

    @Override
    public void onReattachedToActivityForConfigChanges(ActivityPluginBinding binding) {
        activity = binding.getActivity();
    }

    @Override
    public void onDetachedFromActivity() {

    }

    ///Utils methods
    private boolean instagramInstalled() {
        try {
            if (activity != null) {
                activity.getPackageManager()
                        .getApplicationInfo("com.instagram.android", 0);
                return true;
            } else {
                Log.d("App", "Instagram app is not installed on your device");
                return false;
            }
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
//        return false;
    }
}
