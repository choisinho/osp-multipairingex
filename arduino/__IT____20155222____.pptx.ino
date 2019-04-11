/* 라이브러리 선언*/
#include <SoftwareSerial.h>
#include <util/delay.h>
#include <SPI.h>
#include <MFRC522.h>

/* 변수 선언*/
uint8_t RST_PIN = 9;        // 구성가능, 일반 핀 참조
uint8_t SS_PIN = 10;        // 구성가능, 일반 핀 참조
uint8_t sw = 2;
uint8_t red = 5;            // LED 빨강 핀
uint8_t green = 3;          // LED 초록 핀
uint8_t blue = 4;           // LED 파랑 핀
int deviceNum = 0;
int count = 0;                    // 카운트 저장 변수
int save_count = 0;               // 카운트을 저장해서 보내주는 변수
int sw_state = 0;                 // 스위치 상태 저장 변수
uint32_t time = 0;                // 프로그램 시간 저장 변수
char strExitCount[100] = {0, };   // 종료 카운트 문자열로 수신
int bufferPosition = 0;           // 문자열 길이 확인용 변수
int exitVal = 0;                  // 종료 변수

/* 객체 선언 */
MFRC522 mfrc(SS_PIN, RST_PIN);    // MFRC522 인스턴스 생성
SoftwareSerial mySerial (7, 8);

void setup() {
  Serial.begin(9600);   // 시리얼 통신 시작
  mySerial.begin(9600);  // 블루투스 시작
  SPI.begin();                        // SPI 버스 시작
  mfrc.PCD_Init();                    // MFRC522 시작
  mfrc.PCD_DumpVersionToSerial();     // PCD의 세부정보 표시 - MFRC522 카드 세부정보 표시

  // led는 OUTPUT, sw는 내부 풀업 저항으로 INPUT되게 설정
  pinMode(red, OUTPUT);
  pinMode(green, OUTPUT);
  pinMode(blue, OUTPUT);
  pinMode(sw, INPUT_PULLUP);
  attachInterrupt(INT0, led, FALLING);  // 스위치가 FALLING, HIGH->LOW가 될 때 인터럽트 실행
  digitalWrite(red, LOW);
  digitalWrite(green, LOW);
  digitalWrite(blue, LOW);

  while (1) {
    if (mySerial.available()) {
      deviceNum = mySerial.read() - 48;
      break;
    }
  }
}

void loop() {
  // 태그가 연속적으로 읽히지 않게 3초정도 간격을 두고 리딩
  if (deviceNum == -1) {
    while (1) {
      if (mySerial.available()) {
        deviceNum = mySerial.read() - 48;
        break;
      }
    }
  }


  if ((millis() - time) > 3000) {
    if (mfrc.PICC_IsNewCardPresent()) {
      time = millis();
      count++;
      save_count++;
      return;
    }
  }

  // 인식되는 숫자에 따라 led 색깔 변경
  if (count <  2 && (save_count == count) )
    led_red();    // 0~2번 빨간 led
  else if (2 <= count  && count < 5)
    led_green();  // 2~4번 그린 led
  else if ( count >= 5)
    led_orange(); // 5번 이상 오렌지 led

  // 스위치가 눌리면 led off 시키고 카운트 초기화시킨 뒤 앱으로 종료 신호 전송
  if (sw_state == 1) {
    mySerial.println(255);
    digitalWrite(red, LOW);
    digitalWrite(green, LOW);
    digitalWrite(blue, LOW);
    count = 0;
    sw_state = 0;
    delay(300);
    led_red();
  }

  // 앱에서 종료 신호를 아스키코드로 보내면, 이를 문자열과 숫자 형태로 바꿔 확인하고, 태그 값을 저장한 카운트 보내주고, 변수들 다 초기화
  if (mySerial.available()) {
    char myChar = mySerial.read();
    if (myChar == 'A') {
      mySerial.println(save_count);
      sw_state = 0;
      count = 0;
      save_count = 0;
      myChar = 0;
      led_red();
      delay(3000);
    }
    else if (myChar == 'B') {
      mySerial.println(save_count);
      sw_state = 0;
      count = 0;
      save_count = 0;
      deviceNum = -1;
      myChar = 0;
      led_red();
      delay(3000);
    }
    Serial.println(myChar);
  }
  if (deviceNum != -1) {
    mySerial.println(deviceNum);        // 블루투스로 디바이스 넘버 실시간 전송
    mySerial.println(count + 10); // 블루투스로 카운트 실시간 전송
  }
  //Serial.println(count);             // 시리얼 모니터 카운트 실시간 출력
  delay(1001);
}

void led_green() { // 초록색
  digitalWrite(red, LOW);
  digitalWrite(green, HIGH);
  digitalWrite(blue, LOW);
}
void led_orange() { // 오렌지색
  analogWrite(red, 255);
  analogWrite(green, 165);
  analogWrite(blue, 0);
}
void led_red() {  // 빨간색
  digitalWrite(red, HIGH);
  digitalWrite(green, LOW);
  digitalWrite(blue, LOW);
}

// 디바운싱 적용
void led () {
  _delay_ms(100);   // 100ms 대기
  if (digitalRead(sw) == HIGH)  // 대기 후에도 스위치가 High [안눌린 상태]면 리턴
    return;
  sw_state = !sw_state; // Low [눌린 상태]라면 상태 변경
}
