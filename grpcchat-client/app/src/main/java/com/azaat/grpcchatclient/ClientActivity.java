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
                    MessengerProto.Empty result = stub.stopMessaging(MessengerProto.Empty.newBuilder().build());
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
        new GrpcTask(this)
                .execute(
                        applicationInterface.onSendMessage());
    }

    private static class GrpcTask extends AsyncTask<String, Void, Integer> {
        private final WeakReference<Activity> activityReference;
        private final ManagedChannel channel;

        private GrpcTask(ClientActivity activity) {
            this.activityReference = new WeakReference<>(activity);
            this.channel = activity.channel;
        }

        @Override
        protected Integer doInBackground(String... params) {
            String message = params[0];
            try {
                MessengerGrpc.MessengerBlockingStub stub = MessengerGrpc.newBlockingStub(channel);
                MessengerProto.MessengerMessage request = MessengerProto.MessengerMessage
                        .newBuilder().setMessage(message).build();
                MessengerProto.Empty result = stub.getMessage(request);
                return 1;
            } catch (Exception e) {
                return 0;
                // TODO: proper error handling
            }
        }

        @Override
        protected void onPostExecute(Integer result) {
            Activity activity = activityReference.get();
            if (activity == null) {
                return;
            }
            Button sendButton = (Button) activity.findViewById(R.id.send_button);
            sendButton.setEnabled(true);
        }
    }

    private void getMessages(MessengerGrpc.MessengerBlockingStub stub) {
        Iterator<MessengerProto.MessengerMessage> result = stub.sendMessage(MessengerProto.Empty.newBuilder().build());
        try {
            do {
                MessengerProto.MessengerMessage msg = result.next();
                runOnUiThread(
                        () -> {
                            applicationInterface.onReceiveMessage(msg.getMessage());
                        }
                );
            } while (result.hasNext());
        } catch (io.grpc.StatusRuntimeException e) {
            // TODO: log/notify
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
            try {
                channel = ManagedChannelBuilder.forAddress(host, port)
                        .usePlaintext().build();
                stub = MessengerGrpc.newBlockingStub(channel);

                MessengerProto.MessengerNameRequest request = MessengerProto.MessengerNameRequest
                        .newBuilder().setName(nameStr).build();
                return stub.startMessaging(request);
            } catch (Exception e) {
                // TODO: proper error handling
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
                Log.d("ClientActivity", "Failed connection");
            }
        }
    }
}
