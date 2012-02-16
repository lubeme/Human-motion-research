package com.AccelerometerSensor;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.ToggleButton;




public class AccelerometerSensorActivity extends Activity {
	//Obtenemos el evento de fin de servicio y actualizamos los estados de los botones de la interfaz.
	private class DataUpdateReceiver extends BroadcastReceiver {
	    @Override
	    public void onReceive(Context context, Intent intent) {
	        if (intent.getAction().equals("service_END")) {
	        	Log.i("SENSOR","event recieved");
	        	toggle.setChecked(false);
	        	spinner.setEnabled(true);
	        	
	        }
	    }
	}
	
	private DataUpdateReceiver dataUpdateReceiver;
	private ToggleButton toggle; 

    private Spinner spinner;
    //Claves de envío al servidor, diferentes de las mostradas al usuario.
    private String [] arrayLabels = {"walk","car","metro","bus","run","upstairs","downstairs","stop"};
    private Context context;
    private SharedPreferences sharedPreferences;
    
    @Override
    protected void onResume(){
    	super.onResume();
    	//Comprobamos el estado de la aplicación y se actualiza la interfaz como corresponda.
    	if (dataUpdateReceiver == null) dataUpdateReceiver = new DataUpdateReceiver();
    	IntentFilter intentFilter = new IntentFilter("service_END");
    	boolean running=sharedPreferences.getBoolean("serviceOn",false);
    	if(!running){
    		toggle.setChecked(false);
    		spinner.setEnabled(true);
    	}else{
    		toggle.setChecked(true);
    		spinner.setEnabled(false);
    	}
    	registerReceiver(dataUpdateReceiver, intentFilter);
    	Log.i("SENSOR","succesfully registered reciever");
    	

    	
    }
    @Override
    protected void onPause(){
    	super .onPause();
    	if (dataUpdateReceiver != null) unregisterReceiver(dataUpdateReceiver);
    	Log.i("SENSOR","succesfully unregistered reciever");
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Obtenemos los datos de la aplicación.
        sharedPreferences = getSharedPreferences("confFile",0);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        boolean isFirstTime = sharedPreferences.getBoolean("isFirstTime", true);
        //Si es la primera ejecución se genera un hash anónimo de usuario que se almacena en los datos.
        if(isFirstTime){
        	Random random = new Random(System.currentTimeMillis());	
        	String hash = md5((String.valueOf(random.nextInt())));     	
        	editor.putString("hash",hash);
        	editor.putBoolean("isFirstTime", false);
        	editor.commit();
        }
        //Inicializamos la interfaz
        setContentView(R.layout.main);
        spinner = (Spinner) findViewById(R.id.spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.activity_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        toggle = (ToggleButton)findViewById(R.id.toggleButton1);
        toggle.setTextOff("Start");
        toggle.setTextOn("Abort");
        toggle.setChecked(false);
        context = this.getBaseContext();
        
        toggle.setOnClickListener(new OnClickListener(){
        	public void onClick(View v){  
        		if(toggle.isChecked()){
        			Log.i("SENSOR", "call service");
        			Intent intent = new Intent(context, AccelerometerSensorService.class);
        			Log.i("SENSOR",""+spinner.getSelectedItemPosition());
        			//Llamada al servicio al pulsar el botón.
        			intent.putExtra("activity", arrayLabels[spinner.getSelectedItemPosition()]);
        			startService(intent);
        			spinner.setEnabled(false);
        			Log.i("SENSOR", "service called");
        			
        		}else{
        			Intent intent = new Intent(context, AccelerometerSensorService.class);
        			spinner.setEnabled(true);
        	        stopService(intent);
        		}
        	}
        });
        
        
    }
    //Codificación del hash de usuario.
    private String md5(String s) {
    	String hash="";
    	try{
            MessageDigest m = MessageDigest.getInstance("MD5");
            m.update(s.getBytes(),0,s.length());
    	    hash = new BigInteger(1, m.digest()).toString(16);
    	}catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
    	}
    	return hash;
	   
    }
   
}