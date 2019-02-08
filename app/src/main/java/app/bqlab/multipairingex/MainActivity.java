package app.bqlab.multipairingex;

import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    SharedPreferences mPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();
        showNumberOfDevicesDialog();
    }

    private void init() {
        mPreferences = getSharedPreferences("setting", MODE_PRIVATE);
    }

    private void showNumberOfDevicesDialog() {
        final EditText e = new EditText(this);
        new AlertDialog.Builder(this)
                .setTitle("초기설정")
                .setMessage("몇개의 장치와 연결하나요?")
                .setView(e)
                .setCancelable(false)
                .setPositiveButton("확인", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (e.getText().toString().isEmpty()) {
                            Toast.makeText(MainActivity.this, "입력 오류입니다.", Toast.LENGTH_LONG).show();
                            showNumberOfDevicesDialog();
                        } else
                            mPreferences.edit().putInt("number", Integer.valueOf(e.getText().toString())).apply();
                    }
                }).show();
    }
}
