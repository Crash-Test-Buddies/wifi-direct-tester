package edu.rit.se.crashavoidance.views;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;

import java.util.HashMap;

import edu.rit.se.crashavoidance.R;
import edu.rit.se.wifibuddy.WifiDirectHandler;

/**
 * The Main Fragment of the application, which contains the Switches and Buttons to perform P2P tasks
 */
public class MainFragment extends Fragment {

    private WiFiDirectHandlerAccessor wifiDirectHandlerAccessor;
    private Switch toggleWifiSwitch;
    private Switch serviceRegistrationSwitch;
    private Switch noPromptServiceRegistrationSwitch;
    private Button discoverServicesButton;
    private AvailableServicesFragment availableServicesFragment;
    private MainActivity mainActivity;
    private Toolbar toolbar;
    private static final String TAG = WifiDirectHandler.TAG + "MainFragment";

    /**
     * Sets the layout for the UI, initializes the Buttons and Switches, and returns the View
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Sets the Layout for the UI
        final View view = inflater.inflate(R.layout.fragment_main, container, false);

        // Initialize Switches
        toggleWifiSwitch = (Switch) view.findViewById(R.id.toggleWifiSwitch);
        serviceRegistrationSwitch = (Switch) view.findViewById(R.id.serviceRegistrationSwitch);
        noPromptServiceRegistrationSwitch = (Switch) view.findViewById(R.id.noPromptServiceRegistrationSwitch);

        // Initialize Discover Services Button
        discoverServicesButton = (Button) view.findViewById(R.id.discoverServicesButton);

        updateToggles();

        // Set Toggle Listener for Wi-Fi Switch
        toggleWifiSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            /**
             * Enable or disable Wi-Fi when Switch is toggled
             */
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Log.i(TAG, "\nWi-Fi Switch Toggled");
                if(isChecked) {
                    // Enable Wi-Fi, enable all switches and buttons
                    getHandler().setWifiEnabled(true);
                } else {
                    // Disable Wi-Fi, disable all switches and buttons
                    getHandler().setWifiEnabled(false);
                }
            }
        });

        // Set Toggle Listener for Service Registration Switch
        serviceRegistrationSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            /**
             * Add or Remove a Local Service when Switch is toggled
             */
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Log.i(TAG, "\nService Registration Switch Toggled");
                if (isChecked) {
                    // Add local service
                    if (getHandler().getWifiP2pServiceInfo() == null) {
                        HashMap<String, String> record = new HashMap<>();
                        record.put("Name", getHandler().getThisDevice().deviceName);
                        record.put("Address", getHandler().getThisDevice().deviceAddress);
                        getHandler().addLocalService("Wi-Fi Buddy", record);
                        noPromptServiceRegistrationSwitch.setEnabled(false);
                    } else {
                        Log.w(TAG, "Service already added");
                    }
                } else {
                    // Remove local service
                    getHandler().removeService();
                    noPromptServiceRegistrationSwitch.setEnabled(true);
                }
            }
        });

//        // Set Toggle Listener for No-Prompt Service Registration Switch
//        noPromptServiceRegistrationSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
//            /**
//             * Add or Remove a No-Prompt Local Service when Switch is toggled
//             */
//            @Override
//            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
//                Log.i(TAG, "\nNo-Prompt Service Registration Switch Toggled");
//                if (isChecked) {
//                    // Add no-prompt local service
//                    ServiceData serviceData = new ServiceData(
//                            "Wi-Fi Direct Handler",         // Name
//                            4545,                           // Port
//                            new HashMap<String, String>(),  // Record
//                            ServiceType.PRESENCE_TCP        // Type
//                    );
//                    getHandler().startAddingNoPromptService(serviceData);
//                    serviceRegistrationSwitch.setEnabled(false);
//                } else {
//                    // Remove no-prompt local service
//                    getHandler().removeService();
//                    serviceRegistrationSwitch.setEnabled(true);
//                }
//            }
//        });

        // Set Click Listener for Discover Services Button
        discoverServicesButton.setOnClickListener(new View.OnClickListener() {
            /**
             * Show AvailableServicesFragment when Discover Services Button is clicked
             */
            @Override
            public void onClick(View v) {
                Log.i(TAG, "\nDiscover Services Button Pressed");
                if (availableServicesFragment == null) {
                    availableServicesFragment = new AvailableServicesFragment();
                }
                mainActivity.replaceFragment(availableServicesFragment);
            }
        });

        toolbar = (Toolbar) getActivity().findViewById(R.id.mainToolbar);

        return view;
    }

    /**
     * Sets the Main Activity instance
     */
    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mainActivity = (MainActivity) getActivity();
    }

    /**
     * Sets the WifiDirectHandler instance when MainFragment is attached to MainActivity
     */
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            wifiDirectHandlerAccessor = ((WiFiDirectHandlerAccessor) getActivity());
        } catch (ClassCastException e) {
            throw new ClassCastException(getActivity().toString() + " must implement WiFiDirectHandlerAccessor");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        toolbar.setTitle("Wi-Fi Direct Handler");
    }

    /**
     * Shortcut for accessing the wifi handler
     */
    private WifiDirectHandler getHandler() {
        return wifiDirectHandlerAccessor.getWifiHandler();
    }

    private void updateToggles() {
        // Set state of Switches and Buttons on load
        Log.i(TAG, "Updating toggle switches");
        if(getHandler().isWifiEnabled()) {
            toggleWifiSwitch.setChecked(true);
            serviceRegistrationSwitch.setEnabled(true);
            noPromptServiceRegistrationSwitch.setEnabled(true);
            discoverServicesButton.setEnabled(true);
        } else {
            toggleWifiSwitch.setChecked(false);
            serviceRegistrationSwitch.setEnabled(false);
            noPromptServiceRegistrationSwitch.setEnabled(false);
            discoverServicesButton.setEnabled(false);
        }
    }

    public void handleWifiStateChanged() {
        if (toggleWifiSwitch != null) {
            if (getHandler().isWifiEnabled()) {
                serviceRegistrationSwitch.setEnabled(true);
                discoverServicesButton.setEnabled(true);
            } else {
                serviceRegistrationSwitch.setChecked(false);
                serviceRegistrationSwitch.setEnabled(false);
                discoverServicesButton.setEnabled(false);
            }
        }
    }
}
