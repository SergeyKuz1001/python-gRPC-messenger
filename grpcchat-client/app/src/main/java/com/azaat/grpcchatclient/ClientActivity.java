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

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.Button;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
//import io.grpc.examples.helloworld.GreeterGrpc;
//import io.grpc.examples.helloworld.HelloReply;
//import io.grpc.examples.helloworld.HelloRequest;

//import java.io.PrintWriter;
//import java.io.StringWriter;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Calendar;
import java.util.Iterator;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

public class ClientActivity extends AppCompatActivity {
    private ApplicationInterface applicationInterface;

    public String getServerName() {
        return serverName;
    }

    public String getNameStr() {
        return nameStr;
    }

    private static final int deadlineMs = 1000;
    private static final String TAG = "ClientActivity";
    private String nameStr;
    private String serverName;
    private Thread incomingMsgHandler;
    private ManagedChannel channel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_helloworld);
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
                    try { MessengerProto.Empty result = stub.withDeadlineAfter(deadlineMs, TimeUnit.MILLISECONDS)
                            .stopMessaging(MessengerProto.Empty.newBuilder().build());  }
                    catch (io.grpc.StatusRuntimeException e) {
                        Log.d(TAG, "Exceeded disconnection deadline, server might be down");
                    }
                    channel.shutdownNow().awaitTermination(1, TimeUnit.SECONDS);
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
                MessengerProto.Empty result = stub.withDeadlineAfter(deadlineMs, TimeUnit.MILLISECONDS).getMessage(request);
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
            Button sendButton = (Button) activity.findViewById(R.id.send_button);
            sendButton.setEnabled(true);
            if (result != null) {
                activity.applicationInterface.onMessageSent(result);
            } else {
                activity.applicationInterface.onRequestFailed();
            }
        }
    }

    private void getMessages(MessengerGrpc.MessengerBlockingStub stub) {
        try {
            Iterator<MessengerProto.MessengerMessage> result = stub.sendMessage(MessengerProto.Empty.newBuilder().build());

            do {
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
                return stub.withDeadlineAfter(deadlineMs, TimeUnit.MILLISECONDS).startMessaging(request);
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
