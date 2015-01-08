package com.sebi.cardgame;

import java.util.ArrayList;
import java.util.Random;
import java.util.Set;

import android.R.integer;
import android.app.ActionBar;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.ToggleButton;

public class MainActivity extends Activity {

	private ArrayList<String> cardsArrayList = new ArrayList<String>() {
		{
			add("gnd2"); // 0
			add("gnd3"); // 1
			add("gnd4"); // 2
			add("gnd7"); // 3
			add("gnd8"); // 4
			add("gnd9"); // 5
			add("gnd10"); // 6
			add("gnd11"); // 7

			add("ros2"); // 8
			add("ros3"); // 9
			add("ros4"); // 10
			add("ros7"); // 11
			add("ros8"); // 12
			add("ros9"); // 13
			add("ros10"); // 14
			add("ros11"); // 15

			add("tob2"); // 16
			add("tob3"); // 17
			add("tob4"); // 18
			add("tob7"); // 19
			add("tob8"); // 20
			add("tob9"); // 21
			add("tob10"); // 22
			add("tob11"); // 23

			add("vrd2"); // 24
			add("vrd3"); // 25
			add("vrd4"); // 26
			add("vrd7"); // 27
			add("vrd8"); // 28
			add("vrd9"); // 29
			add("vrd10"); // 30
			add("vrd11"); // 31
		}
	};

	private ArrayList<Integer> dealtCards = new ArrayList<>();

	private ArrayList<Integer> takenCards = new ArrayList<>();
	
	private ArrayList<Integer> currentHandCards = new ArrayList<>();

	private int card1;
	private int card2;
	private int card3;
	private int card4;
	private int firstCard;
	private int secondCard;
	private int firstCardOfHand;

	private boolean gameStarted = false;
	private boolean firstCardSW = true;
	private boolean myHand = false;
	private boolean firstHand = true;

	// Debugging
	private static final String TAG = "BluetoothChat";

	// Message types sent from the BluetoothChatService Handler
	public static final int MESSAGE_STATE_CHANGE = 1;
	public static final int MESSAGE_READ = 2;
	public static final int MESSAGE_WRITE = 3;
	public static final int MESSAGE_DEVICE_NAME = 4;
	public static final int MESSAGE_TOAST = 5;

	// Key names received from the BluetoothChatService Handler
	public static final String DEVICE_NAME = "device_name";
	public static final String TOAST = "toast";

	// Intent request codes
	private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
	// private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
	private static final int REQUEST_ENABLE_BT = 3;

	BluetoothAdapter mBluetoothAdapter;
	ArrayAdapter<String> mArrayAdapter;

	// Member object for the chat services
	private BluetoothChatService mChatService = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// Get local Bluetooth adapter
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

		// If the adapter is null, then Bluetooth is not supported
		if (mBluetoothAdapter == null) {
			Toast.makeText(this, "Bluetooth is not available",
					Toast.LENGTH_LONG).show();
			finish();
			return;
		}

