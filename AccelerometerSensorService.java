package com.AccelerometerSensor;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

public class AccelerometerSensorService extends Service implements SensorEventListener{
	private SensorManager mSensorManager;
	private Sensor mAccelerometer;
	private StringBuilder sb = new StringBuilder("[");
	private CountDownTimer timer;
	private Boolean started;
	private long t0;
	private int startedId;
	private Service context;
	private String activity;
	private NotificationManager mNM;
	private int notificationIndex=0;
	private SharedPreferences sharedPreferences;
	private String hash;
	private SharedPreferences.Editor editor;
	private boolean complete;
	
	//Gestión de notificaciones, se gestiona para casos concretos el uso de sonido y vibración.
    public void updateNotification(String message, boolean warning,boolean ongoing){
    	
    	Notification notification = new Notification(R.drawable.icon_512,
                getText(R.string.app_name), System.currentTimeMillis());
    	if(ongoing){
    		notification.flags |= Notification.FLAG_ONGOING_EVENT;
    	}
    	if(warning){
    		notification.defaults |= Notification.DEFAULT_VIBRATE;
    		notification.defaults |= Notification.DEFAULT_SOUND;
    	}
    	
    	
    	notification.setLatestEventInfo(context, getText(R.string.app_name),
                message, PendingIntent.getActivity(context,
                        0, new Intent(context, AccelerometerSensorActivity.class)
                                .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                                        | Intent.FLAG_ACTIVITY_SINGLE_TOP),
                     PendingIntent.FLAG_CANCEL_CURRENT));
        mNM.notify(this.notificationIndex, notification);
    }

	@Override
    public void onCreate() {
        super.onCreate();
        //Recogemos los datos guardados.
        sharedPreferences = getSharedPreferences("confFile",0);
        editor = sharedPreferences.edit();
        hash = sharedPreferences.getString("hash", "");
        //Se avisa a la activity de que el servicio está en funcionamiento.
        editor.putBoolean("serviceOn", true);
        editor.commit();
        //Se inicializa el sensor. NO se registra todavía.
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        //Variables de control del flujo de ejecución.
        started= false;
        complete = false;
        context = this;
        mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        
        updateNotification("Getting ready for data record...",false,true);

     
        //Iniciamos un timer con 55 segundos, empezará a recoger muestras cuando queden 50segundos. Damos 5 segundos de margen 
        //para guardar el teléfono en el bolsillo.
        timer = new CountDownTimer(55000, 1000) {//55000
        	public void onTick(long millisUntilFinished) {
        		if(millisUntilFinished<=51000){
        			if(!started){
        				//Cuando quedan 50segundos, registramos el sensor para obtener las muestras. y fijamos el origen de tiempos de la captura.
        				registerListener();
            			started = true;
            			t0 = System.currentTimeMillis();
        			}
        			//Informamos del tiempo restante.
        			updateNotification("Acquiring motion data, "+(int)Math.floor(millisUntilFinished/1000)+"s left",false,true);
        			if((int)Math.floor(millisUntilFinished)<2000){
        				complete = true;   				
        			}
        		}
        		
        		Log.i("SENSOR",""+millisUntilFinished);
        		}
        	

   	     	public void onFinish() {
   	     		started = false;
   	     		//Cuando acaba el timer o se destruye el servicio desregistramos el sensor para que no gaste batería.
   	     		desregisterListener();
   	     		if (complete){
   	     			//Si se capturaron 50 segundos de muestras, se termina el array y se envia.
	   	     		sb.append("["+String.valueOf((System.currentTimeMillis()-t0))+",0,0,0]]");
	   	     		enviarDatos();
   	     		}
   	     		//Se destruye el servicio.
   	     		context.stopSelf(startedId);
   	     	}
   	  	}.start();
    }
	@Override
	public void onDestroy(){
		super.onDestroy();
		//Si se invoca desregistramos el sensor para que no se quede gastando batería. Cancelamos el timer y actualizamos los datos de la aplicación.
		started = false;
		desregisterListener();
		timer.cancel();
		Log.i("SENSOR","adios1" +complete);
		if(!complete){
			Log.i("SENSOR","adios2");
			mNM.cancel(notificationIndex);
			complete = false;
		}
		editor.putBoolean("serviceOn", false);
        editor.commit();
		context.stopSelf(startedId);	
		
	}
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
	    Log.i("SENSOR", "Received start id " + startId + ": " + intent);
	    Bundle extras = intent.getExtras();
	    //Obtenemos el botón pulsado.
	    activity = extras.getString("activity");
	    startedId = startId;
	    return START_STICKY;
	    
	}

	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		
		return null;
	}
	//Registro del acelerómetro.
	private void registerListener(){
		mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_FASTEST);
		Log.i("SENSOR","start accelerometer");
	}
	//Desregistro del acelerómetro.
	private void desregisterListener(){
		mSensorManager.unregisterListener(this);
		Log.i("SENSOR","stop Accelerometer");
	}

	public void onAccuracyChanged(Sensor arg0, int arg1) {
		// TODO Auto-generated method stub
			
	 }
	//Capturamos un dato del acelerómetro y lo añadimos al StringBuilder.
	public void onSensorChanged(SensorEvent event) {
		float mAccelerometerValues[] = new float[3];
		mAccelerometerValues = event.values;
		sb.append("["+String.valueOf((System.currentTimeMillis()-t0))+","+mAccelerometerValues[0]+","+mAccelerometerValues[1]+","+mAccelerometerValues[2]+"],");
	}
	
	//envío de datos al servidor
	private void enviarDatos(){
		updateNotification("Adquisition complete, sending data...",true,false);
		try{
			HttpPost post = new HttpPost(getString(R.string.URL));  
			List <BasicNameValuePair> parameters = new ArrayList <BasicNameValuePair>();
			/*
			 * Se envían
			 * 1. "devid":Hash almacenado en los datos de la aplicación (MD5 de número aleatorio).
			 * 2. "label":Etiqueta de la actividad elegida.
			 * 3. "records":Datos del acelerómetro.
			 */
			parameters.add(new BasicNameValuePair("devid", hash));
			parameters.add(new BasicNameValuePair("label", activity));
			parameters.add(new BasicNameValuePair("records",sb.toString()));
			UrlEncodedFormEntity sendentity = new UrlEncodedFormEntity(parameters, HTTP.UTF_8);
			post.setEntity(sendentity);   
			 
			HttpClient client = new DefaultHttpClient();  
			HttpResponse response = client.execute(post);
			ResponseHandler<String> responseHandler = new BasicResponseHandler();
			responseHandler.handleResponse(response);
	        Log.i("SENSOR","response Code "+response.getStatusLine().getStatusCode());
			Log.i("SENSOR","datos enviados ");
			updateNotification("Data succesfully sent",false,false);
			Toast toast = Toast.makeText(context, "Data succesfully sent"  , Toast.LENGTH_LONG);
			toast.show();
		}catch(Exception e){
			updateNotification("Data could not be sent",false,false);
			Toast toast = Toast.makeText(this.getBaseContext(), "An error occurred, no data sent.", Toast.LENGTH_LONG);
			Log.i("SENSOR","error al enviar "+e.getMessage());
			toast.show();
		}
		//Se avisa a la activity del fin del servicio para que restaure el estado inicial de la vista.
		sendBroadcast(new Intent("service_END"));
		
	}
	
	//Posible mejora futura: envío de datos comprimidos.
	/*
	public String encodeToSend(String input) {
    	try{
	        ByteArrayOutputStream out = new ByteArrayOutputStream();
	        OutputStreamWriter writer = new OutputStreamWriter(new GZIPOutputStream(out)); // USAR FLATTERED OUTPUT STREAM http://stackoverflow.com/questions/1406917/compressing-with-java-decompressing-with-php

	        writer.write(input);
	        writer.close();
	        return Base64.encodeToString(out.toByteArray(),Base64.DEFAULT);
    	}catch(IOException e){
    		Log.i("SENSOR",""+e);
    		return "";
    	}
    }*/
}
