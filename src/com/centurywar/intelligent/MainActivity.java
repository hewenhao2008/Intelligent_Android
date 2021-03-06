package com.centurywar.intelligent;


import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.centurywar.intelligent.control.BaseControl;



import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Switch;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;
import at.abraxas.amarino.Amarino;

public class MainActivity extends BaseActivity {
	private Button btnAdd;
	private Button btnClear;
	private Button btnGame1;
	private Button btnSocket;
	private Button btnSocketSend;
	private Button btnGetTem;
	private EditText editName;
	private TextView txtSocket;
	private TextView txtAutoRemain;
	private EditText editPik;
	private TableLayout table;
	private TextView txtError;
	private TextView lightRate;
	private SeekBar lightBar;
	protected SharedPreferences gameInfo;
	private int maxlight = 255, currentlight = 0;
	private BaseControl tembc;
	private Switch switchTest;
	private Switch switchAuto;
	/**
	 * 自动匹配板子倒计时
	 */
	private int autoRemainSec=0;
	private List<Integer> statusChange=new ArrayList<Integer>();
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		btnAdd = (Button) findViewById(R.id.btnAdd);
		btnClear = (Button) findViewById(R.id.btnClear);
		editName = (EditText) findViewById(R.id.editName);
		editPik = (EditText) findViewById(R.id.editPik);
		table = (TableLayout) findViewById(R.id.layoutBtn);
		txtError = (TextView) findViewById(R.id.txtError);
		lightRate = (TextView) findViewById(R.id.lightRate);
		txtAutoRemain = (TextView) findViewById(R.id.autoText);
		gameInfo = getSharedPreferences("gameInfo", 0);
		lightBar = (SeekBar) findViewById(R.id.lightBar);
		
