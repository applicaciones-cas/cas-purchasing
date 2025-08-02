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
    
    //Category
    public static final String MOBILEPHONE = "0001";   //Cellphone    
    public static final String APPLIANCES  = "0002";   //Appliances   
    public static final String MOTORCYCLE  = "0003";   //Motorcycle   
    public static final String SPMC        = "0004";   //Motorcycle SP
    public static final String CAR         = "0005";   //CAR          
    public static final String SPCAR       = "0006";   //CAR SP       
    public static final String GENERAL     = "0007";   //General      
    public static final String FOOD        = "0008";   //Food         
    public static final String HOSPITALITY = "0009";   //Hospitality  
    
}
