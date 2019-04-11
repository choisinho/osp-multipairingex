package app.bqlab.multipairingex;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import app.akexorcist.bluetotohspp.library.BluetoothSPP;
import app.akexorcist.bluetotohspp.library.BluetoothState;
import app.akexorcist.bluetotohspp.library.DeviceList;

public class ClassActivity extends AppCompatActivity {

    //constants
    final String TAG = "ClassActivity";
    //variables
    int classroomNumber, studentNumber;
    boolean isClickedBackbutton, allowedExit;
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
        checkExitService();
        loadStudentList();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == BluetoothState.REQUEST_CONNECT_DEVICE) {
                if (data != null) {
                    mStudent.bluetooth.connect(data);
                }
            }
        } else {
            setEnableChildren(true, classBodyList);
        }
    }

    @Override
    public void onBackPressed() {
        if (!isClickedBackbutton) {
            Toast.makeText(this, "한번 더 누르면 수업이 종료됩니다.", Toast.LENGTH_SHORT).show();
            isClickedBackbutton = true;
        } else {
            super.onBackPressed();
            stopService(new Intent(this, ExitService.class));
            ActivityCompat.finishAffinity(this);
        }
        new CountDownTimer(3000, 1000) {
            @Override
            public void onTick(long l) {

            }
            public void onFinish() {
                exitClass();
            }
        }.start();
    }

    private void init() {
        //objects
        classroomName = getIntent().getStringExtra("classroomName");
        classroomNumber = getIntent().getIntExtra("classroomNumber", 0);
        mBluetooth = new BluetoothSPP(this);
        mClassroom = new Classroom(classroomName, classroomNumber);
        mClassroomPref = getSharedPreferences("classroom", MODE_PRIVATE);
        //layouts
        classBodyList = findViewById(R.id.class_body_list);
        //setting
        findViewById(R.id.class_bot_stop).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopClass();
            }
        });
        findViewById(R.id.class_bot_exit).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exitClass();
            }
        });
    }

    private void loadStudentList() {
        Log.d(TAG, "loadStudentList: ");
        classBodyList.removeAllViews();
        for (int i = 0; i < mClassroom.students.length; i++) {
            if (mClassroom.students[i] == null)
                mClassroom.students[i] = new Student();
            String state = (mClassroom.students[i].number + 1) + "번 학생 참여도: " + mClassroom.students[i].count;
            Button button = new Button(this);
            button.setBackground(getDrawable(R.color.colorWhiteDark));
            button.setTextSize(18f);
            button.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
            if (mClassroom.students[i].connected) {
                button.setText(state);
                if (mClassroom.students[i].count < 2) {
                    button.setBackground(getDrawable(R.color.colorRed));
                } else if (mClassroom.students[i].count < 5) {
                    button.setBackground(getDrawable(R.color.colorGreen));
                } else {
                    button.setBackground(getDrawable(R.color.colorOrange));
                }
                if (mClassroom.students[i].finished) {
                    button.setClickable(false);
                    button.setFocusable(false);
                    button.setText("실습이 종료된 학생입니다.");
                    button.setBackground(getDrawable(R.color.colorWhiteDark));
                }
            } else {
                final int finalI = i;
                button.setText("연결을 위해 여기를 클릭하세요.");
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        connectToDevice(finalI);
                    }
                });
            }
            classBodyList.addView(button);
        }
    }

    private void connectToDevice(int number) {
        mStudent = new Student();
        mStudent.bluetooth = new BluetoothSPP(this);
        mStudent.bluetooth.setOnDataReceivedListener(new BluetoothSPP.OnDataReceivedListener() {
            @Override
            public void onDataReceived(byte[] data, String message) {
                int i = Integer.parseInt(message);
                if (i <= 7) {
                    studentNumber = i;
                    Log.d(TAG, "onDataReceived: studentNumber: " + message);
                } else if (i >= 10 && studentNumber >= 0) {
                    Log.d(TAG, "onDataReceived: data: " + message);
                    if (i == 255) {
                        mClassroom.students[studentNumber].finished = true;
                    } else {
                        int count = i - 10;
                        if (mClassroom.students[studentNumber].count != count) {
                            mClassroom.students[studentNumber].count = count;
                            loadStudentList();
                        }
                    }
                }
            }
        });
        mStudent.bluetooth.setBluetoothConnectionListener(new BluetoothSPP.BluetoothConnectionListener() {
            @Override
            public void onDeviceConnected(String name, String address) {
                Toast.makeText(ClassActivity.this, "장치와 연결되었습니다.", Toast.LENGTH_LONG).show();
                mStudent.connected = true;
                mStudent.bluetooth.send(String.valueOf(mStudent.number), false);
                mClassroom.students[mStudent.number] = mStudent;
                setEnableChildren(true, classBodyList);
                loadStudentList();
            }

            @Override
            public void onDeviceDisconnected() {
                if (allowedExit) {
                    mStudent.connected = false;
                    setEnableChildren(true, classBodyList);
                    loadStudentList();
                } else {
                    new AlertDialog.Builder(ClassActivity.this)
                            .setTitle("장치에서 문제가 발생했습니다!")
                            .setMessage("수업이 진행되는 동안 장치와의 연결이 강제로 끊어진 경우 수업을 더이상 진행할 수 없습니다.")
                            .setCancelable(false)
                            .setPositiveButton("확인", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    exitClass();
                                }
                            }).show();
                }
            }

            @Override
            public void onDeviceConnectionFailed() {
                Toast.makeText(ClassActivity.this, "장치와 연결할 수 없습니다. 장치를 연결할 수 없는 상태이거나 이미 연결되어 있지 않은지 확인 후 다시 시도하세요.", Toast.LENGTH_LONG).show();
                mStudent.connected = false;
                setEnableChildren(true, classBodyList);
                loadStudentList();
            }
        });
        if (!mBluetooth.isBluetoothAvailable()) {
            Toast.makeText(ClassActivity.this, "지원하지 않는 기기입니다.", Toast.LENGTH_LONG).show();
            stopService(new Intent(this, ExitService.class));
            ClassActivity.this.finishAffinity();
        } else if (!mBluetooth.isBluetoothEnabled()) {
            Toast.makeText(ClassActivity.this, "블루투스가 활성화되지 않았습니다.", Toast.LENGTH_LONG).show();
            stopService(new Intent(this, ExitService.class));
            ClassActivity.this.finishAffinity();
        } else {
            setEnableChildren(false, classBodyList);
            mStudent.number = number;
            mStudent.bluetooth.setupService();
            mStudent.bluetooth.startService(BluetoothState.DEVICE_OTHER);
            startActivityForResult(new Intent(ClassActivity.this, DeviceList.class), BluetoothState.REQUEST_CONNECT_DEVICE);
        }
    }

    private void stopClass() {
        new AlertDialog.Builder(ClassActivity.this)
                .setTitle("실습 종료")
                .setMessage("실습을 종료하면 다음 실습을 진행할 수 있습니다.")
                .setCancelable(false)
                .setPositiveButton("다음 실습", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        int average, total = 0;
                        for (Student student : mClassroom.students) {
                            if (student.bluetooth != null) {
                                student.bluetooth.send("A", false);
                                student.count = 0;
                                student.finished = false;
                                total += student.count;
                            }
                        }
                        average = total / mClassroom.students.length;
                        mClassroomPref.edit().putInt("count", mClassroomPref.getInt("count", 0) + 1).apply();
                        mClassroomPref.edit().putInt("average", mClassroomPref.getInt("average", 0) + average).apply();
                        loadStudentList();
                    }
                })
                .setNegativeButton("취소", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                }).show();
    }

    private void exitClass() {
        try {
            startService(new Intent(ClassActivity.this, NotifyService.class));
            new AlertDialog.Builder(ClassActivity.this)
                    .setTitle("수업 종료")
                    .setMessage("수업이 종료되었습니다. 결과를 메일로 보낼 수 있습니다.")
                    .setCancelable(false)
                    .setNegativeButton("종료", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            int average, total = 0;
                            for (Student student : mClassroom.students) {
                                try {
                                    if (student.bluetooth != null) {
                                        allowedExit = true;
                                        student.connected = false;
                                        student.bluetooth.send("B", false);
                                        Thread.sleep(1000);
                                        student.bluetooth.disconnect();
                                        loadStudentList();
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                            average = total / mClassroom.students.length;
                            mClassroomPref.edit().putInt("count", mClassroomPref.getInt("count", 0) + 1).apply();
                            mClassroomPref.edit().putInt("average", mClassroomPref.getInt("average", 0) + average).apply();
                            stopService(new Intent(ClassActivity.this, ExitService.class));
                            Intent intent = new Intent(ClassActivity.this, MainActivity.class);
                            intent.putExtra("finishedClass", classroomName);
                            startActivity(intent);
                            finish();
                        }
                    })
                    .setPositiveButton("다음", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            final EditText input = new EditText(ClassActivity.this);
                            new AlertDialog.Builder(ClassActivity.this)
                                    .setTitle("수업 종료")
                                    .setMessage("전송받을 메일 주소를 입력하세요.")
                                    .setView(input)
                                    .setCancelable(false)
                                    .setPositiveButton("메일 앱으로 전송", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            //variables
                                            int average, total = 0;
                                            //mail processing
                                            new Thread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    String address, content = "";
                                                    //email setting
                                                    address = input.getText().toString();
                                                    for (int i=0; i<mClassroom.students.length;i++) {
                                                        StringBuilder builder = new StringBuilder();
                                                        builder.append(content);
                                                        builder.append(String.valueOf(i+1));
                                                        builder.append("번 학생 참여도: ");
                                                        builder.append(String.valueOf(mClassroom.students[i].count));
                                                        builder.append("\n");
                                                        content = builder.toString();
                                                    }
                                                    Intent emailIntent = new Intent(Intent.ACTION_SEND);
                                                    emailIntent.putExtra(Intent.EXTRA_EMAIL, address);
                                                    emailIntent.putExtra(Intent.EXTRA_SUBJECT, classroomName + " 수업참여도");
                                                    emailIntent.putExtra(Intent.EXTRA_TEXT, content);
                                                    emailIntent.setType("message/rfc822");
                                                    startActivity(Intent.createChooser(emailIntent, "이메일을 보낼 앱을 선택하세요."));
                                                }
                                            }).start();
                                            for (Student student : mClassroom.students) {
                                                try {
                                                    if (student.bluetooth != null) {
                                                        allowedExit = true;
                                                        student.connected = false;
                                                        student.bluetooth.send("B", false);
                                                        Thread.sleep(1000);
                                                        student.bluetooth.disconnect();
                                                        loadStudentList();
                                                    }
                                                } catch (Exception e) {
                                                    e.printStackTrace();
                                                }
                                            }
                                            //exit this class
                                            average = total / mClassroom.students.length;
                                            mClassroomPref.edit().putInt("count", mClassroomPref.getInt("count", 0) + 1).apply();
                                            mClassroomPref.edit().putInt("average", mClassroomPref.getInt("average", 0) + average).apply();
                                            stopService(new Intent(ClassActivity.this, ExitService.class));
                                            Intent intent = new Intent(ClassActivity.this, MainActivity.class);
                                            intent.putExtra("finishedClass", classroomName);
                                            startActivity(intent);
                                            finish();
                                        }
                                    }).show();
                        }
                    })
                    .show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void checkExitService() {
        if (!ServiceCheck.isRunning(this, ExitService.class.getName())) {
            startService(new Intent(this, ExitService.class));
        }
    }

    private void setEnableChildren(boolean enable, ViewGroup viewGroup) {
        for (int i = 0; i < viewGroup.getChildCount(); i++) {
            View child = viewGroup.getChildAt(i);
            child.setEnabled(enable);
            if (child instanceof ViewGroup) {
                setEnableChildren(enable, (ViewGroup) child);
            }
        }
    }
}
