package com.castrelos.tixola.fragment;

import android.Manifest;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.graphics.Rect;
import android.media.Image;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AlertDialog;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.extensions.HdrImageCaptureExtender;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LifecycleOwner;
import androidx.navigation.Navigation;

import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.OrientationEventListener;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

import com.castrelos.tixola.R;
import com.castrelos.tixola.databinding.FragmentCameraBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link fragment_on_camera#newInstance} factory method to
 * create an instance of this fragment.
 */
public class fragment_on_camera extends Fragment {

    private PreviewView previewView;
    private Executor executor = Executors.newSingleThreadExecutor();
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private FragmentCameraBinding binding;
    private FloatingActionButton takePictureButton;
    private Bundle mBundleArgs;
    public TextRecognizer mTextRecognizer;

    // Permision references
    public static final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 123;

    boolean retorno = false;

    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public fragment_on_camera() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment fragment_on_camera.
     */
    // TODO: Rename and change types and number of parameters
    public static fragment_on_camera newInstance(String param1, String param2) {
        fragment_on_camera fragment = new fragment_on_camera();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mBundleArgs = new Bundle();
        mTextRecognizer =
                TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        binding = FragmentCameraBinding.inflate(inflater, container, false);

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_on_camera, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        if(checkPermissionREAD_EXTERNAL_STORAGE(view.getContext())){
            previewView = view.findViewById(R.id.previewView);
            takePictureButton = view.findViewById(R.id.floating_action_button);

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
        }

        super.onViewCreated(view, savedInstanceState);
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

        cameraProvider.bindToLifecycle(
                (LifecycleOwner)this, cameraSelector, imageAnalysis, preview);

        final ImageCapture imageCapture = new ImageCapture.Builder()
                .setTargetRotation(requireView().getDisplay().getRotation())
                .build();
        cameraProvider.bindToLifecycle(
                (LifecycleOwner)this, cameraSelector, imageCapture, imageAnalysis, preview);

        // OnClick function
        takePictureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                /*
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
                 */

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
                                    }else {
                                        Log.i("Render Success", "Rendering was NOT successfull");
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

    public boolean checkPermissionREAD_EXTERNAL_STORAGE(
            final Context context) {
        int currentAPIVersion = Build.VERSION.SDK_INT;
        if (currentAPIVersion >= android.os.Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(context,
                    Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(
                        (Activity) context,
                        Manifest.permission.READ_EXTERNAL_STORAGE)) {
                    showDialog("External storage", context,
                            Manifest.permission.READ_EXTERNAL_STORAGE);

                } else {
                    ActivityCompat
                            .requestPermissions(
                                    (Activity) context,
                                    new String[] { Manifest.permission.READ_EXTERNAL_STORAGE },
                                    MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
                }
                return false;
            } else {
                return true;
            }

        } else {
            return true;
        }
    }

    public void customMaterialDialog(final String msg, final Context context, final List<String> lista){
        CharSequence[] arrayList = lista.toArray(new CharSequence[lista.size()]);
        //String[] singleChoice = {"Alejandro", "Manuel", "Lopez", "Rosales"};

        for (CharSequence msag : arrayList){
            Log.e("Msg", msag.toString());
        }

        new MaterialAlertDialogBuilder(requireActivity(), R.style.AlertDialogTheme)
                .setTitle("Un momento...")
                .setSingleChoiceItems(arrayList, 0, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        CharSequence selec = arrayList[i];
                        mBundleArgs.putString("name", selec.toString());

                    }
                })
                .setPositiveButton("GOT IT", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        //mBundleArgs.putString("name", mSpinner.getSelectedItem().toString());
                        Navigation.findNavController(requireActivity(),
                                R.id.fragmentContainerView).navigate(R.id.newPointFragment, mBundleArgs);

                        dialogInterface.dismiss();
                    }
                })
                .setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                })
                .show();
    }

    public void customDialog(final String msg, final Context context, final ArrayList<String> lista){
        AlertDialog.Builder customizedDialog = new AlertDialog.Builder(context, R.style.CustomDialog);
        View mView = getLayoutInflater().inflate(R.layout.dialog_custom_with_spinner, null);
        Spinner mSpinner = mView.findViewById(R.id.dialog_custom_spinner);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                context,
                android.R.layout.simple_list_item_1,
                lista);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinner.setAdapter(adapter);

        customizedDialog.setTitle("Un momento...");
        customizedDialog.setCancelable(false);
        customizedDialog.setMessage(msg)
                .setPositiveButton(
                    android.R.string.yes,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            mBundleArgs.putString("name", mSpinner.getSelectedItem().toString());
                            Log.e("OnCamera", "CustomDialog says " + retorno);

                            Navigation.findNavController(requireActivity(),
                                    R.id.fragmentContainerView).navigate(R.id.newPointFragment, mBundleArgs);
                        }
                })

                .setNegativeButton(
                    android.R.string.no,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {

                        }
                });

        customizedDialog.setView(mView);
        AlertDialog dialog = customizedDialog.create();
        dialog.show();
    }

    public void showDialog(final String msg, final Context context,
                           final String permission) {
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(context);
        alertBuilder.setCancelable(true);
        alertBuilder.setTitle("Permission necessary");
        alertBuilder.setMessage(msg + " permission is necessary");
        alertBuilder.setPositiveButton(android.R.string.yes,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        ActivityCompat.requestPermissions((Activity) context,
                                new String[] { permission },
                                MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
                    }
                });
        AlertDialog alert = alertBuilder.create();
        alert.show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults)
    {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.e("Permisos", "Persmisos garantizados");
                } else {
                    Log.e("Permisos", "Persmisos NO garantizados");
                    Toast.makeText(requireContext(), "GET_ACCOUNTS Denied",
                            Toast.LENGTH_SHORT).show();
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions,
                        grantResults);
        }
    }

    private boolean renderCaptureImage(InputImage renderImage){
        List<String> opcName = new ArrayList<>();

        String regexName = "S.*L.*$";
        String regexCIF = "B-|C.I.F|CIF|NIF|N.I.F";
        String regexCorreo = "[A-Za-z]+@[a-z]+\\.[a-z]+";
        String regexCodPostal = "^\\d{5} \\w.*";
        String regexTel = "\\+34 9[0-9]{1,2} [0-9]{7}";
        String regexTelAlt1 = "9[0-9]{1,2}[0-9] [0-9]{6}";
        String regexTelAlt2 = "9[0-9]{1,2}[0-9] [0-9]{2} [0-9]{2} [0-9]{2}";
        String regexTelAlt3 = "9[0-9]{1,2}[0-9]{7}";
        String regexTelAlt4 = "9[0-9]{1,2}[0-9] [0-9]{3} [0-9]{3}";


        Task<Text> result = mTextRecognizer.process(renderImage)
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
                                        opcName.add(lineText);
                                        Log.i("Block Text Image", "BlockText Negative : " + lineText);
                                    }
                                }
                                if (cont <= 6){
                                    if (nif) {
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
                                        opcName.add(lineText);
                                        Log.i("Block Text Image", "BlockText Negative : " + lineText);
                                    }
                                }else {
                                    // Resto de lineas de la factura sin valor alguno para almacenar
                                    Log.i("Block Text Image", "BlockText EOL ");
                                }

                                for (Text.Element element : line.getElements()) {
                                    String elementText = element.getText();
                                    Point[] elementCornerPoints = element.getCornerPoints();
                                    Rect elementFrame = element.getBoundingBox();

                                    //Log.i("Element Text", "Element Text: " + elementText);
                                }
                            }

                        }
                    }
                })
                .addOnCompleteListener(new OnCompleteListener<Text>() {
                    @Override
                    public void onComplete(@NonNull Task<Text> task) {
                        //customDialog("Posibles matches", requireContext(), lista);
                        customMaterialDialog("Posibles nombres", requireContext(), opcName);
                    }
                })
                .addOnFailureListener(
                        e -> Log.e("RenderCapture", "An error occurred " + e)
                );

        return result.isSuccessful();
    }
}