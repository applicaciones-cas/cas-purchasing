package org.guanzon.cas.purchasing.status;

public class PurchaseOrderStatus {

    public static final String OPEN = "0";
    public static final String CONFIRMED = "1";
    public static final String PROCESSED = "2";
    public static final String CANCELLED = "3";
    public static final String VOID = "4";
    public static final String APPROVED = "5";
    public static final String POSTED = "6";
    public static final String RETURNED = "9";
    
    public static class Reverse  {
        public static final  String INCLUDE = "+"; 
        public static final  String EXCLUDE = "-"; 
    }
    
    public static class SourceCode  {
        public static final  String STOCKREQUEST = "InvR"; 
        public static final  String POQUOTATION = "POQt"; 
    }
}
