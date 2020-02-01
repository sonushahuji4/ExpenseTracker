package com.example.shadrak.expensestracker;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.app.DatePickerDialog;
import android.widget.DatePicker;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class formpopup extends Activity {

    private static final int RESULT_LOAD_IMAGE = 1;
    EditText vendor, place, edit_date, amount;
    Button submit, upload;
    DatePickerDialog.OnDateSetListener onDateSetListener;
    String vendorname, location, costprice, Category, date, uid, filename;
    Spinner category_spin;
    DatabaseReference rootref, childref;
    private Uri filePath, selectedImage;
    TextView file;
    private final int PICK_IMAGE_REQUEST = 71;

    //Firebase
    FirebaseStorage storage;
    StorageReference storageReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_formpopup);

        Bundle bundle = getIntent().getExtras();
        assert bundle != null;
        uid = bundle.getString("uid");

        //firebase stuff
        rootref = FirebaseDatabase.getInstance().getReference();
        storageReference = FirebaseStorage.getInstance().getReference();

        // Spinner code -------- Starts
        rootref.child("Category").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                ArrayList<String> cate = new ArrayList<>();
                category_spin = findViewById(R.id.category);

                for (DataSnapshot categorySnapshot: dataSnapshot.getChildren()) {
                    String cat = categorySnapshot.getValue(String.class);
                    cate.add(cat);
                }

                ArrayAdapter<String> cat_adap = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_spinner_item, cate);
                cat_adap.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                category_spin.setAdapter(cat_adap);

                category_spin.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                        Category = (String) adapterView.getSelectedItem();
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> adapterView) {

                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        // Spinner code ---------- Ends

        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);

        int width = dm.widthPixels;
        int height = dm.heightPixels;

        getWindow().setLayout((int)(width*.8),(int)(height*.8));

        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.gravity = Gravity.CENTER;
        params.x = 0;
        params.y = -20;

        getWindow().setAttributes(params);

        vendor = findViewById(R.id.vendorname);
        place = findViewById(R.id.place);
        edit_date = findViewById(R.id.editdate);
        amount = findViewById(R.id.amount);
        submit = findViewById(R.id.submitbutton);
        upload = findViewById(R.id.uploadbtn);
        file = findViewById(R.id.imageid);

        upload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                chooseImage();
                //uploadImage();
            }
        });


        // Pick date function
        edit_date.setOnClickListener(new View.OnClickListener(){
            //Log.i("date","onclicklistener");
            @Override
            public void onClick(View view) {
                Log.i("date","onclickview");
                Calendar calendar = Calendar.getInstance();
                int year = calendar.get(Calendar.YEAR);
                int month = calendar.get(Calendar.MONTH);
                int day = calendar.get(Calendar.DAY_OF_MONTH);

                DatePickerDialog datePickerDialog = new DatePickerDialog(formpopup.this, onDateSetListener,year,month,day);

                datePickerDialog.show();
            }
        });

        onDateSetListener = new DatePickerDialog.OnDateSetListener() {
          @Override
          public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
              Log.i("date","onDateset");
              month = month+1;
              String date = dayOfMonth + "/" + month + "/" + year;
              edit_date.setText(date);

          }
        };

        submit.setOnClickListener(  new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i("button","submit button clicked");
                vendorname = vendor.getText().toString();
                location = place.getText().toString();
                costprice = amount.getText().toString();
                date = edit_date.getText().toString();

                //get userid
                Random r = new Random();
                int billid = r.nextInt(9999 - 1000) + 1000;

                //childref = rootref.child("Bills").child(userid).child(String.valueOf(billid));
                //childref = rootref.child("Bills").child(userid);
                childref = rootref.child("Bills");

                childref.child(uid).child(String.valueOf(billid)).setValue(new newBill(costprice, date, location, vendorname, Category));
                Toast.makeText(getApplicationContext(),"Submitted",Toast.LENGTH_SHORT).show();
                uploadImage(billid);
                finish();
            }
        });

    }

    private void chooseImage() {
        Intent i = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
//        i.setType(Environment.getExternalStorageDirectory()+"/*");
//        i.setAction(Intent.ACTION_GET_CONTENT);
//        startActivityForResult(Intent.createChooser(i, "Select pic"), PICK_IMAGE_REQUEST);
        startActivityForResult(i, RESULT_LOAD_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == RESULT_LOAD_IMAGE && resultCode == RESULT_OK
                && data != null && data.getData() != null )
        {
//            filePath = data.getData();
//            try {
//                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), filePath);
////                imageView.setImageBitmap(bitmap)
//                file.setText((CharSequence) filePath);
//            }
//            catch (IOException e)
//            {
//                e.printStackTrace();
//            }
            selectedImage = data.getData();
            String[] filePathColumn = { MediaStore.Images.Media.DATA };

            Cursor cursor = getContentResolver().query(selectedImage,
                    filePathColumn, null, null, null);
            cursor.moveToFirst();

            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            String storagePath = cursor.getString(columnIndex);
            file.setText(storagePath);
            cursor.close();
        }
    }

    private void uploadImage(int billid) {
//        if(filePath != null)
//        {
//            final ProgressDialog progressDialog = new ProgressDialog(this);
//            progressDialog.setTitle("Uploading...");
//            progressDialog.show();

            Uri uri = selectedImage;
            StorageReference ref = storageReference.child(uid).child(String.valueOf(billid)).child(uri.getLastPathSegment());
            ref.putFile(selectedImage)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
//                            if(progressDialog != null && progressDialog.isShowing()) {
//                                progressDialog.dismiss();
//                            }
                            Toast.makeText(getApplicationContext(), "Uploaded", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
//                            progressDialog.dismiss();
                            Toast.makeText(getApplicationContext(), "Failed "+e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                            double progress = (100.0*taskSnapshot.getBytesTransferred()/taskSnapshot
                                    .getTotalByteCount());
//                            progressDialog.setMessage("Uploaded "+(int)progress+"%");
                        }
                    });
//        }
    }

}
