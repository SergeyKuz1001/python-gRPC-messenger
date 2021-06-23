/*
 * Copyright 2015 The gRPC Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.azaat.grpcchatclient;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.view.View;

import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

public class ClientActivity extends AppCompatActivity {
    private static final int REQUEST_DEADLINE_MS = 1000;
    private static final int TERMINATION_TIMEOUT_S = 1;
    private static final String TAG = "ClientActivity";
    private ApplicationInterface applicationInterface;
    private String nameStr;
    private String serverName;
    private Thread incomingMsgHandler;
    private ManagedChannel channel;

    public String getServerName() {
        return serverName;
    }

    public String getNameStr() {
        return nameStr;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.client_activity);
        applicationInterface = new ApplicationInterface(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        FutureTask<Integer> disconnectionTask = disconnect();
        try {
            disconnectionTask.get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    public void connect(View view) {
        Pair<Pair<String, String>, String> result = applicationInterface.onInitiateConnection();
        Pair<String, String> address = result.first;
        new ConnectionTask(this)
                .execute(
                        address.first, address.second, result.second
                );
    }

    public void disconnect(View view) {
        disconnect();
    }

    public FutureTask<Integer> disconnect() {
        FutureTask<Integer> disconnectionTask = new FutureTask<>(
                () -> {
                    MessengerGrpc.MessengerBlockingStub stub = MessengerGrpc.newBlockingStub(channel);
                    try {
                        MessengerProto.Empty result = stub.withDeadlineAfter(REQUEST_DEADLINE_MS, TimeUnit.MILLISECONDS)
                                .stopMessaging(MessengerProto.Empty.newBuilder().build());
                    } catch (io.grpc.StatusRuntimeException e) {
                        Log.d(TAG, "Exceeded disconnection deadline, server might be down");
                    }
                    channel.shutdownNow().awaitTermination(TERMINATION_TIMEOUT_S, TimeUnit.SECONDS);
                    channel = null;
                    incomingMsgHandler.interrupt();
                    runOnUiThread(
                            () -> {
                                applicationInterface.onDisconnect();
                            }
                    );
                    return 0;
                }
        );
        disconnectionTask.run();
        return disconnectionTask;
    }

    public void sendMessage(View view) {
        String msg = applicationInterface.onSendMessage();
        new GrpcTask(this)
                .execute(msg
                );
    }

    /**
     * Blocking call, should be used in a separate thread
     *
     * @param stub gRPC client object
     */
    private void getMessages(MessengerGrpc.MessengerBlockingStub stub) {
        try {
            Iterator<MessengerProto.MessengerMessage> result = stub.sendMessage(MessengerProto.Empty.newBuilder().build());

            do {
                // Trying to get the next message from server
                MessengerProto.MessengerMessage msg = result.next();
                runOnUiThread(
                        () -> applicationInterface.onReceiveMessage(msg.getMessage())
                );
            } while (result.hasNext());
        } catch (io.grpc.StatusRuntimeException e) {
            Log.d(TAG, "Failed rpc request");
            runOnUiThread(
                    () -> {
                        applicationInterface.onRequestFailed();
                    }
            );
        }
    }

    private static class GrpcTask extends AsyncTask<String, Void, String> {
        private final WeakReference<ClientActivity> activityReference;
        private final ManagedChannel channel;

        private GrpcTask(ClientActivity activity) {
            this.activityReference = new WeakReference<>(activity);
            this.channel = activity.channel;
        }

        @Override
        protected String doInBackground(String... params) {
            String message = params[0];
            try {
                MessengerGrpc.MessengerBlockingStub stub = MessengerGrpc.newBlockingStub(channel);
                MessengerProto.MessengerMessage request = MessengerProto.MessengerMessage
                        .newBuilder().setMessage(message).build();
                // Perform gRPC request to send message with specified timeout
                MessengerProto.Empty result = stub.withDeadlineAfter(REQUEST_DEADLINE_MS, TimeUnit.MILLISECONDS).getMessage(request);
                return message;
            } catch (io.grpc.StatusRuntimeException e) {
                Log.d(TAG, "Failed rpc request");
                return null;
            }
        }

        @Override
        protected void onPostExecute(String result) {
            ClientActivity activity = activityReference.get();
            if (activity == null) {
                return;
            }
            if (result != null) {
                activity.applicationInterface.onMessageSent(result);
            } else {
                activity.applicationInterface.onRequestFailed();
            }
        }
    }

    private static class ConnectionTask extends AsyncTask<String, Void, MessengerProto.MessengerNameResponse> {
        private final WeakReference<ClientActivity> activityReference;
        private ManagedChannel channel;
        private MessengerGrpc.MessengerBlockingStub stub;
        private String nameStr;

        private ConnectionTask(ClientActivity activity) {
            this.activityReference = new WeakReference<>(activity);
        }

        @Override
        protected MessengerProto.MessengerNameResponse doInBackground(String... params) {
            String host = params[0];
            String portStr = params[1];
            nameStr = params[2];

            int port = TextUtils.isEmpty(portStr) ? 0 : Integer.parseInt(portStr);
            channel = ManagedChannelBuilder.forAddress(host, port)
                    .usePlaintext().build();
            stub = MessengerGrpc.newBlockingStub(channel);

            MessengerProto.MessengerNameRequest request = MessengerProto.MessengerNameRequest
                    .newBuilder().setName(nameStr).build();
            try {
                // Perform gRPC request to initiate connection
                return stub.withDeadlineAfter(REQUEST_DEADLINE_MS, TimeUnit.MILLISECONDS).startMessaging(request);
            } catch (io.grpc.StatusRuntimeException e) {
                Log.d(TAG, "Failed rpc request");
                return null;
            }

        }

        @Override
        protected void onPostExecute(MessengerProto.MessengerNameResponse result) {
            ClientActivity activity = activityReference.get();
            if (activity == null) {
                return;
            }
            if (result != null && result.getConnected()) {
                activity.channel = channel;
                activity.nameStr = nameStr;
                activity.serverName = result.getName();
                activity.incomingMsgHandler = new Thread(
                        () -> activity.getMessages(stub)
                );
                activity.incomingMsgHandler.start();
                activity.applicationInterface.onConnected();

            } else {
                Log.d(TAG, "Failed connection");
                activity.applicationInterface.onConnectFailed();
            }
        }
    }
}
