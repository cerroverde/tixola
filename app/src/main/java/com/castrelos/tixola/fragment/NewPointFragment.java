package com.castrelos.tixola.fragment;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.castrelos.tixola.R;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomappbar.BottomAppBar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;


import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Objects;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link NewPointFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class NewPointFragment extends Fragment {
    public TextRecognizer mTextRecognizer;
    public int iconFloat;

    // CameraX
    private static final String[] CAMERA_PERMISSION = new String[]{Manifest.permission.CAMERA};
    private static final int CAMERA_REQUEST_CODE = 10;


    /**
     * Variables para el swicth del NavegationView Menu
     */
    private static final int vTakePicture = R.id.menu_newpoint_camera;
    private static final int vUploadFile = R.id.menu_newpoint_upload_file;


    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";


    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public NewPointFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment NewPointFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static NewPointFragment newInstance(String param1, String param2) {
        NewPointFragment fragment = new NewPointFragment();
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

        mTextRecognizer =
                TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_new_point, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        final FloatingActionButton floatingActionButton = view.findViewById(R.id.floatingBtnNewPoint);
        final BottomAppBar bottomAppBar = (BottomAppBar) view.findViewById(R.id.bottomAppBar);
        final TextInputLayout comercioName = (TextInputLayout) view.findViewById(R.id.newpoint_ti_local);
        final TextInputLayout comercioAddress = (TextInputLayout) view.findViewById(R.id.newpoint_ti_address);
        final TextInputLayout comercioTel = (TextInputLayout) view.findViewById(R.id.newpoint_ti_tel);

        if (getArguments() != null) {
            /*
             * Se elimina por ser solo de uso para desarrollo
             * Aqui se mostraba la foto de la factura tomada
             *
            invoice.setImageURI(Uri.parse(getArguments().getString("savedUri")));
            invoice.setMinimumWidth(imageWidth);
            invoice.setMinimumHeight(imageHeight);
             */

            // Get all arguments from Bundle
            String mName = getArguments().getString("name");
            String mAddress = getArguments().getString("address");
            String mTel = getArguments().getString("telephone");

            // Setting text
            Objects.requireNonNull(comercioName.getEditText()).setText(mName);
            Objects.requireNonNull(comercioAddress.getEditText()).setText(mAddress);
            Objects.requireNonNull(comercioTel.getEditText()).setText(mTel);

            // Icono de salvar el nuevo point
            iconFloat = R.drawable.ic_save;
            floatingActionButton.setImageResource(iconFloat);

        }else{
            // Icono para tomar la foto a la factura solo si no hay ningun dato en el Bundle
            iconFloat = R.drawable.ic_camera;
            floatingActionButton.setImageResource(iconFloat);
        }

        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(iconFloat == R.drawable.ic_camera) {
                    if (hasCameraPermission()) {
                        enableCamera();
                    } else {
                        requestPermission();
                    }
                }else{
                    Toast.makeText(getContext(),"Nuevo point guardado", Toast.LENGTH_LONG).show();
                }
            }
        });

        bottomAppBar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Handle navigation icon press
            }
        });
        bottomAppBar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                int menuID = item.getItemId();
                switch (menuID){
                    case vTakePicture:
                        if (hasCameraPermission()) {
                            enableCamera();
                        } else {
                            requestPermission();
                        }
                        break;

                    case vUploadFile:
                        getPdfFile();
                        break;
                }
                return false;
            }
        });
    }

    // You can do the assignment inside onAttach or onCreate, i.e, before the activity is displayed
    ActivityResultLauncher<Intent> getActivityResult = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        // There are no request codes
                        Intent data = result.getData();
                        dumpImageMetaData(result.getData().getData());

                        try {
                            renderPDF(data.getData());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                    }
                }
            });

    public void getPdfFile() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/pdf");

        getActivityResult.launch(intent);
    }

    public void dumpImageMetaData(Uri uri) {

        // The query, because it only applies to a single document, returns only
        // one row. There's no need to filter, sort, or select fields,
        // because we want all fields for one document.
        Cursor cursor = getActivity().getContentResolver()
                .query(uri, null, null, null, null, null);

        try {
            // moveToFirst() returns false if the cursor has 0 rows. Very handy for
            // "if there's anything to look at, look at it" conditionals.
            if (cursor != null && cursor.moveToFirst()) {

                // Note it's called "Display Name". This is
                // provider-specific, and might not necessarily be the file name.
                @SuppressLint("Range") String displayName = cursor.getString(
                        cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                Log.i("URI", "Display Name: " + displayName);

                int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
                // If the size is unknown, the value stored is null. But because an
                // int can't be null, the behavior is implementation-specific,
                // and unpredictable. So as
                // a rule, check if it's null before assigning to an int. This will
                // happen often: The storage API allows for remote files, whose
                // size might not be locally known.
                String size = null;
                if (!cursor.isNull(sizeIndex)) {
                    // Technically the column stores an int, but cursor.getString()
                    // will do the conversion automatically.
                    size = cursor.getString(sizeIndex);
                } else {
                    size = "Unknown";
                }
                Log.i("URI", "Size: " + size);
            }
        } finally {
            cursor.close();
        }
    }

    public void renderPDF(Uri uri) throws IOException {

        PdfRenderer pdfRenderer = new PdfRenderer( fileDescriptor(uri,"r", requireContext()) );
        final int pageCount = pdfRenderer.getPageCount();
        int rotationDegree = 0;

        for (int i = 0; i < pageCount; i++){
            PdfRenderer.Page page = pdfRenderer.openPage(i);
            // Ha mayor dimensiones, mejor los resultados de lectura
            Bitmap pdfBitmap = Bitmap.createBitmap(1200, 1600, Bitmap.Config.ARGB_8888);
            page.render(pdfBitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);

            InputImage image = InputImage.fromBitmap(pdfBitmap, rotationDegree);

            Task<Text> result =
                    mTextRecognizer.process(image)
                            .addOnSuccessListener(new OnSuccessListener<Text>() {
                                @Override
                                public void onSuccess(Text visionText) {
                                    String resultText = visionText.getText();
                                    for (Text.TextBlock block : visionText.getTextBlocks()) {
                                        String blockText = block.getText();
                                        Point[] blockCornerPoints = block.getCornerPoints();
                                        Rect blockFrame = block.getBoundingBox();

                                        Log.i("Block Text", "Block Text: " + blockText);

                                        for (Text.Line line : block.getLines()) {
                                            String lineText = line.getText();
                                            Point[] lineCornerPoints = line.getCornerPoints();
                                            Rect lineFrame = line.getBoundingBox();

                                            //Log.i("Line Text", "Line Text: " + lineText);

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
                            .addOnFailureListener(
                                    new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            // Task failed with an exception
                                            // ...
                                        }
                                    });
            page.close();
        }
    }

    public static ParcelFileDescriptor fileDescriptor(Uri uri, String mode, Context context)
            throws IOException {

        return context.getContentResolver().openFileDescriptor(uri, mode);
    }


    // CAMERA PERMISSION
    private boolean hasCameraPermission() {
        return ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermission() {
        ActivityCompat.requestPermissions(
                requireActivity(),
                CAMERA_PERMISSION,
                CAMERA_REQUEST_CODE
        );
    }

    private void enableCamera() {
        Navigation.findNavController(
                requireActivity(),
                R.id.fragmentContainerView).navigate(R.id.fragment_on_camera);
    }

}