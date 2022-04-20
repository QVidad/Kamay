package com.example.kamay.ui.home;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.kamay.HandsResultGlRenderer;
import com.example.kamay.HandsResultImageView;
import com.example.kamay.databinding.FragmentHomeBinding;

import android.content.Intent;
import android.util.Log;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
// ContentResolver dependency
import com.google.mediapipe.formats.proto.LandmarkProto.Landmark;
import com.google.mediapipe.formats.proto.LandmarkProto.NormalizedLandmark;
import com.google.mediapipe.solutioncore.CameraInput;
import com.google.mediapipe.solutioncore.SolutionGlSurfaceView;
import com.google.mediapipe.solutions.hands.HandLandmark;
import com.google.mediapipe.solutions.hands.Hands;
import com.google.mediapipe.solutions.hands.HandsOptions;
import com.google.mediapipe.solutions.hands.HandsResult;

public class HomeFragment extends Fragment {
    private static String TAG = "HomeFragment" ;
    private FragmentHomeBinding binding;
    private int CAMERA_CODE = 100;

    private Hands hands;
    // Run the pipeline and the model inference on GPU or CPU.
    private static final boolean RUN_ON_GPU = true;
    private enum InputSource {
        UNKNOWN,
        CAMERA,
    }
    private InputSource inputSource = InputSource.UNKNOWN;

    // Image demo UI and image loader components.
    private HandsResultImageView imageView;
    // Video demo UI and video loader components.
    private ActivityResultLauncher<Intent> videoGetter;
    // Live camera demo UI and camera components.
    private CameraInput cameraInput;

    private SolutionGlSurfaceView<HandsResult> glSurfaceView;
    private Button button;

    public HomeFragment(){
        Log.i(TAG,"Instantiated new "+this.getClass());
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        HomeViewModel homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        button = binding.cameraButton;
        checkPermission();

        return root;
    }

    public void checkPermission(){
        if (ContextCompat.checkSelfPermission(HomeFragment.this.getContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED) {
            binding.camera.setText("This feature needs to use the camera. Please give permission to the application to access your phone camera.");
            requestPermissions(new String[]{Manifest.permission.CAMERA}, CAMERA_CODE);
            button.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    requestPermissions(new String[] { Manifest.permission.CAMERA }, CAMERA_CODE);
                }
            });
        } else {
            imageView = new HandsResultImageView(this.getContext());
            setupLiveDemoUiComponents();
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(HomeFragment.this.getContext(), "Camera Permission Granted", Toast.LENGTH_LONG).show();
                imageView = new HandsResultImageView(this.getContext());
                setupLiveDemoUiComponents();
            } else {
                Toast.makeText(HomeFragment.this.getContext(), "Camera Permission Denied", Toast.LENGTH_LONG).show();
            }
        }
    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
    @Override
    public void onPause() {
        super.onPause();
        glSurfaceView.setVisibility(View.GONE);
        cameraInput.close();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Restarts the camera and the opengl surface rendering.
        cameraInput = new CameraInput(this.getActivity());
        cameraInput.setNewFrameListener(textureFrame -> hands.send(textureFrame));
        glSurfaceView.post(this::startCamera);
        glSurfaceView.setVisibility(View.VISIBLE);
    }


    /** Sets up the UI components for the live demo with camera input. */
    private void setupLiveDemoUiComponents() {
        stopCurrentPipeline();
        setupStreamingModePipeline(InputSource.CAMERA);
    }

    /** Sets up core workflow for streaming mode. */
    private void setupStreamingModePipeline(InputSource inputSource) {
        this.inputSource = inputSource;
        // Initializes a new MediaPipe Hands solution instance in the streaming mode.
        hands =
                new Hands(
                        this.getContext(),
                        HandsOptions.builder()
                                .setStaticImageMode(false)
                                .setMaxNumHands(2)
                                .setRunOnGpu(RUN_ON_GPU)
                                .build());
        hands.setErrorListener((message, e) -> Log.e(TAG, "MediaPipe Hands error:" + message));

            cameraInput = new CameraInput(this.getActivity());
            cameraInput.setNewFrameListener(textureFrame -> hands.send(textureFrame));


        // Initializes a new Gl surface view with a user-defined HandsResultGlRenderer.
        glSurfaceView =
                new SolutionGlSurfaceView(this.getContext(), hands.getGlContext(), hands.getGlMajorVersion());
        glSurfaceView.setSolutionResultRenderer(new HandsResultGlRenderer());
        glSurfaceView.setRenderInputImage(true);
        hands.setResultListener(
                handsResult -> {
                    logWristLandmark(handsResult, /*showPixelValues=*/ false);
                    glSurfaceView.setRenderData(handsResult);
                    glSurfaceView.requestRender();
                });

        // The runnable to start camera after the gl surface view is attached.
        // For video input source, videoInput.start() will be called when the video uri is available.
        if (inputSource == InputSource.CAMERA) {
            glSurfaceView.post(this::startCamera);
        }

        // Updates the preview layout.
        FrameLayout frameLayout = binding.previewDisplayLayout;
        imageView.setVisibility(View.GONE);
        frameLayout.removeAllViewsInLayout();
        frameLayout.addView(glSurfaceView);
        glSurfaceView.setVisibility(View.VISIBLE);
        frameLayout.requestLayout();
    }

    private void startCamera() {
        cameraInput.start(
                this.getActivity(),
                hands.getGlContext(),
                CameraInput.CameraFacing.BACK,
                glSurfaceView.getWidth(),
                glSurfaceView.getHeight());
        Toast.makeText(HomeFragment.this.getContext(), "Scanning", Toast.LENGTH_SHORT).show();
    }

    private void stopCurrentPipeline() {
        if (cameraInput != null) {
            cameraInput.setNewFrameListener(null);
            cameraInput.close();
        }
        if (glSurfaceView != null) {
            glSurfaceView.setVisibility(View.GONE);
        }
        if (hands != null) {
            hands.close();
        }
    }

    private void logWristLandmark(HandsResult result, boolean showPixelValues) {
        if (result.multiHandLandmarks().isEmpty()) {
            return;
        }
        NormalizedLandmark wristLandmark =
                result.multiHandLandmarks().get(0).getLandmarkList().get(HandLandmark.WRIST);
        // For Bitmaps, show the pixel values. For texture inputs, show the normalized coordinates.
        if (showPixelValues) {
            int width = result.inputBitmap().getWidth();
            int height = result.inputBitmap().getHeight();
            Log.i(
                    TAG,
                    String.format(
                            "MediaPipe Hand wrist coordinates (pixel values): x=%f, y=%f",
                            wristLandmark.getX() * width, wristLandmark.getY() * height));
        } else {
            Log.i(
                    TAG,
                    String.format(
                            "MediaPipe Hand wrist normalized coordinates (value range: [0, 1]): x=%f, y=%f",
                            wristLandmark.getX(), wristLandmark.getY()));
        }
        if (result.multiHandWorldLandmarks().isEmpty()) {
            return;
        }
        Landmark wristWorldLandmark =
                result.multiHandWorldLandmarks().get(0).getLandmarkList().get(HandLandmark.WRIST);
        Log.i(
                TAG,
                String.format(
                        "MediaPipe Hand wrist world coordinates (in meters with the origin at the hand's"
                                + " approximate geometric center): x=%f m, y=%f m, z=%f m",
                        wristWorldLandmark.getX(), wristWorldLandmark.getY(), wristWorldLandmark.getZ()));
    }
}