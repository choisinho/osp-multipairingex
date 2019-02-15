package app.bqlab.multipairingex;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
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

public class ClassActivity extends AppCompatActivity{

    //variables
    int classroomNumber, temp;
    String classroomName;
    //objects
    Classroom mClassroom;
    BluetoothSPP mBluetooth;
    SharedPreferences mClassroomPref;
    Student mStudent;
    //layouts
    LinearLayout classBodyList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_class);
        init();
        loadStudentList();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == BluetoothState.REQUEST_CONNECT_DEVICE) {
                assert data != null;
                mStudent.bluetooth.connect(data);
            }
        }
    }

    private void init() {
        //objects
        classroomName = getIntent().getStringExtra("classroomName");
        classroomNumber = getIntent().getIntExtra("classroomNumber", 0);
        mBluetooth = new BluetoothSPP(this);
        mStudent = new Student();
        mClassroom = new Classroom(classroomName, classroomNumber);
        mClassroomPref = getSharedPreferences("classroom", MODE_PRIVATE);
        //layouts
        classBodyList = findViewById(R.id.class_body_list);
        //setting
        findViewById(R.id.class_bot_exit).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exitClass();
            }
        });
    }

    private void loadStudentList() {
        Log.d("시발" ," qlff");
        classBodyList.removeAllViews();
        for (int i = 0; i < mClassroom.students.length; i++) {
            if (mClassroom.students[i] == null)
                mClassroom.students[i] = new Student();
            String state = (mClassroom.students[i].number + 1) + "번 학생 참여도: " + mClassroom.students[i].count;
            Button button = new Button(this);
            button.setBackground(getDrawable(R.color.colorWhiteDark));
            button.setTextSize(18f);
            button.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
            if (mClassroom.students[i].isConnected) {
                button.setText(state);
                if (mClassroom.students[i].count < 2) {
                    button.setBackground(getDrawable(R.color.colorRed));
                } else if (mClassroom.students[i].count < 5) {
                    button.setBackground(getDrawable(R.color.colorGreen));
                } else {
                    button.setBackground(getDrawable(R.color.colorOrange));
                }
            } else {
                button.setText("연결을 위해 여기를 클릭하세요.");
                final int finalI = i;
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        connectDeviceViaBluetooth(finalI);
                    }
                });
            }
            classBodyList.addView(button);
        }
    }

    private void connectDeviceViaBluetooth(int number) {
        mStudent = new Student();
        mStudent.bluetooth = new BluetoothSPP(this);
        mStudent.bluetooth.setOnDataReceivedListener(new BluetoothSPP.OnDataReceivedListener() {
            @Override
            public void onDataReceived(byte[] data, String message) {
                mStudent.count = Integer.parseInt(message);
                if (temp != mStudent.count) {
                    loadStudentList();
                }
                if (mStudent.count == 255) {
                    exitClass();
                }
                temp = mStudent.count;
            }
        });
        mStudent.bluetooth.setBluetoothConnectionListener(new BluetoothSPP.BluetoothConnectionListener() {
            @Override
            public void onDeviceConnected(String name, String address) {
                Toast.makeText(ClassActivity.this, "장치와 연결되었습니다.", Toast.LENGTH_LONG).show();
                mStudent.isConnected = true;
                mClassroom.students[mStudent.number] = mStudent;
                disableEnableControls(true, classBodyList);
                loadStudentList();
            }

            @Override
            public void onDeviceDisconnected() {
                Toast.makeText(ClassActivity.this, "장치와 연결할 수 없습니다.", Toast.LENGTH_LONG).show();
                mStudent.isConnected = false;
                disableEnableControls(true, classBodyList);
                loadStudentList();
            }

            @Override
            public void onDeviceConnectionFailed() {
                Toast.makeText(ClassActivity.this, "장치와 연결할 수 없습니다.", Toast.LENGTH_LONG).show();
                mStudent.isConnected = false;
                disableEnableControls(true, classBodyList);
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
            disableEnableControls(false, classBodyList);
            mStudent.number = number;
            mStudent.bluetooth.setupService();
            mStudent.bluetooth.startService(BluetoothState.DEVICE_OTHER);
            startActivityForResult(new Intent(ClassActivity.this, DeviceList.class), BluetoothState.REQUEST_CONNECT_DEVICE);
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
                        try {
                            for (Student s : mClassroom.students) {
                                s.bluetooth.send("255", true);
                                total += s.count;
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
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

    private void disableEnableControls(boolean enable, ViewGroup vg) {
        for (int i = 0; i < vg.getChildCount(); i++) {
            View child = vg.getChildAt(i);
            child.setEnabled(enable);
            if (child instanceof ViewGroup) {
                disableEnableControls(enable, (ViewGroup) child);
            }
        }
    }
}
