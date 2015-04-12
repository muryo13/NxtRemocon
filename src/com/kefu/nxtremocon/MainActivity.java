package com.kefu.nxtremocon;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.Set;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

public class MainActivity extends Activity {

	static final int REQUEST_ENABLE_BT = 0;
//	private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
	private static final char COMMAND_FORWORD = 'J';
	private static final char COMMAND_RIGHT = 'L';
	private static final char COMMAND_LEFT = 'H';
	private static final char COMMAND_BACK = 'K';
	private static final char COMMAND_STOP = 'C';
	
	private BluetoothAdapter mBluetoothAdapter;
	private BluetoothSocket socket;
	private OutputStream outputStream;
	private int nxtCount = 0;
	private Spinner spinerDevices;
	private ArrayAdapter<String> adapter;
	private Set<BluetoothDevice> bondedDevices;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		//bluetooth
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if (mBluetoothAdapter == null) {

			finish(); return;
		}
		if (!mBluetoothAdapter.isEnabled()) {
		    Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
		    startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
		// Otherwise, ok
		} else {
			//nothing.
		}
	
		//connectボタン
		Button button3 = (Button)findViewById(R.id.button3);	
		button3.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				String selectedName = (String)spinerDevices.getSelectedItem();
				if (selectedName != null) {
					connectToDevice(getBluetoothDevice(selectedName));
				}
			}
		});
		
		//Cancelボタン
		Button button4 = (Button)findViewById(R.id.button4);	
		button4.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				cancel();
			}
		});
		
		//デバイスリストのスピナー
		adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item);
	        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
	        		spinerDevices = (Spinner)findViewById(R.id.spinner1);
	        		spinerDevices.setAdapter(adapter);
		spinerDevices.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1,
					int arg2, long arg3) {
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
			}
		});
		setupList();
		
		//前進ボタン
		Button button1 = (Button)findViewById(R.id.button1);
		button1.setOnTouchListener(new KeyListener(COMMAND_FORWORD));

		//回転ボタン
		Button button2 = (Button)findViewById(R.id.button2);
		button2.setOnTouchListener(new KeyListener(COMMAND_RIGHT));
		
		Button button5 = (Button)findViewById(R.id.button5);
		button5.setOnTouchListener(new KeyListener(COMMAND_LEFT));

		Button button6 = (Button)findViewById(R.id.button6);
		button6.setOnTouchListener(new KeyListener(COMMAND_BACK));

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	// デバイスリスト作成
	private void setupList() {
		bondedDevices = mBluetoothAdapter.getBondedDevices();
		for (BluetoothDevice bluetoothDevice : bondedDevices) {
			if (bluetoothDevice.getAddress().contains("00:16:53")) {
				Log.i("", bluetoothDevice.getAddress()
						+ ","
						+ bluetoothDevice.getUuids()
						+ ","
						+ bluetoothDevice.getName()
						);
				adapter.add(bluetoothDevice.getName());
//				connectToDevice(bluetoothDevice);
			}
		}
	}
	
	private BluetoothDevice getBluetoothDevice(String name) {
		if (bondedDevices == null) return null;
		
		for (BluetoothDevice device : bondedDevices) {
			if (device.getName().compareTo(name) == 0) {
				return device;
			}
		}
		return null;
	}

	/**
	 * @param bluetoothDevice
	 */
	private void connectToDevice(BluetoothDevice bluetoothDevice) {
		if (bluetoothDevice == null) return;
		try {
//					socket = bluetoothDevice.createRfcommSocketToServiceRecord(MY_UUID);
			Method m = bluetoothDevice.getClass().getMethod("createRfcommSocket", new Class[] {int.class});
			socket = (BluetoothSocket) m.invoke(bluetoothDevice, 1);
			socket.connect();
			outputStream = socket.getOutputStream();
		} catch (Exception e) {
			Log.e("setup", e.toString());
		}
		
		if(socket != null) {
			new Thread(new EchoBack(nxtCount)).start();
		}
	}
	
	private class EchoBack implements Runnable {

		
		public EchoBack(int number) {
		}

		@Override
		public void run() {
			InputStream input = null;
			try {
				input = socket.getInputStream();
				while(true) {
					byte[] data = new byte[1024];
					input.read(data);
					Log.i("input", data.toString());
				}
			} catch (IOException e) {
				e.printStackTrace();
				try {
					if (input != null) input.close();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		}
	}

	
	private void cancel() {
		try {
		if (outputStream != null)outputStream.close();
		} catch (Exception e) {
			Log.e("cancel", e.toString());
		}
	}

	private void sendCommand(byte command) {
		Message msg = Message.obtain();
		msg.obj = command;
		commandHandler.sendMessage(msg);
	}

	private Handler commandHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			byte[] buffer = {(Byte) msg.obj};
			try {
				if (outputStream != null) {
					Log.i("out", "write:"+buffer.toString());
					outputStream.write(buffer);
				}
			} catch (IOException e) {
				Log.e("write", e.toString());
			}
			
		}
		
	};
	
	class KeyListener implements OnTouchListener {
		
		private byte command;
		
		public KeyListener(char command) {
			this.command = (byte) command;
		}

		@Override
		public boolean onTouch(View v, MotionEvent event) {
			if (event.getAction() == MotionEvent.ACTION_DOWN) {
				sendCommand(command);
			} else if (event.getAction() == MotionEvent.ACTION_UP) {
				sendCommand((byte) COMMAND_STOP);
			}
			return false;
		}
	}
}