		switchTest = (Switch) findViewById(R.id.switch1);
		switchTest
				.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
					@Override
					public void onCheckedChanged(CompoundButton buttonView,
							boolean isChecked) {
						updateUserMode(isChecked ? 2 : 1);
					}
				});
		switchAuto = (Switch) findViewById(R.id.switch2);
		switchAuto
				.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
					@Override
					public void onCheckedChanged(CompoundButton buttonView,
							boolean isChecked) {
						if (switchAuto.isChecked()) {
							autoArduino();
							switchAuto.setChecked(true);
						}
					}
				});
		int mode = getGameInfoInt("mode");
		switchTest.setChecked(mode==ConstantControl.MODE_OUT?true:false);
		
		btnClear.setOnClickListener(new Button.OnClickListener() {
			@Override
			public void onClick(View v) {
				gameInfo.edit().putString("user_setting", "").commit();
				updateword();
				updateDeviceToServer();
			}
		});
		
		

		btnAdd.setOnClickListener(new Button.OnClickListener() {
			@Override
			public void onClick(View v) {
				//
				String strtem = gameInfo.getString("user_setting", "");
				JSONArray jsa;
				try {
					if (strtem.length() > 0) {
						jsa = new JSONArray(strtem);
					} else {
						jsa = new JSONArray();
					}
					JSONObject obj = new JSONObject();
					obj.put("name", editName.getText().toString());
					obj.put("pik", editPik.getText().toString());
					obj.put("value", 0);
					jsa.put(obj);
					System.out.println(jsa.toString());
					gameInfo.edit().putString("user_setting", jsa.toString())
							.commit();
				} catch (Exception e) {

				}

				editName.setText("");
				editPik.setText("");
				try {
					updateword();
					updateDeviceToServer();
				} catch (Exception e) {
					e.printStackTrace();
				}

			}
		});

		try {
			updateword();
		} catch (Exception e) {
			e.printStackTrace();
		}
		lightBar.setMax(100);
		lightBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			public void onProgressChanged(SeekBar arg0, int progress,
					boolean fromUser) {
				currentlight = progress;
				lightBar.setProgress(currentlight);
				lightRate.setText(currentlight + "");

				sendMessage(getJsonobject(20, 9, (int) (currentlight*2.55), 0));
//				 bl.setPikType(mac, 3, 20);
				// bl.setValue(currentlight);
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub

			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub

			}
		});
	}

	
	public void MessageCallBack(JSONObject jsonobj) throws Exception {
		String command = jsonobj.getString("control");
		if (command.equals(ConstantControl.GET_USER_INFO)) {
			updateword();
		} else if (command.equals(ConstantControl.ECHO_GET_USER_TEMPERATURE)) {
			// 取得温度
			JSONObject tem = (JSONObject) jsonobj.getJSONArray("data").get(0);
			btnGetTem.setText("当前温度：" + tem.get("values").toString());
		} else if (command.equals(ConstantControl.ECHO_SET_STATUS)) {
			String str = jsonobj.getString("command");
			String[] temcommand = str.split("_");
			// r_10_3_1_0
			int pik = Integer.parseInt(temcommand[2]);
			if (statusChange.contains(pik)) {
				statusChange.remove(statusChange.indexOf(pik));
			}
			int val = Integer.parseInt(temcommand[3]);
			String strtem = gameInfo.getString("user_setting", "");
			try {
				JSONArray jsa = new JSONArray(strtem);
				for (int i = 0; i < jsa.length(); i++) {
					JSONObject obj = jsa.getJSONObject(i);
					if (obj.getInt("pik") == pik) {
						obj.put("value", val);
					}
				}
				gameInfo.edit().putString("user_setting", jsa.toString())
						.commit();
			} catch (Exception e) {
				e.printStackTrace();
			}
			updateword();

		}

	}
	
	/**
	 * 把本地配置同步到网上
	 */
	private void updateDeviceToServer() {
		JSONObject jsob = new JSONObject();
		try {
			jsob.put("control", ConstantControl.UPDAT_DEVICE_TO_SERVER);
			JSONArray jsa = new JSONArray(
					gameInfo.getString("user_setting", ""));
			jsob.put("device", jsa);
		} catch (Exception e) {
		}
		sendMessage(jsob);
	}

	
	/**
	 * 自动匹配arudion
	 */
	private void autoArduino() {
		autoRemainSec = 10;
		sendControl(ConstantControl.AUTO_GET_ARUDINO_ID, null);
	}
	
	
	/**
	 * 更新用户的模式
	 * 
	 * @param mode
	 */
	private void updateUserMode(int mode) {
		JSONObject jsob = new JSONObject();
		try {
			jsob.put("control", ConstantControl.UPDAT_USER_MODE);
			jsob.put("mode", mode);
			sendMessage(jsob);
		} catch (Exception e) {
			System.out.println(e.toString());
		}
	}

	private void updateword()  {
		try {
			JSONArray jsa = new JSONArray(
					gameInfo.getString("user_setting", ""));
			table.removeAllViews();
			for (int n = 0; n < jsa.length(); n++) {
				JSONObject obj = jsa.getJSONObject(n);
				if (jsa.getJSONObject(n) == null) {
					continue;
				}
				TableRow tr = new TableRow(this);
				final int pik = obj.getInt("pik");
				Button open = new Button(this);
				open.setText("open");
				open.setTag(pik);
				open.setOnClickListener(new Button.OnClickListener() {
					@Override
					public void onClick(View v) {
						setControl(1 + pik * 10);
					}
				});
				Button close = new Button(this);
				close.setText("close");
				open.setTag(pik);
				close.setOnClickListener(new Button.OnClickListener() {
					@Override
					public void onClick(View v) {
						setControl(0 + pik * 10);
					}
				});
				
				Button remove = new Button(this);
				remove.setText("移除");
				remove.setTag(pik);
				remove.setOnClickListener(new Button.OnClickListener() {
					@Override
					public void onClick(View v) {
						removeControl(pik);
					}
				});
				
				final EditText temtext = new EditText(this);

				temtext.setText(obj.getString("name"));
				
				final TextView statustext = new TextView(this);

				String closeOrOpen = obj.getInt("value") == 0 ? "关闭" : "打开";

				if (statusChange.contains(obj.getInt("pik"))) {
					closeOrOpen = "loading...";
					open.setEnabled(false);
					close.setEnabled(false);
				}
				
				
				temtext.setOnFocusChangeListener(new OnFocusChangeListener() {
					public void onFocusChange(View v, boolean hasFocus) {
						// hasFocus?
						changeNameControl(pik,((EditText)v).getText().toString().trim());
					}
				});

				statustext.setText(String.format("[%d] %s",
						obj.getInt("pik"), closeOrOpen));
				
				tr.addView(temtext);
				tr.addView(statustext);
				tr.addView(open);
				tr.addView(close);
				tr.addView(remove);
				table.addView(tr);
			}
		} catch (Exception e) {
			System.out.println(e.toString());
		}
	}

	private void removeControl(int pik) {
		try {
			JSONArray jsa = new JSONArray(
					gameInfo.getString("user_setting", ""));
			JSONArray jsatem = new JSONArray();
			for (int i = 0; i < jsa.length(); i++) {
				final int bepik = jsa.getJSONObject(i).getInt("pik");
				if (bepik != pik) {
					jsatem.put(jsa.getJSONObject(i));
				}
			}
			gameInfo.edit().putString("user_setting", jsatem.toString())
					.commit();
			updateword();
			updateDeviceToServer();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void changeNameControl(int pik, String name) {
		try {
			JSONArray jsa = new JSONArray(
					gameInfo.getString("user_setting", ""));
			boolean ischange = false;
			for (int i = 0; i < jsa.length(); i++) {
				final int bepik = jsa.getJSONObject(i).getInt("pik");
				if (bepik == pik
						&& !jsa.getJSONObject(i).getString("name").equals(name)) {
					jsa.getJSONObject(i).put("name", name);
					ischange = true;
				}
			}
			if (ischange) {
				gameInfo.edit().putString("user_setting", jsa.toString())
						.commit();
				updateword();
				updateDeviceToServer();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void setControl(int getstatus) {
		int pik = getstatus / 10;
		int type = 10;
		boolean status = false;
		if (getstatus % 10 == 1) {
			status = true;
		}

		int delay=currentlight;
		if (status) {
			sendMessage(getJsonobject(10, pik, 1, 0));
		} else {
			sendMessage(getJsonobject(10, pik, 0, 0));
		}
		setButtonLoading(pik);
	}

	/**
	 * 把按键状态设置为loading
	 * @param pik
	 */
	public void setButtonLoading(int pik){
		statusChange.add(pik);
		updateword();
	}
	
	public void onResume() {
		super.onResume();
//		if (checkBluetooth()) {
//			txtError.setVisibility(View.GONE);
//		} else {
//			txtError.setVisibility(View.VISIBLE);
//			txtError.setText("未打开蓝牙");
//		}
	}

	
	/**
	 * 在主函数哦中的回调函数，每秒调用一次
	 */
	@Override
	protected void addOneSec() {
		// 使上次板子登录时间更新
		setGameInfoInt("last_arduino_login",
				getGameInfoInt("last_arduino_login") + 1);
		int time = getGameInfoInt("last_arduino_login");
		int lestTimeShow = 60;
		if (time < lestTimeShow) {
			txtError.setVisibility(View.GONE);
			txtError.setText("板子连接正常");
		} else {
			txtError.setVisibility(View.VISIBLE);
			txtError.setText("板子离线时间：" + (time - lestTimeShow));
		}
		
		
		if (autoRemainSec > 0) {
			autoRemainSec--;
			txtAutoRemain.setText(autoRemainSec+"");
		} else {
			txtAutoRemain.setText("");
			switchAuto.setChecked(false);
		}
		
	}
	
	public void onPause() {
		super.onPause();
		finish();
	}
	
	@Override
	protected void onStart() {
		super.onStart();
	}


	@Override
	protected void onStop() {
		super.onStop();
		// do never forget to unregister a registered receiver
	}
	
	public void StatusCallBack(JSONObject jsonobj) throws Exception {
		int code = jsonobj.getInt("code");
		if (code == ConstantCode.AUTO_GET_ARDUINO_ID_SUCCESS) {
			autoRemainSec = 0;
			sendControl(ConstantControl.GET_USER_INFO, null);
		}
	}

}


//	