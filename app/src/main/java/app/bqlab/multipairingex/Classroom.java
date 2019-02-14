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
        for (Student student : students) {
            student = new Student();
        }
    }
}
