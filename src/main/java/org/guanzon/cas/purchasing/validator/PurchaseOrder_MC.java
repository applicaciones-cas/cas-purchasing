package org.guanzon.cas.purchasing.validator;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.guanzon.appdriver.agent.ShowDialogFX;
import org.guanzon.appdriver.base.GRiderCAS;
import org.guanzon.appdriver.constant.EditMode;
import org.guanzon.appdriver.iface.GValidator;
import org.guanzon.cas.purchasing.model.Model_PO_Detail;
import org.guanzon.cas.purchasing.model.Model_PO_Master;
import org.guanzon.cas.purchasing.status.PurchaseOrderStatus;
import org.json.simple.JSONObject;

public class PurchaseOrder_MC implements GValidator {

    GRiderCAS poGrider;
    String psTranStat;
    JSONObject poJSON;

    Model_PO_Master poMaster;
    ArrayList<Model_PO_Detail> poDetail;

    @Override
    public void setApplicationDriver(Object applicationDriver) {
        poGrider = (GRiderCAS) applicationDriver;
    }

    @Override
    public void setTransactionStatus(String transactionStatus) {
        psTranStat = transactionStatus;
    }

    @Override
    public void setMaster(Object value) {
        poMaster = (Model_PO_Master) value;
    }

    @Override
    public void setDetail(ArrayList<Object> value) {
        poDetail.clear();
        for (int lnCtr = 0; lnCtr <= value.size() - 1; lnCtr++) {
            poDetail.add((Model_PO_Detail) value.get(lnCtr));
        }
    }

    @Override
    public void setOthers(ArrayList<Object> value) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public JSONObject validate() {
        try {
            switch (psTranStat) {
                case PurchaseOrderStatus.OPEN:
                    return validateNew();
                case PurchaseOrderStatus.CONFIRMED:
                    return validateConfirmed();
                case PurchaseOrderStatus.APPROVED:
                    return validateApproved();
                case PurchaseOrderStatus.CANCELLED:
                    return validateCancelled();
                case PurchaseOrderStatus.VOID:
                    return validateVoid();
                default:
                    poJSON = new JSONObject();
                    poJSON.put("result", "success");
            }

            return poJSON;
        } catch (SQLException ex) {
            Logger.getLogger(PurchaseOrder_MP.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    private JSONObject validateNew() throws SQLException {
        poJSON = new JSONObject();
        LocalDate transactionDate = new java.sql.Date(poMaster.getTransactionDate().getTime()).toLocalDate();
        LocalDate expectedDate = new java.sql.Date(poMaster.getExpectedDate().getTime()).toLocalDate();
        LocalDate serverDate = new java.sql.Date(poGrider.getServerDate().getTime()).toLocalDate();
        LocalDate oneYearAgo = serverDate.minusYears(1);

        if (poMaster.getSupplierID() == null || poMaster.getTermCode().isEmpty()) {
            poJSON.put("message", "Invalid Suuplier.");
            return poJSON;
        }
        if (transactionDate == null) {
            poJSON.put("message", "Invalid Transaction Date.");
            return poJSON;
        }

        if (LocalDate.of(1900, 1, 1).equals(transactionDate)) {
            poJSON.put("message", "Invalid Transaction Date.");
            return poJSON;
        }

        if (transactionDate.isAfter(serverDate)) {
            poJSON.put("message", "Future transaction dates are not allowed.");
            return poJSON;
        }

        // Backdated beyond 1 year validation
        if (transactionDate.isBefore(oneYearAgo)) {
            poJSON.put("message", "Backdated transactions beyond 1 year are not allowed.");
            return poJSON;
        }

        if (poMaster.getDestinationID() == null || poMaster.getDestinationID().isEmpty()) {
            poJSON.put("message", "Destination is not set.");
            return poJSON;
        }
        if (poMaster.getExpectedDate() == null) {
            poJSON.put("message", "Invalid Expected Delivery Date.");
            return poJSON;
        }
        if ("1900-01-01".equals(expectedDate)) {
            poJSON.put("message", "Invalid Expected Delivery Transaction Date.");
            return poJSON;
        }
        if (expectedDate.isBefore(transactionDate)) {
            poJSON.put("message", "The expected date cannot be earlier than the transaction date.");
            return poJSON;
        }

        if (poMaster.getTermCode() == null || poMaster.getTermCode().isEmpty()) {
            poJSON.put("message", "Invalid Term.");
            return poJSON;
        }
        if (poMaster.getWithAdvPaym() == true) {
            if (poMaster.getDownPaymentRatesPercentage() == null
                    || poMaster.getDownPaymentRatesPercentage().doubleValue() < 0.00
                    || poMaster.getDownPaymentRatesPercentage().doubleValue() > 100.00
                    || poMaster.getDownPaymentRatesPercentage().doubleValue() < 0.0
                    || poMaster.getDownPaymentRatesPercentage().doubleValue() > 100.00) {
                poJSON.put("message", "Invalid Advance Payment Rates.");
                return poJSON;
            }
            if (poMaster.getDownPaymentRatesAmount() == null || poMaster.getDownPaymentRatesAmount().doubleValue() < 0.00) {
                poJSON.put("message", "Invalid Advance Payment Amount.");
                return poJSON;
            }
            if (poMaster.getDownPaymentRatesPercentage() == null) {
                poJSON.put("message", "Invalid Advance Payment Rates.");
                return poJSON;
            }
            if (poMaster.getDownPaymentRatesAmount() == null) {
                poJSON.put("message", "Invalid Advance Payment Amount.");
                return poJSON;
            }
        }
        if (transactionDate.isBefore(serverDate) && poMaster.getReference().trim().isEmpty()) {
            poJSON.put("message", "A reference number is required for backdated transactions.");
            return poJSON;
        }
        poJSON.put("result", "success");
        return poJSON;
    }

    private JSONObject validateConfirmed() {
        poJSON = new JSONObject();

        poJSON.put("result", "success");
        return poJSON;
    }

    private JSONObject validateApproved() {
        poJSON = new JSONObject();

        poJSON.put("result", "success");
        return poJSON;
    }

    private JSONObject validateCancelled() {
        poJSON = new JSONObject();

        poJSON.put("result", "success");
        return poJSON;
    }

    private JSONObject validateVoid() {
        poJSON = new JSONObject();

        poJSON.put("result", "success");
        return poJSON;
    }
}
