package org.guanzon.cas.purchasing.validator;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.guanzon.appdriver.agent.ShowDialogFX;
import org.guanzon.appdriver.base.GRiderCAS;
import org.guanzon.appdriver.constant.EditMode;
import org.guanzon.appdriver.iface.GValidator;
import org.guanzon.cas.purchasing.status.PurchaseOrderStatus;
import org.guanzon.cas.purchasing.model.Model_PO_Detail;
import org.guanzon.cas.purchasing.model.Model_PO_Master;
import org.json.simple.JSONObject;

public class PurchaseOrder_LP implements GValidator {

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
        Date loTransactionDate = poMaster.getTransactionDate();
        Date loExpectedDate = poMaster.getExpectedDate();

        if (loTransactionDate == null) {
            poJSON.put("message", "Invalid Transaction Date.");
            return poJSON;
        }

        if ("1900-01-01".equals(xsDateShort(loTransactionDate))) {
            poJSON.put("message", "Invalid Transaction Date.");
            return poJSON;
        }
        LocalDate transactionDate = poMaster.getTransactionDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate serverDate = poGrider.getServerDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate oneYearAgo = serverDate.minusYears(1);

        if (transactionDate.isAfter(serverDate)) {
            poJSON.put("message", "Future transaction dates are not allowed.");
            return poJSON;
        }

        // Backdated beyond 1 year validation
        if (transactionDate.isBefore(oneYearAgo)) {
            poJSON.put("message", "Backdated transactions beyond 1 year are not allowed.");
            return poJSON;
        }

        if (transactionDate.isBefore(serverDate)) {
            String referenceNo = poMaster.getReference();
            if (referenceNo == null || referenceNo.trim().isEmpty()) {
                poJSON.put("message", "A reference number is required for backdated transactions.");
                return poJSON;
            }
        }

        if (poMaster.getIndustryID() == null) {
            poJSON.put("message", "Industry is not set.");
            return poJSON;
        }
        if (poMaster.getCompanyID() == null || poMaster.getCompanyID().isEmpty()) {
            poJSON.put("message", "Company is not set.");
            return poJSON;
        }
//        if (poMaster.getSupplierID() == null || poMaster.getSupplierID().isEmpty()) {
//            poJSON.put("message", "Supplier is not set.");
//            return poJSON;
//        }
        if (poMaster.getDestinationID() == null || poMaster.getDestinationID().isEmpty()) {
            poJSON.put("message", "Destination is not set.");
            return poJSON;
        }
        if (poMaster.getExpectedDate() == null) {
            poJSON.put("message", "Invalid Expected Delivery Date.");
            return poJSON;
        }
        if ("1900-01-01".equals(xsDateShort(loExpectedDate))) {
            poJSON.put("message", "Invalid Expected Delivery Transaction Date.");
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
        poJSON.put("result", "success");
        return poJSON;
    }

    private static String xsDateShort(Date fdValue) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String date = sdf.format(fdValue);
        return date;
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
