package app.bqlab.multipairingex;

import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.google.gson.Gson;

import java.util.Map;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    SharedPreferences mSettingPref, mClassroomPref, mClasseNamePref;
    LinearLayout mainBodyList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();
        loadClassrooms();
    }

    private void init() {
        //objects
        mSettingPref = getSharedPreferences("setting", MODE_PRIVATE);
        mClassroomPref = getSharedPreferences("classroom", MODE_PRIVATE);
        mClasseNamePref = getSharedPreferences("classname", MODE_PRIVATE);
        //layouts
        mainBodyList = findViewById(R.id.main_body_list);
        //initializing or setting
        findViewById(R.id.main_bot_add).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Classroom classroom;
                final EditText nameInput = new EditText(MainActivity.this);
                final EditText numberInput = new EditText(MainActivity.this);
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("강의실 추가")
                        .setMessage("강의실의 이름을 설정하세요.")
                        .setView(nameInput)
                        .setCancelable(false)
                        .setPositiveButton("다음", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                final String name = nameInput.getText().toString();
                                if (Objects.equals(mClasseNamePref.getString(String.valueOf(name.hashCode()), ""), "")) {
                                    mClasseNamePref.edit().putString(String.valueOf(name.hashCode()), name).apply();
                                    new AlertDialog.Builder(MainActivity.this)
                                            .setTitle("강의실 추가")
                                            .setMessage("강의실의 인원을 설정하세요.")
                                            .setView(numberInput)
                                            .setCancelable(false)
                                            .setPositiveButton("확인", new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    SharedPreferences.Editor editor = mClassroomPref.edit();
                                                    Gson gson = new Gson();
                                                    String json = gson.toJson(Classroom.class);
                                                    editor.putString(name, json);
                                                    editor.apply();
                                                    loadClassrooms();
                                                }
                                            }).show();
                                }
                            }
                        })
                        .show();

            }
        });
    }

    private void loadClassrooms() {
        Map<String, ?> names = mClasseNamePref.getAll();
        if (!names.isEmpty()) {
            findViewById(R.id.main_body_list_empty).setVisibility(View.GONE);
            for (final Map.Entry<String, ?> entry : names.entrySet()) {
                Button button = new Button(this);
                final String name = entry.getKey();
                button.setText(name);
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Gson gson = new Gson();
                        String json = mClassroomPref.getString(name, "");
                        Classroom classroom = gson.fromJson(json, Classroom.class);
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
