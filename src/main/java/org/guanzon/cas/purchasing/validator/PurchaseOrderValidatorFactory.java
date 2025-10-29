package org.guanzon.cas.purchasing.validator;

import ph.com.guanzongroup.cas.iface.GValidator;

public class PurchaseOrderValidatorFactory {

    public static GValidator make(String industryId) {
        switch (industryId) {
            case "01": //Mobile Phone
                return new PurchaseOrder_MP();
            case "02": //Motorcycle
                return new PurchaseOrder_MC();
            case "03": //Vehicle
                return new PurchaseOrder_Vehicle();
            case "04": //Hospitality
                return new PurchaseOrder_Hospitality();
            case "05": //Los Pedritos
                return new PurchaseOrder_LP();
            default:
                return new PurchaseOrder_General();
        }
    }
}
