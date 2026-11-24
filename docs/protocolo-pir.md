# Protocolo PIR y registro

El monitor reconoce los mensajes reales del sketch Arduino original:
- ALERTA: Movimiento detectado -> movimiento (rojo).
- ESTADO: Zona despejada -> sin movimiento (verde).
- OK: Sensor PIR ACTIVADO -> espera de lectura.
- OK: Sensor PIR DESACTIVADO -> sensor apagado (gris).

El original solo publica zona despejada después de una detección. Por eso, para conocer el estado LOW desde el arranque o al activar el PIR, carga el firmware actualizado en firmware/ARDUINO_IOT/ARDUINO_IOT.ino. Conserva A-F, pines D4/D6/D7, HC-06 RX11/TX10 y 9600 baudios. Es el protocolo de texto del proyecto Android, no el firmware cifrado de otros proyectos.

El nuevo firmware publica PIR:1, PIR:0 y PIR:OFF, inmediatamente al cambiar y cada 2 segundos. Android distingue sensor apagado de zona despejada. Con el firmware original, conserva la última lectura confirmada mientras siga conectado; con el nuevo, si faltan lecturas por 10 segundos, indica SIN DATOS RECIENTES (no inventa una ausencia). Al apagar se espera confirmación. El panel guarda configuración deseada y la reenvía; no confirma físicamente cada LED.

Historial local por HC-06: conserva los últimos 200 eventos, incluida activación, desactivación, movimiento y zona despejada. Se mantienen las detecciones guardadas por la versión anterior. Los mensajes repetidos de un mismo estado no generan entradas; una nueva sesión de conexión registra su primera lectura. En pantalla hay resumen, ocho eventos recientes y un botón para ver todo el historial. La fecha corresponde a la recepción en Android; no se recuperan eventos ocurridos con el teléfono desconectado o la app detenida.

## Prueba manual
1. Carga el firmware actualizado; conecta el HC-06.
2. Activa el sensor sin movimiento: debe aparecer SIN MOVIMIENTO en verde.
3. Provoca movimiento: debe aparecer MOVIMIENTO DETECTADO en rojo.
4. Espera a que la salida del PIR vuelva a LOW: debe aparecer SIN MOVIMIENTO.
5. Repite el paso 3 y 4 dos veces. Comprueba varias entradas alternadas en el historial.
6. Desactiva el PIR: debe aparecer SENSOR APAGADO en gris y una entrada de apagado.
7. Abre Ver historial completo, cierra y reabre la app y revisa que persistan los eventos.
8. Desconecta el HC-06; no debe mostrarse una falsa ausencia de movimiento.
