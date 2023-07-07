package com.example.carro_giroscopio;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private static final String DEVICE_ADDRESS = "98:DA:20:04:F6:D1"; // Dirección MAC del módulo Bluetooth HC-04
    private static final UUID PORT_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothDevice bluetoothDevice;
    private BluetoothSocket bluetoothSocket;
    private OutputStream outputStream;

    private SensorManager sensorManager;
    private Sensor accelerometerSensor;
    private float accelerometerValueX, accelerometerValueY, accelerometerValueZ;

    private Handler handler;
    private Runnable sendRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // Conectar con el dispositivo Bluetooth
        bluetoothDevice = bluetoothAdapter.getRemoteDevice(DEVICE_ADDRESS);
        try {
            bluetoothSocket = bluetoothDevice.createInsecureRfcommSocketToServiceRecord(PORT_UUID);
            bluetoothSocket.connect();
            outputStream = bluetoothSocket.getOutputStream();
            Toast.makeText(MainActivity.this, "Conexión Bluetooth establecida", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Inicializar el sensor del acelerómetro
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }

        // Configurar el envío constante de la coordenada X del acelerómetro
        handler = new Handler();
        sendRunnable = new Runnable() {
            @Override
            public void run() {
                sendAccelerometerValueX();
                handler.postDelayed(this, 1000); // Intervalo de envío (en milisegundos)
            }
        };
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (accelerometerSensor != null) {
            sensorManager.registerListener(this, accelerometerSensor, SensorManager.SENSOR_DELAY_NORMAL);
        } else {
            Toast.makeText(this, "El dispositivo no admite el sensor de acelerómetro", Toast.LENGTH_SHORT).show();
        }

        // Iniciar el envío constante cuando se reanuda la actividad
        handler.post(sendRunnable);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }

        // Detener el envío constante cuando se pausa la actividad
        handler.removeCallbacks(sendRunnable);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            // Actualizar los valores del acelerómetro
            accelerometerValueX = event.values[0];
            accelerometerValueY = event.values[1];
            accelerometerValueZ = event.values[2];
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // No es necesario implementar esto aquí
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (bluetoothSocket != null) {
            try {
                outputStream.close();
                bluetoothSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void sendAccelerometerValueX() {
        // Verificar si el outputStream no es nulo
        if (outputStream != null) {
            // Enviar solo el valor de la coordenada X del acelerómetro a través del Bluetooth
            String message = "";
            if (accelerometerValueX >= 0 && accelerometerValueX < 2) {
                message = "forward";
            } else if (accelerometerValueX <= 0 && accelerometerValueX > -2) {
                message = "backward";
            } else if (accelerometerValueX >= 5) {
                message = "left";
            } else if (accelerometerValueX <= -5) {
                message = "right";
            } else {
                message = "stop";
            }
            try {
                outputStream.write(message.getBytes());
                Toast.makeText(MainActivity.this, "Enviado correctamente", Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            Toast.makeText(MainActivity.this, "Error en la conexión Bluetooth", Toast.LENGTH_SHORT).show();
        }
    }
}
