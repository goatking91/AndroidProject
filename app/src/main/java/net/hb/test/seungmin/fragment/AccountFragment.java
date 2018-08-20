package net.hb.test.seungmin.fragment;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;


import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.UploadTask;

import net.hb.test.seungmin.MainActivity;
import net.hb.test.seungmin.R;
import net.hb.test.seungmin.SignupActivity;
import net.hb.test.seungmin.model.UserModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AccountFragment extends Fragment{

    private static final int PICK_FROM_ALBUM = 10;
    private EditText email;
    private EditText name;
    private EditText password;
    private Button edit;
    private ImageView profile;
    private Uri imageUri;
    private String profileImageUrl;
    private String userName;
    private String uid;
    private String pushToken;
    private String comment;
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_account,container,false);

        Button button = view.findViewById(R.id.accountFragment_button_comment);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showDialog(view.getContext());
            }
        });

        profile = view.findViewById(R.id.accountFragment_imageView_profile);
        email = view.findViewById(R.id.accountFragment_editText_email);
        name = view.findViewById(R.id.accountFragment_editText_name);
        password = view.findViewById(R.id.accountFragment_editText_password);
        edit = view.findViewById(R.id.accountFragment_button_edit);

        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseDatabase.getInstance().getReference().child("users").child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                UserModel users = dataSnapshot.getValue(UserModel.class);
                profileImageUrl = users.profileImageUrl;
                Glide.with(view.getContext())
                        .load(profileImageUrl)
                        .apply(new RequestOptions().circleCrop())
                        .into(profile);
                pushToken = users.pushToken;
                if(users.comment != null) {
                    comment = users.comment;
                } else {
                    comment = "";
                }
                userName = users.userName;
                email.setText(FirebaseAuth.getInstance().getCurrentUser().getEmail());
                name.setText(userName);
                System.out.println(users.userName);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });


        profile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType(MediaStore.Images.Media.CONTENT_TYPE);
                startActivityForResult(intent, PICK_FROM_ALBUM);
            }
        });


        edit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {

                if (email.getText().toString() == null || name.getText().toString() ==null || password.getText().toString() == null || imageUri == null){
                    Toast.makeText(view.getContext(),"모두 입력해 주세요.",Toast.LENGTH_SHORT).show();
                    return;
                }

                FirebaseStorage.getInstance().getReference().child("userImages").child(uid).putFile(imageUri).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                        String imageUrl = task.getResult().getDownloadUrl().toString();

                        FirebaseAuth.getInstance().getCurrentUser().updatePassword(password.getText().toString());

                        UserModel userModel = new UserModel();
                        userModel.userName = name.getText().toString();
                        userModel.pushToken = pushToken;
                        if(comment != null) {
                            userModel.comment = comment;
                        }else {
                            userModel.comment = "";
                        }
                        userModel.profileImageUrl = imageUrl;
                        userModel.uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
                        
                        FirebaseDatabase.getInstance().getReference().child("users").child(uid).setValue(userModel).addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                Toast.makeText(view.getContext(),"변경되었습니다.",Toast.LENGTH_SHORT).show();
                                getFragmentManager().beginTransaction().replace(R.id.mainactivity_framelayout,new AccountFragment()).commit();
                            }
                        });
                    }
                });

            }
        });

        return view;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PICK_FROM_ALBUM && resultCode == getActivity().RESULT_OK) {
            profile.setImageURI(data.getData()); // 가운데 뷰를 바꿈
            imageUri = data.getData();// 이미지 경로 원본
        }
    }

    void showDialog(Context context){

        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        LayoutInflater layoutInflater = getActivity().getLayoutInflater();
        View view = layoutInflater.inflate(R.layout.dialog_comment,null);
        final EditText editText = view.findViewById(R.id.commentDialog_edittext);
        FirebaseDatabase.getInstance().getReference().child("users").child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                UserModel users = dataSnapshot.getValue(UserModel.class);
                editText.setText(users.comment);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        builder.setView(view).setPositiveButton("확인", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

                Map<String,Object> stringObjectMap = new HashMap<>();
                String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
                stringObjectMap.put("comment",editText.getText().toString());
                FirebaseDatabase.getInstance().getReference().child("users").child(uid).updateChildren(stringObjectMap);

            }
        }).setNegativeButton("취소", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

            }
        });

        builder.show();
    }
}
