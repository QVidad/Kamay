package com.example.kamay.ui.home;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;
import com.example.kamay.R;
import com.example.kamay.databinding.FragmentHomeBinding;

import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Gravity;
import android.view.SurfaceView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Scroller;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

public class HomeFragment extends Fragment implements CameraBridgeViewBase.CvCameraViewListener2{
    private JavaCameraView openCvCameraView;
    private FragmentHomeBinding binding;
    Mat mRGBA, mRGBAT;
    private static String TAG = "MainActivity" ;
    private int CAMERA_CODE = 100;

    private BaseLoaderCallback baseloadercallback = new BaseLoaderCallback(this.getActivity()) {
        @Override
        public void onManagerConnected(int status) {
            super.onManagerConnected(status);
            if (status == BaseLoaderCallback.SUCCESS)
                openCvCameraView.enableView();
            else
                super.onManagerConnected(status);
        }
    };

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        HomeViewModel homeViewModel =
                new ViewModelProvider(this).get(HomeViewModel.class);

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        if(!Python.isStarted()){
            Python.start(new AndroidPlatform(getContext()));
        }
        Python py = Python.getInstance();
        PyObject pyobj =py.getModule("script");
        PyObject obj = pyobj.callAttr("main");

        binding.translatedText.setText(obj.toString());

        openCvCameraView = (JavaCameraView) root.findViewById(R.id.my_camera_view);
        openCvCameraView.setVisibility(SurfaceView.VISIBLE);
        openCvCameraView.setCvCameraViewListener(this);

        if (ContextCompat.checkSelfPermission(HomeFragment.this.getContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, CAMERA_CODE);
        }

        return root;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(HomeFragment.this.getContext(), "camera permission granted", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(HomeFragment.this.getContext(), "camera permission denied", Toast.LENGTH_LONG).show();
            }
        }
    }
    private void requestCameraPermission(){
        if(ActivityCompat.shouldShowRequestPermissionRationale(this.getActivity(), Manifest.permission.CAMERA)){
            new AlertDialog.Builder(this.getActivity())
                    .setTitle("Permission Needed")
                    .setMessage("Camera permission is needed to use Kamay.")
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                           
                        }
                    })
                    .setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
                        }
                    })
                    .create().show();
        } else {
            ActivityCompat.requestPermissions(this.getActivity(), new String[] {Manifest.permission.CAMERA}, CAMERA_CODE);
        }
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        mRGBA = new Mat(height, width, CvType.CV_8UC4);
    }

    @Override
    public void onCameraViewStopped() {
        mRGBA.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mRGBA = inputFrame.rgba();
        mRGBAT = mRGBA.t();
        Core.flip(mRGBA.t(), mRGBAT, 1);
        Imgproc.resize(mRGBA.t(), mRGBAT, mRGBA.size());
        return mRGBAT;
    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        if (openCvCameraView != null) {
            openCvCameraView.disableView();
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        if (openCvCameraView != null) {
            openCvCameraView.disableView();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if(OpenCVLoader.initDebug()){
            Log.d(TAG,"OPENCV INSTALLED SUCCESSFULLY");
            baseloadercallback.onManagerConnected(BaseLoaderCallback.SUCCESS);
        } else {
            Log.d(TAG,"OPENCV IS NOT INSTALLED");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this.getActivity(), baseloadercallback);
        }
    }
}