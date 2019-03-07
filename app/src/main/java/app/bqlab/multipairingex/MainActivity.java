package app.bqlab.multipairingex;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
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
import java.nio.channels.NotYetBoundException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    //variables
    SharedPreferences mClassroomPref;
    ArrayList<Classroom> classrooms;
    //layouts
    LinearLayout mainBodyList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();
        loadData();
        checkClassCount();
        checkFinishedClass();
    }

    private void init() {
        //objects
        mClassroomPref = getSharedPreferences("classroom", MODE_PRIVATE);
        classrooms = new ArrayList<>();
        //layouts
        mainBodyList = findViewById(R.id.main_body_list);
        //initializing or setting
        findViewById(R.id.main_bot_add).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final EditText nameInput = new EditText(MainActivity.this);
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("강의실 추가")
                        .setMessage("강의실의 이름을 설정하세요.")
                        .setView(nameInput)
                        .setPositiveButton("다음", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                final String name = nameInput.getText().toString();
                                if (!name.isEmpty()) {
                                    final EditText numberInput = new EditText(MainActivity.this);
                                    numberInput.setInputType(InputType.TYPE_CLASS_NUMBER);
                                    new AlertDialog.Builder(MainActivity.this)
                                            .setTitle("강의실 추가")
                                            .setMessage("강의실의 인원을 설정하세요. (7명 이내)")
                                            .setView(numberInput)
                                            .setPositiveButton("확인", new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    if (!numberInput.getText().toString().isEmpty()) {
                                                        int number = Integer.parseInt(numberInput.getText().toString());
                                                        if (number == 0 || number > 7) {
                                                            Toast.makeText(MainActivity.this, "잘못된 입력입니다.", Toast.LENGTH_LONG).show();
                                                            dialog.dismiss();
                                                        }
                                                        classrooms.add(new Classroom(name, number));
                                                        saveData();
                                                    } else
                                                        Toast.makeText(MainActivity.this, "잘못된 입력입니다.", Toast.LENGTH_LONG).show();
                                                }
                                            })
                                            .setNegativeButton("취소", new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {

                                                }
                                            })
                                            .show();
                                } else {
                                    Toast.makeText(MainActivity.this, "잘못된 입력입니다.", Toast.LENGTH_LONG).show();
                                }
                            }
                        })
                        .setNegativeButton("취소", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                            }
                        })
                        .show();

            }
        });
    }

    private void saveData() {
        SharedPreferences.Editor editor = mClassroomPref.edit();
        Gson gson = new Gson();
        String json = gson.toJson(classrooms);
        editor.putString("list", json);
        editor.apply();
        loadData();
    }

    private void loadData() {
        Gson gson = new Gson();
        String json = mClassroomPref.getString("list", null);
        Type type = new TypeToken<ArrayList<Classroom>>() {
        }.getType();
        classrooms = gson.fromJson(json, type);
        if (classrooms != null) {
            if (classrooms.size() == 0) {
                TextView textView = new TextView(this);
                textView.setGravity(Gravity.CENTER_HORIZONTAL);
                textView.setText("수업이 없습니다. 버튼을 눌러 수업을 추가하세요.");
                mainBodyList.addView(textView);
            } else {
                mainBodyList.removeAllViews();
                for (final Classroom classroom : classrooms) {
                    Button button = new Button(this);
                    final String name = classroom.name;
                    button.setText(name);
                    button.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(final View v) {
                            new AlertDialog.Builder(MainActivity.this)
                                    .setTitle("강의실 입장")
                                    .setMessage("강의실을 확인하시겠습니까?")
                                    .setPositiveButton("확인", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            Intent intent = new Intent(MainActivity.this, ClassActivity.class);
                                            intent.putExtra("classroomName", classroom.name);
                                            intent.putExtra("classroomNumber", classroom.students.length);
                                            startActivity(intent);
                                        }
                                    })
                                    .setNegativeButton("취소", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            dialog.dismiss();
                                        }
                                    })
                                    .setNeutralButton("강의 삭제", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            removeClass(classroom);
                                        }
                                    }).show();
                        }
                    });
                    mainBodyList.addView(button);
                }
            }
        } else {
            classrooms = new ArrayList<>();
            TextView textView = new TextView(this);
            textView.setGravity(Gravity.CENTER_HORIZONTAL);
            textView.setText("수업이 없습니다. 버튼을 눌러 수업을 추가하세요.");
            mainBodyList.addView(textView);
        }
    }

    private void removeClass(Classroom classroom) {
        for (int i = 0; i < mainBodyList.getChildCount(); i++) {
            if (((Button) mainBodyList.getChildAt(i)).getText().equals(classroom.name))
                mainBodyList.removeView(mainBodyList.getChildAt(i));
        }
        classrooms.remove(classroom);
        saveData();
    }

    private void checkClassCount() {
        if ((mClassroomPref.getInt("count", 0) % 3) == 0) {
            try {
                int average = mClassroomPref.getInt("average", 0) / mClassroomPref.getInt("count", 0);
                new AlertDialog.Builder(this)
                        .setTitle("강의 참여도 통계")
                        .setMessage("현재까지의 참여도 평균은 " + String.valueOf(average) + "입니다.")
                        .setPositiveButton("확인", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Toast.makeText(MainActivity.this, "평균은 계속해서 누적됩니다.", Toast.LENGTH_LONG).show();
                                dialog.dismiss();
                            }
                        }).show();
            } catch (ArithmeticException e) {
                e.printStackTrace();
            }
        }
    }

    private void checkFinishedClass() {
        if (getIntent().getStringExtra("finishedClass") != null) {
            for (Classroom classroom : classrooms) {
                if (getIntent().getStringExtra("finishedClass").equals(classroom.name))
                    removeClass(classroom);
            }
        }
    }
}