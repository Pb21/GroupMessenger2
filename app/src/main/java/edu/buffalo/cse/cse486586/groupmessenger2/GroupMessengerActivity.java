package edu.buffalo.cse.cse486586.groupmessenger2;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Message;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Queue;

import edu.buffalo.cse.cse486586.groupmessenger2.MsgToSend;

/**
 * GroupMessengerActivity is the main Activity for the assignment.
 * 
 * @author stevko
 *
 */
public class GroupMessengerActivity extends Activity {


    static final String[] PORTS={"11108","11112","11116","11120","11124"};
    static final int SERVER_PORT = 10000;

    private static final int TEST_CNT = 50;
    private static final String KEY_FIELD = "key";
    private static final String VALUE_FIELD = "value";

    private static int counter = 0;
    private static int msgId =0;
    static final String UNDELV = "UNDELIVERABLE";
    static final String DELV = "DELIVERABLE";
    private static String ownPortno;
    //For each avd, create a hold back queue
    PriorityQueue<MsgToSend> holdBackQueue;

    //For each sender keeping track of its own msgs


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);

        holdBackQueue = new  PriorityQueue<MsgToSend>();

        /*
         * TODO: Use the TextView to display your messages. Though there is no grading component
         * on how you display the messages, if you implement it, it'll make your debugging easier.
         */
        TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());
        
        /*
         * Registers OnPTestClickListener for "button1" in the layout, which is the "PTest" button.
         * OnPTestClickListener demonstrates how to access a ContentProvider.
         */
        findViewById(R.id.button1).setOnClickListener(
                new OnPTestClickListener(tv, getContentResolver()));
        
        /*
         * TODO: You need to register and implement an OnClickListener for the "Send" button.
         * In your implementation you need to get the message from the input box (EditText)
         * and send it to other AVDs.
         */

        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        final String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        final String myPort = String.valueOf((Integer.parseInt(portStr) * 2));
        ownPortno=portStr;
        try {
            /*
             * Create a server socket as well as a thread (AsyncTask) that listens on the server
             * port.
             *
             * AsyncTask is a simplified thread construct that Android provides. Please make sure
             * you know how it works by reading
             * http://developer.android.com/reference/android/os/AsyncTask.html
             */
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket, portStr);
        } catch (IOException e) {
            /*
             * Log is a good way to debug your code. LogCat prints out all the messages that
             * Log class writes.
             *
             * Please read http://developer.android.com/tools/debugging/debugging-projects.html
             * and http://developer.android.com/tools/debugging/debugging-log.html
             * for more information on debugging.
             */
            Log.e("Error", "Can't create a ServerSocket");
            return;
        }


        findViewById(R.id.button4).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                final TextView editText = (TextView) findViewById(R.id.editText1);
                String msg = editText.getText().toString() + "\n";
                editText.setText(""); // This is one way to reset the input box.
                new ClientTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, msg, myPort);

            }
        });



    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }


    private class ServerTask extends AsyncTask<Object, String, Void> {

        @Override
        protected Void doInBackground(Object... objects) {
            ServerSocket serverSocket = (ServerSocket) objects[0];
            String portStr = (String) objects[1];
            MsgToSend msgRecvd;
            float seqNo=0;
            try {
                while(true) {
                    Socket sSocket = serverSocket.accept();
                   /* BufferedReader incoming = new BufferedReader(
                            new InputStreamReader(sSocket.getInputStream()));
*/
                    ObjectInputStream ois =
                            new ObjectInputStream(sSocket.getInputStream());

                    msgRecvd = (MsgToSend) ois.readObject();
                    //On receiving the message first time, increment local sequence id, send it back to sender, and enqueue in avdspecific holdbackqueue
                    if(msgRecvd.getSugProc()!=null) {
                        seqNo = seqNo + 1;
                        Log.d(ServerTask.class.getSimpleName() + "portStr", portStr);
                        // public MsgToSend(int msgId,String msgContent,float seqNo,String sugProc,String msgSenderId,String msgStatus){
                        MsgToSend m1 = new MsgToSend(msgRecvd.getMsgId(), msgRecvd.getMsgContent(), seqNo, portStr, msgRecvd.getMsgSenderId(), UNDELV);
                        holdBackQueue.add(m1);

                        ObjectOutputStream outS = new ObjectOutputStream(sSocket.getOutputStream());
                        outS.writeObject(m1);
                    }else{
                        //Receiving the message with the final seqNo
                        float finalSeqNo=msgRecvd.getSeqNo();
                        if(finalSeqNo>seqNo){
                            //update own seqNo then
                            seqNo=finalSeqNo;
                            //trying to find own message with the same msg id and update the seqNo there
                            for(MsgToSend mm:holdBackQueue){
                                if(mm.getMsgId() ==msgRecvd.getMsgId()){
                                    mm.setSeqNo(finalSeqNo);
                                    mm.setMsgStatus("DELIVERABLE");
                                    break;
                                }
                            }
                            insertInProvider();
                        }
                //call insertInProvider() to check the head of the queue and pop all the deliverable messages and save it in database
                        insertInProvider();

                    }

                    //Sender multicasting msg, seqNo is redundant,now,so kept 0 & sugProc =null & msgStatus=null


                }
            }catch (ClassNotFoundException e) {
                e.printStackTrace();
            }catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        private void insertInProvider(){

            Iterator tr = holdBackQueue.iterator();
            while(tr.hasNext()){
                MsgToSend m=holdBackQueue.peek();
                if(m.getMsgStatus().equalsIgnoreCase(DELV)){
                    m=holdBackQueue.poll();
                    saveMessages(counter++,m.getMsgContent());
                }
            }
        }




        protected void onProgressUpdate(String...strings) {
            /*
             * The following code displays what is received in doInBackground().
             */
            String strReceived = strings[0].trim();
            Log.d("Inside ProgressUpdate", "strReceived" + strReceived);
            TextView textView1 = (TextView) findViewById(R.id.textView1);
            textView1.append(strReceived + "\t\n");
            return;
        }
    }
    /***
     * ClientTask is an AsyncTask that should send a string over the network.
     * It is created by ClientTask.executeOnExecutor() call whenever OnKeyListener.onKey() detects
     * an enter key press event.
     *
     * @author stevko
     *
     */
    private class ClientTask extends AsyncTask<String, Void, Void> {
        @Override
        protected Void doInBackground(String... msgs) {
            try {
                String msgToSend = msgs[0];
                String portStr = ownPortno;
                msgId++;
                String senderId;
                float proposedSequence;
                float sequenceTracker[] = new float[5];
                int[] senderIdArray={5554,5556,5558,5560,5562};
                Socket[] socket=new Socket[5];
                for (int i=0;i<5;i++){

                    socket[i] = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(PORTS[i]));


                    //public MsgToSend(int msgId,String msgContent,int seqNo,String sugProc,String msgSenderId,String msgStatus)

                    //Sender multicasting msg, seqNo is redundant,now,so kept 0 & sugProc =null & msgStatus=null
                    MsgToSend msg1 = new MsgToSend(msgId,msgToSend,0,null,portStr,null);
                    ObjectOutputStream out=new ObjectOutputStream(socket[i].getOutputStream());
                    out.writeObject(msg1);
                 //   out.close();

                    //Client is receiving all the proposed Seq nos
                    ObjectInputStream ins = new ObjectInputStream(socket[i].getInputStream());
                    MsgToSend msgReceived = (MsgToSend)ins.readObject();

                    senderId = msgReceived.getMsgSenderId();
                    proposedSequence=msgReceived.getSeqNo();
                    switch(senderId){
                        case "5554":
                            sequenceTracker[0]= proposedSequence;
                            break;
                        case "5556":
                            sequenceTracker[1]= proposedSequence;
                            break;
                        case "5558":
                            sequenceTracker[2]= proposedSequence;
                            break;
                        case "5560":
                            sequenceTracker[3]= proposedSequence;
                            break;
                        case "5562":
                            sequenceTracker[4]= proposedSequence;
                            break;

                    }
                    float max=-10;
                    int maxIndex=-1,count=0;
                    int procId = 0;

                    for(int k=0;k<5;k++){
                        if(sequenceTracker[k] == max & maxIndex!=-1){
                            count++;
                            //If both proc offer same seq no, then choose process with lowerId
                           if(senderIdArray[k]< senderIdArray[maxIndex]){
                                procId = senderIdArray[k];
                                maxIndex=k;
                           }else {
                               procId = senderIdArray[maxIndex];
                           }
                        }else if (sequenceTracker[k]> max)
                        {
                           // maxSequence[count]=k;
                            maxIndex=k;
                            max=sequenceTracker[k];
                            procId=senderIdArray[maxIndex];
                        }
                    }
                    float finalSequence=0;

                        //my final sequence with process Id
                    String finalSeq = String.valueOf(max) + "." + String.valueOf(procId);
                    Log.d("finalseq",finalSeq);
                    finalSequence=Float.parseFloat(finalSeq);

//    public MsgToSend(int msgId,String msgContent,int seqNo,String sugProc,String msgSenderId,String msgStatus){
                    MsgToSend msgFinalMulticast = new MsgToSend(msgId,msgToSend,finalSequence,Integer.toString(procId),portStr,null);
                    out.writeObject(msgFinalMulticast);
                    //

                 }
/*
                holdBackQueue = new  PriorityQueue<MsgToSend>();
                senderTrackerList = new ArrayList<Tracker>();



                //Once the sender receives the proposed seqNo, it adds to tracker. if tracker is full for msg, it chooses finl seq
                //Its the sender receiving the msg,with proposed seq no, since proposed seq no.> 0
                if (portStr.equals(msgRecvd.getMsgSenderId()) & msgRecvd.getSeqNo()!=0)
                {   List<Tracker> senderTrackerList = new ArrayList<Tracker>();

                    Tracker tr = new Tracker(msgRecvd.getSeqNo(),msgRecvd.getSugProc());
                    if(senderQueue.containsKey(msgRecvd.getMsgSenderId())){
                        senderTrackerList = senderQueue.get(msgRecvd.getMsgSenderId());

                    }

                    senderTrackerList.add(tr);

                    Log.d("senderTrackerList size",portStr);
                    //check the size of the list
                    if(senderTrackerList.size()==5){
                        //sort the list in terms of sequence number
                        Collections.sort(senderTrackerList);
                        //Final seq no is the max
                        int finalSeqNo=senderTrackerList.get(0).getProposedSeqNo();
                        //Log.d("senderTrackerList",senderTrackerList.get(1).getProposedSeqNo());

                    }

                    senderQueue.put(msgRecvd.getMsgId(),senderTrackerList);


                }
*/


            } catch (UnknownHostException e) {
                Log.e("Error", "ClientTask UnknownHostException");
            } catch (IOException e) {
                Log.e("Error", "ClientTask socket IOException"+e.getMessage());
            }catch(ClassNotFoundException e){
                Log.e("Error", "ClassNotFoundException"+e.getMessage());
            }

            return null;
        }
    }


    /*private void sortHoldBackQueue(){

        //public MsgToSend(int msgId,String msgContent,int seqNo,String sugProc,String msgSenderId,String msgStatus)
         PriorityQueue<MsgToSend> holdBackQueue;
        //seqNo,Undelv,procId
        //1==this>parameter
        if(this.seqNo.compareTo(a.seqNo)){

        }

    }*/



    /**
     * buildUri() demonstrates how to build a URI for a ContentProvider.
     *
     * @param scheme
     * @param authority
     * @return the URI
     */
    private Uri buildUri(String scheme, String authority) {
        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.authority(authority);
        uriBuilder.scheme(scheme);
        return uriBuilder.build();
    }


    private boolean saveMessages(int k,String values) {
        final Uri mUri= buildUri("content", "edu.buffalo.cse.cse486586.groupmessenger2.provider");
        final ContentResolver smContentResolver =getContentResolver();
        ContentValues cv = new ContentValues();
        cv = new ContentValues();
        cv.put(KEY_FIELD, Integer.toString(k));
        cv.put(VALUE_FIELD, values);

        try {
            smContentResolver.insert(mUri, cv);

        } catch (Exception e) {
            Log.e("Error", e.toString());
            return false;
        }
        return true;
    }
}


