/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.guanzon.cas.purchasing.status;

/**
 *
 * @author Arsiela 04-28-2025
 */
public class PurchaseOrderReturnStatus {
    public static final String OPEN = "0";
    public static final  String CONFIRMED = "1";
    public static final  String PAID = "2";  //Approved / Paid
    public static final  String CANCELLED = "3";
    public static final  String VOID = "4";
    public static final  String POSTED = "6"; 
    public static final  String RETURNED = "7";

    public static class Reverse  {
        public static final  String INCLUDE = "+"; 
        public static final  String EXCLUDE = "-"; 
    }
    
}
