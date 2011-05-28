package petersutton.wakeonlan;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public final class WakeComputerActivity extends Activity
{
    @SuppressWarnings("unused")
    private static final String TAG =
            WakeComputerActivity.class.getSimpleName();
    private static final int PORT = 9;
    
    /*
     * YOU'RE MAC ADDRESS HERE
     */
    private static final byte[] MAC_ADDRESS =
            new byte[] { 0x1c, 0x6f, 0x65, 0x3f, (byte) 0x84, 0x0d };
   
    /*
     * YOUR BROADCAST IP HERE
     */
    private static final String BROADCAST_IP = "192.168.1.255";
    
    private static final int HEADER_LENGTH_BYTES = 6;
    private static final int MAC_ADDRESS_REPETITIONS = 16;
    private static final int MAGIC_PACKET_LENGTH_BYTES =
            HEADER_LENGTH_BYTES + MAC_ADDRESS.length * MAC_ADDRESS_REPETITIONS;
    
    private static final InetAddress BROADCAST_ADDRESS;
    private static final DatagramPacket MAGIC_PACKET;

    static
    {
        /*
         * Build magic packet payload.
         */

        final byte[] payload = new byte[MAGIC_PACKET_LENGTH_BYTES];

        // Add header.
        for (int header = 0; header < HEADER_LENGTH_BYTES; header++)
        {
            payload[header] = (byte) 0xff;
        } // for

        // Add MAC address 16 times.
        for (int rep = 0; rep < MAC_ADDRESS_REPETITIONS; rep++)
        {
            System.arraycopy(MAC_ADDRESS, 0 /* srcPos */, payload,
                    rep * MAC_ADDRESS.length, MAC_ADDRESS.length);
        } // for

        /*
         * Build broadcast address.
         */

        try
        {
            BROADCAST_ADDRESS = InetAddress.getByName(BROADCAST_IP);
        } // try
        catch (final UnknownHostException e)
        {
            throw new AssertionError(e);
        } // catch

        MAGIC_PACKET = new DatagramPacket(
                payload,
                payload.length,
                BROADCAST_ADDRESS,
                PORT);

    } // static

    private boolean mOnRetainNonConfigurationInstanceCalled = false;
    private Button mStartComputerButton = null;
    private Toast mToast = null;
    private WakeComputerTask mWakeComputerTask = null;

    private final class WakeComputerTask
            extends AsyncTask<Void, Void, Boolean>
    {
        @Override
        protected void onPreExecute()
        {
            mStartComputerButton.setClickable(false);
        } // onPreExecute

        @Override
        protected Boolean doInBackground(final Void... noParams)
        {
            DatagramSocket socket = null;
            try
            {
                socket = new DatagramSocket();
                socket.send(MAGIC_PACKET);
            } // try
            catch (final IOException e)
            {
                return false;
            } // catch
            finally
            {
                if (socket != null)
                {
                    socket.close();
                } // if
            } // finally

            return true;
        } // doInBackground

        @Override
        protected void onCancelled()
        {
            onStop();
        } // onCancelled

        @Override
        protected void onPostExecute(final Boolean successful)
        {
            mWakeComputerTask = null;
            final String message;
            if (successful)
            {
                message = "Sent wake request";
            } // if
            else
            {
                message = "Couldn't send wake request";
            } // else
            if (mToast != null)
            {
                mToast.cancel();
            } // if
            mToast = Toast.makeText(WakeComputerActivity.this, message,
                    Toast.LENGTH_SHORT);
            mToast.show();
            onStop();
        } // onPostExecute

        private void onStop()
        {
            mWakeComputerTask = null;
            mStartComputerButton.setClickable(true);
        } // onStop

    } // WakeComputerTask

    public WakeComputerActivity()
    {
        super();
    } // WakeComputerActivity

    @Override
    public void onCreate(final Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.wake_computer);
        mToast = (Toast) getLastNonConfigurationInstance();
        mStartComputerButton =
                (Button) findViewById(R.id.start_computer_button);
    } // onCreate

    @Override
    protected void onPause()
    {
        if (mWakeComputerTask != null)
        {
            mWakeComputerTask.cancel(true);
        } // if
        super.onPause();
    } // onPause

    @Override
    public Object onRetainNonConfigurationInstance()
    {
        mOnRetainNonConfigurationInstanceCalled = true;
        return mToast;
    } // onRetainNonConfigurationInstance

    @Override
    protected void onDestroy()
    {
        if (!mOnRetainNonConfigurationInstanceCalled && mToast != null)
        {
            mToast.cancel();
        } // if
        super.onDestroy();
    } // onDestroy

    public void onStartComputerClick(final View startComputerButton)
    {
        if (mWakeComputerTask == null)
        {
            mWakeComputerTask = (WakeComputerTask) new WakeComputerTask()
                    .execute((Void) null);
        } // if
    } // onStartComputerClick

} // WakeComputerActivity
