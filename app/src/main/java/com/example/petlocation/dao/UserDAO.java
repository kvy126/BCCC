package com.example.petlocation.dao;

import android.content.Context;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.example.petlocation.model.User;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Objects;
import java.util.UUID;

public class UserDAO {
    private FirebaseFirestore db;
    private Context context;

    public UserDAO(Context context) {
        db = FirebaseFirestore.getInstance();
        this.context = context;
    }

    public void insertUser(User user) {
        user.setUuid(UUID.randomUUID().toString());
        HashMap<String, Object> userMap = user.convertHashMap();
        db.collection("Users").document(user.getUuid())
                .set(userMap)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        Toast.makeText(context, "Thêm người dùng mới thành công!", Toast.LENGTH_SHORT).show();

                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(context, "Thêm mới người dùng thất bại!", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    public interface UpdateUserCallback {
        void onSuccess();

        void onFailure(String errorMessage);
    }
}
