package edu.buffalo.cse.cse486586.groupmessenger2;

/**
 * Created by pragya on 3/14/15.
 */
public class Tracker implements Comparable{
       // int msgId;
        int proposedSeqNo;
        String proposingProcess;

        public Tracker(){
            super();
        }


        public Tracker(int proposedSeqNo,String proposingProcess){
         //   this.msgId = msgId;
            this.proposedSeqNo = proposedSeqNo;
            this.proposingProcess = proposingProcess;
        }

    /*public int getMsgId() {
            return msgId;
        }

        public void setMsgId(int msgId) {
            this.msgId = msgId;
        }
*/
        public int getProposedSeqNo() {
            return proposedSeqNo;
        }

        public void setProposedSeqNo(int proposedSeqNo) {
            this.proposedSeqNo = proposedSeqNo;
        }

        public String getProposingProcess() {
            return proposingProcess;
        }

        public void setProposingProcess(String proposingProcess) {
            this.proposingProcess = proposingProcess;
        }

        @Override
        public int compareTo(Object compareTr) {
            int compareSeq=((Tracker)compareTr).getProposedSeqNo();
            /* For Ascending order*/
            return this.proposedSeqNo-compareSeq;
        }
}

