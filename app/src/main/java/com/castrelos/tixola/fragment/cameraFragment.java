package com.castrelos.tixola.fragment;

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.extensions.HdrImageCaptureExtender;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LifecycleOwner;
import androidx.navigation.Navigation;

import android.app.Activity;
import android.content.ContentValues;
import android.content.ContextWrapper;
import android.graphics.Point;
import android.graphics.Rect;
import android.media.Image;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.castrelos.tixola.R;
import com.castrelos.tixola.databinding.FragmentCameraBinding;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class cameraFragment extends Fragment{
    /*
     *  Initiate instance variables
     */
    private Bundle mBundleArgs;
    private PreviewView previewView;
    private ImageView captureImage;

    private Executor executor = Executors.newSingleThreadExecutor();
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private TextView textView;
    private Button mDummyButton;

    public TextRecognizer mTextRecognizer;

    /**
     * Whether or not the system UI should be auto-hidden after
     * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
     */
    private static final boolean AUTO_HIDE = false;

    /**
     * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
     * user interaction before hiding the system UI.
     */
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

    /**
     * Some older devices needs a small delay between UI widget updates
     * and a change of the status and navigation bar.
     */
    private static final int UI_ANIMATION_DELAY = 300;
    private final Handler mHideHandler = new Handler();
    private final Runnable mHidePart2Runnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            // Delayed removal of status and navigation bar

            // Note that some of these constants are new as of API 16 (Jelly Bean)
            // and API 19 (KitKat). It is safe to use them, as they are inlined
            // at compile-time and do nothing on earlier devices.
            int flags = View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;

            Activity activity = getActivity();
            if (activity != null
                    && activity.getWindow() != null) {
                activity.getWindow().getDecorView().setSystemUiVisibility(flags);
            }
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.hide();
            }

        }
    };

    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    private final View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if (AUTO_HIDE) {
                delayedHide(AUTO_HIDE_DELAY_MILLIS);
            }

            return false;
        }
    };
    private View mContentView;
    private View mControlsView;
    private final Runnable mShowPart2Runnable = new Runnable() {
        @Override
        public void run() {
            // Delayed display of UI elements
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.show();
            }
            mControlsView.setVisibility(View.VISIBLE);
        }
    };
    private boolean mVisible;
    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };
    private FragmentCameraBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {


        mBundleArgs = new Bundle();
        mTextRecognizer =
                TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        binding = FragmentCameraBinding.inflate(inflater, container, false);

        return binding.getRoot();

    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mVisible = true;

        previewView = view.findViewById(R.id.previewView);
        textView = view.findViewById(R.id.orientation);
        mDummyButton = view.findViewById(R.id.dummy_button);

        mControlsView = binding.fullscreenContentControls;
        mContentView = binding.fullscreenContent;


        cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext());
        cameraProviderFuture.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                    bindImageAnalysis(cameraProvider);
                } catch (ExecutionException | InterruptedException e){
                    e.printStackTrace();
                }
            }
        }, ContextCompat.getMainExecutor(requireContext()));

        // Set up the user interaction to manually show or hide the system UI.
        mContentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggle();
            }
        });

        // Upon interacting with UI controls, delay any scheduled hide()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.
        binding.dummyButton.setOnTouchListener(mDelayHideTouchListener);


    }

    private void DrawFocusRect(int color){
        DisplayMetrics displayMetrics = new DisplayMetrics();
        
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() != null && getActivity().getWindow() != null) {
            getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        }

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (getActivity() != null && getActivity().getWindow() != null) {
            getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);

            // Clear the systemUiVisibility flag
            getActivity().getWindow().getDecorView().setSystemUiVisibility(0);
        }
        show();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mContentView = null;
        mControlsView = null;
    }

    private void toggle() {
        if (mVisible) {
            hide();
        } else {
            show();
        }
    }

    private void hide() {
        // Hide UI first
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        mControlsView.setVisibility(View.GONE);
        mVisible = false;

        // Schedule a runnable to remove the status and navigation bar after a delay
        mHideHandler.removeCallbacks(mShowPart2Runnable);
        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY);
    }

    @SuppressLint("InlinedApi")
    private void show() {
        // Show the system bar
        mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        mVisible = true;

        // Schedule a runnable to display UI elements after a delay
        mHideHandler.removeCallbacks(mHidePart2Runnable);
        mHideHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.show();
        }
    }

    /**
     * Schedules a call to hide() in delay milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }

    @Nullable
    private ActionBar getSupportActionBar() {
        ActionBar actionBar = null;
        if (getActivity() instanceof AppCompatActivity) {
            AppCompatActivity activity = (AppCompatActivity) getActivity();
            actionBar = activity.getSupportActionBar();
        }
        return actionBar;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void bindImageAnalysis(@NonNull ProcessCameraProvider cameraProvider) {
        // Set Image Analysis
        ImageAnalysis imageAnalysis =
                new ImageAnalysis.Builder().setTargetResolution(new Size(1280, 1488))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build();

        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(requireContext()), new ImageAnalysis.Analyzer() {
            @Override
            public void analyze(@NonNull ImageProxy image) {
                //image.close();

            }
        });

        OrientationEventListener orientationEventListener = new OrientationEventListener(requireContext()) {
            @Override
            public void onOrientationChanged(int orientation) {
                textView.setText(Integer.toString(orientation));
            }
        };
        orientationEventListener.enable();

        // Set a preview
        Preview preview = new Preview.Builder().build();
        // Set ImageCapture Builder
        ImageCapture.Builder builder = new ImageCapture.Builder();
        // Set Camera Selector
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK).build();

        //Vendor-Extensions (The CameraX extensions dependency in build.gradle)
        HdrImageCaptureExtender hdrImageCaptureExtender = HdrImageCaptureExtender.create(builder);
        // Query if extension is available (optional).
        if (hdrImageCaptureExtender.isExtensionAvailable(cameraSelector)){
            // Enable the extension if available.
            hdrImageCaptureExtender.enableExtension(cameraSelector);
        }

        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        cameraProvider.bindToLifecycle((LifecycleOwner)this, cameraSelector,
                imageAnalysis, preview);

        final ImageCapture imageCapture = new ImageCapture.Builder()
                .setTargetRotation(requireView().getDisplay().getRotation())
                .build();
        cameraProvider.bindToLifecycle(
                (LifecycleOwner)this, cameraSelector, imageCapture, imageAnalysis, preview);

        // OnClick function
        mDummyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                SimpleDateFormat mDateFormat =
                        new SimpleDateFormat("yyyyMMddHHmmss", Locale.US);
                File file = new File(
                        getBatchDirectoryName(), mDateFormat.format(new Date()) + ".jpg"
                );

                //MediaStore
                final ContentValues contentValues = new ContentValues();
                contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, mDateFormat.format(new Date()));
                contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");

                ImageCapture.OutputFileOptions outputFileOptions =
                        new ImageCapture.OutputFileOptions.Builder(
                                requireContext().getContentResolver(),
                                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                contentValues
                        ).build();

                imageCapture.takePicture(outputFileOptions,
                        executor, new ImageCapture.OnImageSavedCallback() {
                            @Override
                            public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                                Log.i("CameraX", "Image saved in " + file.getPath());
                                Log.i("CameraX", "OutputFileResults " + outputFileResults.getSavedUri());

                                mBundleArgs.putString("savedUri",
                                        String.valueOf(outputFileResults.getSavedUri()));
                            }

                            @Override
                            public void onError(@NonNull ImageCaptureException exception) {
                                exception.printStackTrace();
                            }


                        });

                imageCapture.takePicture(executor,
                        new ImageCapture.OnImageCapturedCallback(){
                            @Override
                            @OptIn(markerClass = androidx.camera.core.ExperimentalGetImage.class)
                            public void onCaptureSuccess(@NonNull ImageProxy imageProxy) {
                                super.onCaptureSuccess(imageProxy);

                                Image mediaImage = imageProxy.getImage();
                                if (mediaImage != null) {
                                    InputImage image =
                                            InputImage.fromMediaImage(
                                                    mediaImage,
                                                    imageProxy.getImageInfo().getRotationDegrees()
                                            );

                                    // Render the image
                                    if (renderCaptureImage(image)){
                                        Log.i("Render Success", "Rendering was successfull");
                                        imageProxy.close();
                                    }else{

                                    }
                                }
                            }
                        });
            }
        });
    }

    public File getBatchDirectoryName() {
        ContextWrapper contextWrapper = new ContextWrapper(requireContext());
        File imageDirectory = contextWrapper.getExternalFilesDir(Environment.DIRECTORY_PICTURES);

        Log.i("Directorio", "BatchDirectory " + imageDirectory);

        return imageDirectory;
    }

    private boolean renderCaptureImage(InputImage renderImage){
        String regexName = "S.*L.*$";
        String regexCIF = "B-|C.I.F|CIF|NIF|N.I.F";
        String regexCorreo = "[A-Za-z]+@[a-z]+\\.[a-z]+";
        String regexCodPostal = "^\\d{5} \\w.*";
        String regexTel = "\\+34 9[0-9]{1,2} [0-9]{7}";
        String regexTelAlt1 = "9[0-9]{1,2}[0-9] [0-9]{6}";
        String regexTelAlt2 = "9[0-9]{1,2}[0-9] [0-9]{2} [0-9]{2} [0-9]{2}";
        String regexTelAlt3 = "9[0-9]{1,2}[0-9]{7}";
        String regexTelAlt4 = "9[0-9]{1,2}[0-9] [0-9]{3} [0-9]{3}";


        Task<Text> result =
                mTextRecognizer.process(renderImage)
                    .addOnSuccessListener(new OnSuccessListener<Text>() {
                        @Override
                        public void onSuccess(@NonNull Text visionText) {
                            int cont = 0;
                            for (Text.TextBlock block : visionText.getTextBlocks()) {
                                String blockText = block.getText();
                                Point[] blockCornerPoints = block.getCornerPoints();
                                Rect blockFrame = block.getBoundingBox();

                                for (Text.Line line : block.getLines()) {
                                    cont++;
                                    String lineText = line.getText();
                                    Point[] lineCornerPoints = line.getCornerPoints();
                                    Rect lineFrame = line.getBoundingBox();

                                    boolean name = Pattern.compile(regexName).matcher(lineText).find();
                                    boolean nif = Pattern.compile(regexCIF).matcher(lineText).find();
                                    boolean postal = Pattern.compile(regexCodPostal).matcher(lineText).find();
                                    boolean correo = Pattern.compile(regexCorreo).matcher(lineText).find();
                                    boolean tel = Pattern.compile(regexTel).matcher(lineText).find();
                                    boolean telAlt = Pattern.compile(regexTelAlt1).matcher(lineText).find();
                                    boolean telAlt2 = Pattern.compile(regexTelAlt2).matcher(lineText).find();
                                    boolean telAlt3 = Pattern.compile(regexTelAlt3).matcher(lineText).find();
                                    boolean telAlt4 = Pattern.compile(regexTelAlt4).matcher(lineText).find();

                                    if (cont == 1){
                                        if (name){
                                            mBundleArgs.putString("name", lineText);
                                            Log.i("Block Text Image", " BlockText Positive: " + lineText);
                                        }else{
                                            //TODO se puede hacer un Dialog pregunstando el nombre del comercio
                                        }
                                    }
                                    if (cont <= 6){
                                        if (name){
                                            mBundleArgs.putString("name", lineText);
                                            Log.i("Block Text Image", " BlockText Positive: " + lineText);
                                        }else if (nif) {
                                            mBundleArgs.putString("nif", lineText);
                                            Log.i("Block Text Image", " BlockText Positive: " + lineText);
                                        }else if (postal) {
                                            mBundleArgs.putString("address", lineText);
                                            Log.i("Block Text Image", " BlockText Positive: " + lineText);
                                        }else if(correo){
                                            mBundleArgs.putString("correo", lineText);
                                            Log.i("Block Text Image", " BlockText Positive: " + lineText);
                                        }else if (tel) {
                                            mBundleArgs.putString("telephone", lineText);
                                            Log.i("Block Text Image", " BlockText Positive: " + lineText);
                                        }else if(telAlt){
                                            mBundleArgs.putString("telephone", lineText);
                                            Log.i("Block Text Image", " BlockText Positive: " + lineText);
                                        }else if(telAlt2){
                                            mBundleArgs.putString("telephone", lineText);
                                            Log.i("Block Text Image", " BlockText Positive: " + lineText);
                                        }else if(telAlt3){
                                            mBundleArgs.putString("telephone", lineText);
                                            Log.i("Block Text Image", " BlockText Positive: " + lineText);
                                        }else if(telAlt4){
                                            mBundleArgs.putString("telephone", lineText);
                                            Log.i("Block Text Image", " BlockText Positive: " + lineText);
                                        }else {
                                            Log.i("Block Text Image", "BlockText Negative : " + lineText);
                                        }
                                    }else {
                                        Log.i("Block Text Image", "BlockText EOL ");
                                    }




                                    //Log.i("Line Text", "Line Text: " + lineText);

                                    for (Text.Element element : line.getElements()) {
                                        String elementText = element.getText();
                                        Point[] elementCornerPoints = element.getCornerPoints();
                                        Rect elementFrame = element.getBoundingBox();

                                        //Log.i("Element Text", "Element Text: " + elementText);
                                    }
                                }
                            }
                            Navigation.findNavController(requireActivity(),
                                    R.id.fragmentContainerView).navigate(R.id.newPointFragment, mBundleArgs);
                        }
                    })
                    .addOnFailureListener(
                            new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Log.e("RenderCapture", "An error occurred " + e);
                                }
                            }
                    );

        return result.isSuccessful();
    }
}