		/*
		 * ToggleButton bluetoothToggleBtn = (ToggleButton)
		 * findViewById(R.id.toggleButton1);
		 * 
		 * if (mBluetoothAdapter.isEnabled())
		 * bluetoothToggleBtn.setChecked(true); else
		 * bluetoothToggleBtn.setChecked(false);
		 */
	}

	@Override
	public void onStart() {
		super.onStart();
		Log.e(TAG, "++ ON START ++");

		// If BT is not on, request that it be enabled.
		// setupChat() will then be called during onActivityResult
		if (!mBluetoothAdapter.isEnabled()) {
			Intent enableIntent = new Intent(
					BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
			// Otherwise, setup the chat session
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		/*
		 * // Handle action bar item clicks here. The action bar will //
		 * automatically handle clicks on the Home/Up button, so long // as you
		 * specify a parent activity in AndroidManifest.xml. int id =
		 * item.getItemId(); if (id == R.id.action_settings) { return true;
		 */
		Intent serverIntent = null;
		switch (item.getItemId()) {
		case R.id.secure_connect_scan:
			// Launch the DeviceListActivity to see devices and do scan
			serverIntent = new Intent(this, DeviceListActivity.class);
			startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_SECURE);
			return true;
		case R.id.discoverable:
			// Ensure this device is discoverable by others
			ensureDiscoverable();
			return true;
		}
		return false;
		/*
		 * } return super.onOptionsItemSelected(item);
		 */
	}

	private void ensureDiscoverable() {
		Log.d(TAG, "ensure discoverable");
		if (mBluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
			Intent discoverableIntent = new Intent(
					BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
			discoverableIntent.putExtra(
					BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
			startActivity(discoverableIntent);
		}
	}

	public void deckCLicked(View view) {
		shuffleCards();

	}

	public void shuffleCards() {

		Random random = new Random();
		int randomNumber;

		while (dealtCards.size() < 32) {
			randomNumber = random.nextInt(32);
			if (!dealtCards.contains(randomNumber))
				dealtCards.add(randomNumber);
		}

		for (int i = 0; i < dealtCards.size(); i++) {
			Log.d("DEALING CARDS", dealtCards.get(i).toString());
		}

		// Check that we're actually connected before trying anything
		if (mChatService.getState() != BluetoothChatService.STATE_CONNECTED) {
			Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT)
					.show();
			return;
		}

		String message = "";

		for (int i = 0; i < dealtCards.size(); i++) {
			message = message + dealtCards.get(i).toString() + "_";
		}

		mChatService.write(message.getBytes());
	}

	public static int getCardDrawable(String cardName) {
		switch (cardName) {
		case "gnd2":
			return R.drawable.gnd2;
		case "gnd3":
			return R.drawable.gnd3;
		case "gnd4":
			return R.drawable.gnd4;
		case "gnd7":
			return R.drawable.gnd7;
		case "gnd8":
			return R.drawable.gnd8;
		case "gnd9":
			return R.drawable.gnd9;
		case "gnd10":
			return R.drawable.gnd10;
		case "gnd11":
			return R.drawable.gnd11;

		case "ros2":
			return R.drawable.ros2;
		case "ros3":
			return R.drawable.ros3;
		case "ros4":
			return R.drawable.ros4;
		case "ros7":
			return R.drawable.ros7;
		case "ros8":
			return R.drawable.ros8;
		case "ros9":
			return R.drawable.ros9;
		case "ros10":
			return R.drawable.ros10;
		case "ros11":
			return R.drawable.ros11;

		case "tob2":
			return R.drawable.tob2;
		case "tob3":
			return R.drawable.tob3;
		case "tob4":
			return R.drawable.tob4;
		case "tob7":
			return R.drawable.tob7;
		case "tob8":
			return R.drawable.tob8;
		case "tob9":
			return R.drawable.tob9;
		case "tob10":
			return R.drawable.tob10;
		case "tob11":
			return R.drawable.tob11;

		case "vrd2":
			return R.drawable.vrd2;
		case "vrd3":
			return R.drawable.vrd3;
		case "vrd4":
			return R.drawable.vrd4;
		case "vrd7":
			return R.drawable.vrd7;
		case "vrd8":
			return R.drawable.vrd8;
		case "vrd9":
			return R.drawable.vrd9;
		case "vrd10":
			return R.drawable.vrd10;
		case "vrd11":
			return R.drawable.vrd11;

		default:
			return -1;
		}
	}

	/*
	 * public void onToggleClicked(View view) { // Is the toggle on? boolean on
	 * = ((ToggleButton) view).isChecked();
	 * 
	 * if (on) { if (!mBluetoothAdapter.isEnabled()) { Intent enableBtIntent =
	 * new Intent( BluetoothAdapter.ACTION_REQUEST_ENABLE);
	 * startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
	 * Toast.makeText(getApplicationContext(), "Bluetooth turned on",
	 * Toast.LENGTH_SHORT).show(); } } else {
	 * 
	 * mBluetoothAdapter.disable(); Toast.makeText(getApplicationContext(),
	 * "Bluetooth turned off", Toast.LENGTH_SHORT).show(); } }
	 * 
	 * public void queryClicked(View view) { Intent serverIntent;
	 * 
	 * serverIntent = new Intent(this, DeviceListActivity.class);
	 * startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_SECURE); }
	 */

	public void onActivityResult(int requestCode, int resultCode, Intent data) {

		super.onActivityResult(requestCode, resultCode, data);

		switch (requestCode) {
		case REQUEST_CONNECT_DEVICE_SECURE:
			// When DeviceListActivity returns with a device to connect
			if (resultCode == Activity.RESULT_OK) {

				connectDevice(data, true);
			}
			break;

		}
		/*
		 * case REQUEST_CONNECT_DEVICE_INSECURE: // When DeviceListActivity
		 * returns with a device to connect if (resultCode ==
		 * Activity.RESULT_OK) { connectDevice(data, false); } break; case
		 * REQUEST_ENABLE_BT: // When the request to enable Bluetooth returns if
		 * (resultCode == Activity.RESULT_OK) { // Bluetooth is now enabled, so
		 * set up a chat session setupChat(); } else { // User did not enable
		 * Bluetooth or an error occurred Log.d(TAG, "BT not enabled");
		 * Toast.makeText(this, R.string.bt_not_enabled_leaving,
		 * Toast.LENGTH_SHORT).show(); finish(); }
		 */
	}

	private void connectDevice(Intent data, boolean secure) {
		// Get the device MAC address
		String address = data.getExtras().getString(
				DeviceListActivity.EXTRA_DEVICE_ADDRESS);
		// Get the BluetoothDevice object
		Log.d("connectDevice", address);
		BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
		// Attempt to connect to the device
		mChatService = new BluetoothChatService(this, mHandler);
		mChatService.connect(device, secure);
	}

	// The Handler that gets information back from the BluetoothChatService
	private final Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MESSAGE_STATE_CHANGE:
				Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
				switch (msg.arg1) {
				case BluetoothChatService.STATE_CONNECTED:
					setStatus(getString(R.string.title_connected_to));
					LinearLayout lLayout = (LinearLayout) findViewById(R.id.linearLayout1);
					lLayout.setVisibility(View.VISIBLE);
					break;
				case BluetoothChatService.STATE_CONNECTING:
					setStatus(R.string.title_connecting);
					break;
				case BluetoothChatService.STATE_LISTEN:
				case BluetoothChatService.STATE_NONE:
					setStatus(R.string.title_not_connected);
					break;
				}
				break;

			case MESSAGE_WRITE:

				if (gameStarted == false) {

					Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
					ImageButton imgBtn1 = (ImageButton) findViewById(R.id.card1);
					ImageButton imgBtn2 = (ImageButton) findViewById(R.id.card2);
					ImageButton imgBtn3 = (ImageButton) findViewById(R.id.card3);
					ImageButton imgBtn4 = (ImageButton) findViewById(R.id.card4);

					card1 = dealtCards.get(0);
					card2 = dealtCards.get(1);
					card3 = dealtCards.get(2);
					card4 = dealtCards.get(3);

					imgBtn1.setImageResource(MainActivity
							.getCardDrawable(cardsArrayList.get(dealtCards
									.get(0))));
					imgBtn2.setImageResource(MainActivity
							.getCardDrawable(cardsArrayList.get(dealtCards
									.get(1))));
					imgBtn3.setImageResource(MainActivity
							.getCardDrawable(cardsArrayList.get(dealtCards
									.get(2))));
					imgBtn4.setImageResource(MainActivity
							.getCardDrawable(cardsArrayList.get(dealtCards
									.get(3))));

					gameStarted = true;

					
				} /*else {

					ImageButton card1ImgBtn = (ImageButton) findViewById(R.id.card1);
					ImageButton card2ImgBtn = (ImageButton) findViewById(R.id.card2);
					ImageButton card3ImgBtn = (ImageButton) findViewById(R.id.card3);
					ImageButton card4ImgBtn = (ImageButton) findViewById(R.id.card4);

					card1ImgBtn.setEnabled(false);
					card2ImgBtn.setEnabled(false);
					card3ImgBtn.setEnabled(false);
					card4ImgBtn.setEnabled(false);

					Log.i(TAG, "Message_write else");
					
					 * String message = "";
					 * 
					 * message
					 * 
					 * mChatService.write(message.getBytes());
					 
					break;
				}*/
				break;
			case MESSAGE_READ:

				byte[] readBuf = (byte[]) msg.obj;
				// construct a string from the valid bytes in the buffer
				String readMessage = new String(readBuf, 0, msg.arg1);

				Log.i(TAG, "Mesajul: " + readMessage);

				//takeCards
				if (readMessage.contains("@")) {// pun cartile din mana in array-ul cu cartile luate de mine
					Log.i(TAG, "Mesajul: " + readMessage.substring(0, 8));
					if (readMessage.substring(0, 8).equals("yourHand")) {
						String[] handCards = (readMessage.substring(readMessage.indexOf("@") + 1)).split("@");
						for (int i = 0; i < handCards.length; i++)
							Log.i(TAG, "Hand Cards: " + handCards[i]);
						
						for (int i = 0; i < handCards.length; i++)
							takenCards.add(Integer.valueOf(handCards[i]));

						for (int i = 0; i < takenCards.size(); i++)
							Log.i(TAG, "Eu am cartile: " + takenCards.get(i));
						firstHand = true;
						
						ImageButton imageBtn1 = (ImageButton) findViewById(R.id.card1);
						ImageButton imageBtn2 = (ImageButton) findViewById(R.id.card2);
						ImageButton imageBtn3 = (ImageButton) findViewById(R.id.card3);
						ImageButton imageBtn4 = (ImageButton) findViewById(R.id.card4);
						
						imageBtn1.setEnabled(true);
						imageBtn2.setEnabled(true);
						imageBtn3.setEnabled(true);
						imageBtn4.setEnabled(true);
						
						ImageButton myCard = (ImageButton) findViewById(R.id.myCardImgBtn);
						myCard.setVisibility(View.INVISIBLE);
						ImageButton opponentCard = (ImageButton) findViewById(R.id.opponentCardImgBtn);
						opponentCard.setVisibility(View.INVISIBLE);
					}
					else if (readMessage.substring(0, 9).equals("hideCards")) {
						ImageButton myCard = (ImageButton) findViewById(R.id.myCardImgBtn);
						myCard.setVisibility(View.INVISIBLE);
						ImageButton opponentCard = (ImageButton) findViewById(R.id.opponentCardImgBtn);
						opponentCard.setVisibility(View.INVISIBLE);
					}
				} else

				if (gameStarted == false) {
					Log.i(TAG, "Cartile: " + readMessage);

					obtainDealtCards(readMessage);

					ImageButton imageBtn1 = (ImageButton) findViewById(R.id.card1);
					ImageButton imageBtn2 = (ImageButton) findViewById(R.id.card2);
					ImageButton imageBtn3 = (ImageButton) findViewById(R.id.card3);
					ImageButton imageBtn4 = (ImageButton) findViewById(R.id.card4);

					card1 = dealtCards.get(5);
					card2 = dealtCards.get(6);
					card3 = dealtCards.get(7);
					card4 = dealtCards.get(8);

					imageBtn1.setImageResource(MainActivity
							.getCardDrawable(cardsArrayList.get(dealtCards
									.get(5))));
					imageBtn2.setImageResource(MainActivity
							.getCardDrawable(cardsArrayList.get(dealtCards
									.get(6))));
					imageBtn3.setImageResource(MainActivity
							.getCardDrawable(cardsArrayList.get(dealtCards
									.get(7))));
					imageBtn4.setImageResource(MainActivity
							.getCardDrawable(cardsArrayList.get(dealtCards
									.get(8))));

					gameStarted = true;

					break;
				} else if (firstCardSW == true) {

					ImageButton card1ImgBtn = (ImageButton) findViewById(R.id.card1);
					ImageButton card2ImgBtn = (ImageButton) findViewById(R.id.card2);
					ImageButton card3ImgBtn = (ImageButton) findViewById(R.id.card3);
					ImageButton card4ImgBtn = (ImageButton) findViewById(R.id.card4);

					card1ImgBtn.setEnabled(true);
					card2ImgBtn.setEnabled(true);
					card3ImgBtn.setEnabled(true);
					card4ImgBtn.setEnabled(true);

					Log.i(TAG, "MESSAGE_READ else");

					ImageButton imageBtn7 = (ImageButton) findViewById(R.id.myCardImgBtn);
					imageBtn7.setVisibility(View.VISIBLE);
					Log.i(TAG, "Cartea primita 1: " + readMessage);

					firstCard = Integer.valueOf(readMessage);

					imageBtn7.setImageResource(MainActivity
							.getCardDrawable(cardsArrayList.get(Integer
									.valueOf(readMessage))));

					firstCardSW = false;

					break;
				} else {// firstCard == false

					ImageButton card1ImgBtn = (ImageButton) findViewById(R.id.card1);
					ImageButton card2ImgBtn = (ImageButton) findViewById(R.id.card2);
					ImageButton card3ImgBtn = (ImageButton) findViewById(R.id.card3);
					ImageButton card4ImgBtn = (ImageButton) findViewById(R.id.card4);

					card1ImgBtn.setEnabled(true);
					card2ImgBtn.setEnabled(true);
					card3ImgBtn.setEnabled(true);
					card4ImgBtn.setEnabled(true);

					Log.i(TAG, "MESSAGE_READ else");
					ImageButton secondCardImgBtn = (ImageButton) findViewById(R.id.opponentCardImgBtn);
					secondCardImgBtn.setVisibility(View.VISIBLE);
					Log.i(TAG, "Cartea primita 2: " + readMessage);

					secondCard = Integer.valueOf(readMessage);

					secondCardImgBtn.setImageResource(MainActivity
							.getCardDrawable(cardsArrayList.get(Integer
									.valueOf(readMessage))));

					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
					firstCardSW = true;

					compare(firstCard, secondCard);

					break;
				}
			}
		}

		private void obtainDealtCards(String cards) {

			String[] cardsArray = cards.split("_");
			for (int i = 0; i < cardsArray.length; i++) {
				Log.i(TAG, "Cartile: " + cardsArray[i]);
				dealtCards.add(new Integer(cardsArray[i]));

			}
		}
	};

	private void compare(int firstCard, int secondCard) {

		int x = Integer.valueOf(cardsArrayList.get(firstCard).substring(3));
		int y = Integer.valueOf(cardsArrayList.get(secondCard).substring(3));
		Log.i(TAG, "x si y: " + x + " " + y);

		currentHandCards.add(x);
		currentHandCards.add(y);
		
		if (x != y)
			if (x != 7 && y != 7) {
				//takeCards
				
				takenCards.addAll(currentHandCards);
				for (int i = 0; i < takenCards.size(); i++)
					Log.i(TAG, "Eu am cartiile: " + takenCards.get(i));
				firstHand = true;
				currentHandCards.clear();
				
				ImageButton card1ImgBtn = (ImageButton) findViewById(R.id.card1);
				ImageButton card2ImgBtn = (ImageButton) findViewById(R.id.card2);
				ImageButton card3ImgBtn = (ImageButton) findViewById(R.id.card3);
				ImageButton card4ImgBtn = (ImageButton) findViewById(R.id.card4);

				card1ImgBtn.setEnabled(true);
				card2ImgBtn.setEnabled(true);
				card3ImgBtn.setEnabled(true);
				card4ImgBtn.setEnabled(true);
				
				ImageButton myCard = (ImageButton) findViewById(R.id.myCardImgBtn);
				myCard.setVisibility(View.INVISIBLE);
				ImageButton opponentCard = (ImageButton) findViewById(R.id.opponentCardImgBtn);
				opponentCard.setVisibility(View.INVISIBLE);
				
				String message = "hideCards@";
				mChatService.write(message.getBytes());
				
				
			} else if (x == 7) {
				if (firstHand == true) {
					// cazul in care nu-i prima mana
					// si cel care o dat al doilea a luat cu cartea din prima mana

					//takeCards
					takenCards.addAll(currentHandCards);
					for (int i = 0; i < takenCards.size(); i++)
						Log.i(TAG, "Eu am cartiile: " + takenCards.get(i));
					currentHandCards.clear();
					firstHand = true;
					
					ImageButton card1ImgBtn = (ImageButton) findViewById(R.id.card1);
					ImageButton card2ImgBtn = (ImageButton) findViewById(R.id.card2);
					ImageButton card3ImgBtn = (ImageButton) findViewById(R.id.card3);
					ImageButton card4ImgBtn = (ImageButton) findViewById(R.id.card4);

					card1ImgBtn.setEnabled(true);
					card2ImgBtn.setEnabled(true);
					card3ImgBtn.setEnabled(true);
					card4ImgBtn.setEnabled(true);
					
					ImageButton myCard = (ImageButton) findViewById(R.id.myCardImgBtn);
					myCard.setVisibility(View.INVISIBLE);
					ImageButton opponentCard = (ImageButton) findViewById(R.id.opponentCardImgBtn);
					opponentCard.setVisibility(View.INVISIBLE);
					
					String message = "hideCards@";
					mChatService.write(message.getBytes());
				} else {
					if(y == firstCardOfHand)
					{
						ImageButton card1ImgBtn = (ImageButton) findViewById(R.id.card1);
						ImageButton card2ImgBtn = (ImageButton) findViewById(R.id.card2);
						ImageButton card3ImgBtn = (ImageButton) findViewById(R.id.card3);
						ImageButton card4ImgBtn = (ImageButton) findViewById(R.id.card4);

						card1ImgBtn.setEnabled(true);
						card2ImgBtn.setEnabled(true);
						card3ImgBtn.setEnabled(true);
						card4ImgBtn.setEnabled(true);

						ImageButton flagButton = (ImageButton) findViewById(R.id.flagImgBtn);
						flagButton.setEnabled(true);
						flagButton.setImageResource(R.drawable.whiteflag);
						firstHand = false;
					}
					else {
						//takeCards
						
						takenCards.addAll(currentHandCards);
						for (int i = 0; i < takenCards.size(); i++)
							Log.i(TAG, "Eu am cartiile: " + takenCards.get(i));
						currentHandCards.clear();
						firstHand = true;
						
						ImageButton card1ImgBtn = (ImageButton) findViewById(R.id.card1);
						ImageButton card2ImgBtn = (ImageButton) findViewById(R.id.card2);
						ImageButton card3ImgBtn = (ImageButton) findViewById(R.id.card3);
						ImageButton card4ImgBtn = (ImageButton) findViewById(R.id.card4);

						card1ImgBtn.setEnabled(true);
						card2ImgBtn.setEnabled(true);
						card3ImgBtn.setEnabled(true);
						card4ImgBtn.setEnabled(true);
						
						ImageButton myCard = (ImageButton) findViewById(R.id.myCardImgBtn);
						myCard.setVisibility(View.INVISIBLE);
						ImageButton opponentCard = (ImageButton) findViewById(R.id.opponentCardImgBtn);
						opponentCard.setVisibility(View.INVISIBLE);
						
						String message = "hideCards@";
						mChatService.write(message.getBytes());
					}
				}
			} else {// y == 7

				// trimite cartile la adversar

				ImageButton card1ImgBtn = (ImageButton) findViewById(R.id.card1);
				ImageButton card2ImgBtn = (ImageButton) findViewById(R.id.card2);
				ImageButton card3ImgBtn = (ImageButton) findViewById(R.id.card3);
				ImageButton card4ImgBtn = (ImageButton) findViewById(R.id.card4);

				card1ImgBtn.setEnabled(true);
				card2ImgBtn.setEnabled(true);
				card3ImgBtn.setEnabled(true);
				card4ImgBtn.setEnabled(true);

				ImageButton flagButton = (ImageButton) findViewById(R.id.flagImgBtn);
				flagButton.setEnabled(true);
				flagButton.setImageResource(R.drawable.whiteflag);
				firstHand = false;

				for (int i = 0; i < takenCards.size(); i++)
					Log.i(TAG, "Eu am cartile: " + takenCards.get(i));

			}
		else {
			Log.i(TAG, "Cartile sunt egale");
			ImageButton card1ImgBtn = (ImageButton) findViewById(R.id.card1);
			ImageButton card2ImgBtn = (ImageButton) findViewById(R.id.card2);
			ImageButton card3ImgBtn = (ImageButton) findViewById(R.id.card3);
			ImageButton card4ImgBtn = (ImageButton) findViewById(R.id.card4);

			card1ImgBtn.setEnabled(true);
			card2ImgBtn.setEnabled(true);
			card3ImgBtn.setEnabled(true);
			card4ImgBtn.setEnabled(true);

			ImageButton flagButton = (ImageButton) findViewById(R.id.flagImgBtn);
			flagButton.setEnabled(true);
			flagButton.setImageResource(R.drawable.whiteflag);

			for (int i = 0; i < takenCards.size(); i++)
				Log.i(TAG, "Eu am cartile: " + takenCards.get(i));
		}

	}

	public void flagBtnClicked(View view) {		
		String message = "yourHand";
		
		for(int i=0; i < currentHandCards.size(); i++)
			message = message + "@" + currentHandCards.get(i);
		
		currentHandCards.clear();
		mChatService.write(message.getBytes());
		
		ImageButton flagButton = (ImageButton) findViewById(R.id.flagImgBtn);
		flagButton.setVisibility(View.INVISIBLE);
		
		ImageButton myCard = (ImageButton) findViewById(R.id.myCardImgBtn);
		myCard.setVisibility(View.INVISIBLE);
		ImageButton opponentCard = (ImageButton) findViewById(R.id.opponentCardImgBtn);
		opponentCard.setVisibility(View.INVISIBLE);
		
	}

	public void imgBtn1Clicked(View view) {
		if (firstCardSW == true) {
			if (firstHand == true)
				firstCardOfHand = card1;
			firstCard = card1;
			ImageButton imgBtn7 = (ImageButton) findViewById(R.id.myCardImgBtn);
			imgBtn7.setVisibility(View.VISIBLE);
			imgBtn7.setImageResource(MainActivity
					.getCardDrawable(cardsArrayList.get(card1)));

			String message = Integer.toString(card1);
			Log.i(TAG, "Cartea 1 din onCLick: " + message);
			mChatService.write(message.getBytes());
			firstCardSW = false;

			ImageButton card1 = (ImageButton) findViewById(R.id.card1);
			ImageButton card2 = (ImageButton) findViewById(R.id.card2);
			ImageButton card3 = (ImageButton) findViewById(R.id.card3);
			ImageButton card4 = (ImageButton) findViewById(R.id.card4);

			card1.setEnabled(false);
			card2.setEnabled(false);
			card3.setEnabled(false);
			card4.setEnabled(false);
		} else {

			secondCard = card1;
			ImageButton opponentCard = (ImageButton) findViewById(R.id.opponentCardImgBtn);
			opponentCard.setVisibility(View.VISIBLE);
			opponentCard.setImageResource(MainActivity
					.getCardDrawable(cardsArrayList.get(card1)));

			String message = Integer.toString(card1);
			Log.i(TAG, "Cartea 1 din onCLick: " + message);
			mChatService.write(message.getBytes());
			firstCardSW = true;

			ImageButton card1 = (ImageButton) findViewById(R.id.card1);
			ImageButton card2 = (ImageButton) findViewById(R.id.card2);
			ImageButton card3 = (ImageButton) findViewById(R.id.card3);
			ImageButton card4 = (ImageButton) findViewById(R.id.card4);

			card1.setEnabled(false);
			card2.setEnabled(false);
			card3.setEnabled(false);
			card4.setEnabled(false);
		}

	}

	public void imgBtn2Clicked(View view) {
		if (firstCardSW == true) {
			if (firstHand == true)
				firstCardOfHand = card2;
			firstCard = card2;
			ImageButton imgBtn7 = (ImageButton) findViewById(R.id.myCardImgBtn);
			imgBtn7.setVisibility(View.VISIBLE);
			imgBtn7.setImageResource(MainActivity
					.getCardDrawable(cardsArrayList.get(card2)));

			String message = Integer.toString(card2);
			Log.i(TAG, "Cartea 2 din onCLick: " + message);
			mChatService.write(message.getBytes());
			firstCardSW = false;

			ImageButton card1 = (ImageButton) findViewById(R.id.card1);
			ImageButton card2 = (ImageButton) findViewById(R.id.card2);
			ImageButton card3 = (ImageButton) findViewById(R.id.card3);
			ImageButton card4 = (ImageButton) findViewById(R.id.card4);

			card1.setEnabled(false);
			card2.setEnabled(false);
			card3.setEnabled(false);
			card4.setEnabled(false);
		} else {

			secondCard = card2;
			ImageButton opponentCard = (ImageButton) findViewById(R.id.opponentCardImgBtn);
			opponentCard.setVisibility(View.VISIBLE);
			opponentCard.setImageResource(MainActivity
					.getCardDrawable(cardsArrayList.get(card2)));

			String message = Integer.toString(card2);
			Log.i(TAG, "Cartea 2 din onCLick: " + message);
			mChatService.write(message.getBytes());
			firstCardSW = true;

			ImageButton card1 = (ImageButton) findViewById(R.id.card1);
			ImageButton card2 = (ImageButton) findViewById(R.id.card2);
			ImageButton card3 = (ImageButton) findViewById(R.id.card3);
			ImageButton card4 = (ImageButton) findViewById(R.id.card4);

			card1.setEnabled(false);
			card2.setEnabled(false);
			card3.setEnabled(false);
			card4.setEnabled(false);
		}
	}

	public void imgBtn3Clicked(View view) {
		if (firstCardSW == true) {
			if (firstHand == true)
				firstCardOfHand = card3;
			firstCard = card3;
			ImageButton imgBtn7 = (ImageButton) findViewById(R.id.myCardImgBtn);
			imgBtn7.setVisibility(View.VISIBLE);
			imgBtn7.setImageResource(MainActivity
					.getCardDrawable(cardsArrayList.get(card3)));

			String message = Integer.toString(card3);
			Log.i(TAG, "Cartea 3 din onCLick: " + message);
			mChatService.write(message.getBytes());
			firstCardSW = false;

			ImageButton card1 = (ImageButton) findViewById(R.id.card1);
			ImageButton card2 = (ImageButton) findViewById(R.id.card2);
			ImageButton card3 = (ImageButton) findViewById(R.id.card3);
			ImageButton card4 = (ImageButton) findViewById(R.id.card4);

			card1.setEnabled(false);
			card2.setEnabled(false);
			card3.setEnabled(false);
			card4.setEnabled(false);
		} else {

			secondCard = card3;
			ImageButton opponentCard = (ImageButton) findViewById(R.id.opponentCardImgBtn);
			opponentCard.setVisibility(View.VISIBLE);
			opponentCard.setImageResource(MainActivity
					.getCardDrawable(cardsArrayList.get(card3)));

			String message = Integer.toString(card3);
			Log.i(TAG, "Cartea 3 din onCLick: " + message);
			mChatService.write(message.getBytes());
			firstCardSW = true;

			ImageButton card1 = (ImageButton) findViewById(R.id.card1);
			ImageButton card2 = (ImageButton) findViewById(R.id.card2);
			ImageButton card3 = (ImageButton) findViewById(R.id.card3);
			ImageButton card4 = (ImageButton) findViewById(R.id.card4);

			card1.setEnabled(false);
			card2.setEnabled(false);
			card3.setEnabled(false);
			card4.setEnabled(false);
		}
	}

	public void imgBtn4Clicked(View view) {
		if (firstCardSW == true) {
			if (firstHand == true)
				firstCardOfHand = card4;
			firstCard = card4;
			ImageButton imgBtn7 = (ImageButton) findViewById(R.id.myCardImgBtn);
			imgBtn7.setVisibility(View.VISIBLE);
			imgBtn7.setImageResource(MainActivity
					.getCardDrawable(cardsArrayList.get(card4)));

			String message = Integer.toString(card4);
			Log.i(TAG, "Cartea 4 din onCLick: " + message);
			mChatService.write(message.getBytes());
			firstCardSW = false;

			ImageButton card1 = (ImageButton) findViewById(R.id.card1);
			ImageButton card2 = (ImageButton) findViewById(R.id.card2);
			ImageButton card3 = (ImageButton) findViewById(R.id.card3);
			ImageButton card4 = (ImageButton) findViewById(R.id.card4);

			card1.setEnabled(false);
			card2.setEnabled(false);
			card3.setEnabled(false);
			card4.setEnabled(false);
		} else {

			secondCard = card4;
			ImageButton opponentCard = (ImageButton) findViewById(R.id.opponentCardImgBtn);
			opponentCard.setVisibility(View.VISIBLE);
			opponentCard.setImageResource(MainActivity
					.getCardDrawable(cardsArrayList.get(card4)));

			String message = Integer.toString(card4);
			Log.i(TAG, "Cartea 4 din onCLick: " + message);
			mChatService.write(message.getBytes());
			firstCardSW = true;

			ImageButton card1 = (ImageButton) findViewById(R.id.card1);
			ImageButton card2 = (ImageButton) findViewById(R.id.card2);
			ImageButton card3 = (ImageButton) findViewById(R.id.card3);
			ImageButton card4 = (ImageButton) findViewById(R.id.card4);

			card1.setEnabled(false);
			card2.setEnabled(false);
			card3.setEnabled(false);
			card4.setEnabled(false);

		}
	}

	private final void setStatus(int resId) {
		final ActionBar actionBar = getActionBar();
		actionBar.setSubtitle(resId);
	}

	private final void setStatus(CharSequence subTitle) {
		final ActionBar actionBar = getActionBar();
		actionBar.setSubtitle(subTitle);
	}
}
