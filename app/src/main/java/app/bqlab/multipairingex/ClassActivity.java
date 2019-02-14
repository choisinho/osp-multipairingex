package app.bqlab.multipairingex;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;

import app.akexorcist.bluetotohspp.library.BluetoothSPP;
import app.akexorcist.bluetotohspp.library.BluetoothState;
import app.akexorcist.bluetotohspp.library.DeviceList;

public class ClassActivity extends AppCompatActivity {

    //variables
    int classroomNumber;
    String classroomName;
    //objects
    Classroom mClassroom;
    BluetoothSPP mBluetooth;
    SharedPreferences mClassroomPref;
    //layouts
    LinearLayout classBodyList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_class);
        init();
        loadStudentList();
    }

    private void init() {
        //objects
        classroomName = getIntent().getStringExtra("classroomName");
        classroomNumber = getIntent().getIntExtra("classroomNumber", 0);
        mBluetooth = new BluetoothSPP(this);
        mClassroom = new Classroom(classroomName, classroomNumber);
        mClassroomPref = getSharedPreferences("classroom", MODE_PRIVATE);
        //setting
        findViewById(R.id.class_bot_exit).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exitClass();
            }
        });
        //layouts
        classBodyList = findViewById(R.id.class_body_list);
    }

    private void loadStudentList() {
        for (int i = 0; i<mClassroom.students.length;i++) {
            if (mClassroom.students[i] == null)
                mClassroom.students[i] = new Student();
            String state = "Count: " + String.valueOf(mClassroom.students[i].count);
            TextView textView = new TextView(this);
            textView.setBackground(getDrawable(R.color.colorWhiteDark));
            textView.setClickable(true);
            textView.setFocusable(true);
            textView.setTextSize(18f);
            if (mClassroom.students[i].isConnected) {
                textView.setText(state);
                if (mClassroom.students[i].count < 2) {
                    textView.setBackground(getDrawable(R.color.colorRed));
                } else if (mClassroom.students[i].count < 5) {
                    textView.setBackground(getDrawable(R.color.colorGreen));
                } else {
                    textView.setBackground(getDrawable(R.color.colorOrange));
                }
            } else {
                textView.setText("연결을 위해 여기를 클릭하세요.");
                final int finalI = i;
                textView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        connectDeviceViaBluetooth(mClassroom.students[finalI]);
                    }
                });
            }
            classBodyList.addView(textView);
        }
    }

    private void connectDeviceViaBluetooth(final Student student) {
        student.bluetooth = new BluetoothSPP(this);
        student.bluetooth.setOnDataReceivedListener(new BluetoothSPP.OnDataReceivedListener() {
            @Override
            public void onDataReceived(byte[] data, String message) {
                student.count = Integer.parseInt(message);
                if (student.count == 255) {
                    exitClass();
                }
            }
        });
        student.bluetooth.setBluetoothConnectionListener(new BluetoothSPP.BluetoothConnectionListener() {
            @Override
            public void onDeviceConnected(String name, String address) {
                Toast.makeText(ClassActivity.this, "장치와 연결되었습니다.", Toast.LENGTH_LONG).show();
                student.isConnected = true;
                loadStudentList();
            }

            @Override
            public void onDeviceDisconnected() {
                Toast.makeText(ClassActivity.this, "장치와 연결할 수 없습니다.", Toast.LENGTH_LONG).show();
                student.isConnected = false;
                loadStudentList();
            }

            @Override
            public void onDeviceConnectionFailed() {
                Toast.makeText(ClassActivity.this, "장치와 연결할 수 없습니다.", Toast.LENGTH_LONG).show();
                student.isConnected = false;
                loadStudentList();
            }
        });
        if (!mBluetooth.isBluetoothAvailable()) {
            Toast.makeText(ClassActivity.this, "지원하지 않는 기기입니다.", Toast.LENGTH_LONG).show();
            ClassActivity.this.finishAffinity();
        } else if (!mBluetooth.isBluetoothEnabled()) {
            Toast.makeText(ClassActivity.this, "블루투스가 활성화되지 않았습니다.", Toast.LENGTH_LONG).show();
            ClassActivity.this.finishAffinity();
        } else {
            mBluetooth.setupService();
            mBluetooth.startService(BluetoothState.DEVICE_OTHER);
            startActivity(new Intent(ClassActivity.this, DeviceList.class));
        }
    }

    private void exitClass() {
        startService(new Intent(this, NotifyService.class));
        new AlertDialog.Builder(ClassActivity.this)
                .setTitle("수업 종료")
                .setMessage("수업이 종료되어 메인 화면으로 이동합니다.")
                .setCancelable(false)
                .setPositiveButton("확인", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        int average, total = 0;
                        for (Student s : mClassroom.students) {
                            total += s.count;
                        }
                        average = total / mClassroom.students.length;
                        mClassroomPref.edit().putInt("count", mClassroomPref.getInt("count", 0) + 1).apply();
                        mClassroomPref.edit().putInt("average", mClassroomPref.getInt("average", 0) + average).apply();
                        Intent intent = new Intent(ClassActivity.this, MainActivity.class);
                        intent.putExtra("finishedClass", classroomName);
                        startActivity(intent);
                        finish();
                    }
                }).show();
    }
}
