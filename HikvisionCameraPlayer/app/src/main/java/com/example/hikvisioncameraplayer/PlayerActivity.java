package com.example.hikvisioncameraplayer;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.hikvision.netsdk.ExceptionCallBack;
import com.hikvision.netsdk.HCNetSDK;
import com.hikvision.netsdk.INT_PTR;
import com.hikvision.netsdk.NET_DVR_DEVICEINFO_V30;
import com.hikvision.netsdk.NET_DVR_PREVIEWINFO;
import com.hikvision.netsdk.RealPlayCallBack;

import org.MediaPlayer.PlayM4.Player;

public class PlayerActivity extends AppCompatActivity implements SurfaceHolder.Callback {

    public final static String EXTRA_IP_ADDRESS = "IP_ADDRESS";
    public final static String EXTRA_USERNAME = "USERNAME";
    public final static String EXTRA_PASSWORD = "PASSWORD";


    private final String TAG = "PlayerActivity";

    private NET_DVR_DEVICEINFO_V30 netDeviceInfoV30 = null;
    private final HCNetSDK netSDKInstance = HCNetSDK.getInstance();
    private final Player playerInstance = Player.getInstance();

    private int loginId = -1; // return by NET_DVR_Login_v30
    private int realPlayId = -1; // return by NET_DVR_RealPlay_V30
    private int playbackId = -1; // return by NET_DVR_PlayBackByTime
    private int playPort = -1; // play port
    private int startChannel = 0; // start channel no
    private boolean stopPlayback = false;
    private boolean isShow = true;

    private String ADDRESS;
    private final int PORT = 8000;
    private String USER;
    private String PSD;


