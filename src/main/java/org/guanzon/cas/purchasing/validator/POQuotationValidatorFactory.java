package org.guanzon.cas.purchasing.validator;

import ph.com.guanzongroup.cas.iface.GValidator;

/**
 *
 * @author Arsiela 
 */
public class POQuotationValidatorFactory {
    public static GValidator make(String industryId){
        switch (industryId) {
            case "01": //Mobile Phone
                return new POQuotation_MP();
            case "02": //Motorcycle
                return new POQuotation_MC();
            case "03": //Vehicle
                return new POQuotation_Vehicle();
            case "04": //Monarch 
                return new POQuotation_Monarch();
            case "05": //Los Pedritos
            case "09": //General
                return new POQuotation_LP();
            case "07": //Appliances
                return new POQuotation_Appliances();
            default:
                return null;
        }
    }
    
}
