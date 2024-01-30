package com.example.zegovideocalldemo;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.SeekBar;

import com.example.zegovideocalldemo.databinding.ActivityCallBinding;

import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;

import im.zego.effects.ZegoEffects;
import im.zego.effects.entity.ZegoEffectsRosyParam;
import im.zego.effects.entity.ZegoEffectsSharpenParam;
import im.zego.effects.entity.ZegoEffectsSmoothParam;
import im.zego.effects.entity.ZegoEffectsVideoFrameParam;
import im.zego.effects.entity.ZegoEffectsWhitenParam;
import im.zego.effects.enums.ZegoEffectsVideoFrameFormat;
import im.zego.zegoexpress.ZegoExpressEngine;
import im.zego.zegoexpress.callback.IZegoCustomVideoProcessHandler;
import im.zego.zegoexpress.callback.IZegoEventHandler;
import im.zego.zegoexpress.constants.ZegoPlayerState;
import im.zego.zegoexpress.constants.ZegoPublishChannel;
import im.zego.zegoexpress.constants.ZegoPublisherState;
import im.zego.zegoexpress.constants.ZegoRoomState;
import im.zego.zegoexpress.constants.ZegoRoomStateChangedReason;
import im.zego.zegoexpress.constants.ZegoScenario;
import im.zego.zegoexpress.constants.ZegoUpdateType;
import im.zego.zegoexpress.constants.ZegoVideoBufferType;
import im.zego.zegoexpress.entity.ZegoCanvas;
import im.zego.zegoexpress.entity.ZegoCustomVideoProcessConfig;
import im.zego.zegoexpress.entity.ZegoEngineProfile;
import im.zego.zegoexpress.entity.ZegoRoomConfig;
import im.zego.zegoexpress.entity.ZegoStream;
import im.zego.zegoexpress.entity.ZegoUser;

public class CallActivity extends AppCompatActivity {

    private ActivityCallBinding binding;

    private String userId;
    private String userName;
    private String roomId;
    private String licenseData;

