#include <SoftwareSerial.h>

// Firmware para la app Android con protocolo Bluetooth A-F.
// Conserva los pines y la velocidad del sketch original.
SoftwareSerial bluetooth(11, 10);
const int ledVerde = 6;
const int ledRojo = 7;
const int sensor = 4;
bool sensorActivo = false;
int estadoAnterior = -1;
unsigned long ultimoEnvio = 0;
const unsigned long INTERVALO = 2000UL;

void publicarEstado() {
  if (!sensorActivo) {
    bluetooth.println("PIR:OFF");
    return;
  }
  int lectura = digitalRead(sensor);
  digitalWrite(ledRojo, lectura == HIGH ? HIGH : LOW);
  digitalWrite(ledVerde, lectura == HIGH ? LOW : HIGH);
  bluetooth.println(lectura == HIGH ? "PIR:1" : "PIR:0");
  estadoAnterior = lectura;
  ultimoEnvio = millis();
}

void setup() {
  Serial.begin(9600);
  bluetooth.begin(9600);
  pinMode(ledVerde, OUTPUT);
  pinMode(ledRojo, OUTPUT);
  pinMode(sensor, INPUT);
  digitalWrite(ledRojo, LOW);
  digitalWrite(ledVerde, LOW);
  publicarEstado();
}

void loop() {
  while (bluetooth.available()) {
    char dato = bluetooth.read();
    switch (dato) {
      case 'A': digitalWrite(ledRojo, HIGH); bluetooth.println("OK: LED Rojo ENCENDIDO"); break;
      case 'B': digitalWrite(ledRojo, LOW); bluetooth.println("OK: LED Rojo APAGADO"); break;
      case 'C': digitalWrite(ledVerde, HIGH); bluetooth.println("OK: LED Verde ENCENDIDO"); break;
      case 'D': digitalWrite(ledVerde, LOW); bluetooth.println("OK: LED Verde APAGADO"); break;
      case 'E':
        sensorActivo = true;
        estadoAnterior = -1;
        bluetooth.println("OK: Sensor PIR ACTIVADO");
        publicarEstado(); // También publica LOW al activar sin movimiento.
        break;
      case 'F':
        sensorActivo = false;
        estadoAnterior = -1;
        digitalWrite(ledRojo, LOW);
        digitalWrite(ledVerde, LOW);
        bluetooth.println("OK: Sensor PIR DESACTIVADO");
        ultimoEnvio = millis();
        break;
      case 'Q': publicarEstado(); ultimoEnvio = millis(); break;
      case '\r': case '\n': case ' ': break;
      default: bluetooth.println("ERROR: Comando invalido"); break;
    }
  }
  unsigned long ahora = millis();
  if (sensorActivo) {
    int lectura = digitalRead(sensor);
    if (lectura != estadoAnterior || ahora - ultimoEnvio >= INTERVALO) {
      publicarEstado();
    }
  } else if (ahora - ultimoEnvio >= INTERVALO) {
    publicarEstado();
    ultimoEnvio = ahora;
  }
}
