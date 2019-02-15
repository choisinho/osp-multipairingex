package app.bqlab.multipairingex;

import android.content.Context;
import android.util.Log;

import java.io.Serializable;

class Classroom implements Serializable {
    String name;
    Student[] students;

    Classroom(String name, int number) {
        this.name = name;
        students = new Student[number];
        for (int i = 0; i < students.length; i++) {
            students[i] = new Student();
        }
    }
}
