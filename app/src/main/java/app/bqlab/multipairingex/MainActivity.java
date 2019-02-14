package app.bqlab.multipairingex;

import android.content.DialogInterface;
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
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    //variables
    SharedPreferences mSettingPref, mClassroomPref;
    ArrayList<Classroom> classrooms;
    //layouts
    LinearLayout mainBodyList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();
        loadData();
    }

    private void init() {
        //objects
        mSettingPref = getSharedPreferences("setting", MODE_PRIVATE);
        mClassroomPref = getSharedPreferences("classroom", MODE_PRIVATE);
        classrooms = new ArrayList<>();
        //layouts
        mainBodyList = findViewById(R.id.main_body_list);
        //initializing or setting
        findViewById(R.id.main_bot_add).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Classroom classroom;
                final EditText nameInput = new EditText(MainActivity.this);
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("강의실 추가")
                        .setMessage("강의실의 이름을 설정하세요.")
                        .setView(nameInput)
                        .setCancelable(false)
                        .setPositiveButton("다음", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                final String name = nameInput.getText().toString();
                                final EditText numberInput = new EditText(MainActivity.this);
                                numberInput.setInputType(InputType.TYPE_CLASS_NUMBER);
                                new AlertDialog.Builder(MainActivity.this)
                                        .setTitle("강의실 추가")
                                        .setMessage("강의실의 인원을 설정하세요.")
                                        .setView(numberInput)
                                        .setCancelable(false)
                                        .setPositiveButton("확인", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                int number = Integer.parseInt(numberInput.getText().toString());
                                                classrooms.add(new Classroom(name, number));
                                                saveData();
                                            }
                                        }).show();
                            }
                        }).show();

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
        Type type = new TypeToken<ArrayList<Classroom>>() {}.getType();
        classrooms = gson.fromJson(json, type);
        if (classrooms == null) {
            classrooms = new ArrayList<>();
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
                    public void onClick(View v) {
                        Toast.makeText(MainActivity.this, classroom.name, Toast.LENGTH_LONG).show();
                    }
                });
                mainBodyList.addView(button);
            }
        }
    }

//    private void showNumberOfDevicesDialog() {
//        final EditText e = new EditText(this);
//        e.setInputType(InputType.TYPE_CLASS_NUMBER);
//        new AlertDialog.Builder(this)
//                .setTitle("초기설정")
//                .setMessage("몇개의 장치와 연결하나요?")
//                .setView(e)
//                .setCancelable(false)
//                .setPositiveButton("확인", new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialog, int which) {
//                        if (e.getText().toString().isEmpty()) {
//                            Toast.makeText(MainActivity.this, "입력 오류입니다.", Toast.LENGTH_LONG).show();
//                            showNumberOfDevicesDialog();
//                        } else
//                            mPreferences.edit().putInt("number", Integer.valueOf(e.getText().toString())).apply();
//                    }
//                }).show();
//    }
}
