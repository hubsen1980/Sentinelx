package com.invertedx.sentinelx.channel

import android.Manifest
import android.app.Activity
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.ShareCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.invertedx.sentinelx.MainActivity
import com.invertedx.sentinelx.R
import com.invertedx.sentinelx.SentinelxApp
import com.invertedx.sentinelx.api.ApiService
import com.invertedx.sentinelx.utils.SentinalPrefs
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import org.bitcoinj.params.MainNetParams
import org.bitcoinj.params.TestNet3Params
import java.io.*
import java.util.*


class SystemChannel(private val applicationContext: Context, private val activity: MainActivity) : MethodChannel.MethodCallHandler, EventChannel.StreamHandler {

    private val NOTIIFCATION_STREAM = "NOTIFICATION_STREAM";
    private var notificationSink: EventChannel.EventSink? = null;
    private val disposables: CompositeDisposable = CompositeDisposable();

    init {
        EventChannel(activity.flutterView, NOTIIFCATION_STREAM)
                .setStreamHandler(object : EventChannel.StreamHandler {
                    override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
                        notificationSink = events;
                    }

                    override fun onCancel(arguments: Any?) {
                        notificationSink = null;
                    }

                });
    }

    override fun onMethodCall(methodCall: MethodCall, result: MethodChannel.Result) {
        when (methodCall.method) {
            "documentPath" -> {
                val dir = applicationContext.getDir("sentinelx", Context.MODE_PRIVATE);
                if (!dir.exists()) {
                    dir.mkdir()
                }
                return result.success(dir.path)
            }
            "setNetwork" -> {
                val isTestNet = methodCall.argument<Boolean>("mode")
                val pref = SentinalPrefs(applicationContext)
                if (isTestNet != null) {
                    pref.isTestNet = isTestNet
                    pref.firstRunComplete = true
                    SentinelxApp.setNetWorkParam(if (isTestNet) TestNet3Params.get() else MainNetParams.get())
                }
                result.success("Success")
            }
            "getNetWork" -> {
                val pref = SentinalPrefs(applicationContext)
                if (pref.isTestNet != null) {
                    result.success(if (pref.isTestNet!!) "TESTNET" else "MAINNET");
                }
            }
            "isFirstRun" -> {
                val pref = SentinalPrefs(applicationContext)
                return if (pref.firstRunComplete != null) {
                    result.success(!pref.firstRunComplete!!)
                } else {
                    result.success(true)
                }
            }
            "openURL" -> {
                val url = methodCall.arguments as String;
                val i = Intent(Intent.ACTION_VIEW)
                i.data = Uri.parse(url)
                i.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                applicationContext.startActivity(i)
                result.success(true)
            }
            "share" -> {
                val sharingIntent = Intent(Intent.ACTION_SEND)
                sharingIntent.type = "text/plain"
                sharingIntent.putExtra(Intent.EXTRA_TEXT, methodCall.arguments as String)
                sharingIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                activity.startActivity(Intent.createChooser(sharingIntent, "Share using"))
                result.success(true)
            }
            "cameraPermission" -> {
                val permission = ContextCompat.checkSelfPermission(activity,
                        Manifest.permission.CAMERA)

                if (permission == PackageManager.PERMISSION_GRANTED) {
                    result.success(true)
                    return
                }

                ActivityCompat.requestPermissions(activity,
                        arrayOf(Manifest.permission.CAMERA),
                        1)

                this.activity.setOnPermissionResult { requestCode, permissions, grantResults ->

                    for (i in permissions.indices) {
                        val permissionResult = permissions[i]
                        val grantResult = grantResults[i]

                        if (permissionResult == Manifest.permission.CAMERA) {
                            if (grantResult == PackageManager.PERMISSION_GRANTED) {
                                result.success(true)

                            } else {
                                result.success(false)
                            }
                        }
                    }
                }
            }

            "setHttpTimeout" -> {
                val timeout = methodCall.arguments as Int
                SentinalPrefs(applicationContext).timeout = timeout;
                ApiService(applicationContext).makeClient()
                result.success(true);
            }
            "getHttpTimeout" -> {
                val timeout = SentinalPrefs(applicationContext).timeout
                if (timeout != null) {
                    result.success(timeout)
                } else {
                    result.success(90)
                }
            }


            "getPackageInfo" -> {
                val pm = applicationContext.packageManager
                val info = pm.getPackageInfo(applicationContext.packageName, 0)
                val map: MutableMap<String, String> = HashMap()
                map["appName"] = info.applicationInfo.loadLabel(pm).toString()
                map["packageName"] = applicationContext.packageName
                map["version"] = info.versionName
                map["buildNumber"] = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    info.longVersionCode.toString()
                } else info.versionCode.toString()

                result.success(map)
            }

            "shareDirectory" -> {
                val dir = applicationContext.filesDir;
                val imageDir = File("${dir.path}/images");
                if (!imageDir.exists()) {
                    imageDir.mkdirs();
                }
                return result.success(imageDir.path)
            }

            "shareQR" -> {
                val dir = applicationContext.filesDir;
                val imageDir = File("${dir.path}/images");
                val cacheFile = File(imageDir, "/qr.jpg")
                val uri = FileProvider.getUriForFile(activity, "${applicationContext.packageName}.provider", cacheFile)

                val intent: Intent = ShareCompat.IntentBuilder.from(activity)
                        .setType("image/jpg")
                        .setSubject("Qr code")
                        .setStream(uri)
                        .setChooserTitle("Share QR image")
                        .createChooserIntent()
                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

                result.success(true)

                activity.startActivity(intent)

            }

            "setDojo" -> {
                val dojoUrl = methodCall.argument<String>("dojoUrl");
                val dojoKey = methodCall.argument<String>("dojoKey");

                SentinalPrefs(applicationContext).dojoUrl = dojoUrl
                SentinalPrefs(applicationContext).dojoKey = dojoKey

                result.success(true)
            }

            "clearDojo" -> {

                SentinalPrefs(applicationContext).dojoUrl = null
                SentinalPrefs(applicationContext).dojoKey = null

                result.success(true)
            }

            "showUpdateNotification" -> {
                val newVersion = methodCall.argument<String>("newVersion");
                if (newVersion.isNullOrEmpty()) {
                    result.error("Invalid", "invalid", null);
                    return;
                }

                val resultIntent = Intent(applicationContext, MainActivity::class.java)
                resultIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP

                resultIntent.putExtra("TYPE", "NOTIFY")

                val resultPendingIntent = PendingIntent.getActivity(applicationContext, 11, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                val builder = NotificationCompat.Builder(applicationContext, "UPDATE_CHANNEL")
                        .setSmallIcon(R.drawable.ic_update_black_24dp)
                        .setContentTitle("Update v$newVersion is available")
                        .setAutoCancel(true)
                        .setContentText("A new version of SentinelX is now available to download")
                        .apply {
                            setContentIntent(resultPendingIntent)
                        }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    // Register the channel with the system
                    val notificationManager: NotificationManager =
                            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    notificationManager.notify(11, builder.build());
                }

            }


            "saveToFile" -> {
                val REQUEST_CODE = 68;

                val fileName = methodCall.argument<String>("name");
                val data = methodCall.argument<String>("data");

                val intent = Intent(Intent.ACTION_CREATE_DOCUMENT);

                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.type = "text/plain";
                intent.putExtra(Intent.EXTRA_TITLE, fileName);
                intent.putExtra(Intent.EXTRA_TITLE, fileName);

                activity.startActivityForResult(intent, REQUEST_CODE);

                //This will bind to MainActivity onActivityResult
                activity.listenResult(ActivityResultListener { intent, resultCode ->

                    if (resultCode == Activity.RESULT_CANCELED) {
                        return@ActivityResultListener
                    }

                    if (intent == null) {
                        result.error("00", "Unable to retrieve intent", null);
                        return@ActivityResultListener
                    }

                    try {
                        val outputStream = activity.contentResolver.openOutputStream(intent.data!!);
                        val bw = BufferedWriter(OutputStreamWriter(outputStream!!));
                        bw.write(data!!);
                        bw.flush();
                        bw.close();
                        result.success(true);
                    } catch (e: IOException) {
                        result.error("00", "Unable to write file", e.message);
                        e.printStackTrace();
                    }

                }, REQUEST_CODE);
            }
            "openFile" -> {
                val requestCode = 21;
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.type = "*/*";
                activity.startActivityForResult(intent, requestCode);

                //This will bind to MainActivity onActivityResult
                activity.listenResult(ActivityResultListener { intent, resultCode ->

                    if (resultCode == Activity.RESULT_CANCELED) {
                        return@ActivityResultListener
                    }

                    if (intent == null) {
                        result.error("00", "Unable to retrieve intent", null);
                        return@ActivityResultListener
                    }

                    try {
                        val disposable = Single.fromCallable<String> {

                            //get file size
                            val size = activity.contentResolver.openFileDescriptor(intent.data!!, "r")
                                    ?.statSize;

                            if (size != null) {
                                if (size > 2e+6) {
                                    throw  IOException("File size is too large")
                                }
                                //Restrict Files that has size more than 2 mb
                                activity.contentResolver.openInputStream(intent.data!!)
                                        .use {
                                            it!!.bufferedReader().readText();
                                        }
                            } else {
                                return@fromCallable "";
                            }

                        }.subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe { content, throwable ->
                                    if (throwable == null)
                                        result.success(content)
                                    if (throwable != null)
                                        result.error("00", throwable.message, throwable.message);
                                }
                        disposables.add(disposable);
                    } catch (e: IOException) {
                        result.error("00", "Unable to write file", e.message);
                        e.printStackTrace();
                    }

                }, requestCode);
            }

        }
    }

    override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
    }

    override fun onCancel(arguments: Any?) {
    }

    fun onNotificationIntent(intent: Intent) {
        if (intent.hasExtra("TYPE") && intent.getStringExtra("TYPE") == "NOTIFY") {
            if (notificationSink != null) {
                notificationSink!!.success("UPDATE_NOTIFICATION");
            }
        }
    }

    fun dispose() {
        disposables .dispose()
    }


}