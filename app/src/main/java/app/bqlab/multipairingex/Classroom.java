package app.bqlab.multipairingex;

class Classroom {
    String name;
    Student[] students;
    Classroom(String name, int number) {
        this.name = name;
        students = new Student[number];
    }
}
