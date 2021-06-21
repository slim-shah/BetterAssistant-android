/*
 * Copyright (c) 2018 Samsung Electronics Co., Ltd. All rights reserved.
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that
 * the following conditions are met:
 *
 *     * Redistributions of source code must retain the above copyright notice,
 *       this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright notice,
 *       this list of conditions and the following disclaimer in the documentation and/or
 *       other materials provided with the distribution.
 *     * Neither the name of Samsung Electronics Co., Ltd. nor the names of its contributors may be used to endorse or
 *       promote products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package com.cybernetic87.GAssist;

import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.fragment.app.FragmentActivity;
import androidx.navigation.NavController;
import androidx.navigation.NavGraph;
import androidx.navigation.NavInflater;
import androidx.navigation.fragment.NavHostFragment;

import java.io.File;
import java.util.Objects;


public class MainActivity extends FragmentActivity
        implements ChooseFileFragment.OnFragmentInteractionListener {


    private final BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    private ServiceConnection mConnection;
    private ProviderService providerService;

//    public void updateTextView(final String str) {
//        mTextView.setText(str);
//    }
//
//    public void SetStatusIcon(int resId) {
//        mImageView.setImageResource(resId);
//    }

    private void startService() {
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, 1);
        } else {
//            String packageName = this.getPackageName();
//            PowerManager pm = (PowerManager) this.getSystemService(Context.POWER_SERVICE);
//            if (pm.isIgnoringBatteryOptimizations(packageName))
//                intent.setAction(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
//
//            else {
//                intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
//                intent.setData(Uri.parse("package:" + packageName));
//                startActivityForResult(intent, 2);
//            }
            Intent intent = new Intent(this, Service.class);
            intent.setAction(Service.ACTION_START_FOREGROUND_SERVICE);
            startForegroundService(intent);
            //moveTaskToBack(true);
        }

    }

    private void BindToService() {

        mConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName className, IBinder service) {
                providerService = ((ProviderService.LocalBinder) service).getService();
                providerService.SetmProviderActivity(MainActivity.this);
            }

            @Override
            public void onServiceDisconnected(ComponentName className) {
                providerService = null;
            }
        };

        bindService(new Intent(MainActivity.this, ProviderService.class),
                mConnection,
                Context.BIND_AUTO_CREATE);
    }

    private void UnbindService() {
        if (mConnection != null)
            MainActivity.this.unbindService(mConnection);
    }

//    public void refreshLog(View view) {
//        try {
//            Process process = Runtime.getRuntime().exec("logcat EmbeddedAssistant:W MainActivity:W ProviderService:V -d");
//            BufferedReader bufferedReader = new BufferedReader(
//                    new InputStreamReader(process.getInputStream()));
//
//            StringBuilder log = new StringBuilder();
//            String line;
//            while ((line = bufferedReader.readLine()) != null) {
//                log.append(line + "\n");
//            }
//            TextView tv = findViewById(R.id.logTextView);
//            tv.setTextIsSelectable(true);
//            tv.setFocusable(true);
//            tv.setFocusableInTouchMode(true);
//            tv.setText(log.toString());
//        } catch (IOException e) {
//            // Handle Exception
//        }
//    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        startService();

        NavHostFragment navHost = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        NavController navController = Objects.requireNonNull(navHost).getNavController();

        NavInflater navInflater = navController.getNavInflater();
        NavGraph graph = navInflater.inflate(R.navigation.nav_graph);

        File device = new File(getApplicationContext().getFilesDir().getAbsolutePath() + "/device.json");
        Log.println(Log.ERROR, "Meet:", device.getAbsolutePath());
        Log.println(Log.ERROR, "Device:", Boolean.toString(device.exists()));

        if (device.exists()) {
            graph.setStartDestination(R.id.fragment_main);
        } else {
            graph.setStartDestination(R.id.fragment_permissions);
        }

        navController.setGraph(graph);
        //mTextView = findViewById(R.id.tvStatus);
        //mImageView = findViewById(R.id.imageView);
        //updateTextView("Disconnected");
        BindToService();

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1) {
            if (resultCode == RESULT_CANCELED) {
                Toast.makeText(MainActivity.this, "Bluetooth must be enabled to use this app", Toast.LENGTH_LONG).show();
                closeApp();
            } else {
                startService();
            }
        }
    }


    @Override
    protected void onDestroy() {
        UnbindService();
        super.onDestroy();
    }

    private void closeApp() {
        this.finishAndRemoveTask();
    }

}
