package edu.rit.se.crashavoidance.views;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.v4.app.ListFragment;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import org.apache.commons.lang3.SerializationUtils;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import edu.rit.se.crashavoidance.R;
import edu.rit.se.crashavoidance.network.Message;
import edu.rit.se.crashavoidance.network.MessageType;
import edu.rit.se.wifibuddy.CommunicationManager;
import edu.rit.se.wifibuddy.WifiDirectHandler;

/**
 * This fragment handles chat related UI which includes a list view for messages
 * and a message entry field with a send button.
 */
public class ChatFragment extends ListFragment {

    private EditText textMessageEditText;
    private ChatMessageAdapter adapter = null;
    private List<String> items = new ArrayList<>();
    private ArrayList<String> messages = new ArrayList<>();
    private WiFiDirectHandlerAccessor handlerAccessor;
    private Toolbar toolbar;
    private Button sendButton;
    private ImageButton cameraButton;
    private static final String TAG = WifiDirectHandler.TAG + "ListFragment";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat, container, false);

        sendButton = (Button) view.findViewById(R.id.sendButton);
        sendButton.setEnabled(false);

        cameraButton = (ImageButton) view.findViewById(R.id.cameraButton);

        textMessageEditText = (EditText) view.findViewById(R.id.textMessageEditText);
        textMessageEditText.addTextChangedListener(new TextWatcher() {

            @Override
            public void afterTextChanged(Editable s) {}

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                sendButton.setEnabled(true);
            }
        });

        ListView messagesListView = (ListView) view.findViewById(android.R.id.list);
        adapter = new ChatMessageAdapter(getActivity(), android.R.id.text1, items);
        messagesListView.setAdapter(adapter);
        messagesListView.setDividerHeight(0);

        // Prevents the keyboard from pushing the fragment and messages up and off the screen
        messagesListView.setTranscriptMode(ListView.TRANSCRIPT_MODE_NORMAL);
        messagesListView.setStackFromBottom(true);

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                Log.i(WifiDirectHandler.TAG, "Send button tapped");
                CommunicationManager communicationManager = handlerAccessor.getWifiHandler().getCommunicationManager();
                if (communicationManager != null && !textMessageEditText.toString().equals("")) {
                    String message = textMessageEditText.getText().toString();
                    // Gets first word of device name
                    String author = handlerAccessor.getWifiHandler().getThisDevice().deviceName.split(" ")[0];
                    byte[] messageBytes = (author + ": " + message).getBytes();
                    Message finalMessage = new Message(MessageType.TEXT, messageBytes);
                    communicationManager.write(SerializationUtils.serialize(finalMessage));
                } else {
                    Log.e(TAG, "Communication Manager is null");
                }
                String message = textMessageEditText.getText().toString();
                if (!message.equals("")) {
                    pushMessage("Me: " + message);
                    messages.add(message);
                    Log.i(TAG, "Message: " + message);
                    textMessageEditText.setText("");
                }
                sendButton.setEnabled(false);
            }
        });

        cameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if (takePictureIntent.resolveActivity(getContext().getPackageManager()) != null) {
                    startActivityForResult(takePictureIntent, 1);
                }
            }
        });

        toolbar = (Toolbar) getActivity().findViewById(R.id.mainToolbar);

        return view;
    }

    public interface MessageTarget {
        Handler getHandler();
    }

    public void pushMessage(byte[] readMessage) {
        Message message = SerializationUtils.deserialize(readMessage);
        switch(message.messageType) {
            case TEXT:
                Log.i(TAG, "Text message received");
                pushMessage(new String(message.message));
                break;
            case IMAGE:
                Log.i(TAG, "Image message received");
                Bitmap bitmap = BitmapFactory.decodeByteArray(message.message, 0, message.message.length);
                ImageView imageView = new ImageView(getContext());
                imageView.setImageBitmap(bitmap);
                loadPhoto(imageView, bitmap.getWidth(), bitmap.getHeight());
                break;
        }
    }

    public void pushMessage(String message) {
        adapter.add(message);
        adapter.notifyDataSetChanged();
    }

    public void pushImage(Bitmap image) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.PNG, 100, stream);
        byte[] byteArray = stream.toByteArray();
        Message message = new Message(MessageType.IMAGE, byteArray);
        CommunicationManager communicationManager = handlerAccessor.getWifiHandler().getCommunicationManager();
        Log.i(TAG, "Attempting to send image");
        communicationManager.write(SerializationUtils.serialize(message));
    }

    /**
     * ArrayAdapter to manage chat messages.
     */
    public class ChatMessageAdapter extends ArrayAdapter<String> {

        public ChatMessageAdapter(Context context, int textViewResourceId, List<String> items) {
            super(context, textViewResourceId, items);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = convertView;
            if (v == null) {
                LayoutInflater vi = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                v = vi.inflate(android.R.layout.simple_list_item_1, null);
            }
            String message = items.get(position);
            if (message != null && !message.isEmpty()) {
                TextView nameText = (TextView) v.findViewById(android.R.id.text1);
                if (nameText != null) {
                    nameText.setText(message);
                    if (message.startsWith("Me: ")) {
                        // My message
                        nameText.setGravity(Gravity.RIGHT);
                    } else {
                        // Buddy's message
                        nameText.setGravity(Gravity.LEFT);
                    }
                }
            }
            return v;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        toolbar.setTitle("Chat");
    }

    @Override
    public void onPause() {
        super.onPause();
        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(textMessageEditText.getWindowToken(), 0);
    }

    /**
     * This is called when the Fragment is opened and is attached to MainActivity
     */
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            handlerAccessor = ((WiFiDirectHandlerAccessor) getActivity());
        } catch (ClassCastException e) {
            throw new ClassCastException(getActivity().toString() + " must implement WiFiDirectHandlerAccessor");
        }
    }

    private void loadPhoto(ImageView imageView, int width, int height) {

        ImageView tempImageView = imageView;


        AlertDialog.Builder imageDialog = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Activity.LAYOUT_INFLATER_SERVICE);

        View layout = inflater.inflate(R.layout.custom_fullimage_dialog,
                (ViewGroup) getActivity().findViewById(R.id.layout_root));
        ImageView image = (ImageView) layout.findViewById(R.id.fullimage);
        image.setImageDrawable(tempImageView.getDrawable());
        imageDialog.setView(layout);
        imageDialog.setPositiveButton("Ok", new DialogInterface.OnClickListener(){

            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }

        });


        imageDialog.create();
        imageDialog.show();
    }
}
