package com.example.kamay;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class DashboardType extends AppCompatActivity {
  EditText editName;
 Button buttonClick;
   TextView textName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        editName=(EditText) findViewById(R.id.editName);


        buttonClick.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v){
                String name=editName.getText().toString();
                textName.setText(""+name);
            }
        });

    }

}
