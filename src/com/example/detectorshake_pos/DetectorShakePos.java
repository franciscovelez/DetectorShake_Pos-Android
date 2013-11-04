package com.example.detectorshake_pos;

import java.util.ArrayList;
import java.util.List;
import android.content.pm.ActivityInfo;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;
import android.widget.ProgressBar;
import android.widget.TextView;

public class DetectorShakePos extends Activity implements SensorEventListener{
	private long last_update, current_time, time_difference; //Para controlar el tiempo
	private float curX, curY, curZ; //Para almacenar el valor de cada eje
	private float umbral_pos; //Para detectar la posici�n
	//Para calcular las aceleraciones
	private float delta;
	private float mAccel;
	private float mAccelLast; 
	private float mAccelCurrent;
	//Para controlar y detectar las agitaciones
	private int history_size, umbral_shake, umbral_cont; 
	private ArrayList<Float> mov_history = new ArrayList<Float>();		
	int contn, contp, cont, tmp;
	private MediaPlayer sonido = null; //Reproductor de sonidos

		/**************************************
		 * Eventos de estado de la aplicaci�n *
		 **************************************/
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_detector_shake_pos);
		this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		
		//Inicializamos los atributos
		sonido = MediaPlayer.create(this, R.raw.touch);		
		cont   = 0;
		mAccel = 0;
		last_update = 0;
		curX = curY = curZ = 0;
		mAccelLast    = SensorManager.GRAVITY_EARTH; 
		mAccelCurrent = SensorManager.GRAVITY_EARTH;		
		history_size  = 50; //Tama�o del vector
		umbral_shake  = 7;  //Valor de gravedad para considerar una agitaci�n
		umbral_cont   = 4;  //n� de agitaciones necesarias para que sea una agitaci�n completa
		umbral_pos    = 7f; //Valor necesario para detectar que el dispositivo est� en una posici�n
		
		//Ponemos el vector a cero
		for(int i=0; i<history_size; i++)	
			mov_history.add(0f);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.detector_shake_pos, menu);
		return true;
	}

	@Override
	protected void onResume() {
		super.onResume();
		SensorManager sm = (SensorManager) getSystemService(SENSOR_SERVICE);
		List<Sensor> sensors = sm.getSensorList(Sensor.TYPE_ACCELEROMETER);				
		if (sensors.size() > 0) {
			sm.registerListener(this, sensors.get(0), SensorManager.SENSOR_DELAY_GAME);
		}
	}
	
	@Override
	protected void onStop() {
		SensorManager sm = (SensorManager) getSystemService(SENSOR_SERVICE);			
		sm.unregisterListener(this);
		super.onStop();
	}

					
	/**************************************
	 * Eventos propios del sensor				 *
	 **************************************/
	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {}

	//Cuando se detecten nuevos datos en el sensor
	@Override
	public void onSensorChanged(SensorEvent event) {		
		synchronized (this) {
			TextView textPantalla = ((TextView) findViewById(R.id.texto));	
			//Monitoreamos la frecuencia de muestreo del acelerometro
			current_time = event.timestamp;
			time_difference = current_time - last_update;
			if(time_difference>10E8 && cont!=0){
				last_update = current_time;
				System.out.println("Frequency: "+cont+"Hz");
				cont=0;
			}
			cont++;
			
			//Obtenemos las lecturas de la aceleracion separada por ejes
			curX = event.values[0];
			curY = event.values[1];
			curZ = event.values[2];
			
			//Comparamos la lectura de aceleracion anterior con la actual para detectar movimiento de
			//aceleracion/desaceleracion. Como aceleracion se obtiene una medida global que es la 
			//ra�z cuadrada de la suma de los cuadrados de la aceleracion en cada eje
			mAccelLast    = mAccelCurrent;
			mAccelCurrent = (float) Math.sqrt((double) (curX*curX + curY*curY + curZ*curZ));
			delta         = mAccelCurrent - mAccelLast;
			mAccel        = mAccel * 0.9f + delta;
			mAccelLast    = mAccelCurrent;
			//Almacenamos en una cola las history_size ultimas lecturas para la deteccion del gesto shake
			mov_history.add(mAccel);
			mov_history.remove(0);
			
			//Contamos las lecturas mayores y menores que umbral_shake y -umbral_shake
			//en mov_history
			contp=contn=0;
			for(int i=0; i<history_size; i++){
				if(mov_history.get(i)>umbral_shake)	contp++;
				if(mov_history.get(i)<-umbral_shake) contn++;
			}
			
			//Si hemos contado los suficientes registros por encima del valor umbral de
			//aceleracion en mov_history entonces hemos detectado un SHAKE
			if(contp>umbral_cont && contn>umbral_cont){
				textPantalla.setText("Movimiento Shake Detectado");
				sonido.start();
				System.out.println("SHAKE DETECTADO");
				tmp=((ProgressBar) findViewById(R.id.progressBar1)).getProgress();
				((ProgressBar) findViewById(R.id.progressBar1)).setProgress(tmp+5);
		
				for (int i=0; i<history_size; i++) 
					mov_history.set(i, 0f);							 
			}
			
			//Detectamos si el dispositivo esta bocarriba o bocabajo
			if(curX<-umbral_pos) textPantalla.setText("Posici�n: Horizontal");
			if(curX> umbral_pos) textPantalla.setText("Posici�n: Horizontal inverso");
			if(curY<-umbral_pos) textPantalla.setText("Posici�n: Vertical inverso");
			if(curY> umbral_pos) textPantalla.setText("Posici�n: Vertical");
			if(curZ<-umbral_pos) textPantalla.setText("Posici�n: Bocabajo");
			if(curZ> umbral_pos) textPantalla.setText("Posici�n: Bocarriba");
			
			
			//Lectura actual del acelerometro
			((TextView) findViewById(R.id.txtAccX)).setText("Valor de X: " + curX);
			((TextView) findViewById(R.id.txtAccY)).setText("Valor de Y: " + curY);
			((TextView) findViewById(R.id.txtAccZ)).setText("Valor de Z: " + curZ);						
		}		
	}	
}
