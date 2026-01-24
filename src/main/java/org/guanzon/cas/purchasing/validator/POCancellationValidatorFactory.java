package org.guanzon.cas.purchasing.validator;

import org.guanzon.appdriver.iface.GValidator;

public class POCancellationValidatorFactory {

    public static GValidator make(String industryId) {
        switch (industryId) {
            case "00": //Mobile Phone
                return new POCancellation_MP();
            case "01": //Motorcycle
                return new POCancellation_MC();
            case "02": //Vehicle
            case "05":
            case "06":
                return new POCancellation_Car();
            case "03": //Monarch
                return new POCancellation_Monarch();
            case "04": //Los Pedritos
                return new POCancellation_LP();
            case "07": //Guanzon Services Office
            case "08": //Main Office
            case "09": //General Purchases
            case "10": //Engineering
                return new POCancellation_General();
            case "11": //Appliances
                return new POCancellation_Appliance();

//            case "": //Main Office
//                return new CheckDeposit_General();
            default:
                return null;
        }
        
    }

}