    private Thread thread;
    private SurfaceView surfaceView;
    private INT_PTR error;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);
        surfaceView = findViewById(R.id.surface_player);
        surfaceView.getHolder().addCallback(this);

        ADDRESS = getIntent().getStringExtra(EXTRA_IP_ADDRESS);
        USER = getIntent().getStringExtra(EXTRA_USERNAME);
        PSD = getIntent().getStringExtra(EXTRA_PASSWORD);

        initHikSdk();
        initClient();
    }

    private void initClient() {
        // login on the device
        loginId = loginDevice();
        if (loginId < 0) {
            Log.e(TAG, "This device logins failed!");
            return;
        } else {
            Log.d(TAG, "loginId=" + loginId);
        }

        // get instance of exception callback and set
        ExceptionCallBack oexceptionCbf = getExceptiongCbf();
        if (oexceptionCbf == null) {
            Log.e(TAG, "ExceptionCallBack object is failed!");
            return;
        }

        if (!netSDKInstance.NET_DVR_SetExceptionCallBack(
                oexceptionCbf)) {
            Log.e(TAG, "NET_DVR_SetExceptionCallBack is failed!");
            return;
        }

        //预览
        final NET_DVR_PREVIEWINFO ClientInfo = new NET_DVR_PREVIEWINFO();
        ClientInfo.lChannel = 0;
        ClientInfo.dwStreamType = 0; // substream
        ClientInfo.bBlocked = 1;
        //设置默认点
        thread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (!Thread.currentThread().isInterrupted()) {
                    SystemClock.sleep(1000);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (isShow)
                                startSinglePreview();
                        }
                    });
                }
            }
        });
        thread.start();
    }

    private void startSinglePreview() {
        if (playbackId >= 0) {
            Log.i(TAG, "Please stop palyback first");
            return;
        }
        RealPlayCallBack fRealDataCallBack = getRealPlayerCbf();
        if (fRealDataCallBack == null) {
            Log.e(TAG, "fRealDataCallBack object is failed!");
            return;
        }
        Log.i(TAG, "startChannel:" + startChannel);

        NET_DVR_PREVIEWINFO previewInfo = new NET_DVR_PREVIEWINFO();
        previewInfo.lChannel = startChannel;
        previewInfo.dwStreamType = 0; // substream
        previewInfo.bBlocked = 1;

        realPlayId = netSDKInstance.NET_DVR_RealPlay_V40(loginId,
                previewInfo, fRealDataCallBack);
        if (realPlayId < 0) {
            Log.e(TAG, "NET_DVR_RealPlay is failed!Err: "
                    + netSDKInstance.NET_DVR_GetLastError());
            return;
        }
        isShow = false;
        if (thread != null) {
            thread.interrupt();
        }
    }

    /**
     * @fn initHikSdk
     * @brief SDK init
     */
    private void initHikSdk() {
        // init net sdk
        if (!netSDKInstance.NET_DVR_Init()) {

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(R.string.init_sdk_init_error)
                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            try {
                                this.finalize();
                            } catch (Throwable throwable) {
                                throwable.printStackTrace();
                            }
                        }
                    });
            // Create the AlertDialog object and return it
            builder.create().show();

        }
    }

    /**
     * @return login ID
     * @fn loginDevice
     * @brief login on device
     */
    private int loginDevice() {
        int iLogID = -1;
        iLogID = loginNormalDevice();
        return iLogID;
    }

    /**
     * @return login ID
     * @fn loginNormalDevice
     * @brief login on device
     */
    private int loginNormalDevice() {

        // get instance
        netDeviceInfoV30 = new NET_DVR_DEVICEINFO_V30();

        // call NET_DVR_Login_v30 to login on, port 8000 as default
        int iLogID = netSDKInstance.NET_DVR_Login_V30(ADDRESS, PORT,
                USER, PSD, netDeviceInfoV30);
        if (iLogID < 0) {
            error = new INT_PTR();
            error.iValue = netSDKInstance.NET_DVR_GetLastError();
            Log.e(TAG, "NET_DVR_Login is failed!Err: "
                    + netSDKInstance.NET_DVR_GetErrorMsg(error));
            return -1;
        }
        if (netDeviceInfoV30.byChanNum > 0) {
            startChannel = netDeviceInfoV30.byStartChan;
        } else if (netDeviceInfoV30.byIPChanNum > 0) {
            startChannel = netDeviceInfoV30.byStartDChan;
        }
        Log.i(TAG, "NET_DVR_Login is Successful!");
        return iLogID;
    }

    /**
     * @return exception instance
     * @fn getExceptiongCbf
     */
    private ExceptionCallBack getExceptiongCbf() {
        ExceptionCallBack oExceptionCbf = new ExceptionCallBack() {
            public void fExceptionCallBack(int iType, int iUserID, int iHandle) {
                Log.d(TAG, "recv exception, type:" + iType);
            }
        };
        return oExceptionCbf;
    }

    /**
     * @return callback instance
     * @fn getRealPlayerCbf
     * @brief get realplay callback instance
     */
    private RealPlayCallBack getRealPlayerCbf() {
        RealPlayCallBack cbf = new RealPlayCallBack() {
            public void fRealDataCallBack(int iRealHandle, int iDataType,
                                          byte[] pDataBuffer, int iDataSize) {
                // player channel 1
                PlayerActivity.this.processRealData(iDataType, pDataBuffer,
                        iDataSize, Player.STREAM_REALTIME);
            }
        };
        return cbf;
    }

    /**
     * @param iDataType   - data type [in]
     * @param pDataBuffer - data buffer [in]
     * @param iDataSize   - data size [in]
     * @param iStreamMode - stream mode [in]
     * @return NULL
     * @fn processRealData
     * @brief process real data
     */
    public void processRealData(int iDataType,
                                byte[] pDataBuffer, int iDataSize, int iStreamMode) {

        // must decode data
        if (HCNetSDK.NET_DVR_SYSHEAD == iDataType) {
            if (playPort >= 0) {
                return;
            }
            playPort = playerInstance.getPort();
            if (playPort == -1) {
                Log.e(TAG, "getPort is failed with: "
                        + playerInstance.getLastError(playPort));
                return;
            }
            Log.i(TAG, "getPort succ with: " + playPort);
            if (iDataSize > 0) {
                if (!playerInstance.setStreamOpenMode(playPort,
                        iStreamMode)) // set stream mode
                {
                    Log.e(TAG, "setStreamOpenMode failed");
                    return;
                }
                if (!playerInstance.openStream(playPort, pDataBuffer,
                        iDataSize, 2 * 1024 * 1024)) // open stream
                {
                    Log.e(TAG, "openStream failed");
                    return;
                }
                if (!playerInstance.play(playPort,
                        surfaceView.getHolder())) {
                    Log.e(TAG, "play failed");
                    return;
                }
                if (!playerInstance.playSound(playPort)) {
                    Log.e(TAG, "playSound failed with error code:"
                            + playerInstance.getLastError(playPort));
                    return;
                }
            }
        } else {
            if (!playerInstance.inputData(playPort, pDataBuffer,
                    iDataSize)) {
                // Log.e(TAG, "inputData failed with: " +
                // playerInstance.getLastError(playPort));
                for (int i = 0; i < 4000 && playbackId >= 0
                        && !stopPlayback; i++) {
                    if (playerInstance.inputData(playPort,
                            pDataBuffer, iDataSize)) {
                        break;

                    }

                    if (i % 100 == 0) {
                        Log.e(TAG, "inputData failed with: "
                                + playerInstance
                                .getLastError(playPort) + ", i:" + i);
                    }
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    /**
     * @return NULL
     * @fn Cleanup
     * @brief cleanup
     */
    public void Cleanup() {
        // release player resource

        stopPlayback = true;
        playerInstance.freePort(playPort);
        playPort = -1;
        // release net SDK resource
        netSDKInstance.NET_DVR_Cleanup();
    }

    /**
     * @return NULL
     * @fn stopSinglePreview
     * @brief stop preview
     */
    private void stopSinglePreview() {
        if (realPlayId < 0) {
            Log.e(TAG, "realPlayId < 0");
            return;
        }
        // net sdk stop preview
        if (!netSDKInstance.NET_DVR_StopRealPlay(realPlayId)) {
            Log.e(TAG, "StopRealPlay is failed!Err:"
                    + netSDKInstance.NET_DVR_GetLastError());
            return;
        }

        realPlayId = -1;
        stopSinglePlayer();
    }

    private void stopSinglePlayer() {
        playerInstance.stopSound();
        // player stop play
        if (!playerInstance.stop(playPort)) {
            Log.e(TAG, "stop is failed!");
            return;
        }

        if (!playerInstance.closeStream(playPort)) {
            Log.e(TAG, "closeStream is failed!");
            return;
        }
        if (!playerInstance.freePort(playPort)) {
            Log.e(TAG, "freePort is failed!" + playPort);
            return;
        }
        playPort = -1;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        surfaceView.getHolder().setFormat(PixelFormat.TRANSLUCENT);
        Log.i(TAG, "surface is created" + playPort);
        if (-1 == playPort) {
            return;
        }
        Surface surface = holder.getSurface();
        if (surface.isValid()) {
            if (!playerInstance
                    .setVideoWindow(playPort, 0, holder)) {
                Log.e(TAG, "Player setVideoWindow failed!");
            }
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.i(TAG, "Player setVideoWindow release!" + playPort);
        if (-1 == playPort) {
            return;
        }
        if (holder.getSurface().isValid()) {
            if (!playerInstance.setVideoWindow(playPort, 0, null)) {
                Log.e(TAG, "Player setVideoWindow failed!");
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt("playPort", playPort);
        super.onSaveInstanceState(outState);
        Log.i(TAG, "onSaveInstanceState");
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        playPort = savedInstanceState.getInt("playPort");
        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Cleanup();
        loginId = -1;
        // whether we have logout
        if (!netSDKInstance.NET_DVR_Logout_V30(loginId)) {
            Log.e(TAG, " NET_DVR_Logout is failed!");
            return;
        }
        stopSinglePreview();
    }

}