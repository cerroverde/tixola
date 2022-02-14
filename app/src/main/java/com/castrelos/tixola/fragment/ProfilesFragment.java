package com.castrelos.tixola.fragment;

import static android.content.Context.MODE_PRIVATE;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.castrelos.tixola.R;
import com.castrelos.tixola.utils.loadingDialog;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.squareup.picasso.Picasso;

import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ProfilesFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ProfilesFragment extends Fragment {
    private static SharedPreferences mPrefUser;
    private static final String mDataUser = "user_data";

    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // Custom Dialog for change password
    private View customAlertDialogView;
    private MaterialAlertDialogBuilder materialAlertDialogBuilder;


    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public ProfilesFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment ProfilesFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static ProfilesFragment newInstance(String param1, String param2) {
        ProfilesFragment fragment = new ProfilesFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);


        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Preferencias user_data
        mPrefUser = requireContext()
                .getSharedPreferences(mDataUser, MODE_PRIVATE);

        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_profiles, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        String userName = mPrefUser.getString("UserName", "Null");
        String userMail = mPrefUser.getString("UserMail", "Null");
        String userPhone = mPrefUser.getString("userPhone", "Null");

        //TODO change default url to a valid destination inside our servers
        Uri userPhotoUrl = Uri.parse(mPrefUser.getString("UserPhotoUrl", "http://default.com"));

        // Init View's
        final TextView mUserTitle = (TextView) view.findViewById(R.id.profile_name_title);
        final TextInputLayout mUserMail = (TextInputLayout) view.findViewById(R.id.profile_mail);
        final TextInputLayout mUserName = (TextInputLayout) view.findViewById(R.id.profile_name);
        final TextInputLayout mUserPhone = (TextInputLayout) view.findViewById(R.id.profile_phone);
        final CircleImageView mUserPhoto = (CircleImageView) view.findViewById(R.id.profileImage);


        final Button password = (Button) view.findViewById(R.id.profile_btn_password);
        final Button saveChanges = (Button) view.findViewById(R.id.profile_btn_save);

        // Init Custom Dialog for change password
        materialAlertDialogBuilder = new MaterialAlertDialogBuilder(
                view.getContext());
        customAlertDialogView = LayoutInflater.from(view.getContext())
                .inflate(R.layout.password_dialog, null, false);

        // Update profile variables
        mUserTitle.setText(userName);
        mUserMail.getEditText().setText(userMail);
        mUserName.getEditText().setText(userName);
        mUserPhone.getEditText().setText(userPhone);
        Uri uri = userPhotoUrl;
        Picasso.with(view.getContext())
                .load(uri)
                .placeholder(android.R.drawable.sym_def_app_icon)
                .error(android.R.drawable.sym_def_app_icon)
                .into(mUserPhoto);

        password.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                launchCustomAlertDialog(customAlertDialogView);
            }
        });

        password.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {

                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN)
                    password.setBackgroundColor(getResources().getColor(R.color.colorSecondary));
                if(motionEvent.getAction() == MotionEvent.ACTION_UP)
                    password.setBackgroundColor(getResources().getColor(R.color.colorOnPrimary));

                return false;
            }
        });


        saveChanges.setOnTouchListener((v, event) -> {
            v.performClick();
            String declarePhone = String.valueOf(mUserPhone.getEditText().getText());
            String declareName = String.valueOf(mUserName.getEditText().getText());

            // MotionEvent KeyDown
            if(event.getAction() == MotionEvent.ACTION_DOWN)
                saveChanges.setBackgroundColor(getResources().getColor(R.color.colorSecondary));

            // MotionEvent KeyUp
            if(event.getAction() == MotionEvent.ACTION_UP){
                saveChanges.setBackgroundColor(getResources().getColor(R.color.colorOnPrimary));

                loadingChanges();
                mPrefUser.edit().putString("userPhone", declarePhone).apply();
                mPrefUser.edit().putString("userName", declareName).apply();

                mUserName.getEditText().setText(declareName);
                mUserPhone.getEditText().setText(declarePhone);

            }

            return v.onTouchEvent(event);
        });

    }

    private void loadingChanges(){
        loadingDialog mLoadingDialog = new loadingDialog(getActivity(), "Guardando...");
        mLoadingDialog.startLoadingDialog();

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mLoadingDialog.dismissDialog();
            }
        },5000);
    }

    private void launchCustomAlertDialog(View dialogView){
        final TextInputLayout password1 = (TextInputLayout)
                dialogView.findViewById(R.id.dialog_password_new);
        final TextInputLayout password2 = (TextInputLayout)
                dialogView.findViewById(R.id.dialog_password_repeate);

        if (dialogView.getParent()!=null){
            ((ViewGroup) dialogView.getParent()).removeView(dialogView);
        }
        materialAlertDialogBuilder.setView(dialogView)
                .setTitle(getResources().getString(R.string.change_password_title))
                .setMessage(getResources().getString(R.string.change_password_message))
                .setPositiveButton("Cambiar", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        //TODO set password change with firebase
                        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                        String newPassword = String.valueOf(password1);
                        String TAG = "User update";

                        user.updatePassword(newPassword)
                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        if (task.isSuccessful()) {
                                            Log.d(TAG, "User password updated.");
                                        }else {
                                            reauth(user, dialogView);
                                            //Log.d(TAG, "Something goes wrong. " + task.getResult());
                                        }
                                    }
                                });

                        displayMessage("Operación realizada con exito"+password1.getEditText().getText());
                        dialogInterface.dismiss();

                    }
                })
                .setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        displayMessage("Operación Cancelada");
                        dialogInterface.dismiss();
                    }
                }).show();
    }

    private void reauth(FirebaseUser user, View view){
        // Get the account
        GoogleSignInAccount acct = GoogleSignIn.getLastSignedInAccount(requireContext());
        if (acct != null) {
            AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
            user.reauthenticate(credential).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if (task.isSuccessful()) {
                        launchCustomAlertDialog(view);
                        //Log.d(TAG, "Reauthenticated.");
                    }
                }
            });
        }
    }

    private void displayMessage(String message){
        Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
    }
}