/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.guanzon.cas.purchasing.validator;

import org.guanzon.appdriver.iface.GValidator;

/**
 *
 * @author Arsiela 04-28-2025
 */
public class PurchaseOrderReturnValidatorFactory {
    public static GValidator make(String industryId){
        switch (industryId) {
            case "01": //Mobile Phone
                return new PurchaseOrderReturn_General();
            case "02": //Motorcycle
                return new PurchaseOrderReturn_General();
            case "03": //Vehicle
                return new PurchaseOrderReturn_General();
            case "04": //Hospitality
                return new PurchaseOrderReturn_General();
            case "05": //Los Pedritos
                return new PurchaseOrderReturn_General();
            case "06": //Main Office
                return new PurchaseOrderReturn_General();
            case "07": //Appliances
                return new PurchaseOrderReturn_General();
            default:
                return null;
        }
    }
    
}
