package com.sebi.cardgame;

import java.util.ArrayList;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

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
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

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
	private int usedCardsCounter = 6;

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
	private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
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
		Log.i(TAG, "++ ON START ++");

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
		case R.id.insecure_connect_scan:
			// Launch the DeviceListActivity to see devices and do scan
			serverIntent = new Intent(this, DeviceListActivity.class);
			startActivityForResult(serverIntent,
					REQUEST_CONNECT_DEVICE_INSECURE);
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

	private void shuffleCards() {

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

		case REQUEST_CONNECT_DEVICE_INSECURE:
			// When DeviceListActivity returns with a device to connect
			if (resultCode == Activity.RESULT_OK) {
				connectDevice(data, false);
			}
			break;

		}
		/*
		 * case REQUEST_ENABLE_BT: // When the request to enable Bluetooth
		 * returns if (resultCode == Activity.RESULT_OK) { // Bluetooth is now
		 * enabled, so set up a chat session setupChat(); } else { // User did
		 * not enable Bluetooth or an error occurred Log.d(TAG,
		 * "BT not enabled"); Toast.makeText(this,
		 * R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
		 * finish(); }
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
				updateScore();
				if (gameStarted == false) {
					ImageButton deckImgBtn = (ImageButton) findViewById(R.id.deckImgBtn);
					deckImgBtn.setEnabled(false);
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
				}
				break;
			case MESSAGE_READ:
				updateScore();
				byte[] readBuf = (byte[]) msg.obj;
				// construct a string from the valid bytes in the buffer
				String readMessage = new String(readBuf, 0, msg.arg1);

				Log.i(TAG, "Mesajul: " + readMessage);

				// takeCards prin flag
				if (readMessage.contains("@")) {// pun cartile din mana in
												// array-ul cu cartile luate de
												// mine
					Log.i(TAG, "Mesajul cu @: " + readMessage);
					if (readMessage.substring(0, 8).equals("yourHand")) {
						if (readMessage.contains("takeCards")) {
							String[] handCards = (readMessage.substring(
									(readMessage.indexOf("@") + 1),
									readMessage.indexOf("takeCards")))
									.split("@");
							for (int i = 0; i < handCards.length; i++)
								Log.i(TAG, "Hand Cards: " + handCards[i]);

							for (int i = 0; i < handCards.length; i++)
								takenCards.add(Integer.valueOf(handCards[i]));

							for (int i = 0; i < takenCards.size(); i++)
								Log.i(TAG,
										"Eu am cartile: " + takenCards.get(i));
							firstHand = true;

							decarteazaUnNumarDeCarti(readMessage.substring(
									readMessage.indexOf("takeCards") + 10,
									readMessage.lastIndexOf("@")),
									readMessage.substring(readMessage
											.lastIndexOf("@") + 1));
						} else

						{
							String[] handCards = (readMessage
									.substring((readMessage.indexOf("@") + 1)))
									.split("@");
							for (int i = 0; i < handCards.length; i++)
								Log.i(TAG, "Hand Cards: " + handCards[i]);

							for (int i = 0; i < handCards.length; i++)
								takenCards.add(Integer.valueOf(handCards[i]));

							for (int i = 0; i < takenCards.size(); i++)
								Log.i(TAG,
										"Eu am cartile: " + takenCards.get(i));
							firstHand = true;

						}

						setCardButtonsEnabled(true);

						ImageButton myCard = (ImageButton) findViewById(R.id.myCardImgBtn);
						myCard.setVisibility(View.INVISIBLE);
						ImageButton opponentCard = (ImageButton) findViewById(R.id.opponentCardImgBtn);
						opponentCard.setVisibility(View.INVISIBLE);

						decarteazaCarti();

					} else if (readMessage.substring(0, 9).equals("hideCards")) {
						decarteazaCarti();
						ImageButton myCard = (ImageButton) findViewById(R.id.myCardImgBtn);
						myCard.setVisibility(View.INVISIBLE);
						ImageButton opponentCard = (ImageButton) findViewById(R.id.opponentCardImgBtn);
						opponentCard.setVisibility(View.INVISIBLE);
					} else if (readMessage.substring(0, 9).equals("takeCards")) {
						decarteazaUnNumarDeCarti(readMessage.substring(10,
								readMessage.lastIndexOf("@")),
								readMessage.substring(readMessage
										.lastIndexOf("@") + 1));
					}
				} else

				if (gameStarted == false) {
					Log.i(TAG, "Cartile: " + readMessage);
					ImageButton deckImgBtn = (ImageButton) findViewById(R.id.deckImgBtn);
					deckImgBtn.setEnabled(false);
					obtainDealtCards(readMessage);

					ImageButton imageBtn1 = (ImageButton) findViewById(R.id.card1);
					ImageButton imageBtn2 = (ImageButton) findViewById(R.id.card2);
					ImageButton imageBtn3 = (ImageButton) findViewById(R.id.card3);
					ImageButton imageBtn4 = (ImageButton) findViewById(R.id.card4);

					card1 = dealtCards.get(4);
					card2 = dealtCards.get(5);
					card3 = dealtCards.get(6);
					card4 = dealtCards.get(7);

					imageBtn1.setImageResource(MainActivity
							.getCardDrawable(cardsArrayList.get(dealtCards
									.get(4))));
					imageBtn2.setImageResource(MainActivity
							.getCardDrawable(cardsArrayList.get(dealtCards
									.get(5))));
					imageBtn3.setImageResource(MainActivity
							.getCardDrawable(cardsArrayList.get(dealtCards
									.get(6))));
					imageBtn4.setImageResource(MainActivity
							.getCardDrawable(cardsArrayList.get(dealtCards
									.get(7))));

					setCardButtonsEnabled(false);
					
					gameStarted = true;
					usedCardsCounter++;

					break;
				} else if (firstCardSW == true) {

					setCardButtonsEnabled(true);

					Log.i(TAG, "MESSAGE_READ else");

					ImageButton imageBtn7 = (ImageButton) findViewById(R.id.myCardImgBtn);
					imageBtn7.setVisibility(View.VISIBLE);
					imageBtn7.bringToFront();
					imageBtn7.requestLayout();
					imageBtn7.invalidate();
					Log.i(TAG, "Cartea primita 1: " + readMessage);

					firstCard = Integer.valueOf(readMessage);

					imageBtn7.setImageResource(MainActivity
							.getCardDrawable(cardsArrayList.get(Integer
									.valueOf(readMessage))));

					firstCardSW = false;

					break;
				} else {// firstCard == false

					setCardButtonsEnabled(true);

					Log.i(TAG, "MESSAGE_READ else");
					ImageButton secondCardImgBtn = (ImageButton) findViewById(R.id.opponentCardImgBtn);
					secondCardImgBtn.setVisibility(View.VISIBLE);
					secondCardImgBtn.bringToFront();
					secondCardImgBtn.requestLayout();
					secondCardImgBtn.invalidate();
					Log.i(TAG, "Cartea primita 2: " + readMessage);

					secondCard = Integer.valueOf(readMessage);

					secondCardImgBtn.setImageResource(MainActivity
							.getCardDrawable(cardsArrayList.get(Integer
									.valueOf(readMessage))));

					firstCardSW = true;

					new Timer().schedule(new TimerTask() {

						@Override
						public void run() {
							runOnUiThread(new Runnable() {
								@Override
								public void run() {

									// stuff that updates ui
									compare(firstCard, secondCard);
								}
							});
							// this code will be executed after 0.5 seconds
						}
					}, 500);

					// compare(firstCard, secondCard);

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

	private int updateScore() {
		TextView scoreTextView = (TextView) findViewById(R.id.textView1);
		int x = 0;
		for (int i = 0; i < takenCards.size(); i++)
			if (takenCards.get(i) > 9)
				x++;
		scoreTextView.setText("My score: " + x);
		return x;
	}

	private void compare(int firstCard, int secondCard) {
		int x = Integer.valueOf(cardsArrayList.get(firstCard).substring(3));
		int y = Integer.valueOf(cardsArrayList.get(secondCard).substring(3));
		Log.i(TAG, "x si y: " + x + " " + y);

		currentHandCards.add(x);
		currentHandCards.add(y);

		if (x != y) {
			Log.i(TAG, "Cartile sunt diferite");
			if (x != 7 && y != 7) {
				// takeCards
				Log.i(TAG, "Si ambele sunt diferite de 7");
				takenCards.addAll(currentHandCards);
				for (int i = 0; i < takenCards.size(); i++)
					Log.i(TAG, "Eu am cartile: " + takenCards.get(i));
				firstHand = true;
				currentHandCards.clear();

				setCardButtonsEnabled(true);

				ImageButton myCard = (ImageButton) findViewById(R.id.myCardImgBtn);
				myCard.setVisibility(View.INVISIBLE);
				ImageButton opponentCard = (ImageButton) findViewById(R.id.opponentCardImgBtn);
				opponentCard.setVisibility(View.INVISIBLE);
				;

				String message = "hideCards@";
				mChatService.write(message.getBytes());

				decarteazaCarti();

			} else if (x == 7) {
				if (firstHand == true) {
					Log.i(TAG, "x ii 7 si ii prima mana");
					// cazul in care nu-i prima mana
					// si cel care o dat al doilea a luat cu cartea din prima
					// mana

					// takeCards
					takenCards.addAll(currentHandCards);
					for (int i = 0; i < takenCards.size(); i++)
						Log.i(TAG, "Eu am cartiile: " + takenCards.get(i));
					currentHandCards.clear();
					firstHand = true;

					setCardButtonsEnabled(true);

					ImageButton myCard = (ImageButton) findViewById(R.id.myCardImgBtn);
					myCard.setVisibility(View.INVISIBLE);
					ImageButton opponentCard = (ImageButton) findViewById(R.id.opponentCardImgBtn);
					opponentCard.setVisibility(View.INVISIBLE);

					String message = "hideCards@";
					mChatService.write(message.getBytes());

					decarteazaCarti();
				} else {
					if (y == firstCardOfHand) {
						Log.i(TAG,
								"x ii 7 si nu ii prima mana, iar y = prima carte");
						Log.i(TAG, "Prima carte: " + firstCardOfHand);

						if (checkMatchingCards() == true) {
							setCardButtonsEnabled(true);

							firstHand = false;
							
							disableUnmatchingCards();

							ImageButton flagButton = (ImageButton) findViewById(R.id.flagImgBtn);
							flagButton.setVisibility(View.VISIBLE);
							flagButton.setImageResource(R.drawable.whiteflag);
						} else {
							String message = "yourHand";

							for (int i = 0; i < currentHandCards.size(); i++)
								message = message + "@"
										+ currentHandCards.get(i);

							currentHandCards.clear();
							mChatService.write(message.getBytes());

							ImageButton flagButton = (ImageButton) findViewById(R.id.flagImgBtn);
							flagButton.setVisibility(View.INVISIBLE);

							ImageButton myCard = (ImageButton) findViewById(R.id.myCardImgBtn);
							myCard.setVisibility(View.INVISIBLE);
							ImageButton opponentCard = (ImageButton) findViewById(R.id.opponentCardImgBtn);
							opponentCard.setVisibility(View.INVISIBLE);

							setCardButtonsEnabled(false);

							decarteazaCarti();
						}
					} else {
						// takeCards
						Log.i(TAG,
								"x ii 7 si nu ii prima mana, iar y != prima carte");
						Log.i(TAG, "Prima carte: " + firstCardOfHand);
						takenCards.addAll(currentHandCards);
						for (int i = 0; i < takenCards.size(); i++)
							Log.i(TAG, "Eu am cartiile: " + takenCards.get(i));
						currentHandCards.clear();
						firstHand = true;

						setCardButtonsEnabled(true);

						ImageButton myCard = (ImageButton) findViewById(R.id.myCardImgBtn);
						myCard.setVisibility(View.INVISIBLE);
						ImageButton opponentCard = (ImageButton) findViewById(R.id.opponentCardImgBtn);
						opponentCard.setVisibility(View.INVISIBLE);

						String message = "hideCards@";
						mChatService.write(message.getBytes());

						decarteazaCarti();
					}
				}
			} else {// y == 7
				Log.i(TAG, "y ii 7");
				// trimite cartile la adversar

				if (checkMatchingCards() == true) {
					setCardButtonsEnabled(true);

					firstHand = false;

					disableUnmatchingCards();

					ImageButton flagButton = (ImageButton) findViewById(R.id.flagImgBtn);
					flagButton.setVisibility(View.VISIBLE);
					flagButton.setImageResource(R.drawable.whiteflag);
				} else {
					String message = "yourHand";

					for (int i = 0; i < currentHandCards.size(); i++)
						message = message + "@" + currentHandCards.get(i);

					currentHandCards.clear();
					mChatService.write(message.getBytes());

					ImageButton flagButton = (ImageButton) findViewById(R.id.flagImgBtn);
					flagButton.setVisibility(View.INVISIBLE);

					ImageButton myCard = (ImageButton) findViewById(R.id.myCardImgBtn);
					myCard.setVisibility(View.INVISIBLE);
					ImageButton opponentCard = (ImageButton) findViewById(R.id.opponentCardImgBtn);
					opponentCard.setVisibility(View.INVISIBLE);

					setCardButtonsEnabled(false);

					decarteazaCarti();
				}

				for (int i = 0; i < takenCards.size(); i++)
					Log.i(TAG, "Eu am cartile: " + takenCards.get(i));

			}
		} else {
			Log.i(TAG, "Cartile sunt egale");

			if (checkMatchingCards() == true) {
				setCardButtonsEnabled(true);

				firstHand = false;

				disableUnmatchingCards();
				
				ImageButton flagButton = (ImageButton) findViewById(R.id.flagImgBtn);
				flagButton.setVisibility(View.VISIBLE);
				flagButton.setImageResource(R.drawable.whiteflag);
			} else {
				String message = "yourHand";

				for (int i = 0; i < currentHandCards.size(); i++)
					message = message + "@" + currentHandCards.get(i);

				currentHandCards.clear();
				mChatService.write(message.getBytes());

				ImageButton flagButton = (ImageButton) findViewById(R.id.flagImgBtn);
				flagButton.setVisibility(View.INVISIBLE);

				ImageButton myCard = (ImageButton) findViewById(R.id.myCardImgBtn);
				myCard.setVisibility(View.INVISIBLE);
				ImageButton opponentCard = (ImageButton) findViewById(R.id.opponentCardImgBtn);
				opponentCard.setVisibility(View.INVISIBLE);

				setCardButtonsEnabled(false);

				decarteazaCarti();
			}

			for (int i = 0; i < takenCards.size(); i++)
				Log.i(TAG, "Eu am cartile: " + takenCards.get(i));
		}

	}

	private void disableUnmatchingCards() {
		int c1 = 0, c2 = 0, c3 = 0, c4 = 0;
		ImageButton cardBtn1 = (ImageButton) findViewById(R.id.card1);
		ImageButton cardBtn2 = (ImageButton) findViewById(R.id.card2);
		ImageButton cardBtn3 = (ImageButton) findViewById(R.id.card3);
		ImageButton cardBtn4 = (ImageButton) findViewById(R.id.card4);

		if (card1 != -1)
			c1 = Integer.valueOf(cardsArrayList.get(card1).substring(3));
		if (card2 != -1)
			c2 = Integer.valueOf(cardsArrayList.get(card2).substring(3));
		if (card3 != -1)
			c3 = Integer.valueOf(cardsArrayList.get(card3).substring(3));
		if (card4 != -1)
			c4 = Integer.valueOf(cardsArrayList.get(card4).substring(3));

		if (c1 != 7 && c1 != firstCardOfHand) {
			cardBtn1.setEnabled(false);
			cardBtn1.setAlpha(0.5f);
			cardBtn1.requestLayout();
			cardBtn1.invalidate();
		}
		if (c2 != 7 && c2 != firstCardOfHand) {
			cardBtn2.setEnabled(false);
			cardBtn2.setAlpha(0.5f);
			cardBtn2.requestLayout();
			cardBtn2.invalidate();
		}
		if (c3 != 7 && c3 != firstCardOfHand) {
			cardBtn3.setEnabled(false);
			cardBtn3.setAlpha(0.5f);
			cardBtn3.requestLayout();
			cardBtn3.invalidate();
		}
		if (c4 != 7 && c4 != firstCardOfHand) {
			cardBtn4.setEnabled(false);
			cardBtn4.setAlpha(0.5f);
			cardBtn4.requestLayout();
			cardBtn4.invalidate();
		}
	}

	private boolean checkMatchingCards() {
		
		if(usedCardsCounter == 30 || usedCardsCounter == 31)
			if(card1 == -1 && card2 == -1 && card3 == -1 && card4 == -1)
			{
				Log.i(TAG, "End game!!!");
				ImageButton deckImgBtn = (ImageButton) findViewById(R.id.deckImgBtn);
				deckImgBtn.setEnabled(true);
				gameStarted = false;
				usedCardsCounter = 6;
				firstCardSW = true;
				myHand = false;
				firstHand = true;
				takenCards.clear();
				dealtCards.clear();
				ImageButton display = (ImageButton) findViewById(R.id.myCardImgBtn);
				if(updateScore() > 4)
					display.setImageResource(R.drawable.win);
				else
					if(updateScore() == 4)
						display.setImageResource(R.drawable.draw);
				shuffleCards();
				return false;
			}
			
		
		int c1 = 0, c2 = 0, c3 = 0, c4 = 0;
		if (card1 != -1)
			c1 = Integer.valueOf(cardsArrayList.get(card1).substring(3));
		if (card2 != -1)
			c2 = Integer.valueOf(cardsArrayList.get(card2).substring(3));
		if (card3 != -1)
			c3 = Integer.valueOf(cardsArrayList.get(card3).substring(3));
		if (card4 != -1)
			c4 = Integer.valueOf(cardsArrayList.get(card4).substring(3));
		Log.i(TAG, "First card of the hand: " + firstCardOfHand);
		Log.i(TAG, "Cards to check: " + c1 + " " + c2 + " " + c3 + " " + c4
				+ " ");
		if (c1 == 7 || c2 == 7 || c3 == 7 || c4 == 7 || c1 == firstCardOfHand
				|| c2 == firstCardOfHand || c3 == firstCardOfHand
				|| c4 == firstCardOfHand)
			return true;
		else
			return false;
	}

	public void flagBtnClicked(View view) {
		String message = "yourHand";

		for (int i = 0; i < currentHandCards.size(); i++)
			message = message + "@" + currentHandCards.get(i);

		currentHandCards.clear();
		mChatService.write(message.getBytes());

		ImageButton flagButton = (ImageButton) findViewById(R.id.flagImgBtn);
		flagButton.setVisibility(View.INVISIBLE);

		ImageButton myCard = (ImageButton) findViewById(R.id.myCardImgBtn);
		myCard.setVisibility(View.INVISIBLE);
		ImageButton opponentCard = (ImageButton) findViewById(R.id.opponentCardImgBtn);
		opponentCard.setVisibility(View.INVISIBLE);

		setCardButtonsEnabled(false);

		decarteazaCarti();
	}

	private void decarteazaCarti() {

		int numberOfCardsLeft = (31 - usedCardsCounter) / 2;
		int totalCardsTaken = 0;

		if(usedCardsCounter == 30 || usedCardsCounter == 31)
			if(card1 == -1 && card2 == -1 && card3 == -1 && card4 == -1)
			{
				Log.i(TAG, "End game!!!");
				ImageButton deckImgBtn = (ImageButton) findViewById(R.id.deckImgBtn);
				deckImgBtn.setEnabled(true);
				gameStarted = false;
				usedCardsCounter = 6;
				firstCardSW = true;
				myHand = false;
				firstHand = true;
				takenCards.clear();
				dealtCards.clear();
				ImageButton display = (ImageButton) findViewById(R.id.myCardImgBtn);
				if(updateScore() > 4)
					display.setImageResource(R.drawable.win);
				else
					if(updateScore() == 4)
						display.setImageResource(R.drawable.draw);
				shuffleCards();
			}
		
		if (numberOfCardsLeft > 0)
			if (card1 == -1) {

				usedCardsCounter = usedCardsCounter + 2;
				card1 = dealtCards.get(usedCardsCounter);
				Log.i(TAG, "Iau cartea nr " + usedCardsCounter + " : " + card1);
				/*
				 * Toast.makeText(this, "Iau cartea nr " + usedCardsCounter +
				 * " : " + card1, Toast.LENGTH_LONG).show();
				 */
				ImageButton cardImgBtn1 = (ImageButton) findViewById(R.id.card1);
				cardImgBtn1.setVisibility(View.VISIBLE);
				cardImgBtn1.setImageResource(MainActivity
						.getCardDrawable(cardsArrayList.get(card1)));
				numberOfCardsLeft--;
				totalCardsTaken++;
			}
		if (numberOfCardsLeft > 0)
			if (card2 == -1) {
				usedCardsCounter = usedCardsCounter + 2;
				card2 = dealtCards.get(usedCardsCounter);
				Log.i(TAG, "Iau cartea nr " + usedCardsCounter + " : " + card2);
				/*
				 * Toast.makeText(this, "Iau cartea nr " + usedCardsCounter +
				 * " : " + card2, Toast.LENGTH_LONG).show();
				 */
				ImageButton cardImgBtn2 = (ImageButton) findViewById(R.id.card2);
				cardImgBtn2.setVisibility(View.VISIBLE);
				cardImgBtn2.setImageResource(MainActivity
						.getCardDrawable(cardsArrayList.get(card2)));
				numberOfCardsLeft--;
				totalCardsTaken++;
			}
		if (numberOfCardsLeft > 0)
			if (card3 == -1) {
				usedCardsCounter = usedCardsCounter + 2;
				card3 = dealtCards.get(usedCardsCounter);
				Log.i(TAG, "Iau cartea nr " + usedCardsCounter + " : " + card3);
				/*
				 * Toast.makeText(this, "Iau cartea nr " + usedCardsCounter +
				 * " : " + card3, Toast.LENGTH_LONG).show();
				 */
				ImageButton cardImgBtn3 = (ImageButton) findViewById(R.id.card3);
				cardImgBtn3.setVisibility(View.VISIBLE);
				cardImgBtn3.setImageResource(MainActivity
						.getCardDrawable(cardsArrayList.get(card3)));
				numberOfCardsLeft--;
				totalCardsTaken++;
			}
		if (numberOfCardsLeft > 0)
			if (card4 == -1) {
				usedCardsCounter = usedCardsCounter + 2;
				card4 = dealtCards.get(usedCardsCounter);
				Log.i(TAG, "Iau cartea nr " + usedCardsCounter + " : " + card4);
				/*
				 * Toast.makeText(this, "Iau cartea nr " + usedCardsCounter +
				 * " : " + card4, Toast.LENGTH_LONG).show();
				 */
				ImageButton cardImgBtn4 = (ImageButton) findViewById(R.id.card4);
				cardImgBtn4.setVisibility(View.VISIBLE);
				cardImgBtn4.setImageResource(MainActivity
						.getCardDrawable(cardsArrayList.get(card4)));
				numberOfCardsLeft--;
				totalCardsTaken++;
			}

		/*
		 * String message = "takeCards@";
		 * 
		 * message = message + totalCardsTaken + "@" + usedCardsCounter;
		 * 
		 * Log.i(TAG, "Mesajul trimis din decarteazaCarti: " + message);
		 * mChatService.write(message.getBytes());
		 */

		// usedCardsCounter = usedCardsCounter + totalCardsTaken;

	}

	private void decarteazaUnNumarDeCarti(String substring, String counter) {
		int numberOfCardsToTake = Integer.valueOf(substring);
		usedCardsCounter = Integer.valueOf(counter);

		if (numberOfCardsToTake > 0)
			if (card1 == -1) {
				usedCardsCounter++;
				card1 = dealtCards.get(usedCardsCounter);
				Log.i(TAG, "Iau cartea nr " + usedCardsCounter + " : " + card1);
				Toast.makeText(this,
						"Iau cartea nr " + usedCardsCounter + " : " + card1,
						Toast.LENGTH_LONG).show();
				ImageButton cardImgBtn1 = (ImageButton) findViewById(R.id.card1);
				cardImgBtn1.setVisibility(View.VISIBLE);
				cardImgBtn1.setImageResource(MainActivity
						.getCardDrawable(cardsArrayList.get(card1)));
				numberOfCardsToTake--;
			}
		if (numberOfCardsToTake > 0)
			if (card2 == -1) {
				usedCardsCounter++;
				card2 = dealtCards.get(usedCardsCounter);
				Log.i(TAG, "Iau cartea nr " + usedCardsCounter + " : " + card2);
				Toast.makeText(this,
						"Iau cartea nr " + usedCardsCounter + " : " + card2,
						Toast.LENGTH_LONG).show();
				ImageButton cardImgBtn2 = (ImageButton) findViewById(R.id.card2);
				cardImgBtn2.setVisibility(View.VISIBLE);
				cardImgBtn2.setImageResource(MainActivity
						.getCardDrawable(cardsArrayList.get(card2)));
				numberOfCardsToTake--;
			}
		if (numberOfCardsToTake > 0)
			if (card3 == -1) {
				usedCardsCounter++;
				card3 = dealtCards.get(usedCardsCounter);
				Log.i(TAG, "Iau cartea nr " + usedCardsCounter + " : " + card3);
				Toast.makeText(this,
						"Iau cartea nr " + usedCardsCounter + " : " + card3,
						Toast.LENGTH_LONG).show();
				ImageButton cardImgBtn3 = (ImageButton) findViewById(R.id.card3);
				cardImgBtn3.setVisibility(View.VISIBLE);
				cardImgBtn3.setImageResource(MainActivity
						.getCardDrawable(cardsArrayList.get(card3)));
				numberOfCardsToTake--;
			}
		if (numberOfCardsToTake > 0)
			if (card4 == -1) {
				usedCardsCounter++;
				card4 = dealtCards.get(usedCardsCounter);
				Log.i(TAG, "Iau cartea nr " + usedCardsCounter + " : " + card4);
				Toast.makeText(this,
						"Iau cartea nr " + usedCardsCounter + " : " + card4,
						Toast.LENGTH_LONG).show();
				ImageButton cardImgBtn4 = (ImageButton) findViewById(R.id.card4);
				cardImgBtn4.setVisibility(View.VISIBLE);
				cardImgBtn4.setImageResource(MainActivity
						.getCardDrawable(cardsArrayList.get(card4)));
				numberOfCardsToTake--;
			}
	}

	private void setCardButtonsEnabled(boolean value) {
		ImageButton card1 = (ImageButton) findViewById(R.id.card1);
		ImageButton card2 = (ImageButton) findViewById(R.id.card2);
		ImageButton card3 = (ImageButton) findViewById(R.id.card3);
		ImageButton card4 = (ImageButton) findViewById(R.id.card4);

		card1.setEnabled(value);
		card2.setEnabled(value);
		card3.setEnabled(value);
		card4.setEnabled(value);

		if (value == true) {
			card1.setAlpha(0.99f);
			card2.setAlpha(0.99f);
			card3.setAlpha(0.99f);
			card4.setAlpha(0.99f);
		} else {
			card1.setAlpha(0.5f);
			card2.setAlpha(0.5f);
			card3.setAlpha(0.5f);
			card4.setAlpha(0.5f);
		}

		card1.requestLayout();
		card1.invalidate();
		card2.requestLayout();
		card2.invalidate();
		card3.requestLayout();
		card3.invalidate();
		card4.requestLayout();
		card4.invalidate();
	}

	public void imgBtn1Clicked(View view) {

		ImageButton firstImgBtn = (ImageButton) findViewById(R.id.myCardImgBtn);
		ImageButton secondImgBtn = (ImageButton) findViewById(R.id.opponentCardImgBtn);

		if (firstCardSW == true) {

			firstImgBtn.bringToFront();
			firstImgBtn.requestLayout();
			firstImgBtn.invalidate();
			if (firstHand == true)
				firstCardOfHand = Integer.valueOf(cardsArrayList.get(card1)
						.substring(3));

			firstCard = card1;
			ImageButton imgBtn7 = (ImageButton) findViewById(R.id.myCardImgBtn);
			imgBtn7.setVisibility(View.VISIBLE);
			imgBtn7.setImageResource(MainActivity
					.getCardDrawable(cardsArrayList.get(card1)));

			String message = Integer.toString(card1);
			Log.i(TAG, "Cartea 1 din onCLick: " + message);
			mChatService.write(message.getBytes());
			firstCardSW = false;

			setCardButtonsEnabled(false);

			ImageButton card1 = (ImageButton) findViewById(R.id.card1);
			card1.setVisibility(View.INVISIBLE);
			// card1.setImageDrawable(new ColorDrawable(0xFFFFFF));

			ImageButton flagButton = (ImageButton) findViewById(R.id.flagImgBtn);
			flagButton.setVisibility(View.INVISIBLE);

			this.card1 = -1;
		} else {

			secondImgBtn.bringToFront();
			secondImgBtn.requestLayout();
			secondImgBtn.invalidate();
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

			setCardButtonsEnabled(false);

			card1.setVisibility(View.INVISIBLE);
			// card1.setImageDrawable(new ColorDrawable(0xFFFFFF));
			this.card1 = -1;
		}

	}

	public void imgBtn2Clicked(View view) {

		ImageButton firstImgBtn = (ImageButton) findViewById(R.id.myCardImgBtn);
		ImageButton secondImgBtn = (ImageButton) findViewById(R.id.opponentCardImgBtn);
		if (firstCardSW == true) {

			firstImgBtn.bringToFront();
			firstImgBtn.requestLayout();
			firstImgBtn.invalidate();
			if (firstHand == true)
				firstCardOfHand = Integer.valueOf(cardsArrayList.get(card2)
						.substring(3));

			firstCard = card2;
			ImageButton imgBtn7 = (ImageButton) findViewById(R.id.myCardImgBtn);
			imgBtn7.setVisibility(View.VISIBLE);
			imgBtn7.setImageResource(MainActivity
					.getCardDrawable(cardsArrayList.get(card2)));

			String message = Integer.toString(card2);
			Log.i(TAG, "Cartea 2 din onCLick: " + message);
			mChatService.write(message.getBytes());
			firstCardSW = false;

			ImageButton card2 = (ImageButton) findViewById(R.id.card2);

			setCardButtonsEnabled(false);

			card2.setVisibility(View.INVISIBLE);
			// card2.setImageDrawable(new ColorDrawable(0xFFFFFF));
			ImageButton flagButton = (ImageButton) findViewById(R.id.flagImgBtn);
			flagButton.setVisibility(View.INVISIBLE);

			this.card2 = -1;
		} else {

			secondImgBtn.bringToFront();
			secondImgBtn.requestLayout();
			secondImgBtn.invalidate();
			secondCard = card2;
			ImageButton opponentCard = (ImageButton) findViewById(R.id.opponentCardImgBtn);
			opponentCard.setVisibility(View.VISIBLE);
			opponentCard.setImageResource(MainActivity
					.getCardDrawable(cardsArrayList.get(card2)));

			String message = Integer.toString(card2);
			Log.i(TAG, "Cartea 2 din onCLick: " + message);
			mChatService.write(message.getBytes());
			firstCardSW = true;

			ImageButton card2 = (ImageButton) findViewById(R.id.card2);

			setCardButtonsEnabled(false);

			card2.setVisibility(View.INVISIBLE);
			// card2.setImageDrawable(new ColorDrawable(0xFFFFFF));
			this.card2 = -1;
		}
	}

	public void imgBtn3Clicked(View view) {
		ImageButton firstImgBtn = (ImageButton) findViewById(R.id.myCardImgBtn);
		ImageButton secondImgBtn = (ImageButton) findViewById(R.id.opponentCardImgBtn);
		if (firstCardSW == true) {

			firstImgBtn.bringToFront();
			firstImgBtn.requestLayout();
			firstImgBtn.invalidate();
			if (firstHand == true)
				firstCardOfHand = Integer.valueOf(cardsArrayList.get(card3)
						.substring(3));

			firstCard = card3;
			ImageButton imgBtn7 = (ImageButton) findViewById(R.id.myCardImgBtn);
			imgBtn7.setVisibility(View.VISIBLE);
			imgBtn7.setImageResource(MainActivity
					.getCardDrawable(cardsArrayList.get(card3)));

			String message = Integer.toString(card3);
			Log.i(TAG, "Cartea 3 din onCLick: " + message);
			mChatService.write(message.getBytes());
			firstCardSW = false;

			ImageButton card3 = (ImageButton) findViewById(R.id.card3);

			setCardButtonsEnabled(false);

			card3.setVisibility(View.INVISIBLE);
			// card3.setImageDrawable(new ColorDrawable(0xFFFFFF));

			ImageButton flagButton = (ImageButton) findViewById(R.id.flagImgBtn);
			flagButton.setVisibility(View.INVISIBLE);
			this.card3 = -1;
		} else {
			secondImgBtn.bringToFront();
			secondImgBtn.requestLayout();
			secondImgBtn.invalidate();
			secondCard = card3;
			ImageButton opponentCard = (ImageButton) findViewById(R.id.opponentCardImgBtn);
			opponentCard.setVisibility(View.VISIBLE);
			opponentCard.setImageResource(MainActivity
					.getCardDrawable(cardsArrayList.get(card3)));

			String message = Integer.toString(card3);
			Log.i(TAG, "Cartea 3 din onCLick: " + message);
			mChatService.write(message.getBytes());
			firstCardSW = true;

			ImageButton card3 = (ImageButton) findViewById(R.id.card3);

			setCardButtonsEnabled(false);

			card3.setVisibility(View.INVISIBLE);
			// card3.setImageDrawable(new ColorDrawable(0xFFFFFF));
			this.card3 = -1;
		}
	}

	public void imgBtn4Clicked(View view) {
		ImageButton firstImgBtn = (ImageButton) findViewById(R.id.myCardImgBtn);
		ImageButton secondImgBtn = (ImageButton) findViewById(R.id.opponentCardImgBtn);
		if (firstCardSW == true) {

			firstImgBtn.bringToFront();
			firstImgBtn.requestLayout();
			firstImgBtn.invalidate();
			if (firstHand == true)
				firstCardOfHand = Integer.valueOf(cardsArrayList.get(card4)
						.substring(3));

			firstCard = card4;
			ImageButton imgBtn7 = (ImageButton) findViewById(R.id.myCardImgBtn);
			imgBtn7.setVisibility(View.VISIBLE);
			imgBtn7.setImageResource(MainActivity
					.getCardDrawable(cardsArrayList.get(card4)));

			String message = Integer.toString(card4);
			Log.i(TAG, "Cartea 4 din onCLick: " + message);
			mChatService.write(message.getBytes());
			firstCardSW = false;

			ImageButton card4 = (ImageButton) findViewById(R.id.card4);

			setCardButtonsEnabled(false);

			card4.setVisibility(View.INVISIBLE);
			// card4.setImageDrawable(new ColorDrawable(0xFFFFFF));
			ImageButton flagButton = (ImageButton) findViewById(R.id.flagImgBtn);
			flagButton.setVisibility(View.INVISIBLE);
			this.card4 = -1;
		} else {
			secondImgBtn.bringToFront();
			secondImgBtn.requestLayout();
			secondImgBtn.invalidate();
			secondCard = card4;
			ImageButton opponentCard = (ImageButton) findViewById(R.id.opponentCardImgBtn);
			opponentCard.setVisibility(View.VISIBLE);
			opponentCard.setImageResource(MainActivity
					.getCardDrawable(cardsArrayList.get(card4)));

			String message = Integer.toString(card4);
			Log.i(TAG, "Cartea 4 din onCLick: " + message);
			mChatService.write(message.getBytes());
			firstCardSW = true;

			ImageButton card4 = (ImageButton) findViewById(R.id.card4);

			setCardButtonsEnabled(false);

			card4.setVisibility(View.INVISIBLE);
			// card4.setImageDrawable(new ColorDrawable(0xFFFFFF));
			this.card4 = -1;
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
