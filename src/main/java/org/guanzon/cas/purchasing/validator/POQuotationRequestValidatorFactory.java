package org.guanzon.cas.purchasing.validator;

import ph.com.guanzongroup.cas.iface.GValidator;

/**
 *
 * @author Arsiela 
 */
public class POQuotationRequestValidatorFactory {
    public static GValidator make(String industryId){
        switch (industryId) {
            case "01": //Mobile Phone
                return new POQuotationRequest_MP();
            case "02": //Motorcycle
                return new POQuotationRequest_MC();
            case "03": //Vehicle
                return new POQuotationRequest_Vehicle();
            case "04": //Monarch 
                return new POQuotationRequest_Monarch();
            case "05": //Los Pedritos
            case "09": //General
                return new POQuotationRequest_LP();
            case "07": //Appliances
                return new POQuotationRequest_Appliances();
            default:
                return null;
        }
    }
    
}
