package org.guanzon.cas.purchasing.services;

import org.guanzon.appdriver.base.GRiderCAS;
import org.guanzon.cas.purchasing.model.Model_PO_Detail;
import org.guanzon.cas.purchasing.model.Model_PO_Master;

public class PurchaseOrderModels {

    public PurchaseOrderModels(GRiderCAS applicationDriver) {
        poGRider = applicationDriver;
    }

    public Model_PO_Master PurchaseOrderMaster() {
        if (poGRider == null) {
            System.err.println("PurchaseOrderModels.PurchaseOrderMaster: Application driver is not set.");
            return null;
        }

        if (POMaster == null) {
            POMaster = new Model_PO_Master();
            POMaster.setApplicationDriver(poGRider);
            POMaster.setXML("Model_PO_Master");
            POMaster.setTableName("PO_Master");
            POMaster.initialize();
        }

        return POMaster;
    }

    public Model_PO_Detail PurchaseOrderDetails() {
        if (poGRider == null) {
            System.err.println("PurchaseOrderModels.PurchaseOrderDetails: Application driver is not set.");
            return null;
        }

        if (PODetail == null) {
            PODetail = new Model_PO_Detail();
            PODetail.setApplicationDriver(poGRider);
            PODetail.setXML("Model_PO_Detail");
            PODetail.setTableName("PO_Detail");
            PODetail.initialize();
        }

        return PODetail;
    }

    private final GRiderCAS poGRider;

    private Model_PO_Master POMaster;
    private Model_PO_Detail PODetail;
}
