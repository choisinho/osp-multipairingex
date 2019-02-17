package app.bqlab.multipairingex;


import android.content.Context;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import org.w3c.dom.Text;

import app.akexorcist.bluetotohspp.library.BluetoothSPP;

class Student {
    //variables
    int count, number;
    boolean isConnected, isFinished;
    //objects
    BluetoothSPP bluetooth;

    Student() {
        count = 0;
        isConnected = false;
    }
}