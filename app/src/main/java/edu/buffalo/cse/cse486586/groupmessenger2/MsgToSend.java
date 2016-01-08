package edu.buffalo.cse.cse486586.groupmessenger2;

import java.io.Serializable;
import java.util.Comparator;

/**
 * Created by pragya on 3/12/15.
 */
public class MsgToSend implements Serializable, Comparable<MsgToSend>{
    private int msgId;
    private String msgContent;
    private float seqNo;
    private String sugProc;
    private String msgSenderId;
    private String msgStatus;

    public MsgToSend() {
        super();
    }


    public MsgToSend(int msgId,String msgContent,float seqNo,String sugProc,String msgSenderId,String msgStatus){
        msgId = msgId;
        msgContent = msgContent;
        seqNo = seqNo;
        sugProc = sugProc;
        msgSenderId = msgSenderId;
        msgStatus = msgStatus;
    }

    public int getMsgId() {
        return msgId;
    }


    public void setSeqNo(float seqNo) {
        this.seqNo = seqNo;
    }

    public float getSeqNo() {
        return seqNo;
    }

    public void setMsgId(int msgId) {
        this.msgId = msgId;
    }

    public String getMsgContent() {
        return msgContent;
    }

    public void setMsgContent(String msgContent) {
        this.msgContent = msgContent;
    }

    public String getSugProc() {
        return sugProc;
    }

    public void setSugProc(String sugProc) {
        this.sugProc = sugProc;
    }

    public String getMsgSenderId() {
        return msgSenderId;
    }

    public void setMsgSenderId(String msgSenderId) {
        this.msgSenderId = msgSenderId;
    }

    public String getMsgStatus() {
        return msgStatus;
    }

    public void setMsgStatus(String msgStatus) {
        this.msgStatus = msgStatus;
    }

    @Override
    public int compareTo(MsgToSend another) {

        if(this.seqNo>another.seqNo){
            return 1;
        }
        else if(this.seqNo<another.seqNo){
            return -1;
        }else{
            //SeqNo are equal
            if(this.msgStatus.equals("UNDELIVERABLE")){
            //Both are undelv
                if(another.msgStatus.equals("UNDELIVERABLE")){

                    if(Integer.parseInt(this.sugProc)>Integer.parseInt(another.sugProc)){
                        return 1;
                    }else{
                        return -1;
                    }

                }else{
                    // another=== delv
                    return -1;
                }

            }else{
                //this==delv,another==undelv
                if(another.msgStatus.equals("UNDELIVERABLE")){
                    return 1;
                }else{
                    //both are delv,then proc id
                    if(Integer.parseInt(this.sugProc)>Integer.parseInt(another.sugProc)){
                        return 1;
                    }else{
                        return -1;
                    }

                }
            }
        }
        //return 0;
    }
}