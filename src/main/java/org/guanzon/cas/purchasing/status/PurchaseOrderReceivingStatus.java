/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.guanzon.cas.purchasing.status;

/**
 *
 * @author Arsiela 03-12-2025
 */
public class PurchaseOrderReceivingStatus {
    public static final String OPEN = "0";
    public static final  String CONFIRMED = "1";
    public static final  String POSTED = "2"; // approve > posted 
    public static final  String CANCELLED = "3";
    public static final  String VOID = "4";
    public static final  String PAID = "5"; //posted > paid
    public static final  String RETURNED = "6";
    
    public static final  String GLCODE = "ACCOUNTS_PAYABLE";
}
