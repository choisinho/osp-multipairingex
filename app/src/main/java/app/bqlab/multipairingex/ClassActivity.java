package app.bqlab.multipairingex;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
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

import java.util.ArrayList;

import app.akexorcist.bluetotohspp.library.BluetoothSPP;
import app.akexorcist.bluetotohspp.library.BluetoothState;
import app.akexorcist.bluetotohspp.library.DeviceList;

public class ClassActivity extends AppCompatActivity {

    //constants
    final int CHOOSE_MAIL_APP = 0;
    final String NEXT_CLASS = "A";
    final String EXIT_CLASS = "B";
    //variables
    int classNumber = 1;
    int classroomNumber, studentNumber;
    boolean isClickedBackbutton;
    String classroomName, emailContent = "";
    String TAG = "ClassActivity";
    String[] connectedAddresses;
    ArrayList<String[]> counts;
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
        setBluetoothReceiver();
        checkExitService();
        loadStudentList();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(bluetoothReceiver);
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
            if (requestCode == BluetoothState.REQUEST_CONNECT_DEVICE) {
                setEnableChildren(true, classBodyList);
            } else if (requestCode == CHOOSE_MAIL_APP) {
                for (Student student : mClassroom.students) {
                    try {
                        if (student.bluetooth != null) {
                            student.connected = false;
                            student.bluetooth.send(EXIT_CLASS, false);
                            Thread.sleep(1000);
                            student.bluetooth.disconnect();
                            loadStudentList();
                        } else {
                            student.connected = false;
                            loadStudentList();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                stopService(new Intent(ClassActivity.this, ExitService.class));
                Intent intent = new Intent(ClassActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
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
        connectedAddresses = new String[mClassroom.students.length];
        //layouts
        classBodyList = findViewById(R.id.class_body_list);
        //setting
        findViewById(R.id.class_bot_stop).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                nextClass();
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
                } else if (i >= 10 && studentNumber >= 0) {
                    if (i == 255) {
                        mClassroom.students[studentNumber].finished = true;
                        loadStudentList();
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
                connectedAddresses[mStudent.number] = address;
                Log.d(TAG, "onDeviceConnected: connectedAddress: " + mStudent.number);
                setEnableChildren(true, classBodyList);
                loadStudentList();
            }

            @Override
            public void onDeviceDisconnected() {
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

    private void nextClass() {
        new AlertDialog.Builder(ClassActivity.this)
                .setTitle("실습 종료")
                .setMessage("실습을 종료하면 다음 실습을 진행할 수 있습니다.")
                .setCancelable(false)
                .setPositiveButton("다음 실습", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        appendEmailContent(NEXT_CLASS);
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
                            for (Student student : mClassroom.students) {
                                try {
                                    if (student.bluetooth != null) {
                                        student.connected = false;
                                        student.bluetooth.send(EXIT_CLASS, false);
                                        Thread.sleep(1000);
                                        student.bluetooth.disconnect();
                                        loadStudentList();
                                    } else {
                                        student.connected = false;
                                        loadStudentList();
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                            stopService(new Intent(ClassActivity.this, ExitService.class));
                            startActivity(new Intent(ClassActivity.this, MainActivity.class));
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
                                            new Thread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    appendEmailContent(EXIT_CLASS);
                                                    String email[] = new String[1];
                                                    email[0] = input.getText().toString();
                                                    Intent emailIntent = new Intent(Intent.ACTION_SEND);
                                                    emailIntent.putExtra(Intent.EXTRA_EMAIL, email);
                                                    emailIntent.putExtra(Intent.EXTRA_SUBJECT, classroomName + " 수업참여도");
                                                    emailIntent.putExtra(Intent.EXTRA_TEXT, emailContent);
                                                    emailIntent.setType("message/rfc822");
                                                    startActivityForResult(Intent.createChooser(emailIntent, "이메일을 보낼 앱을 선택하세요."), CHOOSE_MAIL_APP);
                                                }
                                            }).start();
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

    private void setBluetoothReceiver() {
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED);
        registerReceiver(bluetoothReceiver, filter);
        filter = new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        registerReceiver(bluetoothReceiver, filter);
    }

    private void appendEmailContent(String type) {
        int average = 0, total = 0, connected = 0;
        StringBuilder builder = new StringBuilder();
        builder.append(emailContent);
        builder.append("\n")
                .append(classNumber)
                .append("차 실습\n");
        for (int i = 0; i < mClassroom.students.length; i++) {
            if (mClassroom.students[i].connected) {
                builder.append((i + 1))
                        .append("번 학생 참여도: ")
                        .append(mClassroom.students[i].count)
                        .append("\n");
                connected++;
            }
        }
        for (Student student : mClassroom.students) {
            try {
                if (student.bluetooth != null) {
                    total += student.count;
                    student.bluetooth.send(type, false);
                    student.count = 0;
                    student.finished = false;
                    total += student.count;
                } else {
                    student.count = 0;
                    student.finished = false;
                    total += student.count;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (connected == 0)
            builder.append("이 실습에서는 연결된 장치가 없었습니다.");
        else {
            try {
                average = total / connected;
            } catch (ArithmeticException e) {
                average = 0;
            }
            builder.append("평균: ").append(average).append("\n");
        }
        emailContent = builder.toString();
        classNumber += 1;
        new Thread(new Runnable() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        loadStudentList();
                    }
                });
            }
        }).start();
    }

    BroadcastReceiver bluetoothReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                for (int i = 0; i < connectedAddresses.length; i++) {
                    try {
                        if (connectedAddresses[i].equals(device.getAddress())) {
                            mClassroom.students[i].connected = false;
                            setEnableChildren(true, classBodyList);
                            loadStudentList();
                            break;
                        }
                    } catch (NullPointerException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    };
}