    private boolean isFront = true;
    private boolean isOpen = false;
    private ZegoEffects effects;
    private ZegoEngineProfile profile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCallBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (getIntent() != null) {
            Bundle bundle = getIntent().getBundleExtra("bundle");
            if (bundle != null) {
                userId = bundle.getString("userId");
                userName = bundle.getString("userName");
                roomId = bundle.getString("roomId");
                licenseData = bundle.getString("license");

                initAiEffect();
                createEngine();
                initVideoProcess();

                faceWhiten(0);
                smoothFace(0);
                rosyEffect(0);
                sharpenEffect(0);

                startListenerEvent();
                loginRoom();

                adjustmentFaceWhiten();
                adjustmentSmoothFace();
                adjustmentRosy();
                adjustmentSharpen();

                onOpenSetting();
            }
        }

    }

    @Override
    protected void onStart() {
        super.onStart();

        onSwitchCameraDirectionListener();

        binding.ivEnd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopPublishStream();
        stopPreview();
        stopListenEvent();
        destroyEngine();
    }

    private void initAiEffect() {
        String path = this.getCacheDir().getPath();
        String faceDetection = "Resource/FaceWhiteningResources.bundle";
        String pendantDetection = "Resource/PendantResources.bundle";
        String rosyDetection = "Resource/RosyResources.bundle";
        String teethWhiteningDetection = "Resource/TeethWhiteningResources";

        AssetsFileUtil.copyFileFromAssets(this, faceDetection, path + File.separator + faceDetection);
        AssetsFileUtil.copyFileFromAssets(this, pendantDetection, path + File.separator + pendantDetection);
        AssetsFileUtil.copyFileFromAssets(this, rosyDetection, path + File.separator + rosyDetection);
        AssetsFileUtil.copyFileFromAssets(this, teethWhiteningDetection, path + File.separator + teethWhiteningDetection);

        ArrayList<String> effectList = new ArrayList<>();
        effectList.add(path + File.separator + faceDetection);
        effectList.add(path + File.separator + pendantDetection);
        effectList.add(path + File.separator + rosyDetection);
        effectList.add(path + File.separator + teethWhiteningDetection);
        ZegoEffects.setResources(effectList);

        effects = ZegoEffects.create(licenseData, this.getApplication());
    }

    private void createEngine() {
        profile = new ZegoEngineProfile();
        profile.appID = 1488316440L;
        profile.appSign = "d3999f62e4648c5ad695d3d49da189f0b5e0b6edf1ebbff8104571aadc04dfbb";
        profile.scenario = ZegoScenario.STANDARD_VIDEO_CALL;
        profile.application = this.getApplication();
        ZegoExpressEngine.createEngine(profile, null);
    }

    private void initVideoProcess() {
        ZegoCustomVideoProcessConfig config = new ZegoCustomVideoProcessConfig();
        config.bufferType = ZegoVideoBufferType.GL_TEXTURE_2D;
        ZegoExpressEngine.getEngine().enableCustomVideoProcessing(true, config, ZegoPublishChannel.MAIN);
        ZegoExpressEngine.getEngine().setCustomVideoProcessHandler(new IZegoCustomVideoProcessHandler() {

            @Override
            public void onStart(ZegoPublishChannel channel) {
                super.onStart(channel);

                effects.initEnv(1280, 720);

            }

            @Override
            public void onStop(ZegoPublishChannel channel) {
                super.onStop(channel);
            }

            @Override
            public void onCapturedUnprocessedTextureData(int textureID, int width, int height, long referenceTimeMillisecond, ZegoPublishChannel channel) {
                ZegoEffectsVideoFrameParam param = new ZegoEffectsVideoFrameParam();
                param.format = ZegoEffectsVideoFrameFormat.BGRA32;
                param.width = width;
                param.height = height;

                int processedTextureID = effects.processTexture(textureID, param);
                ZegoExpressEngine.getEngine().sendCustomVideoProcessedTextureData(
                        processedTextureID,
                        width,
                        height,
                        referenceTimeMillisecond);
            }
        });

    }

    private void destroyEngine() {
        ZegoExpressEngine.destroyEngine(null);
    }

    private void startListenerEvent() {
        ZegoExpressEngine.getEngine().setEventHandler(new IZegoEventHandler() {

            @Override
            public void onRoomStreamUpdate(String roomID, ZegoUpdateType updateType, ArrayList<ZegoStream> streamList, JSONObject extendedData) {
                super.onRoomStreamUpdate(roomID, updateType, streamList, extendedData);

                if (updateType == ZegoUpdateType.ADD) {
                    startPlayStream(streamList.get(0).streamID);
                } else {
                    stopPlayStream(streamList.get(0).streamID);
                }
            }

            @Override
            public void onRoomUserUpdate(String roomID, ZegoUpdateType updateType, ArrayList<ZegoUser> userList) {
                super.onRoomUserUpdate(roomID, updateType, userList);

            }

            @Override
            public void onRoomStateUpdate(String roomID, ZegoRoomState state, int errorCode, JSONObject extendedData) {
                super.onRoomStateUpdate(roomID, state, errorCode, extendedData);
            }

            @Override
            public void onRoomStateChanged(String roomID, ZegoRoomStateChangedReason reason, int errorCode, JSONObject extendedData) {
                super.onRoomStateChanged(roomID, reason, errorCode, extendedData);
            }

            @Override
            public void onPublisherStateUpdate(String streamID, ZegoPublisherState state, int errorCode, JSONObject extendedData) {
                super.onPublisherStateUpdate(streamID, state, errorCode, extendedData);
            }

            @Override
            public void onPlayerStateUpdate(String streamID, ZegoPlayerState state, int errorCode, JSONObject extendedData) {
                super.onPlayerStateUpdate(streamID, state, errorCode, extendedData);
            }

        });
    }

    private void stopListenEvent() {
        ZegoExpressEngine.getEngine().setEventHandler(null);
    }

    private void loginRoom() {
        ZegoUser user = new ZegoUser(userId, userName);
        ZegoRoomConfig roomConfig = new ZegoRoomConfig();
        roomConfig.isUserStatusNotify = true;
        ZegoExpressEngine.getEngine().loginRoom(roomId, user, roomConfig, (int error, JSONObject extendData) -> {

            if (error == 0) {
                startPreview();
                startPublishStream();
            }
        });
    }

    //顯示畫面預覽(自己)
    private void startPreview() {
        ZegoCanvas canvas = new ZegoCanvas(binding.ttvMySelf);
        ZegoExpressEngine.getEngine().startPreview(canvas);
    }

    //停止畫面預覽
    private void stopPreview() {
        ZegoExpressEngine.getEngine().stopPreview();
    }


    //推流
    private void startPublishStream() {
        ZegoCanvas canvas = new ZegoCanvas(binding.ttvMySelf);
        ZegoExpressEngine.getEngine().startPreview(canvas);
        String streamId = roomId + "_" + userId;
        ZegoExpressEngine.getEngine().startPublishingStream(streamId);
    }

    //停止推流
    private void stopPublishStream() {
        ZegoExpressEngine.getEngine().stopPublishingStream();
    }

    //播放串流
    private void startPlayStream(String streamId) {
        ZegoCanvas canvas = new ZegoCanvas(binding.ttvRemote);
        ZegoExpressEngine.getEngine().startPlayingStream(streamId, canvas);
    }

    //停止播放串流
    private void stopPlayStream(String streamId) {
        ZegoExpressEngine.getEngine().stopPlayingStream(streamId);
    }

    //翻轉鏡頭方向
    private void onSwitchCameraDirectionListener() {
        binding.sw.setOnClickListener(v -> {
            stopPreview();
            stopPublishStream();

            ZegoExpressEngine.getEngine().useFrontCamera(isFront);
            isFront = !isFront;

            startPreview();
            startPublishStream();
        });
    }

    //膚色增強
    private void adjustmentFaceWhiten() {
        binding.sbWhiteFace.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                binding.tvSbWhiteFace.setText(String.valueOf(progress));
                faceWhiten(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    //平滑肌膚
    private void adjustmentSmoothFace() {
        binding.sbSmoothFace.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                binding.tvSbSmoothFace.setText(String.valueOf(progress));
                smoothFace(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    //腮紅
    private void adjustmentRosy() {
        binding.sbRosy.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                binding.tvSbRosy.setText(String.valueOf(progress));
                rosyEffect(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    //影像銳化
    private void adjustmentSharpen() {
        binding.sbSharpen.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                binding.tvSbSharpen.setText(String.valueOf(progress));
                sharpenEffect(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    //膚色增強
    private void faceWhiten(int progress) {
        effects.enableWhiten(true);
        ZegoEffectsWhitenParam param = new ZegoEffectsWhitenParam();
        param.intensity = progress;
        effects.setWhitenParam(param);
    }
    //平滑肌膚
    private void smoothFace(int progress) {
        effects.enableSmooth(true);
        ZegoEffectsSmoothParam param = new ZegoEffectsSmoothParam();
        param.intensity = progress;
        effects.setSmoothParam(param);
    }
    //腮紅
    private void rosyEffect(int progress) {
        effects.enableRosy(true);
        ZegoEffectsRosyParam param = new ZegoEffectsRosyParam();
        param.intensity = progress;
        effects.setRosyParam(param);
    }
    //影像銳化
    private void sharpenEffect(int progress) {
        effects.enableSharpen(true);
        ZegoEffectsSharpenParam param = new ZegoEffectsSharpenParam();
        param.intensity = progress;
        effects.setSharpenParam(param);
    }

    private void onOpenSetting() {
        binding.swEffectSetting.setOnClickListener(v -> {
            binding.llSetting.setVisibility(isOpen ? View.GONE : View.VISIBLE);
            isOpen = !isOpen;
        });
    }

}