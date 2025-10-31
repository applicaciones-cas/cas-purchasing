package org.guanzon.cas.purchasing.validator;

import org.guanzon.appdriver.iface.GValidator;

public class POCancellationValidatorFactory {

    public static GValidator make(String industryId) {
        switch (industryId) {
            case "01": //Mobile Phone
                return new POCancellation_MP();
            case "02": //Motorcycle
                return new POCancellation_MC();
            case "03": //Vehicle
                return new POCancellation_Car();
            case "04": //Monarch
                return new POCancellation_Monarch();
            case "05": //Los Pedritos
                return new POCancellation_LP();
            case "06": //General
                return new POCancellation_General();
            case "07": //Appliances
                return new POCancellation_Appliance();

            case "": //Main Office
                return new POCancellation_General();
            default:
                return null;
        }
    }

}
