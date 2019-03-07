package app.bqlab.multipairingex;


import app.akexorcist.bluetotohspp.library.BluetoothSPP;

class Student {
    //variables
    int count, number;
    boolean connected, finished;
    //objects
    BluetoothSPP bluetooth;

    Student() {
        count = 0;
        connected = false;
    }
}