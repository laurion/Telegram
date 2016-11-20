package org.telegram.hype;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.hypelabs.hype.Error;
import com.hypelabs.hype.Hype;
import com.hypelabs.hype.Instance;
import com.hypelabs.hype.Message;
import com.hypelabs.hype.MessageInfo;
import com.hypelabs.hype.MessageObserver;
import com.hypelabs.hype.NetworkObserver;
import com.hypelabs.hype.StateObserver;

import org.json.JSONException;
import org.json.JSONObject;
import org.telegram.messenger.ApplicationLoader;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

/**
 * Created by Intersect on 11/19/2016.
 */
public class HypeServices extends Service implements StateObserver, NetworkObserver, MessageObserver {


    private static final String TAG = "aa";

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.v("aa","start service");
        requestHypeToStart();
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    protected void requestHypeToStart() {
        Log.v("aa","ready to start hype");
        // The application context is used to query the user for permissions, such as using
        // the Bluetooth adapter or enabling Wi-Fi. The context must be set before anything
        // else is attempted, otherwise resulting in an exception being thrown.
        Hype.getInstance().setContext(ApplicationLoader.getApp().getApplicationContext());

        // Adding itself as an Hype state observer makes sure that the application gets
        // notifications for lifecycle events being triggered by the Hype framework. These
        // events include starting and stopping, as well as some error handling.
        Hype.getInstance().addStateObserver(this);

        // Network observer notifications include other devices entering and leaving the
        // network. When a device is found all observers get a onInstanceFound notification,
        // and when they leave onInstanceLost is triggered instead.
        Hype.getInstance().addNetworkObserver(this);

        // I/O notifications indicate when messages are sent, delivered, or fail to be sent.
        // Notice that a message being sent does not imply that it has been delivered, only
        // that it has been queued for output. This is especially important when using mesh
        // networking, as the destination device might not be connect in a direct link.
        Hype.getInstance().addMessageObserver(this);

        // Requesting Hype to start is equivalent to requesting the device to publish
        // itself on the network and start browsing for other devices in proximity. If
        // everything goes well, the onStart(Hype) observer method gets called, indicating
        // that the device is actively participating on the network. The 00000000 realm is
        // reserved for test apps, so it's not recommended that apps are shipped with it.
        // For generating a realm go to https://hypelabs.io, login, access the dashboard
        // under the Apps section and click "Create New App". The resulting app should
        // display a realm number. Copy and paste that here.
        Hype.getInstance().start(new HashMap<String, Object>() {{

            put(Hype.OptionRealmKey, "00000000");
        }});
    }

    protected void requestHypeToStop() {

        // Stopping the Hype framework does not break existing connections. When the framework
        // stops, all active connections are kept and found devices are not lost. Stopping means
        // that no new devices will be found, as the framework won't be looking for them anymore
        // and that this device is not advertising itself either.
        Hype.getInstance().stop();
    }

    @Override
    public void onStart(Hype hype) {

        // At this point, the device is actively participating on the network. Other devices
        // (instances) can be found at any time and the domestic (this) device can be found
        // by others. When that happens, the two devices should be ready to communicate.
        Log.v("aa", "Hype started!" );
    }

    @Override
    public void onStop(Hype hype, Error error) {

        String description = "";

        if (error != null) {

            // The error parameter will usually be null if the framework stopped because
            // it was requested to stop. This might not always happen, as even if requested
            // to stop the framework might do so with an error.
            description = String.format("[%s]", error.getDescription());
        }

        // The framework has stopped working for some reason. If it was asked to do so (by
        // calling stop) the error parameter is null. If, on the other hand, it was forced
        // by some external means, the error parameter indicates the cause. Common causes
        // include the user turning the Bluetooth and/or Wi-Fi adapters off. When the later
        // happens, you shouldn't attempt to start the Hype services again. Instead, the
        // framework triggers a onReady delegate method call if recovery from the failure
        // becomes possible.
        Log.i(TAG, String.format("Hype stopped [%s]", description));
    }

    @Override
    public void onFailedStarting(Hype hype, Error error) {

        // Hype couldn't start its services. Usually this means that all adapters (Wi-Fi
        // and Bluetooth) are not on, and as such the device is incapable of participating
        // on the network. The error parameter indicates the cause for the failure. Attempting
        // to restart the services is futile at this point. Instead, the implementation should
        // wait for the framework to trigger a onReady notification, indicating that recovery
        // is possible, and start the services then.
        Log.i(TAG, String.format("Hype failed starting [%s]", error.getDescription()));
    }

    @Override
    public void onReady(Hype hype) {

        // This Hype delegate event indicates that the framework believes that it's capable
        // of recovering from a previous start failure. This event is only triggered once.
        // It's not guaranteed that starting the services will result in success, but it's
        // known to be highly likely. If the services are not needed at this point it's
        // possible to delay the execution for later, but it's not guaranteed that the
        // recovery conditions will still hold by then.
        requestHypeToStart();
    }

    @Override
    public void onStateChange(Hype hype) {

        // State change updates are triggered before their corresponding, specific, observer
        // call. For instance, when Hype starts, it transits to the State.Running state,
        // triggering a call to this method, and only then is onStart(Hype) called. Every
        // such event has a corresponding observer method, so state change notifications
        // are mostly for convenience. This method is often not used.
    }

