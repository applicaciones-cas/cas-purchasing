/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.guanzon.cas.purchasing.validator;

import org.guanzon.appdriver.iface.GValidator;

/**
 *
 * @author Arsiela 03-12-2025
 */
public class PurchaseOrderReceivingValidatorFactory {
    public static GValidator make(String industryId){
        switch (industryId) {
            case "01": //Mobile Phone
                return new PurchaseOrderReceiving_MP();
            case "02": //Motorcycle
                return new PurchaseOrderReceiving_MC();
            case "03": //Vehicle
                return new PurchaseOrderReceiving_Vehicle();
            case "04": //Hospitality
                return new PurchaseOrderReceiving_Hospitality();
            case "05": //Los Pedritos
                return new PurchaseOrderReceiving_LP();
            case "06": //Main Office
                return new PurchaseOrderReceiving_General();
            default:
                return null;
        }
    }
    
}
