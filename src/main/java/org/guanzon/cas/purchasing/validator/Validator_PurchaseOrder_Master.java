package org.guanzon.cas.purchasing.validator;
import java.util.ArrayList;
import org.guanzon.appdriver.agent.ShowMessageFX;
import org.guanzon.appdriver.base.GRider;
import org.guanzon.cas.purchasing.model.Model_PO_Master;


import org.guanzon.cas.validators.ValidatorInterface;

/**
 *
 * @author Michael Cuison
 */
public class Validator_PurchaseOrder_Master implements ValidatorInterface {
    GRider poGRider;
    String psMessage;
    
    Model_PO_Master poEntity;
    
    public Validator_PurchaseOrder_Master(Object foValue){
        poEntity = (Model_PO_Master) foValue;
    }
    

    @Override
    public void setGRider(GRider foValue) {
        poGRider = foValue;
    }
    @Override
    public boolean isEntryOkay() {
        if (poEntity.getTransactionNo().isEmpty()){
            psMessage = "Transaction Number is not set.";
            return false;
        }
        
        if (poEntity.getDestination().isEmpty()){
            psMessage = "Destination is not set.";
            return false;
        }
        
        if (!"0".equals(poEntity.getTransactionStatus())) {
            psMessage = "Transaction Status is not Open";
            return false;
        }
        if (poEntity.getSupplier().isEmpty()) {
            psMessage = "Supplier is not set";
            return false;
        }
        if (poEntity.getMobileNo().isEmpty() ) {
            psMessage = "Mobile Number is not set";
            return false;
        }else if(poEntity.getMobileNo().length()!=11){
            psMessage = "Mobile Number is not in 11 digits";
            return false;
        }else{
        }

        if (poEntity.getReferenceNo().isEmpty()) {
            psMessage = "Reference Number is not set";
            return false;
        }
        if (poEntity.getTermName().isEmpty()) {
            psMessage = "Term is not set";
            return false;
        }



        return true;
    }
    

    @Override
    public String getMessage() {
        return psMessage;
    }

}