    @Override
    public void onInstanceFound(Hype hype, Instance instance) {

        // Hype instances that are participating on the network are identified by a full
        // UUID, composed by the vendor's realm followed by a unique identifier generated
        // for each instance.
        Log.v("aa", String.format("Found instance: %s", instance.getStringIdentifier()));
        sendUserToInstanceFound(instance);
       // WDatabase.addInstaceToDB(instance);TODO
        //ChatApplication.getApp().sendNotification("zebra",  "New Users nearby");
        //EventBus.getDefault().post(new RefreshDiscoveryList());TODO
        // Instances should be strongly kept by some data structure. Their identifiers
        // are useful for keeping track of which instances are ready to communicate

        // Notify the contact activity to refresh the UI
    }

    @Override
    public void onInstanceLost(Hype hype, Instance instance, Error error) {

        // An instance being lost means that communicating with it is no longer possible.
        // This usually happens by the link being broken. This can happen if the connection
        // times out or the device goes out of range. Another possibility is the user turning
        // the adapters off, in which case not only are all instances lost but the framework
        // also stops with an error.
        Log.i(TAG, String.format("Lost instance: %s [%s]", instance.getStringIdentifier(), error.getDescription()));
        //WDatabase.removeInstanceObjectFromDb(instance.getStringIdentifier());TODO
        // Cleaning up is always a good idea. It's not possible to communicate with instances
        // that were previously lost.

        // Notify the contact activity to refresh the UI
        // ContactActivity contactActivity = ContactActivity.getDefaultInstance();

        /// if (contactActivity != null) {
        //  contactActivity.notifyContactsChanged();
        // }
    }

    @Override
    public void onMessageReceived(Hype hype, Message message, Instance instance) {
        Log.i(TAG, String.format("Got a message from: %s", instance.getStringIdentifier()));
        String messagetTxt =  new String(message.getData(), StandardCharsets.UTF_8);
    /*    if(messagetTxt.contains("xxxmosxxx")){
            WDatabase.saveInstanceInfoToDB(instance.getStringIdentifier(), messagetTxt);
        } else {
            UserMessage newMessage = AppUtils.convertHypeMessageToUserMessage(messagetTxt, instance.getStringIdentifier());
            EventBus.getDefault().post(new MessageReceivedEvent(newMessage));
            //    sendNotification("zebra",  "PINGGGGG");
        }*/ //TODO

        //Add unseen messages to Database
        // Update the UI for the ContactActivity as well
        //   ContactActivity contactActivity = ContactActivity.getDefaultInstance();

        // if (contactActivity != null) {
        //  contactActivity.notifyAddedMessage();
        //}
    }

    @Override
    public void onMessageFailedSending(Hype hype, MessageInfo messageInfo, Instance instance, Error error) {

        // Sending messages can fail for a lot of reasons, such as the adapters
        // (Bluetooth and Wi-Fi) being turned off by the user while the process
        // of sending the data is still ongoing. The error parameter describes
        // the cause for the failure.
        Log.i(TAG, String.format("Failed to send message: %d [%s]", messageInfo.getIdentifier(), error.getDescription()));
    }

    @Override
    public void onMessageSent(Hype hype, MessageInfo messageInfo, Instance instance, float progress, boolean done) {

        // A message being "sent" indicates that it has been written to the output
        // streams. However, the content could still be buffered for output, so it
        // has not necessarily left the device. This is useful to indicate when a
        // message is being processed, but it does not indicate delivery by the
        // destination device.
        Log.i(TAG, String.format("Message being sent: %f", progress));
    }

    @Override
    public void onMessageDelivered(Hype hype, MessageInfo messageInfo, Instance instance, float progress, boolean done) {

        // A message being delivered indicates that the destination device has
        // acknowledge reception. If the "done" argument is true, then the message
        // has been fully delivered and the content is available on the destination
        // device. This method is useful for implementing progress bars.
        Log.i(TAG, String.format("Message being delivered: %f", progress));
    }

    public void sendUserToInstanceFound(Instance instance){
      /*  JSONObject object = new JSONObject();
        WUser currentUser = WDatabase.getCurrentWUser();
        try {
            object.put("type", "xxxmosxxx");
            object.put("nickname", currentUser.nickname);
            object.put("user_id", currentUser.user_id);
            object.put("profile_url", currentUser.profile_url);
        } catch (JSONException e){
            e.printStackTrace();
        }

        try {
            sendMessage(object.toString(), instance);
        }catch (UnsupportedEncodingException e){
            e.printStackTrace();
        }*/
    }

    protected Message sendMessage(String text, Instance instance) throws UnsupportedEncodingException {

        // When sending content there must be some sort of protocol that both parties
        // understand. In this case, we simply send the text encoded in UTF-8. The data
        // must be decoded when received, using the same encoding.
        byte[] data = text.getBytes("UTF-8");

        // Sends the data and returns the message that has been generated for it. Messages have
        // identifiers that are useful for keeping track of the message's deliverability state.
        // In order to track message delivery set the last parameter to true. Notice that this
        // is not recommend, as it incurs extra overhead on the network. Use this feature only
        // if progress tracking is really necessary.
        return Hype.getInstance().send(data, instance, false);
    }
}