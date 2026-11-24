# Configuración y monitor PIR

- Se guardan las preferencias por módulo HC-06 en SharedPreferences. Se restauran sin ejecutar los listeners de los interruptores y se reenvían al reconectar.
- Sin una elección previa no se envía ningún valor predeterminado. El protocolo A-F no confirma la recepción; el panel muestra la configuración deseada, no el estado físico confirmado.
- El historial conserva los últimos 200 cambios por módulo, con hora local de recepción, sin repetir estados consecutivos. Registra mientras la conexión y el proceso Android sigan activos, incluso fuera del monitor; no registra durante desconexiones ni con la app detenida.
- Tras 10 segundos sin una lectura PIR válida se muestra SIN DATOS RECIENTES. Una desconexión no equivale a ausencia de movimiento.

## Arduino / HC-06

No se incluye el sketch original en este repositorio. El firmware debe enviar los dos estados mediante la UART del HC-06, terminados en salto de línea:

- `PIR:1`: movimiento.
- `PIR:0`: ausencia de movimiento.

Enviar al cambiar y repetir cada 2 segundos permite recuperar el estado después de reconectar. Ejemplo para integrar sin cambiar los pines ni los comandos existentes:

```cpp
void publicarPir(Stream &bluetooth, bool sensorHabilitado, uint8_t pinPir) {
  static int anterior = -1;
  static unsigned long ultimoEnvio = 0;
  if (!sensorHabilitado) {
    anterior = -1;
    return;
  }
  int actual = digitalRead(pinPir);
  unsigned long ahora = millis();
  if (actual != anterior || ahora - ultimoEnvio >= 2000UL) {
    bluetooth.println(actual == HIGH ? "PIR:1" : "PIR:0");
    anterior = actual;
    ultimoEnvio = ahora;
  }
}
```

Llama a publicarPir desde loop() con la instancia serial del HC-06, la variable de habilitación y el pin PIR existentes. Conserva el manejo de A/B (LED1), C/D (LED2), E/F (sensor) y la lógica local de alarma.

## Validación

1. Cambia los interruptores, vuelve al monitor y entra otra vez al panel.
2. Cierra y reabre la app; reconecta y verifica que las preferencias se restauran.
3. Envía PIR:1, PIR:1, PIR:0, PIR:0, PIR:1. Con historial vacío deben aparecer tres eventos.
4. Desconecta y verifica SIN CONEXIÓN; al reconectar espera una lectura nueva.
5. Sin enviar lecturas durante 10 segundos verifica SIN DATOS RECIENTES, sin registrar una falsa ausencia.
6. Reabre la app y verifica el historial.
7. Ejecuta las pruebas de PirMessageParserTest en Android Studio.
