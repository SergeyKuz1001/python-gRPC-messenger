package com.azaat.grpcchatclient;

import android.content.Context;
import android.text.method.ScrollingMovementMethod;
import android.util.Pair;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.io.LineReader;


public class ApplicationInterface {
    private final ClientActivity context;
    private final Button sendButton;
    private final EditText hostEdit;
    private final Button connectButton;
    private final EditText portEdit;
    private final EditText nameEdit;
    private final EditText messageEdit;
    private final TextView resultText;

//    private final Button connectButton;
    private final LinearLayout overlayLayout;
    private final LinearLayout chatLayout;
    private final TextView connectRes;

    private void addMessage(String msg, String name) {
        // append the new string
        resultText.append(name + " : " + msg + "\n");
        // find the amount we need to scroll.  This works by
        // asking the TextView's internal layout for the position
        // of the final line and then subtracting the TextView's height
        final int scrollAmount = resultText.getLayout().getLineTop(resultText.getLineCount()) - resultText.getHeight();
        // if there is no need to scroll, scrollAmount will be <=0
        if (scrollAmount > 0)
            resultText.scrollTo(0, scrollAmount);
        else
            resultText.scrollTo(0, 0);
    }

    public ApplicationInterface(ClientActivity context) {
        this.context = context;
        sendButton = (Button) context.findViewById(R.id.send_button);
        hostEdit = (EditText) context.findViewById(R.id.host_edit_text);
        portEdit = (EditText) context.findViewById(R.id.port_edit_text);
        overlayLayout = (LinearLayout) context.findViewById(R.id.overlay);
        messageEdit = (EditText) context.findViewById(R.id.message_edit_text);
        resultText = (TextView) context.findViewById(R.id.grpc_response_text);
        chatLayout = (LinearLayout) context.findViewById(R.id.chat_layout);
        connectRes = (TextView) context.findViewById(R.id.connect_result);
        nameEdit = (EditText) context.findViewById(R.id.name_edit_text);
        connectButton = (Button) context.findViewById(R.id.connect_button);
        resultText.setMovementMethod(new ScrollingMovementMethod());
    }

    public Pair<Pair<String, String>, String> onInitiateConnection() {
        connectButton.setEnabled(false);
        ((InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE))
                .hideSoftInputFromWindow(hostEdit.getWindowToken(), 0);
        return new Pair<>(new Pair<>(hostEdit.getText().toString(),
                portEdit.getText().toString()), nameEdit.getText().toString());
    }

    public void onDisconnect() {
        overlayLayout.setVisibility(View.VISIBLE);
        chatLayout.setVisibility(View.GONE);
        connectButton.setEnabled(true);
    }

    public String onSendMessage() {
        sendButton.setEnabled(false);
        String msg = messageEdit.getText().toString();
        messageEdit.setText("");
        addMessage(msg, context.getNameStr());
        return msg;
    }

    public void onReceiveMessage(String msg) {
        addMessage(msg, context.getServerName());
    }

    public void onConnectFailed() {
        Toast.makeText(context, "Couldn't connect to server..",
                Toast.LENGTH_LONG).show();
        connectButton.setEnabled(true);
    }

    public void onConnected() {
        chatLayout.setVisibility(View.VISIBLE);
        overlayLayout.setVisibility(View.GONE);
        sendButton.setEnabled(true);
    }
}
