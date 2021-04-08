package com.sampleassignment.lifespark;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;
import com.sampleassignment.lifespark.common.Common;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class SelectionActivity extends AppCompatActivity {
    @BindView(R.id.selection_btn_receiver)
    MaterialButton btn_receiver;

    @BindView(R.id.selection_btn_transmitter)
    MaterialButton btn_transmitter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_selection);
        ButterKnife.bind(this);


        String[] permissions = new String[]{
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE,

               };


        Dexter.withContext(SelectionActivity.this)
                .withPermissions(permissions)
                .withListener(new MultiplePermissionsListener() {
                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport multiplePermissionsReport) {
                        setListener();

                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(List<PermissionRequest> list, PermissionToken permissionToken) {
                        permissionToken.continuePermissionRequest();
                    }
                }).check();

    }

    private void setListener() {
        btn_receiver.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Common.appMode = Common.RECEIVER_MODE;
                Toast.makeText(SelectionActivity.this, "Make Sure that  \"TRANSMITTER\" is in your paired devices list  ", Toast.LENGTH_LONG).show();
                startActivity(new Intent(SelectionActivity.this,MainActivity.class));
            }
        });

        btn_transmitter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(SelectionActivity.this, "Make Sure that \"RECEIVER\" is in your paired devices list ", Toast.LENGTH_LONG).show();
                Common.appMode = Common.TRANSMITTER_MODE;
                startActivity(new Intent(SelectionActivity.this,MainActivity.class));
            }
        });
    }
}