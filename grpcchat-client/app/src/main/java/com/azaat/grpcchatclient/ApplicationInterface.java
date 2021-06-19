package com.azaat.grpcchatclient;

import android.content.Context;
import android.text.method.ScrollingMovementMethod;
import android.util.Pair;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.azaat.UIHelpers.RainbowTextView;

import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Provides public calls, which perform UI modifications
 * on various application events
 */
public class ApplicationInterface {
    private final ClientActivity context;
    private final Button sendButton;
    private final EditText hostEdit;
    private final Button connectButton;
    private final EditText portEdit;
    private final EditText nameEdit;
    private final EditText messageEdit;
    private final RainbowTextView resultText;

    private final LinearLayout overlayLayout;
    private final LinearLayout chatLayout;

    /**
     * Instantiates UI element objects, should be called AFTER
     * View is created
     * @param context - activity parameter
     */
    public ApplicationInterface(ClientActivity context) {
        this.context = context;
        sendButton = context.findViewById(R.id.send_button);
        hostEdit = context.findViewById(R.id.host_edit_text);
        portEdit = context.findViewById(R.id.port_edit_text);
        overlayLayout = context.findViewById(R.id.overlay);
        messageEdit = context.findViewById(R.id.message_edit_text);
        resultText = context.findViewById(R.id.grpc_response_text);
        chatLayout = context.findViewById(R.id.chat_layout);
        nameEdit = context.findViewById(R.id.name_edit_text);
        connectButton = context.findViewById(R.id.connect_button);
        resultText.setMovementMethod(new ScrollingMovementMethod());
    }

    private void addMessage(String msg, String name) {
        // append the new string
        String timeStamp = new SimpleDateFormat("HH:mm:ss").format(Calendar.getInstance().getTime());
        resultText.append(timeStamp + " | " + name + " : " + msg + "\n");
        // find the amount we need to scroll.  This works by
        // asking the TextView's internal layout for the position
        // of the final line and then subtracting the TextView's height
        final int scrollAmount = resultText.getLayout().getLineTop(resultText.getLineCount()) - resultText.getHeight();
        // if there is no need to scroll, scrollAmount will be <=0
        resultText.scrollTo(0, Math.max(scrollAmount, 0));
    }

    /**
     * Is called when connection is initiated
     *
     * @return Pair of (HOSTNAME, PORT) and NAME
     */
    public Pair<Pair<String, String>, String> onInitiateConnection() {
        connectButton.setEnabled(false);
        ((InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE))
                .hideSoftInputFromWindow(hostEdit.getWindowToken(), 0);
        return new Pair<>(new Pair<>(hostEdit.getText().toString(),
                portEdit.getText().toString()), nameEdit.getText().toString());
    }

    /**
     * Call when disconnected from server
     */
    public void onDisconnect() {
        overlayLayout.setVisibility(View.VISIBLE);
        chatLayout.setVisibility(View.GONE);
        connectButton.setEnabled(true);
    }

    /**
     * Called when send button is pressed.
     * Deactivates button until message is sent/sending is failed due to timeout
     *
     * @return Returns
     */
    public String onSendMessage() {
        sendButton.setEnabled(false);
        String msg = messageEdit.getText().toString();
        messageEdit.setText("");
        return msg;
    }

    /**
     * Call when message is sent
     *
     * @param msg - message text
     */
    public void onMessageSent(String msg) {
        addMessage(msg, context.getNameStr());
        sendButton.setEnabled(true);
    }

    /**
     * Call when any gRPC request failed, which could
     * mean losing connection to server
     */
    public void onRequestFailed() {
        Toast.makeText(context, "Couldn't connect to server..",
                Toast.LENGTH_LONG).show();
        sendButton.setEnabled(true);
    }

    /**
     * Call when message is received from the server
     *
     * @param msg - message text
     */
    public void onReceiveMessage(String msg) {
        addMessage(msg, context.getServerName());
    }

    /**
     * Call after unsuccessful connection attempt
     */
    public void onConnectFailed() {
        Toast.makeText(context, "Disconnected",
                Toast.LENGTH_SHORT).show();
        connectButton.setEnabled(true);
    }

    /**
     * Call when connection is established
     */
    public void onConnected() {
        chatLayout.setVisibility(View.VISIBLE);
        overlayLayout.setVisibility(View.GONE);
        sendButton.setEnabled(true);
    }
}
